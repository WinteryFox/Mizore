package bot.horo.command

import discord4j.rest.util.Permission

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Command(
    val botPermissions: Array<Permission> = [Permission.SEND_MESSAGES],
    val userPermissions: Array<Permission> = [Permission.SEND_MESSAGES]
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Parameter(
    val name: String
)