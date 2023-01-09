package ru.tinkoff.acquiring.sdk.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ru.tinkoff.acquiring.sdk.R

/**
 * Created by i.golovachev
 */
internal class YandexPaymentStubFragment : BaseAcquiringFragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.acq_fragment_yandex_stub, container, false)
    }
}