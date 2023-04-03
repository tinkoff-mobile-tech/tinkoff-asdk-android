package ru.tinkoff.acquiring.sdk.utils

import android.content.Intent

/**
 * Created by i.golovachev
 */
fun Intent.getAsError(key: String) = getSerializableExtra(key) as Throwable

fun Intent.getLongOrNull(key: String) : Long? = getLongExtra(key,-1).takeIf { it > -1 }