package dev.feedi

public interface FeediFeedbackSubmitting {
    @Throws(FeediError::class)
    public suspend fun submitFeedback(
        message: String,
        userEmail: String? = null,
        customMetadata: Map<String, String> = emptyMap(),
    ): FeediReceipt
}
