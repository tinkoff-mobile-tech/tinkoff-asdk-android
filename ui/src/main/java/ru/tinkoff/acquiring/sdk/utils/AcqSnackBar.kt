package ru.tinkoff.acquiring.sdk.utils

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import ru.tinkoff.acquiring.sdk.R


/**
 * Created by Ivan Golovachev
 */
// TODO  до конца не ясно что он должен из себя предсатвлять

class AcqSnackBar {

    fun show(view: View, textValue: String) {
        val snackbar = Snackbar.make(view, "", Snackbar.LENGTH_LONG)

        // inflate the custom_snackbar_view created previously

        // inflate the custom_snackbar_view created previously

        val customSnackView: View = LayoutInflater.from(view.context).inflate(R.layout.acq_snackbar_layout, null)
        val textView = customSnackView.findViewById<TextView>(R.id.acq_snackbar_text)
        textView.text = textValue

        // set the background of the default snackbar as transparent

        // now change the layout of the snackbar

        // now change the layout of the snackbar
        val snackbarLayout = snackbar.view as SnackbarLayout

        // set padding of the all corners as 0

        // set padding of the all corners as 0
        snackbarLayout.setPadding(0, 0, 0, 0)
        snackbar.show()
    }
}