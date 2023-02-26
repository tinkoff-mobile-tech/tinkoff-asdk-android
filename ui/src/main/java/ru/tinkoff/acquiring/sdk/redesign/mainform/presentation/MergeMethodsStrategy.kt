package ru.tinkoff.acquiring.sdk.redesign.mainform.presentation

/**
 * Created by i.golovachev
 */
internal interface MergeMethodsStrategy {

    fun merge(
        primary: MainPaymentForm.Primary,
        secondaries: Set<MainPaymentForm.Secondary>
    ): MainPaymentForm.Ui

    object ImplV1 : MergeMethodsStrategy {
        override fun merge(
            primary: MainPaymentForm.Primary,
            secondaries: Set<MainPaymentForm.Secondary>
        ): MainPaymentForm.Ui {
            return MainPaymentForm.Ui(
                primary = primary,
                secondaries = secondaries.toMutableSet().apply {
                    removeIf { it.paymethod == primary.paymethod }
                    sortedBy { it.order }
                }
            )
        }
    }
}