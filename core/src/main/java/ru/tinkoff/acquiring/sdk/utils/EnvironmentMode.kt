package ru.tinkoff.acquiring.sdk.utils

sealed class EnvironmentMode {
    object IsDebugMode : EnvironmentMode()
    object IsPreProdMode : EnvironmentMode()
    object IsCustomMode : EnvironmentMode()
}