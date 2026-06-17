package dev.feedi

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class FeediFacadeTest {
    @Test
    fun `configure enables shared facade submissions`() = runTest {
        val transport = RecordingTransport()
        Feedi.resetForTesting(
            transport = transport,
            metadataProvider = StaticMetadataProvider(),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        Feedi.configure(apiKey = "feedi_pk_shared")

        val receipt = Feedi.submitFeedback(
            message = "Please add export.",
            customMetadata = mapOf("screen" to "settings"),
        )

        assertEquals("fb_shared", receipt.id)
        assertEquals("https://api.feedi.dev/v1/feedback", transport.request.url)
        assertEquals("Bearer feedi_pk_shared", transport.request.headers["Authorization"])
    }

    private class RecordingTransport : HttpTransport {
        lateinit var request: HttpRequest

        override fun execute(request: HttpRequest): HttpResponse {
            this.request = request
            return HttpResponse(
                statusCode = 200,
                body = """{"id":"fb_shared","receivedAt":"2026-05-27T18:00:00Z"}""",
            )
        }
    }
}
