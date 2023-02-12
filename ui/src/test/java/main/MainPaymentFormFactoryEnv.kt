package main

import kotlinx.coroutines.runBlocking
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentFormFactory
import ru.tinkoff.acquiring.sdk.redesign.payment.model.CardChosenModel
import ru.tinkoff.acquiring.sdk.redesign.sbp.util.NspkBankAppsProvider
import ru.tinkoff.acquiring.sdk.redesign.sbp.util.NspkInstalledAppsChecker
import ru.tinkoff.acquiring.sdk.requests.GetCardListRequest
import ru.tinkoff.acquiring.sdk.requests.GetTerminalPayMethodsRequest
import ru.tinkoff.acquiring.sdk.requests.performSuspendRequest
import ru.tinkoff.acquiring.sdk.responses.*
import ru.tinkoff.acquiring.sdk.utils.BankCaptionProvider

val tinkoffAppSet = setOf("com.idamob.tinkoff.android")
val nspkAppSet = setOf("ru.nspk.sbpay")


/**
 * Created by i.golovachev
 */
internal class MainPaymentFormFactoryEnv(
    val customerKey: String = "customerKey",
    val defaultBank: String = "Tinkoff",
    val defaultCard: Card = Card("pan"),
    val defaultTinkoffDeeplink: String = "wwww.tinkoff.ru/tpay/",
    val defaultNspkDeeplink: String = "https://qr.nspk.ru/AS10003P3RH0LJ2A9ROO038L6NT5RU1M?type=01",
    val nspkBankAppsProvider: NspkBankAppsProvider = mock {},
    val getCardListRequest: GetCardListRequest = mock(),
    val getTerminalPayMethodsRequest: GetTerminalPayMethodsRequest = mock(),

    private var installedAppsProvider: NspkInstalledAppsChecker = NspkInstalledAppsChecker { _, _ ->
        emptyList()
    },
) {

    init {
        runBlocking {
            whenever(nspkBankAppsProvider.getNspkApps()).thenReturn(nspkAppSet + tinkoffAppSet)
        }
    }

    val sdk = mock<AcquiringSdk> {
        on { getCardList(any()) } doReturn getCardListRequest
        on { getTerminalPayMethods() } doReturn getTerminalPayMethodsRequest
    }
    val bankCaptionProvider = BankCaptionProvider { defaultBank }

    val mainPaymentFormFactory get() =
        MainPaymentFormFactory(
            sdk,
            nspkBankAppsProvider,
            installedAppsProvider,
            bankCaptionProvider,
            customerKey
        )

    val cardChosenModel = defaultCard.let { CardChosenModel(it, defaultBank) }

    suspend fun setInstalledApps(apps: List<String> = emptyList()) {
        installedAppsProvider = NspkInstalledAppsChecker { _, deeplink ->
            when (deeplink) {
                defaultNspkDeeplink -> apps
                defaultTinkoffDeeplink -> apps.filter { it == tinkoffAppSet.first() }
                else -> emptyList()
            }
        }
    }

    suspend fun setCard(list: List<Card> = emptyList()) {
        whenever(getCardListRequest.performSuspendRequest().getOrThrow()).thenReturn(
            GetCardListResponse(list.toTypedArray())
        )
    }

    suspend fun setCardError(throwable: Throwable) {
        whenever(getCardListRequest.performSuspendRequest().getOrThrow()).thenThrow(throwable)
    }

    suspend fun setMethod(vararg paymethod: Paymethod, addScheme: Boolean = false) {
        whenever(getTerminalPayMethodsRequest.performSuspendRequest().getOrThrow()).thenReturn(
            GetTerminalPayMethodsResponse(
                TerminalInfo(
                    paymethods = paymethod.map { PaymethodData(it) }, addCardScheme = addScheme
                )
            )
        )
    }

    suspend fun setMethodError(throwable: Throwable) {
        whenever(getCardListRequest.performSuspendRequest().getOrThrow()).thenThrow(
            throwable
        )
    }
}