package ru.tinkoff.acquiring.sdk.utils

import android.widget.ViewFlipper
import androidx.core.view.children

/**
 * Created by Ivan Golovachev
 */
fun ViewFlipper.showById(id: Int) {
    displayedChild = children.indexOfFirst { it.id == id }
}