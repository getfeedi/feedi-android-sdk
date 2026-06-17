package dev.feedi

import android.content.Context
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

public object Feedi : FeediFeedbackSubmitting {
    private val sharedClient = AtomicReference(FeediClient())

    @JvmStatic
    public fun configure(apiKey: String): Unit {
        sharedClient.get().configure(apiKey = apiKey)
    }

    @JvmStatic
    public fun configure(apiKey: String, apiBaseUrl: String): Unit {
        sharedClient.get().configure(apiKey = apiKey, apiBaseUrl = apiBaseUrl)
    }

    @JvmStatic
    public fun configure(context: Context, apiKey: String): Unit {
        sharedClient.get().configure(context = context, apiKey = apiKey)
    }

    @JvmStatic
    public fun configure(context: Context, apiKey: String, apiBaseUrl: String): Unit {
        sharedClient.get().configure(context = context, apiKey = apiKey, apiBaseUrl = apiBaseUrl)
    }

    @Throws(FeediError::class)
    override suspend fun submitFeedback(
        message: String,
        userEmail: String?,
        customMetadata: Map<String, String>,
    ): FeediReceipt {
        return sharedClient.get().submitFeedback(
            message = message,
            userEmail = userEmail,
            customMetadata = customMetadata,
        )
    }

    @JvmStatic
    @Throws(FeediError::class)
    public suspend fun submitFeedback(message: String): FeediReceipt {
        return submitFeedback(message = message, userEmail = null, customMetadata = emptyMap())
    }

    @JvmStatic
    @Throws(FeediError::class)
    public suspend fun submitFeedback(message: String, userEmail: String?): FeediReceipt {
        return submitFeedback(message = message, userEmail = userEmail, customMetadata = emptyMap())
    }

    @JvmStatic
    @Throws(FeediError::class)
    public suspend fun submitFeedback(message: String, customMetadata: Map<String, String>): FeediReceipt {
        return submitFeedback(message = message, userEmail = null, customMetadata = customMetadata)
    }

    internal fun resetForTesting(
        transport: HttpTransport = UrlConnectionHttpTransport(),
        metadataProvider: MetadataProvider = EmptyMetadataProvider,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) {
        sharedClient.set(
            FeediClient(
                configuration = null,
                transport = transport,
                metadataProvider = metadataProvider,
                ioDispatcher = ioDispatcher,
            ),
        )
    }
}
