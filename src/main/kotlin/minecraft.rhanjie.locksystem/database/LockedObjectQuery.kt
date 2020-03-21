package minecraft.rhanjie.locksystem.database

import minecraft.rhanjie.locksystem.utility.Utility
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.block.data.type.Door
import java.sql.PreparedStatement

abstract class LockedObjectQuery {
    protected fun getSecondBlockPart(block: Block): Location? {
        var secondLocation: Location?

        if (block.state is Chest) {
            secondLocation = Utility.getChestSecondPartLocation(block.state as Chest)
            if (secondLocation != null)
                return secondLocation
        }

        if (block.blockData is Door) {
            secondLocation = Utility.getDoorSecondPartLocation(block.blockData as Door, block.location)

            return secondLocation
        }

        return null
    }

    protected fun setPreparedStatement(index: Int, preparedStatement: PreparedStatement, firstLocation: Location, secondLocation: Location?) {
        preparedStatement.setString(index, firstLocation.world.toString())
        preparedStatement.setInt(index + 1, firstLocation.blockX)
        preparedStatement.setInt(index + 2, firstLocation.blockY)
        preparedStatement.setInt(index + 3, firstLocation.blockZ)

        if (secondLocation != null) {
            preparedStatement.setInt(index + 4, secondLocation.blockX)
            preparedStatement.setInt(index + 5, secondLocation.blockY)
            preparedStatement.setInt(index + 6, secondLocation.blockZ)
        }
    }

    protected fun getStandardConditionWhere(): String {
        return "world_uuid = ? AND loc_x = ? AND loc_y = ? AND loc_z = ? AND destroyed_at IS NULL;"
    }

    protected fun getDoubleConditionWhere(): String {
        return "world_uuid = ? AND (loc_x = ? AND loc_y = ? AND loc_z = ? OR loc_x = ? AND loc_y = ? AND loc_z = ?) AND destroyed_at IS NULL;"
    }
}