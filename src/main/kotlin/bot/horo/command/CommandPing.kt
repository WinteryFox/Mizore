package bot.horo.command

import kotlinx.coroutines.reactive.awaitSingle

fun CommandsBuilder.ping() {
    command("ping") {
        dispatch {
            this.event.message.channel.awaitSingle()
                .createMessage(
                    "Pong! My ping is %s milliseconds".format(
                        this.event.client.getGatewayClient(this.event.shardInfo.index)
                            .get().responseTime.toMillis()
                    )
                )
                .awaitSingle()
        }
    }
}