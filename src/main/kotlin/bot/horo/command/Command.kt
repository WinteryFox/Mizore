package bot.horo.command

import discord4j.rest.util.Permission
import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import kotlin.reflect.jvm.kotlinFunction

internal val commands = Reflections(
    ConfigurationBuilder().setUrls(ClasspathHelper.forJavaClassPath()).setScanners(MethodAnnotationsScanner())
)
    .getMethodsAnnotatedWith(Command::class.java)
    .map { method -> method.kotlinFunction!! }

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Command(
    val botPermissions: Array<Permission> = [Permission.SEND_MESSAGES],
    val userPermissions: Array<Permission> = [Permission.SEND_MESSAGES]
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Parameter