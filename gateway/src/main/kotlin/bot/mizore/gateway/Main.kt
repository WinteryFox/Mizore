package bot.mizore.gateway

import com.rabbitmq.client.BuiltinExchangeType
import dev.kord.core.Kord
import dev.kord.core.event.interaction.InteractionCreateEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun main(): Unit = runBlocking {
    val json = Json
    Amqp.createExchange("gateway", BuiltinExchangeType.DIRECT)
    Amqp.createQueue("interactions", arguments = mapOf("x-message-ttl" to 900))
    Amqp.bind("interactions", "gateway", "interactions-key")

    val kord = Kord(System.getenv("TOKEN"))
    kord.gateway.events
        .map { it.event }
        .filterIsInstance<InteractionCreateEvent>()
        .map { it.interaction }
        .onEach { Amqp.publish("gateway", "interaction-key", json.encodeToString(it.data)) }
        .launchIn(this)
    kord.login()
}
