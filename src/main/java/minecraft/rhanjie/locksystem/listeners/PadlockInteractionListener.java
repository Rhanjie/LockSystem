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
        Block block = event.getClickedBlock();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || block == null)
            return;

        FileConfiguration config = LockSystem.access.getConfig();

        if (!(LockSystem.access).checkIfElementIsAvailable(config, block.getType().toString(), "lockableBlocks"))
            return;

        String uuid = player.getUniqueId().toString();
        API.playerList.put(uuid, new SeggelinPlayer(player.getName(), uuid));

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

        if ((LockSystem.access).checkIfElementIsAvailable(config, itemInHand.toString(), "padlocks")) {
            this.createPadlock(event, player, isLocked, playerIsOwner, currentPadlockLevel, newPadlockLevel, conditionWhere);

            return;
        }

        if (!isLocked)
            return;

        if ((LockSystem.access).checkIfElementIsAvailable(config, itemInHand.toString(), "picklocks")) {
            if (!playerIsOwner) {
                this.tryBreakPadlock(event, player, currentPadlockLevel, picklockLevel);

                return;
            }
        }

        this.displayPadlockInfo(event, player, itemInHand, playerIsOwner, ownerName, currentPadlockLevel);
    }

    private void createPadlock(PlayerInteractEvent event, Player player, boolean isLocked, boolean playerIsOwner, int currentPadlockLevel, int newPadlockLevel, String conditionWhere) {
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

        SeggelinPlayer seggelinPlayer = API.playerList.get(player.getUniqueId().toString());

        Block block = event.getClickedBlock();
        int loc_x = block.getLocation().getBlockX();
        int loc_y = block.getLocation().getBlockY();
        int loc_z = block.getLocation().getBlockZ();

        //TODO: Add support for double blocks like chests or doors
        API.updateSQL("INSERT INTO locked_objects_list(loc_x, loc_y, loc_z, type, owner_id, level, created_at) values (" +
                loc_x + ", " + loc_y + ", " + loc_z + ", '" + block.getType().toString() + "', " + seggelinPlayer.id + ", " + newPadlockLevel + ", now());");

        int currentAmount = player.getInventory().getItemInMainHand().getAmount();
        player.getInventory().getItemInMainHand().setAmount(currentAmount - 1);
        player.sendMessage(LockSystem.access.getMessage("lockable.createSuccess"));

        event.setCancelled(true);
    }

    private void tryBreakPadlock(PlayerInteractEvent event, Player player, int currentPadlockLevel, int picklockLevel) {
        Random random = new Random();

        //TODO: Wait for player skills system
        int playerLockpickingLevel = 12;
        int chanceToSuccess = 10 - 50 * (currentPadlockLevel - picklockLevel); //+ player lockpicking level
        if (random.nextInt(100) < chanceToSuccess) {
            player.sendMessage(LockSystem.access.getMessage("lockable.breakSuccess"));

            return;
        }

        String conditionWhere = LockSystem.access.getStandardConditionWhere(event.getClickedBlock().getLocation());
        API.updateSQL("UPDATE locked_objects_list SET last_break_attempt = now() WHERE " + conditionWhere);
        //TODO: Add information for [W]

        player.sendMessage(LockSystem.access.getMessage("lockable.breakFail"));
        event.setCancelled(true);
    }

    private void displayPadlockInfo(PlayerInteractEvent event, Player player, Material itemInHand, boolean playerIsOwner, String ownerName, int currentPadlockLevel) {
        if (playerIsOwner && itemInHand == Material.BOOK) {
            player.sendMessage(LockSystem.access.getMessage("lockable.levelInfo") + ChatColor.GREEN + currentPadlockLevel);
            player.sendMessage(LockSystem.access.getMessage("lockable.levelTip"));

            return;
        }

        player.sendMessage(LockSystem.access.getMessage("lockable.ownerInfo") + ChatColor.RED + ownerName);
        player.sendMessage(LockSystem.access.getMessage("lockable.padlockInfo"));

        event.setCancelled(true);
    }
}
