package bot.horo.bot.command

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.snowflake
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.createdAt
import dev.kord.common.Color
import dev.kord.rest.builder.message.create.embed

import java.text.DateFormat
import java.text.SimpleDateFormat

class ProfileExtension : Extension() {
    override val name: String = "profile"
    override val bundle: String = "Display a users profile"

    private class ProfileArguments : Arguments() {
         val id by snowflake {
            name = "id"
            description = "put a user id here"
        }
    }

    override suspend fun setup() {
        ephemeralSlashCommand({ProfileArguments()}) { //pass an instance of the ProfileArguments class as an argument
            name = "profile"
            description = "Display a users profile"

            action {
                //access the ID from the parsed arguments
                val id =  arguments.id

                //use the user ID to get the user from the guild
                val guild = this@action.guild!!
                val user = guild.getMember(id)


                fun formatDate(rawDate: String): String {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    val parsedDate = inputFormat.parse(rawDate)
                    val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    return dateFormat.format(parsedDate)
                }
                respond {
                    content = "content"
                    embed {
                        title = "Profile"
                        field {
                            name = "ID"
                            value = user.id.toString()
                        }
                        field {
                            name = "Display name"
                            value = user.displayName
                        }
                        field {
                            name = "User name"
                            value = user.username + "#" + user.discriminator
                        }
                        field {
                            name = "Account created At"
                            value = formatDate(user.createdAt.toString())
                        }
                        field {
                            name = "Server Joined at"
                            value = formatDate(user.joinedAt.toString())
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
