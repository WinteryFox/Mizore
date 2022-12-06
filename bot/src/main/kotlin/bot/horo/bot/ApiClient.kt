package bot.horo.bot

import bot.horo.api.table.Guild
import bot.horo.api.table.SelfRole
import dev.kord.common.entity.Snowflake
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*

object Api {
    private val http = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
        install(DefaultRequest) {
            url("http://localhost:8080/api/")
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }
    }

    suspend fun getGuild(id: Snowflake): Guild.Response = http.get("guilds/$id").call.response.body()

    suspend fun getSelfRoles(guildId: Snowflake, id: String): HttpResponse = http.get("guilds/$guildId/selfroles/$id").call.response.body()

    suspend fun getSelfRolesByGuild(id: Snowflake): List<SelfRole.Response> =
        http.get("guilds/$id/selfroles").call.response.body()

    suspend fun postSelfRoles(id: Snowflake, body: SelfRole.Post): HttpResponse =
        http.post("guilds/$id/selfroles") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    suspend fun patchSelfRoles(id: Snowflake, body: SelfRole.Post): HttpResponse =
        http.patch("guilds/$id/selfroles") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
}
