@file:Suppress("UNCHECKED_CAST")

package ru.tinkoff.acquiring.sdk.utils

import androidx.fragment.app.Fragment
import ru.tinkoff.acquiring.sdk.redesign.common.carddatainput.CardDataInputFragment

/**
 * Created by i.golovachev
 */
fun <T> Fragment.getParent() : T? {
    val parent = (parentFragment as? T) ?: (activity as? T)
    return parent
}