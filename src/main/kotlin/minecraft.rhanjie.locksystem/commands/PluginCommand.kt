package minecraft.rhanjie.locksystem.commands

import minecraft.rhanjie.locksystem.database.*
import minecraft.rhanjie.locksystem.utility.ConfigManager
import minecraft.rhanjie.locksystem.utility.Utility.checkIfElementIsAvailable
import minecraft.throk.api.database.Database
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.sql.SQLException
import java.util.*

class PluginCommand(private val database: Database, private val configManager: ConfigManager) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (args.isEmpty() || args[0].equals("pomoc", ignoreCase = true))
            return helpCommand(sender)

        return when (args[0]) {
            "info"          -> infoCommand(sender)
            "reload"        -> reloadCommand(sender)
            "usun"          -> removePadlockCommand(sender)
            "dodaj_czlonka" -> addMember(sender, args)
            "usun_czlonka"  -> removeMember(sender, args)
            "dodaj_gildie"  -> addGuild(sender, args)
            "usun_gildie"   -> removeGuild(sender, args)

            else -> {
                sender.sendMessage("${ChatColor.RED}Nierozpoznana komenda!\n" +
                        "Uzyj ${ChatColor.GREEN} /klodka pomoc ${ChatColor.RED}, aby uzyskac liste komend!")

                false
            }
        }
    }

    private fun helpCommand(sender: CommandSender): Boolean {
        var helpInfo = "--- [LISTA KOMEND] ---\n"
        helpInfo += "${ChatColor.GREEN}/klodka usun\n"
        helpInfo += "${ChatColor.GREEN}/klodka dodaj_czlonka <nick_1> <nick_n>\n"
        helpInfo += "${ChatColor.GREEN}/klodka usun_czlonka <nick_1> <nick_n>\n"
        helpInfo += "${ChatColor.RED}/klodka dodaj_gildie <gildia_1> <gildia_n>\n";
        helpInfo += "${ChatColor.RED}/klodka usun_gildie <gildia_1> <gildia_n>\n";

        sender.sendMessage(helpInfo)
        return true
    }

    private fun infoCommand(player: CommandSender): Boolean {
        if (player !is Player) {
            return false
        }

        val block = player.getTargetBlock(null, 10)

        val config = configManager.config
        val elementIndex = checkIfElementIsAvailable(config, block.type.toString(), "lockableBlocks")
        if (elementIndex == -1) {
            return false
        }

        val selectLockedObjectsQuery = SelectLockedObjectsQuery(database)
        val result = selectLockedObjectsQuery.getLockedObjectWithOwner(block) ?: return false

        val recordId: Int
        val isLocked: Boolean
        val playerIsOwner: Boolean
        val currentPadlockLevel: Int
        val ownerName: String

        try {
            isLocked = result.next()
            if (!isLocked) {
                return false
            }

            recordId = result.getInt("locked_objects_list.id")

            val playerUuid = player.uniqueId.toString()
            val ownerUuid = UUID.fromString(result.getString("player_list.uuid"))

            playerIsOwner = playerUuid == ownerUuid.toString()
            ownerName = Bukkit.getOfflinePlayer(ownerUuid).name!!

            currentPadlockLevel = result.getInt("level")
        }

        catch (exception: SQLException) {
            player.sendMessage(configManager.getMessage("lockable.criticalError"))
            exception.printStackTrace()

            return false
        }

        if (playerIsOwner) {
            val members = ArrayList<String?>()

            val selectMembersQuery = SelectMembersQuery(database)
            val membersResult = selectMembersQuery.getMembersFromLockedObjectId(recordId) ?: return false

            try {
                while (membersResult.next()) {
                    val rawUuid = membersResult.getString(1)
                    val uuid = UUID.fromString(rawUuid)
                    members.add(Bukkit.getOfflinePlayer(uuid).name)
                }
            }

            catch (exception: SQLException) {
                player.sendMessage(configManager.getMessage("lockable.criticalError"))
                exception.printStackTrace()

                return false
            }

            var message = ""
            message += "${ChatColor.GREEN}Właściciel: ${ChatColor.GOLD}Ty\n"
            message += "${ChatColor.GREEN}${configManager.getMessage("lockable.levelInfo")} ${ChatColor.GOLD}$currentPadlockLevel\n"
            message += "${ChatColor.RESET}Osoby majace dostep:\n:${ChatColor.GOLD}"

            for (memberName in members) {
                message += "- $memberName\n"
            }

            player.sendMessage(message)
            return true
        }

        player.sendMessage("${configManager.getMessage("lockable.ownerInfo")} ${ChatColor.GOLD}$ownerName")
        player.sendMessage(configManager.getMessage("lockable.padlockInfo"))

        return true
    }

    private fun reloadCommand(sender: CommandSender): Boolean {
        if (!sender.hasPermission("LockSystem.Reload")) {
            sender.sendMessage(configManager.getMessage("${ChatColor.RED}Nie masz uprawnień!"))

            return false
        }

        //TODO: Complete reload command
        //LockSystem.access.reloadConfig();
        return true
    }

    private fun removePadlockCommand(player: CommandSender): Boolean {
        if (player !is Player) {
            return false
        }

        val block = player.getTargetBlock(null, 10)

        val config = configManager.config
        val elementIndex = checkIfElementIsAvailable(config, block.type.toString(), "lockableBlocks")
        if (elementIndex == -1) {
            return false
        }

        val selectLockedObjectsQuery = SelectLockedObjectsQuery(database)
        val result = selectLockedObjectsQuery.getLockedObjectWithOwner(block) ?: return false

        val isLocked: Boolean
        val playerIsOwner: Boolean
        val padlockLevel: Int

        try {
            isLocked = result.next()

            if (!isLocked) {
                return false
            }

            val playerUuid = player.uniqueId.toString()
            val ownerUuid = UUID.fromString(result.getString("player_list.uuid"))

            playerIsOwner = playerUuid == ownerUuid.toString()
            padlockLevel = result.getInt("level")
        }

        catch (exception: SQLException) {
            exception.printStackTrace()
            return false
        }

        if (playerIsOwner) {
            val updateLocketObjectsQuery = UpdateLocketObjectsQuery(database)
            val isWell = updateLocketObjectsQuery.destroy(block, player.name, "Usunięcie kłódki")
            if (!isWell) {
                player.sendMessage(configManager.getMessage("lockable.criticalError"))
                return false
            }

            val padlocks = config.getStringList("padlocks")
            val padlockName = padlocks[padlockLevel - 1].toUpperCase()

            val padlockMaterial = Material.getMaterial(padlockName)
            if (padlockMaterial != null) {
                player.inventory.addItem(ItemStack(padlockMaterial))
            }

            player.sendMessage(configManager.getMessage("lockable.removeSuccess"))
            return true
        }

        player.sendMessage(configManager.getMessage("lockable.notOwner"))
        return false
    }

    private fun addMember(player: CommandSender, args: Array<String>): Boolean {
        if (player !is Player) {
            player.sendMessage(ChatColor.RED.toString() + "Musisz byc graczem, aby uzyc tej komendy!")
            return false
        }

        if (args.size < 2) {
            player.sendMessage(ChatColor.RED.toString() + "Podaj nicki dodawanych czlonkow!")
            return false
        }

        val block = player.getTargetBlock(null, 10)
        if (block.type == Material.AIR) {
            return false
        }

        val config = configManager.config
        val elementIndex = checkIfElementIsAvailable(config, block.type.toString(), "lockableBlocks")
        if (elementIndex == -1) {
            return false
        }

        val selectLockedObjectsQuery = SelectLockedObjectsQuery(database)
        val result = selectLockedObjectsQuery.getLockedObjectWithOwner(block) ?: return false

        val isLocked: Boolean
        val playerIsOwner: Boolean
        val padlockId: Int

        try {
            isLocked = result.next()
            if (!isLocked) {
                return false
            }

            padlockId = result.getInt("locked_objects_list.id")

            val playerUuid = player.uniqueId.toString()
            val ownerUuid = UUID.fromString(result.getString("player_list.uuid"))

            playerIsOwner = playerUuid == ownerUuid.toString()
        }

        catch (exception: SQLException) {
            exception.printStackTrace()
            return false
        }

        if (!playerIsOwner) {
            player.sendMessage(configManager.getMessage("lockable.notOwner"))
            return false
        }

        //database.transaction().start();
        for (i in 1 until args.size) {
            //TODO: Change it when the whole code will be refactored
            val playerUuid = Bukkit.getOfflinePlayer(args[i]).uniqueId.toString()

            //TODO: Add cache system
            val selectMembersQuery = SelectMembersQuery(database)
            val memberResult = selectMembersQuery.checkIfMemberExists(padlockId, playerUuid)
            if (memberResult != null) {
                player.sendMessage("${ChatColor.RED}Gracz ${ChatColor.RESET}$args[i] ${ChatColor.RED}był już dodany do kłódki!")

                continue
            }

            val insertMemberQuery = InsertMemberQuery(database)
            if (!insertMemberQuery.addNewObject(padlockId, playerUuid)) {
                player.sendMessage(ChatColor.RED.toString() + "Nie udało się zaaktualizować kłódki!")

                return false
            }
        }
        //database.transaction().commit();

        player.sendMessage(ChatColor.GREEN.toString() + "Klodka zaaktualizowana!")
        return true
    }

    private fun removeMember(player: CommandSender, args: Array<String>): Boolean {
        if (player !is Player) {
            player.sendMessage(ChatColor.RED.toString() + "Musisz byc graczem, aby uzyc tej komendy!")
            return false
        }
        if (args.size < 2) {
            player.sendMessage(ChatColor.RED.toString() + "Podaj nicki usuwanych czlonkow!")
            return false
        }

        val block = player.getTargetBlock(null, 10)
        if (block.type == Material.AIR) {
            return false
        }

        val config = configManager.config
        val elementIndex = checkIfElementIsAvailable(config, block.type.toString(), "lockableBlocks")
        if (elementIndex == -1) {
            return false
        }

        val selectLockedObjectsQuery = SelectLockedObjectsQuery(database)
        val result = selectLockedObjectsQuery.getLockedObjectWithOwner(block) ?: return false

        val playerIsOwner: Boolean
        val padlockId: Int

        try {
            if (!result.next()) {
                return false
            }

            padlockId = result.getInt("locked_objects_list.id")

            val playerUuid = player.uniqueId.toString()
            val ownerUuid = UUID.fromString(result.getString("player_list.uuid"))

            playerIsOwner = playerUuid == ownerUuid.toString()
        }

        catch (exception: SQLException) {
            exception.printStackTrace()
            return false
        }

        if (!playerIsOwner) {
            player.sendMessage(configManager.getMessage("lockable.notOwner"))

            return false
        }

        val updateMembersQuery = UpdateMembersQuery(database)
        val members = ArrayList(Arrays.asList(*args).subList(1, args.size))

        if (!updateMembersQuery.remove(padlockId, members)) {
            player.sendMessage(ChatColor.RED.toString() + "Blad podczas aktualizowania klodki!")

            return false
        }

        player.sendMessage(ChatColor.GREEN.toString() + "Klodka zaaktualizowana!")
        return true
    }

    private fun addGuild(player: CommandSender, args: Array<String>): Boolean {
        if (player !is Player) {
            player.sendMessage(ChatColor.RED.toString() + "Musisz byc graczem, aby uzyc tej komendy!")
            return false
        }
        if (args.size < 2) {
            player.sendMessage(ChatColor.RED.toString() + "Podaj nazwy dodawanych gildii!")
            return false
        }

        val block = player.getTargetBlock(null, 10)
        if (block.type == Material.AIR) {
            return false
        }

        val config = configManager.config
        val elementIndex = checkIfElementIsAvailable(config, block.type.toString(), "lockableBlocks")
        if (elementIndex == -1) {
            return false
        }

        val selectLockedObjectsQuery = SelectLockedObjectsQuery(database)
        val result = selectLockedObjectsQuery.getLockedObjectWithOwner(block) ?: return false

        val isLocked: Boolean
        val playerIsOwner: Boolean
        val padlockId: Int

        try {
            isLocked = result.next()
            if (!isLocked) {
                return false
            }

            padlockId = result.getInt("locked_objects_list.id")

            val playerUuid = player.uniqueId.toString()
            val ownerUuid = UUID.fromString(result.getString("player_list.uuid"))

            playerIsOwner = playerUuid == ownerUuid.toString()
        }

        catch (exception: SQLException) {
            exception.printStackTrace()
            return false
        }

        if (!playerIsOwner) {
            player.sendMessage(configManager.getMessage("lockable.notOwner"))

            return false
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
        return true
    }

    private fun removeGuild(sender: CommandSender, args: Array<String>): Boolean {
        return true
    }
}