package bot.horo.bot.command

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.user
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.createdAt
import com.kotlindiscord.kord.extensions.utils.profileLink
import dev.kord.common.Color
import dev.kord.rest.builder.message.create.embed

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter

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


                fun formatDate(rawDate: String): String {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    val parsedDate = inputFormat.parse(rawDate)
                    val dateFormat = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT)
                    return dateFormat.format(parsedDate)
                }
                respond {
                    embed {
                        author {
                            icon = user.avatar?.url.toString()
                            name = user.tag
                        }
                        field {
                            name = "Display name"
                            value = user.mention
                        }
                        field {
                            name = "Created at"
                            value = formatDate(user.createdAt.toString())
                        }
                        field {
                            name = "Joined at"
                            value = formatDate(user.joinedAt.toString())
                        }
                        field {
                            name = "Roles: " + user.roleIds.size.toString()
                            value = user.roleIds.joinToString { "<@&$it>" }
                        }
                        field {
                            name = "Profile link"
                            value = user.profileLink
                        }
                        footer {
                            text = "ID:" + user.id.toString() + " ┇ " + java.time.LocalDateTime.now().format(
                                DateTimeFormatter.ofPattern("E d MMM uuuu")) + " at " +  java.time.LocalDateTime.now().format(
                                DateTimeFormatter.ofPattern("HH:mm"))
                        }
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
