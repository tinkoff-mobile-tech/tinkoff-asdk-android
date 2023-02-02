package ru.tinkoff.acquiring.sdk.utils

import android.content.Intent
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import ru.tinkoff.acquiring.sdk.models.options.screen.BaseAcquiringOptions

/**
 * Created by i.golovachev
 */


private const val EXTRA_OPTIONS = "options"

fun bundleOfOptions(options: BaseAcquiringOptions) = bundleOf(EXTRA_OPTIONS to options)

fun BaseAcquiringOptions.toBundle() = bundleOf(EXTRA_OPTIONS to this)

fun Intent.putOptions(options: BaseAcquiringOptions) {
    putExtra(EXTRA_OPTIONS, options)
}

fun <T : BaseAcquiringOptions> Intent.getOptions(): T {
    return checkNotNull(getParcelableExtra<T>(EXTRA_OPTIONS)) {
        "extra by key $EXTRA_OPTIONS not fount"
    }
}

fun <T : BaseAcquiringOptions> SavedStateHandle.getExtra(): T {
    return checkNotNull(get<T>(EXTRA_OPTIONS)) {
        "extra by key $EXTRA_OPTIONS not fount"
    }
}