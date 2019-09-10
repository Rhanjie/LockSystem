package minecraft.rhanjie.locksystem.listeners;

import minecraft.rhanjie.locksystem.LockSystem;
import minecraft.throk.api.API;
import minecraft.throk.api.SeggelinPlayer;
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

            API.playerList.put(player.getUniqueId().toString(), new SeggelinPlayer(player.getName(), player.getUniqueId().toString()));
            SeggelinPlayer seggelinPlayer = API.playerList.get(player.getUniqueId().toString());

            ResultSet result = API.selectSQL("SELECT player_list.uuid, player_list.name, level FROM locked_chests_list " +
                    "INNER JOIN player_list ON locked_chests_list.owner_id = player_list.id WHERE " +
                    "loc_x = " + loc_x + " AND " +
                    "loc_y = " + loc_y + " AND " +
                    "loc_z = " + loc_z + ";");

            boolean isChestLocked = false;
            boolean playerIsOwner = false;
            String ownerName = player.getName();
            int chestLevel = 1;

            //TODO: Clean try catch code
            try {
                isChestLocked = result.next();
            } catch (SQLException exception) {
                exception.printStackTrace();
            }

            if (isChestLocked) {
                try {
                    String uniqueID = player.getUniqueId().toString();
                    playerIsOwner = uniqueID.equals(result.getString(1));

                    ownerName = result.getString(2);
                    chestLevel = result.getInt(3);
                } catch (SQLException exception) {
                    exception.printStackTrace();
                }
            }

            Material itemInHand = player.getInventory().getItemInMainHand().getType();
            FileConfiguration config = LockSystem.access.getConfig();

            List<String> padlocks = config.getStringList("padlocks");

            for (String padlock : padlocks) {
                if (itemInHand.toString().equalsIgnoreCase(padlock)) {
                    if (isChestLocked) {
                        if (!playerIsOwner) {
                            player.sendMessage(ChatColor.RED + "To nie jest twoja skrzynia!");

                            event.setCancelled(true);
                            return;
                        }

                        //TODO: Update padlock level to the chest
                        /*String name = API.playerList.get(player.getUniqueId().toString()).name;
                        API.updateSQL("UPDATE ...");*/

                        player.sendMessage(ChatColor.GREEN + "[test] Klodka zalozona!");

                        event.setCancelled(true);
                        return;
                    }


                    API.updateSQL("INSERT INTO `locked_chests_list`(`loc_x`, `loc_y`, `loc_z`, `owner_id`, `level`) " +
                            "values (" + loc_x + ", " + loc_y + ", " + loc_z + ", " + seggelinPlayer.id + ", 1);");

                    player.sendMessage(ChatColor.GREEN + "Klodka zalozona!");

                    event.setCancelled(true);
                    return;
                }
            }

            if (isChestLocked) {
                if (itemInHand.equals(Material.AIR)) {
                    //TODO: Chest info from config file
                    if (playerIsOwner) {
                        player.sendMessage("Poziom twojej skrzyni: " + ChatColor.GREEN + chestLevel);
                        player.sendMessage("Jesli chcesz zwiekszyc poziom klodki, kliknij odpowiednim blokiem");

                        return;
                    }

                    player.sendMessage( "Skrzynia " + ChatColor.RED + ownerName);
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
        }
    }
}
