package bot.horo.data.codec

import discord4j.common.util.Snowflake
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.r2dbc.postgresql.client.Parameter
import io.r2dbc.postgresql.codec.Codec
import io.r2dbc.postgresql.message.Format
import io.r2dbc.postgresql.type.PostgresqlObjectId
import io.r2dbc.postgresql.util.ByteBufUtils
import reactor.core.publisher.Mono

class SnowflakeCodec(private val allocator: ByteBufAllocator) : Codec<Snowflake> {
    override fun canEncodeNull(type: Class<*>): Boolean = false

    override fun canEncode(value: Any): Boolean = value is Snowflake

    override fun encode(value: Any): Parameter =
        Parameter(
            Format.FORMAT_TEXT,
            PostgresqlObjectId.NUMERIC.objectId,
            Mono.fromCallable { ByteBufUtils.encode(allocator, (value as Snowflake).asString()) }
        )

    override fun canDecode(dataType: Int, format: Format, type: Class<*>): Boolean = false

    override fun decode(buffer: ByteBuf?, dataType: Int, format: Format, type: Class<out Snowflake>): Snowflake? =
        null

    override fun type(): Class<*> = Snowflake::class.java

    override fun encodeNull(): Parameter =
        Parameter(Format.FORMAT_TEXT, PostgresqlObjectId.NUMERIC.objectId, Parameter.NULL_VALUE)
}