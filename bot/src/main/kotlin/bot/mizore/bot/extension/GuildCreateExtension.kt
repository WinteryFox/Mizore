package bot.mizore.bot.extension

import bot.mizore.bot.Api
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.core.event.guild.GuildCreateEvent

class GuildCreateExtension : Extension() {
    override val name: String = "guild_create"

    override suspend fun setup() {
        event<GuildCreateEvent> {
            action {
                Api.postGuild(event.guild.id)
            }
        }
    }
}
