import java.io.BufferedReader
import java.net.ServerSocket
import java.net.Socket

val keyValueStore = mutableMapOf<String, ValueWithExpiry>()

data class ValueWithExpiry(val value: String, val expiryTime: Long)

fun main(args: Array<String>) {
    var serverSocket = ServerSocket(6379)
    serverSocket.reuseAddress = true

    while (true) {
        val client = serverSocket.accept()
        Thread { handleRequest(client) }.start()
    }
}

fun handleRequest(client: Socket) {
    val inputClient = client.getInputStream()
    val outputClient = client.getOutputStream()

    while (true) {
        val reader = inputClient.bufferedReader()
        val request = readRequest(reader)

        if (!isValidRequestBody(request)) {
            return
        }

        val response = when (request.command) {
            "PING" -> "+PONG\r\n"
            "ECHO" -> formatBulkString(request.arguments.joinToString(""))
            "SET" -> handleSetRequest(request.arguments)
            "GET" -> handleGetRequest(request.arguments)
            else -> return
        }

        println(response)

        outputClient.write(response.toByteArray())
        outputClient.flush()
    }
}

fun handleSetRequest(arguments: List<String>): String {
    val key = arguments[0]
    val value = arguments[1]
    val simpleString = "+OK\r\n"

    if (arguments.size == 2) {
        keyValueStore[key] = ValueWithExpiry(value, Long.MAX_VALUE)
        return simpleString
    }

    val isExpiry = arguments[2].lowercase() == "px"

    if (!isExpiry) {
        return "-ERR syntax error"
    }

    val expiryTime = arguments[3].toLong()
    val expiryInMs = System.currentTimeMillis() + expiryTime

    keyValueStore[key] = ValueWithExpiry(value, expiryInMs)

    return simpleString
}

fun handleGetRequest(arguments: List<String>): String {
    val key = arguments[0]
    val nullBulkString = "$-1\r\n"

    keyValueStore[key]?.let {
        if (it.expiryTime < System.currentTimeMillis()) {
            keyValueStore.remove(key)
            return nullBulkString
        }
        return formatBulkString(it.value)
    }

    return nullBulkString
}

data class RedisRequest(val command: String, val arguments: List<String>)

fun readRequest(reader: BufferedReader): RedisRequest {
    val numberOfArgs = reader.readLine().substring(1).toInt()
    var command = ""
    val arguments = mutableListOf<String>()

    for (i in 0 until numberOfArgs) {
        val argLength = reader.readLine().substring(1).toInt()
        val arg = reader.readLine()
        if (i == 0) {
            command = arg
        } else {
            arguments.add(arg)
        }
    }

    println("Command: $command, Arguments: $arguments")

    return RedisRequest(command, arguments)
}

fun isValidRequestBody(requestBody: RedisRequest): Boolean {
    return requestBody.command.isNotEmpty()
}

fun formatBulkString(data: String): String {
    return "$${data.length}\r\n$data\r\n"
}
