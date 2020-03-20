package minecraft.rhanjie.locksystem.database

import minecraft.throk.api.database.Database
import org.bukkit.block.Block
import java.sql.ResultSet
import java.sql.SQLException

class SelectLockedObjectsQuery constructor(private val database: Database): LockedObjectQuery() {
    fun getAllLockedObjects(): ResultSet? {
        val selectQuery = database.selectQuery()

        try {
            selectQuery.setQuery("SELECT * FROM locked_objects_list;")
        }

        catch (exception: SQLException) {
            exception.printStackTrace()

            return null
        }

        selectQuery.execute()

        return selectQuery.resultSet
    }

    fun getLockedObjectFromBlock(block: Block): ResultSet? {
        val selectQuery = database.selectQuery()

        val firstLocation = block.location
        val secondLocation = getSecondBlockPart(block)
        var condition = getStandardConditionWhere()

        if (secondLocation != null) {
            condition = getDoubleConditionWhere()
        }

        val preparedStatement = try {
            selectQuery.setQuery("SELECT * FROM locked_objects_list WHERE $condition")
        }

        catch (exception: SQLException) {
            exception.printStackTrace()

            return null
        }

        setPreparedStatement(1, preparedStatement, firstLocation, secondLocation)

        selectQuery.execute()

        return selectQuery.resultSet
    }

    //TODO: Refactor the method
    fun getLockedObjectWithOwner(block: Block): ResultSet? {
        val selectQuery = database.selectQuery()

        val firstLocation = block.location
        val secondLocation = getSecondBlockPart(block)
        var condition = getStandardConditionWhere()

        if (secondLocation != null) {
            condition = getDoubleConditionWhere()
        }

        val preparedStatement = try {
            selectQuery.setQuery(
                "SELECT player_list.uuid, level FROM locked_objects_list " +
                "INNER JOIN player_list ON locked_objects_list.owner_id = player_list.id WHERE $condition"
            )
        }

        catch (exception: SQLException) {
            exception.printStackTrace()

            return null
        }

        setPreparedStatement(1, preparedStatement, firstLocation, secondLocation)

        selectQuery.execute()

        return selectQuery.resultSet
    }
}