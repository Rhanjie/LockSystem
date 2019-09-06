package minecraft.rhanjie.locksystem.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ChestInteractionListener implements Listener {
    @EventHandler(priority = EventPriority.LOW)
    public void onChestInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getClickedBlock() == null)
            return;

        if (event.getClickedBlock().getType() == Material.CHEST) {
            //TODO: Check if chest has lockpick
            //TODO: Check chest owner
            boolean playerIsOwner = true;

            ItemStack lockpick = new ItemStack(Material.SHEARS);
            Material itemInHand = player.getInventory().getItemInMainHand().getType();

            if (itemInHand.equals(Material.AIR)) {

                if (playerIsOwner) {
                    player.sendMessage("To jest twoja skrzynia");
                    //TODO: Chech info
                }

                else {
                    player.sendMessage("To nie jest twoja skrzynia");
                }
            }

            else if (itemInHand.equals(Material.SHEARS)) {
                //TODO: Edit mode

                player.sendMessage("Probowales wlamac sie do skrzyni!");
                event.setCancelled(true);
            }


        }
    }
}
