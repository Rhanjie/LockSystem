package minecraft.rhanjie.locksystem.database

import minecraft.throk.api.database.Database
import minecraft.throk.api.database.UpdateQuery
import org.bukkit.Bukkit
import org.bukkit.block.Block
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Timestamp
import java.time.LocalDateTime

class UpdateMembersQuery constructor(private val database: Database): LockedObjectQuery() {
    private lateinit var updateQuery: UpdateQuery

    fun remove(padlockId: Int, members: List<String>): Boolean {
        if (members.isEmpty())
            return false

        updateQuery = database.updateQuery()

        var condition = "locked_object_id = ?"
        for (i in members.indices) {
            if (i == 0) {
                condition += " AND (uuid = ? "
            }

            else condition += " OR uuid = ? "
        }
        condition += ");"

        val preparedStatement: PreparedStatement = try {
            updateQuery.setQuery("DELETE FROM locked_objects_members_list WHERE $condition")
        }

        catch (exception: SQLException) {
            exception.printStackTrace()

            return false
        }

        preparedStatement.setInt(1, padlockId)

        var i = 2
        for (member in members) {
            preparedStatement.setString(i++, member)
        }

        updateQuery.execute()
        return true
    }
}