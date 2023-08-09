package ru.tinkoff.acquiring.sample.ui.payable.delegates

import androidx.activity.result.ActivityResultLauncher
import ru.tinkoff.acquiring.sample.SampleApplication
import ru.tinkoff.acquiring.sample.ui.payable.PayableActivity
import ru.tinkoff.acquiring.sdk.models.options.screen.SavedCardsOptions
import ru.tinkoff.acquiring.sdk.payment.RecurrentPaymentProcess
import ru.tinkoff.acquiring.sdk.redesign.cards.list.ChooseCardLauncher
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsHelper

/**
 * @author k.shpakovskiy
 */
interface CardsForRecurrentDelegate {
    fun initCardsForRecurrentDelegate(activity: PayableActivity)
    fun launchCardsScreen()
}

class CardsForRecurrent : CardsForRecurrentDelegate {
    private lateinit var activity: PayableActivity
    private lateinit var cardsForRecurrent: ActivityResultLauncher<SavedCardsOptions>

    override fun initCardsForRecurrentDelegate(activity: PayableActivity) {
        this.activity = activity
        with(activity) {
            cardsForRecurrent = registerForActivityResult(ChooseCardLauncher.Contract) { result ->
                when (result) {
                    is ChooseCardLauncher.Canceled -> Unit
                    is ChooseCardLauncher.Error -> Unit
                    is ChooseCardLauncher.Success -> launchRecurrent(result.card)
                    is ChooseCardLauncher.NeedInputNewCard -> Unit
                }
            }
        }
    }

    override fun launchCardsScreen() {
        with(activity) {
            RecurrentPaymentProcess.init(
                sdk = SampleApplication.tinkoffAcquiring.sdk,
                application = application,
                threeDsDataCollector = ThreeDsHelper.CollectData
            )
            cardsForRecurrent.launch(createSavedCardOptions())
        }
    }
}
