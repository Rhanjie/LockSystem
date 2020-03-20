package minecraft.rhanjie.locksystem.database

import minecraft.rhanjie.locksystem.utility.Utility
import minecraft.throk.api.database.Database
import minecraft.throk.api.database.UpdateQuery
import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.block.Chest
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Timestamp
import java.time.LocalDateTime

class UpdateLocketObjectsQuery constructor(private val database: Database): LockedObjectQuery() {
    private lateinit var updateQuery: UpdateQuery

    fun destroy(block: Block, destroyGuilty: String, destroyReason: String): Boolean {
        val columns = listOf("destroyed_at", "destroy_guilty", "destroy_reason")
        val preparedStatement = getPreparedStatement(block, columns) ?: return false

        val now = Timestamp.valueOf(LocalDateTime.now())
        preparedStatement.setTimestamp(1, now)
        preparedStatement.setString(2, destroyGuilty)
        preparedStatement.setString(3, destroyReason)

        updateQuery.execute()
        return true
    }

    fun updateLevel(block: Block, newPadlockLevel: Int): Boolean {
        val columns = listOf("level")
        val preparedStatement = getPreparedStatement(block, columns) ?: return false

        preparedStatement.setInt(1, newPadlockLevel)

        updateQuery.execute()
        return true
    }

    fun updateBreakProtectionTime(block: Block, timestamp: Timestamp): Boolean {
        val columns = listOf("break_protection_time")
        val preparedStatement = getPreparedStatement(block, columns) ?: return false

        preparedStatement.setTimestamp(1, timestamp)

        updateQuery.execute()
        return true
    }

    fun updateLastBreakAttempt(block: Block, timestamp: Timestamp): Boolean {
        val columns = listOf("last_break_attempt")
        val preparedStatement = getPreparedStatement(block, columns) ?: return false

        preparedStatement.setTimestamp(1, timestamp)

        updateQuery.execute()
        return true
    }

    private fun getPreparedStatement(block: Block, columnsToUpdate: List<String>): PreparedStatement? {
        if (columnsToUpdate.isEmpty())
            return null

        updateQuery = database.updateQuery()

        val firstLocation = block.location
        val secondLocation = getSecondBlockPart(block)

        var condition = getStandardConditionWhere()

        if (secondLocation != null) {
            condition = getDoubleConditionWhere()
        }

        var queryPart = ""

        for (column in columnsToUpdate) {
            queryPart += "$column = ?, "
        }

        queryPart.removeRange(queryPart.length - 2, queryPart.length - 1)

        Bukkit.getLogger().warning("[DEBUG] $queryPart")

        val preparedStatement: PreparedStatement = try {
            updateQuery.setQuery("UPDATE locked_objects_list SET $queryPart WHERE $condition")
        }

        catch (exception: SQLException) {
            exception.printStackTrace()

            return null
        }

        setPreparedStatement(columnsToUpdate.size + 1, preparedStatement, firstLocation, secondLocation)

        return preparedStatement
    }

    fun updatePosition(block: Block): Boolean {
        updateQuery = database.updateQuery()

        if (block.state !is Chest) {
            return false
        }

        val firstLocation = block.location
        val newLocation = Utility.getChestSecondPartLocation(block.state as Chest) ?: return false

        val condition = getStandardConditionWhere()

        val preparedStatement = try {
            updateQuery.setQuery("UPDATE locked_objects_list SET loc_x = ?, loc_y = ?, loc_z = ? WHERE $condition")
        }

        catch (exception: SQLException) {
            exception.printStackTrace()

            return false
        }

        preparedStatement.setInt(1, newLocation.blockX)
        preparedStatement.setInt(2, newLocation.blockY)
        preparedStatement.setInt(3, newLocation.blockZ)
        setPreparedStatement(3, preparedStatement, firstLocation, null)

        updateQuery.execute()
        return true
    }
}