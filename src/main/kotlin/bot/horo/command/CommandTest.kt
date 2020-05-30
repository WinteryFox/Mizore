package bot.horo.command

import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.discordjson.json.EmbedData
import kotlinx.coroutines.reactive.awaitSingle

@Command
suspend fun test(
    event: MessageCreateEvent,
    @Parameter
    title: String,
    @Parameter
    description: String
) {
    event.message.restChannel.createMessage(
        EmbedData.builder()
            .title(title)
            .description(description)
            .build()
    ).awaitSingle()
}