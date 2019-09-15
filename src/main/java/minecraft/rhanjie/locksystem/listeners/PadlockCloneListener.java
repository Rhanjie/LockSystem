package minecraft.rhanjie.locksystem.listeners;

import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;

public class PadlockCloneListener implements Listener {
    @EventHandler(priority = EventPriority.NORMAL)
    public void onChestBuild(BlockPlaceEvent event) {
        Block block = event.getBlock();

        //TODO: Not finished yet
        event.getPlayer().sendMessage("[DEBUG] 111");
        if (event.getBlock().getState() instanceof Chest) {
            Chest chest = (Chest) block.getState();
            InventoryHolder holder = chest.getInventory().getHolder();

            event.getPlayer().sendMessage("[DEBUG] 222");
            if (holder instanceof DoubleChest) {
                DoubleChest doubleChest = (DoubleChest) holder;
                Chest secondPart = (Chest) doubleChest.getLeftSide();

                event.getPlayer().sendMessage("[DEBUG] Teraz stworzyles podwojna skrzynie");
            }
        }
    }
}
