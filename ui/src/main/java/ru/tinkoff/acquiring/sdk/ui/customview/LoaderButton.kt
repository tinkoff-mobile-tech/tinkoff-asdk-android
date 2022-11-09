package ru.tinkoff.acquiring.sdk.ui.customview

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.withStyledAttributes
import androidx.core.view.isGone
import androidx.core.view.isVisible
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.utils.dpToPx

internal class LoaderButton
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = R.style.AcqLoaderButton
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    var text: CharSequence
        get() = textView.text
        set(value) {
            textView.text = value
        }

    var isLoading = false
        set(value) {
            field = value
            textView.isGone = field
            loader.isVisible = field
        }

    val textView = TextView(context).apply {
        textSize = 16f
        setTextColor(ColorStateList.valueOf(ResourcesCompat.getColor(
            context.resources, R.color.acq_colorButtonText, context.theme)))
    }

    val loader = ProgressBar(context).apply {
        isIndeterminate = true
        indeterminateTintList = ColorStateList.valueOf(ResourcesCompat.getColor(
            context.resources, R.color.acq_colorButtonText, context.theme))
        isGone = true
    }

    init {
        addView(textView, LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
        })
        addView(loader, LayoutParams(context.dpToPx(24), context.dpToPx(24)).apply {
            gravity = Gravity.CENTER
        })

        context.withStyledAttributes(attrs, R.styleable.LoaderButton, defStyleAttr, defStyleRes) {
            text = getString(R.styleable.LoaderButton_acq_text).orEmpty()
        }
        setBackgroundResource(R.drawable.acq_button_yellow_bg)
    }
}