package ru.tinkoff.acquiring.yandexpay

import org.junit.Assert
import org.junit.Test
import ru.tinkoff.acquiring.sdk.utils.Money
import ru.tinkoff.acquiring.yandexpay.models.toYandexString

/**
 * Created by i.golovachev
 */
class MoneyUtilsTest {

    @Test
    fun `when amount with coins`() {
        Assert.assertEquals(
            Money.ofCoins(1950).toYandexString(),
            "19.50"
        )
    }

    @Test
    fun `when amount without coins`() {
        Assert.assertEquals(
            Money.ofCoins(1900).toYandexString(),
            "19.00"
        )
    }

    @Test
    fun `when amount without one coin`() {
        Assert.assertEquals(
            Money.ofCoins(100901).toYandexString(),
            "1009.01"
        )
    }

    @Test
    fun `when amount its only coin`() {
        Assert.assertEquals(
            Money.ofCoins(25).toYandexString(),
            "0.25"
        )
    }
}