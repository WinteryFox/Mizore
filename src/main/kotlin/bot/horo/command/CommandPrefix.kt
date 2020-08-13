package bot.horo.command

import bot.horo.QUESTION_MARK
import bot.horo.data.getSettings
import discord4j.rest.util.Color
import discord4j.rest.util.Permission
import kotlinx.coroutines.reactive.awaitSingle

fun CommandsBuilder.prefix() {
    command("prefix") {
        dispatch {
            val title = localization.translate("prefix.title", this.event.member.get())
            val footer = localization.translate("prefix.footer", this.event.member.get())
            val description =
                this.event.guild.awaitSingle().getSettings(this.database).prefixes.joinToString { "`$it`" }

            this.event.message.channel.awaitSingle()
                .createEmbed { embed ->
                    embed.setTitle(title)
                        .setDescription(description)
                        .setFooter(footer, QUESTION_MARK)
                        .setColor(Color.PINK)
                }
                .awaitSingle()
        }

        subcommand("add") {
            userPermission(Permission.MANAGE_GUILD)
            parameter("prefix", true)

            dispatch {
                if (this.parameters["prefix"]!!.length > 5) {
                    this.event.message.channel.awaitSingle()
                        .createMessage(this.localization.translate("prefix.length", this.event.member.get()))
                        .awaitSingle()
                    return@dispatch
                }

                if (this.database.execute(
                        "INSERT INTO prefixes (snowflake, prefix) VALUES ($1, $2) ON CONFLICT DO NOTHING",
                        mapOf(
                            Pair("$1", this.event.guildId.get()),
                            Pair("$2", this.parameters["prefix"]!!)
                        )
                    ) == 1
                )
                    this.event.message.channel.awaitSingle()
                        .createMessage(
                            localization.translate(
                                "prefix.added",
                                this.event.member.get()
                            ).format(this.parameters["prefix"])
                        )
                        .awaitSingle()
                else
                    this.event.message.channel.awaitSingle()
                        .createMessage(
                            localization.translate(
                                "prefix.duplicate",
                                this.event.member.get()
                            ).format(this.parameters["prefix"])
                        )
                        .awaitSingle()
            }
        }

        subcommand("remove") {
            userPermission(Permission.MANAGE_GUILD)
            parameter("prefix", true)

            dispatch {
                if (this.database.execute(
                        "DELETE FROM prefixes WHERE snowflake = $1 AND prefix = $2",
                        mapOf(
                            Pair("$1", this.event.guildId.get()),
                            Pair("$2", this.parameters["prefix"]!!)
                        )
                    ) == 1
                )
                    this.event.message.channel.awaitSingle()
                        .createMessage(
                            localization.translate(
                                "prefix.removed",
                                this.event.member.get()
                            ).format(this.parameters["prefix"])
                        )
                        .awaitSingle()
                else
                    this.event.message.channel.awaitSingle()
                        .createMessage(
                            localization.translate(
                                "prefix.unknown",
                                this.event.member.get()
                            ).format(this.parameters["prefix"])
                        )
                        .awaitSingle()
            }
        }
    }
}