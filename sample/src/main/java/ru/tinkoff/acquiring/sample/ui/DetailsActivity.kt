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
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import ru.tinkoff.acquiring.sample.R
import ru.tinkoff.acquiring.sample.models.Book
import ru.tinkoff.acquiring.sample.models.BooksRegistry
import ru.tinkoff.acquiring.sample.models.Cart

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

    private fun fillViews() {
        imageViewCover.setImageResource(book!!.coverDrawableId)
        textViewTitle.text = book!!.title
        textViewAuthor.text = book!!.author
        textViewYear.text = book!!.year
        textViewAnnotation.text = book!!.annotation

        val price = getString(R.string.book_price, book!!.price)
        textViewPrice.text = price
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
