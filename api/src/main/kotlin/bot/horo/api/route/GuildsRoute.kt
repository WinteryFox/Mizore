package bot.horo.api.route

import bot.horo.api.Guilds
import bot.horo.api.response.GuildResponse
import bot.horo.api.table.GuildEntity
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
        val id = params.id
        newSuspendedTransaction {
            val guild = GuildEntity.findById(id)
            if (guild == null) {
                call.response.status(HttpStatusCode.NotFound)
                return@newSuspendedTransaction
            }

            call.respond(GuildResponse(id))
        }
    }

    post<Guilds> { params ->
        transaction {
            GuildEntity.new(params.id) { }
        }
    }
}