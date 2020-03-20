package minecraft.rhanjie.locksystem.utility;

import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.type.Door;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

public class Utility {
    public static int checkIfElementIsAvailable(FileConfiguration config, String findingPhrase, String configId) {
        List<String> elements = config.getStringList(configId);

        int number = 1;
        for (String element : elements) {
            if (findingPhrase.equalsIgnoreCase(element))
                return number;

            number += 1;
        }

        return -1;
    }

    public static Location getChestSecondPartLocation(Chest chest) {
        InventoryHolder holder = chest.getInventory().getHolder();
        if (holder instanceof DoubleChest) {
            DoubleChest doubleChest = (DoubleChest) holder;
            Chest secondPart = (Chest) doubleChest.getLeftSide();

            if (doubleChest.getLeftSide() == null && doubleChest.getRightSide() == null)
                return null;

            if (chest.getBlock().getLocation().equals(secondPart.getLocation()))
                secondPart = (Chest) doubleChest.getRightSide();

            return secondPart.getBlock().getLocation();
        }

        return null;
    }

    public static Location getDoorSecondPartLocation(Door door, Location secondLocation) {
        String doorPart = door.getHalf().toString();

        if (doorPart.equals("TOP"))
            secondLocation.setY(secondLocation.getBlockY() - 1);

        else if (doorPart.equals("BOTTOM"))
            secondLocation.setY(secondLocation.getBlockY() + 1);

        return secondLocation;
    }
}
