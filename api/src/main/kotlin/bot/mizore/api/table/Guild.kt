package bot.mizore.api.table

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object Guild {
    object Table : LongIdTable("guild")

    class Entity(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<Entity>(Table)

        val selfRoles by SelfRole.Entity referrersOn SelfRole.Table.guildId
    }

    @Serializable
    data class Response(
        val id: Long
    ) {
        companion object {
            fun Entity.asResponse() = Response(
                id.value
            )
        }
    }
}
