package bot.horo.bot

import bot.horo.api.response.GuildResponse
import bot.horo.api.response.SelfRoleResponse
import dev.kord.common.entity.Snowflake
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
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

    suspend fun getGuild(id: Snowflake) = http.get("guilds/$id").call.response.body<GuildResponse>()

    suspend fun getSelfRolesByGuild(guildId: Snowflake) =
        http.get("guilds/$guildId/selfroles").call.response.body<List<SelfRoleResponse>>()
}
