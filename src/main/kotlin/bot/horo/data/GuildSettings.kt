package bot.horo.data

import bot.horo.Database
import bot.horo.PREFIX
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Guild
import java.util.*

data class GuildSettings(
    val snowflake: Snowflake,
    val prefixes: Set<String>,
    val locale: Locale?
)

suspend fun Guild.getSettings(database: Database) =
    database.query(
        """
SELECT guilds.*, array_remove(array_agg(p.prefix), null) prefixes
FROM guilds
         LEFT JOIN prefixes p on guilds.snowflake = p.snowflake
WHERE guilds.snowflake = $1
GROUP BY guilds.snowflake
        """,
        mapOf(Pair("$1", this.id.asLong()))
    ) { row, _ ->
        GuildSettings(
            id,
            (row["prefixes"] as Array<String>).toSet().plus(PREFIX),
            if (row["locale"] as String? == null) null else Locale(row["locale"] as String)
        )
    }.single()