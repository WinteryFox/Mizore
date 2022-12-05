package bot.horo.api.table

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object GuildTable : LongIdTable("guild")

class GuildEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<GuildEntity>(GuildTable)

    val selfRoles by SelfRoleEntity referrersOn SelfRoleTable.guildId
}
