# Changelog

All notable changes to the Feedi Android SDK are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-06-17

Initial public release. The version is aligned with the Feedi iOS SDK.

### Added

- Headless feedback submission through the `Feedi` shared facade
  (`Feedi.configure(...)` / `Feedi.submitFeedback(...)`).
- `FeediClient` instance-based API for dependency injection, and the
  `FeediFeedbackSubmitting` interface for app-level unit tests.
- Built-in non-identifying metadata (app version, build number, Android version,
  locale) plus the app's package name sent as `appIdentifier`.
- Local validation of message, optional email, and custom metadata before any
  network request.
- Typed `FeediError` hierarchy covering configuration, validation, transport,
  decoding, and API status failures, including `AppIdentifierMismatch` for
  HTTP 403.
- Strict configuration: region-segmented public write keys
  (`feedi_us_pk_` / `feedi_eu_pk_`) and HTTPS-only API base URLs (plain HTTP
  allowed only for local development hosts).

[1.0.0]: https://github.com/getfeedi/feedi-android-sdk/releases/tag/v1.0.0
