package ru.tinkoff.acquiring.sdk.utils

import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Parcelable
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.models.options.screen.BaseAcquiringOptions
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants
import java.io.Serializable
import kotlin.reflect.KClass

/**
 * Created by i.golovachev
 */


private const val EXTRA_OPTIONS = "options"

fun bundleOfOptions(options: BaseAcquiringOptions) = bundleOf(EXTRA_OPTIONS to options)

fun BaseAcquiringOptions.toBundle() = bundleOf(EXTRA_OPTIONS to this)

fun Intent.putOptions(options: BaseAcquiringOptions) {
    putExtra(EXTRA_OPTIONS, options)
}

fun Intent.getAsError(key: String) = getSerializableExtra(key) as Throwable

fun Intent.getLongOrNull(key: String) : Long? = getLongExtra(key,-1).takeIf { it > -1 }

internal fun Intent.getSdk(application: Application): TinkoffAcquiring {
    val opt = getOptions<BaseAcquiringOptions>()
    return TinkoffAcquiring(
        application,
        opt.terminalKey,
        opt.publicKey
    )
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

fun <EXTRA : Serializable> Intent.getExtra(name: String, clazz: KClass<EXTRA>): EXTRA {
    return checkNotNull(
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            getSerializableExtra(name) as? EXTRA
        } else {
            getSerializableExtra(name, clazz.java)
        }
    )
}

fun <PARCELABLE : Parcelable> Intent.getParcelable(name: String, clazz: KClass<PARCELABLE>): PARCELABLE {
    return checkNotNull(
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(name) as? PARCELABLE
        } else {
            getParcelableExtra(name, clazz.java)
        }
    )
}

fun Intent?.getError(): Throwable {
    return checkNotNull(this?.getExtra(LauncherConstants.EXTRA_ERROR, Throwable::class))
}
