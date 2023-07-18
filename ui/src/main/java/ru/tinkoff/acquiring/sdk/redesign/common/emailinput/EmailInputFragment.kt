package ru.tinkoff.acquiring.sdk.redesign.common.emailinput

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.smartfield.AcqTextFieldView
import ru.tinkoff.acquiring.sdk.smartfield.BaubleClearButton
import ru.tinkoff.acquiring.sdk.utils.SimpleTextWatcher
import ru.tinkoff.acquiring.sdk.utils.getParent
import ru.tinkoff.acquiring.sdk.utils.lazyView
import java.util.regex.Pattern

/**
 * Created by i.golovachev
 */
internal class EmailInputFragment : Fragment() {

    val emailInput: AcqTextFieldView by lazyView(R.id.email_input)
    val emailValue get() = emailInput.text.orEmpty()

    private val textWatcher = SimpleTextWatcher.after {
        onDataChanged()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.acq_fragment_email_input, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(emailInput) {
            BaubleClearButton().attach(this)
            arguments?.getString(EMAIL_ARG)?.let { editText.setText(it) }
            editText.addTextChangedListener(textWatcher)
        }
    }

    fun withArguments(email: String?): EmailInputFragment = apply {
        arguments = bundleOf(EMAIL_ARG to email)
    }

    fun isValid(): Boolean = EmailValidator.validate(emailValue)

    fun clearFocus() {
        emailInput.clearViewFocus()
    }

    fun enableEmail(isEnabled: Boolean) {
        emailInput.editText.isEnabled = isEnabled
        if (isEnabled) {
            emailInput.editText.clearFocus()
            emailInput.editText.hideKeyboard()
        }
    }

    private fun onDataChanged() {
        getParent<OnEmailDataChanged>()?.onEmailDataChanged(isValid())
    }

    fun interface OnEmailDataChanged {
        fun onEmailDataChanged(isValid: Boolean)
    }

    companion object {
        private const val EMAIL_ARG = "EMAIL_ARG"
        fun getInstance(email: String?) = EmailInputFragment().withArguments(email)
    }
}

object EmailValidator {
    private const val EMAIL_REGEX = ".+\\@.+\\..+"
    private val pattern = Pattern.compile(EMAIL_REGEX)

    fun validate(text: String?): Boolean {
        if (text == null) return false

        return text.isNotBlank() && pattern.matcher(text).matches()
    }
}