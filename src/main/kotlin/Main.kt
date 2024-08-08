import java.io.BufferedReader
import java.net.CacheRequest
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
        val request = inputClient.bufferedReader()
        val requestBody = parseInput(request)
        if (requestBody.isEmpty()) {
            break
        }
        outputClient.write("+PONG\r\n".toByteArray())
        outputClient.flush()
    }
}

// not parse input for now
fun parseInput(request: BufferedReader): String {
    return request.readLine() ?: ""
}
