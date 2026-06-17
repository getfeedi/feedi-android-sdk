package dev.feedi

import android.content.Context
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

public class FeediClient internal constructor(
    configuration: FeediConfiguration?,
    private val transport: HttpTransport,
    metadataProvider: MetadataProvider,
    private val ioDispatcher: CoroutineDispatcher,
) : FeediFeedbackSubmitting {
    private val state = AtomicReference<ClientState?>(
        configuration?.let { ClientState(configuration = it, metadataProvider = metadataProvider) },
    )

    public constructor(apiKey: String) : this(
        configuration = FeediConfiguration(apiKey = apiKey),
        transport = UrlConnectionHttpTransport(),
        metadataProvider = EmptyMetadataProvider,
        ioDispatcher = Dispatchers.IO,
    )

    public constructor(
        apiKey: String,
        apiBaseUrl: String,
    ) : this(
        configuration = FeediConfiguration(apiKey = apiKey, apiBaseUrl = apiBaseUrl),
        transport = UrlConnectionHttpTransport(),
        metadataProvider = EmptyMetadataProvider,
        ioDispatcher = Dispatchers.IO,
    )

    public constructor(
        context: Context,
        apiKey: String,
    ) : this(
        configuration = FeediConfiguration(apiKey = apiKey),
        transport = UrlConnectionHttpTransport(),
        metadataProvider = AndroidMetadataProvider(context.applicationContext),
        ioDispatcher = Dispatchers.IO,
    )

    public constructor(
        context: Context,
        apiKey: String,
        apiBaseUrl: String,
    ) : this(
        configuration = FeediConfiguration(apiKey = apiKey, apiBaseUrl = apiBaseUrl),
        transport = UrlConnectionHttpTransport(),
        metadataProvider = AndroidMetadataProvider(context.applicationContext),
        ioDispatcher = Dispatchers.IO,
    )

    public constructor() : this(
        configuration = null,
        transport = UrlConnectionHttpTransport(),
        metadataProvider = EmptyMetadataProvider,
        ioDispatcher = Dispatchers.IO,
    )

    public fun configure(apiKey: String): Unit {
        configure(apiKey = apiKey, apiBaseUrl = FeediConfiguration.defaultApiBaseUrl)
    }

    public fun configure(apiKey: String, apiBaseUrl: String): Unit {
        state.set(
            ClientState(
                configuration = FeediConfiguration(apiKey = apiKey, apiBaseUrl = apiBaseUrl),
                metadataProvider = EmptyMetadataProvider,
            ),
        )
    }

    public fun configure(context: Context, apiKey: String): Unit {
        configure(context = context, apiKey = apiKey, apiBaseUrl = FeediConfiguration.defaultApiBaseUrl)
    }

    public fun configure(context: Context, apiKey: String, apiBaseUrl: String): Unit {
        state.set(
            ClientState(
                configuration = FeediConfiguration(apiKey = apiKey, apiBaseUrl = apiBaseUrl),
                metadataProvider = AndroidMetadataProvider(context.applicationContext),
            ),
        )
    }

    @Throws(FeediError::class)
    override suspend fun submitFeedback(
        message: String,
        userEmail: String?,
        customMetadata: Map<String, String>,
    ): FeediReceipt {
        val currentState = state.get() ?: throw FeediError.NotConfigured()
        val payload = FeedbackPayload.create(
            message = message,
            userEmail = userEmail,
            customMetadata = customMetadata,
        )
        val response = try {
            withContext(ioDispatcher) {
                transport.execute(
                    HttpRequest(
                        url = currentState.configuration.feedbackUrl,
                        method = "POST",
                        headers = mapOf(
                            "Authorization" to "Bearer ${currentState.configuration.apiKey}",
                            "Content-Type" to "application/json; charset=utf-8",
                            "Accept" to "application/json",
                        ),
                        body = payload.toJson(
                            currentState.metadataProvider.metadata(),
                            currentState.metadataProvider.appIdentifier(),
                        ),
                    ),
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: FeediError) {
            throw error
        } catch (error: Exception) {
            throw FeediError.TransportError(cause = error)
        }

        return decodeReceipt(response)
    }

    @Throws(FeediError::class)
    public suspend fun submitFeedback(message: String): FeediReceipt {
        return submitFeedback(message = message, userEmail = null, customMetadata = emptyMap())
    }

    @Throws(FeediError::class)
    public suspend fun submitFeedback(message: String, userEmail: String?): FeediReceipt {
        return submitFeedback(message = message, userEmail = userEmail, customMetadata = emptyMap())
    }

    @Throws(FeediError::class)
    public suspend fun submitFeedback(message: String, customMetadata: Map<String, String>): FeediReceipt {
        return submitFeedback(message = message, userEmail = null, customMetadata = customMetadata)
    }

    private fun decodeReceipt(response: HttpResponse): FeediReceipt {
        return when (response.statusCode) {
            in 200..299 -> ReceiptJson.decode(response.body)
            403 -> throw FeediError.AppIdentifierMismatch()
            429 -> throw FeediError.RateLimited()
            503 -> throw FeediError.ServerUnavailable()
            else -> throw FeediError.RequestFailed(statusCode = response.statusCode)
        }
    }

    private data class ClientState(
        val configuration: FeediConfiguration,
        val metadataProvider: MetadataProvider,
    )
}
