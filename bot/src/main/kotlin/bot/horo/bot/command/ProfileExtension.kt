package bot.horo.bot.command

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.user
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.createdAt
import dev.kord.common.Color
import dev.kord.common.toMessageFormat
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock

class ProfileExtension : Extension() {
    override val name: String = "horo.profile"
    override val bundle: String = "horo.profile"

    private class ProfileArguments : Arguments() {
        val target by user {
            name = "tag.arguments.user.name"
            description = "tag.arguments.user.description"
        }
    }

    override suspend fun setup() {
        publicSlashCommand { //pass an instance of the ProfileArguments class as an argument
            name = "name"
            description = "description"
            val theme = Color(202, 117, 201)

            publicSubCommand(::ProfileArguments)
            {
                name = "avatar.profile.sub.name"
                description = "avatar.profile.sub.description"

                action {
                    //access the ID from the parsed arguments
                    val id = arguments.target.id

                    //use the user ID to get the user from the guild
                    val guild = this@action.guild!!
                    val user = guild.getMember(id)

                    val imageUrl = user.avatar?.url.toString() + "?size=512"

                    respond {
                        embed {
                            author {
                                icon = user.avatar?.url.toString()
                                name = user.tag
                            }
                            image = imageUrl
                            color = theme
                        }
                        actionRow {
                            linkButton(imageUrl) {
                                label = translate("avatar.source.profile.sub.button")
                            }
                        }
                    }
                }
            }

            publicSubCommand(::ProfileArguments) {
                name = "view.profile.sub.name"
                description = "view.profile.sub.description"

                action {
                    //access the ID from the parsed arguments
                    val id = arguments.target.id

                    //use the user ID to get the user from the guild
                    val guild = this@action.guild!!
                    val user = guild.getMember(id)

                    val rolesLabel = translate("view.profile.sub.field.roles")

                    respond {
                        embed {
                            author {
                                icon = user.avatar?.url.toString()
                                name = user.tag
                            }
                            field {
                                name = translate("view.profile.sub.field.nickname")
                                value = user.mention
                            }
                            field {
                                name = translate("view.profile.sub.field.joinedAt")
                                value = user.joinedAt.toMessageFormat()
                            }
                            if (user.roleIds.isNotEmpty()) {
                                field {
                                    name = "$rolesLabel (${user.roleIds.size})"
                                    value = user.roleIds.joinToString(" ") { "<@&$it>" }
                                }
                            } else {
                                field {
                                    name = "$rolesLabel (0)"
                                    value = "This user has no roles"
                                }
                            }
                            field {
                                name = translate("view.profile.sub.field.createdAt")
                                value = user.createdAt.toMessageFormat()
                            }
                            footer {
                                text = "ID:" + user.id.toString()
                            }
                            timestamp = Clock.System.now()
                            color = theme
                            thumbnail {
                                url = user.avatar?.url.toString()
                            }
                        }
                    }
                }
            }
        }
    }
}
