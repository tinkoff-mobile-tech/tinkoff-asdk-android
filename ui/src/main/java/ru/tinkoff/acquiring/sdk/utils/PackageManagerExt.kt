package ru.tinkoff.acquiring.sdk.utils

import android.content.pm.PackageManager


/**
 * Created by i.golovachev
 */
fun PackageManager.isPackageInstalled(packageName: String): Boolean {
    return try {
        getPackageInfo(packageName, 0) != null
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}