package ru.tinkoff.acquiring.sdk.redesign.common.carddatainput

import android.content.Context
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.smartfield.AcqEditText
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
    val onDataChange: (Boolean, String) -> Unit = { _, _ -> }
) : UiComponent<String?> {

    private val cvcInput: AcqTextFieldView = root.findViewById(R.id.cvc_input)
    val cvc get() = cvcInput.text.orEmpty()

    init {
        root.setOnClickListener {
            requestViewFocus()
        }
        with(cvcInput) {
            transformationMethod = PasswordTransformationMethod()
            MaskFormatWatcher(createCvcMask()).installOn(editText)

            editText.afterTextChanged {
                errorHighlighted = false

                val cvc = cvc
                if (cvc.length > CVC_MASK.length) {
                    if (validate(cvc)) {
                       cvcInput.clearViewFocus()
                        onInputComplete(cvc)
                    } else {
                        errorHighlighted = true
                    }
                }

                onDataChange(validate(cvc), cvc)
            }

            editText.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
                if (hasFocus.not()) {
                    val isValid = validate(cvc)
                    errorHighlighted = isValid.not()
                    onDataChange(isValid, cvc)
                }
            }
        }
    }

    private fun validate(code: String): Boolean {
        return CardValidator.validateSecurityCode(code)
    }

    override fun render(state: String?) {
        cvcInput.text = state
    }

    fun enable(isEnable: Boolean) {
        cvcInput.isEnabled = isEnable
        cvcInput.editable = isEnable
        if (isEnable.not()) {
            cvcInput.hideKeyboard()
        }
    }

    fun requestViewFocus() {
        cvcInput.requestViewFocus()
    }

    fun isVisible(isVisible: Boolean) {
        root.isVisible = isVisible
    }

    companion object {
        const val CVC_MASK = "___"
        fun createCvcMask(): MaskImpl = MaskImpl
            .createTerminated(UnderscoreDigitSlotsParser().parseSlots(CVC_MASK))
    }
}