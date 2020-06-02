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

package ru.tinkoff.acquiring.sample.models

import android.content.Context
import ru.tinkoff.acquiring.sample.R
import ru.tinkoff.acquiring.sdk.utils.Money
import java.util.*

/**
 * @author Mariya Chernyadieva
 */
class BooksRegistry {

    private var books: ArrayList<Book>? = null

    fun getBook(context: Context, id: Int): Book? {
        for (book in getBooks(context)) {
            if (book.id == id) {
                return book
            }
        }
        return null
    }

    fun getBooks(context: Context): ArrayList<Book> {
        return books ?: initBooks(context)
    }

    private fun initBooks(context: Context): ArrayList<Book> {
        val resources = context.resources
        val descriptions = resources.getStringArray(R.array.book_descriptions)
        val authors = resources.getStringArray(R.array.book_authors)
        val titles = resources.getStringArray(R.array.book_titles)
        val prices = resources.getStringArray(R.array.book_prices)
        val years = resources.getStringArray(R.array.book_years)
        val size = resources.getInteger(R.integer.books_count)

        val booksList = ArrayList<Book>(size)
        for (i in 0 until size) {
            val book = Book(i)

            book.annotation = descriptions[i]
            book.author = authors[i]
            book.title = titles[i]
            book.year = years[i]
            val priceString = prices[i]
            val price = Money.ofRubles(java.lang.Double.valueOf(priceString))
            book.coverDrawableId = R.drawable.cover_1
            book.price = price
            booksList.add(book)
        }
        return booksList
    }
}
