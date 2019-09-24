package minecraft.rhanjie.locksystem.listeners;

import minecraft.rhanjie.locksystem.LockSystem;
import minecraft.throk.api.API;
import minecraft.throk.api.SeggelinPlayer;
import minecraft.throk.api.database.UpdateQuery;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Door;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

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
        ResultSet result = LockSystem.access.getInfoFromDatabase(player,
                "SELECT locked_objects_list.id, player_list.uuid, player_list.name, level FROM locked_objects_list " +
                 "INNER JOIN player_list ON locked_objects_list.owner_id = player_list.id WHERE " + conditionWhere);

        if (result == null)
            return;

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

            try {
                UpdateQuery query = (UpdateQuery) API.getDatabase().updateQuery();
                PreparedStatement statement = query.setQuery("UPDATE locked_objects_list SET level = ? WHERE id = ?");

                statement.setInt(1, newPadlockLevel);
                statement.setInt(2, recordId);
                query.execute();
            }

            catch (SQLException exception) {
                player.sendMessage(ChatColor.RED + "Cos poszlo nie tak! Zglos to krolowi");
                exception.printStackTrace();

                event.setCancelled(true);
                return;
            }

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

        try {
            UpdateQuery query = (UpdateQuery) API.getDatabase().updateQuery();
            PreparedStatement statement = query.setQuery(
                    "INSERT INTO locked_objects_list(world_uuid, loc_x, loc_y, loc_z, type, owner_id, level, created_at) " +
                    "values (?, ?, ?, ?, ?, ?, ?, now());");

            statement.setString(1, block.getLocation().getWorld().getUID().toString());
            statement.setInt(2, block.getLocation().getBlockX());
            statement.setInt(3, block.getLocation().getBlockY());
            statement.setInt(4, block.getLocation().getBlockZ());
            statement.setString(5, block.getType().toString());
            statement.setInt(6, seggelinPlayer.id);
            statement.setInt(7, newPadlockLevel);
            query.execute();
        }

        catch (SQLException exception) {
            player.sendMessage(ChatColor.RED + "Cos poszlo nie tak! Zglos to krolowi");
            exception.printStackTrace();

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

        try {
            UpdateQuery query = (UpdateQuery) API.getDatabase().updateQuery();
            PreparedStatement statement = query.setQuery(
                    "UPDATE locked_objects_list SET last_break_attempt = now() WHERE id = ?;");

            //TODO: Add information for [W]
            statement.setInt(1, recordId);
            query.execute();
        }

        catch (SQLException exception) {
            player.sendMessage(ChatColor.RED + "Cos poszlo nie tak! Zglos to krolowi");
            exception.printStackTrace();

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

        ResultSet result = LockSystem.access.getInfoFromDatabase(player,
                "SELECT uuid FROM locked_objects_members_list WHERE locked_object_id = " + recordId);

        boolean padlockMember = false;

        try {
            while (result.next()) {
                UUID memberUuid = UUID.fromString(result.getString(1));
                members.add(memberUuid);

                if (player.getUniqueId().equals(memberUuid))
                    padlockMember = true;
            }
        } catch(SQLException exception) {
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
