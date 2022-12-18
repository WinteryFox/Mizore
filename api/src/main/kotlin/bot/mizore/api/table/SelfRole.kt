package bot.mizore.api.table

import bot.mizore.api.exposed.array
import bot.mizore.api.serialization.OptionalProperty
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.LongColumnType
import java.util.*

object SelfRole {
    object Table : UUIDTable("self_role", "id") {
        val guildId = reference("guild_id", Guild.Table)
        val roleIds = array<Long>("role_ids", LongColumnType())
        val channelId = long("channel_id")
        val messageId = long("message_id")
        val label = text("label")
        val title = text("title")
        val description = text("description").nullable()
        val imageUrl = text("image_url").nullable()
        val color = integer("color").nullable()
    }

    class Entity(id: EntityID<UUID>) : UUIDEntity(id) {
        companion object : UUIDEntityClass<Entity>(Table)

        var guild by Guild.Entity referencedOn Table.guildId
        var roleIds by Table.roleIds
        var channelId by Table.channelId
        var messageId by Table.messageId
        var label by Table.label
        var title by Table.title
        var description by Table.description
        var imageUrl by Table.imageUrl
        var color by Table.color
    }

    @Serializable
    data class Post(
        @SerialName("role_ids")
        val roleIds: Set<Long>,
        @SerialName("channel_id")
        val channelId: Long,
        val label: String,
        val title: String,
        val description: String?,
        @SerialName("image_url")
        val imageUrl: String?,
        val color: Int?
    )

    @Serializable
    data class Patch(
        @SerialName("role_ids")
        val roleIds: Set<Long>? = null,
        val title: String? = null,
        val description: OptionalProperty<String?> = OptionalProperty.Missing,
        @SerialName("image_url")
        val imageUrl: OptionalProperty<String?> = OptionalProperty.Missing,
        val color: OptionalProperty<Int?> = OptionalProperty.Missing,
        val label: String? = null
    )

    @Serializable
    data class Response(
        val id: String,
        @SerialName("guild_id")
        val guildId: Long,
        @SerialName("role_ids")
        val roleIds: Set<Long>,
        @SerialName("channel_id")
        val channelId: Long,
        @SerialName("message_id")
        val messageId: Long,
        val title: String,
        val description: String?,
        val imageUrl: String?,
        val color: Int?,
        val label: String?
    ) {
        companion object {
            fun Entity.asResponse() = Response(
                id = id.value.toString(),
                guildId = guild.id.value,
                roleIds = roleIds.toSet(),
                channelId = channelId,
                messageId = messageId,
                title = title,
                description = description,
                imageUrl = imageUrl,
                color = color,
                label = label
            )
        }
    }
}
