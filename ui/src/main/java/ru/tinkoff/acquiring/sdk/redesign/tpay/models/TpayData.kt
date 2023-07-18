package ru.tinkoff.acquiring.sdk.redesign.tpay.models

import ru.tinkoff.acquiring.sdk.responses.Paymethod
import ru.tinkoff.acquiring.sdk.responses.TerminalInfo

/**
 * Created by i.golovachev
 */
fun TerminalInfo.getTinkoffPayVersion(): String? {
    return paymethods.firstOrNull { it.paymethod == Paymethod.TinkoffPay }
        ?.params
        ?.get("Version")

}

fun TerminalInfo?.enableTinkoffPay(): Boolean {
    this ?: return false
    return paymethods.any { it.paymethod == Paymethod.TinkoffPay }
}

fun TerminalInfo?.enableMirPay(): Boolean {
    this ?: return false
    return paymethods.any { it.paymethod == Paymethod.MirPay }
}
