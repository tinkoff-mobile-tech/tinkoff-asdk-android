package ru.tinkoff.acquiring.sdk.utils

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import ru.tinkoff.acquiring.sdk.R


/**
 * Created by Ivan Golovachev
 */
class AcqSnackBarHelper(private val view: View) {

    private var snackbar: Snackbar? = null

    fun showProgress(textValue: String) {
        snackbar?.takeIf { it.isShown }?.dismiss()
        val bar = Snackbar.make(view, "", Snackbar.LENGTH_SHORT).apply { snackbar = this }
        val customSnackView: View =
            LayoutInflater.from(view.context).inflate(R.layout.acq_snackbar_progress_layout, null)
        val textView = customSnackView.findViewById<TextView>(R.id.acq_snackbar_text)
        val progressBar = customSnackView.findViewById<ProgressBar>(R.id.acq_snackbar_progress_bar)
        val snackbarLayout = bar.view as SnackbarLayout
        textView.text = textValue

        snackbarLayout.addView(customSnackView, 0)

        snackbarLayout.setBackgroundColor(
            ContextCompat.getColor(view.context, android.R.color.transparent)
        )
        snackbarLayout.setPadding(
            view.context.dpToPx(16),
            0,
            view.context.dpToPx(16),
            view.context.dpToPx(24)
        )

        bar.show()
    }

    fun showProgress(@StringRes textValue: Int) {
        showProgress(view.context.getString(textValue))
    }

    fun showWithIcon(@DrawableRes iconRes: Int, textValue: String) {
        snackbar?.takeIf { it.isShown }?.dismiss()
        val bar = Snackbar.make(view, "", Snackbar.LENGTH_SHORT).apply { snackbar = this }
        val customSnackView: View =
            LayoutInflater.from(view.context).inflate(R.layout.acq_snackbar_with_icon_layout, null)
        val textView = customSnackView.findViewById<TextView>(R.id.acq_snackbar_text)
        val imageView = customSnackView.findViewById<ImageView>(R.id.acq_snackbar_icon)
        imageView.setImageResource(iconRes)
        val snackbarLayout = bar.view as SnackbarLayout
        textView.text = textValue

        snackbarLayout.addView(customSnackView, 0)

        snackbarLayout.setBackgroundColor(
            ContextCompat.getColor(view.context, android.R.color.transparent)
        )
        snackbarLayout.setPadding(
            view.context.dpToPx(16),
            0,
            view.context.dpToPx(16),
            view.context.dpToPx(24)
        )

        bar.show()
    }

    fun showWithIcon(@DrawableRes iconRes: Int, @StringRes textValue: Int) {
        showWithIcon(iconRes, view.context.getString(textValue))
    }

    fun hide(delay: Long = 0) {
        if (delay == 0L) {
            snackbar?.dismiss()
        } else {
            view.postDelayed({
                snackbar?.dismiss()
            }, delay)
        }
    }
}