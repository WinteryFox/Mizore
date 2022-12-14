package bot.horo.api.serialization

import kotlinx.serialization.Serializable

@Serializable(with = OptionalPropertySerializer::class)
sealed class OptionalProperty<out T> {
    object Missing : OptionalProperty<Nothing>()

    data class Present<T>(val value: T) : OptionalProperty<T>()

    val valueOrNull: T?
        get() = when (this) {
            is Present -> value
            is Missing -> null
        }
}
