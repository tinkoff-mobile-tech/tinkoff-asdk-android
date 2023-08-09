package ru.tinkoff.acquiring.sample.utils

import android.app.Activity
import android.widget.Toast
import androidx.annotation.StringRes

/**
 * @author k.shpakovskiy
 */
fun Activity.toast(message: String) = runOnUiThread {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Activity.toast(@StringRes message: Int) = runOnUiThread {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
