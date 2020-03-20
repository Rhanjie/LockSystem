package minecraft.rhanjie.locksystem.listeners;

import minecraft.rhanjie.locksystem.LockSystem;
import minecraft.rhanjie.locksystem.database.InsertLockedObjectQuery;
import minecraft.rhanjie.locksystem.database.SelectLockedObjectsQuery;
import minecraft.rhanjie.locksystem.database.SelectMembersQuery;
import minecraft.rhanjie.locksystem.database.UpdateLocketObjectsQuery;
import minecraft.rhanjie.locksystem.utility.Utility;
import minecraft.throk.api.database.Database;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
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
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

public class PadlockInteractionListener implements Listener {
    private Database database;

    public PadlockInteractionListener(Database database) {
        this.database = database;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPadlockInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || block == null)
            return;

        FileConfiguration config = LockSystem.access.getConfig();
        if (Utility.checkIfElementIsAvailable(config, block.getType().toString(), "lockableBlocks") == -1)
            return;

        Material itemInHand = player.getInventory().getItemInMainHand().getType();

        SelectLockedObjectsQuery selectLockedObjectsQuery = new SelectLockedObjectsQuery(database);
        ResultSet result = selectLockedObjectsQuery.getLockedObjectWithOwner(block);

        boolean isLocked = false;
        boolean playerIsOwner = false;
        boolean padlockProtection = false;
        String ownerName = player.getName();

        int recordId = -1;
        int currentPadlockLevel = 1;
        int newPadlockLevel = 0;
        int picklockLevel = 0;

        try {
            isLocked = result.next();

            if (isLocked) {
                recordId = result.getInt("locked_objects_list.id");

                String uniqueID = player.getUniqueId().toString();
                playerIsOwner = uniqueID.equals(result.getString("player_list.uuid"));

                ownerName = result.getString("player_list.name");
                currentPadlockLevel = result.getInt("level");

                if (result.getTimestamp("break_protection_time") != null)
                    padlockProtection = result.getTimestamp("break_protection_time").toInstant().isAfter(Instant.now());
            }
        }

        catch (SQLException exception) {
            exception.printStackTrace();
        }

        newPadlockLevel = Utility.checkIfElementIsAvailable(config, itemInHand.toString(), "padlocks");
        if (newPadlockLevel != -1) {
            this.createPadlock(event, player, recordId, isLocked, playerIsOwner, currentPadlockLevel, newPadlockLevel);

            return;
        }

        if (!isLocked)
            return;

        picklockLevel = Utility.checkIfElementIsAvailable(config, itemInHand.toString(), "picklocks");
        if (picklockLevel != -1) {
            if (!playerIsOwner) {
                if (padlockProtection) {
                    player.sendMessage(ChatColor.RED + "Kłódka została niedawno zniszczona! Spróbuj ponownie później");

                    event.setCancelled(true);
                    return;
                }

                this.tryBreakPadlock(event, player, recordId, currentPadlockLevel, picklockLevel);
                return;
            }
        }

        this.displayPadlockInfo(event, player, itemInHand, playerIsOwner, ownerName, recordId, currentPadlockLevel);
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

            Block block = event.getClickedBlock();

            UpdateLocketObjectsQuery updateLocketObjectsQuery = new UpdateLocketObjectsQuery(database);
            if (!updateLocketObjectsQuery.updateLevel(block, newPadlockLevel)) {
                player.sendMessage(LockSystem.access.getMessage("lockable.criticalError"));

                event.setCancelled(true);
                return;
            }

            int currentAmount = player.getInventory().getItemInMainHand().getAmount();
            player.getInventory().getItemInMainHand().setAmount(currentAmount - 1);
            player.sendMessage(LockSystem.access.getMessage("lockable.improveSuccess") + " Aktualny poziom: " + newPadlockLevel);

