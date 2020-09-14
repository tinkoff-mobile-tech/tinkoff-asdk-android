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

package ru.tinkoff.acquiring.sdk.ui.fragments

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import ru.tinkoff.acquiring.sdk.localization.AsdkLocalization
import ru.tinkoff.acquiring.sdk.localization.LocalizationResources
import ru.tinkoff.acquiring.sdk.models.PaymentSource
import ru.tinkoff.acquiring.sdk.models.paysources.CardSource
import java.util.regex.Pattern

/**
 * @author Mariya Chernyadieva
 */
internal open class BaseAcquiringFragment : Fragment() {

    protected lateinit var localization: LocalizationResources

    companion object {
        private const val EMAIL_PATTERN = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        localization = AsdkLocalization.resources
    }

    protected fun validateInput(paymentSource: PaymentSource, email: String? = null): Boolean {
        if (paymentSource is CardSource) {
            try {
                paymentSource.validate()
            } catch (e: IllegalStateException) {
                Toast.makeText(
                        requireActivity(),
                        AsdkLocalization.resources.payDialogValidationInvalidCard,
                        Toast.LENGTH_SHORT
                ).show()
                return false
            }
        }

        if (email != null && (email.isEmpty() || !Pattern.compile(EMAIL_PATTERN).matcher(email).matches())) {
            Toast.makeText(
                    requireActivity(),
                    AsdkLocalization.resources.payDialogValidationInvalidEmail,
                    Toast.LENGTH_SHORT
            ).show()
            return false
        }
        return true
    }
}