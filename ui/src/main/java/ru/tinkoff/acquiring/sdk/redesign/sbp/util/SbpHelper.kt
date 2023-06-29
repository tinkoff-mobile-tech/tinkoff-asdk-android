package ru.tinkoff.acquiring.sdk.redesign.sbp.util

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import ru.tinkoff.acquiring.sdk.redesign.common.util.openDeepLink
import ru.tinkoff.acquiring.sdk.responses.NspkC2bResponse

object SbpHelper {

    const val SBP_BANK_REQUEST_CODE = 112

    fun openSbpDeeplink(
        deeplink: String,
        activity: AppCompatActivity,
        errorCallback: (Throwable) -> Unit
    ) {
        runCatching { activity.openDeepLink(SBP_BANK_REQUEST_CODE, deeplink) }
            .onFailure { errorCallback(it) }
    }

    @SuppressLint("QueryPermissionsNeeded")
    internal fun getBankApps(packageManager: PackageManager, deeplink: String, banks: List<NspkC2bResponse.NspkAppInfo>): Map<String,String> {
        val sbpIntent = Intent(Intent.ACTION_VIEW)
        val appAndLinks = buildMap {
            banks.forEach { appInfo ->
                val deepLink = prepareNspkDeeplinkWithScheme(appInfo.schema, deeplink)
                sbpIntent.setDataAndNormalize(deepLink)
                packageManager.queryIntentActivities(sbpIntent, 0).forEach {
                    put(it.activityInfo.packageName, deepLink.toString())
                }
            }
        }
        return appAndLinks
    }

    private fun prepareNspkDeeplinkWithScheme(schema: String, deepLink: String): Uri {
        val raw = Uri.parse(deepLink)
        return Uri.Builder().apply {
            this.scheme(schema)
            this.authority(raw.authority)
            this.path(raw.path)
        }.build()
    }
}
