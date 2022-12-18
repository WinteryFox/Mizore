package bot.mizore.bot

import bot.mizore.api.table.Guild
import bot.mizore.api.table.SelfRole
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
import kotlinx.serialization.json.Json

object Api {
    data class Response<T>(
        val body: T?,
        val status: HttpStatusCode
    )

    suspend inline fun <reified T> HttpResponse.response(): Response<T?> {
        val body = try {
            body<T>()
        } catch (_: NoTransformationFoundException) {
            null
        }

        return Response(
            body,
            status
        )
    }

    private val http = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                encodeDefaults = false
            })
        }
        install(DefaultRequest) {
            url("http://localhost:8080/api/")
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }
    }

    suspend fun getGuild(id: Snowflake) =
        http.get("guilds/$id").response<Guild.Response>()

    suspend fun postGuild(id: Snowflake) =
        http.post("guilds/$id").response<Guild.Response>()

    suspend fun getSelfRoles(guildId: Snowflake, id: String) =
        http.get("guilds/$guildId/selfroles/$id").response<SelfRole.Response>()

    suspend fun getSelfRolesByGuild(id: Snowflake) =
        http.get("guilds/$id/selfroles").response<List<SelfRole.Response>>()

    suspend fun postSelfRoles(id: Snowflake, body: SelfRole.Post) =
        http.post("guilds/$id/selfroles") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.response<SelfRole.Response>()

    suspend fun patchSelfRoles(guildId: Snowflake, id: String, body: SelfRole.Patch) =
        http.patch("guilds/$guildId/selfroles/$id") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.response<SelfRole.Response>()

    suspend fun deleteSelfRoles(guildId: Snowflake, id: String) =
        http.delete("guilds/$guildId/selfroles/$id")
}
