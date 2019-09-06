package minecraft.rhanjie.locksystem.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Random;

public class ChestInteractionListener implements Listener {
    @EventHandler(priority = EventPriority.LOW)
    public void onChestInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getClickedBlock() == null)
            return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType() == Material.CHEST) {
            //TODO: Check if chest has lockpick. If yes, check if player is owner and set variables below
            Random debugRandom = new Random(); //TODO: Only for testing
            boolean playerIsOwner = debugRandom.nextBoolean();
            int level = 1;

            Material itemInHand = player.getInventory().getItemInMainHand().getType();

            if (itemInHand.equals(Material.AIR)) {
                //TODO: Chest info from config file
                if (playerIsOwner) {
                    player.sendMessage("To jest twoja skrzynia");
                    player.sendMessage("Poziom " + ChatColor.GREEN + level);
                    player.sendMessage("Jesli chcesz zwiekszyc poziom klodki, kliknij zelaznym blokiem");

                    return;
                }

                player.sendMessage( "Skrzynia " + ChatColor.GREEN + player.getName());
                player.sendMessage( "Zabezpieczenia solidne");
            }

            else if (itemInHand.equals(Material.SHEARS)) {
                if (!playerIsOwner) {
                    //TODO: Calculate the chance of success. If it succeeded, open the chest. If not, tell that accident to the owner
                    //e.g 10% + 10% * player lockpicking level - (padlock level - 1) * 50

                    Random random = new Random();

                    if (random.nextInt(100) > 50) {
                        player.sendMessage(ChatColor.GREEN + "Wlamales sie do skrzyni");

                        return;
                    }

                    player.sendMessage(ChatColor.RED + "Zlamales wytrych i zostawiles slady...");
                    event.setCancelled(true);
                }
            }
        }
    }
}
