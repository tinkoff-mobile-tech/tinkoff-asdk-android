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
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sample.R
import ru.tinkoff.acquiring.sample.models.Book
import ru.tinkoff.acquiring.sample.models.BooksRegistry
import ru.tinkoff.acquiring.sample.models.Cart
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.payment.methods.InitConfigurator.configure
import ru.tinkoff.acquiring.yandexpay.models.YandexPayData
import ru.tinkoff.acquiring.sdk.requests.performSuspendRequest
import ru.tinkoff.acquiring.yandexpay.*

/**
 * @author Mariya Chernyadieva
 */
class DetailsActivity : PayableActivity() {

    private lateinit var imageViewCover: ImageView
    private lateinit var textViewTitle: TextView
    private lateinit var textViewAuthor: TextView
    private lateinit var textViewYear: TextView
    private lateinit var textViewAnnotation: TextView
    private lateinit var textViewPrice: TextView
    private lateinit var buttonAddToCart: TextView

    private var book: Book? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bookId = intent.getIntExtra(EXTRA_BOOK, -1)
        check(bookId != -1) { "Book is not passed to the DetailsActivity. Start it with start() method" }
        val booksRegistry = BooksRegistry()
        book = booksRegistry.getBook(this, bookId)

        totalPrice = book?.price!!
        title = getString(R.string.pay_form_title_single)
        description = book!!.announce

        setContentView(R.layout.activity_details)

        imageViewCover = findViewById(R.id.iv_book_cover)
        textViewTitle = findViewById(R.id.tv_book_title)
        textViewAuthor = findViewById(R.id.tv_book_author)
        textViewYear = findViewById(R.id.tv_book_year)
        textViewAnnotation = findViewById(R.id.tv_book_annotation)
        textViewPrice = findViewById(R.id.tv_book_price)

        buttonAddToCart = findViewById(R.id.btn_add_to_cart)
        buttonAddToCart.setOnClickListener {
            Cart.add(Cart.CartEntry(book?.id!!, book?.price!!))
            Toast.makeText(this@DetailsActivity, R.string.added_to_cart, Toast.LENGTH_SHORT).show()
        }

        val buttonBuy = findViewById<TextView>(R.id.btn_buy_now)
        buttonBuy.setOnClickListener {
            initPayment()
        }

        val sbpButton = findViewById<View>(R.id.btn_fps_pay)
        sbpButton.visibility = if (settings.isFpsEnabled) View.VISIBLE else View.GONE
        sbpButton.setOnClickListener {
            startSbpPayment()
        }

        setupTinkoffPay()

        setupMirPay()

        setupYandexPay(savedInstanceState = savedInstanceState)

        fillViews()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.details_menu, menu)

        if (!settings.isFpsEnabled) {
            menu.findItem(R.id.menu_action_dynamic_qr).isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_action_cart -> {
                CartActivity.start(this)
                true
            }
            R.id.menu_action_dynamic_qr -> {
                openDynamicQrScreen()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun createYandexButtonFragment(
        savedInstanceState: Bundle?,
        paymentOptions: PaymentOptions,
        yandexPayData: YandexPayData,
        theme: Int?
    ): YandexButtonFragment {
        return savedInstanceState?.let {
            try {
                val yaFragment = (supportFragmentManager.getFragment(savedInstanceState, YANDEX_PAY_FRAGMENT_KEY) as? YandexButtonFragment)
                yaFragment?.also {
                    tinkoffAcquiring.addYandexResultListener(
                        fragment = it,
                        activity = this,
                        yandexPayRequestCode = YANDEX_PAY_REQUEST_CODE,
                        onYandexErrorCallback = { showErrorDialog() },
                        onYandexCancelCallback = {
                            Toast.makeText(this, R.string.payment_cancelled, Toast.LENGTH_SHORT)
                                .show()
                        },
                        onYandexSuccessCallback = ::handleYandexSuccess
                    )
                }
            } catch (i: IllegalStateException) {
                null
            }
        } ?: tinkoffAcquiring.createYandexPayButtonFragment(
            activity = this,
            yandexPayData = yandexPayData,
            options = paymentOptions,
            yandexPayRequestCode = YANDEX_PAY_REQUEST_CODE,
            themeId = theme,
            onYandexErrorCallback = { showErrorDialog() },
            onYandexCancelCallback = {
                Toast.makeText(this, R.string.payment_cancelled, Toast.LENGTH_SHORT).show()
            },
            onYandexSuccessCallback = ::handleYandexSuccess
        )
    }

    private fun fillViews() {
        imageViewCover.setImageResource(book!!.coverDrawableId)
        textViewTitle.text = book!!.title
        textViewAuthor.text = book!!.author
        textViewYear.text = book!!.year
        textViewAnnotation.text = book!!.annotation

        val price = getString(R.string.book_price, book!!.price)
        textViewPrice.text = price
    }

    protected fun handleYandexSuccess(it: AcqYandexPayResult.Success) {
        showProgressDialog()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                AcquiringSdk.log("=== ASDK combi init call")
                val result = tinkoffAcquiring.sdk.init { configure(it.paymentOptions) }.performSuspendRequest().getOrThrow()
                hideProgressDialog()
                tinkoffAcquiring.openYandexPaymentScreen(
                    this@DetailsActivity,
                    YANDEX_PAY_REQUEST_CODE,
                    it,
                    result.paymentId
                )
            } catch (e: java.lang.Exception) {
                if (e !is CancellationException) {
                    runOnUiThread {
                        hideProgressDialog()
                        showErrorDialog()
                    }
                }
            }
        }
    }

    companion object {

        private const val EXTRA_BOOK = "book"

        fun start(context: Context, book: Book) {
            val intent = Intent(context, DetailsActivity::class.java)
            intent.putExtra(EXTRA_BOOK, book.id)
            context.startActivity(intent)
        }
    }
}
