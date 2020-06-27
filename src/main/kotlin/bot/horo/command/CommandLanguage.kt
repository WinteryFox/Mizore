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
            val title = translate("language.title", this.event.member.get())
            val guild = translate("language.guild", this.event.member.get())
            val user = translate("language.user", this.event.member.get())
            val none = translate("language.none", this.event.member.get())
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
                val locale = when (this.parameters["language"]?.toLowerCase()) {
                    "english" -> Locale.forLanguageTag("en-GB")
                    "dutch" -> Locale.forLanguageTag("nl-NL")
                    else -> null
                }

                if (locale == null) {
                    this.event.message.channel.awaitSingle()
                        .createMessage(translate("language.unknown", this.event.member.get()).format(
                            this.parameters["language"],
                            this.getAvailableLocales()
                                .joinToString { "`${it.getDisplayLanguage(Locale.forLanguageTag("en-GB"))}`" }
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
                        translate(
                            "language.user.updated",
                            this.event.member.get()
                        ).format(locale.getDisplayLanguage(Locale.forLanguageTag("en-GB")))
                    )
                    .awaitSingle()
            }
        }
    }
}