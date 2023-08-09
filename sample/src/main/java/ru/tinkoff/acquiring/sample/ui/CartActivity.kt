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
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.core.view.isVisible
import ru.tinkoff.acquiring.sample.R
import ru.tinkoff.acquiring.sample.adapters.CartListAdapter
import ru.tinkoff.acquiring.sample.models.BooksRegistry
import ru.tinkoff.acquiring.sample.models.Cart
import ru.tinkoff.acquiring.sample.ui.payable.PayableActivity
import ru.tinkoff.acquiring.sdk.utils.Money

/**
 * @author Mariya Chernyadieva
 */
class CartActivity : PayableActivity(), CartListAdapter.DeleteCartItemListener {

    private lateinit var textViewTotalPrice: TextView
    private lateinit var emptyCartTextView: TextView
    private lateinit var listViewCartItems: ListView
    private lateinit var cartContentLayout: LinearLayout
    private lateinit var buttonPay: TextView
    private lateinit var recurrentButton: TextView

    private var cartEmpty: Boolean = true
    private var adapter: ArrayAdapter<*>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_cart)

        textViewTotalPrice = findViewById(R.id.tv_total_price)
        cartContentLayout = findViewById(R.id.ll_cart_content)
        emptyCartTextView = findViewById(R.id.tv_empty_cart)
        listViewCartItems = findViewById(R.id.lv_cart_items)

        buttonPay = findViewById(R.id.card_pay)
        buttonPay.setOnClickListener {
            initPayment()
        }

        val sbpButton = findViewById<View>(R.id.btn_fps_pay)
        sbpButton.visibility = if (settings.isFpsEnabled) View.VISIBLE else View.GONE
        sbpButton.setOnClickListener {
            startSbpPayment()
        }

        setupTinkoffPay()

        setupMirPay()

        checkCartEmpty()

        setupYandexPay(R.style.AcquiringTheme_Base_Yandex,savedInstanceState)

        setupRecurrentParentPayment()
    }

    override fun onResume() {
        super.onResume()

        val registry = BooksRegistry()
        adapter = CartListAdapter(this, Cart, registry, this)
        listViewCartItems.adapter = adapter

        updateBottomBar()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.cart_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val clearItem = menu.findItem(R.id.menu_action_clear)
        clearItem.isVisible = !cartEmpty
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_action_clear) {
            Cart.clear()
            checkCartEmpty()
            invalidateOptionsMenu()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSuccessPayment() {
        super.onSuccessPayment()
        Cart.clear()
        invalidateOptionsMenu()
        checkCartEmpty()
    }

    override fun onDeleteItemPressed(cartEntry: Cart.CartEntry) {
        Cart.remove(cartEntry)
        checkCartEmpty()
        updateBottomBar()
        invalidateOptionsMenu()
        adapter!!.notifyDataSetChanged()
    }

    private fun updateBottomBar() {
        var priceCoins = 0L
        for (entry in Cart) {
            priceCoins += entry.getPrice().coins
        }

        totalPrice = Money.ofCoins(priceCoins)
        if (Cart.size > 0) {
            title = getString(R.string.pay_form_title_single)
            description = createBookDescription()
        } else {
            title = ""
            description = ""
        }

        val stringTotalPrice = getString(R.string.book_price_total, totalPrice)
        textViewTotalPrice.text = stringTotalPrice
        if (totalPrice.coins > 0L) {
            buttonPay.isEnabled = true
        }
    }

    private fun checkCartEmpty() {
        cartEmpty = Cart.size == 0
        emptyCartTextView.visibility = if (cartEmpty) View.VISIBLE else View.GONE
        cartContentLayout.visibility = if (cartEmpty) View.GONE else View.VISIBLE
    }

    private fun createBookDescription(): String {
        val result = StringBuilder()
        val booksRegistry = BooksRegistry()
        for (entry in Cart) {
            val book = booksRegistry.getBook(this, entry.bookId)
            result.append(book?.announce)
            result.append(",\n")
        }

        result.setLength(result.length - 2)
        return result.toString()
    }

    companion object {

        fun start(context: Context) {
            context.startActivity(Intent(context, CartActivity::class.java))
        }
    }
}
