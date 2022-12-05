package bot.horo.api.table

import bot.horo.api.exposed.array
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.LongColumnType

object SelfRoleTable : IdTable<String>("self_role") {
    override val id: Column<EntityID<String>> = text("id").entityId()
    val guildId = reference("guild_id", GuildTable)
    val messageId = long("message_id")
    val roleIds = array<Long>("role_ids", LongColumnType())
}

class SelfRoleEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, SelfRoleEntity>(SelfRoleTable)

    var guildId by GuildEntity referencedOn SelfRoleTable.guildId
    var messageId by SelfRoleTable.messageId
    var roleIds by SelfRoleTable.roleIds
}
