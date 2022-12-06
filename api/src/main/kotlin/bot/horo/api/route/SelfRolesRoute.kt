@file:OptIn(KtorExperimentalLocationsAPI::class)

package bot.horo.api.route

import bot.horo.api.Guilds
import bot.horo.api.table.Guild
import bot.horo.api.table.SelfRole
import bot.horo.api.table.SelfRole.Response.Companion.asResponse
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

@OptIn(KtorExperimentalLocationsAPI::class)
fun Route.selfRoles() {
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

            var entity = SelfRole.Entity.findById(body.id)?.asResponse()
            if (entity == null) {
                entity = SelfRole.Entity.new(body.id) {
                    guild = g
                    roleIds = body.roleIds.toTypedArray()
                    messageId = body.messageId
                    description = body.description
                    imageUrl = body.imageUrl
                }.asResponse()
                call.respond(HttpStatusCode.Created, entity)
            } else {
                call.respond(HttpStatusCode.OK, entity)
            }
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
                    messageId = body.messageId ?: messageId
                    roleIds = body.roleIds?.toTypedArray() ?: roleIds
                    description = if (body.description.isPresent()) body.description.value() else description
                    imageUrl = if (body.imageUrl.isPresent()) body.imageUrl.value() else imageUrl
                }
            }
        }
    }
}
