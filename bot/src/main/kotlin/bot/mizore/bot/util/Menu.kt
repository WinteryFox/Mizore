package bot.mizore.bot.util

import com.kotlindiscord.kord.extensions.types.EphemeralInteractionContext
import com.kotlindiscord.kord.extensions.types.edit
import dev.kord.rest.builder.message.modify.MessageModifyBuilder
import kotlin.collections.ArrayDeque

typealias Page = suspend MessageModifyBuilder.() -> Unit

abstract class Menu {
    var currentPage: String? = null
    val history: ArrayDeque<String> = ArrayDeque()
    protected val pages: MutableMap<String, Page> = mutableMapOf()

    fun page(name: String, page: Page) {
        pages[name] = page
    }
}

class EphemeralMenu(
    val context: EphemeralInteractionContext
) : Menu() {
    suspend fun previous() {
        val page = history.removeLastOrNull() ?: return
        open(page, false)
    }

    suspend fun open(page: String, updateHistory: Boolean = true) {
        context.edit {
            pages[page]!!()
        }
        if (updateHistory && currentPage != null)
            history.add(currentPage!!)
        currentPage = page
    }

    suspend fun refresh() {
        context.edit {
            pages[currentPage]!!()
        }
    }
}

suspend fun EphemeralInteractionContext.menu(
    initialPage: String,
    dsl: suspend EphemeralMenu.() -> Unit
): EphemeralMenu {
    val menu = EphemeralMenu(this)
    dsl(menu)
    menu.open(initialPage)
    return menu
}
