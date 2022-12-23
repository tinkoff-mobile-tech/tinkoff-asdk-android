package ru.tinkoff.acquiring.sdk.redesign.sbp.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.ui.customview.LoaderButton
import ru.tinkoff.acquiring.sdk.utils.lazyView

class SbpNoBanksStubActivity: AppCompatActivity() {

    private val buttonOk: LoaderButton by lazyView(R.id.acq_button_ok)
    private val buttonDetails: LoaderButton by lazyView(R.id.acq_button_details)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acq_activity_stub_sbp_no_banks)

        buttonOk.setOnClickListener { finish() }

        buttonDetails.setOnClickListener {
            // todo
        }
    }

    companion object {

        fun show(activity: Activity) {
            activity.startActivity(Intent(activity, SbpNoBanksStubActivity::class.java))
        }
    }
}