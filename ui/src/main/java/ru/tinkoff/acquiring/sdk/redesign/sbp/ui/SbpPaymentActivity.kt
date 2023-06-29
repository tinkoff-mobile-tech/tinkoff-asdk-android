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

package ru.tinkoff.acquiring.sdk.redesign.sbp.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.EXTRA_ERROR
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.EXTRA_PAYMENT_ID
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.RESULT_ERROR
import ru.tinkoff.acquiring.sdk.redesign.common.util.AcqShimmerAnimator
import ru.tinkoff.acquiring.sdk.redesign.dialog.*
import ru.tinkoff.acquiring.sdk.redesign.sbp.SbpPayLauncher.Contract.EXTRA_PAYMENT_DATA
import ru.tinkoff.acquiring.sdk.redesign.sbp.SbpPayLauncher.Contract.SBP_BANK_RESULT_CODE_NO_BANKS
import ru.tinkoff.acquiring.sdk.redesign.sbp.SbpPayLauncher.StartData
import ru.tinkoff.acquiring.sdk.redesign.sbp.util.SbpHelper.openSbpDeeplink
import ru.tinkoff.acquiring.sdk.utils.*
import ru.tinkoff.acquiring.sdk.utils.lazyUnsafe
import ru.tinkoff.acquiring.sdk.utils.lazyView

internal class SbpPaymentActivity : AppCompatActivity(), OnPaymentSheetCloseListener {

    private val startData: StartData by lazyUnsafe {
        intent.getParcelable(EXTRA_PAYMENT_DATA, StartData::class)
    }

    private val viewModel: SbpPaymentViewModel by viewModels {
        SbpPaymentViewModel.factory(ConnectionChecker(application))
    }

    private val statusFragment: PaymentStatusSheet = createPaymentSheetWrapper()

    private val recyclerView: RecyclerView by lazyView(R.id.acq_bank_list_content)
    private val cardShimmer: LinearLayout by lazyView(R.id.acq_bank_list_shimmer)
    private val viewFlipper: ViewFlipper by lazyView(R.id.acq_view_flipper)
    private val stubImage: ImageView by lazyView(R.id.acq_stub_img)
    private val stubTitleView: TextView by lazyView(R.id.acq_stub_title)
    private val stubSubtitleView: TextView by lazyView(R.id.acq_stub_subtitle)
    private val stubButtonView: TextView by lazyView(R.id.acq_stub_retry_button)

