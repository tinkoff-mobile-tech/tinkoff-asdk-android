package ru.tinkoff.acquiring.sdk.redesign.dialog

import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Created by i.golovachev
 */

internal fun FragmentActivity.dismissDialog(tag: String = DIALOG_ACTIVITY_EXT) {
    val f = supportFragmentManager.findFragmentByTag(tag)
    if (f == null) {
        return
    } else {
        (f as BottomSheetDialogFragment).dismissAllowingStateLoss()
    }
}

internal fun FragmentActivity.showDialog(dialogFragment: BottomSheetDialogFragment) {
    dialogFragment.show(supportFragmentManager, DIALOG_ACTIVITY_EXT)
}

private const val DIALOG_ACTIVITY_EXT = "DIALOG_ACTIVITY_EXT"