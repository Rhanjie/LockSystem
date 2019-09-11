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
            int loc_x = event.getClickedBlock().getLocation().getBlockX();
            int loc_y = event.getClickedBlock().getLocation().getBlockY();
            int loc_z = event.getClickedBlock().getLocation().getBlockZ();

            String conditionWhere = "loc_x = " + loc_x + " AND loc_y = " + loc_y + " AND loc_z = " + loc_z + ";";

            API.playerList.put(player.getUniqueId().toString(), new SeggelinPlayer(player.getName(), player.getUniqueId().toString()));
            SeggelinPlayer seggelinPlayer = API.playerList.get(player.getUniqueId().toString());

            ResultSet result = API.selectSQL("SELECT player_list.uuid, player_list.name, level FROM locked_chests_list " +
                    "INNER JOIN player_list ON locked_chests_list.owner_id = player_list.id WHERE " + conditionWhere);

            boolean isChestLocked = false;
            boolean playerIsOwner = false;
            String ownerName = player.getName();
            int chestLevel = 1;
            int newChestLevel = 0;
            int picklockLevel = 0;

            try {
                isChestLocked = result.next();

                if (isChestLocked) {
                    String uniqueID = player.getUniqueId().toString();
                    playerIsOwner = uniqueID.equals(result.getString(1));

                    ownerName = result.getString(2);
                    chestLevel = result.getInt(3);
                }
            } catch (SQLException exception) {
                exception.printStackTrace();
            }

            Material itemInHand = player.getInventory().getItemInMainHand().getType();
            FileConfiguration config = LockSystem.access.getConfig();

            List<String> padlocks = config.getStringList("padlocks");
            for (String padlock : padlocks) {
                newChestLevel += 1;

                if (itemInHand.toString().equalsIgnoreCase(padlock)) {
                    if (isChestLocked) {
                        if (!playerIsOwner) {
                            player.sendMessage(LockSystem.access.getMessage("chest.notOwner"));

                            event.setCancelled(true);
                            return;
                        }

                        if (chestLevel >= newChestLevel) {
                            player.sendMessage(LockSystem.access.getMessage("chest.improveFail"));

                            event.setCancelled(true);
                            return;
                        }

                        API.updateSQL("UPDATE locked_chests_list SET level = " + newChestLevel + " WHERE " + conditionWhere);
                        player.sendMessage(LockSystem.access.getMessage("chest.improveSuccess") + " Aktualny poziom: " + newChestLevel);

                        event.setCancelled(true);
                        return;
                    }


                    API.updateSQL("INSERT INTO locked_chests_list(loc_x, loc_y, loc_z, owner_id, level) " +
                            "values (" + loc_x + ", " + loc_y + ", " + loc_z + ", " + seggelinPlayer.id + ", " + newChestLevel + ");");

                    player.sendMessage(LockSystem.access.getMessage("chest.createSuccess"));

                    event.setCancelled(true);
                    return;
                }
            }

            if (isChestLocked) {
                if (itemInHand.equals(Material.AIR)) {
                    //TODO: Change it. This may cause some spam in the chat
                    if (playerIsOwner) {
                        player.sendMessage(LockSystem.access.getMessage("chest.levelInfo") + ChatColor.GREEN + chestLevel);
                        player.sendMessage(LockSystem.access.getMessage("chest.levelTip"));

                        return;
                    }

                    player.sendMessage(LockSystem.access.getMessage("chest.ownerInfo") + ChatColor.RED + ownerName);
                    player.sendMessage(LockSystem.access.getMessage("chest.padlockInfo"));

                    event.setCancelled(true);
                    return;
                }

                List<String> picklocks = config.getStringList("picklocks");
                for (String picklock : picklocks) {
                    picklockLevel += 1;

                    if (itemInHand.toString().equalsIgnoreCase(picklock)) {
                        if (!playerIsOwner) {
                            Random random = new Random();

                            //TODO: Change it
                            int chanceToSuccess = 10 - 50 * (chestLevel - picklockLevel); //+ player lockpicking level
                            if (random.nextInt(100) < chanceToSuccess) {
                                player.sendMessage(LockSystem.access.getMessage("chest.breakSuccess"));

                                return;
                            }

                            player.sendMessage(LockSystem.access.getMessage("chest.breakFail"));

                            event.setCancelled(true);
                            return;
                        }
                    }
                }
            }
        }
    }
}
