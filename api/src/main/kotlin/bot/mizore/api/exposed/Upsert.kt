package bot.mizore.api.exposed

import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager

/*inline fun <ID : Comparable<ID>, T : Entity<ID>> EntityClass<ID, T>.upsert(
    id: ID?,
    init: EntityClass<ID, T>.() -> Unit
) = InsertOrUpdate(
    table,
    keys = (primaryKey ?: throw IllegalArgumentException("No primary key defined")).columns
).apply {
    //body(this)
    //val prototype = Entity(DaoEntityID(id, table as IdTable<ID>))
    init(this)
    execute(TransactionManager.current())
}*/

/*fun <ID : Comparable<*>, T : EntityClass<ID, Entity<ID>>> T.upsert(
    id: ID,
    body: T.(InsertStatement<ID>) -> Unit
) = InsertOrUpdate<ID>(
    table,
    keys = (primaryKey ?: throw IllegalArgumentException("No primary key defined")).columns
).apply {
    body(this)
    execute(TransactionManager.current())
}

fun <ID : Comparable<*>, E : EntityClass<ID, Entity<ID>>> E.upsert(
    vararg keys: Column<*> = (primaryKey ?: throw IllegalArgumentException("No primary key defined")).columns,
    body: E.(InsertStatement<ID>) -> Unit
) = InsertOrUpdate<ID>(table, keys = keys).apply {
    body(this)
    execute(TransactionManager.current())
}

fun <ID : Comparable<*>, T : IdTable<ID>> T.upsert(
    vararg keys: Column<*> = (primaryKey ?: throw IllegalArgumentException("primary key is missing")).columns,
    body: T.(InsertStatement<ID>) -> Unit
) = InsertOrUpdate<ID>(this, keys = keys).apply {
    body(this)
    execute(TransactionManager.current())
}*/

class InsertOrUpdate<Key : Comparable<Key>>(
    table: IdTable<Key>,
    isIgnore: Boolean = false,
    private vararg val keys: Column<*>
) : InsertStatement<Key>(table, isIgnore) {
    override fun prepareSQL(transaction: Transaction): String {
        val tm = TransactionManager.current()
        val updateSetter =
            (table.columns - keys.toSet()).joinToString { "${tm.identity(it)} = EXCLUDED.${tm.identity(it)}" }
        val onConflict = "ON CONFLICT (${keys.joinToString { tm.identity(it) }}) DO UPDATE SET $updateSetter"
        return "${super.prepareSQL(transaction)} $onConflict"
    }
}

fun <T : IdTable<*>, E> T.batchUpsert(
    data: Collection<E>,
    vararg keys: Column<*> = (primaryKey ?: throw IllegalArgumentException("primary key is missing")).columns,
    ignore: Boolean = false,
    shouldReturnGeneratedValues: Boolean = false,
    body: T.(BatchInsertStatement, E) -> Unit
) =
    BatchInsertOrUpdate(
        this,
        keys = keys,
        isIgnore = ignore,
        shouldReturnGeneratedValues = shouldReturnGeneratedValues
    ).apply {
        data.forEach {
            addBatch()
            body(this, it)
        }
        execute(TransactionManager.current())
    }

class BatchInsertOrUpdate(
    table: IdTable<*>,
    isIgnore: Boolean,
    private vararg val keys: Column<*>,
    shouldReturnGeneratedValues: Boolean
) : BatchInsertStatement(table, isIgnore, shouldReturnGeneratedValues) {
    override fun prepareSQL(transaction: Transaction): String {
        val tm = TransactionManager.current()
        val updateSetter = (table.columns - keys).joinToString { "${tm.identity(it)} = EXCLUDED.${tm.identity(it)}" }
        val onConflict = "ON CONFLICT (${keys.joinToString { tm.identity(it) }}) DO UPDATE SET $updateSetter"
        return "${super.prepareSQL(transaction)} $onConflict"
    }
}
