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
            name = "tag"
            description = "tag a user"
        }
    }

    override suspend fun setup() {
        publicSlashCommand { //pass an instance of the ProfileArguments class as an argument
            name = "profile"
            description = "profile commands"
            val theme = Color(202, 117, 201)

            publicSubCommand(::ProfileArguments)
            {
                name = "avatar"
                description = "Display a users avatar"

                action {
                    //access the ID from the parsed arguments
                    val id = arguments.target.id

                    //use the user ID to get the user from the guild
                    val guild = this@action.guild!!
                    val user = guild.getMember(id)

                    val imageUrl = user.avatar?.url.toString() + "?size=512"

                    respond {
                        embed {
                            image = imageUrl
                            color = theme
                        }
                        actionRow {
                            linkButton(imageUrl) {
                                label = "Open source"
                            }
                        }
                    }
                }
            }

            publicSubCommand(::ProfileArguments) {
                name = "view"
                description = "Display a users profile"

                action {
                    //access the ID from the parsed arguments
                    val id = arguments.target.id

                    //use the user ID to get the user from the guild
                    val guild = this@action.guild!!
                    val user = guild.getMember(id)

                    respond {
                        embed {
                            author {
                                icon = user.avatar?.url.toString()
                                name = user.tag
                            }
                            field {
                                name = "Nickname"
                                value = user.mention
                            }
                            field {
                                name = "Joined at"
                                value = user.joinedAt.toMessageFormat()
                            }
                            field {
                                name = "Roles (" + user.roleIds.size.toString() + ")"
                                value = user.roleIds.joinToString { "<@&$it>" }
                            }
                            field {
                                name = "Created at"
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
