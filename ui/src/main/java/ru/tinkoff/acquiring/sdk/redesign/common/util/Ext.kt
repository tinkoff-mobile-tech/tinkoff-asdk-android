package ru.tinkoff.acquiring.sdk.redesign.common.util

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import java.io.Serializable
import kotlin.reflect.KClass

/**
 * @author k.shpakovskiy
 */
fun AppCompatActivity.openDeepLink(
    requestCode: Int,
    deeplink: String
) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = Uri.parse(deeplink)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivityForResult(intent, requestCode)
}
