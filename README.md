# Feedi Android SDK

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
    apiKey = "feedi_us_pk_...",
)
```

`context` is used for built-in app metadata (app version, build number, Android version, and locale) and to read the app's package name. The package name is sent automatically with every submission so the backend can verify it matches the app the write key belongs to; a mismatch is rejected with HTTP 403. A context-free `Feedi.configure(apiKey = "...")` overload is also available for tests and custom dependency injection; it sends fallback metadata and an empty app identifier, so the backend cannot match it to a registered app. Use the `context` overload for real submissions.

Feedi rejects blank keys, keys that are not Feedi public write keys, malformed API base URLs, and non-HTTPS API base URLs. Plain HTTP is accepted only for explicit local development hosts such as `localhost` or `127.0.0.1`.

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
