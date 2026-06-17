package dev.feedi

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.util.Locale

internal interface MetadataProvider {
    fun metadata(): Map<String, String>

    /**
     * The app's package name, captured automatically so the backend can verify
     * it matches the app the write key belongs to. Sent as a dedicated
     * top-level submission field rather than part of [metadata].
     */
    fun appIdentifier(): String
}

internal class AndroidMetadataProvider(context: Context) : MetadataProvider {
    private val applicationContext = context.applicationContext

    override fun metadata(): Map<String, String> {
        val packageInfo = applicationContext.safePackageInfo()

        return mapOf(
            "appVersion" to packageInfo?.versionName.safeMetadataValue(),
            "buildNumber" to packageInfo.versionCodeString(),
            "androidVersion" to Build.VERSION.RELEASE.safeMetadataValue(),
            "locale" to Locale.getDefault().toLanguageTag().safeMetadataValue(fallback = "und"),
        )
    }

    override fun appIdentifier(): String = applicationContext.packageName
}

internal object EmptyMetadataProvider : MetadataProvider {
    override fun metadata(): Map<String, String> {
        return mapOf(
            "appVersion" to "unknown",
            "buildNumber" to "unknown",
            "androidVersion" to "unknown",
            "locale" to "und",
        )
    }

    override fun appIdentifier(): String = ""
}

private fun Context.safePackageInfo(): PackageInfo? {
    return try {
        if (Build.VERSION.SDK_INT >= 33) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
}

private fun PackageInfo?.versionCodeString(): String {
    if (this == null) {
        return "unknown"
    }

    val code = if (Build.VERSION.SDK_INT >= 28) {
        longVersionCode
    } else {
        @Suppress("DEPRECATION")
        versionCode.toLong()
    }

    return code.toString().safeMetadataValue()
}

private fun String?.safeMetadataValue(fallback: String = "unknown"): String {
    val trimmed = this?.trim().orEmpty()
    return when {
        trimmed.isEmpty() -> fallback
        trimmed.length <= 64 -> trimmed
        else -> trimmed.take(64)
    }
}
