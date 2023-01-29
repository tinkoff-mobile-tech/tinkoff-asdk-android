package ru.tinkoff.acquiring.sdk.redesign.cards.list.models

import ru.tinkoff.acquiring.sdk.models.Card

data class CardItemUiModel(
    val card: Card,

    val showDelete: Boolean = false,

    val showChoose: Boolean = false,

    val isBlocked: Boolean = false,

    val bankName: String?
) {
    val id = card.cardId

    val pan: String? = card.pan

    val tail = card.pan?.takeLast(4)
}