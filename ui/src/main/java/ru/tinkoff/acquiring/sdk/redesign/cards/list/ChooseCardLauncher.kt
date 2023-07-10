package ru.tinkoff.acquiring.sdk.redesign.cards.list

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.models.options.screen.SavedCardsOptions
import ru.tinkoff.acquiring.sdk.redesign.cards.list.ui.CardsListActivity
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.RESULT_ERROR
import ru.tinkoff.acquiring.sdk.ui.activities.BaseAcquiringActivity
import ru.tinkoff.acquiring.sdk.utils.getError
import ru.tinkoff.acquiring.sdk.utils.getExtra

/**
 * @author k.shpakovskiy
 */
object ChooseCardLauncher {
    sealed class Result
    class Success(val card: Card) : Result()
    class Canceled : Result()
    class Error(val error: Throwable) : Result()
    object NeedInputNewCard : Result()

    object Contract : ActivityResultContract<SavedCardsOptions, Result>() {
        internal const val SELECT_NEW_CARD = 509
        internal const val EXTRA_CHOSEN_CARD = "extra_chosen_card"

        override fun createIntent(context: Context, input: SavedCardsOptions): Intent =
            BaseAcquiringActivity.createIntent(context, input, CardsListActivity::class)

        override fun parseResult(resultCode: Int, intent: Intent?): Result = when (resultCode) {
            RESULT_OK ->
                Success(
                    checkNotNull(
                        intent?.getExtra(EXTRA_CHOSEN_CARD, Card::class)
                    )
                )

            SELECT_NEW_CARD -> NeedInputNewCard
            RESULT_ERROR -> Error(intent.getError())
            else -> Canceled()
        }
    }
}
