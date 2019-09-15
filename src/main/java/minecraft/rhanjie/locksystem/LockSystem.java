package minecraft.rhanjie.locksystem;

import minecraft.rhanjie.locksystem.listeners.PadlockCloneListener;
import minecraft.rhanjie.locksystem.listeners.PadlockDestroyListener;
import minecraft.rhanjie.locksystem.listeners.PadlockInteractionListener;
import minecraft.rhanjie.locksystem.utility.ConfigManager;
import minecraft.throk.api.API;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class LockSystem extends JavaPlugin {
    public static LockSystem access;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        access = this;

        configManager = new ConfigManager(this);

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

    //TODO: Move code below to utils file
    public String getStandardConditionWhere(Location location) {
        int loc_x = location.getBlockX();
        int loc_y = location.getBlockY();
        int loc_z = location.getBlockZ();

        return "(loc_x = " + loc_x + " AND loc_y = " + loc_y + " AND loc_z = " + loc_z + " OR " +
                "sec_loc_x = " + loc_x + " AND sec_loc_y = " + loc_y + " AND sec_loc_z = " + loc_z + ") AND destroyed_at IS NULL;";
    }

    public int checkIfElementIsAvailable(FileConfiguration config, String findingPhrase, String configId) {
        List<String> elements = config.getStringList(configId);

        int number = 1;
        for (String element : elements) {
            if (findingPhrase.equalsIgnoreCase(element))
                return number;

            number += 1;
        }

        return -1;
    }

    private void prepareMySqlTable() {
        API.updateSQL("CREATE TABLE IF NOT EXISTS locked_objects_list(id int AUTO_INCREMENT NOT NULL PRIMARY KEY," +
                "loc_x int NOT NULL, loc_y int NOT NULL, loc_z int NOT NULL, sec_loc_x int, sec_loc_y int, sec_loc_z int," +
                "type varchar(255) NOT NULL, owner_id int NOT NULL, level int NOT NULL, " +
                "created_at datetime NOT NULL, last_break_attempt datetime, destroyed_at datetime, destroy_guilty varchar(255), destroy_reason varchar(255), " +
                "KEY owner_id (owner_id), FOREIGN KEY (owner_id) REFERENCES player_list(id))" +
                "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
    }

    private void registerCommands() {
        //...
    }

    private void registerListeners() {
        PluginManager manager = this.getServer().getPluginManager();

        manager.registerEvents(new PadlockInteractionListener(), this);
        manager.registerEvents(new PadlockDestroyListener(), this);
        manager.registerEvents(new PadlockCloneListener(), this);
    }
}
