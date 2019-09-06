package minecraft.rhanjie.locksystem;

import minecraft.rhanjie.locksystem.listeners.ChestInteractionListener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class LockSystem extends JavaPlugin {
    @Override
    public void onEnable() {
        this.getLogger().info("Test message!");

        this.registerCommands();
        this.registerListeners();
    }

    @Override
    public void onDisable() {

    }


    private void registerCommands() {
        //...
    }

    private void registerListeners() {
        PluginManager manager = this.getServer().getPluginManager();

        manager.registerEvents(new ChestInteractionListener(), this);
    }
}
