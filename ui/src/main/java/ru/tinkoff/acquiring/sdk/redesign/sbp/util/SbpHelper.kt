package ru.tinkoff.acquiring.sdk.redesign.sbp.util

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity

object SbpHelper {

    const val SBP_BANK_REQUEST_CODE = 112

    fun openSbpDeeplink(
        deeplink: String,
        packageName: String,
        activity: AppCompatActivity
    ) {
        // stub
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(deeplink)
        intent.setPackage(packageName)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivityForResult(intent, SBP_BANK_REQUEST_CODE)
    }

    @SuppressLint("QueryPermissionsNeeded")
    internal fun getBankApps(packageManager: PackageManager, deeplink: String, banks: Set<Any?>): List<String> {
        // get sbp packages
        val sbpIntent = Intent(Intent.ACTION_VIEW)
        sbpIntent.setDataAndNormalize(Uri.parse(deeplink))
        val sbpPackages = packageManager.queryIntentActivities(sbpIntent, 0)
            .map { it.activityInfo.packageName }

        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://"))
        val browserPackages = packageManager.queryIntentActivities(browserIntent, 0)
            .map { it.activityInfo.packageName }
        // filter out browsers
        val nonBrowserSbpPackages = sbpPackages.filter { it !in browserPackages }

        // get bank packages
        val bankPackages = packageManager.getInstalledApplications(0)
            .map { it.packageName }.filter { it in banks }

        // merge two lists
        return mutableListOf<String>().apply {
            addAll(nonBrowserSbpPackages)
            addAll(bankPackages)
        }.distinct()
    }
}
