package common

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Created by i.golovachev
 */
internal fun <T : Any> T.runWithEnv(
    given: suspend T.() -> Unit, `when`: suspend T.() -> Unit, then: suspend T.() -> Unit
) {
    runBlocking {
        launch { given.invoke(this@runWithEnv) }.join()
        launch { `when`.invoke(this@runWithEnv) }.join()
        launch { then.invoke(this@runWithEnv) }.join()
    }
}
