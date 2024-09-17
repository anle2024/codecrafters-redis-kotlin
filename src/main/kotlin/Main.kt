import java.io.BufferedReader
import java.net.ServerSocket
import java.net.Socket

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

        val response = when(request.command) {
            "PING" -> "+PONG\r\n"
            "ECHO" -> formatBulkString(request.arguments.joinToString(""))
            else -> return
        }
        println(response)

        outputClient.write(response.toByteArray())
        outputClient.flush()
    }
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
