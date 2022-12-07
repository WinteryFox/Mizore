@file:OptIn(KtorExperimentalLocationsAPI::class)

package bot.horo.api.route

import bot.horo.api.BadRequestException
import bot.horo.api.Guilds
import bot.horo.api.serialization.OptionalProperty
import bot.horo.api.table.Guild
import bot.horo.api.table.SelfRole
import bot.horo.api.table.SelfRole.Response.Companion.asResponse
import dev.kord.common.Color
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.Channel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.exception.EntityNotFoundException
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import dev.kord.rest.request.RestRequestException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.locations.*
import io.ktor.server.locations.patch
import io.ktor.server.locations.post
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

@OptIn(KtorExperimentalLocationsAPI::class)
fun Route.selfRoles(kord: Kord) {
    get<Guilds.SelfRoles> { params ->
        call.respond(
            transaction {
                SelfRole.Entity.find { SelfRole.Table.guildId.eq(params.guild.guildId) }.map { it.asResponse() }
            }
        )
    }

    post<Guilds.SelfRoles> { params ->
        val body = call.receive<SelfRole.Post>()
        newSuspendedTransaction {
            val g = Guild.Entity.findById(params.guild.guildId)
            if (g == null) {
                call.respond(HttpStatusCode.NotFound)
                return@newSuspendedTransaction
            }

            val id = UUID.randomUUID()
            val channel = kord.getChannel(Snowflake(body.channelId))
                ?: throw BadRequestException.FailedToFetchChannelException()
            if (channel.type != ChannelType.GuildText)
                throw BadRequestException.InvalidChannelTypeException()
            val message = try {
                channel.asChannelOf<TextChannel>().createMessage {
                    if (body.title != null || body.description != null || body.imageUrl != null || body.color != null) {
                        embed {
                            if (body.title != null) title = body.title
                            if (body.description != null) description = body.description
                            if (body.imageUrl != null) image = body.imageUrl
                            if (body.color != null) color = Color(body.color)
                        }
                    }

                    actionRow {
                        interactionButton(ButtonStyle.Primary, "srm/$id") {
                            label = body.label
                        }
                    }
                }
            } catch (e: RestRequestException) {
                throw BadRequestException.InsufficientPermissionsException()
            }

            call.respond(HttpStatusCode.Created, SelfRole.Entity.new(id) {
                guild = g
                roleIds = body.roleIds.toTypedArray()
                channelId = body.channelId
                messageId = message.id.value.toLong()
                label = body.label
                title = body.title
                description = body.description
                imageUrl = body.imageUrl
                color = body.color
            }.asResponse())
        }
    }

    get<Guilds.SelfRoles.Id> { params ->
        newSuspendedTransaction {
            val entity = SelfRole.Entity.findById(params.selfRolesId)?.asResponse()
            if (entity == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(entity)
            }
        }
    }

    patch<Guilds.SelfRoles.Id> { params ->
        val body = call.receive<SelfRole.Patch>()
        newSuspendedTransaction {
            val g = Guild.Entity.findById(params.selfRoles.guild.guildId)
            if (g == null) {
                call.respond(HttpStatusCode.NotFound)
                return@newSuspendedTransaction
            }

            val entity = SelfRole.Entity.findById(params.selfRolesId)
            if (entity == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                entity.apply {
                    roleIds = body.roleIds?.toTypedArray() ?: roleIds
                    label = if (body.label is OptionalProperty.Present) body.label.value else label
                    title = if (body.title is OptionalProperty.Present) body.title.value else title
                    description =
                        if (body.description is OptionalProperty.Present) body.description.value else description
                    imageUrl = if (body.imageUrl is OptionalProperty.Present) body.imageUrl.value else imageUrl
                    color = if (body.color is OptionalProperty.Present) body.color.value else color
                }

                getMessage(kord, Snowflake(entity.channelId), Snowflake(entity.messageId)).edit {
                    if (entity.title != null || entity.description != null || entity.imageUrl != null || entity.color != null) {
                        embed {
                            if (entity.title != null) title = entity.title
                            if (entity.description != null) description = entity.description
                            if (entity.imageUrl != null) image = entity.imageUrl
                            if (entity.color != null) color = Color(entity.color!!)
                        }
                    }

                    actionRow {
                        interactionButton(ButtonStyle.Primary, "srm/$id") {
                            //label = translate("create.select.menu.label")
                            label = entity.label
                        }
                    }
                }

                call.respond(HttpStatusCode.OK, entity.asResponse())
            }
        }
    }
}

suspend inline fun <reified T : Channel> getChannel(kord: Kord, channelId: Snowflake): T {
    val channel = try {
        kord.getChannel(channelId) ?: throw BadRequestException.FailedToFetchChannelException()
    } catch (e: EntityNotFoundException) {
        throw BadRequestException.FailedToFetchChannelException()
    } catch (e: RestRequestException) {
        throw BadRequestException.FailedToFetchChannelException()
    }
    return channel.asChannelOfOrNull() ?: throw BadRequestException.InvalidChannelTypeException()
}

suspend fun getMessage(kord: Kord, channelId: Snowflake, messageId: Snowflake): Message {
    val channel = getChannel<TextChannel>(kord, channelId)
    val message = try {
        channel.getMessage(messageId)
    } catch (e: EntityNotFoundException) {
        throw BadRequestException.FailedToFetchMessageException()
    } catch (e: RestRequestException) {
        throw BadRequestException.FailedToFetchMessageException()
    }
    return message
}
