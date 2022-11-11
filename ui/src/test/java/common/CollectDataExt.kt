package common

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert

/**
 * Created by Ivan Golovachev
 */

internal class MutableCollector<T>(
    private val f: Flow<T?>
) {

    private val values = mutableListOf<T?>()
    val flow: Flow<T> = flow {
        values.forEach { it?.let { emit(it) } }
    }
    private val scope = CoroutineScope(Dispatchers.IO)
    lateinit var collectJob: Job
    var expectedCount: Int = 0

    fun takeValues(expectedCount: Int) {
        this.expectedCount = expectedCount
        collectJob = scope.launch {
            f.collect {
                if (it != null) {
                    values += it
                }
                if (values.size == expectedCount) {
                    collectJob.cancel()
                }
                if (values.size > expectedCount) {
                    throw IllegalStateException("expected values are $expectedCount, incomed - ${values.size} \n$values")
                }
            }
        }
    }

    suspend fun joinWithTimeout(timeout: Long = 1000) {
        var waiting = 0L
        val step = 333L
        while (collectJob.isActive && timeout > waiting) {
            delay(step)
            waiting += step
        }
        Assert.assertEquals(expectedCount, values.size)
    }
}

