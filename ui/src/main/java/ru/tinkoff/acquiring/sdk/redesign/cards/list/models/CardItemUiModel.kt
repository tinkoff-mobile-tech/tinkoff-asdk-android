package ru.tinkoff.acquiring.sdk.redesign.cards.list.models

import ru.tinkoff.acquiring.sdk.models.Card

class CardItemUiModel(
    val card: Card,

    // TODO after brandByBin algo impl
    val bankName: String = "***",

    // TODO after delete card task
    val showDelete: Boolean = false
) {
    val id = card.cardId

    val tale = card.pan?.takeLast(4)
}