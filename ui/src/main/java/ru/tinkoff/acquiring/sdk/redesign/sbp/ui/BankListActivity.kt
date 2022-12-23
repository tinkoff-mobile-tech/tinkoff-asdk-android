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
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.redesign.common.util.AcqShimmerAnimator
import ru.tinkoff.acquiring.sdk.redesign.sbp.ui.BankListActivity.Companion.SBP_BANK_RESULT_CODE_NO_BANKS
import ru.tinkoff.acquiring.sdk.redesign.sbp.util.SbpHelper
import ru.tinkoff.acquiring.sdk.utils.ConnectionChecker
import ru.tinkoff.acquiring.sdk.utils.lazyView
import ru.tinkoff.acquiring.sdk.utils.showById

internal class BankListActivity : AppCompatActivity() {

    private lateinit var viewModel: BankListViewModel

    private val recyclerView: RecyclerView by lazyView(R.id.acq_bank_list_content)
    private val cardShimmer: LinearLayout by lazyView(R.id.acq_bank_list_shimmer)
    private val viewFlipper: ViewFlipper by lazyView(R.id.acq_view_flipper)
    private val stubImage: ImageView by lazyView(R.id.acq_stub_img)
    private val stubTitleView: TextView by lazyView(R.id.acq_stub_title)
    private val stubSubtitleView: TextView by lazyView(R.id.acq_stub_subtitle)
    private val stubButtonView: TextView by lazyView(R.id.acq_stub_retry_button)

    private lateinit var deeplink: String

    private var banks: List<String>? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            recyclerView.adapter?.notifyDataSetChanged()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acq_activity_bank_list)
        deeplink = intent.getStringExtra(EXTRA_DEEPLINK)!!

        viewModel = BankListViewModel({ nspkBanks ->
            SbpHelper.getBankApps(packageManager, deeplink, nspkBanks)
        }, ConnectionChecker(application))
        viewModel.loadData()

        initToolbar()
        initViews()
        subscribeOnState()
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
        setResult(RESULT_CANCELED)
        finish()
    }

    private fun initViews() {
        recyclerView.adapter = Adapter()
    }

    private fun subscribeOnState() {
        lifecycleScope.launch {
            subscribeOnUiState()
        }
    }

    private fun CoroutineScope.subscribeOnUiState() {
        launch {
            viewModel.stateUiFlow.collectLatest {
                when (it) {
                    is BankListState.Content -> {
                        viewFlipper.showById(R.id.acq_bank_list_content)
                        banks = it.banks
                    }
                    is BankListState.Shimmer -> {
                        viewFlipper.showById(R.id.acq_bank_list_shimmer)
                        AcqShimmerAnimator.animateSequentially(
                            cardShimmer.children.toList()
                        )
                    }
                    is BankListState.Error -> {
                        showStub(
                            imageResId = R.drawable.acq_ic_generic_error_stub,
                            titleTextRes = R.string.acq_generic_alert_label,
                            subTitleTextRes = R.string.acq_generic_stub_description,
                            buttonTextRes = R.string.acq_generic_alert_access
                        )
                        stubButtonView.setOnClickListener { _ -> finishWithError(it.throwable) }
                    }
                    is BankListState.NoNetwork -> {
                        showStub(
                            imageResId = R.drawable.acq_ic_no_network,
                            titleTextRes = R.string.acq_generic_stubnet_title,
                            subTitleTextRes = R.string.acq_generic_stubnet_description,
                            buttonTextRes = R.string.acq_generic_button_stubnet
                        )
                        stubButtonView.setOnClickListener {
                            viewModel.loadData()
                        }
                    }
                    is BankListState.Empty -> {
                        setResult(SBP_BANK_RESULT_CODE_NO_BANKS)
                        finish()
                    }
                }
            }
        }
    }

    private fun onBankSelected(packageName: String) {
        setResult(RESULT_OK, Intent().apply {
            putExtra(EXTRA_DEEPLINK, deeplink)
            putExtra(EXTRA_PACKAGE_NAME, packageName)
        })
        finish()
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

    private fun finishWithError(throwable: Throwable) {
        setErrorResult(throwable)
        finish()
    }

    private fun setErrorResult(throwable: Throwable) {
        val intent = Intent()
        intent.putExtra(TinkoffAcquiring.EXTRA_ERROR, throwable)
        setResult(TinkoffAcquiring.RESULT_ERROR, intent)
    }

    inner class Adapter : RecyclerView.Adapter<VH>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(this@BankListActivity).inflate(
                R.layout.acq_bank_list_item, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(banks!![position])

        override fun getItemCount(): Int = banks?.size ?: 0
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {

        private val logo = view.findViewById<ImageView>(R.id.acq_bank_list_item_logo)
        private val name = view.findViewById<TextView>(R.id.acq_bank_list_item_name)

        fun bind(packageName: String) {
            logo.setImageDrawable(packageManager.getApplicationIcon(packageName))
            name.text = packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0))

            itemView.setOnClickListener {
                onBankSelected(packageName)
            }
        }
    }

    companion object {
        internal const val EXTRA_DEEPLINK = "extra_deeplink"
        internal const val EXTRA_PACKAGE_NAME = "extra_package_name"

        internal const val SBP_BANK_RESULT_CODE_NO_BANKS = 501
    }
}

sealed class BankListState {
    object Shimmer : BankListState()
    object Empty : BankListState()
    class Error(val throwable: Throwable) : BankListState()
    object NoNetwork : BankListState()

    class Content(val banks: List<String>) : BankListState()
}

object BankList {

    sealed class Result
    class Success(val deeplink: String, val packageName: String) : Result()
    class Canceled : Result()
    class Error(val error: Throwable) : Result()
    class NoBanks() : Result()


    object Contract : ActivityResultContract<String, Result>() {

        override fun createIntent(context: Context, deeplink: String): Intent =
            Intent(context, BankListActivity::class.java).apply {
                putExtra(BankListActivity.EXTRA_DEEPLINK, deeplink)
            }

        override fun parseResult(resultCode: Int, intent: Intent?): Result = when (resultCode) {
            AppCompatActivity.RESULT_OK -> Success(
                intent!!.getStringExtra(BankListActivity.EXTRA_DEEPLINK)!!,
                intent.getStringExtra(BankListActivity.EXTRA_PACKAGE_NAME)!!)
            TinkoffAcquiring.RESULT_ERROR -> Error(intent!!.getSerializableExtra(TinkoffAcquiring.EXTRA_ERROR)!! as Throwable)
            SBP_BANK_RESULT_CODE_NO_BANKS -> NoBanks()
            else -> Canceled()
        }
    }
}