            event.setCancelled(true);
            return;
        }

        Block block = event.getClickedBlock();

        InsertLockedObjectQuery insertLockedObjectQuery = new InsertLockedObjectQuery(database);
        if (!insertLockedObjectQuery.addNewObject(block, player, newPadlockLevel)) {
            player.sendMessage(LockSystem.access.getMessage("lockable.criticalError"));

            event.setCancelled(true);
            return;
        }

        int currentAmount = player.getInventory().getItemInMainHand().getAmount();
        player.getInventory().getItemInMainHand().setAmount(currentAmount - 1);
        player.sendMessage(LockSystem.access.getMessage("lockable.createSuccess"));

        event.setCancelled(true);
    }

    private void tryBreakPadlock(PlayerInteractEvent event, Player player, int recordId, int currentPadlockLevel, int picklockLevel) {
        Random random = new Random();

        BlockData blockData = event.getClickedBlock().getBlockData();
        if (blockData instanceof Door) {
            Door door = (Door) event.getClickedBlock().getBlockData();

            if (door.isOpen())
                return;
        }

        SimpleDateFormat mysqlFriendlyFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 6);
        Date datetime = calendar.getTime();

        String currentTime = mysqlFriendlyFormat.format(datetime);
        Timestamp timestamp = Timestamp.valueOf(currentTime);

        UpdateLocketObjectsQuery updateLocketObjectsQuery = new UpdateLocketObjectsQuery(database);

        //TODO: Wait for player skills system
        int playerLockpickingLevel = random.nextInt(10);
        int chanceToSuccess = 10 + (playerLockpickingLevel * 10) - 50 * (currentPadlockLevel - 1);

        if (random.nextInt(100) < chanceToSuccess) {
            if (!updateLocketObjectsQuery.updateBreakProtectionTime(event.getClickedBlock(), timestamp)) {
                player.sendMessage(LockSystem.access.getMessage("lockable.criticalError"));

                event.setCancelled(true);
                return;
            }

            player.sendMessage(LockSystem.access.getMessage("lockable.breakSuccess"));

            return;
        }

        if (!updateLocketObjectsQuery.updateBreakProtectionTime(event.getClickedBlock(), timestamp)) {
            player.sendMessage(LockSystem.access.getMessage("lockable.criticalError"));

            event.setCancelled(true);
            return;
        }

        int amount = player.getInventory().getItemInMainHand().getAmount();
        player.getInventory().getItemInMainHand().setAmount(amount - 1);

        player.getWorld().playSound(event.getClickedBlock().getLocation(), Sound.BLOCK_ANVIL_USE, 100.0F, 1F);
        player.sendMessage(LockSystem.access.getMessage("lockable.breakFail"));
        event.setCancelled(true);
    }

    private void displayPadlockInfo(PlayerInteractEvent event, Player player, Material itemInHand, boolean playerIsOwner, String ownerName, int recordId, int currentPadlockLevel) {
        ArrayList<UUID> members = new ArrayList<UUID>();

        SelectMembersQuery selectMembersQuery = new SelectMembersQuery(database);
        ResultSet result = selectMembersQuery.getMembersFromLockedObjectId(recordId);

        if (result == null) {
            return;
        }

        boolean padlockMember = false;

        try {
            while (result.next()) {
                String rawUuid = result.getString(1);
                UUID memberUuid = UUID.fromString(rawUuid);
                members.add(memberUuid);

                if (player.getUniqueId().equals(memberUuid)) {
                    padlockMember = true;
                }
            }
        }

        catch(SQLException exception) {
            exception.printStackTrace();
        }

        if (playerIsOwner) {
            if (itemInHand == Material.BOOK) {
                String message = "";
                message += ChatColor.GREEN + LockSystem.access.getMessage("lockable.ownerInfo") + ChatColor.GOLD + "Ty\n";
                message += ChatColor.GREEN + LockSystem.access.getMessage("lockable.levelInfo") + ChatColor.GOLD + currentPadlockLevel + "\n";
                message += ChatColor.RESET + "Osoby majace dostep:\n" + ChatColor.GOLD;

                for (UUID uuid : members) {
                    message += "- " + Bukkit.getOfflinePlayer(uuid).getName() + "\n";
                }

                player.sendMessage(message);
                event.setCancelled(true);
            }

            return;
        }

        if (padlockMember)
            return;

        player.sendMessage(LockSystem.access.getMessage("lockable.ownerInfo") + ChatColor.GOLD + ownerName);
        player.sendMessage(LockSystem.access.getMessage("lockable.padlockInfo"));

        event.setCancelled(true);
    }
}
