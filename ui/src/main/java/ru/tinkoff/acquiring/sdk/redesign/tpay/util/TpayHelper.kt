package ru.tinkoff.acquiring.sdk.redesign.tpay.util

import androidx.appcompat.app.AppCompatActivity
import ru.tinkoff.acquiring.sdk.redesign.common.util.openDeepLink

internal object TpayHelper {

    const val TPAY_REQUEST_CODE = 104

    fun openTpayDeeplink(
        deeplink: String,
        activity: AppCompatActivity
    ) {
        activity.openDeepLink(TPAY_REQUEST_CODE, deeplink)
    }
}
