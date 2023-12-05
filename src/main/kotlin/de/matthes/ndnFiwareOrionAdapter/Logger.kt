package de.matthes.ndnFiwareOrionAdapter

class Logger {

    enum class LogLevel(val i: Int, val text: String) {
        DEBUG(2, "DEBUG"),
        INFO(1, "INFO "),
    }

    var level = LogLevel.INFO

    constructor(levelString: String) {
        level = when(levelString.uppercase()) {
            "DEBUG" -> LogLevel.DEBUG
            "INFO" -> LogLevel.INFO
            else -> LogLevel.INFO
        }
    }

    fun log(level: LogLevel, msg: String) {
        if (level > this.level) {
            return
        }
        println("[${level.text}] $msg")
    }

    fun info(msg: String) {
        log(LogLevel.INFO, msg)
    }

    fun debug(msg: String) {
        log(LogLevel.DEBUG, msg)
    }

}