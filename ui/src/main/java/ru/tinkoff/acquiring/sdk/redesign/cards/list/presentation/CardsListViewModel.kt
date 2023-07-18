package ru.tinkoff.acquiring.sdk.redesign.cards.list.presentation

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.exceptions.checkCustomerNotFoundError
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.models.enums.CardStatus
import ru.tinkoff.acquiring.sdk.models.options.screen.SavedCardsOptions
import ru.tinkoff.acquiring.sdk.redesign.cards.list.models.CardItemUiModel
import ru.tinkoff.acquiring.sdk.redesign.cards.list.ui.CardListEvent
import ru.tinkoff.acquiring.sdk.redesign.cards.list.ui.CardListMode
import ru.tinkoff.acquiring.sdk.redesign.cards.list.ui.CardListNav
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
    savedStateHandle: SavedStateHandle,
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

    @VisibleForTesting
    val navigationChannel = Channel<CardListNav>()

    val navigationFlow = navigationChannel.receiveAsFlow()

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
                        onFailure = { e ->
                            val list =
                                checkNotNull((stateFlow.value as? CardsListState.Content)?.cards)
                            stateFlow.update { CardsListState.Content(it.mode, true, list) }
                            eventFlow.value = CardListEvent.ShowCardDeleteError(e)
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

    fun returnBaseMode() {
        changeMode(
            if (selectedCardIdFlow.value == null)
                CardListMode.ADD
            else
                CardListMode.CHOOSE
        )
    }

    fun chooseCard(model: CardItemUiModel) {
        if (stateFlow.value.mode === CardListMode.CHOOSE) {
            eventFlow.value = CardListEvent.SelectCard(model.card)
        }
    }

    fun chooseNewCard() {
        if (stateFlow.value.mode === CardListMode.CHOOSE) {
            eventFlow.value = CardListEvent.SelectNewCard
        }
    }

    fun onAttachCard(cardPan: String) {
        eventFlow.value = CardListEvent.ShowCardAttachDialog(cardPan)
    }

    fun onStubClicked() = viewModelScope.launch {
        if (stateFlow.value.mode === CardListMode.CHOOSE) {
            eventFlow.value = CardListEvent.SelectNewCard
        } else {
            navigationChannel.send(CardListNav.ToAttachCard)
        }
    }

    fun onAddNewCardClicked() = viewModelScope.launch {
        if (stateFlow.value.mode === CardListMode.CHOOSE) {
            eventFlow.value = CardListEvent.SelectNewCard
        } else {
            navigationChannel.send(CardListNav.ToAttachCard)
        }
    }

    fun onBackPressed() {
        if (eventFlow.value is CardListEvent.RemoveCardProgress) return

        eventFlow.value = when (val state = stateFlow.value) {
            is CardsListState.Error -> CardListEvent.SelectCancel
            is CardsListState.NoNetwork -> CardListEvent.SelectCancel
            is CardsListState.Content -> handleCancelWithContent(state)
            else -> CardListEvent.SelectCancel
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
        val recurrentFilter = { card: Card ->
            if (recurrentOnly) card.rebillId.isNullOrBlank().not() else true
        }

        return it.filter { card -> card.status == CardStatus.ACTIVE && recurrentFilter(card) }
            .map {
                val cardNumber = checkNotNull(it.pan)
                CardItemUiModel(
                    card = it,
                    bankName = bankCaptionProvider(cardNumber),
                    showChoose = (selectedCardIdFlow.value == it.cardId) && mode === CardListMode.CHOOSE
                )
            }
    }

    private fun handleGetCardListError(it: Exception) {
        if (it.checkCustomerNotFoundError()) {
            stateFlow.value = CardsListState.Empty
        } else {
            stateFlow.value = CardsListState.Error(it)
        }
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

    /**
      если небыло предвыбранной карты -  выход с экрана не требует выбора новой карты
      если была предвыбранная карта   -
                                        при выбранной карте - возвращаем выбранную карту
                                        без выбранной карты - посылаем инфу, что нужно выбрать карту
     */
    private fun handleCancelWithContent(state: CardsListState.Content): CardListEvent {
        return when(val selectedId = selectedCardIdFlow.value) {
            null -> cancelWithoutPredefinedCard()
            else -> selectCardOrNew(state, selectedId)
        }
    }

    private fun cancelWithoutPredefinedCard() =  CardListEvent.SelectCancel

    private fun selectCardOrNew(
        state: CardsListState.Content,
        selectedId: String
    ) = state.cards.firstOrNull { it.id == selectedId }
        ?.let { CardListEvent.SelectCard(it.card) }
        ?: CardListEvent.SelectNewCard

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
