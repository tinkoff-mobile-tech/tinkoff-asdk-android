package ru.tinkoff.acquiring.sdk.utils

import android.content.Context
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager

/**
 * Created by i.golovachev
 */
object KeyboardVisionUtils {

    fun showKeyboard(view: View) = with(view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            this.windowInsetsController?.show(WindowInsets.Type.ime())
        } else {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(this, 0)
        }
    }

    fun hideKeyboard(view: View) = with(view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            this.windowInsetsController?.hide(WindowInsets.Type.ime())
        } else {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(windowToken, 0)
        }
    }
}