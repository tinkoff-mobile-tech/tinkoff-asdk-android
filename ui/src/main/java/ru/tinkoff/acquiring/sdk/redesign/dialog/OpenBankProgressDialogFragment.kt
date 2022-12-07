package ru.tinkoff.acquiring.sdk.redesign.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.ui.customview.LoaderButton
import ru.tinkoff.acquiring.sdk.utils.lazyView

class OpenBankProgressDialogFragment : BottomSheetDialogFragment() {

    private val buttonCancel: LoaderButton by lazyView(R.id.acq_button_cancel)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, theme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.acq_fragment_open_bank_progress, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonCancel.setOnClickListener { onCancel() }
    }

    private fun onCancel() {
        ((parentFragment as? OnCancel) ?: (activity as? OnCancel))?.onOpenBankProgressCancel(this)
    }

    fun interface OnCancel {
        fun onOpenBankProgressCancel(fragment: OpenBankProgressDialogFragment)
    }
}