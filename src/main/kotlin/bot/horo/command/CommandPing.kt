package bot.horo.command

import kotlinx.coroutines.reactive.awaitSingle

fun CommandsBuilder.ping() {
    command("ping") {
        dispatch {
            this.event.message.channel.awaitSingle()
                .createMessage(
                    "Pong! My ping is ${event.client.getGatewayClient(this.event.shardInfo.index)
                        .get().responseTime.toMillis()} milliseconds"
                )
                .awaitSingle()
        }
    }
}