package minecraft.rhanjie.locksystem.commands;

import minecraft.rhanjie.locksystem.LockSystem;
import minecraft.throk.api.API;
import minecraft.throk.api.SeggelinPlayer;
import minecraft.throk.api.database.UpdateQuery;
import minecraft.throk.api.database.repositories.PlayerRepository;
import minecraft.throk.api.exceptions.EntityNotFound;
import minecraft.throk.api.exceptions.RepositoryRequired;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.management.Query;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PluginCommand implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("pomoc"))
            return helpCommand(sender);

        if (args[0].equalsIgnoreCase("info"))
            return infoCommand(sender);

        if (args[0].equalsIgnoreCase("reload"))
            return reloadCommand(sender);

        if (args[0].equalsIgnoreCase("usun"))
            return removePadlockCommand(sender);

        if (args[0].equalsIgnoreCase("dodaj_czlonka"))
            return addMember(sender, args);

        if (args[0].equalsIgnoreCase("usun_czlonka"))
            return removeMember(sender, args);

        if (args[0].equalsIgnoreCase("dodaj_gildie"))
            return addGuild(sender, args);

        if (args[0].equalsIgnoreCase("usun_gildie"))
            return removeGuild(sender, args);

        sender.sendMessage(ChatColor.RED + "Nierozpoznana komenda!\n" +
                "Uzyj " + ChatColor.GREEN + "/klodka pomoc" + ChatColor.RED + ", aby uzyskac liste komend!");
        return false;
    }

    private boolean helpCommand(CommandSender sender) {
        String helpInfo = "--- [LISTA KOMEND] ---\n";
        helpInfo += ChatColor.GREEN + "/klodka usun\n";
        helpInfo += ChatColor.GREEN + "/klodka dodaj_czlonka <nick_1> <nick_n>\n";
        helpInfo += ChatColor.GREEN + "/klodka usun_czlonka <nick_1> <nick_n>\n";
        helpInfo += ChatColor.RED + "/klodka dodaj_gildie <gildia_1> <gildia_n>\n";
        helpInfo += ChatColor.RED + "/klodka usun_gildie <gildia_1> <gildia_n>\n";

        sender.sendMessage(helpInfo);
        return true;
    }

    private boolean infoCommand(CommandSender sender) {
        if (!(sender instanceof Player))
            return false;

        Player player = (Player) sender;
        Block block = player.getTargetBlock(null, 10);

        FileConfiguration config = LockSystem.access.getConfig();
        if ((LockSystem.access).checkIfElementIsAvailable(config, block.getType().toString(), "lockableBlocks") == -1)
            return false;

        String conditionWhere = (LockSystem.access).getAutomaticConditionWhere(block);
        ResultSet result = LockSystem.access.getInfoFromDatabase(player,
                "SELECT locked_objects_list.id, player_list.uuid, level FROM locked_objects_list " +
                 "INNER JOIN player_list ON locked_objects_list.owner_id = player_list.id WHERE " + conditionWhere);

        if (result == null)
            return false;

        int recordId = 0;
        boolean isLocked = false;
        boolean playerIsOwner = false;
        int currentPadlockLevel = 0;
        String ownerName = player.getName();

        try {
            isLocked = result.next();

            if (!isLocked)
                return false;

            recordId = result.getInt(1);

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
            ArrayList<String> members = new ArrayList<String>();

            ResultSet membersResult = LockSystem.access.getInfoFromDatabase(player,
                    "SELECT uuid FROM locked_objects_members_list WHERE locked_object_id = " + recordId);

            if (membersResult == null)
                return false;

            try {
                PlayerRepository playerRepository = (PlayerRepository) API.getRepository(PlayerRepository.class);
                SeggelinPlayer seggelinPlayer;

                while (membersResult.next()) {
                    seggelinPlayer = playerRepository.getPlayerByUid(membersResult.getString(1));
                    members.add(seggelinPlayer.name);
                }
            }

            catch(SQLException | RepositoryRequired | EntityNotFound exception) {
                exception.printStackTrace();
            }

            String message = "";
            message += ChatColor.GREEN + "Właściciel: " + ChatColor.GOLD + "Ty\n";
            message += ChatColor.GREEN + LockSystem.access.getMessage("lockable.levelInfo") + ChatColor.GOLD + currentPadlockLevel + "\n";
            message += ChatColor.RESET + "Osoby majace dostep:\n" + ChatColor.GOLD;
            for (String memberName : members) {
                message += "- " + memberName + "\n";
            }

            player.sendMessage(message);
            return true;
        }

        player.sendMessage(LockSystem.access.getMessage("lockable.ownerInfo") + ChatColor.GOLD + ownerName);
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
        if (!(sender instanceof Player))
            return false;

        Player player = (Player) sender;
        Block block = player.getTargetBlock(null, 10);

        FileConfiguration config = LockSystem.access.getConfig();
        if ((LockSystem.access).checkIfElementIsAvailable(config, block.getType().toString(), "lockableBlocks") == -1)
            return false;

        String conditionWhere = (LockSystem.access).getAutomaticConditionWhere(block);
        ResultSet result = LockSystem.access.getInfoFromDatabase(player,
                "SELECT player_list.uuid, level FROM locked_objects_list " +
                 "INNER JOIN player_list ON locked_objects_list.owner_id = player_list.id WHERE " + conditionWhere);

        if (result == null)
            return false;

        boolean isLocked = false;
        boolean playerIsOwner = false;
        int padlockLevel = 0;

        try {
            isLocked = result.next();

            if (!isLocked)
                return false;

            String playerUuid = player.getUniqueId().toString();
            UUID ownerUuid = UUID.fromString(result.getString(1));

            playerIsOwner = playerUuid.equals(ownerUuid.toString());
            padlockLevel = result.getInt(2);
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }

        if (playerIsOwner) {
            try {
                UpdateQuery query = (UpdateQuery) API.getDatabase().updateQuery();
                PreparedStatement statement = query.setQuery(
                        "UPDATE locked_objects_list SET destroyed_at = now(), " +
                        "destroy_guilty = '" + player.getName() + "', destroy_reason = 'Usuniecie klodki' WHERE " + conditionWhere);

                statement.setString(1, player.getName());
                query.execute();
            }

            catch (SQLException exception) {
                player.sendMessage(ChatColor.RED + "Cos poszlo nie tak! Zglos to krolowi");
                exception.printStackTrace();

                return false;
            }


            List<String> padlocks = config.getStringList("padlocks");
            String padlockName = padlocks.get(padlockLevel - 1).toUpperCase();

            Material padlockMaterial = Material.getMaterial(padlockName);
            if (padlockMaterial != null)
                player.getInventory().addItem(new ItemStack(padlockMaterial));

            player.sendMessage(LockSystem.access.getMessage("lockable.removeSuccess"));
            return true;
        }

        player.sendMessage(LockSystem.access.getMessage("lockable.notOwner"));
        return false;
    }

    private boolean addMember(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Musisz byc graczem, aby uzyc tej komendy!");

            return false;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Podaj nicki dodawanych czlonkow!");
            return false;
        }

        Player player = (Player) sender;
        Block block = player.getTargetBlock(null, 10);

        if (block.getType() == Material.AIR)
            return false;

        FileConfiguration config = LockSystem.access.getConfig();
        if ((LockSystem.access).checkIfElementIsAvailable(config, block.getType().toString(), "lockableBlocks") == -1)
            return false;

        String conditionWhere = (LockSystem.access).getAutomaticConditionWhere(block);
        ResultSet result = LockSystem.access.getInfoFromDatabase(player,
                "SELECT locked_objects_list.id, player_list.uuid, level FROM locked_objects_list " +
                 "INNER JOIN player_list ON locked_objects_list.owner_id = player_list.id WHERE " + conditionWhere);

        if (result == null)
            return false;

        boolean isLocked = false;
        boolean playerIsOwner = false;
        int padlockId = 0;

        try {
            isLocked = result.next();

            if (!isLocked)
                return false;

            padlockId = result.getInt(1);

            String playerUuid = player.getUniqueId().toString();
            UUID ownerUuid = UUID.fromString(result.getString(2));

            playerIsOwner = playerUuid.equals(ownerUuid.toString());
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }

        if (!playerIsOwner) {
            player.sendMessage(LockSystem.access.getMessage("lockable.notOwner"));

            return false;
        }


        PlayerRepository playerRepository = null;
        try {
            playerRepository = (PlayerRepository) API.getRepository(PlayerRepository.class);
        }

        catch (RepositoryRequired exception) {
            exception.printStackTrace();
            return false;
        }

        for (int i = 1; i < args.length; i += 1) {
            try {
                SeggelinPlayer seggelinPlayer = playerRepository.getPlayerByName(args[i]);

                ResultSet memberResult = LockSystem.access.getInfoFromDatabase(player,
                        "SELECT id FROM locked_objects_members_list WHERE " +
                         "locked_object_id = " + padlockId + " AND uuid = '" + seggelinPlayer.uuid + "';");

                if (memberResult == null)
                    return false;

                if (memberResult.next()) {
                    player.sendMessage(ChatColor.RED + "Gracz " + ChatColor.RESET + args[i] + ChatColor.RED + " był już dodany do kłódki!");

                    continue;
                }

                UpdateQuery query = (UpdateQuery) API.getDatabase().updateQuery();

                PreparedStatement statement = query.setQuery("INSERT INTO locked_objects_members_list" +
                        " (locked_object_id, uuid, added_at) values (?, ?, now());");
                statement.setInt(1, padlockId);
                statement.setString(2, seggelinPlayer.uuid);

                query.execute();
            }

            catch (EntityNotFound notFoundException) {
                player.sendMessage(ChatColor.RED + "Nie znaleziono gracza " + ChatColor.RESET + args[i] + ChatColor.RED + "!");
            }

            catch (SQLException exception) {
                exception.printStackTrace();
            }
        }

        player.sendMessage(ChatColor.GREEN + "Klodka zaaktualizowana!");
        return true;
    }

    private boolean removeMember(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Musisz byc graczem, aby uzyc tej komendy!");

            return false;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Podaj nicki usuwanych czlonkow!");
            return false;
        }

        Player player = (Player) sender;
        Block block = player.getTargetBlock(null, 10);

        if (block.getType() == Material.AIR)
            return false;

        FileConfiguration config = LockSystem.access.getConfig();
        if ((LockSystem.access).checkIfElementIsAvailable(config, block.getType().toString(), "lockableBlocks") == -1)
            return false;

        String conditionWhere = (LockSystem.access).getAutomaticConditionWhere(block);
        ResultSet result = LockSystem.access.getInfoFromDatabase(player,
                "SELECT locked_objects_list.id, player_list.uuid, level FROM locked_objects_list " +
                 "INNER JOIN player_list ON locked_objects_list.owner_id = player_list.id WHERE " + conditionWhere);

        if (result == null)
            return false;

        boolean playerIsOwner = false;
        int padlockId = 0;

        try {
            if (!result.next())
                return false;

            padlockId = result.getInt(1);

            String playerUuid = player.getUniqueId().toString();
            UUID ownerUuid = UUID.fromString(result.getString(2));

            playerIsOwner = playerUuid.equals(ownerUuid.toString());
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }

        if (!playerIsOwner) {
            player.sendMessage(LockSystem.access.getMessage("lockable.notOwner"));

            return false;
        }

        PlayerRepository playerRepository;
        try {
            playerRepository = (PlayerRepository) API.getRepository(PlayerRepository.class);
        }

        catch (RepositoryRequired exception) {
            exception.printStackTrace();
            return false;
        }

        StringBuilder builder = new StringBuilder("locked_object_id = " + padlockId);
        for (int i = 1; i < args.length; i += 1) {
            try {
                SeggelinPlayer seggelinPlayer = playerRepository.getPlayerByName(args[i]);

                if (i == 1)
                    builder.append(" AND (uuid = '");
                else builder.append(" OR uuid = '");

                builder.append(seggelinPlayer.uuid);
                builder.append("'");
            }

            catch (EntityNotFound exception) {
                player.sendMessage(ChatColor.RED + "Nie znaleziono gracza " + ChatColor.RESET + args[i] + ChatColor.RED + "!");
            }

            catch (SQLException exception) {
                exception.printStackTrace();
            }
        }
        builder.append(");");

        try {
            UpdateQuery query = (UpdateQuery) API.getDatabase().updateQuery();
            query.setQuery("DELETE FROM locked_objects_members_list WHERE " + builder.toString());

            if (!query.execute()) {
                player.sendMessage(ChatColor.RED + "Blad podczas aktualizowania klodki!");

                return false;
            }

            player.sendMessage(ChatColor.GREEN + "Klodka zaaktualizowana!");
        }

        catch(SQLException exception) {
            player.sendMessage(ChatColor.RED + "Blad podczas aktualizowania klodki!");
            exception.printStackTrace();

            return false;
        }

        return true;
    }

    private boolean addGuild(CommandSender sender, String[] args) {
        return true;
    }

    private boolean removeGuild(CommandSender sender, String[] args) {
        return true;
    }
}