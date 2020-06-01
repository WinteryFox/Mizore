package bot.horo

import kotlinx.coroutines.ThreadContextElement
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class CoroutineName(private val name: String) : AbstractCoroutineContextElement(Key), ThreadContextElement<String> {
    override fun updateThreadContext(context: CoroutineContext): String {
        val previousName = Thread.currentThread().name
        Thread.currentThread().name = "${Thread.currentThread().name}@$name"
        return previousName
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: String) {
        Thread.currentThread().name = oldState
    }

    companion object Key : CoroutineContext.Key<CoroutineName>
}