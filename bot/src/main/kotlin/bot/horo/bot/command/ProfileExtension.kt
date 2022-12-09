package bot.horo.bot.command

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.user
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.createdAt
import dev.kord.common.Color
import dev.kord.common.toMessageFormat
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class ProfileExtension : Extension() {
    override val name: String = "profile"
    override val bundle: String = "Display a users profile"

    private class ProfileArguments : Arguments() {
         val target by user {
            name = "tag"
            description = "tag a user"
        }
    }

    override suspend fun setup() {
        publicSlashCommand(::ProfileArguments) { //pass an instance of the ProfileArguments class as an argument
            name = "profile"
            description = "Display a users profile"

            action {
                //access the ID from the parsed arguments
                val id =  arguments.target.id

                //use the user ID to get the user from the guild
                val guild = this@action.guild!!
                val user = guild.getMember(id)

                val joinedAt = user.joinedAt
                val createdAt = user.createdAt
                val now = Clock.System.now()
                val joinedDate = joinedAt.toMessageFormat()
                val createdDate = createdAt.toMessageFormat()

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
                            value = joinedDate
                        }
                        field {
                            name = "Roles (" + user.roleIds.size.toString() + ")"
                            value = user.roleIds.joinToString { "<@&$it>" }
                        }
                        field {
                            name = "Created at"
                            value = createdDate
                        }
                        field {
                            name = "Avatar source"
                            value = user.avatar?.url.toString()
                        }
                        footer {
                            text = "ID:" + user.id.toString()
                        }
                        timestamp = now
                        color = Color(202, 117, 201)
                        thumbnail {
                            url = user.avatar?.url.toString()
                        }
                    }
                }
            }
        }
    }
}
