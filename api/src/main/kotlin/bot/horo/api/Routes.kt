@file:OptIn(KtorExperimentalLocationsAPI::class)

package bot.horo.api

import io.ktor.server.locations.*
import kotlinx.serialization.Transient

@Location("/guilds/{guildId}")
data class Guilds(
    val guildId: Long
) {
    @Location("/selfroles")
    data class SelfRoles(
        val guild: Guilds
    ) {
        @Location("/{selfRolesId}")
        data class Id(
            val selfRoles: SelfRoles,
            val selfRolesId: String
        )
    }
}
