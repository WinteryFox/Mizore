package bot.mizore.bot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import com.kotlindiscord.kord.extensions.storage.DataAdapter
import com.kotlindiscord.kord.extensions.utils.loadModule
import dev.kord.core.Kord
import dev.kord.gateway.Intents
import org.koin.dsl.bind

class Mizore(settings: ExtensibleBotBuilder, token: String) : ExtensibleBot(settings, token) {
    override suspend fun start() {
        settings.hooksBuilder.runBeforeStart(this)

        if (!initialized) registerListeners()

        getKoin().get<Kord>().login {
            this.presence(settings.presenceBuilder)
            this.intents = Intents(settings.intentsBuilder!!)
        }
    }
}

class MizoreBuilder : ExtensibleBotBuilder() {
    override suspend fun build(token: String): Mizore {
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

        val bot = Mizore(this, token)

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

suspend fun Mizore(token: String, builder: suspend MizoreBuilder.() -> Unit): Mizore {
    val settings = MizoreBuilder()

    builder(settings)

    return settings.build(token)
}
