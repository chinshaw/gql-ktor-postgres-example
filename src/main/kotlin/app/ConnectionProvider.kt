package app

import java.sql.Connection
import java.sql.DriverManager
import java.util.*

class ConnectionProvider(val uri: String, val username: String, val password: String) {

    val props = Properties()
    init {
        props.setProperty("user", username)
        props.setProperty("password", password)
    }

    /**
     * This connection should be closed when completed.
     */
    fun get() : Connection {
        return  DriverManager.getConnection(uri, props)
    }
}
