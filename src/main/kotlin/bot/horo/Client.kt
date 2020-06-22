package bot.horo

import bot.horo.command.CommandContext
import bot.horo.command.commands
import bot.horo.command.traverse
import discord4j.core.DiscordClient
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.core.`object`.presence.Activity
import discord4j.core.`object`.presence.Presence
import discord4j.core.event.domain.guild.GuildCreateEvent
import discord4j.core.event.domain.lifecycle.ConnectEvent
import discord4j.core.event.domain.lifecycle.DisconnectEvent
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
import java.lang.RuntimeException
import java.lang.management.ManagementFactory
import kotlin.concurrent.thread
import kotlin.math.floor
import kotlin.time.*

@ExperimentalTime
class Client {
    private val logger = LoggerFactory.getLogger(Client::class.java)

    init {
        DiscordClient.create(System.getenv("token"))
            .gateway()
            .setSharding(ShardingStrategy.recommended())
            .setEnabledIntents(IntentSet.of(Intent.GUILDS, Intent.GUILD_MESSAGES))
            .setInitialStatus { Presence.doNotDisturb(Activity.playing("Loading... Please wait...")) }
            .withEventDispatcher { dispatcher ->
                mono(CoroutineName("EventDispatcherCoroutine")) {
                    launch(CoroutineName("ReadyCoroutine")) {
                        dispatcher.on(ReadyEvent::class.java)
                            .asFlow()
                            .collect {
                                logger.info("Shard #${it.shardInfo.index} is ready!")
                                it.client.updatePresence(
                                    Presence.online(Activity.listening(".horohelp for help | Shard #${it.shardInfo.index}")),
                                    it.shardInfo.index
                                ).awaitFirstOrNull()
                            }
                    }

                    launch(CoroutineName("ConnectCoroutine")) {
                        dispatcher.on(ConnectEvent::class.java)
                            .asFlow()
                            .collect {
                                logger.info("Shard #${it.shardInfo.index} has connected")
                            }
                    }

                    launch(CoroutineName("DisconnectCoroutine")) {
                        dispatcher.on(DisconnectEvent::class.java)
                            .asFlow()
                            .collect {
                                val message =
                                    "Shard #${it.shardInfo.index} has disconnected (${it.status.code}: ${it.status.reason.orElse(
                                        "No reason specified"
                                    )})"
                                if (it.cause.isPresent)
                                    logger.error(message, it.cause.get())
                                else
                                    logger.error(message)
                            }
                    }

                    launch(CoroutineName("ReconnectCoroutine")) {
                        dispatcher.on(ReconnectEvent::class.java)
                            .asFlow()
                            .collect {
                                logger.info("Shard #${it.shardInfo.index} has reconnected")
                                it.client.updatePresence(
                                    Presence.online(Activity.listening(".horohelp for help | Shard #${it.shardInfo.index}")),
                                    it.shardInfo.index
                                ).awaitFirstOrNull()
                            }
                    }

                    launch(CoroutineName("GuildCreateCoroutine")) {
                        dispatcher.on(GuildCreateEvent::class.java)
                            .asFlow()
                            .collect {
                                logger.info(
                                    "Guild \"${it.guild.name}\" created, shard at ${it.client.guilds.count()
                                        .awaitSingle()} guilds (${it.client.gatewayClientGroup.shardCount} total)"
                                )
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
            .doOnNext { client ->
                thread {
                    var input = readLine()
                    while (input != null) {
                        when (input.toLowerCase()) {
                            "count" -> println(
                                "Currently have ${client.guilds.count().block()} guilds and ${client.users.count()
                                    .block()} users"
                            )
                            "uptime" -> {
                                val uptime = ManagementFactory.getRuntimeMXBean().uptime.milliseconds
                                println(
                                    "Client has been online for %s days %s hours %s minutes %s seconds".format(
                                        floor(uptime.inDays).toInt(),
                                        floor(uptime.inHours).toInt(),
                                        floor(uptime.inMinutes).toInt(),
                                        floor(uptime.inSeconds).toInt()
                                    )
                                )
                            }
                        }
                        input = readLine()
                    }
                }
            }
            .block()!!
            .onDisconnect()
            .block()
    }

    private suspend fun handleMessage(event: MessageCreateEvent) {
        val command = commands.traverse(event.message.content.removePrefix(".horo").substringBefore(" --"))
        if (command == null) {
            event.message.restChannel.createMessage(
                EmbedData.builder()
                    .title("Unknown command")
                    .description("Are you sure you typed that right?")
                    .color(Color.YELLOW.rgb)
                    .build()
            ).awaitSingle()
            return
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

        if (command.parameters.size != parametersSupplied.size ||
            !command.parameters.all { parameter ->
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
        } catch (exception: RuntimeException) {
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