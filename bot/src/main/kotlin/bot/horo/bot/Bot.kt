package bot.horo.bot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import com.kotlindiscord.kord.extensions.storage.DataAdapter
import com.kotlindiscord.kord.extensions.utils.loadModule
import dev.kord.core.Kord
import dev.kord.gateway.Intents
import org.koin.dsl.bind

class Horo(settings: ExtensibleBotBuilder, token: String) : ExtensibleBot(settings, token) {
    override suspend fun start() {
        settings.hooksBuilder.runBeforeStart(this)

        if (!initialized) registerListeners()

        getKoin().get<Kord>().login {
            this.presence(settings.presenceBuilder)
            this.intents = Intents(settings.intentsBuilder!!)
        }
    }
}

class HoroBuilder : ExtensibleBotBuilder() {
    override suspend fun build(token: String): Horo {
        hooksBuilder.beforeKoinSetup {  // We have to do this super-duper early for safety
            loadModule { single { dataAdapterCallback() } bind DataAdapter::class }
        }

        hooksBuilder.beforeKoinSetup {
            if (pluginBuilder.enabled) {
                loadPlugins()
            }

            deferredExtensionsBuilders.forEach { it(extensionsBuilder) }
        }

        setupKoin()

        val bot = Horo(this, token)

        loadModule { single { bot } bind ExtensibleBot::class }

        hooksBuilder.runCreated(bot)

        bot.setup()

        hooksBuilder.runSetup(bot)
        hooksBuilder.runBeforeExtensionsAdded(bot)

        @Suppress("TooGenericExceptionCaught")
        extensionsBuilder.extensions.forEach {
            try {
                bot.addExtension(it)
            } catch (e: Exception) {
                logger.error(e) {
                    "Failed to set up extension: $it"
                }
            }
        }

        if (pluginBuilder.enabled) {
            startPlugins()
        }

        hooksBuilder.runAfterExtensionsAdded(bot)

        return bot
    }
}

suspend fun Horo(token: String, builder: suspend HoroBuilder.() -> Unit): Horo {
    val settings = HoroBuilder()

    builder(settings)

    return settings.build(token)
}
