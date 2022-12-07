package bot.horo.api.route

import bot.horo.api.Guilds
import bot.horo.api.table.Guild
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.locations.*
import io.ktor.server.locations.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

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
            // TODO: Make not error if exists
            Guild.Entity.new(params.guildId) { }
            call.respond(HttpStatusCode.OK)
        }
    }
}