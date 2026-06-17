package dev.feedi

import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

internal class FeediConfiguration(
    apiKey: String,
    apiBaseUrl: String = defaultApiBaseUrl,
) {
    val apiKey: String = normalizeApiKey(apiKey)
    val apiBaseUrl: String = normalizeApiBaseUrl(apiBaseUrl)
    val feedbackUrl: String = "${this.apiBaseUrl}/v1/feedback"

    companion object {
        const val defaultApiBaseUrl: String = "https://api.feedi.dev"

        // Public write keys are region-segmented, e.g. `feedi_us_pk_…` / `feedi_eu_pk_…`.
        // The region segment is optional so a legacy `feedi_pk_…` key is still accepted.
        private val publicWriteKeyPattern = Regex("^feedi_([a-z]+_)?pk_")

        private fun normalizeApiKey(apiKey: String): String {
            val normalized = apiKey.trim()
            if (normalized.isBlank()) {
                throw FeediError.InvalidConfiguration("apiKey must not be blank.")
            }
            if (!publicWriteKeyPattern.containsMatchIn(normalized)) {
                throw FeediError.InvalidConfiguration("apiKey must be a Feedi public write key.")
            }
            return normalized
        }

        private fun normalizeApiBaseUrl(apiBaseUrl: String): String {
            val normalized = apiBaseUrl.trim().trimEnd('/')
            if (normalized.isBlank()) {
                throw FeediError.InvalidConfiguration("apiBaseUrl must not be blank.")
            }

            val uri = try {
                URI(normalized)
            } catch (_: URISyntaxException) {
                throw FeediError.InvalidConfiguration("apiBaseUrl must be a valid URL.")
            }

            val scheme = uri.scheme?.lowercase(Locale.US)
            val host = uri.host?.lowercase(Locale.US)
            if (scheme == null || host == null || uri.userInfo != null || uri.query != null || uri.fragment != null) {
                throw FeediError.InvalidConfiguration("apiBaseUrl must be a valid base URL.")
            }
            if (scheme != "https" && !(scheme == "http" && host.isLocalHost())) {
                throw FeediError.InvalidConfiguration("apiBaseUrl must use HTTPS.")
            }

            return normalized
        }

        private fun String.isLocalHost(): Boolean {
            return this == "localhost" || this == "127.0.0.1" || this == "::1"
        }
    }
}
