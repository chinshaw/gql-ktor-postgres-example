package app

import graphql.VisibleForTesting
import graphql.schema.GraphQLSchema
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.config.ApplicationConfig
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.http.ContentType
import io.ktor.http.content.default
import io.ktor.http.content.static
import io.ktor.jackson.JacksonConverter
import io.ktor.request.receive
import io.ktor.response.respondText
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.netty.EngineMain
import org.slf4j.event.Level


fun Application.module() {
    install(DefaultHeaders)
    install(CallLogging) {
        level = Level.INFO
    }
    // CORS with anyHost so that it works in Kubernetes
    install(CORS) {
        anyHost()
    }
    // Register the jackson converter for mapping requests and responses.
    install(ContentNegotiation) {
        val converter = JacksonConverter(mapper)
        register(ContentType.Application.Json, converter)
    }

    // Here we pass the ktor environment config to create an executable schema.
    val executableSchema = getSchema(environment.config).asExecutable();

    routing {
        // Adding serving of the GraphQL
        static("/") {
            default("index.html")
        }

        // Entry for the graphql service where we
        post("/graphql") {
            val request = call.receive<GQlRequest>()
            val executionResult = executableSchema.execute(request.query)
            val jsonResponse = mapper.writeValueAsString(executionResult)
            // Send the response back as json
            call.respondText(jsonResponse, ContentType.Application.Json)
        }
    }
}

// DTO representation of a graphql request
private data class GQlRequest(var query: String, var variables: String?, var operationName: String?)

/**
 * Start the netty server to host the application.
 */
fun main(args: Array<String>) {
    EngineMain.main(args)
}

/**
 * This function creates a connection provider and an database introspection provider to
 * create a GraphQLSchema.
 */
@VisibleForTesting
internal fun getSchema(config: ApplicationConfig): GraphQLSchema {
    val connectionProvider: ConnectionProvider = createConnectionProvider(config)
    val schemaIntrospector = DBSchemaIntrospector(connectionProvider);
    return schemaIntrospector.genSchema();
}

// Create the connection provider for DB used in introspector and Data Fetchers
private fun createConnectionProvider(config: ApplicationConfig): ConnectionProvider {
    val username = config.property("ktor.db.username").getString()
    val password = config.property("ktor.db.password").getString()
    val uri = config.property("ktor.db.uri").getString()

    return ConnectionProvider(uri, username, password)
}


