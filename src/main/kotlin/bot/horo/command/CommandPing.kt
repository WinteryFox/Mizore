package bot.horo.command

import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.reactive.awaitSingle

@Command
suspend fun ping(event: MessageCreateEvent) {
    event.message.restChannel
        .createMessage("Pong!")
        .awaitSingle()
}