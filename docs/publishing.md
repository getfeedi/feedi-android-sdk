# Publishing to Maven Central

The Feedi Android SDK publishes to [Maven Central](https://central.sonatype.com)
through the Central Portal using the
[`com.vanniktech.maven.publish`](https://vanniktech.github.io/gradle-maven-publish-plugin/)
plugin. The build is already configured; this document covers the one-time
account setup and the per-release steps.

## One-time setup

1. **Central Portal account** — sign in at https://central.sonatype.com.
2. **Namespace verification** — register the `dev.feedi` namespace and verify
   ownership of the `feedi.dev` domain (DNS TXT record). The artifact coordinates
   are `dev.feedi:feedi-android-sdk`.
3. **Portal token** — generate a user token in the Portal account settings. The
   token's username and password are the publishing credentials.
4. **GPG signing key** — Maven Central requires signed artifacts. Generate a key,
   publish the public half to a public keyserver, and export the private key in
   ASCII-armored form for in-memory signing.

## Credentials

The build reads credentials from Gradle properties / environment variables, so
no secrets are committed. Set these in the environment (or `~/.gradle/gradle.properties`)
before a release:

```bash
export ORG_GRADLE_PROJECT_mavenCentralUsername="<portal-token-username>"
export ORG_GRADLE_PROJECT_mavenCentralPassword="<portal-token-password>"
export ORG_GRADLE_PROJECT_signingInMemoryKey="$(gpg --armor --export-secret-keys <KEY_ID>)"
export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword="<key-password>"   # if the key has one
```

Signing is only enabled when `signingInMemoryKey` is present, so local builds and
`publishToMavenLocal` work without a key. Central rejects unsigned artifacts, so a
release run must export the key.

## Cutting a release

1. Bump `version` in `build.gradle.kts` (drop any `-SNAPSHOT` suffix for a final
   release).
2. Run the publish task:

   ```bash
   JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
   ANDROID_HOME="$HOME/Library/Android/sdk" \
   ./gradlew publishAndReleaseToMavenCentral --no-configuration-cache
   ```

   - `publishAndReleaseToMavenCentral` uploads and automatically releases.
   - Use `publishToMavenCentral` instead to upload a deployment and release it
     manually from the Portal UI after review.
3. Tag the release in git (`vX.Y.Z`) and push the tag.

It can take a few minutes to hours for a newly released version to be searchable
and synced to the Maven Central CDN.
