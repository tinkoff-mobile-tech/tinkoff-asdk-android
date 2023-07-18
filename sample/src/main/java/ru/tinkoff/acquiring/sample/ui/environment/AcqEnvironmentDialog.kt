package ru.tinkoff.acquiring.sample.ui.environment


import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import ru.tinkoff.acquiring.sample.R
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.network.AcquiringApi
import ru.tinkoff.acquiring.sdk.utils.EnvironmentMode

/**
 * Created by i.golovachev
 */
class AcqEnvironmentDialog : DialogFragment() {

    companion object {

        private const val PRE_PROD_URL = "https://qa-mapi.tcsbank.ru"
        const val TAG = "AcqEnvironmentDialog"

    }

    private val ok: TextView by lazy {
        requireView().findViewById(R.id.acq_env_ok)
    }
    private val editUrlText: EditText by lazy {
        requireView().findViewById(R.id.acq_env_url)
    }
    private val evnGroup: RadioGroup by lazy {
        requireView().findViewById(R.id.acq_env_urls)
    }
    private var customUrl: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.asdk_environment_dialog, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        evnGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.acq_env_is_pre_prod_btn -> configureCustomUrl(PRE_PROD_URL, EnvironmentMode.IsPreProdMode)
                R.id.acq_env_is_debug_btn -> configureCustomUrl(AcquiringApi.getUrl("/"), EnvironmentMode.IsDebugMode)
                R.id.acq_env_is_custom_btn -> {
                    AcquiringSdk.environmentMode = EnvironmentMode.IsCustomMode
                    editUrlText.setText("https://")
                    editUrlText.setSelection(editUrlText.text.length)
                    editUrlText.isEnabled = true
                    editUrlText.requestFocus()
                }
            }
        }

        setupEnv()

        ok.setOnClickListener {
            customUrl = editUrlText.text.toString()
            AcquiringSdk.customUrl = customUrl
            dismiss()
        }
    }

    private fun configureCustomUrl(url: String, mode: EnvironmentMode) {
        AcquiringSdk.environmentMode = mode
        AcquiringSdk.customUrl = url
        disableEditing(url)
    }

    private fun disableEditing(url: String) = with(editUrlText) {
        setText(url)
        isEnabled = false
    }

    override fun onResume() {
        dialog?.window?.setLayout(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        super.onResume()
    }

    private fun setupEnv() {
        when(AcquiringSdk.environmentMode) {
            is EnvironmentMode.IsPreProdMode -> {
                evnGroup.check(R.id.acq_env_is_pre_prod_btn)
            }
            is EnvironmentMode.IsDebugMode -> {
                evnGroup.check(R.id.acq_env_is_debug_btn)
            }
            is EnvironmentMode.IsCustomMode -> {
                evnGroup.check(R.id.acq_env_is_custom_btn)
            }
        }
    }
}