import app.cash.turbine.ReceiveTurbine
import kotlinx.coroutines.delay
import org.junit.Assert

/**
 * Created by Ivan Golovachev
 */

//почему то всегда нужно немного подождать перед взамодействием с турбиной.....
internal suspend fun turbineDelay() {
    delay(10)
}

suspend fun <T> ReceiveTurbine<T>.awaitWithConditionOrNext(condition: (T) -> Boolean) {
    val item = awaitItem()
    if (condition(item)) {
        Assert.assertTrue(condition(item))
    } else {
        awaitWithConditionOrNext(condition)
    }
}
