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
import discord4j.store.redis.RedisStoreService
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import kotlin.concurrent.thread

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
            animals()
        }
    }

    fun login() {
        thread(name = "Client") {
            DiscordClient.create(System.getenv("TOKEN"))
                .gateway()
                .setSharding(ShardingStrategy.recommended())
                .setEnabledIntents(IntentSet.of(Intent.GUILDS, Intent.GUILD_MESSAGES))
                .setInitialStatus { Presence.doNotDisturb(Activity.playing("Loading... Please wait...")) }
                .setStoreService(
                    RedisStoreService
                        .builder()
                        .redisClient(
                            RedisClient.create(RedisURI.create(System.getenv("REDIS_HOST"), 6379))
                        )
                        .build()
                )
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
                                        "Shard #${it.shardInfo.index} has disconnected (${it.status.code}: ${
                                            it.status.reason.orElse(
                                                "No reason specified"
                                            )
                                        })"
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
                                            "Guild \"${it.guild.name}\" created (${
                                                it.client.guilds.count()
                                                    .awaitSingle()
                                            } guilds)"
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
                                        event.message.content.isNotBlank() &&
                                        event.message.channel
                                            .ofType(GuildMessageChannel::class.java)
                                            .awaitFirstOrNull()
                                            ?.getEffectivePermissions(event.client.selfId)
                                            ?.awaitSingle()
                                            ?.containsAll(
                                                setOf(
                                                    Permission.VIEW_CHANNEL,
                                                    Permission.SEND_MESSAGES
                                                )
                                            ) ?: false &&
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
                                                    if ((event.message.channel.awaitSingle() as GuildMessageChannel)
                                                            .getEffectivePermissions(event.client.selfId)
                                                            .awaitSingle()
                                                            .contains(Permission.SEND_MESSAGES))
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
                        "You are missing the following permissions; ${
                            command.userPermissions
                                .andNot(userPermissions)
                                .joinToString { permission ->
                                    permission.name.toLowerCase().capitalize().replace("_", " ")
                                }
                        }"
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
                        "I am missing the following permissions; ${
                            command.botPermissions
                                .andNot(botPermissions)
                                .joinToString { permission ->
                                    permission.name.toLowerCase().capitalize().replace("_", " ")
                                }
                        }"
                    )
                    .setColor(Color.ORANGE)
            }.awaitSingle()
            return
        }

        val parameters = "--(\\w+)\\s+((?:.(?!--\\w))+)".toRegex().findAll(event.message.content)
            .map { it.groupValues[1] to it.groupValues[2] }.toMap().toMutableMap()

        if (parameters.isEmpty() && command.parameters.size == 1) {
            val parameter = event.message.content.substring(
                event.message.content.findAnyOf(command.aliases.plus(command.name))!!
                    .let { it.first + it.second.length }, event.message.content.length
            ).removePrefix(" ")

            if (command.name != parameter && !command.aliases.contains(parameter) && parameter.isNotBlank())
                parameters[command.parameters.keys.first()] = parameter
        }

        if (command.parameters.filter { it.value }.size != parameters.size ||
            !command.parameters.filter { it.value }
                .all { parameter -> parameters.containsKey(parameter.key) }
        ) {
            event.message.channel
                .awaitSingle()
                .createEmbed { embed ->
                    embed.setTitle("Missing arguments")
                        .setDescription("TODO")
                        .setColor(Color.YELLOW)
                }
                .awaitSingle()
            return
        }

        logger.debug("Executing command ${command.name}")
        command.dispatch.invoke(
            CommandContext(
                event,
                parameters,
                database,
                localization
            )
        )
    }
}