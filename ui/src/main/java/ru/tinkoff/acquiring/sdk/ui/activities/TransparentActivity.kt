/*
 * Copyright © 2020 Tinkoff Bank
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ru.tinkoff.acquiring.sdk.ui.activities

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.widget.Toolbar
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.localization.AsdkLocalization
import ru.tinkoff.acquiring.sdk.localization.LocalizationResources
import ru.tinkoff.acquiring.sdk.models.ThreeDsData
import ru.tinkoff.acquiring.sdk.models.result.AsdkResult
import ru.tinkoff.acquiring.sdk.ui.customview.BottomContainer

/**
 * @author Mariya Chernyadieva
 */
internal open class TransparentActivity : BaseAcquiringActivity() {

    private lateinit var bottomContainer: BottomContainer
    private var toolbar: Toolbar? = null

    private lateinit var localization: LocalizationResources
    private var showBottomView = true
    private var orientation: Int = 0
    private var viewType: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        localization = AsdkLocalization.resources
        orientation = resources.configuration.orientation

        savedInstanceState?.let {
            showBottomView = it.getBoolean(STATE_SHOW_BOTTOM)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == THREE_DS_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                finishWithSuccess(data.getSerializableExtra(ThreeDsActivity.RESULT_DATA) as AsdkResult)
            } else if (resultCode == ThreeDsActivity.RESULT_ERROR) {
                finishWithError(data?.getSerializableExtra(ThreeDsActivity.ERROR_DATA) as Throwable)
            } else {
                setResult(Activity.RESULT_CANCELED)
                hideActivity()
            }
        } else {
            supportFragmentManager
                    .findFragmentById(R.id.acq_activity_fl_container)?.onActivityResult(requestCode, resultCode, data)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_SHOW_BOTTOM, showBottomView)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onBackPressed() {
        hideActivity()
    }

    override fun finishWithSuccess(result: AsdkResult) {
        setSuccessResult(result)
        hideActivity()
    }

    override fun finishWithError(throwable: Throwable) {
        setErrorResult(throwable)
        hideActivity()
    }

    protected fun initViews() {
        val optionsTheme = options.features.theme
        var activityTheme = R.style.AcquiringTheme
        if (optionsTheme != 0) activityTheme = optionsTheme

        setTheme(activityTheme)

        viewType = theme.obtainStyledAttributes(intArrayOf(R.attr.acqScreenViewType)).run {
            getInt(FULL_SCREEN_INDEX, EXPANDED_INDEX)
        }

        if (viewType == FULL_SCREEN_INDEX && activityTheme == R.style.AcquiringTheme) {
            setTheme(R.style.AcquiringTheme_Base)
        }

        resolveThemeMode(options.features.darkThemeMode)
        setContentView(R.layout.acq_activity)

        bottomContainer = findViewById(R.id.acq_activity_bottom_container)
        bottomContainer.setContainerStateListener(object : BottomContainer.ContainerStateListener {
            override fun onHidden() {
                finish()
                overridePendingTransition(0, 0)
            }

            override fun onShowed() = Unit

            override fun onFullscreenOpened() {
                showBottomView = false
            }
        })

        showBottomView = showBottomView && viewType == EXPANDED_INDEX && orientation == Configuration.ORIENTATION_PORTRAIT

        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            when (viewType) {
                EXPANDED_INDEX -> {
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    setupTranslucentStatusBar()
                }
                FULL_SCREEN_INDEX -> setupToolbar()
            }
        }

        bottomContainer.containerState = if (viewType == EXPANDED_INDEX && orientation == Configuration.ORIENTATION_PORTRAIT) {
            BottomContainer.STATE_SHOWED
        } else {
            BottomContainer.STATE_FULLSCREEN
        }
        bottomContainer.showInitAnimation = showBottomView
    }

    protected fun openThreeDs(data: ThreeDsData) {
        val intent = ThreeDsActivity.createIntent(this, options, data)
        startActivityForResult(intent, THREE_DS_REQUEST_CODE)
    }

    private fun setupTranslucentStatusBar() {
        if (Build.VERSION.SDK_INT in 19..20) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
        if (Build.VERSION.SDK_INT >= 21) {
            window.statusBarColor = Color.TRANSPARENT
        }
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.acq_toolbar)
        toolbar?.visibility = View.VISIBLE
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun hideActivity() {
        if (viewType == FULL_SCREEN_INDEX || orientation == Configuration.ORIENTATION_LANDSCAPE) {
            finish()
        } else {
            bottomContainer.hide()
        }
    }

    companion object {
        private const val FULL_SCREEN_INDEX = 0
        private const val EXPANDED_INDEX = 1

        private const val THREE_DS_REQUEST_CODE = 143

        private const val STATE_SHOW_BOTTOM = "state_show_bottom"
    }
}