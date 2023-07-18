package ru.tinkoff.acquiring.sdk.redesign.sbp.util

import ru.tinkoff.acquiring.sdk.responses.NspkC2bResponse

/**
 * Created by i.golovachev
 */
fun interface NspkInstalledAppsChecker {

    fun checkInstalledApps(nspkBanks: List<NspkC2bResponse.NspkAppInfo>, deeplink: String): Map<String,String>
}

