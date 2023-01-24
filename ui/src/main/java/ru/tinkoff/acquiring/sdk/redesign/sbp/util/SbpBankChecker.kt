package ru.tinkoff.acquiring.sdk.redesign.sbp.util

/**
 * Created by i.golovachev
 */
fun interface SbpBankAppsProvider {

    fun checkInstalledApps(nspkBanks: Set<Any?>, deeplink: String): List<String>
}

