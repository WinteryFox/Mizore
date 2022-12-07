package bot.horo.bot.command

import bot.horo.api.serialization.OptionalProperty
import bot.horo.api.table.SelfRole
import bot.horo.bot.Api
import bot.horo.bot.Api.response
import bot.horo.bot.parseHex
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.checks.userFor
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.channel
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralSelectMenu
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.modules.unsafe.annotations.UnsafeAPI
import com.kotlindiscord.kord.extensions.modules.unsafe.extensions.unsafeSubCommand
import com.kotlindiscord.kord.extensions.modules.unsafe.types.InitialSlashCommandResponse
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.*
import dev.kord.common.Color
import dev.kord.common.entity.*
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import io.ktor.client.call.*
import io.ktor.http.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class SelfRoleExtension : Extension() {
    override val name: String = "horo.self_role"
    override val bundle: String = "horo.self_role"

    private class SelfRoleArguments : Arguments() {
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
        event<ButtonInteractionCreateEvent> {
            check {
                passIf(event.interaction.componentId.startsWith("srm/"))
            }
            action {
                val response = Api.getSelfRoles(
                    event.interaction.data.guildId.asOptional.value!!,
                    event.interaction.componentId.removePrefix("srm/")
                )
                if (response.status != HttpStatusCode.OK) {
                    event.interaction.respondEphemeral {
                        content = translate("inactive")
                    }
                    return@action
                }

                val guildRoles =
                    guildFor(event)!!.roles.filter { response.body!!.roleIds.contains(it.id.value.toLong()) }.toList()
                //val memberBehaviour = memberFor(event)!!
                val memberBehaviour = userFor(event)!!.asMember(event.interaction.data.guildId.asOptional.value!!)
                val memberRoles = memberBehaviour.asMember().roleIds

                event.interaction.respondEphemeral {
                    components {
                        ephemeralSelectMenu {
                            minimumChoices = 0
                            maximumChoices = guildRoles.size
                            for (role in guildRoles)
                                option(role.name, role.id.toString()) {
                                    default = memberRoles.contains(role.id) == true
                                    if (role.unicodeEmoji != null)
                                        emoji = DiscordPartialEmoji(null, role.unicodeEmoji)
                                }

                            action {
                                respond {
                                    memberBehaviour.edit {
                                        this.roles = memberRoles.toMutableSet()
                                        this.roles!!.addAll(selected.map { Snowflake(it) })
                                        this.roles!!.removeAll(
                                            options.map { Snowflake(it.value) }
                                                .filter { role ->
                                                    !selected.map {
                                                        Snowflake(
                                                            it
                                                        )
                                                    }.contains(role)
                                                }
                                                .sorted()
                                                .toSet()
                                        )
                                    }
                                    content = selected.size.toString()
                                }
                            }
                        }
                    }
                }
            }
        }

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
                        actionRow {
                            textInput(TextInputStyle.Short, "label", translate("create.modal.fields.label.label")) {
                                allowedLength = 0..80
                                placeholder = translate("create.modal.fields.label.placeholder")
                                value = translate("create.modal.fields.label.placeholder")
                                required = true
                            }
                        }
                        actionRow {
                            textInput(TextInputStyle.Short, "title", translate("create.modal.fields.title.label")) {
                                allowedLength = 0..256
                                placeholder = translate("create.modal.fields.title.placeholder")
                                required = false
                            }
                        }
                        actionRow {
                            textInput(TextInputStyle.Short, "color", translate("create.modal.fields.color.label")) {
                                allowedLength = 0..6
                                placeholder = translate("create.modal.fields.color.placeholder")
                                required = false
                            }
                        }
                        actionRow {
                            textInput(TextInputStyle.Short, "image_url", translate("create.modal.fields.url.label")) {
                                allowedLength = 0..1024
                                placeholder = translate("create.modal.fields.url.placeholder")
                                required = false
                            }
                        }
                        actionRow {
                            textInput(
                                TextInputStyle.Paragraph,
                                "description",
                                translate("create.modal.fields.description.label")
                            ) {
                                allowedLength = 0..4000
                                placeholder = translate("create.modal.fields.description.placeholder")
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
                                        val color = event.interaction.textInputs["color"]!!.value?.parseHex()
                                        val title = event.interaction.textInputs["title"]!!.value?.ifBlank { null }
                                        val description =
                                            event.interaction.textInputs["description"]!!.value?.ifBlank { null }
                                        val imageUrl =
                                            event.interaction.textInputs["image_url"]!!.value?.ifBlank { null }
                                        val roleIds = selected.map { it.toLong() }.toSet()

                                        val label = event.interaction.textInputs["label"]!!.value?.ifBlank { null }
                                        if (label == null) {
                                            content = translate("create.select.response.failed")
                                            return@respond
                                        }

                                        Api.postSelfRoles(
                                            guild.id,
                                            SelfRole.Post(
                                                roleIds,
                                                arguments.channel.id.value.toLong(),
                                                label,
                                                title,
                                                description,
                                                imageUrl,
                                                color
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
