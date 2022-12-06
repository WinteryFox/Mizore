package bot.horo.api.table

import bot.horo.api.exposed.array
import bot.horo.api.serialization.OptionalProperty
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.LongColumnType

object SelfRole {
    object Table : IdTable<String>("self_role") {
        override val id: Column<EntityID<String>> = text("id").entityId()
        val guildId = reference("guild_id", Guild.Table)
        val messageId = long("message_id")
        val roleIds = array<Long>("role_ids", LongColumnType())
        val description = text("description").nullable()
        val imageUrl = text("image_url").nullable()
        val color = integer("color").nullable()
    }

    class Entity(id: EntityID<String>) : org.jetbrains.exposed.dao.Entity<String>(id) {
        companion object : EntityClass<String, Entity>(Table)

        var guild by Guild.Entity referencedOn Table.guildId
        var messageId by Table.messageId
        var roleIds by Table.roleIds
        var description by Table.description
        var imageUrl by Table.imageUrl
        var color by Table.color
    }

    @Serializable
    data class Post(
        val id: String,
        @SerialName("message_id")
        val messageId: Long,
        @SerialName("role_ids")
        val roleIds: Set<Long>,
        val description: String?,
        @SerialName("image_url")
        val imageUrl: String?,
        val color: Int?
    )

    @Serializable
    data class Patch(
        @SerialName("message_id")
        val messageId: Long? = null,
        @SerialName("role_ids")
        val roleIds: Set<Long>? = null,
        val description: OptionalProperty<String?> = OptionalProperty.Missing,
        @SerialName("image_url")
        val imageUrl: OptionalProperty<String?> = OptionalProperty.Missing,
        val color: OptionalProperty<Int?> = OptionalProperty.Missing
    )

    @Serializable
    data class Response(
        val id: String,
        @SerialName("guild_id")
        val guildId: Long,
        @SerialName("message_id")
        val messageId: Long,
        @SerialName("role_ids")
        val roleIds: Set<Long>,
        val description: String?,
        val imageUrl: String?,
        val color: Int?
    ) {
        companion object {
            fun Entity.asResponse() = Response(
                id.value,
                guild.id.value,
                messageId,
                roleIds.toSet(),
                description,
                imageUrl,
                color
            )
        }
    }
}
