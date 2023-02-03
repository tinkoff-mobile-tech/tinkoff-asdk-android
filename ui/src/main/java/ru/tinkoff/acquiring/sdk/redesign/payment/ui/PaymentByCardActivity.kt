package ru.tinkoff.acquiring.sdk.redesign.payment.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.result.PaymentResult
import ru.tinkoff.acquiring.sdk.payment.PaymentByCardState
import ru.tinkoff.acquiring.sdk.redesign.common.carddatainput.CardDataInputFragment
import ru.tinkoff.acquiring.sdk.redesign.dialog.OnPaymentSheetCloseListener
import ru.tinkoff.acquiring.sdk.redesign.dialog.PaymentSheetStatus
import ru.tinkoff.acquiring.sdk.redesign.dialog.createPaymentSheetWrapper
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsHelper
import ru.tinkoff.acquiring.sdk.ui.activities.TransparentActivity
import ru.tinkoff.acquiring.sdk.ui.customview.LoaderButton
import ru.tinkoff.acquiring.sdk.utils.getOptions
import ru.tinkoff.acquiring.sdk.utils.lazyUnsafe
import ru.tinkoff.acquiring.sdk.utils.lazyView
import ru.tinkoff.acquiring.sdk.utils.toBundle

/**
 * Created by i.golovachev
 */
internal class PaymentByCardActivity : AppCompatActivity(),
    CardDataInputFragment.OnCardDataChanged, OnPaymentSheetCloseListener {

    private val cardDataInput
        get() = supportFragmentManager.findFragmentById(R.id.fragment_card_data_input) as CardDataInputFragment

    private val payButton: LoaderButton by lazyView(R.id.acq_pay_btn)

    private val viewModel: PaymentByCardViewModel by viewModels { PaymentByCardViewModel.factory() }

    private val statusSheetStatus = createPaymentSheetWrapper()

    private var onPaymentInternal: OnPaymentSheetCloseListener? = null

    private val paymentOptions : PaymentOptions by lazyUnsafe {
        intent.getOptions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acq_payment_by_card_new_activity)
        initToolbar()

        cardDataInput.setupScanner(paymentOptions.features.cameraCardScanner)
        cardDataInput.validateNotExpired = paymentOptions.features.validateExpiryDate

        lifecycleScope.launchWhenCreated { buttonState() }
        lifecycleScope.launch { processState() }
        payButton.setOnClickListener {
            viewModel.pay()
        }
    }

    override fun onResume() {
        super.onResume()

        if (statusSheetStatus.state != null
            && statusSheetStatus.state != PaymentSheetStatus.NotYet
            && statusSheetStatus.state != PaymentSheetStatus.Hide
        ) {
            if (statusSheetStatus.isAdded.not()) {
                statusSheetStatus.showNow(supportFragmentManager, null)
            }
        }
    }

    override fun onCardDataChanged(isValid: Boolean) {
        viewModel.setCardDate(
            cardNumber = cardDataInput.cardNumber,
            cvc = cardDataInput.cvc,
            dateExpired = cardDataInput.expiryDate,
            isValidCardData = isValid
        )
    }

    override fun onClose(state: PaymentSheetStatus) {

        if (onPaymentInternal != null) {
            onPaymentInternal?.onClose(state)
        } else {
            if (state is PaymentSheetStatus.Error) {
                finishWithError(state.throwable)
            } else {
                finish()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        viewModel.cancelPayment()
        setResult(RESULT_CANCELED)
        finish()
    }

    private fun initToolbar() {
        setSupportActionBar(findViewById(R.id.acq_toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setTitle(R.string.acq_banklist_title)
    }

    private suspend fun buttonState() {
        viewModel.state.collect {
            payButton.text = it.amount
            payButton.isEnabled = it.buttonEnabled
        }
    }

    private suspend fun processState() {
        viewModel.paymentProcessState.collect {
            payButton.isLoading = it is PaymentByCardState.Started

            when (it) {
                is PaymentByCardState.Created -> Unit
                is PaymentByCardState.Error -> {
                    statusSheetStatus.show(supportFragmentManager, null)
                    statusSheetStatus.state = PaymentSheetStatus.Error(
                        title = R.string.acq_commonsheet_failed_title,
                        mainButton = R.string.acq_commonsheet_failed_primary_button,
                        throwable = it.throwable
                    )
                }
                is PaymentByCardState.Started -> Unit
                is PaymentByCardState.Success -> Unit
                is PaymentByCardState.ThreeDsUiNeeded -> try {
                    ThreeDsHelper.Launch.launchBrowserBased(
                        this,
                        TransparentActivity.THREE_DS_REQUEST_CODE,
                        it.paymentOptions,
                        it.threeDsState.data,
                    )
                } catch (e: Throwable) {
                    statusSheetStatus.show(supportFragmentManager, null)
                    statusSheetStatus.state = PaymentSheetStatus.Error(
                        title = R.string.acq_commonsheet_failed_title,
                        mainButton = R.string.acq_commonsheet_failed_primary_button,
                        throwable = e
                    )
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == TransparentActivity.THREE_DS_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val result =
                    data.getSerializableExtra(ThreeDsHelper.Launch.RESULT_DATA) as PaymentResult
                statusSheetStatus.state = PaymentSheetStatus.Success(
                    title = R.string.acq_commonsheet_paid_title,
                    mainButton = R.string.acq_commonsheet_clear_primarybutton,
                    paymentId = result.paymentId!!,
                    cardId = result.cardId,
                    rebillId = result.rebillId
                )
            } else if (resultCode == ThreeDsHelper.Launch.RESULT_ERROR) {
                statusSheetStatus.state = PaymentSheetStatus.Error(
                    title = R.string.acq_commonsheet_failed_title,
                    mainButton = R.string.acq_commonsheet_failed_primary_button,
                    throwable = data?.getSerializableExtra(ThreeDsHelper.Launch.ERROR_DATA) as Throwable
                )
            } else {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun finishWithError(throwable: Throwable) {
        val intent = Intent()
        intent.putExtra(TinkoffAcquiring.EXTRA_ERROR, throwable)
        setResult(TinkoffAcquiring.RESULT_ERROR, intent)
        finish()
    }
}

object PaymentByCardResult {

    private const val EXTRA_ASDK_RESULT = "asdk_result"

    sealed class Result
    class Success(
        val paymentId: Long? = null,
        val cardId: String? = null,
        val rebillId: String? = null
    ) : Result()

    class Canceled : Result()
    class Error(val error: Throwable) : Result()


    object Contract : ActivityResultContract<PaymentOptions, Result>() {

        override fun createIntent(context: Context, paymentOptions: PaymentOptions): Intent =
            Intent(context, PaymentByCardActivity::class.java).apply {
                putExtras(paymentOptions.toBundle())
            }

        override fun parseResult(resultCode: Int, intent: Intent?): Result = when (resultCode) {
            AppCompatActivity.RESULT_OK -> {
                val result = intent!!.getSerializableExtra(EXTRA_ASDK_RESULT) as PaymentResult
                Success(
                    result.paymentId,
                    result.cardId,
                    result.rebillId
                )
            }
            TinkoffAcquiring.RESULT_ERROR -> Error(intent!!.getSerializableExtra(TinkoffAcquiring.EXTRA_ERROR)!! as Throwable)
            else -> Canceled()
        }
    }
}