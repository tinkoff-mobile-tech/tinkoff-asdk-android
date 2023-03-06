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



/**
 * Created by i.golovachev
 */
class AcqEnvironmentDialog : DialogFragment() {

    private val description: TextView by lazy {
        requireView().findViewById(R.id.acq_env_description)
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_asdk_env, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        evnGroup.setOnCheckedChangeListener { _, checkedId ->
            when(checkedId) {
                R.id.acq_env_is_pre_prod_btn ->  {
                    editUrlText.setText(PRE_PROD_URL)
                }
                R.id.acq_env_is_debug_btn ->  {

                }
                R.id.acq_env_is_custom_btn ->  {

                }
            }
        }

        ok.setOnClickListener { dismiss() }
    }

    override fun onResume() {
        dialog?.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        super.onResume()
    }


    companion object {
        private val PRE_PROD_URL = "https://qa-mapi.tcsbank.ru/"

        const val TAG = "AcqEnvironmentDialog"
    }
}