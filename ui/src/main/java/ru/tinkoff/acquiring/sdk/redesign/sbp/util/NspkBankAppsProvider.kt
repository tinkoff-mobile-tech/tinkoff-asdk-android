package ru.tinkoff.acquiring.sdk.redesign.sbp.util

fun interface NspkBankAppsProvider {
    suspend fun getNspkApps() : Set<Any?>
}