package ru.tinkoff.acquiring.sample.ui.environment


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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        evnGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.acq_env_is_pre_prod_btn -> {
                    setEnvironment(
                        EnvironmentMode.IsPreProdMode,
                        PRE_PROD_URL,
                        isDeveloperMode = false,
                        isEditable = false
                    )
                }
                R.id.acq_env_is_debug_btn -> {
                    setEnvironment(
                        EnvironmentMode.IsDebugMode,
                        AcquiringApi.getUrl("/"),
                        isDeveloperMode = true,
                        isEditable = false
                    )
                }
                R.id.acq_env_is_custom_btn -> {
                    setEnvironment(
                        EnvironmentMode.IsCustomMode,
                        "https://",
                        isDeveloperMode = false,
                        isEditable = true
                    )
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

    override fun onResume() {
        dialog?.window?.setLayout(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        super.onResume()
    }

    private fun setEnvironment(
        mode: EnvironmentMode,
        url: String,
        isDeveloperMode: Boolean,
        isEditable: Boolean
    ) {
        AcquiringSdk.environmentMode = mode
        AcquiringSdk.customUrl = if (isEditable) null else url
        AcquiringSdk.isDeveloperMode = isDeveloperMode

        editUrlText.setText(url)
        editUrlText.isEnabled = isEditable

        if (isEditable) {
            editUrlText.requestFocus()
        }
    }

    private fun setupEnv() {

        when(AcquiringSdk.environmentMode) {
            is EnvironmentMode.IsPreProdMode -> {
                AcquiringSdk.isDeveloperMode = false
                evnGroup.check(R.id.acq_env_is_pre_prod_btn)
            }
            is EnvironmentMode.IsDebugMode -> {
                AcquiringSdk.isDeveloperMode = true
                evnGroup.check(R.id.acq_env_is_debug_btn)
            }
            is EnvironmentMode.IsCustomMode -> {
                AcquiringSdk.isDeveloperMode = false
                evnGroup.check(R.id.acq_env_is_custom_btn)
            }
        }
    }

    companion object {
        private val PRE_PROD_URL = "https://qa-mapi.tcsbank.ru"

        const val TAG = "AcqEnvironmentDialog"
    }
}
