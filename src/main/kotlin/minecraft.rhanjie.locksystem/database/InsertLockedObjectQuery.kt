package minecraft.rhanjie.locksystem.database

import minecraft.throk.api.database.Database
import org.bukkit.block.Block
import org.bukkit.entity.Player
import java.sql.PreparedStatement
import java.sql.SQLException

class InsertLockedObjectQuery constructor(private val database: Database) {
    fun addNewObject(block: Block, player: Player, padlockLevel: Int): Boolean {
        val query = database.updateQuery()
        lateinit var statement: PreparedStatement

        try {
            statement = query.setQuery(
                "INSERT INTO locked_objects_list(world_uuid, loc_x, loc_y, loc_z, type, owner_id, level, created_at) " +
                "values (?, ?, ?, ?, ?, ?, ?, now());"
            )
        }

        catch(exception: SQLException) {
            exception.printStackTrace()

            return false
        }

        val location = block.location
        val world = location.world ?: return false

        statement.setString(1, world.uid.toString())
        statement.setInt(2, location.blockX)
        statement.setInt(3, location.blockY)
        statement.setInt(4, location.blockZ)

        statement.setString(5, block.type.toString())
        statement.setString(6, player.uniqueId.toString())
        statement.setInt(7, padlockLevel)

        query.execute()
        return true
    }
}