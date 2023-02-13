package ru.tinkoff.acquiring.sdk.redesign.mainform.ui

import android.content.ContextWrapper
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentFormUi
import ru.tinkoff.acquiring.sdk.ui.component.UiComponent
import ru.tinkoff.acquiring.sdk.utils.lazyUnsafe

/**
 * Created by i.golovachev
 */
internal class PrimaryButtonComponent(
    private val buttonContainer: LinearLayout
) : UiComponent<MainPaymentFormUi.Primary> {

    private val textView: TextView by lazyUnsafe {
        buttonContainer.findViewById(R.id.acq_primary_button_text)
    }

    private val imageView: ImageView by lazyUnsafe {
        buttonContainer.findViewById(R.id.acq_primary_button_image)
    }

    override fun render(state: MainPaymentFormUi.Primary) {
        when (state) {
            is MainPaymentFormUi.Primary.Card -> setState(
                bgColor = R.drawable.acq_button_yellow_bg,
                textColor = R.color.acq_colorTitle,
                buttonText = R.string.common_signin_button_text,
                icon = null
            )
            is MainPaymentFormUi.Primary.Spb -> setState(
                bgColor = R.drawable.acq_button_spb_bg,
                textColor = R.color.acq_colorMain,
                buttonText = R.string.common_signin_button_text,
                icon = R.drawable.acq_ic_sbp_primary_button_logo
            )
            is MainPaymentFormUi.Primary.Tpay -> setState(
                bgColor = R.drawable.acq_button_yellow_bg,
                textColor = R.color.acq_colorTitle,
                buttonText = R.string.common_signin_button_text,
                icon = R.drawable.acq_icon_tinkoff_pay
            )
            else -> setState(
                bgColor = R.drawable.acq_button_yellow_bg,
                textColor = R.color.acq_colorTitle,
                buttonText = R.string.common_signin_button_text,
                icon = null
            )
        }
    }

    private fun setState(bgColor: Int, textColor: Int, buttonText: Int, icon: Int?) {
        textView.setTextColor(ContextCompat.getColor(buttonContainer.context, textColor))
        textView.setText(buttonText)

        buttonContainer.setBackgroundResource(bgColor)

        imageView.isVisible = icon != null
        icon?.let(imageView::setImageResource)
    }
}