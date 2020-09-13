package bot.horo.command

import bot.horo.Client
import kotlinx.coroutines.*
import kotlinx.coroutines.reactive.awaitSingle
import java.awt.Color
import java.awt.Font
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
import javax.imageio.stream.FileImageInputStream

fun CommandsBuilder.animals() =
    command("animals") {
        dispatch {
            val name = event.member.get().id.asString() + LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".png"
            val animals = localization.translate("show-animals", event.member.get())

            val image = BufferedImage(760, 618, BufferedImage.TYPE_INT_ARGB)

            val output = ByteArrayOutputStream()
            withContext(Dispatchers.Default) {
                val graphics = image.graphics

                graphics.drawImage(ImageIO.read(Client::class.java.getResourceAsStream("/animals/fox/fox.png")), 0, 0, null)

                graphics.dispose()
                ImageIO.write(image, "png", output)
            }

            event.message.channel.awaitSingle().createMessage { message ->
                message.setEmbed {
                    it.setTitle(animals)
                        .setImage("attachment://$name")
                }.addFile(name, ByteArrayInputStream(output.toByteArray()))
            }.awaitSingle()
        }
    }