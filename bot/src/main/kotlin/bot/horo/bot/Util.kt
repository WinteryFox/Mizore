package bot.horo.bot

fun String.parseHex(): Int? {
    var res = removePrefix("#").ifBlank { null } ?: return null
    if (res.length > 6)
        return null
    if (res.length < 6)
        res = res.padEnd(6 - res.length, res[res.length - 1])

    if (!"^#[a-f0-9]{2}[a-f0-9]{2}[a-f0-9]{2}\$".toRegex().matches(res))
        return null
    return res.toInt(16)
}
