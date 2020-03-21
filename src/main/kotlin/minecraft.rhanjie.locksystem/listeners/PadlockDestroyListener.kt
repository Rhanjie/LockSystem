package minecraft.rhanjie.locksystem.listeners

import minecraft.rhanjie.locksystem.database.SelectLockedObjectsQuery
import minecraft.rhanjie.locksystem.database.UpdateLocketObjectsQuery
import minecraft.rhanjie.locksystem.utility.ConfigManager
import minecraft.rhanjie.locksystem.utility.Utility.checkIfElementIsAvailable
import minecraft.throk.api.database.Database
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.block.Chest
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack
import java.sql.SQLException

class PadlockDestroyListener constructor(private val database: Database, private val configManager: ConfigManager) : Listener {
    @EventHandler(priority = EventPriority.NORMAL)
    fun onPadlockDestroy(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block
        val blockType = block.type.toString()

        val elementIndex = checkIfElementIsAvailable(configManager.config, blockType, "lockableBlocks")
        if (elementIndex == -1) {
            return
        }

        val selectLockedObjectsQuery = SelectLockedObjectsQuery(database)
        val result = selectLockedObjectsQuery.getLockedObjectWithOwner(block)

        if (result == null) {
            player.sendMessage(configManager.getMessage("lockable.criticalError"))
            event.isCancelled = true

            return
        }

        val isLocked: Boolean
        var playerIsOwner = false
        var padlockLevel = 1

        try {
            isLocked = result.next()
            if (!isLocked) {
                return
            }

            val playerID = player.uniqueId.toString()
            val ownerID = result.getString("player_list.uuid")

            playerIsOwner = playerID == ownerID
            padlockLevel = result.getInt("level")
        }

        catch (exception: SQLException) {
            exception.printStackTrace()
        }

        if (!playerIsOwner && !player.hasPermission("LockSystem.destroyLockable")) {
            player.sendMessage(configManager.getMessage("lockable.destroyFail"))
            event.isCancelled = true

            return
        }

        val updateLocketObjectsQuery = UpdateLocketObjectsQuery(database)

        if (block.state is Chest) {
            updateLocketObjectsQuery.updatePosition(block)
        }

        updateLocketObjectsQuery.destroy(block, player.name, "Zniszczenie bloku")

        if (playerIsOwner) {
            val padlocks = configManager.config.getStringList("padlocks")
            val padlockName = padlocks[padlockLevel - 1].toUpperCase()

            val padlockMaterial = Material.getMaterial(padlockName)
            if (padlockMaterial != null) {
                player.inventory.addItem(ItemStack(padlockMaterial))
            }
        }
    }

}