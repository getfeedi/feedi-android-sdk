package dev.feedi

import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI

internal interface HttpTransport {
    fun execute(request: HttpRequest): HttpResponse
}

internal data class HttpRequest(
    val url: String,
    val method: String,
    val headers: Map<String, String>,
    val body: String,
)

internal data class HttpResponse(
    val statusCode: Int,
    val body: String,
)

internal class UrlConnectionHttpTransport : HttpTransport {
    override fun execute(request: HttpRequest): HttpResponse {
        val connection = URI(request.url).toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = request.method
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.doOutput = true
            request.headers.forEach { (name, value) ->
                connection.setRequestProperty(name, value)
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(request.body)
            }

            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            return HttpResponse(statusCode = statusCode, body = body)
        } finally {
            connection.disconnect()
        }
    }
}
