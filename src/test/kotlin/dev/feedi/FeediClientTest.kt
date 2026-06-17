package dev.feedi

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import java.time.Instant
import java.util.concurrent.Executors
import org.junit.Test
import org.json.JSONObject
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class FeediClientTest {
    @Test
    fun `submit feedback sends authorized api contract payload and returns receipt`() = runTest {
        val transport = RecordingTransport(
            response = HttpResponse(
                statusCode = 201,
                body = """{"id":"fb_123","receivedAt":"2026-05-27T18:00:00.000Z"}""",
            ),
        )
        val client = FeediClient(
            configuration = FeediConfiguration(apiKey = "feedi_pk_test", apiBaseUrl = "https://api.feedi.dev"),
            transport = transport,
            metadataProvider = StaticMetadataProvider(
                mapOf(
                    "appVersion" to "1.2.3",
                    "buildNumber" to "42",
                    "androidVersion" to "15",
                    "locale" to "en-DE",
                ),
            ),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

        val receipt = client.submitFeedback(
            message = "  The import screen is confusing.  ",
            userEmail = " founder@example.com ",
            customMetadata = mapOf(" screen " to " import "),
        )

        assertEquals(
            FeediReceipt(id = "fb_123", receivedAt = Instant.parse("2026-05-27T18:00:00.000Z")),
            receipt,
        )
        assertEquals("https://api.feedi.dev/v1/feedback", transport.request.url)
        assertEquals("POST", transport.request.method)
        assertEquals("Bearer feedi_pk_test", transport.request.headers["Authorization"])
        assertEquals("application/json; charset=utf-8", transport.request.headers["Content-Type"])
        val body = JSONObject(transport.request.body)
        assertEquals("The import screen is confusing.", body.getString("message"))
        assertEquals("founder@example.com", body.getString("email"))
        assertEquals("com.example.app", body.getString("appIdentifier"))
        assertEquals("1.2.3", body.getJSONObject("metadata").getString("appVersion"))
        assertEquals("42", body.getJSONObject("metadata").getString("buildNumber"))
        assertEquals("15", body.getJSONObject("metadata").getString("androidVersion"))
        assertEquals("en-DE", body.getJSONObject("metadata").getString("locale"))
        assertEquals("import", body.getJSONObject("customMetadata").getString("screen"))
    }

    @Test
    fun `submit feedback requires configure before using shared facade`() = runTest {
        Feedi.resetForTesting()

        assertFailsWith<FeediError.NotConfigured> {
            Feedi.submitFeedback(message = "Hello")
        }
    }

    @Test
    fun `submit feedback reports not configured before validating input`() = runTest {
        val transport = RecordingTransport()
        val client = FeediClient(
            configuration = null,
            transport = transport,
            metadataProvider = StaticMetadataProvider(),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

        assertFailsWith<FeediError.NotConfigured> {
            client.submitFeedback(message = "  ")
        }
        assertEquals(0, transport.callCount)
    }

    @Test
    fun `submit feedback validates message email and metadata before transport`() = runTest {
        val transport = RecordingTransport()
        val client = FeediClient(
            configuration = FeediConfiguration(apiKey = "feedi_pk_test"),
            transport = transport,
            metadataProvider = StaticMetadataProvider(),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

        assertFailsWith<FeediError.InvalidMessage> {
            client.submitFeedback(message = "  ")
        }
        assertFailsWith<FeediError.InvalidEmail> {
            client.submitFeedback(message = "Hello", userEmail = "not-email")
        }
        assertFailsWith<FeediError.InvalidCustomMetadata> {
            client.submitFeedback(
                message = "Hello",
                customMetadata = (1..11).associate { "key$it" to "value" },
            )
        }
        assertEquals(0, transport.callCount)
    }

    @Test
    fun `configure rejects invalid public keys and unsafe api base urls`() = runTest {
        val client = FeediClient()

        assertFailsWith<FeediError.InvalidConfiguration> {
            client.configure(apiKey = "  ")
        }
        assertFailsWith<FeediError.InvalidConfiguration> {
            client.configure(apiKey = "secret_key")
        }
        assertFailsWith<FeediError.InvalidConfiguration> {
            client.configure(apiKey = "feedi_pk_test", apiBaseUrl = "http://api.example.com")
        }
        assertFailsWith<FeediError.InvalidConfiguration> {
            client.configure(apiKey = "feedi_pk_test", apiBaseUrl = "not a url")
        }
    }

    @Test
    fun `configure accepts region segmented public write keys`() = runTest {
        val transport = RecordingTransport()
        val client = FeediClient(
            configuration = null,
            transport = transport,
            metadataProvider = StaticMetadataProvider(),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

        client.configure(apiKey = " feedi_us_pk_live ")

        client.submitFeedback(message = "Hello")

        assertEquals("Bearer feedi_us_pk_live", transport.request.headers["Authorization"])
    }

    @Test
    fun `configure allows explicit local http api base url for integration tests`() = runTest {
        val transport = RecordingTransport()
        val client = FeediClient(
            configuration = null,
            transport = transport,
            metadataProvider = StaticMetadataProvider(),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

        client.configure(apiKey = " feedi_pk_test ", apiBaseUrl = " http://127.0.0.1:8787/ ")

        client.submitFeedback(message = "Hello")

        assertEquals("http://127.0.0.1:8787/v1/feedback", transport.request.url)
        assertEquals("Bearer feedi_pk_test", transport.request.headers["Authorization"])
    }

    @Test
    fun `submit feedback maps invalid receipts to decoding error`() = runTest {
        val client = FeediClient(
            configuration = FeediConfiguration(apiKey = "feedi_pk_test"),
            transport = RecordingTransport(HttpResponse(statusCode = 201, body = """{"id":"fb_missing_date"}""")),
            metadataProvider = StaticMetadataProvider(),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

        assertFailsWith<FeediError.DecodingError> {
            client.submitFeedback(message = "Hello")
        }
    }

    @Test
    fun `submit feedback maps api failures`() = runTest {
        val rateLimited = FeediClient(
            configuration = FeediConfiguration(apiKey = "feedi_pk_test"),
            transport = RecordingTransport(HttpResponse(statusCode = 429, body = """{"error":"rate_limited"}""")),
            metadataProvider = StaticMetadataProvider(),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        val unavailable = FeediClient(
            configuration = FeediConfiguration(apiKey = "feedi_pk_test"),
            transport = RecordingTransport(HttpResponse(statusCode = 503, body = """{"error":"unavailable"}""")),
            metadataProvider = StaticMetadataProvider(),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        val failed = FeediClient(
            configuration = FeediConfiguration(apiKey = "feedi_pk_test"),
            transport = RecordingTransport(HttpResponse(statusCode = 400, body = """{"error":"bad_request"}""")),
            metadataProvider = StaticMetadataProvider(),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        val mismatch = FeediClient(
            configuration = FeediConfiguration(apiKey = "feedi_pk_test"),
            transport = RecordingTransport(HttpResponse(statusCode = 403, body = """{"error":"app_identifier_mismatch"}""")),
            metadataProvider = StaticMetadataProvider(),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

        assertFailsWith<FeediError.RateLimited> {
            rateLimited.submitFeedback(message = "Hello")
        }
        assertFailsWith<FeediError.AppIdentifierMismatch> {
            mismatch.submitFeedback(message = "Hello")
        }
        assertFailsWith<FeediError.ServerUnavailable> {
            unavailable.submitFeedback(message = "Hello")
        }
        val error = assertFailsWith<FeediError.RequestFailed> {
            failed.submitFeedback(message = "Hello")
        }
        assertEquals(400, error.statusCode)
    }

    @Test
    fun `submit feedback runs blocking transport on configured dispatcher`() = runTest {
        val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "feedi-test-io")
        }
        val dispatcher = executor.asCoroutineDispatcher()
        val transport = RecordingTransport(
            response = HttpResponse(
                statusCode = 200,
                body = """{"id":"fb_dispatcher","receivedAt":"2026-05-27T18:00:00Z"}""",
            ),
        )
        val client = FeediClient(
            configuration = FeediConfiguration(apiKey = "feedi_pk_test"),
            transport = transport,
            metadataProvider = StaticMetadataProvider(),
            ioDispatcher = dispatcher,
        )

        val receipt = try {
            client.submitFeedback(message = "Hello")
        } finally {
            dispatcher.close()
            executor.shutdown()
        }

        assertEquals("fb_dispatcher", receipt.id)
        assertEquals("feedi-test-io", transport.threadName)
    }

    private class RecordingTransport(
        private val response: HttpResponse = HttpResponse(
            statusCode = 200,
            body = """{"id":"fb_default","receivedAt":"2026-05-27T18:00:00Z"}""",
        ),
    ) : HttpTransport {
        lateinit var request: HttpRequest
        var callCount: Int = 0
            private set
        var threadName: String = ""
            private set

        override fun execute(request: HttpRequest): HttpResponse {
            this.request = request
            callCount += 1
            threadName = Thread.currentThread().name
            return response
        }
    }
}

internal class StaticMetadataProvider(
    private val values: Map<String, String> = mapOf(
        "appVersion" to "unknown",
        "buildNumber" to "unknown",
        "androidVersion" to "unknown",
        "locale" to "und",
    ),
    private val appId: String = "com.example.app",
) : MetadataProvider {
    override fun metadata(): Map<String, String> = values

    override fun appIdentifier(): String = appId
}
