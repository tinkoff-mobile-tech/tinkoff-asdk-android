package ru.tinkoff.acquiring.sdk.redesign.cards.list.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.models.enums.CardStatus
import ru.tinkoff.acquiring.sdk.redesign.cards.list.models.CardItemUiModel
import ru.tinkoff.acquiring.sdk.redesign.cards.list.ui.CardsListState
import ru.tinkoff.acquiring.sdk.responses.GetCardListResponse
import ru.tinkoff.acquiring.sdk.utils.ConnectionChecker
import ru.tinkoff.acquiring.sdk.utils.CoroutineManager

class CardsListViewModel(
    private val sdk: AcquiringSdk,
    private val connectionChecker: ConnectionChecker
) : ViewModel() {

    private val manager = CoroutineManager()

    private val cardsListFlow = MutableStateFlow<List<CardItemUiModel>?>(null)

    val stateFlow = MutableStateFlow<CardsListState>(CardsListState.Loading)

    fun loadData(customerKey: String?, recurrentOnly: Boolean) {
        if (connectionChecker.isOnline().not()) {
            stateFlow.tryEmit(CardsListState.NoNetwork)
            return
        }
        stateFlow.tryEmit(CardsListState.Loading)
        manager.launchOnBackground {
            if (customerKey == null) {
                handleWithoutCustomerKey()
                return@launchOnBackground
            }

            sdk.getCardList { this.customerKey = customerKey }.executeFlow().collect { r ->
                r.process(
                    onSuccess = { handleGetCardListResponse(it, recurrentOnly) },
                    onFailure = ::handleGetCardListError
                )
            }
        }

    }

    private fun handleGetCardListResponse(it: GetCardListResponse, recurrentOnly: Boolean) {
        try {
            val uiCards = filterCards(it.cards, recurrentOnly)
            cardsListFlow.tryEmit(uiCards)
            if (uiCards.isEmpty()) {
                stateFlow.tryEmit(CardsListState.Empty)
            } else {
                stateFlow.tryEmit(CardsListState.Content(uiCards))
            }
        } catch (e: Exception) {
            handleGetCardListError(e)
        }
    }

    private fun filterCards(it: Array<Card>, recurrentOnly: Boolean): List<CardItemUiModel> {
        var activeCards = it.filter { card ->
            card.status == CardStatus.ACTIVE
        }

        if (recurrentOnly) {
            activeCards = activeCards.filter { card -> !card.rebillId.isNullOrBlank() }
        }

        return activeCards.map(::CardItemUiModel)
    }

    private fun handleGetCardListError(it: Exception) {
        cardsListFlow.tryEmit(emptyList())
        stateFlow.tryEmit(CardsListState.Error)
    }

    private fun handleWithoutCustomerKey() {
        stateFlow.tryEmit(CardsListState.Error)
    }

    override fun onCleared() {
        manager.cancelAll()
        super.onCleared()
    }
}