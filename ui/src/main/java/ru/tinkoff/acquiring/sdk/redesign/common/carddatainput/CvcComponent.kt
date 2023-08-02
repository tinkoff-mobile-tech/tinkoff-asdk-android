package ru.tinkoff.acquiring.sdk.redesign.common.carddatainput

import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.smartfield.AcqTextFieldView
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
    private val root: ViewGroup,
    private val shouldFocusOnlyCvc: Boolean,
    private val onInputComplete: (String) -> Unit = {},
    private val onDataChange: (Boolean, String) -> Unit = { _, _ -> },
    private val onInitScreen: (Boolean, View.() -> Unit) -> Unit = { _, function -> }
) : UiComponent<String?> {

    internal val cvcInput: AcqTextFieldView = root.findViewById(R.id.cvc_input)
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

            editText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                if (hasFocus.not()) {
                    val isValid = validate(cvc)
                    errorHighlighted = isValid.not()
                    onDataChange(isValid, cvc)
                }
            }
        }

        onInitScreen(shouldFocusOnlyCvc){
            post { cvcInput.editText.requestFocus() }
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

    private fun requestViewFocus() {
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