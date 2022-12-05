package bot.horo.api.response

import kotlinx.serialization.Serializable

@Serializable
data class SelfRoleResponse(
    val id: String,
    val messageId: Long,
    val roleIds: List<Long>
)

@Serializable
data class GuildResponse(
    val id: Long
)
