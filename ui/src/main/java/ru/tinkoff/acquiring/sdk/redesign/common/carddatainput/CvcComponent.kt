package ru.tinkoff.acquiring.sdk.redesign.common.carddatainput

import android.text.method.PasswordTransformationMethod
import android.view.ViewGroup
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.smartfield.AcqTextFieldView
import ru.tinkoff.acquiring.sdk.smartfield.BaubleClearButton
import ru.tinkoff.acquiring.sdk.ui.component.UiComponent
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.validators.CardValidator
import ru.tinkoff.acquiring.sdk.utils.SimpleTextWatcher.Companion.afterTextChanged
import ru.tinkoff.decoro.MaskImpl
import ru.tinkoff.decoro.parser.UnderscoreDigitSlotsParser
import ru.tinkoff.decoro.watchers.MaskFormatWatcher

/**
 * Created by i.golovachev
 */
class CvcComponent(
    val root: ViewGroup,
    val onInputComplete: (String) -> Unit = {},
    val onDataChange: (Boolean) -> Unit = {}
) : UiComponent<String?> {

    private val cvcInput: AcqTextFieldView = root.findViewById(R.id.card_number_input)
    val cvc get() = cvcInput.text.orEmpty()

    init {
        with(cvcInput) {
            transformationMethod = PasswordTransformationMethod()
            MaskFormatWatcher(createCvcMask()).installOn(editText)

            editText.afterTextChanged {
                errorHighlighted = false

                val cvc = cvc
                if (cvc.length >= CVC_MASK.length) {
                    if (CardValidator.validateSecurityCode(cvc)) {
                        cvcInput.clearViewFocus()
                        if (validate()) {
                            onInputComplete(cvc)
                        }
                    } else {
                        errorHighlighted = true
                    }
                }

                onDataChange(validate())
            }
        }
    }

    fun validate(): Boolean {
        var result = true
        if (CardValidator.validateSecurityCode(cvc)) {
            cvcInput.errorHighlighted = true
            result = false
        }
        return result
    }

    override fun render(state: String?) {
        cvcInput.text = state
    }

    fun requestViewFocus() {
        cvcInput.requestApplyInsets()
    }

    companion object {
        const val CVC_MASK = "___"
        fun createCvcMask(): MaskImpl = MaskImpl
            .createTerminated(UnderscoreDigitSlotsParser().parseSlots(CVC_MASK))
    }
}