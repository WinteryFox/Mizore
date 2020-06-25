package bot.horo.command

import bot.horo.BOT_INVITE
import kotlinx.coroutines.reactive.awaitSingle

fun CommandsBuilder.invite() =
    command("invite") {
        dispatch {
            this.event.message.channel.awaitSingle()
                .createMessage(BOT_INVITE)
                .awaitSingle()
        }
    }