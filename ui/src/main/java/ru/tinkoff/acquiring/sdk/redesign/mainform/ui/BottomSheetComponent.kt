package ru.tinkoff.acquiring.sdk.redesign.mainform.ui

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.android.awaitFrame
import ru.tinkoff.acquiring.sdk.ui.customview.SystemKeyboardManager

/**
 * Created by i.golovachev
 */
internal class BottomSheetComponent(
    private val root: CoordinatorLayout,
    private val sheet: View,
    private val bottomSheetBehavior: BottomSheetBehavior<View> = BottomSheetBehavior.from(sheet),
    private val onSheetHidden: () -> Unit
) {

    private var systemKeyboardManager: SystemKeyboardManager? = null
    private var contentHeight: Int = 0

    init {
        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_HIDDEN -> onSheetHidden()
                    else -> Unit
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
        })
    }

    fun collapse() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    fun expand() {
        if (bottomSheetBehavior.halfExpandedRatio >= 0.9f) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        }
    }

    fun onAttachedToWindow(activity: AppCompatActivity) {
        systemKeyboardManager = SystemKeyboardManager(activity).init()

        systemKeyboardManager?.heightListener = object :
            SystemKeyboardManager.KeyboardHeightListener {
            override fun onHeightChanged(height: Int) {
                val ratio = (height + contentHeight).toFloat() / root.height
                when {
                    ratio > 1f -> {
                        bottomSheetBehavior.halfExpandedRatio = 0.9f
                    }
                    height > 0 -> {
                        bottomSheetBehavior.halfExpandedRatio = ratio
                    }
                    else -> {
                        bottomSheetBehavior.halfExpandedRatio = 0.9f
                    }
                }
                if (height > 0) expand() else collapse()
            }
        }
    }

    suspend fun trimSheetToContent(measuredView: View) {
        awaitFrame()
        awaitFrame()
        val insets = ViewCompat.getRootWindowInsets(sheet)
        val bottom = insets!!.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        val measuredHeight = measuredView.measuredHeight + bottom
        contentHeight = measuredHeight
        bottomSheetBehavior.setPeekHeight(measuredHeight, true)
    }

    fun onDetachWindow() {
        systemKeyboardManager?.heightListener = null
        systemKeyboardManager?.detach()
    }
}
