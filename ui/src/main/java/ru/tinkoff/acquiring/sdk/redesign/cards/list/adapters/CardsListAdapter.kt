package ru.tinkoff.acquiring.sdk.redesign.cards.list.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.redesign.cards.list.models.CardItemUiModel

class CardsListAdapter : RecyclerView.Adapter<CardsListAdapter.CardViewHolder>() {

    private val cards = mutableListOf<CardItemUiModel>()

    @SuppressLint("NotifyDataSetChanged")
    fun setCards(cards: List<CardItemUiModel>) {
        this.cards.clear()
        this.cards.addAll(cards)
        notifyDataSetChanged()
    }

    fun onRemoveCard(id: String) {
        //TODO после задачи на удаление карты
    }

    fun onAddCard(card: CardItemUiModel) {
        //TODO после задачи на добавление карты
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.acq_card_list_item, parent, false) as View
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(cards[position])
    }

    override fun getItemCount(): Int {
        return cards.size
    }

    class CardViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val cardNameView = itemView.findViewById<TextView>(R.id.cardNameMasked)

        fun bind(card: CardItemUiModel) {
            cardNameView.text = itemView.context.getString(
                R.string.card_list_item_card_name_masked_template,
                card.bankName,
                card.tale
            )
        }
    }
}