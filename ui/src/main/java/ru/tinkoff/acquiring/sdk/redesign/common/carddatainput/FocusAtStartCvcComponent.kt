package ru.tinkoff.acquiring.sdk.redesign.common.carddatainput

import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.acq_fragment_cvc_input.view.cvc_input
import ru.tinkoff.acquiring.sdk.ui.component.UiComponent

interface CvcUIComponent : UiComponent<String?> {
    fun clearCvc()
    fun enableCvc(isEnabled: Boolean)
}

class FocusAtStartCvcComponent(
    private val root: ViewGroup,
    private val initingFocusAndKeyboard: Boolean,
    private val onFocusCvc: View.() -> Unit,
    onInputComplete: (String) -> Unit,
    onDataChange: (Boolean, String) -> Unit
) : CvcUIComponent {

    private val cvcComponent: CvcComponent = CvcComponent(
        root,
        initingFocusAndKeyboard,
        onInputComplete,
        onDataChange,
        onInitScreen = { _, function ->
            if(initingFocusAndKeyboard){
                onFocusCvc(root.cvc_input.editText.apply(function))
            }
        }
    )

    override fun clearCvc() {
        cvcComponent.render(null)
    }

    override fun enableCvc(isEnabled: Boolean) = cvcComponent.enable(isEnabled)

    fun enable(isEnable: Boolean) = with(cvcComponent.cvcInput) {
        isEnabled = isEnable
        editable = isEnable
        if (isEnable.not()) {
            hideKeyboard()
        }
    }

    override fun render(state: String?) = cvcComponent.render(state)

}