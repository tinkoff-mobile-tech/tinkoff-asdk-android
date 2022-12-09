package ru.tinkoff.acquiring.sdk.redesign.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ViewFlipper
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.ui.customview.LoaderButton
import ru.tinkoff.acquiring.sdk.utils.lazyView

internal class PaymentLCEDialogFragment : BottomSheetDialogFragment() {

    private val viewFlipper: ViewFlipper by lazyView(R.id.acq_fragment_payment_lce)
    private val buttonChooseAnotherMethod: LoaderButton
            by lazyView(R.id.acq_button_choose_another_method)
    private val buttonOk: LoaderButton by lazyView(R.id.acq_button_ok)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, theme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.acq_fragment_payment_lce, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getDialog()?.getWindow()
            ?.getAttributes()?.windowAnimations = R.style.AcqBottomSheetAnim
    }

    fun loading() {
        viewFlipper.displayedChild = 0
    }

    fun failure(onChooseAnother: () -> Unit) {
        viewFlipper.displayedChild = 1
        buttonChooseAnotherMethod.setOnClickListener {
            onChooseAnother()
        }
    }

    fun success(onOkClick: () -> Unit) {
        viewFlipper.displayedChild = 2
        buttonOk.setOnClickListener { onOkClick() }
    }

}