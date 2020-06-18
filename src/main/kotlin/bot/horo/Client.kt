package bot.horo

import bot.horo.command.CommandContext
import bot.horo.command.commands
import discord4j.core.DiscordClient
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.core.`object`.presence.Activity
import discord4j.core.`object`.presence.Presence
import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.core.event.domain.lifecycle.ReconnectEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.shard.ShardingStrategy
import discord4j.discordjson.json.EmbedData
import discord4j.gateway.intent.Intent
import discord4j.gateway.intent.IntentSet
import discord4j.rest.util.Color
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationTargetException

class Client {
    private val logger = LoggerFactory.getLogger(Client::class.java)
    private val client =
        DiscordClient.create(System.getenv("token"))
            .gateway()
            .setSharding(ShardingStrategy.recommended())
            .setEnabledIntents(IntentSet.of(Intent.GUILDS, Intent.GUILD_MESSAGES))
            .setInitialStatus { Presence.doNotDisturb(Activity.playing("Loading... Please wait...")) }

    fun login() {
        client
            .withEventDispatcher { dispatcher ->
                mono(CoroutineName("EventDispatcherCoroutine")) {
                    launch(CoroutineName("ReadyCoroutine")) {
                        dispatcher.on(ReadyEvent::class.java)
                            .asFlow()
                            .collect {
                                logger.info("Shard ${it.shardInfo.index} is ready!")
                                it.client.updatePresence(
                                    Presence.online(Activity.listening(".horohelp for help | Shard ${it.shardInfo.index}")),
                                    it.shardInfo.index
                                ).awaitFirstOrNull()
                            }
                    }

                    launch(CoroutineName("ReconnectCoroutine")) {
                        dispatcher.on(ReconnectEvent::class.java)
                            .asFlow()
                            .collect {
                                logger.info("Shard ${it.shardInfo.index} has reconnected")
                                it.client.updatePresence(
                                    Presence.online(Activity.listening(".horohelp for help | Shard ${it.shardInfo.index}")),
                                    it.shardInfo.index
                                ).awaitFirstOrNull()
                            }
                    }

                    launch(CoroutineName("MessageCreateCoroutine")) {
                        dispatcher.on(MessageCreateEvent::class.java)
                            .asFlow()
                            .filter {
                                it.member.isPresent &&
                                        !it.member.get().isBot &&
                                        it.message.channel.awaitSingle() is GuildMessageChannel &&
                                        it.message.content.isNotBlank() &&
                                        it.message.content.startsWith(".horo")
                            }
                            .collect { event ->
                                launch(CoroutineName("CommandCoroutine")) {
                                    handleMessage(event)
                                }
                            }
                    }
                }
            }
            .login()
            .block()!!
            .onDisconnect()
            .block()
    }

    private suspend fun handleMessage(event: MessageCreateEvent) {
        var command = commands.find { c -> event.message.content.startsWith(".horo${c.name}") }

        if (command == null) {
            // do fuzzy match here
            val userInput: String = event.message.content
            val boundary: Int = if (userInput.indexOf(" ") == -1) userInput.length else userInput.indexOf(" ")
            val userCmd: String = userInput.substring(".horo".length, boundary)

            logger.debug("No exact command match for $userInput")
            command = commands.map { cmd -> Pair(cmd, cmd.name.fuzzyScore(userCmd)) }
                .filter { pair -> pair.second > 1 }
                .maxBy { pair -> pair.second }
                ?.first

            if (command == null) { // if not null, exit this and continue executing the corrected command
                logger.debug("Fuzzy matched failed to find a match for $userCmd")
                event.message.restChannel.createMessage(
                    EmbedData.builder()
                        .title("Unknown command")
                        .description("Are you sure you typed that right?")
                        .color(Color.YELLOW.rgb)
                        .build()
                ).awaitSingle()
                return
            }
        }

        val channel = event.message.channel.awaitSingle() as GuildMessageChannel
        val userPermissions = channel.getEffectivePermissions(event.member.get().id).awaitSingle()
        if (!userPermissions.containsAll(command.userPermissions.toList())) {
            channel.createEmbed { spec ->
                spec.setTitle("You are missing permissions!")
                    .setDescription(
                        "You are missing the following permissions; ${command.userPermissions
                            .andNot(userPermissions)
                            .joinToString { permission ->
                                permission.name.toLowerCase().capitalize().replace("_", " ")
                            }}"
                    )
                    .setColor(Color.ORANGE)
            }.awaitSingle()
            return
        }

        val botPermissions = channel.getEffectivePermissions(event.client.selfId).awaitSingle()
        if (!botPermissions.containsAll(command.botPermissions.toList())) {
            channel.createEmbed { spec ->
                spec.setTitle("I am missing permissions!")
                    .setDescription(
                        "I am missing the following permissions; ${command.botPermissions
                            .andNot(botPermissions)
                            .joinToString { permission ->
                                permission.name.toLowerCase().capitalize().replace("_", " ")
                            }}"
                    )
                    .setColor(Color.ORANGE)
            }.awaitSingle()
            return
        }

        val parametersSupplied = "--(\\w+)\\s+((?:.(?!--\\w))+)".toRegex().findAll(event.message.content).toList()

        if (command.flags.size != parametersSupplied.size ||
            !command.flags.all { parameter ->
                parametersSupplied.map { it.groupValues[1] }.contains(parameter)
            }
        ) {
            event.message.restChannel.createMessage(
                EmbedData.builder()
                    .title("Missing arguments")
                    .description("TODO (Kat was here)")
                    .color(Color.YELLOW.rgb)
                    .build()
            ).awaitSingle()
            return
        }

        logger.debug("Executing command ${command.name}")
        try {
            command.dispatch.invoke(
                CommandContext(
                    event,
                    parametersSupplied.map { it.groupValues[1] to it.groupValues[2] }.toMap()
                )
            )
        } catch (exception: InvocationTargetException) {
            logger.error("Command handler threw an exception", exception.cause!!)
            channel.createEmbed { spec ->
                spec
                    .setTitle("Well that didn't go as planned...")
                    .setDescription(
                        """
                            An error occurred while processing your command; ${exception.cause!!.message}
                            Try again later!
                            """.trimIndent()
                    )
                    .setColor(Color.RED)
            }.awaitSingle()
        }
    }
}