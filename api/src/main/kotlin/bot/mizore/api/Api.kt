@file:Suppress("unused", "UnnecessaryOptInAnnotation")

package bot.mizore.api

import bot.mizore.api.route.guilds
import bot.mizore.api.route.selfRoles
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.kord.common.annotation.KordExperimental
import dev.kord.core.Kord
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.locations.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database

fun main(args: Array<String>): Unit = EngineMain.main(args)

@OptIn(KtorExperimentalLocationsAPI::class, KordExperimental::class)
fun Application.api() {
    Database.connect(HikariDataSource {
        driverClassName = "org.postgresql.Driver"
        jdbcUrl = System.getenv("POSTGRES_HOST")
        username = System.getenv("POSTGRES_USER")
        password = System.getenv("POSTGRES_PASSWORD")
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
    })
    val kord: Kord = Kord.restOnly(System.getenv("TOKEN"))

    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            call.respond(cause.status, BadRequestResponse(cause.code, cause.message!!, cause.status.value))
        }
    }
    install(Locations)
    install(AutoHeadResponse)
    install(Routing)
    install(ContentNegotiation) {
        json(Json {
            encodeDefaults = false
        })
    }

    routing {
        trace {
            application.log.trace(it.buildText())
        }
        route("/api") {
            guilds()
            selfRoles(kord)
        }
    }
}

fun HikariDataSource(builder: HikariConfig.() -> Unit): HikariDataSource {
    val config = HikariConfig()
    config.apply(builder)
    config.validate()
    return HikariDataSource(config)
}
