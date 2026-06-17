# Android SDK Architecture

Feedi Android is a small headless SDK. It intentionally avoids UI, analytics, device identifiers, screenshot capture, storage, and retry queues.

## Public Surface

- `Feedi`: shared facade for apps that want a single global configuration point.
- `FeediClient`: instance-based client for apps that prefer dependency injection.
- `FeediFeedbackSubmitting`: narrow interface for app-level unit tests.
- `FeediReceipt`: returned receipt with Feedi's feedback id and `java.time.Instant` timestamp.
- `FeediError`: typed errors for validation, configuration, transport, decoding, and API status failures.

## Data Flow

1. App calls `Feedi.configure(context, apiKey)` once. `Feedi.configure(apiKey)` remains available for context-free setup and tests.
2. App calls suspending `submitFeedback(message, userEmail, customMetadata)` from its own UI or domain code.
3. The SDK trims and validates the message, optional email, and custom metadata locally before transport.
4. The SDK adds a narrow built-in metadata object: app version, build number, Android version, and locale. It does not collect device identifiers.
5. Blocking `HttpURLConnection` work runs inside the configured I/O dispatcher.
6. The SDK sends `POST /v1/feedback` with `Authorization: Bearer <public key>`.
7. The SDK decodes the receipt response and returns it to the app.

## Dependency Policy

Runtime code uses Kotlin, `kotlinx-coroutines-core`, and Android/Java platform APIs. The current transport is `HttpURLConnection` to avoid OkHttp or another HTTP runtime dependency. Tests use Kotlin's standard test dependency, coroutine test utilities, JUnit4, and `org.json` only to provide a real JSON implementation for local JVM unit tests.

## Threading

The public submission API is coroutine-first. Callers do not need to choose a dispatcher for the network request; `FeediClient` validates on the caller context, snapshots configuration atomically, then switches to its internal I/O dispatcher for blocking transport.

Shared facade configuration is stored in an atomic client reference. Each `FeediClient` stores its configured state in an `AtomicReference`, so concurrent configure/submit calls see either the previous complete configuration or the next complete configuration.

## API Contract

The request body is:

```json
{
  "message": "The settings screen is confusing.",
  "email": "user@example.com",
  "appIdentifier": "com.example.app",
  "metadata": {
    "appVersion": "1.0",
    "buildNumber": "42",
    "androidVersion": "15",
    "locale": "en-US"
  },
  "customMetadata": {
    "screen": "settings"
  }
}
```

`email` and `customMetadata` are optional. `metadata` is always sent and contains only non-identifying app/runtime fields. `appIdentifier` is the app's package name, captured automatically and sent as a dedicated top-level field so the backend can verify it matches the app the write key belongs to; a mismatch is rejected with HTTP 403.

Configuration is intentionally strict. Public write keys must be Feedi public write keys (region-segmented, e.g. `feedi_us_pk_...` / `feedi_eu_pk_...`), API base URLs must be valid absolute URLs, and non-local endpoints must use HTTPS so feedback payloads and write keys are not sent over cleartext transport.

## Non-Goals

- Built-in feedback UI.
- Offline persistence or retries.
- Device metadata collection.
- Screenshot or attachment capture.
- Analytics-style event collection.
