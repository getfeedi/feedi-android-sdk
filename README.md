# Feedi Android SDK

[![CI](https://github.com/getfeedi/feedi-android-sdk/actions/workflows/ci.yml/badge.svg)](https://github.com/getfeedi/feedi-android-sdk/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-first-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Platform](https://img.shields.io/badge/Android-API%2026%2B-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com)
[![Maven Central](https://img.shields.io/badge/Maven%20Central-dev.feedi-blue.svg)](https://central.sonatype.com/artifact/dev.feedi/feedi-android-sdk)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Feedi is a lightweight, headless feedback SDK for Android apps. It lets an app submit user-written feedback to Feedi while keeping the app's own UI, state, and presentation logic fully under app control.

The SDK has no built-in UI, no screenshot capture, no analytics collection, no device identifiers, no offline queue, and no persistent logging. Runtime code uses Kotlin plus Java/Android platform APIs.

## Requirements

- Kotlin Android app
- Android API 26 or newer
- Java 8-compatible runtime bytecode
- Android `INTERNET` permission, declared by the SDK manifest and merged into the consuming app

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## Install

Feedi is distributed through Maven Central. Add the dependency to your app
module (`mavenCentral()` is already in the default repositories of new Android
projects; add it if yours doesn't have it):

```kotlin
dependencies {
    implementation("dev.feedi:feedi-android-sdk:1.0.0")
}
```

## Configure

Use the public app write key returned by the Feedi dashboard. Public write keys are region-segmented (for example `feedi_us_pk_...` or `feedi_eu_pk_...`) and are safe to embed in an app binary.

```kotlin
import dev.feedi.Feedi

Feedi.configure(
    context = applicationContext,
    apiKey = "feedi_eu_pk_...",
)
```

`context` is used for built-in app metadata (app version, build number, Android version, and locale) and to read the app's package name. The package name is sent automatically with every submission so the backend can verify it matches the app the write key belongs to; a mismatch is rejected with HTTP 403. Use the `feedi_eu_pk_...` or `feedi_us_pk_...` key for the region you chose when registering the app.

Feedi rejects blank keys, keys that are not Feedi public write keys, malformed API base URLs, and non-HTTPS API base URLs. A blank or non-public key throws `FeediError.InvalidConfiguration` from `configure`. Plain HTTP is accepted only for explicit local development hosts such as `localhost` or `127.0.0.1`.

### Always configure with a context

Always pass `context`. There is a context-free `Feedi.configure(apiKey = "...")` overload, but it uses an empty metadata provider, so it sends an **empty** `appIdentifier`. The backend cannot match an empty identifier to a registered app, so **every submission fails with HTTP 403**. The context-free overload exists only for unit tests and custom dependency injection — never use it for real submissions.

```kotlin
// Correct — package name is derived from context and sent on every submission.
Feedi.configure(context = applicationContext, apiKey = "feedi_eu_pk_...")

// Wrong — empty app identifier, every submitFeedback(...) returns 403.
Feedi.configure(apiKey = "feedi_eu_pk_...")
```

### Package name must match

The SDK automatically reads `context.packageName` and sends it as `appIdentifier` on every submission. **The Feedi backend requires this to match — exactly and case-sensitively — the Package name you registered for the app in the Feedi dashboard.** A mismatch is rejected with HTTP 403 `app_identifier_mismatch`, surfaced by the SDK as `FeediError.AppIdentifierMismatch`.

So the Package name you enter in the dashboard when creating the app must equal the app's real `applicationId` from your app-level `build.gradle`, for example:

```kotlin
android {
    defaultConfig {
        applicationId = "com.example.myapp" // must equal the dashboard Package name
    }
}
```

This is set in the dashboard, not in code — the SDK never lets you override the identifier it derives from the running app.

## Submit Feedback

`submitFeedback` is a suspending function and is safe to call from the main thread. Feedi moves blocking network I/O onto its internal I/O dispatcher.

```kotlin
viewModelScope.launch {
    val receipt = Feedi.submitFeedback(
        message = "I could not finish onboarding.",
        userEmail = "founder@example.com",
        customMetadata = mapOf(
            "screen" to "onboarding",
        ),
    )
}
```

`userEmail` and `customMetadata` are optional. Messages and metadata are validated locally before any network request is sent.

Feedi does not queue, retry, or persist feedback locally. If you want background retry behavior, call the SDK from your own WorkManager job or app-owned queue.

## Verify Your Setup

Before (or while) wiring the SDK in, confirm that your public write key and app identifier line up by submitting a test feedback directly with `curl`. Replace the key with your real public write key and set `appIdentifier` to the exact Package name you registered in the dashboard (the same value as your app's `applicationId`):

```bash
curl -i -X POST https://api.feedi.dev/v1/feedback \
  -H "Authorization: Bearer feedi_eu_pk_REPLACE_WITH_PUBLIC_WRITE_KEY" \
  -H "Content-Type: application/json" \
  -d '{"message":"Test from setup","appIdentifier":"com.example.myapp","metadata":{}}'
```

Read the HTTP status line:

- **201 Created** — success. The test feedback appears in your dashboard inbox.
- **403** (`app_identifier_mismatch`) — the `appIdentifier` does not match the app this write key belongs to. Fix the Package name in the dashboard or the `appIdentifier`/`applicationId` so they match exactly.
- **401** — the key is wrong, inactive, or not a public write key.

## Testable App Code

App code that needs unit tests can depend on the narrow interface instead of the global facade:

```kotlin
import dev.feedi.Feedi
import dev.feedi.FeediFeedbackSubmitting
import dev.feedi.FeediReceipt

class SettingsFeedbackController(
    private val feedback: FeediFeedbackSubmitting = Feedi,
) {
    suspend fun submit(message: String, userEmail: String?): FeediReceipt {
        return feedback.submitFeedback(
            message = message,
            userEmail = userEmail,
            customMetadata = mapOf("screen" to "settings"),
        )
    }
}
```

## API Behavior

- Sends `POST /v1/feedback` to `https://api.feedi.dev` by default.
- Sends `Authorization: Bearer <public key>`.
- Encodes `message`, optional `email`, the app's `appIdentifier` (package name), built-in `metadata`, and `customMetadata` as JSON.
- Returns `FeediReceipt(id, receivedAt)` for 2xx responses. `receivedAt` is a `java.time.Instant`.
- Throws `FeediError.RateLimited` for HTTP 429 and `FeediError.ServerUnavailable` for HTTP 503. A `403` response throws `FeediError.AppIdentifierMismatch`, meaning the package name did not match the registered app.

## No Lock-In

Feedback you collect through Feedi is yours. Submissions are stored in the region
you choose (EU or US), are exportable, and can be fanned out to your own backend
via signed webhooks. Nothing in this SDK ties your app to Feedi at the binary
level: it is a thin `POST /v1/feedback` client you can read end to end, so you can
swap the endpoint or replace the SDK at any time. See the docs at
[feedi.dev](https://feedi.dev) for export and webhook details.

## Development

Run the SDK test suite:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
ANDROID_HOME="$HOME/Library/Android/sdk" \
./gradlew testDebugUnitTest
```

To try unreleased changes from a sample app, publish to your local Maven
repository and add `mavenLocal()` to the app's repositories:

```bash
./gradlew publishToMavenLocal
```

Release steps for Maven Central are documented in [docs/publishing.md](docs/publishing.md).

Architecture notes live in [docs/android-sdk-architecture.md](docs/android-sdk-architecture.md).
