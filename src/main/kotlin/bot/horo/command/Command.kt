package bot.horo.command

import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.rest.util.Permission
import discord4j.rest.util.PermissionSet

internal val commands = mutableListOf<Command>()

class Command(
    val name: String,
    val dispatch: suspend CommandContext.() -> Unit,
    val flags: MutableList<String> = mutableListOf(),
    val botPermissions: PermissionSet = PermissionSet.of(Permission.SEND_MESSAGES),
    val userPermissions: PermissionSet = PermissionSet.of(Permission.SEND_MESSAGES)
)

class CommandBuilder(private val name: String) {
    private lateinit var dispatch: suspend CommandContext.() -> Unit
    private val flags = mutableListOf<String>()

    fun dispatch(dispatch: suspend CommandContext.() -> Unit) {
        this.dispatch = dispatch
    }

    fun flag(name: String): Flag {
        this.flags.add(name)
        return Flag(name)
    }

    fun build() = Command(name, dispatch, flags)
}

object CommandsBuilder {
    fun command(name: String, dsl: CommandBuilder.() -> Unit) {
        commands.add(CommandBuilder(name).apply(dsl).build())
    }
}

data class Flag(val name: String)

data class CommandContext(
    val event: MessageCreateEvent,
    val flags: Map<String, String>
) {
    fun Flag.get(): String {
        return flags.getValue(name)
    }
}

fun registerCommands(builder: CommandsBuilder.() -> Unit) {
    CommandsBuilder.apply(builder)
}