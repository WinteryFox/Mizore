package bot.horo.command

import bot.horo.GUILD_INVITE
import bot.horo.data.Database
import discord4j.core.`object`.entity.Member
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.MessageCreateSpec
import discord4j.rest.util.Color
import discord4j.rest.util.Permission
import discord4j.rest.util.PermissionSet
import org.jetbrains.annotations.PropertyKey
import java.util.*

internal val commands = mutableListOf<Command>()
fun MutableList<Command>.traverse(input: String): Command? {
    var sequence = input.splitToSequence(" ")
    var command = this.find { it.name == sequence.first() || it.aliases.contains(sequence.first()) } ?: return null
    sequence = sequence.drop(1)

    for (seq in sequence) {
        command = command.children.find { it.name == seq || it.aliases.contains(seq) } ?: return null
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
    private val botPermissions = PermissionSet.none()
    private val userPermissions = PermissionSet.none()

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

    fun build() = Command(name, aliases, dispatch, parameters, botPermissions, userPermissions, children)
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
    val database: Database
) {
    fun Parameter.get(): String {
        return parameters.getValue(name)
    }

    suspend fun translate(@PropertyKey(resourceBundle = "localization") key: String, member: Member): String =
        ResourceBundle.getBundle(
            "localization",
            Locale.forLanguageTag(
                database.query(
                    """
SELECT CASE
           WHEN (SELECT locale FROM guilds WHERE snowflake = $1) IS NULL THEN
                   (SELECT locale FROM users WHERE snowflake = $2)
           ELSE
                   (SELECT locale FROM guilds WHERE snowflake = $1)
           END
                    """,
                    mapOf(Pair("$1", member.guildId), Pair("$2", member.id))
                ) { row, _ ->
                    row["locale"] as String? ?: "en-GB"
                }.single()
            )
        ).getString(key)

    fun getAvailableLocales(): Set<Locale> {
        val bundles = mutableSetOf<ResourceBundle>()

        for (locale in Locale.getAvailableLocales())
            try {
                bundles.add(ResourceBundle.getBundle("localization", locale))
            } catch (_: MissingResourceException) {
            }

        return bundles.map { it.locale }.toSet()
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