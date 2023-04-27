package ru.tinkoff.acquiring.sdk.redesign.common.util

import android.content.pm.PackageManager
import ru.tinkoff.acquiring.sdk.utils.isPackageInstalled

/**
 * Created by i.golovachev
 */
interface InstalledAppChecker {
    fun isInstall(packageName: String) : Boolean
}

internal class InstalledAppCheckerSdkImpl(
    private val packageManager: PackageManager
) : InstalledAppChecker {
    override fun isInstall(packageName: String): Boolean {
        return packageManager.isPackageInstalled(packageName)
    }
}