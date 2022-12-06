@file:Suppress("unused", "UnnecessaryOptInAnnotation")

package bot.horo.api

import bot.horo.api.route.guilds
import bot.horo.api.route.selfRoles
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import io.ktor.http.ContentType.Application.Json
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.locations.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database

fun main(args: Array<String>): Unit = EngineMain.main(args)

@OptIn(KtorExperimentalLocationsAPI::class)
fun Application.api() {
    Database.connect(HikariDataSource {
        driverClassName = "org.postgresql.Driver"
        jdbcUrl = System.getenv("POSTGRES_HOST")
        username = System.getenv("POSTGRES_USER")
        password = System.getenv("POSTGRES_PASSWORD")
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
    })

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
            selfRoles()
        }
    }
}

fun HikariDataSource(builder: HikariConfig.() -> Unit): HikariDataSource {
    val config = HikariConfig()
    config.apply(builder)
    config.validate()
    return HikariDataSource(config)
}
