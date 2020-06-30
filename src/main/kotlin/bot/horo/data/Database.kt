package bot.horo.data

import bot.horo.data.codec.SnowflakeCodec
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import io.r2dbc.spi.Batch
import io.r2dbc.spi.Closeable
import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.coroutines.reactive.awaitSingle
import org.intellij.lang.annotations.Language
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

class Database {
    private val pool = ConnectionPool(
        ConnectionPoolConfiguration.builder()
            .connectionFactory(
                PostgresqlConnectionFactory(
                    PostgresqlConnectionConfiguration.builder()
                        .host(System.getenv("POSTGRES_HOST"))
                        .port(5432)
                        .username(System.getenv("POSTGRES_USER"))
                        .password(System.getenv("POSTGRES_PASSWORD"))
                        .database(System.getenv("POSTGRES_DB"))
                        .codecRegistrar { _, allocator, registry ->
                            registry.addFirst(SnowflakeCodec(allocator))
                            Mono.empty()
                        }
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
                    .apply { binds.forEach { this.bind(it.key, it.value) } }
                    .execute()
                    .awaitSingle()
                    .rowsUpdated
                    .awaitSingle()
            }

    suspend fun <T> batch(
        batch: (Batch) -> Unit,
        transform: (Row, RowMetadata) -> T
    ): List<T> =
        pool.create()
            .awaitSingle()
            .use { connection ->
                connection.createBatch()
                    .apply(batch)
                    .execute()
                    .toFlux()
                    .concatMap { it.map(transform) }
                    .collectList()
                    .awaitSingle()
            }

    suspend fun <T> query(
        @Language("PostgreSQL") sql: String,
        binds: Map<String, Any> = emptyMap(),
        transform: (Row, RowMetadata) -> T
    ): List<T> =
        pool.create()
            .awaitSingle()
            .use { connection ->
                Flux.from(
                    connection.createStatement(sql)
                        .apply { binds.forEach { this.bind(it.key, it.value) } }
                        .execute()
                        .awaitSingle()
                        .map(transform))
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