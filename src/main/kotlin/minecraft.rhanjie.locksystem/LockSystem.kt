package minecraft.rhanjie.locksystem

import minecraft.rhanjie.locksystem.commands.PluginCommand
import minecraft.rhanjie.locksystem.database.SelectLockedObjectsQuery
import minecraft.rhanjie.locksystem.database.UpdateLocketObjectsQuery
import minecraft.rhanjie.locksystem.listeners.PadlockDestroyListener
import minecraft.rhanjie.locksystem.listeners.PadlockInteractionListener
import minecraft.rhanjie.locksystem.utility.ConfigManager
import minecraft.rhanjie.locksystem.utility.Utility
import minecraft.throk.api.API
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.plugin.PluginManager
import org.bukkit.plugin.java.JavaPlugin
import java.sql.ResultSet
import java.sql.SQLException
import java.util.UUID

class LockSystem : JavaPlugin() {
    private lateinit var configManager: ConfigManager
    private var api: API? = null

    override fun onEnable() {
        api = server.pluginManager.getPlugin("API") as API

        if (api == null) {
            Bukkit.getLogger().severe("API plugin not found!")
            Bukkit.getPluginManager().disablePlugin(this)

            return
        }

        try { api!!.database.runPluginMigrations(this) }
        catch (exception: SQLException) {
            Bukkit.getLogger().severe("Database migration doesn't work!")
            Bukkit.getPluginManager().disablePlugin(this)

            return
        }

        configManager = ConfigManager(this)

        checkAllPadlocks()

        registerCommands()
        registerListeners()
    }

    private fun checkAllPadlocks() {
        val selectLockedObjectsQuery = SelectLockedObjectsQuery(api!!.database)
        val result = selectLockedObjectsQuery.getAllLockedObjects()
        if (result == null) {
            Bukkit.getLogger().severe(configManager.getMessage("lockable.criticalError"))
            Bukkit.getPluginManager().disablePlugin(this)

            return
        }

        try {
            while (result.next()) {
                val worldUuid: UUID = UUID.fromString(result.getString(2))
                val world: World? = Bukkit.getWorld(worldUuid)

                if (world == null) {
                    Bukkit.getLogger().warning("World '$worldUuid' not found! Ignoring padlock...")

                    return
                }

                val x = result.getInt(3).toDouble()
                val y = result.getInt(4).toDouble()
                val z = result.getInt(5).toDouble()

                val location = Location(world, x, y, z)
                val block: Block = location.block

                if (Utility.checkIfElementIsAvailable(this.config, block.type.toString(), "lockableBlocks") != -1) {
                    continue
                }

                val updateLocketObjectsQuery = UpdateLocketObjectsQuery(api!!.getDatabase())
                val destroyReason = "Blad! Brak bloku, uszkodzony zamek!"

                updateLocketObjectsQuery.destroy(block, "undefined", destroyReason)
            }
        }

        catch (exception: SQLException) {
            exception.printStackTrace()
        }
    }

    private fun registerCommands() {
        this.getCommand("klodka")?.setExecutor(PluginCommand(api!!.database, configManager))
    }

    private fun registerListeners() {
        val manager = this.server.pluginManager

        manager.registerEvents(PadlockInteractionListener(api!!.database, configManager), this)
        manager.registerEvents(PadlockDestroyListener(api!!.database, configManager), this)
    }
}