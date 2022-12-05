@file:OptIn(KtorExperimentalLocationsAPI::class)

package bot.horo.api

import io.ktor.server.locations.*

@Location("/guilds/{id}")
data class Guilds(
    val id: Long
) {
    @Location("/selfroles")
    data class SelfRoles(
        val guild: Guilds
    ) {
        class Post(
            val selfRoles: SelfRoles,
            val id: String,
            val messageId: Long,
            val roleIds: List<Long>
        )
    }
}
