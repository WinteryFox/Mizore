package bot.horo

import bot.horo.data.Database
import discord4j.core.`object`.entity.Member
import org.jetbrains.annotations.PropertyKey
import java.util.*

class Localization(
    private val database: Database
) {
    val locales: Set<Locale>

    init {
        val bundles = mutableSetOf<ResourceBundle>()

        for (locale in Locale.getAvailableLocales())
            try {
                bundles.add(ResourceBundle.getBundle("localization", locale))
            } catch (_: MissingResourceException) {
            }

        locales = bundles.map { it.locale }.toSet()
    }

    suspend fun translate(@PropertyKey(resourceBundle = "localization") key: String, member: Member): String =
        ResourceBundle.getBundle(
            "localization",
            Locale.forLanguageTag(
                database.query(
                    """
SELECT CASE
           WHEN (SELECT locale FROM guilds WHERE snowflake = $1) IS NULL THEN
                   (SELECT locale FROM users WHERE snowflake = $2)
           ELSE
                   (SELECT locale FROM guilds WHERE snowflake = $1)
           END
                    """,
                    mapOf(Pair("$1", member.guildId), Pair("$2", member.id))
                ) { row, _ ->
                    row["locale"] as String? ?: "en-GB"
                }.single()
            )
        ).getString(key)
}