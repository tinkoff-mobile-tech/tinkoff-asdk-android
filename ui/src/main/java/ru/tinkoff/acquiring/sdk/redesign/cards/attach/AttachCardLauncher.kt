package ru.tinkoff.acquiring.sdk.redesign.cards.attach

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import ru.tinkoff.acquiring.sdk.models.options.screen.AttachCardOptions
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.EXTRA_CARD_ID
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.EXTRA_CARD_PAN
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.RESULT_ERROR
import ru.tinkoff.acquiring.sdk.ui.activities.AttachCardActivity
import ru.tinkoff.acquiring.sdk.ui.activities.BaseAcquiringActivity
import ru.tinkoff.acquiring.sdk.utils.getError

object AttachCardLauncher {

        sealed class Result
        class Success(
            val cardId: String,
            val panSuffix: String
            ) : Result()
        class Canceled : Result()
        class Error(val error: Throwable) : Result()

        object Contract : ActivityResultContract<AttachCardOptions, Result>() {

            override fun createIntent(context: Context, options: AttachCardOptions): Intent =
                BaseAcquiringActivity.createIntent(context, options, AttachCardActivity::class)

            override fun parseResult(resultCode: Int, intent: Intent?): Result = when (resultCode) {
                RESULT_OK -> {
                    checkNotNull(intent)
                    Success(
                        intent.getStringExtra(EXTRA_CARD_ID)!!,
                        intent.getStringExtra(EXTRA_CARD_PAN)?: ""
                    )
                }
                RESULT_ERROR -> Error(intent.getError())
                else -> Canceled()
            }
        }
    }
