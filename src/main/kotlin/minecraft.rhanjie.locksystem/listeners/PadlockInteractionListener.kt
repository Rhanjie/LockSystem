package minecraft.rhanjie.locksystem.listeners

import minecraft.rhanjie.locksystem.database.InsertLockedObjectQuery
import minecraft.rhanjie.locksystem.database.SelectLockedObjectsQuery
import minecraft.rhanjie.locksystem.database.SelectMembersQuery
import minecraft.rhanjie.locksystem.database.UpdateLocketObjectsQuery
import minecraft.rhanjie.locksystem.utility.ConfigManager
import minecraft.rhanjie.locksystem.utility.Utility.checkIfElementIsAvailable
import minecraft.throk.api.database.Database
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.data.type.Door
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import java.sql.SQLException
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

class PadlockInteractionListener(private val database: Database, private val configManager: ConfigManager) : Listener {
    @EventHandler(priority = EventPriority.LOW)
    fun onPadlockInteract(event: PlayerInteractEvent) {
        val player = event.player
        val block = event.clickedBlock

        if (event.action != Action.RIGHT_CLICK_BLOCK || block == null) {
            return
        }

        val config: FileConfiguration = configManager.config
        if (checkIfElementIsAvailable(config, block.type.toString(), "lockableBlocks") == -1) {
            return
        }

        val itemInHand = player.inventory.itemInMainHand.type

        val selectLockedObjectsQuery = SelectLockedObjectsQuery(database)
        val result = selectLockedObjectsQuery.getLockedObjectWithOwner(block)
        if (result == null) {
            player.sendMessage(configManager.getMessage("lockable.criticalError"))
            event.isCancelled = true

            return
        }

        var isLocked = false
        var playerIsOwner = false
        var padlockProtection = false
        var ownerName = player.name

        var recordId = -1
        var currentPadlockLevel = 1
        val newPadlockLevel: Int
        val picklockLevel: Int

        try {
            isLocked = result.next()

            if (isLocked) {
                recordId = result.getInt("locked_objects_list.id")

                val uniqueID = player.uniqueId.toString()
                playerIsOwner = uniqueID == result.getString("player_list.uuid")

                ownerName = result.getString("player_list.name")
                currentPadlockLevel = result.getInt("level")

                if (result.getTimestamp("break_protection_time") != null)
                    padlockProtection = result.getTimestamp("break_protection_time").toInstant().isAfter(Instant.now())
            }
        }

        catch (exception: SQLException) {
            exception.printStackTrace()
        }

        newPadlockLevel = checkIfElementIsAvailable(config, itemInHand.toString(), "padlocks")
        if (newPadlockLevel != -1) {
            createPadlock(event, player, recordId, isLocked, playerIsOwner, currentPadlockLevel, newPadlockLevel)

            return
        }

        if (!isLocked) {
            return
        }

        picklockLevel = checkIfElementIsAvailable(config, itemInHand.toString(), "picklocks")
        if (picklockLevel != -1) {
            if (!playerIsOwner) {
                if (padlockProtection) {
                    player.sendMessage(ChatColor.RED.toString() + "Kłódka została niedawno zniszczona! Spróbuj ponownie później")

                    event.isCancelled = true
                    return
                }

                tryBreakPadlock(event, player, recordId, currentPadlockLevel, picklockLevel)
                return
            }
        }

        displayPadlockInfo(event, player, itemInHand, playerIsOwner, ownerName, recordId, currentPadlockLevel)
    }

    private fun createPadlock(event: PlayerInteractEvent, player: Player, recordId: Int, isLocked: Boolean, playerIsOwner: Boolean, currentPadlockLevel: Int, newPadlockLevel: Int) {
        val block = event.clickedBlock!!

        if (isLocked) {
            if (!playerIsOwner) {
                player.sendMessage(configManager.getMessage("lockable.notOwner"))
                event.isCancelled = true

                return
            }
            if (currentPadlockLevel >= newPadlockLevel) {
                player.sendMessage(configManager.getMessage("lockable.improveFail"))
                event.isCancelled = true

                return
            }

            val updateLocketObjectsQuery = UpdateLocketObjectsQuery(database)
            if (!updateLocketObjectsQuery.updateLevel(block, newPadlockLevel)) {
                player.sendMessage(configManager.getMessage("lockable.criticalError"))
                event.isCancelled = true

                return
            }

            val currentAmount = player.inventory.itemInMainHand.amount
            player.inventory.itemInMainHand.amount = currentAmount - 1

            player.sendMessage("${configManager.getMessage("lockable.improveSuccess")} Aktualny poziom: $newPadlockLevel")
            event.isCancelled = true

            return
        }

        val insertLockedObjectQuery = InsertLockedObjectQuery(database)
        if (!insertLockedObjectQuery.addNewObject(block, player, newPadlockLevel)) {
            player.sendMessage(configManager.getMessage("lockable.criticalError"))
            event.isCancelled = true

            return
        }

        val currentAmount = player.inventory.itemInMainHand.amount
        player.inventory.itemInMainHand.amount = currentAmount - 1
        player.sendMessage(configManager.getMessage("lockable.createSuccess"))

        event.isCancelled = true
    }

