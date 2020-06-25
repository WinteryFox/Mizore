package bot.horo.command

import kotlinx.coroutines.reactive.awaitSingle

fun CommandsBuilder.ping() {
    command("ping") {
        dispatch {
            this.event.message.channel.awaitSingle()
                .createMessage(
                    translate("ping", this.event.member.get()).format(
                        event.client.getGatewayClient(this.event.shardInfo.index).get().responseTime.toMillis()
                    )
                )
                .awaitSingle()
        }
    }
}