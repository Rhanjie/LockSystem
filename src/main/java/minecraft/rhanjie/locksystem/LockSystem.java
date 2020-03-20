package minecraft.rhanjie.locksystem;

import minecraft.rhanjie.locksystem.commands.PluginCommand;
import minecraft.rhanjie.locksystem.database.SelectLockedObjectsQuery;
import minecraft.rhanjie.locksystem.database.UpdateLocketObjectsQuery;
import minecraft.rhanjie.locksystem.listeners.PadlockDestroyListener;
import minecraft.rhanjie.locksystem.listeners.PadlockInteractionListener;
import minecraft.rhanjie.locksystem.utility.ConfigManager;
import minecraft.rhanjie.locksystem.utility.Utility;
import minecraft.throk.api.API;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class LockSystem extends JavaPlugin {
    public static LockSystem access;
    private ConfigManager configManager;
    private API api;

    @Override
    public void onEnable() {
        access = this;

        api = (API) getServer().getPluginManager().getPlugin("API");

        if (api == null) {
            Bukkit.getLogger().severe("API plugin not found!");
            Bukkit.getPluginManager().disablePlugin(this);

            return;
        }

        try {
            api.getDatabase().runPluginMigrations(this);
        }

        catch(SQLException exception) {
            Bukkit.getLogger().severe("Database migration doesn't work!");
            Bukkit.getPluginManager().disablePlugin(this);

            return;
        }

        configManager = new ConfigManager(this);

        this.prepareMySqlTable();
        this.checkAllPadlocks();

        this.registerCommands();
        this.registerListeners();
    }

    public String getMessage(String id) {
        return configManager.getMessage(id);
    }

    private void prepareMySqlTable() {
        try {
            api.getDatabase().runPluginMigrations(this);
        }

        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void checkAllPadlocks() {
        SelectLockedObjectsQuery selectLockedObjectsQuery = new SelectLockedObjectsQuery(api.getDatabase());

        ResultSet result = selectLockedObjectsQuery.getAllLockedObjects();

        try {
            while (result.next()) {
                UUID worldUuid = UUID.fromString(result.getString(2));
                World world = Bukkit.getWorld(worldUuid);

                double x = result.getInt(3);
                double y = result.getInt(4);
                double z = result.getInt(5);

                Location location = new Location(world, x, y, z);
                Block block = location.getBlock();

                if (Utility.checkIfElementIsAvailable(this.getConfig(), block.getType().toString(), "lockableBlocks") != -1)
                    continue;

                UpdateLocketObjectsQuery updateLocketObjectsQuery = new UpdateLocketObjectsQuery(api.getDatabase());

                String destroyReason = "Blad! Brak bloku, uszkodzony zamek!";
                updateLocketObjectsQuery.destroy(block, "undefined", destroyReason);
            }
        }

        catch(SQLException exception) {
            exception.printStackTrace();
        }
    }

    private void registerCommands() {
        this.getCommand("klodka").setExecutor(new PluginCommand(api.getDatabase()));
    }

    private void registerListeners() {
        PluginManager manager = this.getServer().getPluginManager();

        manager.registerEvents(new PadlockInteractionListener(api.getDatabase()), this);
        manager.registerEvents(new PadlockDestroyListener(api.getDatabase()), this);
    }
}
