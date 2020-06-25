package bot.horo.data

import bot.horo.Database
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.User
import java.util.*

data class UserSettings(
    val snowflake: Snowflake,
    val locale: Locale
)

suspend fun User.getSettings(database: Database) =
    database.query(
        "SELECT * FROM users WHERE snowflake = $1",
        mapOf(Pair("$1", this.id.asLong()))
    ) { row, _ ->
        UserSettings(
            id,
            Locale(row["locale"] as String)
        )
    }.single()