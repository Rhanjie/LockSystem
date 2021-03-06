package minecraft.rhanjie.locksystem;

import minecraft.rhanjie.locksystem.commands.PluginCommand;
import minecraft.rhanjie.locksystem.listeners.PadlockDestroyListener;
import minecraft.rhanjie.locksystem.listeners.PadlockInteractionListener;
import minecraft.rhanjie.locksystem.utility.ConfigManager;
import minecraft.throk.api.API;
import minecraft.throk.api.database.SelectQuery;
import minecraft.throk.api.database.Transaction;
import minecraft.throk.api.database.UpdateQuery;
import minecraft.throk.api.exceptions.EntityNotFound;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.type.Door;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class LockSystem extends JavaPlugin {
    public static LockSystem access;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        access = this;

        configManager = new ConfigManager(this);

        this.prepareMySqlTable();
        this.checkAllPadlocks();

        this.registerCommands();
        this.registerListeners();
    }

    @Override
    public void onDisable() {
    }

    public String getMessage(String id) {
        return configManager.getMessage(id);
    }

    //TODO: Move code below to utils file
    public String getAutomaticConditionWhere(Block block) {
        if (block.getState() instanceof Chest) {
            Location secondLocation = LockSystem.access.getChestSecondPartLocation((Chest) block.getState());

            if (secondLocation != null)
                return LockSystem.access.getDoubleConditionWhere(block.getLocation(), secondLocation);
        }

        if (block.getBlockData() instanceof Door) {
            Location secondLocation = LockSystem.access.getDoorSecondPartLocation((Door) block.getBlockData(), block.getLocation());

            return LockSystem.access.getDoubleConditionWhere(block.getLocation(), secondLocation);
        }

        return LockSystem.access.getStandardConditionWhere(block.getLocation());
    }

    public int checkIfElementIsAvailable(FileConfiguration config, String findingPhrase, String configId) {
        List<String> elements = config.getStringList(configId);

        int number = 1;
        for (String element : elements) {
            if (findingPhrase.equalsIgnoreCase(element))
                return number;

            number += 1;
        }

        return -1;
    }

    public Location getChestSecondPartLocation(Chest chest) {
        InventoryHolder holder = chest.getInventory().getHolder();
        if (holder instanceof DoubleChest) {
            DoubleChest doubleChest = (DoubleChest) holder;
            Chest secondPart = (Chest) doubleChest.getLeftSide();

            if (doubleChest.getLeftSide() == null && doubleChest.getRightSide() == null)
                return null;

            if (chest.getBlock().getLocation().equals(secondPart.getLocation()))
                secondPart = (Chest) doubleChest.getRightSide();

            return secondPart.getBlock().getLocation();
        }

        return null;
    }

    //TODO; Add support for varargs
    public ResultSet getInfoFromDatabase(Player player, String queryContent) { //Object... arguments
        try {
            SelectQuery query = (SelectQuery) API.getDatabase().selectQuery();
            query.setQuery(queryContent);
            query.execute();

            return query.getResultSet();
        }

        catch (SQLException exception) {
            if (player != null)
                player.sendMessage(ChatColor.RED + "Cos poszlo nie tak! Zglos to krolowi");

            exception.printStackTrace();
            return null;
        }
    }

    private Location getDoorSecondPartLocation(Door door, Location secondLocation) {
        String doorPart = door.getHalf().toString();

        if (doorPart.equals("TOP"))
            secondLocation.setY(secondLocation.getBlockY() - 1);

        else if (doorPart.equals("BOTTOM"))
            secondLocation.setY(secondLocation.getBlockY() + 1);

        return secondLocation;
    }

    private String getStandardConditionWhere(Location location) {
        int loc_x = location.getBlockX();
        int loc_y = location.getBlockY();
        int loc_z = location.getBlockZ();

        return "world_uuid = '" + location.getWorld().getUID().toString() +
                "' AND loc_x = " + loc_x + " AND loc_y = " + loc_y + " AND loc_z = " + loc_z + " AND destroyed_at IS NULL;";
    }

    private String getDoubleConditionWhere(Location firstPartLoc, Location secondPartLoc) {
        return  "world_uuid = '" + firstPartLoc.getWorld().getUID().toString() +
                "' AND (loc_x = " + firstPartLoc.getBlockX() + " AND loc_y = " + firstPartLoc.getBlockY() + " AND loc_z = " + firstPartLoc.getBlockZ() +
                " OR loc_x = " + secondPartLoc.getBlockX() + " AND loc_y = " + secondPartLoc.getBlockY() + " AND loc_z = " + secondPartLoc.getBlockZ() +
                ") AND destroyed_at IS NULL;";
    }

    private void prepareMySqlTable() {
        //TODO: I created `world_uuid` but it doesn't work in seggelin. Need test!
        String createLockedObjectsListTable =
                "CREATE TABLE IF NOT EXISTS locked_objects_list(id int AUTO_INCREMENT NOT NULL PRIMARY KEY, " +
                "world_uuid varchar(255) NOT NULL, loc_x int NOT NULL, loc_y int NOT NULL, loc_z int NOT NULL, " +
                "type varchar(255) NOT NULL, owner_id int NOT NULL, level int NOT NULL, " +
                "created_at datetime NOT NULL, last_break_attempt datetime, break_protection_time datetime, " +
                "destroyed_at datetime, destroy_guilty varchar(255), destroy_reason varchar(255), " +
                "KEY owner_id (owner_id), FOREIGN KEY (owner_id) REFERENCES player_list(id)) " +
                "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String createLockedObjectsMembersListTable =
                "CREATE TABLE IF NOT EXISTS locked_objects_members_list(id int AUTO_INCREMENT NOT NULL PRIMARY KEY, " +
                "locked_object_id int NOT NULL, uuid varchar(255) NOT NULL, added_at datetime NOT NULL, " +
                "KEY locked_object_id (locked_object_id), FOREIGN KEY (locked_object_id) REFERENCES locked_objects_list(id)) " +
                "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        try {
            Transaction transaction = API.getDatabase().transaction();
            transaction.start();

            UpdateQuery query = (UpdateQuery) API.getDatabase().updateQuery();
            query.setQuery(createLockedObjectsListTable);
            query.execute();

            query.setQuery(createLockedObjectsMembersListTable);
            query.execute();

            transaction.commit();
        }

        catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private void checkAllPadlocks() {
        ResultSet result = this.getInfoFromDatabase(null, "SELECT * FROM locked_objects_list");

        if (result == null)
            return;

        try {
            while (result.next()) {
                UUID worldUuid = UUID.fromString(result.getString(2));
                World world = Bukkit.getWorld(worldUuid);

                double x = result.getInt(3);
                double y = result.getInt(4);
                double z = result.getInt(5);

                Location location = new Location(world, x, y, z);
                Block block = location.getBlock();

                if ((LockSystem.access).checkIfElementIsAvailable(this.getConfig(), block.getType().toString(), "lockableBlocks") != -1)
                    continue;

                try {
                    String conditionWhere = (LockSystem.access).getAutomaticConditionWhere(block);
                    UpdateQuery query = (UpdateQuery) API.getDatabase().updateQuery();
                    query.setQuery("UPDATE locked_objects_list SET destroyed_at = now(), " +
                            "destroy_guilty = undefined, destroy_reason = 'Blad! Brak bloku, uszkodzony zamek' WHERE " + conditionWhere);

                    query.execute();
                }

                catch (SQLException exception) {
                    exception.printStackTrace();

                    return;
                }
            }
        }

        catch(SQLException exception) {
            exception.printStackTrace();
        }
    }

    private void registerCommands() {
        this.getCommand("klodka").setExecutor(new PluginCommand());

        //...
    }

    private void registerListeners() {
        PluginManager manager = this.getServer().getPluginManager();

        manager.registerEvents(new PadlockInteractionListener(), this);
        manager.registerEvents(new PadlockDestroyListener(), this);
    }
}
