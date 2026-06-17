package dev.feedi

import org.json.JSONObject

internal data class FeedbackPayload(
    val message: String,
    val userEmail: String?,
    val customMetadata: Map<String, String>,
) {
    fun toJson(metadata: Map<String, String>, appIdentifier: String): String {
        val body = JSONObject()
        body.put("message", message)
        if (userEmail != null) {
            body.put("email", userEmail)
        }
        body.put("appIdentifier", appIdentifier)
        body.put("metadata", jsonObject(metadata))
        body.put("customMetadata", jsonObject(customMetadata))
        return body.toString()
    }

    companion object {
        private const val maxMessageLength = 5_000
        private const val maxEmailLength = 320
        private const val maxMetadataKeyCount = 10
        private const val maxMetadataKeyLength = 64
        private const val maxMetadataValueLength = 500
        private val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

        fun create(
            message: String,
            userEmail: String?,
            customMetadata: Map<String, String>,
        ): FeedbackPayload {
            val sanitizedMessage = message.trim()
            if (sanitizedMessage.isEmpty() || sanitizedMessage.length > maxMessageLength) {
                throw FeediError.InvalidMessage()
            }

            return FeedbackPayload(
                message = sanitizedMessage,
                userEmail = sanitizeEmail(userEmail),
                customMetadata = sanitizeMetadata(customMetadata),
            )
        }

        private fun sanitizeEmail(email: String?): String? {
            val sanitizedEmail = email?.trim().orEmpty()
            if (sanitizedEmail.isEmpty()) {
                return null
            }
            if (sanitizedEmail.length > maxEmailLength || !emailPattern.matches(sanitizedEmail)) {
                throw FeediError.InvalidEmail()
            }
            return sanitizedEmail
        }

        private fun sanitizeMetadata(metadata: Map<String, String>): Map<String, String> {
            if (metadata.size > maxMetadataKeyCount) {
                throw FeediError.InvalidCustomMetadata()
            }

            val sanitized = linkedMapOf<String, String>()
            metadata.forEach { (key, value) ->
                val sanitizedKey = key.trim()
                val sanitizedValue = value.trim()
                if (
                    sanitizedKey.isEmpty() ||
                    sanitizedKey.length > maxMetadataKeyLength ||
                    sanitizedValue.length > maxMetadataValueLength ||
                    sanitized.containsKey(sanitizedKey)
                ) {
                    throw FeediError.InvalidCustomMetadata()
                }
                sanitized[sanitizedKey] = sanitizedValue
            }
            return sanitized
        }
    }
}

private fun jsonObject(values: Map<String, String>): JSONObject {
    val body = JSONObject()
    values.forEach { (key, value) ->
        body.put(key, value)
    }
    return body
}
