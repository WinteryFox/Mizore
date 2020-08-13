package bot.horo.command

import bot.horo.data.getSettings
import discord4j.rest.util.Color
import discord4j.rest.util.Permission
import kotlinx.coroutines.reactive.awaitSingle
import java.util.*

fun CommandsBuilder.language() {
    suspend fun getLocale(context: CommandContext): Locale? {
        val locale =
            context.localization.locales.firstOrNull { it.displayLanguage.toLowerCase() == context.parameters["language"]!! }

        if (locale == null) {
            context.event.message.channel.awaitSingle()
                .createMessage(context.localization.translate("language.unknown", context.event.member.get()).format(
                    context.parameters["language"],
                    context.localization.locales.joinToString {
                        "`${it.getDisplayLanguage(
                            Locale.forLanguageTag(
                                "en-GB"
                            )
                        )}`"
                    }
                ))
                .awaitSingle()
        }

        return locale
    }

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
                val locale = getLocale(this) ?: return@dispatch

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

        subcommand("server") {
            userPermission(Permission.MANAGE_GUILD)
            parameter("language", true)

            dispatch {
                val locale = getLocale(this) ?: return@dispatch

                this.database.execute(
                    """
                        INSERT INTO guilds (snowflake, locale)
                        VALUES ($1, $2)
                        ON CONFLICT (snowflake) DO UPDATE SET locale = $2
                    """,
                    mapOf(Pair("$1", this.event.guildId.get()), Pair("$2", locale.toLanguageTag()))
                )

                this.event.message.channel.awaitSingle()
                    .createMessage(
                        localization.translate("language.guild.updated", this.event.member.get())
                            .format(locale.getDisplayLanguage(locale))
                    )
                    .awaitSingle()
            }

            subcommand("remove") {
                userPermission(Permission.MANAGE_GUILD)

                dispatch {
                    this.database.execute(
                        """
                        INSERT INTO guilds (snowflake, locale)
                        VALUES ($1, null)
                        ON CONFLICT (snowflake) DO UPDATE SET locale = null
                        """,
                        mapOf(Pair("$1", this.event.guildId.get()))
                    )

                    this.event.message.channel.awaitSingle()
                        .createMessage(localization.translate("language.guild.removed", this.event.member.get()))
                        .awaitSingle()
                }
            }
        }
    }
}