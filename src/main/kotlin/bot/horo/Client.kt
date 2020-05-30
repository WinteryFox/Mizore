package bot.horo

import bot.horo.command.Command
import bot.horo.command.Parameter
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
import discord4j.rest.util.PermissionSet
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import kotlin.reflect.full.*
import kotlin.reflect.jvm.kotlinFunction

@ExperimentalStdlibApi
internal val commands = Reflections(
    ConfigurationBuilder().setUrls(ClasspathHelper.forJavaClassPath()).setScanners(MethodAnnotationsScanner())
)
    .getMethodsAnnotatedWith(Command::class.java)
    .map { method ->
        method.name to method.kotlinFunction!!
    }
    .toMap()

@ExperimentalStdlibApi
class Client {
    private val client = DiscordClient.create(System.getenv("token"))

    init {
        client.gateway()
            .setSharding(ShardingStrategy.recommended())
            .setEnabledIntents(IntentSet.of(Intent.GUILDS, Intent.GUILD_MESSAGES))
            .setInitialStatus { Presence.doNotDisturb(Activity.playing("Loading... Please wait...")) }
            .withGateway { gateway ->
                mono {
                    launch {
                        gateway.on(ReadyEvent::class.java)
                            .asFlow()
                            .collect {
                                it.client.updatePresence(Presence.online(Activity.listening(".horohelp for help")))
                                    .awaitFirstOrNull()
                            }
                    }

                    launch {
                        gateway.on(ReconnectEvent::class.java)
                            .asFlow()
                            .collect {
                                gateway.updatePresence(Presence.online(Activity.listening(".horohelp for help")))
                                    .awaitFirstOrNull()
                            }
                    }

                    launch {
                        gateway.on(MessageCreateEvent::class.java)
                            .asFlow()
                            .filter {
                                it.member.isPresent &&
                                        !it.member.get().isBot &&
                                        it.message.channel.awaitSingle() is GuildMessageChannel &&
                                        it.message.content.isNotBlank() &&
                                        it.message.content.startsWith(".horo")
                            }
                            .collect { event ->
                                val command =
                                    commands.values.find { c -> event.message.content.startsWith(".horo${c.name}") }

                                if (command != null) {
                                    val channel = event.message.channel.awaitSingle() as GuildMessageChannel
                                    val botPermissions =
                                        channel.getEffectivePermissions(event.client.selfId).awaitSingle()
                                    val userPermissions =
                                        channel.getEffectivePermissions(event.member.get().id).awaitSingle()
                                    val annotation = command.findAnnotation<Command>()!!

                                    if (botPermissions.containsAll(annotation.botPermissions.toList()))
                                        if (userPermissions.containsAll(annotation.userPermissions.toList())) {
                                            val parametersSupplied =
                                                "--(\\w+)\\s+((?:.(?!--\\w))+)".toRegex().findAll(event.message.content)
                                                    .toList()
                                            val parameters =
                                                command.valueParameters
                                                    .filter { it.hasAnnotation<Parameter>() }
                                                    .map { it.name }

                                            if (parameters.size != parametersSupplied.size ||
                                                !parameters.all { parameter ->
                                                    parametersSupplied.map { it.groupValues[1] }
                                                        .contains(parameter)
                                                }
                                            ) {
                                                event.message.restChannel.createMessage(
                                                    EmbedData.builder()
                                                        .title("Missing arguments")
                                                        .description("TODO")
                                                        .color(Color.RED.rgb)
                                                        .build()
                                                ).awaitSingle()
                                                return@collect
                                            }

                                            command.callSuspendBy(
                                                mapOf(
                                                    command.findParameterByName("event")!! to event,
                                                    *parametersSupplied.map { parameter ->
                                                        command.findParameterByName(parameter.groupValues[1])!! to parameter.groupValues[2]
                                                    }.toTypedArray()
                                                )
                                            )
                                        } else
                                            channel.createEmbed { spec ->
                                                spec.setTitle("You are missing permissions!")
                                                    .setDescription(
                                                        "You are missing the following permissions; ${PermissionSet.of(*annotation.userPermissions)
                                                            .andNot(userPermissions)
                                                            .joinToString { permission ->
                                                                permission.name.toLowerCase().capitalize()
                                                            }}"
                                                    )
                                                    .setColor(Color.RED)
                                            }.awaitSingle()
                                    else
                                        channel.createEmbed { spec ->
                                            spec.setTitle("I am missing permissions!")
                                                .setDescription(
                                                    "I am missing the following permissions; ${PermissionSet.of(*annotation.botPermissions)
                                                        .andNot(botPermissions)
                                                        .joinToString { permission ->
                                                            permission.name.toLowerCase().capitalize().replace("_", " ")
                                                        }}"
                                                )
                                                .setColor(Color.RED)
                                        }.awaitSingle()
                                } else
                                    event.message.channel.awaitSingle().createEmbed { spec ->
                                        spec.setTitle("Unknown command")
                                            .setDescription("Are you sure you typed that right?")
                                            .setColor(Color.RED)
                                    }.awaitSingle()
                            }
                    }
                }
            }
            .block()
    }
}