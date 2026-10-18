# Contributing to the Feedi Android SDK

Thanks for your interest in improving the Feedi Android SDK. This is the public,
headless Android client for [Feedi](https://feedi.dev). Contributions that keep
the SDK small, dependency-light, and easy to inspect are very welcome.

## Ground rules

The SDK has a deliberately narrow scope. Please keep these constraints in mind:

- **Dependency-light.** The only approved runtime dependencies are the Kotlin
  standard library and `kotlinx-coroutines-core`. HTTP transport stays on
  `HttpURLConnection` — no OkHttp-class dependencies. Do not add new runtime
  dependencies without prior discussion.
- **Headless and privacy-first.** No SDK UI, screenshot capture, analytics,
  device identifiers, offline persistence, console logging, or hidden background
  work. The manifest declares only `INTERNET`.
- **Stable public API.** The module uses `explicitApi()`; treat public types as
  release commitments. Public API changes need a clear rationale and a changelog
  entry.

## Development setup

- Android Studio (or a standalone Android SDK) with `minSdk` 26 / `compileSdk` 36.
- Use JDK 21 for the build and documentation generation; the SDK targets
  Java 8-compatible bytecode.

Run the unit test suite (using the Android Studio JBR and a configured Android SDK
avoids toolchain drift):

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
ANDROID_HOME="$HOME/Library/Android/sdk" \
./gradlew testDebugUnitTest
```

Assemble the release AAR to catch packaging / `explicitApi()` regressions:

```bash
./gradlew assembleRelease
```

CI runs unit tests, release assembly, lint, and documentation-jar generation on
every push and pull request, using JDK 21 and the Android 36 SDK packages.

## Making changes

1. Fork and branch from `main`.
2. Keep changes scoped and focused — one responsibility per file, small classes.
3. Add or update tests for behavior changes (JUnit 4 + `kotlin.test` +
   `kotlinx-coroutines-test`). Regression tests are expected for bug fixes.
4. Update `README.md`, `docs/android-sdk-architecture.md`, `docs/publishing.md`,
   and `CHANGELOG.md` when behavior, install instructions, or public API change.
5. With JDK 21, make sure `./gradlew testDebugUnitTest assembleRelease lint javaDocReleaseJar` passes locally.

## Commit and PR conventions

- Use [Conventional Commits](https://www.conventionalcommits.org/):
  `feat`, `fix`, `refactor`, `build`, `ci`, `chore`, `docs`, `style`, `perf`,
  `test`. Keep subjects short and specific, e.g. `fix: reject blank api keys`.
- Prefer normal-sized, reviewable commits over squashing everything into one — it
  keeps history meaningful.
- Open a pull request against `main` and fill out the PR template. Link any
  related issue.

## Reporting issues

Use the bug-report or feature-request issue templates. For anything
security-sensitive, follow [SECURITY.md](SECURITY.md) instead of opening a public
issue.
