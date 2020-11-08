package bot.horo.command

import bot.horo.Client
import bot.horo.data.Animal
import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.coroutines.*
import kotlinx.coroutines.reactive.awaitSingle
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

fun CommandsBuilder.animals() =
    command("animals") {
        dispatch {
            val animals = localization.translate("show-animals", event.member.get())

            val image = BufferedImage(760, 618, BufferedImage.TYPE_INT_ARGB)

            val animal = database.query(
                "SELECT * FROM animals WHERE snowflake=$1 AND id=$2",
                mapOf(Pair("$1", event.member.get().id), Pair("$2", 4))) { row: Row, metadata: RowMetadata ->
                return@query Animal(
                    row["snowflake"] as Long,
                    row["id"] as Int,
                    row["name"] as String,
                    row["level"] as Short,
                    row["experience"] as Int,
                    row["hunger"] as Short,
                    row["boredom"] as Short
                )
            }.single()

            val output = ByteArrayOutputStream()
            withContext(Dispatchers.Default) {
                val graphics = image.graphics

                graphics.drawString(animal.name, 0, 0)
                graphics.drawImage(ImageIO.read(Client::class.java.getResourceAsStream("/animals/fox/fox.png")), 0, 0, null)

                graphics.dispose()
                ImageIO.write(image, "png", output)
            }

            event.message.channel.awaitSingle().createMessage { message ->
                message.setEmbed {
                    it.setTitle(animals)
                        .setImage("attachment://animal.png")
                }.addFile("animal.png", ByteArrayInputStream(output.toByteArray()))
            }.awaitSingle()
        }

        subcommand("feed") {
            parameter("food", true)

            dispatch {
                database.execute(
                    """
                        UPDATE animals SET experience = experience + 15 WHERE snowflake = $1
                    """,
                    mapOf(Pair("$1", event.member.get().id))
                )

                val animal = database.query(
                    "SELECT * FROM animals WHERE snowflake=$1 AND id=$2",
                    mapOf(Pair("$1", event.member.get().id), Pair("$2", 4))) { row: Row, metadata: RowMetadata ->
                    return@query Animal(
                        row["snowflake"] as Long,
                        row["id"] as Int,
                        row["name"] as String,
                        row["level"] as Short,
                        row["experience"] as Int,
                        row["hunger"] as Short,
                        row["boredom"] as Short
                    )
                }.single()

                event.message.channel.awaitSingle().createMessage { message ->
                    message.setEmbed {
                        it.setTitle("You fed your wolf a ${parameters["food"]!!.capitalize()}")
                            .addField("Name", animal.name, false)
                            .addField("Level", animal.level.toString(), false)
                            .addField("Experience", animal.experience.toString(), false)
                    }
                }.awaitSingle()
            }
        }
    }