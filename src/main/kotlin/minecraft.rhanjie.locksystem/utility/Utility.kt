package minecraft.rhanjie.locksystem.utility

import org.bukkit.Location
import org.bukkit.block.Chest
import org.bukkit.block.DoubleChest
import org.bukkit.block.data.type.Door
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.inventory.InventoryHolder

object Utility {
    fun checkIfElementIsAvailable(config: FileConfiguration, findingPhrase: String, configId: String): Int {
        val elements: List<String> = config.getStringList(configId)

        var number = 1
        for (element in elements) {
            if (findingPhrase.equals(element, true)) {
                return number
            }

            number += 1
        }
        
        return -1
    }

    fun getChestSecondPartLocation(chest: Chest): Location? {
        val holder: InventoryHolder = chest.inventory.holder ?: return null
        if (holder is DoubleChest) {
            val doubleChest: DoubleChest = holder
            var secondPart: Chest = doubleChest.leftSide as Chest

            if (doubleChest.leftSide == null && doubleChest.rightSide == null) {
                return null
            }

            if (chest.block.location.equals(secondPart.location)) {
                secondPart = doubleChest.rightSide as Chest
            }

            return secondPart.block.location
        }

        return null
    }

    fun getDoorSecondPartLocation(door: Door, secondLocation: Location): Location {
        val doorPart: String = door.half.toString()
        if (doorPart == "TOP") {
            secondLocation.y = (secondLocation.blockY - 1).toDouble()
        }

        else if (doorPart == "BOTTOM") {
            secondLocation.y = (secondLocation.blockY + 1).toDouble()
        }

        return secondLocation
    }
}