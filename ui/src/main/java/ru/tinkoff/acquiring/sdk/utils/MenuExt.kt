package ru.tinkoff.acquiring.sdk.utils

import android.view.Menu

/**
 * Created by i.golovachev
 */

fun Menu.menuItemVisible(menuId: Int, isVisible: Boolean) {
    findItem(menuId)?.isVisible = isVisible
}