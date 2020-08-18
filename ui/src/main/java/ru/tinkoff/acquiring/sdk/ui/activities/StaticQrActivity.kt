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
import android.view.View
import androidx.lifecycle.Observer
import ru.tinkoff.acquiring.sdk.models.ErrorButtonClickedEvent
import ru.tinkoff.acquiring.sdk.models.ErrorScreenState
import ru.tinkoff.acquiring.sdk.models.FinishWithErrorScreenState
import ru.tinkoff.acquiring.sdk.models.LoadState
import ru.tinkoff.acquiring.sdk.models.LoadingState
import ru.tinkoff.acquiring.sdk.models.ScreenState
import ru.tinkoff.acquiring.sdk.ui.fragments.StaticQrFragment
import ru.tinkoff.acquiring.sdk.viewmodel.StaticQrViewModel

internal class StaticQrActivity : TransparentActivity() {

    private lateinit var viewModel: StaticQrViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initViews()

        viewModel = provideViewModel(StaticQrViewModel::class.java) as StaticQrViewModel

        if (savedInstanceState == null) {
            showFragment(StaticQrFragment())
        }
        observeLiveData()
    }

    private fun observeLiveData() {
        viewModel.run {
            loadStateLiveData.observe(this@StaticQrActivity, Observer { handleLoadState(it) })
            screenStateLiveData.observe(this@StaticQrActivity, Observer { handleScreenState(it) })
        }
    }

    private fun handleScreenState(screenState: ScreenState) {
        when (screenState) {
            is ErrorScreenState -> {
                showErrorScreen(screenState.message) {
                    hideErrorScreen()
                    viewModel.createEvent(ErrorButtonClickedEvent)
                }
            }
            is ErrorButtonClickedEvent -> viewModel.getStaticQr()
            is FinishWithErrorScreenState -> finishWithError(screenState.error)
        }
    }

    override fun handleLoadState(loadState: LoadState) {
        when (loadState) {
            is LoadingState -> {
                progressBar?.visibility = View.VISIBLE
                content?.visibility = View.INVISIBLE
            }
        }
    }
}