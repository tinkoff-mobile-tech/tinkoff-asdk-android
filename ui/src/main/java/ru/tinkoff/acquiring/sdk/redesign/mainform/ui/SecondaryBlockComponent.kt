package ru.tinkoff.acquiring.sdk.redesign.mainform.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import ru.tinkoff.acquiring.sdk.databinding.AcqMainFormSecondaryBlockBinding
import ru.tinkoff.acquiring.sdk.databinding.AcqMainFormSecondaryButtonBinding
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentForm
import ru.tinkoff.acquiring.sdk.responses.Paymethod
import ru.tinkoff.acquiring.sdk.ui.component.UiComponent

/**
 * Created by i.golovachev
 */
internal class SecondaryBlockComponent(
    val binding: AcqMainFormSecondaryBlockBinding,
    private val onTpayClick: () -> Unit,
    private val onSpbClick: () -> Unit,
    private val onNewCardClick: () -> Unit,
    private val onMirPayClick: () -> Unit
) : UiComponent<Set<MainPaymentForm.Secondary>> {

    private val adapter = Adapter().apply {
        binding.secondaryList.adapter = this
    }

    override fun render(state: Set<MainPaymentForm.Secondary>) {
        val items = state.map { it.mapButtonState(binding.root.context) }
        binding.root.isGone = items.isEmpty()
        adapter.update(items)
    }

    private fun onClick(paymethod: Paymethod) = when (paymethod) {
        Paymethod.MirPay -> onMirPayClick()
        Paymethod.TinkoffPay -> onTpayClick()
        Paymethod.YandexPay -> Unit
        Paymethod.SBP -> onSpbClick()
        Paymethod.Cards -> onNewCardClick()
        Paymethod.Unknown -> Unit
    }

    inner class Adapter : RecyclerView.Adapter<VH>() {

        private val list: MutableList<SecondaryButtonComponent.State> = mutableListOf()

        @SuppressLint("NotifyDataSetChanged")
        fun update(list: List<SecondaryButtonComponent.State>) {
            this.list.clear()
            this.list.addAll(list)
            notifyDataSetChanged()
        }

        fun updateCardsItems(state: SecondaryButtonComponent.State) {
            val indexCards = list.indexOfFirst { it.paymethod == state.paymethod }
            list.set(indexCards, state)
            notifyItemChanged(indexCards)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(
                SecondaryButtonComponent(
                    AcqMainFormSecondaryButtonBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    )
                )
            )
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val state = list[position]
            holder.component.render(state)
            holder.component.binding.root.setOnClickListener {
                onClick(state.paymethod)
            }
        }

        override fun onBindViewHolder(
            holder: VH,
            position: Int,
            payloads: MutableList<Any>
        ) {
            if (payloads.contains(CARDS_PAYLOAD)) {
                holder.component.subtitle(list.get(position).subtitle)
            } else {
                super.onBindViewHolder(holder, position, payloads)
            }
        }

        override fun getItemCount(): Int = list.size
    }

    inner class VH(val component: SecondaryButtonComponent) :
        RecyclerView.ViewHolder(component.binding.root)

    companion object {
        private const val CARDS_PAYLOAD = "CARDS_PAYLOAD"
    }
}

