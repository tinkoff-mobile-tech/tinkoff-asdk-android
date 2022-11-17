package ru.tinkoff.acquiring.sdk.utils

import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import ru.tinkoff.acquiring.sdk.R


/**
 * Created by Ivan Golovachev
 */
class AcqSnackBarHelper(private val view: View) {

    private var snackbar: Snackbar? = null

    fun show(textValue: String, showProgressBar: Boolean = false) {
        snackbar?.takeIf { it.isShown }?.dismiss()
        val bar = Snackbar.make(view, "", Snackbar.LENGTH_INDEFINITE).apply { snackbar = this }
        val customSnackView: View =
            LayoutInflater.from(view.context).inflate(R.layout.acq_snackbar_layout, null)
        val textView = customSnackView.findViewById<TextView>(R.id.acq_snackbar_text)
        val progressBar = customSnackView.findViewById<ProgressBar>(R.id.acq_snackbar_progress_bar)
        val snackbarLayout = bar.view as SnackbarLayout
        textView.text = textValue
        progressBar.isVisible = showProgressBar

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

    fun show(textValue: Int, showProgressBar: Boolean = false) {
        show(view.context.getString(textValue), showProgressBar)
    }

    fun hide() {
        snackbar?.dismiss()
    }
}