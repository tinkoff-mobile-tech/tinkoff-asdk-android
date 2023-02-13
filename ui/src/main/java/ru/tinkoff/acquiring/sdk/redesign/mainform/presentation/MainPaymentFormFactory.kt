package ru.tinkoff.acquiring.sdk.redesign.mainform.presentation

import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.models.enums.CardStatus
import ru.tinkoff.acquiring.sdk.redesign.common.savedcard.SavedCardsRepository
import ru.tinkoff.acquiring.sdk.redesign.payment.model.CardChosenModel
import ru.tinkoff.acquiring.sdk.redesign.sbp.util.NspkBankAppsProvider
import ru.tinkoff.acquiring.sdk.redesign.sbp.util.NspkInstalledAppsChecker
import ru.tinkoff.acquiring.sdk.requests.performSuspendRequest
import ru.tinkoff.acquiring.sdk.responses.Paymethod
import ru.tinkoff.acquiring.sdk.responses.PaymethodData
import ru.tinkoff.acquiring.sdk.utils.BankCaptionProvider

/**
 * Created by i.golovachev
 */
internal class MainPaymentFormFactory(
    private val sdk: AcquiringSdk,
    private val savedCardsRepository: SavedCardsRepository,
    private val provider: NspkBankAppsProvider,
    private val checker: NspkInstalledAppsChecker,
    private val bankCaptionProvider: BankCaptionProvider,
    private val _customerKey: String
) {

    suspend fun primary(): MainPaymentFormUi.Primary {
        val methods = sdk.getTerminalPayMethods().performSuspendRequest()
            .getOrThrow().terminalInfo

        // не выносить что то в отдельные ф-ции!!!приведет к проблемам в бранчах при изменении алго
        return when {
            // 2.0
            methods == null -> MainPaymentFormUi.Primary.Card(null)

            // 2.1
            methods.addCardScheme && methods.paymethods.has(Paymethod.TinkoffPay) && methods.paymethods.has(
                Paymethod.SBP
            ) -> {
                if (checkApp(TINKOFF_MB_PACKAGE_ID)) {
                    MainPaymentFormUi.Primary.Tpay
                } else {
                    val saved = checkSavedCards()
                    when {
                        saved != null -> MainPaymentFormUi.Primary.Card(saved)
                        checkApp() -> MainPaymentFormUi.Primary.Spb
                        else -> MainPaymentFormUi.Primary.Card(null)
                    }
                }
            }

            // 2.2
            methods.addCardScheme.not() && methods.paymethods.has(Paymethod.TinkoffPay) && methods.paymethods.has(
                Paymethod.SBP
            ) -> {
                if (checkApp(TINKOFF_MB_PACKAGE_ID)) {
                    MainPaymentFormUi.Primary.Tpay
                } else {
                    when {
                        checkApp() -> MainPaymentFormUi.Primary.Spb
                        else -> MainPaymentFormUi.Primary.Card(null)
                    }
                }
            }

            // 2.3
            methods.addCardScheme && methods.paymethods.has(Paymethod.SBP) -> {
                val saved = checkSavedCards()
                when {
                    saved != null -> MainPaymentFormUi.Primary.Card(saved)
                    checkApp() -> MainPaymentFormUi.Primary.Spb
                    else -> MainPaymentFormUi.Primary.Card(null)
                }
            }

            // 2.4
            methods.addCardScheme.not() && methods.paymethods.has(Paymethod.SBP) -> {
                when {
                    checkApp() -> MainPaymentFormUi.Primary.Spb
                    else -> MainPaymentFormUi.Primary.Card(null)
                }
            }

            // 2.5
            methods.addCardScheme && methods.paymethods.has(Paymethod.TinkoffPay) -> {
                if (checkApp(TINKOFF_MB_PACKAGE_ID)) {
                    MainPaymentFormUi.Primary.Tpay
                } else {
                    MainPaymentFormUi.Primary.Card(checkSavedCards())
                }
            }

            // 2.6
            methods.addCardScheme.not() && methods.paymethods.has(Paymethod.TinkoffPay) -> {
                if (checkApp(TINKOFF_MB_PACKAGE_ID)) {
                    MainPaymentFormUi.Primary.Tpay
                } else {
                    MainPaymentFormUi.Primary.Card(null)
                }
            }

            // 2.7
            methods.addCardScheme -> {
                MainPaymentFormUi.Primary.Card(checkSavedCards())
            }

            // 2.8
            else -> MainPaymentFormUi.Primary.Card(null)
        }
    }

    //region utilits
    private fun List<PaymethodData>.has(paymethod: Paymethod) = any { it.paymethod == paymethod }

    private suspend fun checkSavedCards() = savedCardsRepository.getCards(_customerKey, true)
        .firstOrNull { it.status == CardStatus.ACTIVE }
        ?.run { CardChosenModel(this, bankCaptionProvider(pan!!)) }

    private suspend fun checkApp(packageName: String? = null): Boolean {
        val hasApp = if (packageName != null) {
            checker.checkInstalledApps(setOf(packageName), TPAY_DEEPLINK)
        } else {
            checker.checkInstalledApps(provider.getNspkApps(), NSPK_DEEPLINK)
        }
        return hasApp.isNotEmpty()
    }
    //endregion

    companion object {
        const val TINKOFF_MB_PACKAGE_ID = "com.idamob.tinkoff.android"
        const val NSPK_DEEPLINK = "https://qr.nspk.ru/83C25B892E5343E5BF30BA835C9CD2FE"
        const val TPAY_DEEPLINK = "https://www.tinkoff.ru/tpay/1923863684"
    }
}