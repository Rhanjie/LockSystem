package minecraft.rhanjie.locksystem.listeners;

import minecraft.rhanjie.locksystem.LockSystem;
import minecraft.throk.api.API;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.data.type.Door;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

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
        ResultSet result = API.selectSQL("SELECT player_list.uuid, level FROM locked_objects_list " +
                "INNER JOIN player_list ON locked_objects_list.owner_id = player_list.id WHERE " + conditionWhere);

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
                API.updateSQL("UPDATE locked_objects_list SET loc_x = " + location.getBlockX() +
                        ", loc_y = " + location.getBlockY() + ", loc_z = " + location.getBlockZ() + " WHERE " + conditionWhere);

                return;
            }
        }

        API.updateSQL("UPDATE locked_objects_list SET destroyed_at = now(), " +
                "destroy_guilty = '" + player.getName() + "', destroy_reason = 'Zniszczenie bloku' WHERE " + conditionWhere);

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
