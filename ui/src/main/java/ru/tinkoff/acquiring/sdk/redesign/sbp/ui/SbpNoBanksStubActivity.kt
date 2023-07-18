package ru.tinkoff.acquiring.sdk.redesign.sbp.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.ui.customview.LoaderButton
import ru.tinkoff.acquiring.sdk.utils.lazyView

class SbpNoBanksStubActivity : AppCompatActivity() {

    private val toolbar: Toolbar by lazyView(R.id.acq_toolbar)
    private val buttonOk: LoaderButton by lazyView(R.id.acq_button_ok)
    private val buttonDetails: LoaderButton by lazyView(R.id.acq_button_details)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acq_activity_stub_sbp_no_banks)
        initToolbar()

        buttonOk.setOnClickListener { finish() }

        buttonDetails.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(URL_NSPK))
            startActivity(browserIntent)
        }
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        finish()
    }

    companion object {
        private const val URL_NSPK = "https://sbp.nspk.ru/participants/"

        fun show(activity: Activity) {
            activity.startActivity(Intent(activity, SbpNoBanksStubActivity::class.java))
        }
    }
}
