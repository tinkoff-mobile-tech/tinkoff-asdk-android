package ru.tinkoff.acquiring.sample.ui

import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import ru.tinkoff.acquiring.sample.R
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.network.AcquiringApi


class AcqEnvironmentDialog : DialogFragment() {

    private val description: TextView by lazy {
        requireView().findViewById(R.id.acq_env_description)
    }
    private val ok: TextView by lazy {
        requireView().findViewById(R.id.acq_env_ok)
    }
    private val isPreProdSwitcher: Switch by lazy {
        requireView().findViewById(R.id.acq_env_is_pre_prod)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_asdk_env, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isPreProdSwitcher.setOnCheckedChangeListener { _, isPreprod ->
            setEnv(isPreprod)
        }

        setEnv(AcquiringSdk.isPreprodMode)
        isPreProdSwitcher.isChecked = AcquiringSdk.isPreprodMode

        ok.setOnClickListener { dismiss() }
    }

    override fun onResume() {
        dialog?.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        super.onResume()
    }

    private fun getDescriptionText(): String {
        return "Для запросов на Back-end Acquiring будет использован URL: ${AcquiringApi.getUrl("")} "
    }

    private fun setEnv(isPreprod: Boolean) {
        AcquiringSdk.isPreprodMode = isPreprod
        description.text = getDescriptionText()
    }

    companion object {
        const val TAG = "AcqEnvironmentDialog"
    }
}
