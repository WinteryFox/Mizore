package bot.horo.command

import bot.horo.QUESTION_MARK
import bot.horo.data.getSettings
import discord4j.rest.util.Color
import kotlinx.coroutines.reactive.awaitSingle

fun CommandsBuilder.prefix() {
    command("prefix") {
        dispatch {
            val title = translate("prefix.title", this.event.member.get())
            val footer = translate("prefix.footer", this.event.member.get())
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
            parameter("prefix", true)

            dispatch {
                if (this.database.execute(
                        "INSERT INTO prefixes (snowflake, prefix) VALUES ($1, $2) ON CONFLICT DO NOTHING",
                        mapOf(
                            Pair("$1", this.event.guildId.get().asLong()),
                            Pair("$2", this.parameters["prefix"]!!)
                        )
                    ) == 1
                )
                    this.event.message.channel.awaitSingle()
                        .createMessage(
                            translate(
                                "prefix.added",
                                this.event.member.get()
                            ).format(this.parameters["prefix"])
                        )
                        .awaitSingle()
                else
                    this.event.message.channel.awaitSingle()
                        .createMessage(
                            translate(
                                "prefix.duplicate",
                                this.event.member.get()
                            ).format(this.parameters["prefix"])
                        )
                        .awaitSingle()
            }
        }

        subcommand("remove") {
            parameter("prefix", true)

            dispatch {
                if (this.database.execute(
                        "DELETE FROM prefixes WHERE snowflake = $1 AND prefix = $2",
                        mapOf(
                            Pair("$1", this.event.guildId.get().asLong()),
                            Pair("$2", this.parameters["prefix"]!!)
                        )
                    ) == 1
                )
                    this.event.message.channel.awaitSingle()
                        .createMessage(
                            translate(
                                "prefix.removed",
                                this.event.member.get()
                            ).format(this.parameters["prefix"])
                        )
                        .awaitSingle()
                else
                    this.event.message.channel.awaitSingle()
                        .createMessage(
                            translate(
                                "prefix.unknown",
                                this.event.member.get()
                            ).format(this.parameters["prefix"])
                        )
                        .awaitSingle()
            }
        }
    }
}

/*
private suspend fun MessageChannel.createMessageSuspend(spec: suspend (MessageCreateSpec) -> MessageCreateSpec): Message {
    val res = spec(MessageCreateSpec())

    val consumer = Consumer<MessageCreateSpec> { s ->
        s.setContent(res.getDataSomehow().content)
            .doThisForEveryOption()...
    }

    return this.createMessage(consumer).awaitSingle()
}*/
