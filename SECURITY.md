# Security Policy

## Supported versions

The Feedi Android SDK follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
Security fixes are delivered against the latest released `1.x` line. We recommend
always running the most recent published version.

| Version | Supported |
| ------- | --------- |
| 1.x     | Yes       |

## Reporting a vulnerability

Please do not open public GitHub issues for security vulnerabilities.

Report suspected vulnerabilities privately to **support@feedi.dev**. Where
possible, include:

- a description of the issue and its impact,
- the SDK version and Android API level affected,
- and reproduction steps or a proof of concept.

You can also use GitHub's
[private vulnerability reporting](https://github.com/getfeedi/feedi-android-sdk/security/advisories/new)
if it is enabled for this repository.

We aim to acknowledge a report within 3 business days and to keep you updated as
we investigate and ship a fix. Please give us a reasonable window to release a
patch before any public disclosure.

## Scope notes

This SDK is intentionally dependency-light and headless. It collects no
screenshots, device identifiers, analytics, or hidden tracking. Public write keys
are designed to be embedded in app binaries: they are hashed server-side, and
submissions are validated against the app's package name. A leaked public write
key cannot read feedback or account data — it can only submit feedback for the
app it is registered to.
