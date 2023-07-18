package ru.tinkoff.acquiring.sdk.redesign.sbp.util

import ru.tinkoff.acquiring.sdk.responses.NspkC2bResponse

fun interface NspkBankAppsProvider {
    suspend fun getNspkApps() : List<NspkC2bResponse.NspkAppInfo>
}