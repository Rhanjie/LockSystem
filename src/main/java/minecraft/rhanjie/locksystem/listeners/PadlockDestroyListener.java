package minecraft.rhanjie.locksystem.listeners;

import minecraft.rhanjie.locksystem.LockSystem;
import minecraft.throk.api.API;
import minecraft.throk.api.database.UpdateQuery;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class PadlockDestroyListener implements Listener {
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPadlockDestroy(BlockBreakEvent event) {
        FileConfiguration config = LockSystem.access.getConfig();
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if ((LockSystem.access).checkIfElementIsAvailable(config, block.getType().toString(), "lockableBlocks") == -1)
            return;

        String conditionWhere = (LockSystem.access).getAutomaticConditionWhere(block);
        ResultSet result = LockSystem.access.getInfoFromDatabase(player,
                "SELECT player_list.uuid, level FROM locked_objects_list " +
                "INNER JOIN player_list ON locked_objects_list.owner_id = player_list.id WHERE " + conditionWhere);

        if (result == null)
            return;

        boolean isLocked;
        boolean playerIsOwner = false;
        int padlockLevel = 1;

        try {
            isLocked = result.next();

            if (!isLocked)
                return;

            String uniqueID = player.getUniqueId().toString();
            playerIsOwner = uniqueID.equals(result.getString(1));
            padlockLevel = result.getInt(2);
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        if (!playerIsOwner && !player.hasPermission("LockSystem.destroyLockable")) {
            player.sendMessage(LockSystem.access.getMessage("lockable.destroyFail"));

            event.setCancelled(true);
            return;
        }

        if (block.getState() instanceof Chest) {
            Location location = LockSystem.access.getChestSecondPartLocation((Chest) block.getState());
            if (location != null) {
                try {
                    UpdateQuery query = (UpdateQuery) API.getDatabase().updateQuery();
                    PreparedStatement statement = query.setQuery("UPDATE locked_objects_list" +
                            " SET loc_x = ?, loc_y = ?, loc_z = ? WHERE " + conditionWhere);

                    statement.setInt(1, location.getBlockX());
                    statement.setInt(2, location.getBlockY());
                    statement.setInt(3, location.getBlockZ());
                    query.execute();
                }

                catch (SQLException exception) {
                    player.sendMessage(ChatColor.RED + "Cos poszlo nie tak! Zglos to krolowi");
                    exception.printStackTrace();

                    event.setCancelled(true);
                }

                return;
            }
        }

        try {
            UpdateQuery query = (UpdateQuery) API.getDatabase().updateQuery();
            PreparedStatement statement = query.setQuery("UPDATE locked_objects_list SET destroyed_at = now(), " +
                    "destroy_guilty = ?, destroy_reason = 'Zniszczenie bloku' WHERE " + conditionWhere);

            statement.setString(1, player.getName());
            query.execute();
        }

        catch (SQLException exception) {
            player.sendMessage(ChatColor.RED + "Cos poszlo nie tak! Zglos to krolowi");
            exception.printStackTrace();

            event.setCancelled(true);
            return;
        }

        if (playerIsOwner) {
            List<String> padlocks = config.getStringList("padlocks");
            String padlockName = padlocks.get(padlockLevel - 1).toUpperCase();

            Material padlockMaterial = Material.getMaterial(padlockName);
            if (padlockMaterial != null) {
                player.getInventory().addItem(new ItemStack(padlockMaterial));
            }
        }
    }
}
