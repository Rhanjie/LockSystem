package minecraft.rhanjie.locksystem.listeners;

import minecraft.rhanjie.locksystem.LockSystem;
import minecraft.throk.api.API;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;

public class ChestInteractionListener implements Listener {
    @EventHandler(priority = EventPriority.LOW)
    public void onChestInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getClickedBlock() == null)
            return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType() == Material.CHEST) {
            //TODO; Not tested yet
            int loc_x = event.getClickedBlock().getLocation().getBlockX();
            int loc_y = event.getClickedBlock().getLocation().getBlockY();
            int loc_z = event.getClickedBlock().getLocation().getBlockZ();
            ResultSet result = API.selectSQL("SELECT owner, chestLevel FROM locked_chests WHERE" +
                    "loc_x = " + loc_x + " AND " +
                    "loc_y = " + loc_y + " AND " +
                    "loc_z = " + loc_z);

            boolean isChestLocked = false;
            boolean playerIsOwner = false;
            int chestLevel = 1;

            //TODO: Clean try catch code
            try {
                isChestLocked = !result.next();
            } catch (SQLException exception) {
                exception.printStackTrace();
            }

            try {
                String uniqueID = player.getUniqueId().toString();
                playerIsOwner = (isChestLocked && uniqueID.equals(result.getString(0)));
            } catch(SQLException exception) {
                exception.printStackTrace();
            }

            try {
                chestLevel = result.getInt(1);
            } catch(SQLException exception) {
                exception.printStackTrace();
            }

            Material itemInHand = player.getInventory().getItemInMainHand().getType();
            FileConfiguration config = LockSystem.access.getConfig();

            if (isChestLocked) {
                if (itemInHand.equals(Material.AIR)) {
                    //TODO: Chest info from config file
                    if (playerIsOwner) {
                        player.sendMessage("Poziom twojej skrzyni: " + ChatColor.GREEN + chestLevel);
                        player.sendMessage("Jesli chcesz zwiekszyc poziom klodki, kliknij odpowiednim blokiem");

                        return;
                    }

                    player.sendMessage( "Skrzynia " + ChatColor.RED + player.getName());
                    player.sendMessage( "Zabezpieczenia solidne");

                    event.setCancelled(true);
                    return;
                }

                List<String> picklocks = config.getStringList("picklocks");

                for (String picklock : picklocks) {
                    if (itemInHand.toString().equalsIgnoreCase(picklock)) {
                        if (!playerIsOwner) {
                            //TODO: Calculate the chance of success. If it succeeded, open the chest. If not, tell that accident to the owner
                            //e.g 10% + 10% * player lockpicking chestLevel - (padlock chestLevel - 1) * 50

                            Random random = new Random();
                            if (random.nextInt(100) > 50) {
                                player.sendMessage(ChatColor.GREEN + "Wlamales sie do skrzyni");

                                return;
                            }

                            player.sendMessage(ChatColor.RED + "Zlamales wytrych i zostawiles slady...");
                            event.setCancelled(true);

                            return;
                        }
                    }
                }
            }

            List<String> padlocks = config.getStringList("padlocks");

            for (String padlock : padlocks) {
                if (itemInHand.toString().equalsIgnoreCase(padlock)) {
                    if (playerIsOwner) {
                        player.sendMessage(ChatColor.RED + "To nie jest twoja skrzynia!");
                        return;
                    }


                    //TODO: Add padlock to the chest
                    player.sendMessage(ChatColor.GREEN + "Klodka zalozona!");

                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
}
