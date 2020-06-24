package bot.horo.command

import kotlinx.coroutines.reactive.awaitSingle

fun CommandsBuilder.invite() =
    command("invite") {
        dispatch {
            this.event.message.channel.awaitSingle()
                .createMessage("https://discord.com/oauth2/authorize?client_id=715343710660853781&scope=bot&permissions=52224")
                .awaitSingle()
        }
    }