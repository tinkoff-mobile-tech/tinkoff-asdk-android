package ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.vm

import android.app.Application
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.models.NspkRequest
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.payment.PaymentByCardProcess
import ru.tinkoff.acquiring.sdk.redesign.common.savedcard.SavedCardsRepository
import ru.tinkoff.acquiring.sdk.redesign.mainform.navigation.MainFormNavController
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MergeMethodsStrategy
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.primary.PrimaryButtonConfigurator
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.process.MainFormPaymentProcessMapper
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.secondary.SecondButtonConfigurator
import ru.tinkoff.acquiring.sdk.redesign.sbp.util.NspkBankAppsProvider
import ru.tinkoff.acquiring.sdk.redesign.sbp.util.NspkInstalledAppsChecker
import ru.tinkoff.acquiring.sdk.redesign.sbp.util.SbpHelper
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsHelper
import ru.tinkoff.acquiring.sdk.utils.BankCaptionResourceProvider
import ru.tinkoff.acquiring.sdk.utils.CoroutineManager

/**
 * Created by i.golovachev
 */
fun MainPaymentFormFactory(application: Application, paymentOptions: PaymentOptions) =  viewModelFactory {
    val acq = TinkoffAcquiring(
        application, paymentOptions.terminalKey, paymentOptions.publicKey
    )
    val savedCardRepo = SavedCardsRepository.Impl(acq.sdk)
    val navController = MainFormNavController()
    val cardPayProcessMapper = MainFormPaymentProcessMapper(navController)

    acq.initSbpPaymentSession()
    PaymentByCardProcess.init(acq.sdk, application, ThreeDsHelper.CollectData)

    initializer {
        val handle = createSavedStateHandle()
        MainFormInputCardViewModel(
            handle,
            PaymentByCardProcess.get(),
            cardPayProcessMapper,
            CoroutineManager(),
        )
    }
    initializer {
        val handle  = createSavedStateHandle()
        val nspkProvider = NspkBankAppsProvider { NspkRequest().execute().banks }
        val nspkChecker = NspkInstalledAppsChecker { nspkBanks, dl ->
            SbpHelper.getBankApps(application.packageManager, dl, nspkBanks)
        }
        val bankCaptionProvider = BankCaptionResourceProvider(application)
        MainPaymentFormViewModel(
            handle,
            ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentFormFactory(
                acq.sdk,
                savedCardRepo,
                PrimaryButtonConfigurator.Impl(
                    nspkProvider,
                    nspkChecker,
                    bankCaptionProvider
                ),
                SecondButtonConfigurator.Impl(nspkProvider, nspkChecker),
                MergeMethodsStrategy.ImplV1,
                paymentOptions.customer.customerKey!!
            ),
            navController,
            CoroutineManager(),
        )
    }
}