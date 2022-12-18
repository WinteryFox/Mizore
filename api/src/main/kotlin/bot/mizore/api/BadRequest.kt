package bot.mizore.api

import io.ktor.http.*
import kotlinx.serialization.Serializable

sealed class BadRequestException(
    val code: Short,
    message: String,
    val status: HttpStatusCode = HttpStatusCode.BadRequest,
    cause: Throwable? = null
) : Exception(message, cause) {
    class FailedToFetchChannelException : BadRequestException(
        0,
        "Failed to fetch the requested channel"
    )

    class InvalidChannelTypeException : BadRequestException(
        1,
        "Channel is of invalid type"
    )

    class InsufficientPermissionsException : BadRequestException(
        2,
        "The bot has insufficient permissions to perform this action"
    )

    class FailedToFetchMessageException : BadRequestException(
        3,
        "Failed to fetch the requested message"
    )
}

@Serializable
data class BadRequestResponse(
    val code: Short,
    val message: String,
    val status: Int
)
