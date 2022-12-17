package bot.horo.bot

import bot.horo.bot.command.*
import dev.kord.common.Locale
import dev.kord.common.entity.PresenceStatus
import kotlinx.coroutines.runBlocking

fun main(): Unit = runBlocking {
    val bot = Horo(System.getenv("TOKEN")) {
        applicationCommands {
            defaultGuild(System.getenv("DEFAULT_GUILD"))
        }

        i18n {
            interactionUserLocaleResolver()

            applicationCommandLocale(Locale.ENGLISH_UNITED_STATES)
            applicationCommandLocale(Locale.JAPANESE)
        }

        extensions {
            help { enableBundledExtension = false }
            add(::HelpExtension)
            add(::ProfileExtension)
            add(::SelfRoleExtension)
        }

        presence {
            status = PresenceStatus.Online
            listening("/help")
        }
    }
    bot.start()
    /*Amqp.consume("interactions") { tag, delivery ->
        val message = delivery.body.decodeToString()
        logger.trace { "Received a new message with tag $tag and content $message" }
        val interaction = Json.decodeFromString(DiscordInteraction.serializer(), message)
        bot.send()
    }*/
}
