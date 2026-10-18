package dev.feedi

import android.content.Context
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

public object Feedi : FeediFeedbackSubmitting {
    private val sharedClient = AtomicReference(FeediClient())

    @JvmStatic
    public fun configure(apiKey: String): Unit {
        sharedClient.get().configure(apiKey = apiKey, includeBuiltInMetadata = true)
    }

    @JvmStatic
    public fun configure(apiKey: String, includeBuiltInMetadata: Boolean): Unit {
        sharedClient.get().configure(apiKey = apiKey, includeBuiltInMetadata = includeBuiltInMetadata)
    }

    @JvmStatic
    public fun configure(apiKey: String, apiBaseUrl: String): Unit {
        sharedClient.get().configure(apiKey = apiKey, apiBaseUrl = apiBaseUrl, includeBuiltInMetadata = true)
    }

    @JvmStatic
    public fun configure(apiKey: String, apiBaseUrl: String, includeBuiltInMetadata: Boolean): Unit {
        sharedClient.get().configure(apiKey = apiKey, apiBaseUrl = apiBaseUrl, includeBuiltInMetadata = includeBuiltInMetadata)
    }

    @JvmStatic
    public fun configure(context: Context, apiKey: String): Unit {
        sharedClient.get().configure(context = context, apiKey = apiKey, includeBuiltInMetadata = true)
    }

    /** Configure submission; when disabled, app version, build number, Android version, and locale are never read. */
    @JvmStatic
    public fun configure(context: Context, apiKey: String, includeBuiltInMetadata: Boolean): Unit {
        sharedClient.get().configure(context = context, apiKey = apiKey, includeBuiltInMetadata = includeBuiltInMetadata)
    }

    @JvmStatic
    public fun configure(context: Context, apiKey: String, apiBaseUrl: String): Unit {
        sharedClient.get().configure(context = context, apiKey = apiKey, apiBaseUrl = apiBaseUrl, includeBuiltInMetadata = true)
    }

    @JvmStatic
    public fun configure(context: Context, apiKey: String, apiBaseUrl: String, includeBuiltInMetadata: Boolean): Unit {
        sharedClient.get().configure(context = context, apiKey = apiKey, apiBaseUrl = apiBaseUrl, includeBuiltInMetadata = includeBuiltInMetadata)
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
