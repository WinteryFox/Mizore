package bot.horo.bot.command

import bot.horo.api.table.SelfRole
import bot.horo.bot.Api
import bot.horo.bot.parseHex
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.channel
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralSelectMenu
import com.kotlindiscord.kord.extensions.components.publicButton
import com.kotlindiscord.kord.extensions.components.publicSelectMenu
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.modules.unsafe.annotations.UnsafeAPI
import com.kotlindiscord.kord.extensions.modules.unsafe.extensions.unsafeSubCommand
import com.kotlindiscord.kord.extensions.modules.unsafe.types.InitialSlashCommandResponse
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.types.respondEphemeral
import com.kotlindiscord.kord.extensions.utils.*
import dev.kord.common.Color
import dev.kord.common.entity.*
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.rest.builder.message.create.embed
import io.ktor.http.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class SelfRoleExtension : Extension() {
    override val name: String = "horo.self_role"
    override val bundle: String = "horo.self_role"

    private class SelfRoleArguments : Arguments() {
        val id by string {
            name = "create.arguments.id.name"
            description = "create.arguments.id.description"

            validate {
                failIf(
                    Api.getSelfRoles(context.getGuild()!!.id, value).status != HttpStatusCode.NotFound,
                    translate("create.arguments.id.fail")
                )
            }
        }
        val channel by channel {
            requiredChannelTypes = mutableSetOf(ChannelType.GuildText)
            name = "create.arguments.channel.name"
            description = "create.arguments.channel.description"

            validate {
                failIfNot(
                    (value.asChannelOf<TextChannel>()).botHasPermissions(Permission.SendMessages),
                    translate("create.arguments.channel.fail")
                )
            }
        }
    }

    @OptIn(UnsafeAPI::class)
    override suspend fun setup() {
        ephemeralSlashCommand {
            name = "name"
            description = "description"
            allowInDms = false

            unsafeSubCommand({ SelfRoleArguments() }) {
                name = "create.name"
                description = "create.description"
                allowInDms = false
                defaultMemberPermissions = Permissions(Permission.ManageRoles)
                requireBotPermissions(Permission.SendMessages, Permission.ManageRoles)
                initialResponse = InitialSlashCommandResponse.None

                action {
                    val uuid = UUID.randomUUID()
                    event.interaction.modal(translate("create.modal.title"), uuid.toString()) {
                        /*actionRow {
                            textInput(TextInputStyle.Short, "id", translate("create.modal.fields.title"))
                        }*/
                        actionRow {
                            textInput(TextInputStyle.Short, "color", translate("create.modal.fields.color")) {
                                required = false
                            }
                        }
                        actionRow {
                            textInput(TextInputStyle.Short, "image_url", translate("create.modal.fields.url")) {
                                required = false
                            }
                        }
                        actionRow {
                            textInput(
                                TextInputStyle.Paragraph,
                                "description",
                                translate("create.modal.fields.description")
                            ) {
                                required = false
                            }
                        }
                    }

                    val event =
                        this@unsafeSubCommand.kord.waitFor<ModalSubmitInteractionCreateEvent>(15.minutes) { interaction.modalId == uuid.toString() }
                            ?: return@action

                    event.interaction.respondEphemeral {
                        components {
                            ephemeralSelectMenu {
                                bundle = "horo.self_role"

                                val guild = this@action.guild!!
                                val topRole = guild.selfMember().getTopRole()?.getPosition() ?: 0
                                val roles = guild.roles
                                    .filter { it.getPosition() < topRole }
                                    .filter { !it.managed }
                                    .filter { it.id != guild.id }.toList()

                                embed {
                                    color = Color(202, 117, 201)
                                    title = translate("create.select.embed.title")
                                    description = translate("create.select.embed.description")

                                    field {
                                        name = translate("create.select.embed.field.name")
                                        value = translate("create.select.embed.field.value")
                                    }
                                }
                                minimumChoices = 1
                                maximumChoices = roles.size
                                for (role in roles)
                                    option(role.name, role.id.toString())

                                action {
                                    respond {
                                        val id = arguments.id
                                        val clr = event.interaction.textInputs["color"]!!.value?.parseHex()
                                        val desc = event.interaction.textInputs["description"]!!.value?.ifBlank { null }
                                        val imageUrl =
                                            event.interaction.textInputs["image_url"]!!.value?.ifBlank { null }
                                        val roleIds = selected.map { it.toLong() }.toSet()

                                        val message = arguments.channel.asChannelOf<TextChannel>().createMessage {
                                            embed {
                                                title = id
                                                if (desc != null) description = desc
                                                if (imageUrl != null) image = imageUrl
                                                if (clr != null) color = Color(clr)
                                            }

                                            components {
                                                publicButton {
                                                    label = translate("create.select.menu.label")

                                                    action {
                                                        respondEphemeral {
                                                            components {
                                                                ephemeralSelectMenu {
                                                                    val member = member!!.asMember()
                                                                    val r =
                                                                        roleIds.mapNotNull { role -> guild.roles.firstOrNull { role == it.id.value.toLong() } }
                                                                    minimumChoices = 0
                                                                    maximumChoices = r.size
                                                                    for (role in r)
                                                                        option(role.name, role.id.toString()) {
                                                                            default =
                                                                                member.roleIds.contains(role.id) == true
                                                                            if (role.unicodeEmoji != null)
                                                                                emoji = DiscordPartialEmoji(
                                                                                    null,
                                                                                    role.unicodeEmoji
                                                                                )
                                                                        }

                                                                    action {
                                                                        respond {
                                                                            member.edit {
                                                                                this.roles =
                                                                                    member.roleIds.toMutableSet()
                                                                                this.roles!!.addAll(selected.map {
                                                                                    Snowflake(
                                                                                        it
                                                                                    )
                                                                                })
                                                                                this.roles!!.removeAll(options.map {
                                                                                    Snowflake(
                                                                                        it.value
                                                                                    )
                                                                                }.filter { role ->
                                                                                    !selected.map {
                                                                                        Snowflake(
                                                                                            it
                                                                                        )
                                                                                    }.contains(role)
                                                                                }.toSet())
                                                                            }
                                                                            content = selected.size.toString()
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Api.postSelfRoles(
                                            guild.id,
                                            SelfRole.Post(
                                                id,
                                                message.id.value.toLong(),
                                                roleIds,
                                                desc,
                                                imageUrl,
                                                clr
                                            )
                                        )
                                        content = translate("create.select.response.created")
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
