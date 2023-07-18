package main

import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.mockito.kotlin.*
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.redesign.common.savedcard.SavedCardsRepository
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentFormFactory
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentForm
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MergeMethodsStrategy
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.primary.PrimaryButtonConfigurator
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.secondary.SecondButtonConfigurator
import ru.tinkoff.acquiring.sdk.requests.GetTerminalPayMethodsRequest
import ru.tinkoff.acquiring.sdk.requests.performSuspendRequest
import ru.tinkoff.acquiring.sdk.responses.GetTerminalPayMethodsResponse
import ru.tinkoff.acquiring.sdk.responses.TerminalInfo
import ru.tinkoff.acquiring.sdk.utils.BankCaptionProvider


/**
 * Created by i.golovachev
 */

internal class MainPaymentFormFactoryUiTest {

    @Test
    fun `GIVEN paymethods Tpay & Spb & Cards`() = runBlocking {
        FormEnvironment(
            primary = MainPaymentForm.Primary.Tpay,
            secondary = setOf(
                MainPaymentForm.Secondary.Tpay,
                MainPaymentForm.Secondary.Spb,
                MainPaymentForm.Secondary.Cards(2)
            )
        ).assetUi(
            primary = MainPaymentForm.Primary.Tpay,
            secondary = setOf(MainPaymentForm.Secondary.Spb, MainPaymentForm.Secondary.Cards(2))
        )
    }

    @Test
    fun `GIVEN paymethods Spb & Cards`() = runBlocking {
        FormEnvironment(
            primary = MainPaymentForm.Primary.Spb,
            secondary = setOf(MainPaymentForm.Secondary.Spb, MainPaymentForm.Secondary.Cards(2))
        ).assetUi(
            primary = MainPaymentForm.Primary.Spb,
            secondary = setOf(MainPaymentForm.Secondary.Cards(2))
        )
    }

    @Test
    fun `GIVEN Cards`() = runBlocking {
        FormEnvironment(
            primary = MainPaymentForm.Primary.Card(null),
            secondary = setOf(MainPaymentForm.Secondary.Cards(2))
        ).assetUi(
            primary = MainPaymentForm.Primary.Card(null),
            secondary = setOf()
        )
    }

    @Test
    fun `WHEN error on getMethods`() = runBlocking {
        val env = FormEnvironment(
            primary = MainPaymentForm.Primary.Card(null),
            secondary = setOf(MainPaymentForm.Secondary.Cards(2))
        )
        env.setInfoMethodError()
        env.assetUi(
            primary = MainPaymentForm.Primary.Card(null),
            secondary = setOf()
        )
    }

    @Test
    fun `WHEN error on getCard`() = runBlocking {
        val env = FormEnvironment(
            primary = MainPaymentForm.Primary.Card(null),
            secondary = setOf(MainPaymentForm.Secondary.Cards(0))
        )
        env.setGetCardsError()
        env.assetUi(
            primary = MainPaymentForm.Primary.Card(null),
            secondary = setOf()
        )
    }

    class FormEnvironment(
        private val primary: MainPaymentForm.Primary,
        private val secondary: Set<MainPaymentForm.Secondary>
    ) {

        private val getTerminalPayMethodsRequest = mock<GetTerminalPayMethodsRequest>()
        private val sdk = mock<AcquiringSdk> {
            on { getTerminalPayMethods() } doReturn getTerminalPayMethodsRequest
        }
        private val savedCardsRepository = object : SavedCardsRepository {
            override suspend fun getCards(customerKey: String, force: Boolean) = emptyList<Card>()
        }
        private val primaryButtonConfigurator: PrimaryButtonConfigurator = mock()
        private val secondButtonConfigurator: SecondButtonConfigurator = mock()

        init {
            runBlocking {
                whenever(getTerminalPayMethodsRequest.performSuspendRequest().getOrThrow())
                    .thenReturn(GetTerminalPayMethodsResponse(TerminalInfo()))

                whenever(primaryButtonConfigurator.get(anyOrNull(), anyOrNull())).thenReturn(primary)

                whenever(secondButtonConfigurator.get(anyOrNull(), anyOrNull())).thenReturn(secondary)
            }
        }

        private val mainPaymentFormFactory
            get() = MainPaymentFormFactory(
                sdk,
                savedCardsRepository,
                primaryButtonConfigurator,
                secondButtonConfigurator,
                MergeMethodsStrategy.ImplV1,
                mock { on { isOnline() } doReturn true },
                bankCaptionProvider = BankCaptionProvider { "Tinkoff" },
                "_key"
            )


        suspend fun setInfoMethodError() {
            whenever(getTerminalPayMethodsRequest.performSuspendRequest().getOrThrow())
                .thenThrow(IllegalStateException())
        }

        suspend fun setGetCardsError() {
            whenever(getTerminalPayMethodsRequest.performSuspendRequest().getOrThrow())
                .thenThrow(IllegalStateException())
        }

        suspend fun assetUi(
            primary: MainPaymentForm.Primary,
            secondary: Set<MainPaymentForm.Secondary>
        ) {
            val ui = mainPaymentFormFactory.getState()
            Assert.assertEquals(ui.ui.primary, primary)
            Assert.assertEquals(ui.ui.secondaries, secondary)
        }
    }
}