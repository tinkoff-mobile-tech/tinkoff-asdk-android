package ru.tinkoff.acquiring.sdk.redesign.tpay.util

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import ru.tinkoff.acquiring.sdk.redesign.sbp.util.SbpHelper

internal object TpayHelper {

    const val TPAY_REQUEST_CODE = 104

    fun openTpayDeeplink(
        deeplink: String,
        activity: AppCompatActivity
    ) {
        // stub
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(deeplink)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivityForResult(intent, TPAY_REQUEST_CODE)
    }
}