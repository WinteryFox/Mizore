package bot.horo.command

import bot.horo.GUILD_INVITE
import bot.horo.Localization
import bot.horo.data.Database
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.MessageCreateSpec
import discord4j.rest.util.Color
import discord4j.rest.util.Permission
import discord4j.rest.util.PermissionSet

internal val commands = mutableListOf<Command>()
fun MutableList<Command>.traverse(input: String): Command? {
    var sequence = input.splitToSequence(" ")
    var command = this.find { it.name == sequence.first() || it.aliases.contains(sequence.first()) } ?: return null
    sequence = sequence.drop(1)

    for (seq in sequence) {
        command = command.children.find { it.name == seq || it.aliases.contains(seq) } ?: return command
    }

    return command
}

class Command(
        val name: String,
        val aliases: Set<String>,
        val dispatch: suspend CommandContext.() -> Unit,
        val parameters: MutableMap<String, Boolean> = mutableMapOf(),
        val botPermissions: PermissionSet = PermissionSet.of(Permission.SEND_MESSAGES),
        val userPermissions: PermissionSet = PermissionSet.of(Permission.SEND_MESSAGES),
        val children: MutableList<Command> = mutableListOf()
) {
    var parent: Command? = null

    init {
        children.forEach {
            it.parent = this
        }
    }
}

class CommandBuilder(private val name: String) {
    private val aliases = mutableSetOf<String>()
    private lateinit var dispatch: suspend CommandContext.() -> Unit
    private val parameters = mutableMapOf<String, Boolean>()
    private val children = mutableListOf<Command>()
    private val botPermissions = mutableSetOf<Permission>()
    private val userPermissions = mutableSetOf<Permission>()

    fun alias(alias: String) {
        this.aliases.add(alias)
    }

    fun dispatch(dispatch: suspend CommandContext.() -> Unit) {
        this.dispatch = dispatch
    }

    fun parameter(name: String, required: Boolean): Parameter {
        this.parameters[name] = required
        return Parameter(name, required)
    }

    fun botPermissions(vararg permission: Permission) {
        this.botPermissions.addAll(permission)
    }

    fun userPermission(vararg permission: Permission) {
        this.userPermissions.addAll(permission)
    }

    fun subcommand(name: String, dsl: CommandBuilder.() -> Unit) = children.add(CommandBuilder(name).apply(dsl).build())

    fun build() = Command(
            name,
            aliases,
            dispatch,
            parameters,
            PermissionSet.of(*botPermissions.toTypedArray()),
            PermissionSet.of(*userPermissions.toTypedArray()),
            children
    )
}

object CommandsBuilder {
    fun command(name: String, dsl: CommandBuilder.() -> Unit) {
        commands.add(CommandBuilder(name).apply(dsl).build())
    }
}

data class Parameter(val name: String, val required: Boolean)

data class CommandContext(
        val event: MessageCreateEvent,
        val parameters: Map<String, String>,
        val database: Database,
        val localization: Localization
) {
    fun Parameter.get(): String {
        return parameters.getValue(name)
    }
}

fun registerCommands(builder: CommandsBuilder.() -> Unit) {
    CommandsBuilder.apply(builder)
}

val MessageCreateSpec.welcomeEmbed
    get(): MessageCreateSpec =
        this.setContent("Can't see this message? Enable embeds by turning on `Settings > Text & Images > Show website preview info from links pasted into chat`")
                .setEmbed { embed ->
                    embed.setTitle("Thanks for letting me in!")
                            .setDescription("I'm a bot primarily focused on bringing a fun and interactive tamagotchi (digital pet) system to the table!")
                            .addField(
                                    "Quick start",
                                    "To get started type `.horohelp` to see a list of my commands and a short usage guide.",
                                    false
                            )
                            .addField(
                                    "Having issues or need help?",
                                    "If any issues, bugs or questions arise, feel free to join the [support server]($GUILD_INVITE) to report bugs, ask your questions or just hang out and chat.",
                                    false
                            )
                            .setColor(Color.PINK)
                }