package ru.tinkoff.acquiring.sdk.redesign.cards.list

import app.cash.turbine.test
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.redesign.cards.list.models.CardItemUiModel
import ru.tinkoff.acquiring.sdk.redesign.cards.list.presentation.CardsListViewModel
import ru.tinkoff.acquiring.sdk.redesign.cards.list.ui.CardListEvent
import ru.tinkoff.acquiring.sdk.redesign.cards.list.ui.CardListMode
import ru.tinkoff.acquiring.sdk.redesign.cards.list.ui.CardsListState
import ru.tinkoff.acquiring.sdk.requests.RemoveCardRequest
import ru.tinkoff.acquiring.sdk.responses.RemoveCardResponse
import ru.tinkoff.acquiring.sdk.utils.ConnectionChecker
import ru.tinkoff.acquiring.sdk.utils.CoroutineManager
import ru.tinkoff.acquiring.sdk.utils.RequestResult
import turbineDelay
import java.lang.Exception

/**
 * Created by Ivan Golovachev
 */
internal class CardsDeleteViewModelTest {

    val defaultContent = CardsListState.Content(
        CardListMode.ADD, false, listOf(createCard("1"), createCard("2")),
    )

    val extendsContent = CardsListState.Content(
        CardListMode.ADD, false, listOf(createCard("1"), createCard("2"), createCard("3")),
    )

    @Test
    fun `when card delete complete`() = runBlocking {
        with(Environment(initState = defaultContent)) {
            setResponse(RequestResult.Success(RemoveCardResponse(1)))
            vm.deleteCard(createCard("1"), "")
            checkState<CardsListState.Content>()
            checkEvent<CardListEvent.RemoveCard>()
        }
    }

    @Test
    fun `when card delete throw error`() = runBlocking {
        with(Environment(initState = defaultContent)) {
            setResponse(RequestResult.Failure(Exception()))
            vm.deleteCard(createCard("1"), "")
            checkEvent<CardListEvent.ShowError>()
            checkState<CardsListState.Content>()
        }
    }


    @Test
    fun `when card delete is offline`() = runBlocking {
        with(Environment(initState = defaultContent)) {
            setResponse(RequestResult.Failure(Exception()))
            setOnline(true)
            vm.deleteCard(createCard("1"), "")
            checkEvent<CardListEvent.ShowError>()
            checkState<CardsListState.Content>()
        }

    }

    @Test
    fun `when card delete multiply show empty`() = runBlocking {
        with(Environment(initState = defaultContent)) {
            setResponse(RequestResult.Success(RemoveCardResponse(1)))
            vm.deleteCard(createCard("1"), "")
            checkEvent<CardListEvent.RemoveCard>()
            setResponse(RequestResult.Success(RemoveCardResponse(2)))
            vm.deleteCard(createCard("2"), "")
            checkEvent<CardListEvent.RemoveCard>()

            checkState<CardsListState.Empty>()
        }
    }

    @Test
    fun `when card delete multiply show last card`() = runBlocking {
        with(Environment(initState = extendsContent)) {
            setResponse(RequestResult.Success(RemoveCardResponse(1)))
            vm.deleteCard(createCard("1"), "")
            checkEvent<CardListEvent.RemoveCard>()
            setResponse(RequestResult.Success(RemoveCardResponse(2)))
            vm.deleteCard(createCard("2"), "")
            checkEvent<CardListEvent.RemoveCard>()

            checkState<CardsListState.Content>()
        }
    }


    class Environment(
        initState: CardsListState,
        val connectionMock: ConnectionChecker = mock { on { isOnline() } doReturn true },
        val asdk: AcquiringSdk = mock { }
    ) {
        val dispatcher: CoroutineDispatcher = Dispatchers.Default

        val vm = CardsListViewModel(
            asdk,
            connectionMock,
            CoroutineManager(dispatcher, dispatcher)
        ).apply {
            stateFlow.value = initState
        }

        fun setState(initState: CardsListState) {
            vm.stateFlow.value = initState
        }

        fun setOnline(isOnline: Boolean) {
            whenever(connectionMock.isOnline()).doReturn(isOnline)
        }

        fun setResponse(response: RequestResult<out RemoveCardResponse>) {
            val request: RemoveCardRequest =
                mock { on { executeFlow() } doReturn MutableStateFlow(response) }

            whenever(asdk.removeCard(any())).doReturn(request)
        }


        suspend inline fun <reified T : CardsListState> checkState() {
            vm.stateFlow.test {
                val value = awaitItem()

                Assert.assertTrue(
                    "state instance is ${value.javaClass.simpleName}\n expected is ${T::class.simpleName}",
                    value is T
                )
                cancelAndIgnoreRemainingEvents()
            }
        }


        suspend inline fun <reified T : CardListEvent> checkEvent() {
            vm.eventFlow.filterNotNull().test {
                awaitItem().let {
                    val event = it

                    Assert.assertTrue(
                        "state instance is ${event?.javaClass?.simpleName}\n expected is ${T::class.simpleName}",
                        event is T
                    )
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }
    }

    private fun createCard(idMock: String): CardItemUiModel = mock { on { id } doReturn idMock }
}