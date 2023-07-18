package ru.tinkoff.acquiring.sdk.redesign.common.emailinput

import android.animation.LayoutTransition
import android.view.ViewGroup
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.smartfield.AcqTextFieldView
import ru.tinkoff.acquiring.sdk.smartfield.BaubleClearButton
import ru.tinkoff.acquiring.sdk.ui.component.UiComponent
import ru.tinkoff.acquiring.sdk.utils.SimpleTextWatcher
import ru.tinkoff.acquiring.sdk.utils.lazyUnsafe

/**
 * Created by i.golovachev
 */
class EmailInputComponent(
    val root: ViewGroup,
    private val onEmailChange: (String) -> Unit,
    private val onEmailVisibleChange: (Boolean) -> Unit
) : UiComponent<EmailInputComponent.State> {

    val emailValue get() = emailInput.text.orEmpty()

    private val sendReceiptSwitch: SwitchCompat by lazyUnsafe { root.findViewById(R.id.acq_send_receipt_switch) }
    private val emailInput: AcqTextFieldView by lazyUnsafe { root.findViewById(R.id.email_input) }
    private val textWatcher = SimpleTextWatcher.after {
        onEmailChange(emailValue)
    }

    init {
        val transition = LayoutTransition()
        transition.setAnimateParentHierarchy(false)
        root.layoutTransition = transition

        with(emailInput) {
            BaubleClearButton().attach(this)
            editText.addTextChangedListener(textWatcher)
        }

        sendReceiptSwitch.setOnCheckedChangeListener { _, isChecked ->
            emailInput.isVisible = isChecked
            if (isChecked.not()) {
                if (emailInput.isViewFocused()) {
                    emailInput.hideKeyboard()
                }
                emailInput.clearViewFocus()
            }
            onEmailVisibleChange(isChecked)
        }
    }

    override fun render(state: State) {
        emailInput.text = state.email
        emailInput.isVisible = state.email?.isNotBlank() == true
        sendReceiptSwitch.isChecked = emailInput.isVisible
    }

    fun render(email: String?, isShow: Boolean) {
        render(State(email, isShow))
    }

    fun clear() {
        render(State(null, false))
    }

    fun isEnable(isEnable: Boolean) {
        emailInput.isEnabled = isEnable
        sendReceiptSwitch.isEnabled = isEnable
    }

    fun isValid(): Boolean = EmailValidator.validate(emailValue)

    data class State(val email: String?, val isShow: Boolean)
}
