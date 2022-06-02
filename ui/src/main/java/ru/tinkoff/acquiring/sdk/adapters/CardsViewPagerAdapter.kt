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

package ru.tinkoff.acquiring.sdk.adapters

import android.content.Context
import android.content.res.Configuration
import android.os.Parcel
import android.os.Parcelable
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.localization.AsdkLocalization
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.models.PaymentSource
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.paysources.AttachedCard
import ru.tinkoff.acquiring.sdk.models.paysources.CardData
import ru.tinkoff.acquiring.sdk.ui.customview.Shadow
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.EditCard
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.EditCardScanButtonClickListener

/**
 * @author Mariya Chernyadieva
 */
internal class CardsViewPagerAdapter(
        private val context: Context,
        private val options: PaymentOptions
) : PagerAdapter() {

    var enterCardPosition: Int? = null
        private set
    var canScanCard = false
    var scanButtonListener: EditCardScanButtonClickListener? = null

    var cardList: MutableList<Card> = mutableListOf()
        set(value) {
            field = value
            viewTypeList = MutableList(cardList.size) { PageViewType.CARD_ITEM }
            viewTypeList.add(PageViewType.ENTER_CARD)
            enterCardPosition = viewTypeList.size
            notifyDataSetChanged()
        }
    var enterCardData: CardData = CardData()
        get() {
            return if (enterCardView != null) {
                CardData(enterCardView!!.cardNumber, enterCardView!!.cardDate, enterCardView!!.cardCvc)
            } else {
                field
            }
        }
        set(value) {
            field = value
            enterCardView?.let {
                it.cardNumber = value.pan
                it.cardDate = value.expiryDate
                it.cardCvc = value.securityCode
            }
        }

    private var viewTypeList: MutableList<PageViewType> = mutableListOf()
    private var currentView: View? = null
    private var enterCardView: EditCard? = null
    private var rejectedItem: Int = -1
    private var isDarkMode = false

    private val attachedViews: SparseArray<View> = SparseArray<View>(SAVED_VIEW_CAPACITY)
    private var detachedViews: SparseArray<SparseArray<Parcelable?>?>? = null

    init {
        isDarkMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES
    }

    override fun isViewFromObject(view: View, any: Any): Boolean {
        return view === any
    }

    override fun getCount(): Int {
        return viewTypeList.size
    }

    override fun getItemPosition(view: Any): Int {
        return POSITION_NONE
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, view: Any) {
        currentView = view as View
        if (position == rejectedItem) {
            view.requestFocus()
        }
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val inflatedView: View?
        val inflater = LayoutInflater.from(context)
        inflatedView = inflater.inflate(R.layout.acq_item_card, container, false)
        inflatedView.background = Shadow(context, isDarkMode)

        if (detachedViews == null) {
            detachedViews = SparseArray()
        }

        val editCard = inflatedView.findViewById<EditCard>(R.id.acq_edit_card)

        when (viewTypeList[position]) {
            PageViewType.ENTER_CARD -> {
                editCard.run {
                    scanButtonClickListener = scanButtonListener
                    isScanButtonVisible = canScanCard
                    useSecureKeyboard = options.features.useSecureKeyboard
                    validateNotExpired = options.features.validateExpiryDate

                    val localization = AsdkLocalization.resources
                    cardNumberHint = localization.payCardPanHint ?: ""
                    cardDateHint = localization.payCardExpireDateHint ?: ""
                    cardCvcHint = localization.payCardCvcHint ?: ""
                }
                enterCardView = editCard

                val viewState = if (detachedViews!!.size() > 0) {
                    detachedViews?.get(detachedViews?.keyAt(0) ?: 0)
                } else {
                    null
                }

                if (viewState != null) {
                    inflatedView.restoreHierarchyState(viewState)
                }
                attachedViews.put(position, inflatedView)
            }
            PageViewType.CARD_ITEM -> {
                editCard.run {

                    cardList[position].pan?.let { cardNumber ->
                        editCard.cardNumber = cardNumber
                    }

                    cardList[position].expDate?.let { expDate ->
                        editCard.cardDate = expDate
                    }

                    useSecureKeyboard = options.features.useSecureKeyboard
                    isScanButtonVisible = false
                    validateNotExpired = options.features.validateExpiryDate

                    if (rejectedItem == position) {
                        setMode(EditCard.EditCardMode.EDIT_CVC_ONLY)
                    } else {
                        if (options.order.recurrentPayment) {
                            setMode(EditCard.EditCardMode.NUMBER_ONLY)
                            isEnabled = false
                        } else {
                            setMode(EditCard.EditCardMode.EDIT_CVC_ONLY)

                        }
                    }
                }
            }
        }

        editCard.setOnTextChangedListener { field, _ ->
            if (field == EditCard.EditCardField.SECURE_CODE && editCard.isFilledAndCorrect()) {
                editCard.clearFocus()
            }
        }

        container.addView(inflatedView)

        return inflatedView as View
    }

    override fun destroyItem(container: ViewGroup, position: Int, any: Any) {
        val view: View? = any as View?
        if (enterCardPosition == viewTypeList.lastIndex) {
            putInDetached(position, view)
            attachedViews.remove(position)
        }
        container.removeView(view)
    }

    override fun saveState(): SavedPagerState {
        var i = 0
        val size = attachedViews.size()
        while (i < size) {
            val position = attachedViews.keyAt(i)
            val view: View? = attachedViews.valueAt(i)
            putInDetached(position, view)
            i++
        }
        return SavedPagerState(detachedViews)
    }

    override fun restoreState(state: Parcelable?, loader: ClassLoader?) {
        val savedState = state as SavedPagerState
        detachedViews = savedState.detached
    }

    fun getSelectedPaymentSource(position: Int): PaymentSource {
        return when (viewTypeList[position]) {
            PageViewType.ENTER_CARD -> {
                enterCardData
            }
            PageViewType.CARD_ITEM -> {
                val selectedCard = cardList[position]

                if (options.order.recurrentPayment && position != rejectedItem) {
                    AttachedCard(selectedCard.rebillId)
                } else {
                    val cvv = currentView?.findViewById<EditCard>(R.id.acq_edit_card)?.cardCvc
                    AttachedCard(selectedCard.cardId, cvv)
                }
            }
        }
    }

    fun getCardPosition(cardId: String): Int? {
        cardList.forEachIndexed { index, card ->
            if (card.cardId == cardId) {
                return index
            }
        }
        return null
    }

    fun setRejectedCard(position: Int?) {
        rejectedItem = position ?: -1
        notifyDataSetChanged()
    }

    private fun putInDetached(position: Int, view: View?) {
        val viewState = SparseArray<Parcelable?>()
        view?.saveHierarchyState(viewState)
        if (detachedViews?.size() != 0) {
            detachedViews?.clear()
        }
        detachedViews?.put(position, viewState)
    }

    companion object {
        private const val SAVED_VIEW_CAPACITY = 1
    }

    internal enum class PageViewType {
        CARD_ITEM, ENTER_CARD
    }

    internal class SavedPagerState constructor(val detached: SparseArray<SparseArray<Parcelable?>?>?) : Parcelable {
        override fun writeToParcel(dest: Parcel, flags: Int) {
            if (detached == null) {
                dest.writeInt(-1)
                return
            }
            val size = detached.size()
            dest.writeInt(size)
            var i = 0
            while (i != size) {
                dest.writeInt(detached.keyAt(i))
                writeSparseArray(dest, detached.valueAt(i), flags)
                i++
            }
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<SavedPagerState> {

            override fun createFromParcel(parcel: Parcel): SavedPagerState? {
                var size = parcel.readInt()
                if (size == -1) {
                    return null
                }
                val map = SparseArray<SparseArray<Parcelable?>?>(size)
                while (size != 0) {
                    val key = parcel.readInt()
                    val value = readSparseArray(parcel, SavedPagerState::class.java.classLoader)
                    map.append(key, value)
                    size--
                }

                return SavedPagerState(map)
            }

            override fun newArray(size: Int): Array<SavedPagerState?> {
                return arrayOfNulls(size)
            }

            private fun readSparseArray(`in`: Parcel, loader: ClassLoader?): SparseArray<Parcelable?>? {
                var size = `in`.readInt()
                if (size == -1) {
                    return null
                }
                val map = SparseArray<Parcelable?>(size)
                while (size != 0) {
                    val key = `in`.readInt()
                    val value = `in`.readParcelable<Parcelable>(loader)
                    map.append(key, value)
                    size--
                }
                return map
            }

            private fun writeSparseArray(dest: Parcel, map: SparseArray<Parcelable?>?, flags: Int) {
                if (map == null) {
                    dest.writeInt(-1)
                    return
                }
                val size = map.size()
                dest.writeInt(size)
                var i = 0
                while (i != size) {
                    dest.writeInt(map.keyAt(i))
                    dest.writeParcelable(map.valueAt(i), flags)
                    i++
                }
            }
        }
    }
}