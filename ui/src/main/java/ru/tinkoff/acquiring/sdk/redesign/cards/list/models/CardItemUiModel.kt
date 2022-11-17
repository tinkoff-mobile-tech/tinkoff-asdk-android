package ru.tinkoff.acquiring.sdk.redesign.cards.list.models

import android.content.Context
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.utils.BankIssuer

data class CardItemUiModel(
    private val card: Card,

    // TODO after delete card task
    val showDelete: Boolean = false,

    val isBlocked: Boolean = false
) {
    val id = card.cardId

    val pan: String? = card.pan

    val tail = card.pan?.takeLast(4)

    fun bankName(context: Context) = BankIssuer.resolve(pan).getCaption(context)
}