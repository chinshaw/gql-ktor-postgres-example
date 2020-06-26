package app

import graphql.Scalars
import graphql.Scalars.GraphQLID
import graphql.schema.GraphQLArgument.newArgument
import graphql.schema.GraphQLFieldDefinition.newFieldDefinition
import graphql.schema.GraphQLInputObjectField.newInputObjectField
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLList.list
import graphql.schema.GraphQLNonNull.nonNull
import graphql.schema.GraphQLObjectType as GQLOT
import graphql.schema.GraphQLObjectType.newObject
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLObjectType.Builder as GQLOTBuilder
import graphql.schema.GraphqlTypeComparatorRegistry.BY_NAME_REGISTRY
import graphql.schema.GraphQLSchema
import graphql.schema.GraphqlTypeComparatorRegistry
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.ResultSet
import graphql.schema.GraphQLSchema.Builder as GqlSB


/**
 * This is really more a graphql class since it will create
 */
class DBSchemaIntrospector(private val connectionProvider: ConnectionProvider) {

    private val queryType : GQLOTBuilder  = newObject().name("QueryType")
    private val mutationType : GQLOTBuilder = newObject().name("MutationType")
    private val comparatorRegistry: GraphqlTypeComparatorRegistry =  BY_NAME_REGISTRY

    /**
     * Public function that will generate a GraphQLSchema. It will inspect
     * the table to find columns and types to convert them into a graphql queries and types
     * at the moment.
     */
    fun genSchema(): GraphQLSchema {
        val gqlSchemaBuilder = GqlSB()
        inspectDB()
        return gqlSchemaBuilder
            .query(queryType)
            .mutation(mutationType)
            .build()
    }

    /**
     * Inspect the database that was configured for the connection provider. We grab
     * the meta data and inspect all tables that are available.
     */
    private fun inspectDB() {
        connectionProvider.get().use { dbConnection ->
            val dbInfo = DBInfo(dbConnection)
            inspectTables(dbInfo)
        }
    }

    /**
     * Inspect all the tables in the database. From the metadata we extract all tables and iterate
     * over them.
     */
    private fun inspectTables(dbInfo: DBInfo) {
        dbInfo.getTableInfo().forEach(::inspectTable)
    }

    /**
     * Inspect a single table.
     * This uses the query builder to generate a query along with type for the table.
     */
    private fun inspectTable(tableInfo: TableInfo) {
        val tableName = tableInfo.getTableName()

        val tableObjectType = newObject().name("${tableName}_table")
        val tableInputObjectType = GraphQLInputObjectType.newInputObject().name("insert_${tableName}_table")

        tableInfo.getColumnInfo().forEach { columnInfo ->
            inspectColumn(tableObjectType, columnInfo)
            inspectInputColumn(tableInputObjectType, columnInfo)
        }

        // Add ID fields required by Maana.
        tableObjectType.field(newFieldDefinition()
            .name("id")
            .type(nonNull(GraphQLID)))

        tableInputObjectType.field(
            newInputObjectField()
            .name("id")
            .type(nonNull(GraphQLID)))

        var tableType : GQLOT = tableObjectType.comparatorRegistry(comparatorRegistry).build()
        var tableInputType : GraphQLInputObjectType = tableInputObjectType.comparatorRegistry(comparatorRegistry).build()

        addQueryDefinition(tableName, tableType)
        addMutationDefinition(tableName, tableType, tableInputType)
    }

    /**
     * Inspect a single column.
     */
    private fun inspectColumn(tableEntity: GQLOTBuilder, columnInfo: ColumnInfo) {
        val columnName = columnInfo.getColumnName()

        // TODO Fix type, everything is treated as a string
        tableEntity.field(newFieldDefinition()
            .name(columnName)
            .type(columnInfo.getGraphQLType()))
    }

    /**
     * Inspect a single column as input.
     */
    private fun inspectInputColumn(tableEntity: GraphQLInputObjectType.Builder, columnInfo: ColumnInfo) {
        val columnName = columnInfo.getColumnName()

        // TODO Fix type, everything is treated as a string
        tableEntity.field(newInputObjectField()
            .name(columnName)
            .type(columnInfo.getGraphQLType()))
    }

    private fun addQueryDefinition(tableName: String, tableType: GQLOT) {
        queryType.field(newFieldDefinition()
            .name("${tableName}_table")
            .type(list(tableType))
            .dataFetcher(SqlQueryDataFetcher(connectionProvider, tableName)))
    }

    private fun addMutationDefinition(tableName: String, tableType: GQLOT, inputType: GraphQLInputObjectType) {
        mutationType.field(newFieldDefinition()
                        .name("insert_${tableName}_table")
                        .type(tableType)
                            .argument(newArgument()
                                .name("input")
                                .type(list(inputType)))
            .dataFetcher(SqlMutationDataFetcher(connectionProvider, tableName)))
    }
}


private  class DBInfo(val connection: Connection) {

    val metaData = connection.metaData

    fun getTableInfo() : Sequence<TableInfo> {
        // (String catalog, String schemaPattern, String tableNamePattern, String[] types)
        val tables = metaData.getTables(null, null, null, arrayOf("TABLE"))

        return sequence {
            while(tables.next()) {
                yield(TableInfo(metaData, tables))
            }
        }
    }
}

/**
 * Helper to get information about a table from meta data and list of columns.
 */
private class TableInfo(private val metaData: DatabaseMetaData, private val resultSet: ResultSet) {

    fun getTableName() = resultSet.getString("TABLE_NAME")!!

    fun getColumnInfo(): Sequence<ColumnInfo> {
        val columns: ResultSet = metaData.getColumns(null, null, getTableName(), null)
        return sequence {
            while(columns.next()) {
                yield(ColumnInfo(columns))
            }
        }
    }
}

/**
 * Helper to get Column information.
 *
 * Future Features:
 * val datatype = columnAsResultSet.getString("DATA_TYPE")
 * val typeName = columnAsResultSet.getString("TYPE_NAME")
 * val columnsize = columnAsResultSet.getString("COLUMN_SIZE")
 * val decimaldigits = columnAsResultSet.getString("DECIMAL_DIGITS")
 * val isNullable = columnAsResultSet.getString("IS_NULLABLE")
 * val is_autoIncrment = columnAsResultSet.getString("IS_AUTOINCREMENT")
 */
private class ColumnInfo(private val resultSet: ResultSet) {

    enum class GqlEnumType(val type: GraphQLScalarType) {
        BOOLEAN(Scalars.GraphQLString),
        VARCHAR(Scalars.GraphQLString),
        CHAR(Scalars.GraphQLString),
        TEXT(Scalars.GraphQLString),
        DOUBLE(Scalars.GraphQLBigDecimal),
        DECIMAL(Scalars.GraphQLBigDecimal),
        INT4(Scalars.GraphQLInt),
        REAL(Scalars.GraphQLBigDecimal),
        FLOAT(Scalars.GraphQLFloat),
        INTEGER(Scalars.GraphQLInt);

        companion object {
            fun type(columnTypeName: String): GraphQLScalarType {
                return valueOf(columnTypeName.toUpperCase()).type
            }
        }
    }

    fun getColumnName() = resultSet.getString("COLUMN_NAME")

    fun getColumnType() = resultSet.getString("TYPE_NAME")

    fun getGraphQLType(): GraphQLScalarType {
        val columnType = getColumnType()
        return GqlEnumType.type(columnType)
    }
}
