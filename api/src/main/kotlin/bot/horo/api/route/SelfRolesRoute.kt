@file:OptIn(KtorExperimentalLocationsAPI::class)

package bot.horo.api.route

import bot.horo.api.Guilds
import bot.horo.api.response.SelfRoleResponse
import bot.horo.api.table.GuildEntity
import bot.horo.api.table.SelfRoleEntity
import bot.horo.api.table.SelfRoleTable
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.locations.*
import io.ktor.server.locations.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

@OptIn(KtorExperimentalLocationsAPI::class)
fun Route.selfRoles() {
    get<Guilds.SelfRoles> { params ->
        call.respond(
            transaction {
                SelfRoleEntity.find { SelfRoleTable.guildId.eq(params.guild.id) }
                    .map {
                        SelfRoleResponse(
                            it.id.value,
                            it.messageId,
                            it.roleIds.asList()
                        )
                    }
            }
        )
    }

    post<Guilds.SelfRoles.Post> { params ->
        newSuspendedTransaction {
            val guild = GuildEntity.findById(params.selfRoles.guild.id)
            if (guild == null) {
                call.respond(HttpStatusCode.NotFound)
                return@newSuspendedTransaction
            }

            SelfRoleEntity.new(params.id) {
                guildId = guild
                roleIds = params.roleIds.toTypedArray()
                messageId = params.messageId
            }
            call.respond(HttpStatusCode.Created)
        }
    }
}
