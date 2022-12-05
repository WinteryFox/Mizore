package bot.horo.bot.command

import bot.horo.bot.Api
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.channel
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralSelectMenu
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.FilterStrategy
import com.kotlindiscord.kord.extensions.utils.getTopRole
import com.kotlindiscord.kord.extensions.utils.selfMember
import com.kotlindiscord.kord.extensions.utils.suggestStringCollection
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList

class SelfRole : Extension() {
    override val name: String = "horo.self_role"
    override val bundle: String = "horo.self_role"

    private class SelfRoleArguments : Arguments() {
        val id by string {
            name = "create.arguments.id.name"
            description = "create.arguments.id.description"

            autoComplete {
                suggestStringCollection(
                    Api.getSelfRolesByGuild(data.guildId.value!!).map { it.id },
                    FilterStrategy.Contains,
                    true
                )
            }
        }
        val channel by channel {
            requiredChannelTypes = mutableSetOf(ChannelType.GuildText)
            name = "create.arguments.channel.name"
            description = "create.arguments.channel.description"
        }
    }

    override suspend fun setup() {
        ephemeralSlashCommand {
            name = "name"
            description = "description"

            ephemeralSubCommand({ SelfRoleArguments() }) {
                name = "create.name"
                description = "create.description"
                allowInDms = false
                defaultMemberPermissions = Permissions(Permission.ManageRoles)
                requireBotPermissions(Permission.ManageRoles)

                action {
                    respond {
                        components {
                            ephemeralSelectMenu {
                                val guild = this@action.guild!!
                                val topRole = guild.selfMember().getTopRole()?.getPosition() ?: 0
                                val roles =
                                    guild.roles.filter { it.getPosition() < topRole }.filter { !it.managed }.toList()

                                minimumChoices = 1
                                maximumChoices = roles.size
                                for (role in roles)
                                    option(role.name, role.id.toString())

                                action {
                                    respond {
                                        content = arguments.id
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
