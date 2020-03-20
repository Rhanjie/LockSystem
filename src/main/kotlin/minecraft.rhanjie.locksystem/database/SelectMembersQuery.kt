package minecraft.rhanjie.locksystem.database

import minecraft.throk.api.database.Database
import java.sql.ResultSet
import java.sql.SQLException

class SelectMembersQuery constructor(private val database: Database) {
    fun getMembersFromLockedObjectId(id: Int): ResultSet? {
        val selectQuery = database.selectQuery()

        val preparedStatement = try {
            selectQuery.setQuery("SELECT * FROM locked_objects_members_list WHERE locked_object_id = ?")
        }

        catch (exception: SQLException) {
            exception.printStackTrace()

            return null
        }

        preparedStatement.setInt(1, id)
        selectQuery.execute()

        return selectQuery.resultSet
    }

    fun checkIfMemberExists(padlockId: Int, memberUuid: String): ResultSet? {
        val selectQuery = database.selectQuery()

        val preparedStatement = try {
            selectQuery.setQuery("SELECT * FROM locked_objects_members_list WHERE locked_object_id = ?")
        }

        catch (exception: SQLException) {
            exception.printStackTrace()

            return null
        }

        preparedStatement.setInt(1, padlockId)
        preparedStatement.setString(1, memberUuid)
        selectQuery.execute()

        return selectQuery.resultSet
    }
}