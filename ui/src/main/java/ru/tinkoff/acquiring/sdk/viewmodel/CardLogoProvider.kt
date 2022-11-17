package ru.tinkoff.acquiring.sdk.viewmodel

import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.redesign.common.carddatainput.CardNumberFormatter
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.MAESTRO
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.MASTER_CARD
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.MIR
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.UNION_PAY
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.VISA
import ru.tinkoff.acquiring.sdk.utils.BankIssuer
import ru.tinkoff.acquiring.sdk.utils.BankIssuer.ALFABANK
import ru.tinkoff.acquiring.sdk.utils.BankIssuer.GAZPROMBANK
import ru.tinkoff.acquiring.sdk.utils.BankIssuer.OTHER
import ru.tinkoff.acquiring.sdk.utils.BankIssuer.RAIFFEISEN
import ru.tinkoff.acquiring.sdk.utils.BankIssuer.SBERBANK
import ru.tinkoff.acquiring.sdk.utils.BankIssuer.TINKOFF
import ru.tinkoff.acquiring.sdk.utils.BankIssuer.UNKNOWN
import ru.tinkoff.acquiring.sdk.utils.BankIssuer.VTB

internal object CardLogoProvider {

    fun getCardLogo(cardNumber: String?): Int = with(CardNumberFormatter.normalize(cardNumber)) {
        getCardLogo(CardPaymentSystem.resolve(this), BankIssuer.resolve(this))
    }

    fun getCardLogo(paymentSystem: CardPaymentSystem, bankIssuer: BankIssuer): Int = when (bankIssuer) {
        SBERBANK -> when (paymentSystem) {
            MIR -> R.drawable.acq_ic_card_mir_sberbank
            MASTER_CARD -> R.drawable.acq_ic_card_mastercard_sberbank
            VISA -> R.drawable.acq_ic_card_visa_sberbank
            MAESTRO -> R.drawable.acq_ic_card_maestro_sberbank
            UNION_PAY -> R.drawable.acq_ic_card_unionpay_sberbank
            else -> R.drawable.acq_ic_card_unknown_unknown
        }
        VTB -> when (paymentSystem) {
            MIR -> R.drawable.acq_ic_card_mir_vtb
            MASTER_CARD -> R.drawable.acq_ic_card_mastercard_vtb
            VISA -> R.drawable.acq_ic_card_visa_vtb
            MAESTRO -> R.drawable.acq_ic_card_maestro_vtb
            UNION_PAY -> R.drawable.acq_ic_card_unionpay_vtb
            else -> R.drawable.acq_ic_card_unknown_unknown
        }
        ALFABANK -> when (paymentSystem) {
            MIR -> R.drawable.acq_ic_card_mir_alfabank
            MASTER_CARD -> R.drawable.acq_ic_card_mastercard_alfabank
            VISA -> R.drawable.acq_ic_card_visa_alfabank
            MAESTRO -> R.drawable.acq_ic_card_maestro_alfabank
            UNION_PAY -> R.drawable.acq_ic_card_unionpay_alfabank
            else -> R.drawable.acq_ic_card_unknown_unknown
        }
        TINKOFF -> when (paymentSystem) {
            MIR -> R.drawable.acq_ic_card_mir_tinkoff
            MASTER_CARD -> R.drawable.acq_ic_card_mastercard_tinkoff
            VISA -> R.drawable.acq_ic_card_visa_tinkoff
            MAESTRO -> R.drawable.acq_ic_card_maestro_tinkoff
            UNION_PAY -> R.drawable.acq_ic_card_unionpay_tinkoff
            else -> R.drawable.acq_ic_card_unknown_unknown
        }
        RAIFFEISEN -> when (paymentSystem) {
            MIR -> R.drawable.acq_ic_card_mir_raiffeisen
            MASTER_CARD -> R.drawable.acq_ic_card_mastercard_raiffeisen
            VISA -> R.drawable.acq_ic_card_visa_raiffeisen
            MAESTRO -> R.drawable.acq_ic_card_maestro_raiffeisen
            UNION_PAY -> R.drawable.acq_ic_card_unionpay_raiffeisen
            else -> R.drawable.acq_ic_card_unknown_unknown
        }
        GAZPROMBANK -> when (paymentSystem) {
            MIR -> R.drawable.acq_ic_card_mir_gazprombank
            MASTER_CARD -> R.drawable.acq_ic_card_mastercard_gazprombank
            VISA -> R.drawable.acq_ic_card_visa_gazprombank
            MAESTRO -> R.drawable.acq_ic_card_maestro_gazprombank
            UNION_PAY -> R.drawable.acq_ic_card_unionpay_gazprombank
            else -> R.drawable.acq_ic_card_unknown_unknown
        }
        OTHER -> when (paymentSystem) {
            MIR -> R.drawable.acq_ic_card_mir_other
            MASTER_CARD -> R.drawable.acq_ic_card_mastercard_other
            VISA -> R.drawable.acq_ic_card_visa_other
            MAESTRO -> R.drawable.acq_ic_card_maestro_other
            UNION_PAY -> R.drawable.acq_ic_card_unionpay_other
            else -> R.drawable.acq_ic_card_unknown_unknown
        }
        UNKNOWN -> when (paymentSystem) {
            MIR -> R.drawable.acq_ic_card_mir_unknown
            MASTER_CARD -> R.drawable.acq_ic_card_mastercard_unknown
            VISA -> R.drawable.acq_ic_card_visa_unknown
            MAESTRO -> R.drawable.acq_ic_card_maestro_unknown
            UNION_PAY -> R.drawable.acq_ic_card_unionpay_unknown
            else -> R.drawable.acq_ic_card_unknown_unknown
        }
    }
}