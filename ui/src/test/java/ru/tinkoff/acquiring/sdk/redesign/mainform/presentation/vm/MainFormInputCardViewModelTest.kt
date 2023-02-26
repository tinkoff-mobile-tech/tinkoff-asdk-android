package ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.vm

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.mock
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.utils.CoroutineManager

internal class MainFormInputCardViewModelTest {

    val vm = MainFormInputCardViewModel(
        SavedStateHandle.createHandle(null, mock()),
        mock(),
        CoroutineManager(io = Dispatchers.IO, main = Dispatchers.IO)
    )

    @Test
    fun test() {
        runBlocking {

            vm.payEnable.collectLatest {
                println(it)
            }

            vm.needEmail(true)
            vm.email("true")
            vm.setCvc("323")
            vm.setCvc("324")
        }



        Thread.sleep(1000)
    }
}