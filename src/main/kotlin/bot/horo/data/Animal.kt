package bot.horo.data

open class Animal(
    val snowflake: Long,
    val id: Int,
    val name: String,
    val level: Short,
    val experience: Int,
    val hunger: Short,
    val boredom: Short
)