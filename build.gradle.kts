import com.vanniktech.maven.publish.AndroidSingleVariantLibrary

plugins {
    id("com.android.library") version "9.2.0"
    id("com.vanniktech.maven.publish") version "0.36.0"
}

group = "dev.feedi"
version = "1.0.0"

android {
    namespace = "dev.feedi"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.json:json:20250517")
}

mavenPublishing {
    // Publishes the release AAR plus sources and a (Dokka-free) Javadoc jar,
    // both required by Maven Central.
    configure(
        AndroidSingleVariantLibrary(
            variant = "release",
            sourcesJar = true,
            publishJavadocJar = true,
        ),
    )

    // Targets the Central Portal (https://central.sonatype.com). Credentials and
    // the GPG signing key are read from Gradle properties / environment variables
    // (ORG_GRADLE_PROJECT_mavenCentralUsername, ORG_GRADLE_PROJECT_mavenCentralPassword,
    // ORG_GRADLE_PROJECT_signingInMemoryKey, ORG_GRADLE_PROJECT_signingInMemoryKeyPassword),
    // so no secrets live in the repository. See docs/publishing.md.
    publishToMavenCentral()

    // Sign only when a GPG key is configured (Gradle maps the
    // ORG_GRADLE_PROJECT_signingInMemoryKey env var to this property), so local
    // `publishToMavenLocal` works without a key while Central releases are signed.
    // Maven Central rejects unsigned artifacts, so release runs must provide one.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }

    coordinates(group.toString(), "feedi-android-sdk", version.toString())

    pom {
        name.set("Feedi Android SDK")
        description.set("Headless Android feedback SDK for Feedi.")
        inceptionYear.set("2026")
        url.set("https://github.com/getfeedi/feedi-android-sdk")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/license/mit")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("danielmunoz")
                name.set("Daniel Munoz")
                email.set("me@danmunoz.com")
            }
        }
        scm {
            connection.set("scm:git:https://github.com/getfeedi/feedi-android-sdk.git")
            developerConnection.set("scm:git:https://github.com/getfeedi/feedi-android-sdk.git")
            url.set("https://github.com/getfeedi/feedi-android-sdk")
        }
    }
}
