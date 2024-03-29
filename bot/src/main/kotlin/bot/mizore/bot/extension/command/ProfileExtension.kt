package bot.mizore.bot.extension.command

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.user
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.createdAt
import com.kotlindiscord.kord.extensions.utils.getTopRole
import dev.kord.common.Color
import dev.kord.common.toMessageFormat
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed

class ProfileExtension : Extension() {
    override val name: String = "mizore.profile"
    override val bundle: String = "mizore.profile"

    override suspend fun setup() {
        publicSlashCommand {
            name = "name"
            description = "description"

            allowInDms = false

            val theme = Color(202, 117, 201) //this will be in constants file soon @WinteryFox

            publicSubCommand(::ProfileArguments) {
                //command /Profile avatar
                name = "avatar.profile.sub.name"
                description = "avatar.profile.sub.description"

                allowInDms = false

                action {
                    val id = arguments.target.id
                    val user = this@action.guild?.getMember(id) ?: return@action
                    val avatar = user.avatar?.url ?: user.defaultAvatar.url

                    respond {
                        embed {
                            color = theme

                            author {
                                icon = avatar
                                name = user.tag
                            }

                            image = "$avatar?size=512"
                        }

                        if (user.avatar != null) {
                            actionRow {
                                linkButton("$avatar?size=512") {
                                    label = translate("avatar.source.profile.sub.button")
                                }
                            }
                        }
                    }
                }
            }

            publicSubCommand(::ProfileArguments) {
                //command /Profile view
                name = "view.profile.sub.name"
                description = "view.profile.sub.description"

                allowInDms = false

                action {
                    val id = arguments.target.id
                    val user = this@action.guild?.getMember(id) ?: return@action

                    val avatar = user.avatar?.url ?: user.defaultAvatar.url

                    respond {
                        embed {
                            color = theme

                            thumbnail {
                                url = avatar
                            }

                            author {
                                icon = avatar
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
                                    name = translate("view.profile.sub.field.highestRole")
                                    value = "<@&${user.getTopRole()?.id}>"
                                }
                            }

                            field {
                                name = translate("view.profile.sub.field.rolesWithCount", replacements = mapOf("count" to user.roleIds.size))

                                value = if (user.roleIds.isNotEmpty()) {
                                    user.roleIds.joinToString(" ") { "<@&$it>" }
                                } else {
                                    translate("view.profile.sub.field.null.description")
                                }
                            }

                            field {
                                name = translate("view.profile.sub.field.createdAt")
                                value = user.createdAt.toMessageFormat()
                            }

                            footer {
                                text = "${translate("view.profile.sub.field.id")}: ${user.id}"
                            }
                        }
                    }
                }
            }
        }
    }

    inner class ProfileArguments : Arguments() {
        val target by user {
            name = "tag.arguments.user.name"
            description = "tag.arguments.user.description"
        }
    }
}
