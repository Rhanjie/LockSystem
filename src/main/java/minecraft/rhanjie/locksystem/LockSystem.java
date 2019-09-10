package minecraft.rhanjie.locksystem;

import minecraft.rhanjie.locksystem.listeners.ChestInteractionListener;
import minecraft.rhanjie.locksystem.utility.ConfigManager;
import minecraft.throk.api.API;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class LockSystem extends JavaPlugin {
    public static LockSystem access;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        access = this;

        this.prepareMySqlTable();
        this.registerCommands();
        this.registerListeners();
    }

    @Override
    public void onDisable() {
    }

    public String getMessage(String id) {
        return configManager.getMessage(id);
    }

    private void prepareMySqlTable() {
        API.updateSQL("CREATE TABLE IF NOT EXISTS locked_chests_list(id int AUTO_INCREMENT NOT NULL PRIMARY KEY," +
                "loc_x int NOT NULL, loc_y int NOT NULL, loc_z int NOT NULL, owner_id int NOT NULL, level int," +
                "KEY owner_id (owner_id), FOREIGN KEY (owner_id) REFERENCES player_list(id))" +
                "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
    }

    private void registerCommands() {
        //...
    }

    private void registerListeners() {
        PluginManager manager = this.getServer().getPluginManager();

        manager.registerEvents(new ChestInteractionListener(), this);
    }
}
