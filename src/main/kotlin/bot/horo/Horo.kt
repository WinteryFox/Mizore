package bot.horo

import bot.horo.command.Help
import com.kotlindiscord.kord.extensions.ExtensibleBot
import dev.kord.common.Locale
import kotlinx.coroutines.runBlocking

fun main(): Unit = runBlocking {
    val bot = ExtensibleBot(System.getenv("TOKEN")) {
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
            add(::Help)
        }
    }
    bot.start()
}
