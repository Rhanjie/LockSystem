package minecraft.rhanjie.locksystem.commands;

import minecraft.rhanjie.locksystem.LockSystem;
import minecraft.throk.api.API;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class PluginCommand implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("info"))
                return infoCommand(sender);

            if (args[0].equalsIgnoreCase("reload"))
                return reloadCommand(sender);

            if (args[0].equalsIgnoreCase("usun_klodke"))
                return removePadlockCommand(sender);

            if (args[0].equalsIgnoreCase("dodaj"))
                return addMember(sender, args);

            if (args[0].equalsIgnoreCase("usun"))
                return removeMember(sender, args);

            if (args[0].equalsIgnoreCase("dodaj_gildie"))
                return addGuild(sender, args);

            if (args[0].equalsIgnoreCase("usun_gildie"))
                return removeGuild(sender, args);
        }

        return false;
    }

    private boolean infoCommand(CommandSender sender) {
        if (!(sender instanceof Player))
            return false;

        Player player = (Player) sender;
        Block block = player.getTargetBlock(null, 100);

        String conditionWhere = (LockSystem.access).getAutomaticConditionWhere(block);
        ResultSet result = API.selectSQL("SELECT locked_objects_list.id, player_list.uuid, level FROM locked_objects_list " +
                "INNER JOIN player_list ON locked_objects_list.owner_id = player_list.id WHERE " + conditionWhere);

        boolean isLocked = false;
        boolean playerIsOwner = false;
        int currentPadlockLevel = 0;
        String ownerName = player.getName();

        try {
            isLocked = result.next();

            if (!isLocked)
                return false;

            String playerUuid = player.getUniqueId().toString();
            UUID ownerUuid = UUID.fromString(result.getString(2));

            playerIsOwner = playerUuid.equals(ownerUuid.toString());
            ownerName = Bukkit.getOfflinePlayer(ownerUuid).getName();
            currentPadlockLevel = result.getInt(3);
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }

        if (playerIsOwner) {
            player.sendMessage(LockSystem.access.getMessage("lockable.levelInfo") + ChatColor.GREEN + currentPadlockLevel);
            player.sendMessage(LockSystem.access.getMessage("lockable.levelTip"));

            return true;
        }

        player.sendMessage(LockSystem.access.getMessage("lockable.ownerInfo") + ChatColor.RED + ownerName);
        player.sendMessage(LockSystem.access.getMessage("lockable.padlockInfo"));

        return true;
    }

    private boolean reloadCommand(CommandSender sender) {
        if (!sender.hasPermission("LockSystem.Reload"))
            return false;

        LockSystem.access.reloadConfig();
        return true;
    }

    private boolean removePadlockCommand(CommandSender sender) {
        return true;
    }

    private boolean addMember(CommandSender sender, String[] args) {
        return true;
    }

    private boolean removeMember(CommandSender sender, String[] args) {
        return true;
    }

    private boolean addGuild(CommandSender sender, String[] args) {
        return true;
    }

    private boolean removeGuild(CommandSender sender, String[] args) {
        return true;
    }
}