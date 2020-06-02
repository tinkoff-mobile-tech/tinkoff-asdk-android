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

package ru.tinkoff.acquiring.sample.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannedString
import android.text.style.ForegroundColorSpan
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import ru.tinkoff.acquiring.sample.R
import ru.tinkoff.acquiring.sdk.utils.Money

/**
 * @author Mariya Chernyadieva
 */
class PaymentResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_result)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val textView = findViewById<TextView>(R.id.tv_confirm)
        val intent = intent
        if (intent.hasExtra(EXTRA_PRICE)) {
            val price = intent.getSerializableExtra(EXTRA_PRICE) as Money

            val coloredPrice = SpannableString(price.toString())
            coloredPrice.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(this, R.color.colorPrimary)),
                    0,
                    coloredPrice.length,
                    SpannedString.SPAN_INCLUSIVE_INCLUSIVE
            )

            val text = getString(R.string.payment_result_success, coloredPrice)
            textView.text = text
        } else {
            val cardId = intent.getStringExtra(EXTRA_CARD_ID)
            val text = getString(R.string.attachment_result_success, cardId)
            textView.text = text
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {

        private const val EXTRA_PRICE = "price"
        private const val EXTRA_CARD_ID = "card_id"

        fun start(context: Context, price: Money) {
            val intent = Intent(context, PaymentResultActivity::class.java)
            intent.putExtra(EXTRA_PRICE, price)
            context.startActivity(intent)
        }

        fun start(context: Context, cardId: String) {
            val intent = Intent(context, PaymentResultActivity::class.java)
            intent.putExtra(EXTRA_CARD_ID, cardId)
            context.startActivity(intent)
        }
    }
}
