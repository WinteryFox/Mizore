package bot.mizore.api.route

import bot.mizore.api.Guilds
import bot.mizore.api.table.Guild
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.locations.*
import io.ktor.server.locations.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

@OptIn(KtorExperimentalLocationsAPI::class)
fun Route.guilds() {
    get<Guilds> { params ->
        val id = params.guildId
        newSuspendedTransaction {
            val guild = Guild.Entity.findById(id)
            if (guild == null) {
                call.response.status(HttpStatusCode.NotFound)
                return@newSuspendedTransaction
            }

            call.respond(Guild.Response(id))
        }
    }

    post<Guilds> { params ->
        newSuspendedTransaction {
            val guild = Guild.Entity.findById(params.guildId)
            if (guild != null)
                return@newSuspendedTransaction call.respond(HttpStatusCode.OK)

            call.respond(HttpStatusCode.Created, Guild.Entity.new(params.guildId) { })
        }
    }
}