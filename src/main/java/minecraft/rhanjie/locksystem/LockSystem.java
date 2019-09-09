package minecraft.rhanjie.locksystem;

import minecraft.rhanjie.locksystem.listeners.ChestInteractionListener;
import minecraft.rhanjie.locksystem.utility.ConfigManager;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class LockSystem extends JavaPlugin {
    public static LockSystem access;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        access = this;

        this.registerCommands();
        this.registerListeners();

        this.getLogger().info(configManager.getMessage("server.test"));
    }

    @Override
    public void onDisable() {
    }

    public String getMessage(String id) {
        return configManager.getMessage(id);
    }


    private void registerCommands() {
        //...
    }

    private void registerListeners() {
        PluginManager manager = this.getServer().getPluginManager();

        manager.registerEvents(new ChestInteractionListener(), this);
    }
}
