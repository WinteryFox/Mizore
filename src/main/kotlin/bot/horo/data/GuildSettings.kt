package bot.horo.data

import bot.horo.PREFIX
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.rest.util.Permission
import kotlinx.coroutines.reactive.awaitSingle
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
        mapOf(Pair("$1", this.id))
    ) { row, _ ->
        @Suppress("UNCHECKED_CAST")
        GuildSettings(
            id,
            (row["prefixes"] as Array<String>).toSet().plus(PREFIX),
            if (row["locale"] as String? == null) null else Locale.forLanguageTag(row["locale"] as String)
        )
    }.getOrElse(0) { GuildSettings(id, setOf(PREFIX), Locale.forLanguageTag("en-GB")) }

suspend fun Guild.firstChannel(): GuildMessageChannel? =
    this.channels
        .collectList()
        .awaitSingle()
        .filterIsInstance<GuildMessageChannel>()
        .filter {
            it.getEffectivePermissions(this.client.selfId).awaitSingle()
                .containsAll(listOf(Permission.VIEW_CHANNEL, Permission.SEND_MESSAGES))
        }
        .minBy { it.position.awaitSingle() }