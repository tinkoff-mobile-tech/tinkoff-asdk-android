
import kotlinx.coroutines.delay

/**
 * Created by Ivan Golovachev
 */

//почему то всегда нужно немного подождать перед взамодействием с турбиной.....
internal suspend fun turbineDelay() {
    delay(10)
}