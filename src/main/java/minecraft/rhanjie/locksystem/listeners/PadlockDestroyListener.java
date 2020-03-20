package minecraft.rhanjie.locksystem.listeners;

import minecraft.rhanjie.locksystem.LockSystem;
import minecraft.rhanjie.locksystem.database.SelectLockedObjectsQuery;
import minecraft.rhanjie.locksystem.database.UpdateLocketObjectsQuery;
import minecraft.rhanjie.locksystem.utility.Utility;
import minecraft.throk.api.database.Database;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class PadlockDestroyListener implements Listener {
    private Database database;

    public PadlockDestroyListener(Database database) {
        this.database = database;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPadlockDestroy(BlockBreakEvent event) {
        FileConfiguration config = LockSystem.access.getConfig();
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (Utility.checkIfElementIsAvailable(config, block.getType().toString(), "lockableBlocks") == -1)
            return;

        SelectLockedObjectsQuery selectLockedObjectsQuery = new SelectLockedObjectsQuery(database);
        ResultSet result = selectLockedObjectsQuery.getLockedObjectWithOwner(block);

        boolean isLocked;
        boolean playerIsOwner = false;
        int padlockLevel = 1;

        try {
            isLocked = result.next();
            if (!isLocked)
                return;

            String playerID = player.getUniqueId().toString();
            String ownerID = result.getString("player_list.uuid");

            playerIsOwner = playerID.equals(ownerID);
            padlockLevel = result.getInt("level");
        }

        catch (SQLException exception) {
            exception.printStackTrace();
        }

        if (!playerIsOwner && !player.hasPermission("LockSystem.destroyLockable")) {
            player.sendMessage(LockSystem.access.getMessage("lockable.destroyFail"));

            event.setCancelled(true);
            return;
        }

        UpdateLocketObjectsQuery updateLocketObjectsQuery = new UpdateLocketObjectsQuery(database);

        if (block.getState() instanceof Chest) {
            updateLocketObjectsQuery.updatePosition(block);
        }

        updateLocketObjectsQuery.destroy(block, player.getName(), "Zniszczenie bloku");

        if (playerIsOwner) {
            List<String> padlocks = config.getStringList("padlocks");
            String padlockName = padlocks.get(padlockLevel - 1).toUpperCase();

            Material padlockMaterial = Material.getMaterial(padlockName);
            if (padlockMaterial != null) {
                player.getInventory().addItem(new ItemStack(padlockMaterial));
            }
        }
    }
}
