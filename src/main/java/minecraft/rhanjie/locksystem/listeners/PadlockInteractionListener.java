package minecraft.rhanjie.locksystem.listeners;

import minecraft.rhanjie.locksystem.LockSystem;
import minecraft.throk.api.API;
import minecraft.throk.api.SeggelinPlayer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
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

public class PadlockInteractionListener implements Listener {
    @EventHandler(priority = EventPriority.LOW)
    public void onPadlockInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getClickedBlock() == null)
            return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            FileConfiguration config = LockSystem.access.getConfig();

            List<String> lockableBlocks = config.getStringList("lockableBlocks");
            for (String lockableBlock : lockableBlocks) {
                Block block = event.getClickedBlock();

                if (block.getType().toString().equalsIgnoreCase(lockableBlock)) {
                    String uuid = player.getUniqueId().toString();
                    API.playerList.put(uuid, new SeggelinPlayer(player.getName(), uuid));

                    SeggelinPlayer seggelinPlayer = API.playerList.get(uuid);
                    Material itemInHand = player.getInventory().getItemInMainHand().getType();

                    String conditionWhere = LockSystem.access.getStandardConditionWhere(block.getLocation());
                    ResultSet result = API.selectSQL("SELECT player_list.uuid, player_list.name, level FROM locked_objects_list " +
                            "INNER JOIN player_list ON locked_objects_list.owner_id = player_list.id WHERE " + conditionWhere);

                    boolean isLocked = false;
                    boolean playerIsOwner = false;
                    String ownerName = player.getName();

                    int currentPadlockLevel = 1;
                    int newPadlockLevel = 0;
                    int picklockLevel = 0;

                    try {
                        isLocked = result.next();

                        if (isLocked) {
                            String uniqueID = player.getUniqueId().toString();
                            playerIsOwner = uniqueID.equals(result.getString(1));

                            ownerName = result.getString(2);
                            currentPadlockLevel = result.getInt(3);
                        }
                    } catch (SQLException exception) {
                        exception.printStackTrace();
                    }

                    List<String> padlocks = config.getStringList("padlocks");
                    for (String padlock : padlocks) {
                        newPadlockLevel += 1;

                        if (itemInHand.toString().equalsIgnoreCase(padlock)) {
                            if (isLocked) {
                                if (!playerIsOwner) {
                                    player.sendMessage(LockSystem.access.getMessage("lockable.notOwner"));

                                    event.setCancelled(true);
                                    return;
                                }

                                if (currentPadlockLevel >= newPadlockLevel) {
                                    player.sendMessage(LockSystem.access.getMessage("lockable.improveFail"));

                                    event.setCancelled(true);
                                    return;
                                }

                                API.updateSQL("UPDATE locked_objects_list SET level = " + newPadlockLevel + " WHERE " + conditionWhere);

                                int currentAmount = player.getInventory().getItemInMainHand().getAmount();
                                player.getInventory().getItemInMainHand().setAmount(currentAmount - 1);
                                player.sendMessage(LockSystem.access.getMessage("lockable.improveSuccess") + " Aktualny poziom: " + newPadlockLevel);

                                event.setCancelled(true);
                                return;
                            }

                            //TODO: Add support for double blocks like chests or doors
                            API.updateSQL("INSERT INTO locked_objects_list(loc_x, loc_y, loc_z, type, owner_id, level, created_at, is_destroyed) values (" +
                                    loc_x + ", " + loc_y + ", " + loc_z + ", '" + block.getType().toString() + "', " + seggelinPlayer.id + ", " + newPadlockLevel + ", now(), 0);");

                            int currentAmount = player.getInventory().getItemInMainHand().getAmount();
                            player.getInventory().getItemInMainHand().setAmount(currentAmount - 1);
                            player.sendMessage(LockSystem.access.getMessage("lockable.createSuccess"));

                            event.setCancelled(true);
                            return;
                        }
                    }

                    if (isLocked) {
                        List<String> picklocks = config.getStringList("picklocks");
                        for (String picklock : picklocks) {
                            picklockLevel += 1;

                            if (itemInHand.toString().equalsIgnoreCase(picklock)) {
                                if (!playerIsOwner) {
                                    Random random = new Random();

                                    //TODO: Change it
                                    int chanceToSuccess = 10 - 50 * (currentPadlockLevel - picklockLevel); //+ player lockpicking level
                                    if (random.nextInt(100) < chanceToSuccess) {
                                        player.sendMessage(LockSystem.access.getMessage("lockable.breakSuccess"));

                                        return;
                                    }

                                    player.sendMessage(LockSystem.access.getMessage("lockable.breakFail"));
                                    //TODO: Add information for [W]

                                    event.setCancelled(true);
                                    return;
                                }
                            }
                        }

                        if (playerIsOwner) {
                            if (itemInHand == Material.BOOK) {
                                player.sendMessage(LockSystem.access.getMessage("lockable.levelInfo") + ChatColor.GREEN + currentPadlockLevel);
                                player.sendMessage(LockSystem.access.getMessage("lockable.levelTip"));
                            }

                            return;
                        }

                        player.sendMessage(LockSystem.access.getMessage("lockable.ownerInfo") + ChatColor.RED + ownerName);
                        player.sendMessage(LockSystem.access.getMessage("lockable.padlockInfo"));

                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }
}
