package bot.horo.data

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
        mapOf(Pair("$1", this.id))
    ) { row, _ ->
        UserSettings(
            id,
            Locale.forLanguageTag(row["locale"] as String)
        )
    }.getOrElse(0) { UserSettings(id, Locale.forLanguageTag("en-GB")) }