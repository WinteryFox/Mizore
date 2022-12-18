package bot.mizore.api.serialization

import kotlinx.serialization.Serializable

@Serializable(with = OptionalPropertySerializer::class)
sealed class OptionalProperty<out T> {
    object Missing : OptionalProperty<Nothing>()
    data class Present<T>(val value: T) : OptionalProperty<T>()

    companion object {
        fun <T> valueOrMissing(value: T?): OptionalProperty<T> =
            if (value != null)
                Present(value)
            else
                Missing
    }

    val valueOrNull: T?
        get() = when (this) {
            is Present -> value
            is Missing -> null
        }
}
