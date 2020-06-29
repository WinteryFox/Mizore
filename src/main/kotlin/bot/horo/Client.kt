package bot.horo

import bot.horo.command.*
import bot.horo.data.Database
import bot.horo.data.firstChannel
import bot.horo.data.getSettings
import discord4j.common.util.Snowflake
import discord4j.core.DiscordClient
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.core.`object`.presence.Activity
import discord4j.core.`object`.presence.Presence
import discord4j.core.event.domain.guild.GuildCreateEvent
import discord4j.core.event.domain.guild.GuildDeleteEvent
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
import discord4j.rest.util.Permission
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory
import kotlin.concurrent.thread
import kotlin.math.floor
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

@ExperimentalTime
class Client {
    private val logger = LoggerFactory.getLogger(Client::class.java)
    private val oldGuilds = mutableSetOf<Snowflake>()
    private val database = Database()
    private val localization = Localization(database)

    init {
        registerCommands {
            help()
            ping()
            invite()
            prefix()
            language()
        }
    }

    fun login() {
        thread(name = "Client") {
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
                                .collect { event ->
                                    oldGuilds.addAll(event.guilds.map(ReadyEvent.Guild::getId))

                                    database.batch({ batch ->
                                        oldGuilds.forEach {
                                            batch.add(
                                                """
                                                INSERT INTO guilds (snowflake)
                                                VALUES (${it.asString()})
                                                ON CONFLICT DO NOTHING
                                                RETURNING snowflake
                                                """
                                            )
                                        }
                                    }) { row, _ ->
                                        Snowflake.of(row["snowflake"] as Long)
                                    }.map { snowflake ->
                                        event.client.getGuildById(snowflake)
                                            .awaitSingle()
                                            .firstChannel()
                                            ?.createMessage { spec -> spec.welcomeEmbed }
                                            ?.awaitSingle()
                                    }

                                    event.client.updatePresence(
                                        Presence.online(Activity.listening(".horohelp for help | Shard #${event.shardInfo.index}")),
                                        event.shardInfo.index
                                    ).awaitFirstOrNull()
                                    logger.info("Shard #${event.shardInfo.index} is ready!")
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
                                        logger.info(message, it.cause.get())
                                    else
                                        logger.info(message)
                                }
                        }

                        launch(CoroutineName("ReconnectCoroutine")) {
                            dispatcher.on(ReconnectEvent::class.java)
                                .asFlow()
                                .collect {
                                    it.client.updatePresence(
                                        Presence.online(Activity.listening(".horohelp for help | Shard #${it.shardInfo.index}")),
                                        it.shardInfo.index
                                    ).awaitFirstOrNull()
                                    logger.info("Shard #${it.shardInfo.index} has reconnected")
                                }
                        }

                        launch(CoroutineName("GuildCreateCoroutine")) {
                            dispatcher.on(GuildCreateEvent::class.java)
                                .asFlow()
                                .collect {
                                    if (!oldGuilds.contains(it.guild.id)) {
                                        it.guild.firstChannel()
                                            ?.createMessage { message -> message.welcomeEmbed }
                                            ?.awaitSingle()
                                        oldGuilds.add(it.guild.id)
                                        database.execute(
                                            "INSERT INTO guilds (snowflake) VALUES ($1) ON CONFLICT DO NOTHING",
                                            mapOf(Pair("$1", it.guild.id))
                                        )
                                        logger.info(
                                            "Guild \"${it.guild.name}\" created (${it.client.guilds.count()
                                                .awaitSingle()} guilds)"
                                        )
                                    }
                                }
                        }

                        launch(CoroutineName("GuildDeleteCoroutine")) {
                            dispatcher.on(GuildDeleteEvent::class.java)
                                .asFlow()
                                .collect {
                                    oldGuilds.remove(it.guildId)
                                }
                        }

                        launch(CoroutineName("MessageCreateCoroutine")) {
                            dispatcher.on(MessageCreateEvent::class.java)
                                .asFlow()
                                .filter { event ->
                                    event.member.isPresent &&
                                            !event.member.get().isBot &&
                                            event.message.channel.awaitSingle() is GuildMessageChannel &&
                                            event.message.content.isNotBlank() &&
                                            (event.message.channel.awaitSingle() as GuildMessageChannel)
                                                .getEffectivePermissions(event.client.selfId).awaitSingle()
                                                .contains(Permission.SEND_MESSAGES) &&
                                            event.message.guild.awaitSingle().getSettings(database).prefixes.any {
                                                event.message.content.startsWith(
                                                    it
                                                )
                                            }
                                }
                                .collect { event ->
                                    launch(CoroutineName("CommandCoroutine")) {
                                        event.message.channel.awaitSingle().typeUntil(
                                            mono {
                                                try {
                                                    handleMessage(event)
                                                } catch (exception: RuntimeException) {
                                                    logger.error("Command handler threw an exception", exception)
                                                    event.message.channel.awaitSingle().createEmbed { spec ->
                                                        spec
                                                            .setTitle("Well that didn't go as planned...")
                                                            .setDescription(
                                                                """
                                                                An error occurred while processing your command
                                                                ```${exception.message}```
                                                                Try again later!
                                                                """.trimIndent()
                                                            )
                                                            .setColor(Color.RED)
                                                    }.awaitSingle()
                                                }
                                            }
                                        ).awaitLast()
                                    }
                                }
                        }
                    }
                }
                .login()
                .doOnNext { client ->
                    thread(name = "ConsoleCommand", isDaemon = true) {
                        var input = readLine()
                        while (input != null) {
                            when (input.toLowerCase()) {
                                "stop" -> {
                                    logger.info("Client shutting down...")
                                    client.logout().block()
                                }
                                "count" -> logger.info("Currently have ${client.guilds.count().block()} guilds")
                                "uptime" -> {
                                    val uptime = ManagementFactory.getRuntimeMXBean().uptime.milliseconds
                                    logger.info(
                                        "Client has been online for %s weeks %s days %s hours %s minutes %s seconds".format(
                                            floor(uptime.inDays / 7).toInt(),
                                            floor(uptime.inDays).toInt() % 7,
                                            floor(uptime.inHours).toInt() % 24,
                                            floor(uptime.inMinutes).toInt() % 60,
                                            floor(uptime.inSeconds).toInt() % 60
                                        )
                                    )
                                }
                                else -> logger.info("Unknown command; $input")
                            }
                            input = readLine()
                        }
                    }
                }
                .block()!!
                .onDisconnect()
                .block()
        }
    }

    private suspend inline fun handleMessage(event: MessageCreateEvent) {
        val command = commands.traverse(
            event.message.content.removePrefix(
                event.guild.awaitSingle().getSettings(database).prefixes
                    .filter { event.message.content.startsWith(it) }
                    .maxBy { it.length }!!
            )
                .substringBefore(" --")
        )
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
            !command.parameters.filter { it.value }.all { parameter ->
                parametersSupplied.map { it.groupValues[1] }.contains(parameter.key)
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
        command.dispatch.invoke(
            CommandContext(
                event,
                parametersSupplied.map { it.groupValues[1] to it.groupValues[2] }.toMap(),
                database,
                localization
            )
        )
    }
}