package bot.horo

import bot.horo.command.commands
import bot.horo.command.registerCommands
import discord4j.rest.util.Color
import kotlinx.coroutines.reactive.awaitSingle

fun main() {
    registerCommands {
        command("ping") {
            dispatch {
                this.event.message.channel.awaitSingle().createMessage(
                    "Pong! My ping is %s milliseconds %s".format(
                        this.event.client.getGatewayClient(this.event.shardInfo.index).get().responseTime.toMillis()
                    )
                ).awaitSingle()
            }
        }

        command("help") {
            dispatch {
                this.event.message.channel.awaitSingle()
                    .createEmbed { embed ->
                        embed.setTitle("Here's a list of my commands")
                            .setDescription(commands.joinToString(separator = "\n") { command -> ".horo${command.name}" })
                            .setColor(Color.PINK)
                    }.awaitSingle()
            }
        }
    }

    Client().login()
}