    private var banks: Map<String,String>? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            recyclerView.adapter?.notifyDataSetChanged()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acq_activity_bank_list)

        if (savedInstanceState == null) {
            viewModel.loadData(startData.paymentOptions, startData.paymentId)
        }

        initToolbar()
        initViews()
        subscribeOnState()
    }

    override fun onStart() {
        super.onStart()
        viewModel.startCheckingStatus()
    }

    private fun initToolbar() {
        setSupportActionBar(findViewById(R.id.acq_toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setTitle(R.string.acq_banklist_title)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        viewModel.cancelPayment()
        setResult(RESULT_CANCELED)
        finish()
    }

    override fun onClose(status: PaymentStatusSheetState) {
        when (status) {
            is PaymentStatusSheetState.Error -> finishWithError(status.throwable)
            is PaymentStatusSheetState.Progress -> {
                viewModel.cancelPayment()
                statusFragment.dismiss()
            }
            is PaymentStatusSheetState.Success -> finishWithResult(status.paymentId)
            else -> Unit
        }
    }

    private fun initViews() {
        recyclerView.adapter = Adapter()
    }

    private fun subscribeOnState() {
        lifecycleScope.launch { subscribeOnUiState() }
        lifecycleScope.launch { subscribeOnSheetState() }
    }

    private suspend fun subscribeOnUiState() {
        viewModel.stateUiFlow.collectLatest {
            when (it) {
                is SpbBankListState.Content -> {
                    viewFlipper.showById(R.id.acq_bank_list_content)
                    banks = it.banks
                }
                is SpbBankListState.Shimmer -> {
                    viewFlipper.showById(R.id.acq_bank_list_shimmer)
                    AcqShimmerAnimator.animateSequentially(
                        cardShimmer.children.toList()
                    )
                }
                is SpbBankListState.Error -> {
                    showStub(
                        imageResId = R.drawable.acq_ic_generic_error_stub,
                        titleTextRes = R.string.acq_generic_alert_label,
                        subTitleTextRes = R.string.acq_generic_stub_description,
                        buttonTextRes = R.string.acq_generic_alert_access
                    )
                    stubButtonView.setOnClickListener { _ -> finishWithError(it.throwable) }
                }
                is SpbBankListState.NoNetwork -> {
                    showStub(
                        imageResId = R.drawable.acq_ic_no_network,
                        titleTextRes = R.string.acq_generic_stubnet_title,
                        subTitleTextRes = R.string.acq_generic_stubnet_description,
                        buttonTextRes = R.string.acq_generic_button_stubnet
                    )
                    stubButtonView.setOnClickListener {
                        viewModel.loadData(startData.paymentOptions, startData.paymentId)
                    }
                }
                is SpbBankListState.Empty -> {
                    setResult(SBP_BANK_RESULT_CODE_NO_BANKS)
                    finish()
                    SbpNoBanksStubActivity.show(this)
                }
            }
        }
    }

    private suspend fun subscribeOnSheetState() {
        viewModel.paymentStateFlow.collect {
            statusFragment.state = it
            when (it) {
                is PaymentStatusSheetState.Hide -> if (statusFragment.isAdded) {
                    statusFragment.dismiss()
                }
                is PaymentStatusSheetState.NotYet -> Unit
                else -> if (statusFragment.isAdded.not()) {
                    statusFragment.show(supportFragmentManager, null)
                }
            }
        }
    }

    private fun onBankSelected(deeplink: String) {
        viewModel.onGoingToBankApp()
        openSbpDeeplink(deeplink, this) {
            viewModel.errorOpenDeeplink(it)
        }
    }

    private fun showStub(
        imageResId: Int,
        titleTextRes: Int?,
        subTitleTextRes: Int,
        buttonTextRes: Int
    ) {
        viewFlipper.showById(R.id.acq_card_list_stub)

        stubImage.setImageResource(imageResId)
        if (titleTextRes == null) {
            stubTitleView.visibility = View.GONE
        } else {
            stubTitleView.setText(titleTextRes)
            stubTitleView.visibility = View.VISIBLE
        }
        stubSubtitleView.setText(subTitleTextRes)
        stubButtonView.setText(buttonTextRes)
    }

    private fun finishWithResult(paymentId: Long) {
        val intent = Intent()
        intent.putExtra(EXTRA_PAYMENT_ID, paymentId)
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun finishWithError(throwable: Throwable) {
        setErrorResult(throwable)
        finish()
    }

    private fun setErrorResult(throwable: Throwable) {
        val intent = Intent()
        intent.putExtra(EXTRA_ERROR, throwable)
        setResult(RESULT_ERROR, intent)
    }

    inner class Adapter : RecyclerView.Adapter<VH>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(
                LayoutInflater.from(this@SbpPaymentActivity).inflate(
                    R.layout.acq_bank_list_item, parent, false
                )
            )

        override fun onBindViewHolder(holder: VH, position: Int) {
            val pair = banks!!.toList().get(position)
            return holder.bind(pair.first, pair.second)
        }

        override fun getItemCount(): Int = banks?.size ?: 0
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {

        private val logo = view.findViewById<ImageView>(R.id.acq_bank_list_item_logo)
        private val name = view.findViewById<TextView>(R.id.acq_bank_list_item_name)

        fun bind(packageName: String, deeplink: String) {
            logo.setImageDrawable(packageManager.getApplicationIcon(packageName))
            name.text = packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            )

            itemView.setOnClickListener {
                onBankSelected(deeplink)
            }
        }
    }
}

sealed class SpbBankListState {
    object Shimmer : SpbBankListState()
    object Empty : SpbBankListState()
    class Error(val throwable: Throwable) : SpbBankListState()
    object NoNetwork : SpbBankListState()
    class Content(val banks: Map<String,String>) : SpbBankListState()
}
