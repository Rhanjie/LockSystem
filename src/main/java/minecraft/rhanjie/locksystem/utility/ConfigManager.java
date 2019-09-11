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

        ArrayList<String> padlocks = Lists.newArrayList("iron_block", "gold_block", "diamond_block");
        config.addDefault("padlocks", padlocks);

        ArrayList<String> picklocks = Lists.newArrayList("shears", "wooden_pickaxe");
        config.addDefault("picklocks", picklocks);

        plugin.saveConfig();
    }

    private void createDefaultMessagesIfNotExist() {
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        messages.options().copyDefaults(true);

        messages.addDefault("chest.ownerInfo", ChatColor.GREEN + "Skrzynia ");
        messages.addDefault("chest.padlockInfo", "Zabezpieczona zamkiem");
        messages.addDefault("chest.levelInfo", "Poziom twojej skrzyni ");
        messages.addDefault("chest.levelTip", "Jesli chcesz zwiekszyc poziom klodki, kliknij odpowiednim blokiem");
        messages.addDefault("chest.notOwner", ChatColor.RED + "To nie jest twoja skrzynia!");

        messages.addDefault("chest.createSuccess", ChatColor.GREEN + "Klodka zalozona!");
        messages.addDefault("chest.improveSuccess", ChatColor.GREEN + "Klodka ulepszona!");
        messages.addDefault("chest.improveFail", ChatColor.RED + "Skrzynia ma lepsza klodke od tej, ktora probujesz zalozyc!");

        messages.addDefault("chest.breakSuccess", ChatColor.GREEN + "Pomyslnie wlamales sie do skrzyni");
        messages.addDefault("chest.breakFail", ChatColor.RED + "Zlamales wytrych i zostawiles slady!");

        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
