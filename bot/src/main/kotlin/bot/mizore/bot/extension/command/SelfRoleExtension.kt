package bot.mizore.bot.extension.command

import bot.mizore.api.serialization.OptionalProperty
import bot.mizore.api.table.SelfRole
import bot.mizore.bot.Api
import bot.mizore.bot.util.confirm
import bot.mizore.bot.util.menu
import bot.mizore.bot.util.parseHex
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.checks.userFor
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.channel
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.disabledButton
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.components.ephemeralSelectMenu
import com.kotlindiscord.kord.extensions.components.forms.ModalForm
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.modules.unsafe.annotations.UnsafeAPI
import com.kotlindiscord.kord.extensions.modules.unsafe.extensions.unsafeSubCommand
import com.kotlindiscord.kord.extensions.modules.unsafe.types.InitialSlashCommandResponse
import com.kotlindiscord.kord.extensions.types.edit
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.*
import dev.kord.common.Color
import dev.kord.common.entity.*
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.embed
import io.ktor.client.call.*
import io.ktor.http.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class SelfRoleExtension : Extension() {
    override val name: String = "mizore.self_role"
    override val bundle: String = "mizore.self_role"

    private inner class SelfRoleCreateArguments : Arguments() {
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

    private inner class SelfRoleEditArguments : Arguments() {
        val id by string {
            name = "edit.arguments.id.name"
            description = "edit.arguments.id.description"

            validate {
                failIfNot(
                    Api.getSelfRoles(context.getGuild()!!.id, value).status == HttpStatusCode.OK,
                    translate("edit.arguments.id.fail")
                )
            }

            autoComplete {
                val channels = kord.getGuildOrThrow(data.guildId.value!!).channels
                val selfRoles =
                    Api.getSelfRolesByGuild(data.guildId.value!!).body!!.associateWith { channels.firstOrNull { channel -> channel.id.value.toLong() == it.channelId } }

                suggestStringMap(
                    selfRoles.map {
                        "(#${it.value?.name ?: "INVALID CHANNEL"}) ${it.key.title} [${it.key.id}]" to it.key.id
                    }.toMap(),
                    FilterStrategy.Contains
                )
            }
        }
    }

    private inner class ModalArguments(
        labelInitial: String? = null,
        embedTitleInitial: String? = null,
        colorInitial: String? = null,
        imageUrlInitial: String? = null,
        descriptionInitial: String? = null
    ) : ModalForm() {
        override var title: String = "self_role_modal"
        override var bundle: String? = "mizore.self_role"

        val label = lineText {
            label = "create.modal.fields.label.label"
            minLength = 0
            maxLength = 80
            placeholder = "create.modal.fields.label.placeholder"
            initialValue = labelInitial ?: "create.modal.fields.label.placeholder"
            required = true
        }
        val embedTitle = lineText {
            label = "create.modal.fields.title.label"
            minLength = 0
            maxLength = 256
            placeholder = "create.modal.fields.title.placeholder"
            initialValue = embedTitleInitial
            required = true
        }
        val color = lineText {
            label = "create.modal.fields.color.label"
            minLength = 0
            maxLength = 6
            placeholder = "create.modal.fields.color.placeholder"
            initialValue = colorInitial
            required = false
        }
        val imageUrl = lineText {
            label = "create.modal.fields.url.label"
            minLength = 0
            maxLength = 1024
            placeholder = "create.modal.fields.url.placeholder"
            initialValue = imageUrlInitial
            required = false
        }
        val description = paragraphText {
            label = "create.modal.fields.description.label"
            minLength = 0
            maxLength = 4000
            placeholder = "create.modal.fields.description.placeholder"
            initialValue = descriptionInitial
            required = false
        }
    }

    @OptIn(UnsafeAPI::class)
    override suspend fun setup() {
        event<ButtonInteractionCreateEvent> {
            check {
                failIfNot(event.interaction.componentId.startsWith("srm/"))
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

            unsafeSubCommand(::SelfRoleCreateArguments) {
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
                                bundle = "mizore.self_role"

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
                                        val title = event.interaction.textInputs["title"]!!.value
                                        val description =
                                            event.interaction.textInputs["description"]!!.value?.ifBlank { null }
                                        val imageUrl =
                                            event.interaction.textInputs["image_url"]!!.value?.ifBlank { null }
                                        val roleIds = selected.map { it.toLong() }.toSet()

                                        val label = event.interaction.textInputs["label"]!!.value?.ifBlank { null }
                                        if (title == null || label == null) {
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

            ephemeralSubCommand(::SelfRoleEditArguments) {
                name = "edit.name"
                description = "edit.description"
                allowInDms = false
                defaultMemberPermissions = Permissions(Permission.ManageRoles)
                requireBotPermissions(Permission.SendMessages, Permission.ManageRoles)

                action {
                    val commandContext = this
                    val response = Api.getSelfRoles(guild!!.id, arguments.id)
                    if (response.status.value !in (200 until 300)) {
                        // content = translate("edit.response.unknown")
                        // TODO
                        return@action
                    }

                    var body = SelfRole.Patch(
                        response.body?.roleIds,
                        response.body?.title,
                        OptionalProperty.valueOrMissing(response.body?.description),
                        OptionalProperty.valueOrMissing(response.body?.imageUrl),
                        OptionalProperty.valueOrMissing(response.body?.color),
                        response.body?.label
                    )

                    menu("edit") {
                        page("role_select") {
                            components {
                                ephemeralSelectMenu {
                                    bundle = "mizore.self_role"

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
                                        option(role.name, role.id.toString()) {
                                            default = body.roleIds?.contains(role.id.value.toLong())
                                        }

                                    action {
                                        body = body.copy(
                                            roleIds = selected.map { it.toLong() }.toSet()
                                        )
                                        previous()
                                    }
                                }
                            }
                        }

                        page("edit") {
                            embed {
                                title = body.title
                                if (body.description is OptionalProperty.Present ||
                                    body.imageUrl is OptionalProperty.Present ||
                                    body.color is OptionalProperty.Present
                                ) {
                                    if (body.description is OptionalProperty.Present) description =
                                        (body.description as OptionalProperty.Present).value
                                    if (body.imageUrl is OptionalProperty.Present) image =
                                        (body.imageUrl as OptionalProperty.Present).value
                                    if (body.color is OptionalProperty.Present) color =
                                        (body.color as OptionalProperty.Present).value?.let { color -> Color(color) }
                                }

                                footer {
                                    text = translate("edit.footer")
                                }
                            }

                            components {
                                disabledButton(0) {
                                    style = ButtonStyle.Secondary
                                    label = body.label
                                }

                                ephemeralButton(
                                    {
                                        ModalArguments(
                                            body.label,
                                            body.title,
                                            body.color.valueOrNull?.toString(16),
                                            body.imageUrl.valueOrNull,
                                            body.description.valueOrNull
                                        )
                                    },
                                    1
                                ) {
                                    label = translate("edit.buttons.edit.label")

                                    action { modal ->
                                        if (modal == null)
                                            return@action

                                        val color = modal.color.value
                                        val title = modal.embedTitle.value!! // TODO
                                        val description = modal.description.value
                                        val imageUrl = modal.imageUrl.value

                                        val label = modal.label.value!! // TODO
                                        body = body.copy(
                                            label = label.ifBlank { null },
                                            title = title.ifBlank { null },
                                            color = OptionalProperty.Present(color?.ifBlank { null }?.parseHex()),
                                            imageUrl = OptionalProperty.Present(imageUrl?.ifBlank { null }),
                                            description = OptionalProperty.Present(description?.ifBlank { null })
                                        )
                                        refresh()
                                    }
                                }

                                ephemeralButton(1) {
                                    label = translate("edit.buttons.roles.label")

                                    action {
                                        open("role_select")
                                    }
                                }

                                ephemeralButton(2) {
                                    style = ButtonStyle.Danger
                                    label = translate("edit.buttons.delete.label")

                                    action {
                                        open("confirm")
                                    }
                                }

                                ephemeralButton(2) {
                                    style = ButtonStyle.Success
                                    label = translate("edit.buttons.save.label")

                                    action {
                                        val r = Api.patchSelfRoles(guild!!.id, response.body!!.id, body)
                                        edit {
                                            if (r.status.value !in (200 until 300)) {
                                                content = translate("error", "mizore.generic")
                                                return@edit
                                            }
                                            embeds = mutableListOf()
                                            components = mutableListOf()
                                            content = translate("edit.success", "mizore.self_roles")
                                        }
                                    }
                                }
                            }
                        }

                        page("confirm") {
                            confirm {
                                accept {
                                    val r = Api.deleteSelfRoles(guild!!.id, response.body!!.id)
                                    if (r.status.value !in (200 until 300)) {
                                        edit {
                                            content = translate("error", "mizore.generic")
                                            embeds = mutableListOf()
                                            components = mutableListOf()
                                        }
                                        return@accept
                                    }
                                    commandContext.interactionResponse.delete()
                                }

                                reject {
                                    previous()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
