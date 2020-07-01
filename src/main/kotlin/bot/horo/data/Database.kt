package bot.horo.data

import bot.horo.data.codec.SnowflakeCodec
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import io.r2dbc.spi.*
import kotlinx.coroutines.reactive.awaitSingle
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import kotlin.system.exitProcess

class Database {
    private val logger = LoggerFactory.getLogger(Database::class.java)
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
            .initialSize(5)
            .maxSize(100)
            .build()
    )

    init {
        pool.warmup()
            .doOnError {
                logger.error("Failed to warmup database pool", it)
                exitProcess(1)
            }
            .block()
    }

    suspend fun execute(
        @Language("PostgreSQL") sql: String,
        binds: Map<String, Any> = emptyMap()
    ): Int =
        getConnection { connection ->
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
        getConnection { connection ->
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
        getConnection { connection ->
            Flux.from(
                connection.createStatement(sql)
                    .apply { binds.forEach { this.bind(it.key, it.value) } }
                    .execute()
                    .awaitSingle()
                    .map(transform))
                .collectList()
                .awaitSingle()
        }

    private suspend inline fun <T> getConnection(transform: (Connection) -> T) =
        pool.create().awaitSingle().use(transform)

    private inline fun <T : Closeable?, R> T.use(block: (T) -> R): R =
        try {
            block(this)
        } catch (e: Throwable) {
            throw e
        } finally {
            this?.close()
        }
}