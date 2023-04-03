package ru.tinkoff.acquiring.sdk.redesign.cards.list.presentation

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.models.enums.CardStatus
import ru.tinkoff.acquiring.sdk.models.options.screen.SavedCardsOptions
import ru.tinkoff.acquiring.sdk.redesign.cards.list.models.CardItemUiModel
import ru.tinkoff.acquiring.sdk.redesign.cards.list.ui.CardListEvent
import ru.tinkoff.acquiring.sdk.redesign.cards.list.ui.CardListMode
import ru.tinkoff.acquiring.sdk.redesign.cards.list.ui.CardsListState
import ru.tinkoff.acquiring.sdk.responses.GetCardListResponse
import ru.tinkoff.acquiring.sdk.utils.BankCaptionProvider
import ru.tinkoff.acquiring.sdk.utils.ConnectionChecker
import ru.tinkoff.acquiring.sdk.utils.CoroutineManager
import ru.tinkoff.acquiring.sdk.utils.getExtra

/**
 * Created by Ivan Golovachev
 */
internal class CardsListViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val sdk: AcquiringSdk,
    private val connectionChecker: ConnectionChecker,
    private val bankCaptionProvider: BankCaptionProvider,
    private val manager: CoroutineManager = CoroutineManager()
) : ViewModel() {

    private val selectedCardIdFlow =
        MutableStateFlow(savedStateHandle.getExtra<SavedCardsOptions>().features.selectedCardId)

    private var deleteJob: Job? = null

    @VisibleForTesting
    val stateFlow = MutableStateFlow<CardsListState>(CardsListState.Shimmer)

    val stateUiFlow = stateFlow.filter { it.isInternal.not() }

    val modeFlow = stateFlow.map { it.mode }

    val eventFlow = MutableStateFlow<CardListEvent?>(null)

    fun loadData(customerKey: String?, recurrentOnly: Boolean) {
        if (connectionChecker.isOnline().not()) {
            stateFlow.value = CardsListState.NoNetwork
            return
        }
        stateFlow.value = CardsListState.Shimmer
        manager.launchOnBackground {
            if (customerKey == null) {
                stateFlow.value = CardsListState.Error(Throwable())
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

    fun deleteCard(model: CardItemUiModel, customerKey: String?) {
        if (deleteJob?.isActive == true) {
            return
        }

        eventFlow.value = CardListEvent.RemoveCardProgress(model)
        deleteJob = manager.launchOnBackground {
            if (connectionChecker.isOnline().not()) {
                eventFlow.value = CardListEvent.ShowError
                return@launchOnBackground
            }
            if (customerKey == null) {
                eventFlow.value = CardListEvent.ShowError
                return@launchOnBackground
            }
            sdk.removeCard {
                this.cardId = model.id
                this.customerKey = customerKey
            }
                .executeFlow()
                .collect {
                    it.process(
                        onSuccess = {
                            handleDeleteCard(model)
                            deleteJob?.cancel()
                        },
                        onFailure = {
                            val list =
                                checkNotNull((stateFlow.value as? CardsListState.Content)?.cards)
                            stateFlow.update { CardsListState.Content(it.mode, true, list) }
                            eventFlow.value = CardListEvent.ShowCardDeleteError(it)
                            deleteJob?.cancel()
                        }
                    )
                }
        }
    }

    fun changeMode(mode: CardListMode) {
        stateFlow.update { state ->
            val prev = state as CardsListState.Content
            val cards = prev.cards.map {
                it.copy(
                    showDelete = mode == CardListMode.DELETE,
                    isBlocked = it.isBlocked,
                    showChoose = selectedCardIdFlow.value == it.card.cardId && mode === CardListMode.CHOOSE
                )
            }
            CardsListState.Content(mode, false, cards)
        }
    }

    fun chooseCard(model: CardItemUiModel) {
        if (stateFlow.value.mode === CardListMode.CHOOSE) {
            eventFlow.value = CardListEvent.CloseScreen(model.card)
        }
    }

    fun chooseNewCard() {
        if (stateFlow.value.mode === CardListMode.CHOOSE) {
            eventFlow.value = CardListEvent.CloseWithoutCard
        }
    }

    fun onAttachCard(cardId: String) {
        eventFlow.value = CardListEvent.ShowCardAttachDialog(cardId)
    }

    fun onBackPressed() {
        if (eventFlow.value !is CardListEvent.RemoveCardProgress) {
            val _state = stateFlow.value
            eventFlow.value = when(_state) {
                is CardsListState.Error -> CardListEvent.CloseBecauseCardNotLoaded
                is CardsListState.NoNetwork -> CardListEvent.CloseBecauseCardNotLoaded
                else ->  {
                    val state = _state as? CardsListState.Content // пустой список вернет состояние, при котором необходимо выбирать новую карту
                    val card = state?.cards?.firstOrNull { it.id == selectedCardIdFlow.value }
                    CardListEvent.CloseScreen(card?.card)
                }
            }
        }
    }

    private fun handleGetCardListResponse(it: GetCardListResponse, recurrentOnly: Boolean) {
        try {
            val mode = if (selectedCardIdFlow.value != null) {
                CardListMode.CHOOSE
            } else {
                CardListMode.ADD
            }
            val uiCards = filterCards(it.cards, recurrentOnly, mode)
            stateFlow.value = if (uiCards.isEmpty()) {
                CardsListState.Empty
            } else {
                CardsListState.Content(mode, false, uiCards)
            }
        } catch (e: Exception) {
            handleGetCardListError(e)
        }
    }

    private fun filterCards(
        it: Array<Card>,
        recurrentOnly: Boolean,
        mode: CardListMode
    ): List<CardItemUiModel> {
        var activeCards = it.filter { card ->
            card.status == CardStatus.ACTIVE
        }

        if (recurrentOnly) {
            activeCards = activeCards.filter { card -> !card.rebillId.isNullOrBlank() }
        }

        return activeCards.map {
            val cardNumber = checkNotNull(it.pan)
            CardItemUiModel(
                card = it,
                bankName = bankCaptionProvider(cardNumber),
                showChoose = (selectedCardIdFlow.value == it.cardId) && mode === CardListMode.CHOOSE
            )
        }
    }

    private fun handleGetCardListError(it: Exception) {
        stateFlow.value = CardsListState.Error(it)
    }

    private fun handleDeleteCard(deletedCard: CardItemUiModel) {
        val list = checkNotNull((stateFlow.value as? CardsListState.Content)?.cards).toMutableList()
        val indexAt = list.indexOfFirst { it.id == deletedCard.id }
        list.removeAt(indexAt)

        if (list.isEmpty()) {
            stateFlow.value = CardsListState.Empty
            eventFlow.value = CardListEvent.RemoveCardSuccess(deletedCard, null)
        } else {
            if (deletedCard.showChoose || deletedCard.id == selectedCardIdFlow.value) {
                selectedCardIdFlow.value = list.firstOrNull()?.id
            }
            stateFlow.update { CardsListState.Content(it.mode, true, list) }
            eventFlow.value = CardListEvent.RemoveCardSuccess(deletedCard, indexAt)
        }
    }

    override fun onCleared() {
        manager.cancelAll()
        super.onCleared()
    }

    companion object {
        fun factory(
            sdk: AcquiringSdk,
            connectionChecker: ConnectionChecker,
            bankCaptionProvider: BankCaptionProvider,
            manager: CoroutineManager = CoroutineManager()
        ) = viewModelFactory {
            initializer {
                CardsListViewModel(
                    createSavedStateHandle(),
                    sdk,
                    connectionChecker,
                    bankCaptionProvider,
                    manager
                )
            }
        }
    }
}