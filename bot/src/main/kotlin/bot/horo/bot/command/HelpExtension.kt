package bot.horo.bot.command

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.Color
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed

class HelpExtension : Extension() {
    override val name: String = "horo.help"
    override val bundle: String = "horo.help"

    override suspend fun setup() {
        ephemeralSlashCommand {
            name = "name"
            description = "description"

            action {
                respond {
                    content = translate("message.content")
                    embed {
                        title = translate("embed.title")
                        description = translate("embed.description")
                        color = Color(202, 117, 201)
                        thumbnail {
                            url = "https://raw.githubusercontent.com/WinteryFox/Horo/master/icon.png"
                        }
                    }

                    actionRow {
                        linkButton("https://discord.gg/6vJXZ8d") {
                            label = translate("embed.support")
                        }
                    }
                }
            }
        }
    }
}
