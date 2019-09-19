package minecraft.rhanjie.locksystem.utility;

import com.google.common.collect.Lists;
import minecraft.rhanjie.locksystem.LockSystem;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.io.File;

public class ConfigManager {
    private LockSystem plugin;
    private FileConfiguration config;
    private File messagesFile;
    private YamlConfiguration messages;

    public ConfigManager(LockSystem plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();

        this.loadAll();
    }

    public Object get(String id) {
        return config.get(id);
    }

    public String getMessage(String id) {
        if (messages.getString(id) == null)
            return "Error! Message with id '" + id + "' not found!";

        return messages.getString(id);
    }

    private void loadAll() {
        File dataFolder = plugin.getDataFolder();

        if (!dataFolder.exists())
            dataFolder.mkdir();

        this.messagesFile = new File(dataFolder.getAbsolutePath(), "messages.yml");

        this.createDefaultConfigIfNotExist();
        this.createDefaultMessagesIfNotExist();
    }

    private void createDefaultConfigIfNotExist() {
        config.options().copyDefaults(true);

        ArrayList<String> lockableBlocks = Lists.newArrayList("chest", "dispenser", "dropper",
                "oak_door", "spruce_door", "birch_door", "jungle_door", "acacia_door", "dark_oak_door");
        config.addDefault("lockableBlocks", lockableBlocks);

        ArrayList<String> padlocks = Lists.newArrayList("iron_block", "gold_block", "diamond_block");
        config.addDefault("padlocks", padlocks);

        ArrayList<String> picklocks = Lists.newArrayList("shears");
        config.addDefault("picklocks", picklocks);

        plugin.saveConfig();
    }

    private void createDefaultMessagesIfNotExist() {
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        messages.options().copyDefaults(true);

        messages.addDefault("lockable.ownerInfo", ChatColor.GREEN + "Wlasciciel ");
        messages.addDefault("lockable.padlockInfo", "Zabezpieczona zamkiem");
        messages.addDefault("lockable.levelInfo", "Poziom klodki ");
        messages.addDefault("lockable.levelTip", "Jesli chcesz zwiekszyc poziom klodki, kliknij odpowiednim blokiem");
        messages.addDefault("lockable.notOwner", ChatColor.RED + "Nie masz dostepu do klodki!");

        messages.addDefault("lockable.createSuccess", ChatColor.GREEN + "Klodka zalozona!");
        messages.addDefault("lockable.removeSuccess", ChatColor.GREEN + "Klodka pomyslnie usunieta!");
        messages.addDefault("lockable.improveSuccess", ChatColor.GREEN + "Klodka ulepszona!");
        messages.addDefault("lockable.improveFail", ChatColor.RED + "Probujesz zalozyc gorsza lub taka sama klodke!");

        messages.addDefault("lockable.breakSuccess", ChatColor.GREEN + "Pomyslnie wlamales sie do schowka");
        messages.addDefault("lockable.breakFail", ChatColor.RED + "Zlamales wytrych i zostawiles slady!");

        messages.addDefault("lockable.destroyFail", ChatColor.RED + "Nie mozesz zniszczyc zabezpieczonego klodka bloku!");

        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
