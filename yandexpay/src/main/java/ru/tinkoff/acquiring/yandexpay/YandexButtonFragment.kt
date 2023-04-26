package ru.tinkoff.acquiring.yandexpay

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StyleRes
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
            data: YandexPayData,
            options: PaymentOptions? = null,
            isProd: Boolean = false,
            enableLogging: Boolean = false,
            @StyleRes themeForYandexButton: Int? = null
        ): YandexButtonFragment {
            return YandexButtonFragment().apply {
                arguments = bundleOf(
                    YandexButtonFragment::data.name to data,
                    YandexButtonFragment::options.name to options,
                    YandexButtonFragment::isProd.name to isProd,
                    YandexButtonFragment::enableLogging.name to enableLogging,
                    YandexButtonFragment::theme.name to themeForYandexButton,
                )
            }
        }
    }

    internal var listener: AcqYandexPayCallback? = null

    private val data: YandexPayData by lazy {
        arguments?.getSerializable(YandexButtonFragment::data.name) as YandexPayData
    }

    lateinit var options: PaymentOptions

    private val theme: Int? by lazy {
        arguments?.getInt(YandexButtonFragment::theme.name)
    }

    private val enableLogging: Boolean by lazy {
        arguments?.getBoolean(YandexButtonFragment::enableLogging.name) ?: false
    }

    private val isProd: Boolean by lazy {
        arguments?.getBoolean(YandexButtonFragment::isProd.name) ?: false
    }

    private val yandexPayLauncher = registerForActivityResult(OpenYandexPayContract()) { result ->
        val acqResult = when (result) {
            is YandexPayResult.Success ->
                AcqYandexPayResult.Success(result.paymentToken.value, options)
            is YandexPayResult.Failure -> when (result) {
                is YandexPayResult.Failure.Validation -> AcqYandexPayResult.Error(result.details.name)
                is YandexPayResult.Failure.Internal -> AcqYandexPayResult.Error(result.message)
            }
            is YandexPayResult.Cancelled -> AcqYandexPayResult.Cancelled
        }
        listener?.invoke(acqResult)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val options = arguments?.getParcelable<PaymentOptions>(YandexButtonFragment::options.name)
        if (options != null) {
            this.options = options
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        initYandexPay(
            yadata = data,
            yandexPayEnvironment = if (isProd) YandexPayEnvironment.PROD else YandexPayEnvironment.SANDBOX,
            logging = enableLogging
        )

        val inf = theme?.let {
            inflater.cloneInContext(ContextThemeWrapper(requireContext(), it))
        } ?: inflater

        val button = inf.inflate(
            R.layout.acq_view_yandex_pay_button, container, false
        ) as YandexPayButton

        button.setOnClickListener { ->
            val orderDetails = OrderDetails(
                paymentMethods = data.toYandexPayMethods,
                order = options.mapYandexOrder()
            )
            // запустите сервис с помощью лаунчера, передав сформированные orderDetails
            yandexPayLauncher.launch(orderDetails)
        }

        return button
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