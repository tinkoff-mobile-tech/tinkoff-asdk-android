package ru.tinkoff.acquiring.sdk.redesign.mainform.presentation

/**
 * Created by i.golovachev
 */
interface MergeMethodsStrategy {

    fun merge(
        primary: MainPaymentFormUi.Primary,
        secondaries: Set<MainPaymentFormUi.Secondary>
    ): MainPaymentFormUi.Ui

    object ImplV1 : MergeMethodsStrategy {
        override fun merge(
            primary: MainPaymentFormUi.Primary,
            secondaries: Set<MainPaymentFormUi.Secondary>
        ): MainPaymentFormUi.Ui {
            return MainPaymentFormUi.Ui(
                primary = primary,
                secondaries = secondaries.toMutableSet().apply {
                    removeIf { it.paymethod == primary.paymethod }
                    sortedBy { it.order }
                }
            )
        }
    }
}