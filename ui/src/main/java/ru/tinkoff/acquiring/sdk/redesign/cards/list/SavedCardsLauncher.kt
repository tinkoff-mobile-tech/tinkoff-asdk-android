package ru.tinkoff.acquiring.sdk.redesign.cards.list

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import ru.tinkoff.acquiring.sdk.models.options.screen.SavedCardsOptions
import ru.tinkoff.acquiring.sdk.redesign.cards.list.ui.CardsListActivity
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.EXTRA_CARD_ID
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.EXTRA_ERROR
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.RESULT_ERROR
import ru.tinkoff.acquiring.sdk.ui.activities.BaseAcquiringActivity
import ru.tinkoff.acquiring.sdk.utils.getError
import ru.tinkoff.acquiring.sdk.utils.getExtra

object SavedCardsLauncher {

        sealed class Result
        class Success(val selectedCardId: String?, val cardListChanged: Boolean) : Result()
        class Canceled : Result()
        class Error(val error: Throwable) : Result()

        object Contract : ActivityResultContract<SavedCardsOptions, Result>() {
            internal const val EXTRA_CARD_LIST_CHANGED = "extra_cards_changed"

            override fun createIntent(context: Context, input: SavedCardsOptions): Intent =
                BaseAcquiringActivity.createIntent(context, input, CardsListActivity::class)

            override fun parseResult(resultCode: Int, intent: Intent?): Result = when (resultCode) {
                RESULT_OK ->
                    Success(
                        selectedCardId = intent?.getStringExtra(EXTRA_CARD_ID),
                        cardListChanged = intent?.getBooleanExtra(EXTRA_CARD_LIST_CHANGED, false) ?: false
                    )
                RESULT_ERROR ->
                    Error(intent.getError())
                else -> Canceled()
            }
        }
    }
