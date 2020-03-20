package minecraft.rhanjie.locksystem.database

import minecraft.throk.api.database.Database
import java.sql.PreparedStatement
import java.sql.SQLException

class InsertMemberQuery constructor(private val database: Database) {
    fun addNewObject(padlockId: Int, playerUuid: String): Boolean {
        val query = database.updateQuery()
        lateinit var statement: PreparedStatement

        try {
            statement = query.setQuery(
                "INSERT INTO locked_objects_list(locked_object_id, uuid, added_at) values (?, ?, now());"
            )
        }

        catch(exception: SQLException) {
            exception.printStackTrace()

            return false
        }

        statement.setInt(1, padlockId)
        statement.setString(2, playerUuid)

        query.execute()
        return true
    }
}