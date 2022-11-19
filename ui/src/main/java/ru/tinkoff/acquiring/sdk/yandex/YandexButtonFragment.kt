package ru.tinkoff.acquiring.sdk.yandex

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.yandex.pay.core.*
import com.yandex.pay.core.data.*
import com.yandex.pay.core.ui.YandexPayButton
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.models.options.screen.BaseAcquiringOptions
import ru.tinkoff.acquiring.sdk.yandex.models.YandexPayData

/**
 * Created by Your name
 */
class YandexButtonFragment : Fragment() {

    companion object {
        /**
         * если данные получены другим способом, возможность засетить их синхронно
         */
        fun newInstance(data: YandexPayData?, options: BaseAcquiringOptions): YandexButtonFragment {
            return YandexButtonFragment().apply {
                arguments = bundleOf(
                    YandexButtonFragment::data.name to data,
                    YandexButtonFragment::options.name to options
                )
            }
        }
    }

    private val data: YandexPayData? by lazy {
        arguments?.getSerializable(YandexButtonFragment::data.name) as? YandexPayData?
    }

    private val options: BaseAcquiringOptions by lazy {
        checkNotNull(arguments?.getParcelable(YandexButtonFragment::options.name))
    }

    private val viewModel: YandexButtonViewModel by lazy {
        YandexButtonViewModel(AcquiringSdk(options.terminalKey, options.publicKey))
    }

    private val yandexPayLauncher = registerForActivityResult(OpenYandexPayContract()) { result ->
        when (result) {
            is YandexPayResult.Success -> Unit  //showToast("token: ${result.paymentToken}")
            is YandexPayResult.Failure -> when (result) {
                is YandexPayResult.Failure.Validation -> Unit //showToast("failure: ${result.details}")
                is YandexPayResult.Failure.Internal -> Unit// showToast("failure: ${result.message}")
            }
            is YandexPayResult.Cancelled -> Unit //showToast("cancelled")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view =
            inflater.inflate(R.layout.acq_fragment_yandex_pay, container, false) as FrameLayout

        viewLifecycleOwner.lifecycleScope.launch {
            launch {
                viewModel.showButton.collect {
                    view.isVisible = it
                    if (it) {
                        val button =
                            inflater.inflate(
                                R.layout.acq_view_yandex_pay_button,
                                view,
                                false
                            ) as YandexPayButton
                        view.addView(button)
                        button.setOnClickListener { ->
                            val orderDetails = OrderDetails(
                                paymentMethods = listOf(
                                    PaymentMethod(
                                        // Что будет содержаться в платежном токене: зашифрованные данные банковской карты
                                        // или токенизированная карта
                                        listOf(AuthMethod.PanOnly),
                                        // Метод оплаты
                                        PaymentMethodType.Card,
                                        // ID поставщика платежных услуг
                                        Gateway.from("gatewayID"),
                                        // Список поддерживаемых платежных систем
                                        listOf(
                                            CardNetwork.Visa,
                                            CardNetwork.MasterCard,
                                            CardNetwork.MIR
                                        ),
                                        // ID продавца в системе поставщика платежных услуг
                                        GatewayMerchantID.from("MerchantGW1"),
                                    ),
                                ),
                                order = Order(OrderID.from("1"), Amount.from("5000"))
                            )

                            // запустите сервис с помощью лаунчера, передав сформированные orderDetails
                            yandexPayLauncher.launch(orderDetails)
                        }
                    } else {
                        view.removeAllViews()
                    }
                }
            }
            launch {
                viewModel.events.collect {
                    if (it is YandexButtonViewModel.Event.InitLib) {
                        initYandexPay(it.yaData)
                        viewModel.onInitYandexPay()
                    }
                }
            }
        }

        return view
    }

    private fun initYandexPay(
        yadata: YandexPayData,
        yandexPayEnvironment: YandexPayEnvironment = YandexPayEnvironment.SANDBOX,
        logging: Boolean = true
    ) {
        val merch = Merchant(
            id = MerchantId.from(yadata.merchantId),
            name = yadata.merchantName,
            url = yadata.merchantUrl
        )
        if (YandexPayLib.isSupported) {
            YandexPayLib.initialize(
                context = requireContext(),
                config = YandexPayLibConfig(
                    merchantDetails = merch,
                    environment = yandexPayEnvironment,
                    logging = logging,
                    locale = YandexPayLocale.SYSTEM,
                )
            )
        } else {
            throw IllegalAccessException()
        }
    }
}