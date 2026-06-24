# Changelog

All notable changes to the Feedi Android SDK are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.1] - 2026-06-24

### Documentation

- README: documented that the app's package name must exactly match the Package
  name registered for the app in the Feedi dashboard, and that a mismatch makes
  the server reject submissions with HTTP 403 `app_identifier_mismatch`.
- README: added an "Always configure with a context" warning — the context-free
  `Feedi.configure(apiKey = ...)` overload cannot read the package name, so it
  sends an empty `appIdentifier` and every submission is rejected; always use
  `Feedi.configure(context = applicationContext, apiKey = ...)`.
- README: added a "Verify Your Setup" section with a copy-paste `curl` to confirm
  the write key and package name before integrating.
- README: added a "No Lock-In" note (feedback export plus signed webhooks to your
  own backend) and a CI status badge; standardized public write-key examples on
  the region-scoped `feedi_eu_pk_` form.

### Added

- `SECURITY.md` with a private vulnerability disclosure process.
- `CONTRIBUTING.md` with development, testing, and release guidance.
- GitHub issue templates (bug report, feature request) and a pull request
  template.

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

[1.0.1]: https://github.com/getfeedi/feedi-android-sdk/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/getfeedi/feedi-android-sdk/releases/tag/v1.0.0
