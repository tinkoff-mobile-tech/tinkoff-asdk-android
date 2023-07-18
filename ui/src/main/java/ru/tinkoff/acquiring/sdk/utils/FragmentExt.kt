@file:Suppress("UNCHECKED_CAST")

package ru.tinkoff.acquiring.sdk.utils

import androidx.fragment.app.Fragment
import kotlin.properties.ReadOnlyProperty

/**
 * Created by i.golovachev
 */
fun <T> Fragment.getParent(): T? {
    val parent = (parentFragment as? T) ?: (activity as? T)
    return parent
}

fun <T> Fragment.serializableArg() = ReadOnlyProperty<Fragment, T?> { thisRef, property ->
    thisRef.arguments?.getSerializable(property.name) as T
}

fun <T> Fragment.parcelableArg() = ReadOnlyProperty<Fragment, T?> { thisRef, property ->
    thisRef.arguments?.getParcelable(property.name)
}