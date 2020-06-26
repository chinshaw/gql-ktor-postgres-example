package app

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment


class SqlQueryDataFetcher(private val connectionProvider: ConnectionProvider,
                          private val columnName: String) : DataFetcher<List<Map<Any,Any>>> {

    override fun get(environment: DataFetchingEnvironment?): List<Map<Any,Any>> {
        return fetchAll()
    }

    /**
     * Fetch all entities in the table by column name. This is a select all from column. It is not
     * the most efficient solution and is a
     */
    fun fetchAll(): MutableList<Map<Any, Any>> {
        val values = mutableListOf<Map<Any, Any>>()
        connectionProvider.get().use { table ->
            val stmt = table.createStatement()
            val resultSet = stmt.executeQuery("SELECT * FROM ${columnName}")
            resultSet.use {
                while(it.next()) {
                    val metaData = it.metaData
                    val map = mutableMapOf<Any, Any>()
                    for (i in 1 until metaData.columnCount) {
                        val col = metaData.getColumnName(i)
                        map[col] = it.getObject(col) ?: ""
                    }
                    values.add(map)
                }
            }
        }
        return values
    }
}



class SqlMutationDataFetcher(private val connectionProvider: ConnectionProvider,
                             private val tableName: String): DataFetcher<List<Map<Any, Any>>> {

    override fun get(environment: DataFetchingEnvironment?): List<Map<Any, Any>> {
        environment?.let {
            val inputs = environment.arguments["input"] as List<Any>
            inputs.forEach {
                val input = it as Map<String, Any>
                connectionProvider.get().use { conn ->
                    val keys = input.keys.joinToString(" ,")
                    val values = input.values.map{
                        "?"
                    }.joinToString(", ")

                    val prepareStatement = conn.prepareStatement("INSERT INTO ${tableName}(${keys}) VALUES(${values})")

                    input.values.forEachIndexed {idx, v -> prepareStatement.setObject((idx + 1), v)}
                    prepareStatement.executeUpdate()
                }
            }
        }
        return listOf()
    }
}
