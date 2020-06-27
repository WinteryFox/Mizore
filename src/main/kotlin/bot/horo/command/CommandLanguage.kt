package bot.horo.command

import bot.horo.data.getSettings
import discord4j.rest.util.Color
import kotlinx.coroutines.reactive.awaitSingle
import java.util.*

fun CommandsBuilder.language() {
    command("language") {
        alias("locale")
        alias("lang")

        dispatch {
            val title = localization.translate("language.title", this.event.member.get())
            val guild = localization.translate("language.guild", this.event.member.get())
            val user = localization.translate("language.user", this.event.member.get())
            val none = localization.translate("language.none", this.event.member.get())
            val userSettings = this.event.member.get().getSettings(this.database)
            val guildSettings = this.event.guild.awaitSingle().getSettings(this.database)

            this.event.message.channel.awaitSingle()
                .createEmbed { embed ->
                    embed.setTitle(title)
                        .addField(guild, guildSettings.locale?.displayName ?: none, false)
                        .addField(
                            user,
                            userSettings.locale.displayName,
                            false
                        )
                        .setColor(Color.PINK)
                }
                .awaitSingle()
        }

        subcommand("set") {
            parameter("language", true)

            dispatch {
                val locale =
                    this.localization.locales.firstOrNull { it.displayLanguage.toLowerCase() == this.parameters["language"]!! }

                if (locale == null) {
                    this.event.message.channel.awaitSingle()
                        .createMessage(localization.translate("language.unknown", this.event.member.get()).format(
                            this.parameters["language"],
                            this.localization.locales.joinToString {
                                "`${it.getDisplayLanguage(
                                    Locale.forLanguageTag(
                                        "en-GB"
                                    )
                                )}`"
                            }
                        ))
                        .awaitSingle()
                    return@dispatch
                }

                this.database.execute(
                    """
                    INSERT INTO users (snowflake, locale)
                    VALUES ($1, $2)
                    ON CONFLICT (snowflake) DO UPDATE SET locale = $2
                    """,
                    mapOf(Pair("$1", this.event.member.get().id), Pair("$2", locale.toLanguageTag()))
                )

                this.event.message.channel.awaitSingle()
                    .createMessage(
                        localization.translate(
                            "language.user.updated",
                            this.event.member.get()
                        ).format(locale.getDisplayLanguage(locale))
                    )
                    .awaitSingle()
            }
        }
    }
}