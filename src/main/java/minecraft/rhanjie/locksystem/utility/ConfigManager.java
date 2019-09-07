package minecraft.rhanjie.locksystem.utility;

import com.google.common.collect.Lists;
import minecraft.rhanjie.locksystem.LockSystem;
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

        //LockSystem.access

        this.loadAll();
    }

    public Object get(String id) {
        return config.get(id);
    }

    public String getMessage(String id) {
        if (messages.getString(id) == null)
            return "Message with id '" + id + "' not found!";

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

        messages.addDefault("server.test", "testowa wiadomosc");

        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