    private fun tryBreakPadlock(event: PlayerInteractEvent, player: Player, recordId: Int, currentPadlockLevel: Int, picklockLevel: Int) {
        val random = Random()
        val block = event.clickedBlock!!

        val blockData = event.clickedBlock!!.blockData
        if (blockData is Door) {
            val door = event.clickedBlock!!.blockData as Door
            if (door.isOpen) {
                return
            }
        }

        val mysqlFriendlyFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR, 6)
        val datetime = calendar.time

        val currentTime = mysqlFriendlyFormat.format(datetime)
        val timestamp = Timestamp.valueOf(currentTime)

        val updateLocketObjectsQuery = UpdateLocketObjectsQuery(database)

        //TODO: Wait for player skills system
        val playerLockpickingLevel = random.nextInt(10)
        val chanceToSuccess = 10 + playerLockpickingLevel * 10 - 50 * (currentPadlockLevel - 1)

        if (random.nextInt(100) < chanceToSuccess) {
            if (!updateLocketObjectsQuery.updateBreakProtectionTime(block, timestamp)) {
                player.sendMessage(configManager.getMessage("lockable.criticalError"))
                event.isCancelled = true

                return
            }

            player.sendMessage(configManager.getMessage("lockable.breakSuccess"))

            return
        }

        if (!updateLocketObjectsQuery.updateBreakProtectionTime(event.clickedBlock!!, timestamp)) {
            player.sendMessage(configManager.getMessage("lockable.criticalError"))
            event.isCancelled = true

            return
        }

        val amount = player.inventory.itemInMainHand.amount
        player.inventory.itemInMainHand.amount = amount - 1

        player.world.playSound(block.location, Sound.BLOCK_ANVIL_USE, 100.0f, 1f)
        player.sendMessage(configManager.getMessage("lockable.breakFail"))

        event.isCancelled = true
    }

    private fun displayPadlockInfo(event: PlayerInteractEvent, player: Player, itemInHand: Material, playerIsOwner: Boolean, ownerName: String, recordId: Int, currentPadlockLevel: Int) {
        val members = ArrayList<UUID>()

        val selectMembersQuery = SelectMembersQuery(database)
        val result = selectMembersQuery.getMembersFromLockedObjectId(recordId)
        if (result == null) {
            player.sendMessage(configManager.getMessage("lockable.criticalError"))
            event.isCancelled = true

            return
        }

        var padlockMember = false

        try {
            while (result.next()) {
                val rawUuid = result.getString(1)
                val memberUuid = UUID.fromString(rawUuid)
                members.add(memberUuid)

                if (player.uniqueId == memberUuid) {
                    padlockMember = true
                }
            }
        }

        catch (exception: SQLException) {
            exception.printStackTrace()
        }

        if (playerIsOwner) {
            if (itemInHand == Material.BOOK) {
                var message = ""
                message += "${ChatColor.GREEN}${configManager.getMessage("lockable.ownerInfo")}${ChatColor.GOLD} Ty\n"
                message += "${ChatColor.GREEN}${configManager.getMessage("lockable.levelInfo")}${ChatColor.GOLD} $currentPadlockLevel\n"
                message += "${ChatColor.RESET}Osoby majace dostep:\n${ChatColor.GOLD}"

                for (uuid in members) {
                    message += "- " + Bukkit.getOfflinePlayer(uuid).name + "\n"
                }

                player.sendMessage(message)
                event.isCancelled = true
            }

            return
        }

        if (padlockMember) {
            return
        }

        player.sendMessage("${configManager.getMessage("lockable.ownerInfo")} ${ChatColor.GOLD}$ownerName")
        player.sendMessage(configManager.getMessage("lockable.padlockInfo"))

        event.isCancelled = true
    }
}