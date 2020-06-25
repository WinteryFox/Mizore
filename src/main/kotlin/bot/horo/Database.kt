package bot.horo

import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import io.r2dbc.spi.Closeable
import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.coroutines.reactive.awaitSingle
import org.intellij.lang.annotations.Language
import reactor.kotlin.core.publisher.toFlux

class Database {
    private val pool = ConnectionPool(
        ConnectionPoolConfiguration.builder()
            .connectionFactory(
                PostgresqlConnectionFactory(
                    PostgresqlConnectionConfiguration.builder()
                        .host("localhost")
                        .port(5432)
                        .username("horo")
                        .password("12345")
                        .database("horo")
                        .build()
                )
            )
            .maxSize(100)
            .build()
    )

    suspend fun execute(
        @Language("PostgreSQL") sql: String,
        binds: Map<String, Any> = emptyMap()
    ): Int =
        pool.create()
            .awaitSingle()
            .use { connection ->
                connection.createStatement(sql)
                    .let { statement ->
                        binds.forEach {
                            statement.bind(it.key, it.value)
                        }
                        return@let statement
                    }
                    .execute()
                    .awaitSingle()
                    .rowsUpdated
                    .awaitSingle()
            }

    suspend fun <T> query(
        @Language("PostgreSQL") sql: String,
        binds: Map<String, Any> = emptyMap(),
        consumer: (Row, RowMetadata) -> T
    ): List<T> =
        pool.create()
            .awaitSingle()
            .use { connection ->
                connection.createStatement(sql)
                    .let { statement ->
                        binds.forEach {
                            statement.bind(it.key, it.value)
                        }
                        return@let statement
                    }
                    .execute()
                    .toFlux()
                    .flatMap { result ->
                        result.map { t, u ->
                            consumer(t, u)
                        }
                    }
                    .collectList()
                    .awaitSingle()
            }

    private inline fun <T : Closeable?, R> T.use(block: (T) -> R): R =
        try {
            block(this)
        } catch (e: Throwable) {
            throw e
        } finally {
            this?.close()
        }
}