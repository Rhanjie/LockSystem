package minecraft.rhanjie.locksystem.commands;

import minecraft.rhanjie.locksystem.LockSystem;
import minecraft.rhanjie.locksystem.database.*;
import minecraft.rhanjie.locksystem.utility.Utility;
import minecraft.throk.api.database.Database;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class PluginCommand implements CommandExecutor {
    private Database database;

    public PluginCommand(Database database) {
        this.database = database;
    }

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
        //helpInfo += ChatColor.RED + "/klodka dodaj_gildie <gildia_1> <gildia_n>\n";
        //helpInfo += ChatColor.RED + "/klodka usun_gildie <gildia_1> <gildia_n>\n";

        sender.sendMessage(helpInfo);
        return true;
    }

    private boolean infoCommand(CommandSender sender) {
        if (!(sender instanceof Player))
            return false;

        Player player = (Player) sender;
        Block block = player.getTargetBlock(null, 10);

        FileConfiguration config = LockSystem.access.getConfig();
        if (Utility.checkIfElementIsAvailable(config, block.getType().toString(), "lockableBlocks") == -1)
            return false;

        SelectLockedObjectsQuery selectLockedObjectsQuery = new SelectLockedObjectsQuery(database);
        ResultSet result = selectLockedObjectsQuery.getLockedObjectWithOwner(block);

        if (result == null) {
            return false;
        }

        int recordId = 0;
        boolean isLocked = false;
        boolean playerIsOwner = false;
        int currentPadlockLevel = 0;
        String ownerName = player.getName();

        try {
            isLocked = result.next();

            if (!isLocked)
                return false;

            recordId = result.getInt("locked_objects_list.id");

            String playerUuid = player.getUniqueId().toString();
            UUID ownerUuid = UUID.fromString(result.getString("player_list.uuid"));

            playerIsOwner = playerUuid.equals(ownerUuid.toString());
            ownerName = Bukkit.getOfflinePlayer(ownerUuid).getName();
            currentPadlockLevel = result.getInt("level");
        }

        catch (SQLException exception) {
            exception.printStackTrace();

            return false;
        }

        if (playerIsOwner) {
            ArrayList<String> members = new ArrayList<String>();

            SelectMembersQuery selectMembersQuery = new SelectMembersQuery(database);
            ResultSet membersResult = selectMembersQuery.getMembersFromLockedObjectId(recordId);

            if (membersResult == null) {
                return false;
            }

            try {
                while (membersResult.next()) {
                    String rawUuid = membersResult.getString(1);
                    UUID uuid = UUID.fromString(rawUuid);

                    members.add(Bukkit.getOfflinePlayer(uuid).getName());
                }
            }

            catch(SQLException exception) {
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
        if (Utility.checkIfElementIsAvailable(config, block.getType().toString(), "lockableBlocks") == -1)
            return false;

        SelectLockedObjectsQuery selectLockedObjectsQuery = new SelectLockedObjectsQuery(database);
        ResultSet result = selectLockedObjectsQuery.getLockedObjectWithOwner(block);

        if (result == null) {
            return false;
        }

        boolean isLocked = false;
        boolean playerIsOwner = false;
        int padlockLevel = 0;

        try {
            isLocked = result.next();

            if (!isLocked)
                return false;

            String playerUuid = player.getUniqueId().toString();
            UUID ownerUuid = UUID.fromString(result.getString("player_list.uuid"));

            playerIsOwner = playerUuid.equals(ownerUuid.toString());
            padlockLevel = result.getInt("level");
        }

        catch (SQLException exception) {
            exception.printStackTrace();

            return false;
        }

        if (playerIsOwner) {
            UpdateLocketObjectsQuery updateLocketObjectsQuery = new UpdateLocketObjectsQuery(database);
            boolean isWell = updateLocketObjectsQuery.destroy(block, player.getName(), "Usunięcie kłódki");

            if (!isWell) {
                player.sendMessage(LockSystem.access.getMessage("lockable.criticalError"));

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

        if (block.getType() == Material.AIR) {
            return false;
        }

        FileConfiguration config = LockSystem.access.getConfig();
        if (Utility.checkIfElementIsAvailable(config, block.getType().toString(), "lockableBlocks") == -1)
            return false;

        SelectLockedObjectsQuery selectLockedObjectsQuery = new SelectLockedObjectsQuery(database);
        ResultSet result = selectLockedObjectsQuery.getLockedObjectWithOwner(block);

        if (result == null) {
            return false;
        }

        boolean isLocked = false;
        boolean playerIsOwner = false;
        int padlockId = 0;

        try {
            isLocked = result.next();

            if (!isLocked) {
                return false;
            }

            padlockId = result.getInt("locked_objects_list.id");

            String playerUuid = player.getUniqueId().toString();
            UUID ownerUuid = UUID.fromString(result.getString("player_list.uuid"));

            playerIsOwner = playerUuid.equals(ownerUuid.toString());
        }

        catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }

        if (!playerIsOwner) {
            player.sendMessage(LockSystem.access.getMessage("lockable.notOwner"));

            return false;
        }

        //database.transaction().start();
        for (int i = 1; i < args.length; i += 1) {
            //TODO: Change it when the whole code will be refactored
            String playerUuid = Bukkit.getOfflinePlayer(args[i]).getUniqueId().toString();

            //TODO: Add cache system
            SelectMembersQuery selectMembersQuery = new SelectMembersQuery(database);
            ResultSet memberResult = selectMembersQuery.checkIfMemberExists(padlockId, playerUuid);

            if (memberResult != null) {
                player.sendMessage(ChatColor.RED + "Gracz " + ChatColor.RESET + args[i] + ChatColor.RED + " był już dodany do kłódki!");

                continue;
            }

            InsertMemberQuery insertMemberQuery = new InsertMemberQuery(database);
            if (!insertMemberQuery.addNewObject(padlockId, playerUuid)) {
                sender.sendMessage(ChatColor.RED + "Nie udało się zaaktualizować kłódki!");

                return false;
            }
        }

        //database.transaction().commit();

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
        if (Utility.checkIfElementIsAvailable(config, block.getType().toString(), "lockableBlocks") == -1)
            return false;

        SelectLockedObjectsQuery selectLockedObjectsQuery = new SelectLockedObjectsQuery(database);
        ResultSet result = selectLockedObjectsQuery.getLockedObjectWithOwner(block);

        if (result == null) {
            return false;
        }

        boolean playerIsOwner = false;
        int padlockId = 0;

        try {
            if (!result.next())
                return false;

            padlockId = result.getInt("locked_objects_list.id");

            String playerUuid = player.getUniqueId().toString();
            UUID ownerUuid = UUID.fromString(result.getString("player_list.uuid"));

            playerIsOwner = playerUuid.equals(ownerUuid.toString());
        }

        catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }

        if (!playerIsOwner) {
            player.sendMessage(LockSystem.access.getMessage("lockable.notOwner"));

            return false;
        }

        UpdateMembersQuery updateMembersQuery = new UpdateMembersQuery(database);
        ArrayList<String> members = new ArrayList<>(Arrays.asList(args).subList(1, args.length));

        if (!updateMembersQuery.remove(padlockId, members)) {
            player.sendMessage(ChatColor.RED + "Blad podczas aktualizowania klodki!");

            return false;
        }

        player.sendMessage(ChatColor.GREEN + "Klodka zaaktualizowana!");
        return true;
    }

    private boolean addGuild(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Musisz byc graczem, aby uzyc tej komendy!");

            return false;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Podaj nazwy dodawanych gildii!");
            return false;
        }

        Player player = (Player) sender;
        Block block = player.getTargetBlock(null, 10);

        if (block.getType() == Material.AIR)
            return false;

        FileConfiguration config = LockSystem.access.getConfig();
        if (Utility.checkIfElementIsAvailable(config, block.getType().toString(), "lockableBlocks") == -1)
            return false;

        SelectLockedObjectsQuery selectLockedObjectsQuery = new SelectLockedObjectsQuery(database);
        ResultSet result = selectLockedObjectsQuery.getLockedObjectWithOwner(block);

        if (result == null) {
            return false;
        }

        boolean isLocked = false;
        boolean playerIsOwner = false;
        int padlockId = 0;

        try {
            isLocked = result.next();

            if (!isLocked)
                return false;

            padlockId = result.getInt("locked_objects_list.id");

            String playerUuid = player.getUniqueId().toString();
            UUID ownerUuid = UUID.fromString(result.getString("player_list.uuid"));

            playerIsOwner = playerUuid.equals(ownerUuid.toString());
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }

        if (!playerIsOwner) {
            player.sendMessage(LockSystem.access.getMessage("lockable.notOwner"));

            return false;
        }

        //TODO: Finish the command in the next update
        /*HashMap<String, Integer> guilds = new HashMap<>();
        try {
            ResultSet memberResult = Utility.getInfoFromDatabase(player,
                    "SELECT name, id FROM guild_list");

            if (memberResult == null)
                return false;

            while (memberResult.next()) {
                String name = memberResult.getString(1);
                Integer id = memberResult.getInt(2);

                guilds.put(name, id);
            }
        }

        catch (SQLException exception) {
            player.sendMessage(ChatColor.RED + "Coś poszło nie tak! Zgłoś to królowi!");
            exception.printStackTrace();

            return false;
        }

        for (int i = 1; i < args.length; i += 1) {
            for (HashMap.Entry<String, Integer> guild : guilds.entrySet()) {
                if (guild.getKey().startsWith(args[i])) {
                    UpdateQuery query = (UpdateQuery) API.getDatabase().updateQuery();

                    try {
                        //TODO: Find the best way of storing registered guild in the padlock
                        PreparedStatement statement = query.setQuery("UPDATE locked_objects_members_list;");
                        //statement.setInt(1, padlockId);

                        query.execute();

                        player.sendMessage(ChatColor.RED + "Komenda chwilowo nieskonczona!");
                        //player.sendMessage(ChatColor.RED + "Gildia dodana pomyslnie do klodki!");
                    }

                    catch (SQLException exception) {
                        player.sendMessage(ChatColor.RED + "Coś poszło nie tak! Zgłoś to królowi!");
                        exception.printStackTrace();

                        return false;
                    }
                }
            }
        }

        player.sendMessage(ChatColor.GREEN + "Klodka zaaktualizowana!");*/
        return true;
    }

    private boolean removeGuild(CommandSender sender, String[] args) {
        return true;
    }
}