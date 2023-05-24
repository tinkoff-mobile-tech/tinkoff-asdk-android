package main

import kotlinx.coroutines.runBlocking
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.models.enums.CardStatus
import ru.tinkoff.acquiring.sdk.redesign.common.savedcard.SavedCardsRepository
import ru.tinkoff.acquiring.sdk.redesign.common.util.InstalledAppChecker
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentFormFactory
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MergeMethodsStrategy
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.primary.PrimaryButtonConfigurator
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.secondary.SecondButtonConfigurator
import ru.tinkoff.acquiring.sdk.redesign.payment.model.CardChosenModel
import ru.tinkoff.acquiring.sdk.redesign.sbp.util.NspkBankAppsProvider
import ru.tinkoff.acquiring.sdk.redesign.sbp.util.NspkInstalledAppsChecker
import ru.tinkoff.acquiring.sdk.requests.GetTerminalPayMethodsRequest
import ru.tinkoff.acquiring.sdk.requests.performSuspendRequest
import ru.tinkoff.acquiring.sdk.responses.*
import ru.tinkoff.acquiring.sdk.utils.BankCaptionProvider

val tinkoffAppMap =
    mapOf("com.idamob.tinkoff.android" to "bank100000000004://qr.nspk.ru/83C25B892E5343E5BF30BA835C9CD2FE")
val nspkAppMap =
    mapOf("ru.sberbankmobile" to "bank100000000111://qr.nspk.ru/83C25B892E5343E5BF30BA835C9CD2FE")

val nspkC2bData  = listOf(
    NspkC2bResponse.NspkAppInfo("Тинькофф","","bank100000000004","com.idamob.tinkoff.android"),
    NspkC2bResponse.NspkAppInfo("Cбер","","bank100000000111","ru.sberbankmobile"),
)

/**
 * Created by i.golovachev
 */
internal class MainPaymentFormFactoryEnv(
    val customerKey: String = "customerKey",
    val defaultTinkoffDeeplink: String = "https://www.tinkoff.ru/tpay/1923863684",
    val defaultNspkDeeplink: String = "https://qr.nspk.ru/83C25B892E5343E5BF30BA835C9CD2FE",
    val nspkBankAppsProvider: NspkBankAppsProvider = mock {},
    val getTerminalPayMethodsRequest: GetTerminalPayMethodsRequest = mock(),

    private var installedAppsProvider: NspkInstalledAppsChecker = NspkInstalledAppsChecker { _, _ ->
        emptyMap()
    },
    private var installedAppChecker: InstalledAppChecker = mock {},

    private var savedCardsRepository: SavedCardsRepository = object : SavedCardsRepository {
        override suspend fun getCards(customerKey: String, force: Boolean): List<Card> {
            return emptyList()
        }
    },
) {

    init {
        runBlocking {
            whenever(nspkBankAppsProvider.getNspkApps()).thenReturn(nspkC2bData)
        }
    }

    val sdk = mock<AcquiringSdk> {
        on { getTerminalPayMethods() } doReturn getTerminalPayMethodsRequest
    }
    val bankCaptionProvider = BankCaptionProvider { defaultBank }

    val primaryButtonConfigurator
        get() = PrimaryButtonConfigurator.Impl(
            installedAppChecker,
            nspkBankAppsProvider,
            installedAppsProvider,
            bankCaptionProvider
        )

    val secondaryButtonConfigurator
        get() = SecondButtonConfigurator.Impl(
            installedAppChecker,
            nspkBankAppsProvider,
            installedAppsProvider
        )

    val mainPaymentFormFactory
        get() = MainPaymentFormFactory(
            sdk,
            savedCardsRepository,
            primaryButtonConfigurator,
            secondaryButtonConfigurator,
            MergeMethodsStrategy.ImplV1,
            mock { on { isOnline() } doReturn true },
            bankCaptionProvider,
            customerKey
        )

    suspend fun setInstalledApps(apps: Map<String, String> = emptyMap()) {
        installedAppsProvider = NspkInstalledAppsChecker { _, deeplink ->
            when (deeplink) {
                defaultNspkDeeplink -> apps
                defaultTinkoffDeeplink -> apps.filter { it.key == tinkoffAppMap.keys.first() }
                else -> emptyMap()
            }
        }

        installedAppChecker = object: InstalledAppChecker {
            override fun isInstall(packageName: String): Boolean {
                return apps[packageName] != null
            }
        }
    }

    suspend fun setCard(list: List<Card> = emptyList()) {
        savedCardsRepository = object : SavedCardsRepository {
            override suspend fun getCards(customerKey: String, force: Boolean): List<Card> {
                return list.toList()
            }
        }
    }

    suspend fun setCardError(throwable: Throwable) {
        savedCardsRepository = object : SavedCardsRepository {
            override suspend fun getCards(customerKey: String, force: Boolean): List<Card> {
                throw throwable
            }
        }
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
        whenever(getTerminalPayMethodsRequest.performSuspendRequest().getOrThrow()).thenThrow(
            throwable
        )
    }

    suspend fun setNspkError(throwable: Throwable) {
        whenever(nspkBankAppsProvider.getNspkApps()).thenThrow(throwable)
    }

    companion object {
        val defaultBank: String = "Tinkoff"
        val defaultCard: Card = Card("pan", status = CardStatus.ACTIVE)
        val cardChosenModel = defaultCard.let { CardChosenModel(it, defaultBank) }
    }
}
