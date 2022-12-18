package bot.mizore.bot.util

import com.kotlindiscord.kord.extensions.components.buttons.EphemeralInteractionButton
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.types.EphemeralInteractionContext
import com.kotlindiscord.kord.extensions.types.TranslatableContext
import com.kotlindiscord.kord.extensions.types.edit
import com.kotlindiscord.kord.extensions.utils.scheduling.Task
import dev.kord.common.Color
import dev.kord.common.entity.ButtonStyle
import dev.kord.rest.builder.message.modify.embed
import kotlin.time.Duration.Companion.seconds

class Confirm {
    lateinit var onAccept: suspend EphemeralInteractionContext.() -> Unit
    var onReject: suspend EphemeralInteractionContext.() -> Unit = { interactionResponse.delete() }

    fun accept(dsl: suspend EphemeralInteractionContext.() -> Unit) {
        onAccept = dsl
    }

    fun reject(dsl: suspend EphemeralInteractionContext.() -> Unit) {
        onReject = dsl
    }
}

context(TranslatableContext)
suspend fun EphemeralInteractionContext.confirm(dsl: Confirm.() -> Unit) {
    val confirm = Confirm()
    dsl(confirm)
    edit {
        embed {
            color = Color(216, 60, 62)
            title = translate("title", "mizore.confirm")
            description = translate("description", "mizore.confirm")
            footer {
                text = translate("footer", "mizore.confirm")
            }
        }

        lateinit var task: Task
        val components = components {
            ephemeralButton {
                disabled = true
                style = ButtonStyle.Danger
                label = translate("confirm", "mizore.confirm")

                action {
                    confirm.onAccept(this)
                }
            }

            ephemeralButton {
                style = ButtonStyle.Success
                label = translate("reject", "mizore.confirm")

                action {
                    task.cancel()
                    confirm.onReject(this)
                }
            }
        }

        task = scheduler.schedule(5.seconds) {
            (components.rows[0][0] as EphemeralInteractionButton<*>).enable()
            edit {
                with(components) { applyToMessage() }
            }
        }
    }
}
