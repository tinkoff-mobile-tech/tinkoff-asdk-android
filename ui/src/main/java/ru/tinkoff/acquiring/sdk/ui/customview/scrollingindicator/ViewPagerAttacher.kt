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

package ru.tinkoff.acquiring.sdk.ui.customview.scrollingindicator

import android.database.DataSetObserver
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager

/**
 * @author Mariya Chernyadieva
 */
internal class ViewPagerAttacher : PagerAttacher<ViewPager> {

    private var customPageChangeListener: ScrollingPagerIndicator.OnPageChangeListener? = null

    private lateinit var onPageChangeListener: ViewPager.OnPageChangeListener
    private lateinit var dataSetObserver: DataSetObserver
    private lateinit var attachedAdapter: PagerAdapter
    private lateinit var pager: ViewPager

    override fun attachToPager(indicator: ScrollingPagerIndicator, pager: ViewPager) {
        checkNotNull(pager.adapter) { "Set adapter before call attachToPager() method" }
        attachedAdapter = pager.adapter!!

        this.pager = pager

        indicator.dotCount = attachedAdapter.count
        indicator.setCurrentPosition(pager.currentItem)

        dataSetObserver = object : DataSetObserver() {
            override fun onChanged() {
                indicator.reattach()
            }

            override fun onInvalidated() {
                onChanged()
            }
        }
        attachedAdapter.registerDataSetObserver(dataSetObserver)

        onPageChangeListener = getPageChangeListener(indicator)
        pager.addOnPageChangeListener(onPageChangeListener)
    }

    fun setCustomPageChangeListener(listener: ScrollingPagerIndicator.OnPageChangeListener?) {
        this.customPageChangeListener = listener
    }

    private fun getPageChangeListener(indicator: ScrollingPagerIndicator): ViewPager.OnPageChangeListener {
        return object : ViewPager.OnPageChangeListener {

            var idleState = true

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixel: Int) {
                val offset: Float = when {
                    positionOffset < 0 -> 0f
                    positionOffset > 1 -> 1f
                    else -> positionOffset
                }
                indicator.onPageScrolled(position, offset)
            }

            override fun onPageSelected(position: Int) {
                if (idleState) {
                    indicator.dotCount = attachedAdapter.count
                    indicator.setCurrentPosition(this@ViewPagerAttacher.pager.currentItem)
                }
            }

            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    idleState = true
                    customPageChangeListener?.onChange(this@ViewPagerAttacher.pager.currentItem)
                }
            }
        }
    }

    override fun detachFromPager() {
        attachedAdapter.unregisterDataSetObserver(dataSetObserver)
        pager.removeOnPageChangeListener(onPageChangeListener)
    }
}
