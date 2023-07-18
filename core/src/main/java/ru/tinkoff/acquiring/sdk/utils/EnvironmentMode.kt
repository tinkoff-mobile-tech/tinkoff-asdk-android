package ru.tinkoff.acquiring.sdk.utils

sealed class EnvironmentMode {

    object IsPreProdMode : EnvironmentMode()
    object IsDebugMode: EnvironmentMode()
    object IsCustomMode: EnvironmentMode()

}