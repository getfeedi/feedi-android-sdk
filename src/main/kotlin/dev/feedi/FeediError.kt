package dev.feedi

public sealed class FeediError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    public class NotConfigured : FeediError("Feedi.configure(apiKey) must be called before submitting feedback.")
    public class InvalidConfiguration(message: String) : FeediError(message)
    public class InvalidMessage : FeediError("Feedback message must be between 1 and 5000 characters.")
    public class InvalidEmail : FeediError("User email is invalid.")
    public class InvalidCustomMetadata : FeediError("Custom metadata is invalid.")
    public class TransportError(cause: Throwable? = null) : FeediError("Feedback request failed.", cause)
    public class DecodingError : FeediError("Feedback receipt response could not be decoded.")
    public class AppIdentifierMismatch : FeediError("The app package name does not match the app this write key belongs to.")
    public class RateLimited : FeediError("Feedback submission was rate limited.")
    public class ServerUnavailable : FeediError("Feedi is temporarily unavailable.")
    public class RequestFailed(public val statusCode: Int) : FeediError("Feedback request failed with status $statusCode.")
}
