package minecraft.rhanjie.locksystem.listeners;

import minecraft.rhanjie.locksystem.LockSystem;
import minecraft.throk.api.API;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.InventoryHolder;
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

        String conditionWhere = LockSystem.access.getStandardConditionWhere(block.getLocation());
        ResultSet result = API.selectSQL("SELECT player_list.uuid, level FROM locked_objects_list " +
                "INNER JOIN player_list ON locked_objects_list.owner_id = player_list.id WHERE " + conditionWhere);

        boolean isLocked;
        boolean playerIsOwner = false;
        int padlockLevel = 1;

        try {
            isLocked = result.next();

            if (isLocked) {
                String uniqueID = player.getUniqueId().toString();
                playerIsOwner = uniqueID.equals(result.getString(1));
                padlockLevel = result.getInt(2);
            }

            else return;
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        if (!playerIsOwner && !player.hasPermission("LockSystem.destroyLockable")) {
            player.sendMessage(LockSystem.access.getMessage("lockable.destroyFail"));

            event.setCancelled(true);
            return;
        }

        if (block.getState() instanceof Chest) {
            Chest chest = (Chest) block.getState();
            InventoryHolder holder = chest.getInventory().getHolder();
            if (holder instanceof DoubleChest) {
                DoubleChest doubleChest = (DoubleChest) holder;
                Chest secondPart = (Chest) doubleChest.getLeftSide();

                if (secondPart != null) {
                    if (block.getLocation().equals(secondPart.getLocation()))
                        secondPart = (Chest) doubleChest.getRightSide();
                }

                if (secondPart != null) {
                    int newLocX = secondPart.getBlock().getLocation().getBlock().getLocation().getBlockX();
                    int newLocY = secondPart.getBlock().getLocation().getBlock().getLocation().getBlockY();
                    int newLocZ = secondPart.getBlock().getLocation().getBlock().getLocation().getBlockZ();

                    API.updateSQL("UPDATE locked_objects_list SET loc_x = " + newLocX + ", loc_y = " + newLocY + ", loc_z = " + newLocZ +
                            ", sec_loc_x = null, sec_loc_y = null, sec_loc_z = null, destroy_guilty = '" + player.getName() +
                            "', destroy_reason = 'Zniszczenie bloku' WHERE " + conditionWhere);
                }
            }
        }

        API.updateSQL("UPDATE locked_objects_list SET destroyed_at = now(), " +
                "destroy_guilty = '" + player.getName() + "', destroy_reason = 'Zniszczenie bloku' WHERE " + conditionWhere);

        if (playerIsOwner) {
            List<String> padlocks = config.getStringList("padlocks");
            Material padlockMaterial = Material.getMaterial(padlocks.get(padlockLevel - 1).toUpperCase());
            if (padlockMaterial != null) {
                player.getInventory().addItem(new ItemStack(padlockMaterial));
            }
        }
    }
}
