package ru.tinkoff.acquiring.sdk.redesign.mainform.presentation

import ru.tinkoff.acquiring.sdk.redesign.sbp.util.NspkBankAppsProvider
import ru.tinkoff.acquiring.sdk.redesign.sbp.util.NspkInstalledAppsChecker
import ru.tinkoff.acquiring.sdk.responses.Paymethod
import ru.tinkoff.acquiring.sdk.responses.PaymethodData

internal object MainPaymentFromUtils {

    private const val TINKOFF_MB_PACKAGE_ID = "com.idamob.tinkoff.android"
    private const val NSPK_DEEPLINK = "https://qr.nspk.ru/83C25B892E5343E5BF30BA835C9CD2FE"
    private const val TPAY_DEEPLINK = "https://www.tinkoff.ru/tpay/1923863684"

    //keys
    const val EMAIL_KEY = "EMAIL_KEY"
    const val NEED_EMAIL_KEY = "NEED_EMAIL_KEY"
    const val CVC_KEY = "CVC_KEY"
    const val CHOSEN_CARD = "CHOSEN_CARD_KEY"


    // region error handler politic
    suspend fun <T : Any> getOrNull(block: suspend () -> T?): T? = try {
        block()
    } catch (e: Throwable) {
        null
    }

    suspend fun getOrFalse(block: suspend () -> Boolean): Boolean = try {
        block()
    } catch (e: Throwable) {
        false
    }
    // endregion


    suspend fun checkNspk(checker: NspkInstalledAppsChecker, provider: NspkBankAppsProvider) = getOrFalse {
        val nspkApps = provider.getNspkApps()
        checker.checkInstalledApps(nspkApps, NSPK_DEEPLINK).isNotEmpty()
    }

    suspend fun checkTinkoff(checker: NspkInstalledAppsChecker) = getOrFalse {
        checker.checkInstalledApps(setOf(TINKOFF_MB_PACKAGE_ID), TPAY_DEEPLINK).isNotEmpty()
    }
}

internal fun List<PaymethodData>.has(paymethod: Paymethod) = any { it.paymethod == paymethod }