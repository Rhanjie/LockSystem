package minecraft.rhanjie.locksystem.listeners;

import minecraft.rhanjie.locksystem.LockSystem;
import minecraft.throk.api.API;
import minecraft.throk.api.SeggelinPlayer;
import org.bukkit.ChatColor;
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
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class PadlockInteractionListener implements Listener {
    @EventHandler(priority = EventPriority.LOW)
    public void onPadlockInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || block == null)
            return;

        FileConfiguration config = LockSystem.access.getConfig();
        if ((LockSystem.access).checkIfElementIsAvailable(config, block.getType().toString(), "lockableBlocks") == -1)
            return;

        String uuid = player.getUniqueId().toString();
        API.playerList.put(uuid, new SeggelinPlayer(player.getName(), uuid));

        Material itemInHand = player.getInventory().getItemInMainHand().getType();

        String conditionWhere = (LockSystem.access).getAutomaticConditionWhere(block);
        ResultSet result = API.selectSQL("SELECT locked_objects_list.id, player_list.uuid, player_list.name, level FROM locked_objects_list " +
                "INNER JOIN player_list ON locked_objects_list.owner_id = player_list.id WHERE " + conditionWhere);

        boolean isLocked = false;
        boolean playerIsOwner = false;
        String ownerName = player.getName();

        int recordId = -1;
        int currentPadlockLevel = 1;
        int newPadlockLevel = 0;
        int picklockLevel = 0;

        try {
            isLocked = result.next();

            if (isLocked) {
                recordId = result.getInt(1);

                String uniqueID = player.getUniqueId().toString();
                playerIsOwner = uniqueID.equals(result.getString(2));

                ownerName = result.getString(3);
                currentPadlockLevel = result.getInt(4);
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        newPadlockLevel = (LockSystem.access).checkIfElementIsAvailable(config, itemInHand.toString(), "padlocks");
        if (newPadlockLevel != -1) {
            this.createPadlock(event, player, recordId, isLocked, playerIsOwner, currentPadlockLevel, newPadlockLevel);

            return;
        }

        if (!isLocked)
            return;

        picklockLevel = (LockSystem.access).checkIfElementIsAvailable(config, itemInHand.toString(), "picklocks");
        if (picklockLevel != -1) {
            if (!playerIsOwner) {
                this.tryBreakPadlock(event, player, recordId, currentPadlockLevel, picklockLevel);

                return;
            }
        }

        this.displayPadlockInfo(event, player, itemInHand, playerIsOwner, ownerName, currentPadlockLevel);
    }

    private void createPadlock(PlayerInteractEvent event, Player player, int recordId, boolean isLocked, boolean playerIsOwner, int currentPadlockLevel, int newPadlockLevel) {
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

            API.updateSQL("UPDATE locked_objects_list SET level = " + newPadlockLevel + " WHERE id = " + recordId);

            int currentAmount = player.getInventory().getItemInMainHand().getAmount();
            player.getInventory().getItemInMainHand().setAmount(currentAmount - 1);
            player.sendMessage(LockSystem.access.getMessage("lockable.improveSuccess") + " Aktualny poziom: " + newPadlockLevel);

            event.setCancelled(true);
            return;
        }

        SeggelinPlayer seggelinPlayer = API.playerList.get(player.getUniqueId().toString());

        Block block = event.getClickedBlock();
        int locX = block.getLocation().getBlockX();
        int locY = block.getLocation().getBlockY();
        int locZ = block.getLocation().getBlockZ();

        API.updateSQL("INSERT INTO locked_objects_list(loc_x, loc_y, loc_z, type, owner_id, level, created_at) values (" +
                locX + ", " + locY + ", " + locZ + ", '" + block.getType().toString() + "', " + seggelinPlayer.id + ", " + newPadlockLevel + ", now());");

        int currentAmount = player.getInventory().getItemInMainHand().getAmount();
        player.getInventory().getItemInMainHand().setAmount(currentAmount - 1);
        player.sendMessage(LockSystem.access.getMessage("lockable.createSuccess"));

        event.setCancelled(true);
    }

    private void tryBreakPadlock(PlayerInteractEvent event, Player player, int recordId, int currentPadlockLevel, int picklockLevel) {
        Random random = new Random();

        if (event.getClickedBlock().getBlockData() instanceof Door) {
            Door door = (Door) event.getClickedBlock().getBlockData();

            if (door.isOpen())
                return;
        }

        //TODO: Wait for player skills system
        int playerLockpickingLevel = random.nextInt(10);
        int chanceToSuccess = 10 + (playerLockpickingLevel * 10) - 50 * (currentPadlockLevel - 1);
        if (random.nextInt(100) < chanceToSuccess) {
            //TODO: Add break protection
            Date datetime = new java.util.Date();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            String currentTime = format.format(datetime);
            //API.updateSQL("UPDATE locked_objects_list SET break_protection_time = " + currentTime + " WHERE id = " + recordId);

            player.sendMessage(LockSystem.access.getMessage("lockable.breakSuccess"));

            return;
        }

        API.updateSQL("UPDATE locked_objects_list SET last_break_attempt = now() WHERE id = " + recordId);
        //TODO: Add information for [W]

        player.sendMessage(LockSystem.access.getMessage("lockable.breakFail"));
        event.setCancelled(true);
    }

    private void displayPadlockInfo(PlayerInteractEvent event, Player player, Material itemInHand, boolean playerIsOwner, String ownerName, int currentPadlockLevel) {
        if (playerIsOwner) {
            if (itemInHand == Material.BOOK) {
                player.sendMessage(LockSystem.access.getMessage("lockable.levelInfo") + ChatColor.GREEN + currentPadlockLevel);
                player.sendMessage(LockSystem.access.getMessage("lockable.levelTip"));

                event.setCancelled(true);
            }

            return;
        }

        player.sendMessage(LockSystem.access.getMessage("lockable.ownerInfo") + ChatColor.RED + ownerName);
        player.sendMessage(LockSystem.access.getMessage("lockable.padlockInfo"));

        event.setCancelled(true);
    }
}
