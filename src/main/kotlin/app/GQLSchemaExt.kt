package app

import graphql.GraphQL
import graphql.schema.GraphQLSchema
import graphql.schema.idl.SchemaPrinter

// Helper to convert to print a schema as text
fun GraphQLSchema.asSDL() : String {
    val schemaPrinter = SchemaPrinter();
    return schemaPrinter.print(this)
}

// Create a GraphQL executable schema
fun GraphQLSchema.asExecutable() : GraphQL {
    return GraphQL.newGraphQL(this).build()
}
