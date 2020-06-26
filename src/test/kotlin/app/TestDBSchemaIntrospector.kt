//package db
//
//import app.ConnectionProvider
//import app.DBSchemaIntrospector
//import app.asSDL
//import graphql.schema.idl.SchemaPrinter
//import org.junit.Test
//
//class TestDBSchemaIntrospector {
//
//    val connectionProvider: ConnectionProvider =
//            ConnectionProvider("jdbc:postgresql://localhost:5432/cogo", "chinshaw", "postgres")
//
//    @Test
//    fun test_Inspect() {
//        val inspector = DBSchemaIntrospector(connectionProvider)
//        val gqlSchema = inspector.genSchema()
//
//        val schemaPrinter = SchemaPrinter();
//        val sdlSchema = schemaPrinter.print(gqlSchema)
//        print(sdlSchema)
//    }
//
//    @Test
//    fun `Get Schema and Inspect Execute`() {
//        val inspector = DBSchemaIntrospector(connectionProvider)
//        val gqlSchema = inspector.genSchema()
//        println(gqlSchema.asSDL())
//
//        val gql = app.asExecutable()
//        val result = gql.execute(
//            "{rig_table { " +
//                    "id " +
//                    "}}"
//        )
//        print(result.errors)
//        println(result.getData<Any>().toString())
//
//        val q = """
//                    query IntrospectionQueryTypeQuery {
//                        __schema {
//                            queryType {
//                                name
//                            }
//                        }
//                    }
//        """.trimIndent()
//
//        val fullQuery = """
//            {"query":"
//                query IntrospectionQuery {
//                    __schema {
//                        queryType { name }
//                        mutationType { name }
//                        types {        ...FullType\n      }\n      directives {\n        name\n        description\n        locations\n        args {\n          ...InputValue\n        }\n      }\n    }\n  }\n\n  fragment FullType on __Type {\n    kind\n    name\n    description\n    fields(includeDeprecated: true) {\n      name\n      description\n      args {\n        ...InputValue\n      }\n      type {\n        ...TypeRef\n      }\n      isDeprecated\n      deprecationReason\n    }\n    inputFields {\n      ...InputValue\n    }\n    interfaces {\n      ...TypeRef\n    }\n    enumValues(includeDeprecated: true) {\n      name\n      description\n      isDeprecated\n      deprecationReason\n    }\n    possibleTypes {\n      ...TypeRef\n    }\n  }\n\n  fragment InputValue on __InputValue {\n    name\n    description\n    type { ...TypeRef }\n    defaultValue\n  }\n\n  fragment TypeRef on __Type {\n    kind\n    name\n    ofType {\n      kind\n      name\n      ofType {\n        kind\n        name\n        ofType {\n          kind\n          name\n          ofType {\n            kind\n            name\n            ofType {\n              kind\n              name\n              ofType {\n                kind\n                name\n                ofType {\n                  kind\n                  name\n                }\n              }\n            }\n          }\n        }\n      }\n    }\n  }\n"}
//        """.trimIndent()
////        val query = "{\"query\":\"\\n  query IntrospectionQuery {\\n    __schema {\\n      queryType { name }\\n      mutationType { name }\\n      subscriptionType { name }\\n      types {\\n        ...FullType\\n      }\\n      directives {\\n        name\\n        description\\n        locations\\n        args {\\n          ...InputValue\\n        }\\n      }\\n    }\\n  }\\n\\n  fragment FullType on __Type {\\n    kind\\n    name\\n    description\\n    fields(includeDeprecated: true) {\\n      name\\n      description\\n      args {\\n        ...InputValue\\n      }\\n      type {\\n        ...TypeRef\\n      }\\n      isDeprecated\\n      deprecationReason\\n    }\\n    inputFields {\\n      ...InputValue\\n    }\\n    interfaces {\\n      ...TypeRef\\n    }\\n    enumValues(includeDeprecated: true) {\\n      name\\n      description\\n      isDeprecated\\n      deprecationReason\\n    }\\n    possibleTypes {\\n      ...TypeRef\\n    }\\n  }\\n\\n  fragment InputValue on __InputValue {\\n    name\\n    description\\n    type { ...TypeRef }\\n    defaultValue\\n  }\\n\\n  fragment TypeRef on __Type {\\n    kind\\n    name\\n    ofType {\\n      kind\\n      name\\n      ofType {\\n        kind\\n        name\\n        ofType {\\n          kind\\n          name\\n          ofType {\\n            kind\\n            name\\n            ofType {\\n              kind\\n              name\\n              ofType {\\n                kind\\n                name\\n                ofType {\\n                  kind\\n                  name\\n                }\\n              }\\n            }\\n          }\\n        }\\n      }\\n    }\\n  }\\n\"}\n"
//        val r = gql.execute(q)
//        println(r.getData<Any>().toString())
//
//
//    }
//}
