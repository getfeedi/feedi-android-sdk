package dev.feedi

import java.time.Instant
import java.time.format.DateTimeParseException
import org.json.JSONException
import org.json.JSONObject

internal object ReceiptJson {
    fun decode(body: String): FeediReceipt {
        val json = try {
            JSONObject(body)
        } catch (_: JSONException) {
            throw FeediError.DecodingError()
        }

        val id = json.optString("id").trim()
        val receivedAt = json.optString("receivedAt").trim()
        if (id.isEmpty() || receivedAt.isEmpty()) {
            throw FeediError.DecodingError()
        }

        val instant = try {
            Instant.parse(receivedAt)
        } catch (_: DateTimeParseException) {
            throw FeediError.DecodingError()
        }

        return FeediReceipt(id = id, receivedAt = instant)
    }
}
