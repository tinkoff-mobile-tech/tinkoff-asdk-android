package ru.tinkoff.acquiring.sdk.redesign.common.carddatainput

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.acq_fragment_card_data_input.*
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.cardscanners.CameraCardScanner
import ru.tinkoff.acquiring.sdk.cardscanners.CardScanner
import ru.tinkoff.acquiring.sdk.smartfield.BaubleClearButton
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.MASTER_CARD
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.VISA
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.validators.CardValidator
import ru.tinkoff.acquiring.sdk.utils.SimpleTextWatcher.Companion.afterTextChanged
import ru.tinkoff.decoro.MaskImpl
import ru.tinkoff.decoro.parser.UnderscoreDigitSlotsParser
import ru.tinkoff.decoro.watchers.MaskFormatWatcher

internal class CardDataInputFragment : Fragment() {

    private var cardScanner: CardScanner? = null

    var onComplete: ((CardDataInputFragment) -> Unit)? = null
    var validateNotExpired = false

    val cardNumberInput get() = card_number_input
    val expiryDateInput get() = expiry_date_input
    val cvcInput get() = cvc_input

    val cardNumber get() = CardNumberFormatter.normalizeCardNumber(card_number_input.text)
    val expiryDate get() = expiry_date_input.text.orEmpty()
    val cvc get() = cvc_input.text.orEmpty()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.acq_fragment_card_data_input, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(card_number_input) {
            BaubleClearButton().attach(this)
            val cardNumberFormatter = CardNumberFormatter().also {
                editText.addTextChangedListener(it)
            }

            editText.afterTextChanged {
                errorHighlighted = false

                val cardNumber = cardNumber
                val paymentSystem = CardPaymentSystem.resolvePaymentSystem(cardNumber)

                if (cardNumber.startsWith("0")) {
                    errorHighlighted = true
                    return@afterTextChanged
                }

                if (cardNumber.length in paymentSystem.range) {
                    if (!CardValidator.validateCardNumber(cardNumber)) {
                        errorHighlighted = true
                    } else if (cardNumberFormatter.isSingleInsert &&
                        shouldAutoSwitchFromCardNumber(cardNumber, paymentSystem)) {
                        expiry_date_input.requestViewFocus()
                    }
                }

                // logo
            }
        }

        with(expiry_date_input) {
            BaubleClearButton().attach(this)
            MaskFormatWatcher(createExpiryDateMask()).installOn(editText)

            editText.afterTextChanged {
                errorHighlighted = false

                val expiryDate = expiryDate
                if (expiryDate.length >= EXPIRY_DATE_MASK.length) {
                    if (CardValidator.validateExpireDate(expiryDate, false)) {
                        cvc_input.requestViewFocus()
                    } else {
                        errorHighlighted = true
                    }
                }
            }
        }

        with(cvc_input) {
            BaubleClearButton().attach(this)
            transformationMethod = PasswordTransformationMethod()
            MaskFormatWatcher(createCvcMask()).installOn(editText)

            editText.afterTextChanged {
                errorHighlighted = false

                val cvc = cvc
                if (cvc.length >= EXPIRY_DATE_MASK.length) {
                    if (CardValidator.validateSecurityCode(cvc)) {
                        cvc_input.clearViewFocus()
                        if (validate()) {
                            onComplete?.invoke(this@CardDataInputFragment)
                        }
                    } else {
                        errorHighlighted = true
                    }
                }
            }
        }

        cardNumberInput.requestViewFocus()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        cardScanner = CardScanner(context)
    }

    // todo results api
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            CameraCardScanner.REQUEST_CAMERA_CARD_SCAN, CardScanner.REQUEST_CARD_NFC -> {
                val scannedCardData = cardScanner?.getScanResult(requestCode, resultCode, data)
                if (scannedCardData != null) {
                    cardNumberInput.text = scannedCardData.cardNumber
                    expiryDateInput.text = scannedCardData.expireDate
                } else if (resultCode != Activity.RESULT_CANCELED) {
                    Toast.makeText(activity, "todo", Toast.LENGTH_SHORT).show()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun setupScanner(cameraCardScanner: CameraCardScanner?) {
        cardScanner = CardScanner(requireContext()).apply {
            this.cameraCardScanner = cameraCardScanner

            if (cardScanAvailable) {
                // add scan button
            }
        }
    }

    fun validate(): Boolean {
        var result = true
        if (CardValidator.validateCardNumber(cardNumber)) {
            card_number_input.errorHighlighted = true
            result = false
        }
        if (CardValidator.validateExpireDate(expiryDate, false)) {
            expiry_date_input.errorHighlighted = true
            result = false
        }
        if (CardValidator.validateSecurityCode(cvc)) {
            cvc_input.errorHighlighted = true
            result = false
        }
        return result
    }

    fun clearInput() {
        cardNumberInput.text = ""
        expiryDateInput.text = ""
        cvcInput.text = ""
    }

    private companion object {

        const val MIN_LENGTH_FOR_AUTO_SWITCH = 16

        const val EXPIRY_DATE_MASK = "__/__"
        const val CVC_MASK = "___"

        fun shouldAutoSwitchFromCardNumber(cardNumber: String, paymentSystem: CardPaymentSystem): Boolean {
            if (cardNumber.length == paymentSystem.range.last) return true

            if ((paymentSystem == VISA || paymentSystem == MASTER_CARD) &&
                cardNumber.length >= MIN_LENGTH_FOR_AUTO_SWITCH) {
                return true
            }
            return false
        }

        fun createExpiryDateMask(): MaskImpl = MaskImpl
            .createTerminated(UnderscoreDigitSlotsParser().parseSlots(EXPIRY_DATE_MASK))

        fun createCvcMask(): MaskImpl = MaskImpl
            .createTerminated(UnderscoreDigitSlotsParser().parseSlots(CVC_MASK))
    }
}