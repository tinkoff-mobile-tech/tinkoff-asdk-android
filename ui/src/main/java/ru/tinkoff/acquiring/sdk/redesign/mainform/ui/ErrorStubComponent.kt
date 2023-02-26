package ru.tinkoff.acquiring.sdk.redesign.mainform.ui

import androidx.core.view.isVisible
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.databinding.AcqMainFromErrorStubBinding
import ru.tinkoff.acquiring.sdk.ui.component.UiComponent

/**
 * Created by i.golovachev
 */
class ErrorStubComponent (
    private val viewBinding: AcqMainFromErrorStubBinding,
    private val onRetry: () -> Unit
) : UiComponent<ErrorStubComponent.State> {

    override fun render(state: State) = with(viewBinding) {
        acqStubTitle.setText(state.title)
        acqStubRetryDescription.setText(state.subtitle)
        acqStubRetryButton.setText(state.button)
        acqStubRetryButton.setOnClickListener {
            onRetry()
        }
    }

    fun isVisible(isVisible: Boolean) {
        viewBinding.root.isVisible = isVisible
    }

    val root = viewBinding.root

    sealed class State(val title: Int, val subtitle: Int, val button: Int) {

        object NoNetwork : State(
            title = R.string.acq_generic_stubnet_title,
            subtitle = R.string.acq_generic_stubnet_description,
            button = R.string.acq_generic_button_stubnet
        )

        object Error : State(
            title = R.string.acq_generic_alert_label,
            subtitle = R.string.acq_generic_stub_description,
            button = R.string.acq_generic_alert_access
        )
    }
}