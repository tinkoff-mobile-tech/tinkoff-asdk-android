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

import android.os.Bundle
import androidx.lifecycle.Observer
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.models.ErrorButtonClickedEvent
import ru.tinkoff.acquiring.sdk.models.ErrorScreenState
import ru.tinkoff.acquiring.sdk.models.FinishWithErrorScreenState
import ru.tinkoff.acquiring.sdk.models.LoopConfirmationScreenState
import ru.tinkoff.acquiring.sdk.models.Screen
import ru.tinkoff.acquiring.sdk.models.ScreenState
import ru.tinkoff.acquiring.sdk.models.SingleEvent
import ru.tinkoff.acquiring.sdk.models.ThreeDsScreenState
import ru.tinkoff.acquiring.sdk.models.options.screen.AttachCardOptions
import ru.tinkoff.acquiring.sdk.ui.fragments.AttachCardFragment
import ru.tinkoff.acquiring.sdk.ui.fragments.LoopConfirmationFragment
import ru.tinkoff.acquiring.sdk.viewmodel.AttachCardViewModel

/**
 * @author Mariya Chernyadieva
 */
internal class AttachCardActivity : TransparentActivity() {

    private lateinit var attachCardViewModel: AttachCardViewModel
    private lateinit var attachCardOptions: AttachCardOptions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initViews()

        attachCardOptions = options as AttachCardOptions
        attachCardViewModel = provideViewModel(AttachCardViewModel::class.java) as AttachCardViewModel
        observeLiveData()

        if (savedInstanceState == null) {
            showFragment(AttachCardFragment())
        }
    }

    private fun observeLiveData() {
        with(attachCardViewModel) {
            loadStateLiveData.observe(this@AttachCardActivity, Observer { handleLoadState(it) })
            attachCardResultLiveData.observe(this@AttachCardActivity, Observer { finishWithSuccess(it) })
            screenStateLiveData.observe(this@AttachCardActivity, Observer { handleScreenState(it) })
            screenChangeEventLiveData.observe(this@AttachCardActivity, Observer { handleScreenChangeEvent(it) })
        }
    }

    private fun handleScreenState(screenState: ScreenState) {
        when (screenState) {
            is FinishWithErrorScreenState -> finishWithError(screenState.error)
            is ErrorScreenState -> {
                if (supportFragmentManager.findFragmentById(R.id.acq_activity_fl_container) !is LoopConfirmationFragment) {
                    showErrorScreen(screenState.message) {
                        hideErrorScreen()
                        attachCardViewModel.createEvent(ErrorButtonClickedEvent)
                    }
                }
            }
        }
    }

    private fun handleScreenChangeEvent(screenChangeEvent: SingleEvent<Screen>) {
        screenChangeEvent.getValueIfNotHandled()?.let { screen ->
            when (screen) {
                is ThreeDsScreenState -> openThreeDs(screen.data)
                is LoopConfirmationScreenState -> showFragment(LoopConfirmationFragment.newInstance(screen.requestKey))
            }
        }
    }
}