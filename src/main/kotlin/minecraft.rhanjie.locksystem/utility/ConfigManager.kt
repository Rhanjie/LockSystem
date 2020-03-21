package minecraft.rhanjie.locksystem.utility

import com.google.common.collect.Lists
import minecraft.rhanjie.locksystem.LockSystem
import org.bukkit.ChatColor
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.IOException
import java.util.ArrayList
import java.io.File

class ConfigManager constructor(private val plugin: LockSystem) {
    val config: FileConfiguration = plugin.config

    private lateinit var messagesFile: File
    private lateinit var messages: YamlConfiguration

    init {
        loadAll()
    }

    operator fun get(id: String): Any? {
        return config.get(id)
    }

    fun getMessage(id: String): String {
        return if (messages.getString(id) == null)
            "Error! Message with id '$id' not found!"

        else messages.getString(id)!!
    }

    private fun loadAll() {
        val dataFolder: File = plugin.getDataFolder()
        if (!dataFolder.exists()) {
            dataFolder.mkdir()
        }

        messagesFile = File(dataFolder.absolutePath, "messages.yml")

        //TODO: Throw exception if null

        createDefaultConfigIfNotExist()
        createDefaultMessagesIfNotExist()
    }

    private fun createDefaultConfigIfNotExist() {
        config.options().copyDefaults(true)

        val lockableBlocks: ArrayList<String> = Lists.newArrayList("chest", "dispenser", "dropper",
                "oak_door", "spruce_door", "birch_door", "jungle_door", "acacia_door", "dark_oak_door")
        config.addDefault("lockableBlocks", lockableBlocks)

        val padlocks: ArrayList<String> = Lists.newArrayList("iron_block", "gold_block", "diamond_block")
        config.addDefault("padlocks", padlocks)

        val picklocks: ArrayList<String> = Lists.newArrayList("shears")
        config.addDefault("picklocks", picklocks)

        plugin.saveConfig()
    }

    private fun createDefaultMessagesIfNotExist() {
        messages = YamlConfiguration.loadConfiguration(messagesFile)
        messages.options().copyDefaults(true)

        messages.addDefault("lockable.ownerInfo", ChatColor.GREEN.toString() + "Wlaściciel ")
        messages.addDefault("lockable.padlockInfo", "Zabezpieczona zamkiem")
        messages.addDefault("lockable.levelInfo", "Poziom kłódki ")
        messages.addDefault("lockable.levelTip", "Jeśli chcesz zwiekszyć poziom klódki, kliknij odpowiednim blokiem")
        messages.addDefault("lockable.notOwner", ChatColor.RED.toString() + "Nie masz dostępu do klódki!")

        messages.addDefault("lockable.createSuccess", ChatColor.GREEN.toString() + "Klódka zalożona!")
        messages.addDefault("lockable.removeSuccess", ChatColor.GREEN.toString() + "Klódka pomyślnie usunięta!")
        messages.addDefault("lockable.improveSuccess", ChatColor.GREEN.toString() + "Klódka ulepszona!")
        messages.addDefault("lockable.improveFail", ChatColor.RED.toString() + "Próbujesz załozyć taka samą lub gorszą klodkę!")

        messages.addDefault("lockable.breakSuccess", ChatColor.GREEN.toString() + "Pomyślnie wlamaleś się do schowka")
        messages.addDefault("lockable.breakFail", ChatColor.RED.toString() + "Złamaleś wytrych i zostawileś ślady!")

        messages.addDefault("lockable.destroyFail", ChatColor.RED.toString() + "Nie możesz zniszczyć zabezpieczonego kłódką bloku!")
        messages.addDefault("lockable.criticalError", ChatColor.RED.toString() + "Coś poszło nie tak! Zgłoś to królowi!")

        //TODO; Add all messages

        try { messages.save(messagesFile) }
        catch (exception: IOException) {
            exception.printStackTrace()
        }
    }
}