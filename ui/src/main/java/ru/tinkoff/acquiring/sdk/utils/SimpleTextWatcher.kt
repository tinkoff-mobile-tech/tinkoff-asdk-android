package ru.tinkoff.acquiring.sdk.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

internal class SimpleTextWatcher
private constructor(
    private val beforeTextChanged: ((CharSequence?) -> Unit)? = null,
    private val onTextChanged: ((CharSequence?) -> Unit)? = null,
    private val afterTextChanged: ((Editable?) -> Unit)? = null
) : TextWatcher {

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        beforeTextChanged?.invoke(s)
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        onTextChanged?.invoke(s)
    }

    override fun afterTextChanged(s: Editable?) {
        afterTextChanged?.invoke(s)
    }

    companion object {

        fun before(beforeTextChanged: (CharSequence?) -> Unit) =
                SimpleTextWatcher(beforeTextChanged = beforeTextChanged)

        fun onChanged(onTextChanged: (CharSequence?) -> Unit) =
                SimpleTextWatcher(onTextChanged = onTextChanged)

        fun after(afterTextChanged: (Editable?) -> Unit) =
                SimpleTextWatcher(afterTextChanged = afterTextChanged)

        internal fun EditText.beforeTextChanged(beforeTextChanged: (CharSequence?) -> Unit) =
                addTextChangedListener(before(beforeTextChanged))

        internal fun EditText.onTextChanged(onTextChanged: (CharSequence?) -> Unit) =
                addTextChangedListener(onChanged(onTextChanged))

        internal fun EditText.afterTextChanged(afterTextChanged: (Editable?) -> Unit) =
                addTextChangedListener(after(afterTextChanged))
    }
}