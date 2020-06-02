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

import ru.tinkoff.acquiring.sdk.utils.Money
import java.util.*

/**
 * @author Mariya Chernyadieva
 */
object Cart : ArrayList<Cart.CartEntry>() {

    data class CartEntry(val bookId: Int) {

        var count: Int = 1
            private set
        private var price: Money? = null

        constructor(bookId: Int, price: Money) : this(bookId) {
            this.price = price
            this.count = 1
        }

        fun increase() {
            count++
        }

        fun decrease(): Boolean {
            count--
            return count != 0
        }

        fun getPrice(): Money {
            return Money.ofCoins((price?.coins ?: 0) * count)
        }
    }

    override fun add(element: CartEntry): Boolean {
        for (entry in this) {
            if (entry == element) {
                entry.increase()
                return true
            }
        }
        return super.add(element)
    }

    override fun remove(element: CartEntry): Boolean {
        var forDelete: CartEntry? = null
        for (entry in this) {
            if (entry == element) {
                forDelete = entry
                break
            }
        }
        if (forDelete == null) {
            return false
        }

        return if (!forDelete.decrease()) {
            super.remove(element)
        } else true
    }
}
