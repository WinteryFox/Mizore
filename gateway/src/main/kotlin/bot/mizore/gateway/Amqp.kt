package bot.mizore.gateway

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.BuiltinExchangeType
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Delivery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Amqp : AutoCloseable {
    private val factory = ConnectionFactory().load(
        mapOf(
            "HOST" to System.getenv("AMQP_HOST"),
            "PORT" to System.getenv("AMQP_PORT")
        )
    )
    private val connection = factory.newConnection()
    private val channel = connection.createChannel()

    suspend fun createExchange(
        name: String,
        type: BuiltinExchangeType,
        durable: Boolean = true,
        autoDelete: Boolean = false,
        arguments: Map<String, Any> = emptyMap()
    ): AMQP.Exchange.DeclareOk = createExchange(name, type.type, durable, autoDelete, arguments)

    suspend fun createExchange(
        name: String,
        type: String,
        durable: Boolean = true,
        autoDelete: Boolean = false,
        arguments: Map<String, Any> = emptyMap()
    ): AMQP.Exchange.DeclareOk = withContext(Dispatchers.IO) {
        channel.exchangeDeclare(name, type, durable, autoDelete, arguments)
    }

    suspend fun createQueue(
        name: String,
        durable: Boolean = true,
        exclusive: Boolean = false,
        autoDelete: Boolean = false,
        arguments: Map<String, Any> = emptyMap()
    ): AMQP.Queue.DeclareOk = withContext(Dispatchers.IO) {
        channel.queueDeclare(name, durable, exclusive, autoDelete, arguments)
    }

    suspend fun bind(
        queue: String,
        exchange: String,
        key: String
    ): AMQP.Queue.BindOk = withContext(Dispatchers.IO) {
        channel.queueBind(queue, exchange, key)
    }

    suspend fun publish(
        exchange: String,
        routingKey: String,
        message: String
    ) = withContext(Dispatchers.IO) {
        channel.basicPublish(exchange, routingKey, null, message.toByteArray(Charsets.UTF_8))
    }

    suspend fun consume(
        queue: String,
        deliverCallback: (tag: String, delivery: Delivery) -> Unit
    ): String = withContext(Dispatchers.IO) {
        channel.basicConsume(queue, true, deliverCallback) { _ -> }
    }

    override fun close() {
        channel.close()
        connection.close()
    }
}