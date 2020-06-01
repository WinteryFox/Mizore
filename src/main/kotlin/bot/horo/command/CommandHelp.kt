package bot.horo.command

import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.discordjson.json.EmbedData
import discord4j.rest.util.Color
import kotlinx.coroutines.reactive.awaitSingle
import java.lang.reflect.Method

@Command
suspend fun help(event: MessageCreateEvent) {
    event.message.restChannel
        .createMessage(
            EmbedData.builder()
                .title("Here's a list of my commands")
                .description(
                    commands.stream()
                        .map { command ->
                            ".horo${command.name}"
                        }
                        .reduce { t, u -> t + "\n" + u }
                        .get()
                )
                .color(Color.of(202, 117, 201).rgb)
                .build()
        )
        .awaitSingle()
}

fun generateHelp(command: Method) {

}