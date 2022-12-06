package ru.tinkoff.acquiring.yandexpay

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.yandex.pay.core.*
import com.yandex.pay.core.data.*
import com.yandex.pay.core.ui.YandexPayButton
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.yandexpay.models.YandexPayData
import ru.tinkoff.acquiring.yandexpay.models.mapYandexOrder

/**
 * Created by i.golovachev
 */
class YandexButtonFragment : Fragment() {

    companion object {

        fun newInstance(
            data: YandexPayData, options: PaymentOptions
        ): YandexButtonFragment {
            return YandexButtonFragment().apply {
                arguments = bundleOf(
                    YandexButtonFragment::data.name to data,
                    YandexButtonFragment::options.name to options
                )
            }
        }
    }

    var listener : ((AcqYandexPayResult) -> Unit)? = null

    private val data: YandexPayData by lazy {
        arguments?.getSerializable(YandexButtonFragment::data.name) as YandexPayData
    }

    private val options: PaymentOptions by lazy {
        checkNotNull(arguments?.getParcelable(YandexButtonFragment::options.name))
    }

    private val yandexPayLauncher = registerForActivityResult(OpenYandexPayContract()) { result ->
        val acqResult = when (result) {
            is YandexPayResult.Success ->
                AcqYandexPayResult.Success(result.paymentToken.value)
            is YandexPayResult.Failure -> when (result) {
                is YandexPayResult.Failure.Validation -> AcqYandexPayResult.Error(result.details.name)
                is YandexPayResult.Failure.Internal -> AcqYandexPayResult.Error(result.message)
            }
            is YandexPayResult.Cancelled -> AcqYandexPayResult.Cancelled
        }
        listener?.invoke(acqResult)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        initYandexPay(data)
        val view =
            inflater.inflate(R.layout.acq_fragment_yandex_pay, container, false) as FrameLayout

        val button =
            inflater.inflate(
                R.layout.acq_view_yandex_pay_button,
                view,
                false
            ) as YandexPayButton

        view.addView(button)
        button.setOnClickListener { ->
            val orderDetails = OrderDetails(
                paymentMethods = data.toYandexPayMethods,
                order = options.mapYandexOrder()
            )
            // запустите сервис с помощью лаунчера, передав сформированные orderDetails
            yandexPayLauncher.launch(orderDetails)

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