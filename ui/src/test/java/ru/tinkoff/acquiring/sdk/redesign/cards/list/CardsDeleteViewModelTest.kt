package ru.tinkoff.acquiring.sdk.redesign.cards.list

import app.cash.turbine.test
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.redesign.cards.list.models.CardItemUiModel
import ru.tinkoff.acquiring.sdk.redesign.cards.list.presentation.CardsListViewModel
import ru.tinkoff.acquiring.sdk.redesign.cards.list.ui.CardListEvent
import ru.tinkoff.acquiring.sdk.redesign.cards.list.ui.CardListMode
import ru.tinkoff.acquiring.sdk.redesign.cards.list.ui.CardsListState
import ru.tinkoff.acquiring.sdk.requests.RemoveCardRequest
import ru.tinkoff.acquiring.sdk.responses.RemoveCardResponse
import ru.tinkoff.acquiring.sdk.utils.RequestResult
import turbineDelay
import java.lang.Exception

/**
 * Created by Ivan Golovachev
 */
class CardsDeleteViewModelTest {

    @Test
    fun `when card delete complete`() = runBlocking {

        val deletedCardId = 1L
        val deletedCard = createCard(deletedCardId.toString())
        val otherCard = createCard("2")

        val vm = createViewModelMock(
            initState = CardsListState.Content(
                CardListMode.ADD, false, listOf(deletedCard, otherCard),
            ),
            hasConnection = true,
            response = RequestResult.Success(RemoveCardResponse(deletedCardId))
        )

        vm.deleteCard(deletedCard, "")
        delay(100)
        turbineDelay()
        vm.eventFlow.filterNotNull().test {
            val event = awaitItem()
            Assert.assertTrue(
                "event instance is ${event.javaClass.simpleName}\n expected is ${CardListEvent.RemoveCard::class.simpleName}",
                event is CardListEvent.RemoveCard
            )
        }
    }

    @Test
    fun `when card delete throw error`() = runBlocking {

        val deletedCardId = 1L
        val deletedCard = createCard(deletedCardId.toString())

        val vm = createViewModelMock(
            initState = CardsListState.Content(
                CardListMode.ADD, false, listOf(deletedCard),
            ),
            hasConnection = true,
            response = RequestResult.Failure(Exception())
        )

        vm.deleteCard(deletedCard, "")
        turbineDelay()
        vm.eventFlow.filterNotNull().test {
            val event = awaitItem()
            Assert.assertTrue(
                "event instance is ${event.javaClass.simpleName}\n expected is ${CardListEvent.ShowError::class.simpleName}",
                event is CardListEvent.ShowError
            )
        }
    }

    @Test
    fun `when single card delete show empty stub`() = runBlocking {

        val deletedCardId = 1L
        val deletedCard = createCard(deletedCardId.toString())
        val vm = createViewModelMock(
            initState = CardsListState.Content(
                CardListMode.ADD, false, listOf(deletedCard),
            ),
            hasConnection = true,
            response = RequestResult.Success(RemoveCardResponse(deletedCardId))
        )

        vm.deleteCard(deletedCard, "")
        turbineDelay()
        vm.stateUiFlow.test {
            val state = awaitItem()
            Assert.assertTrue(
                "state instance is ${state.javaClass.simpleName}\n expected is ${CardListEvent.ShowError::class.simpleName}",
                state is CardsListState.Empty
            )
        }
    }

    @Test
    fun `when single card delete show empty`() = runBlocking {

        val deletedCardId = 1L
        val deletedCard = createCard(deletedCardId.toString())
        val vm = createViewModelMock(
            initState = CardsListState.Content(
                CardListMode.ADD, false, listOf(deletedCard),
            ),
            hasConnection = true,
            response = RequestResult.Success(RemoveCardResponse(deletedCardId))
        )

        vm.deleteCard(deletedCard, "")
        turbineDelay()
        vm.stateUiFlow.test {
            val state = awaitItem()
            Assert.assertTrue(
                "state instance is ${state.javaClass.simpleName}\n expected is ${CardListEvent.ShowError::class.simpleName}",
                state is CardsListState.Empty
            )
        }
    }


    private fun createViewModelMock(
        initState: CardsListState,
        hasConnection: Boolean,
        response: RequestResult<out RemoveCardResponse>,
    ): CardsListViewModel {
        return CardsListViewModel(
            createSdkMock(response),
            mock { on { isOnline() } doReturn hasConnection }
        )
            .apply {
                stateFlow.value = initState
            }
    }

    private fun createSdkMock(response: RequestResult<out RemoveCardResponse>): AcquiringSdk {
        val request: RemoveCardRequest =
            mock { on { executeFlow() } doReturn MutableStateFlow(response) }
        return mock { on { removeCard(any()) } doReturn request }
    }

    private fun createCard(idMock: String): CardItemUiModel = mock { on { id } doReturn idMock }
}