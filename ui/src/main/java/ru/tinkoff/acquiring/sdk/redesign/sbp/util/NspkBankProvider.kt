package ru.tinkoff.acquiring.sdk.redesign.sbp.util

fun interface NspkBankProvider {
    suspend fun getNspkApps() : Set<Any?>
}