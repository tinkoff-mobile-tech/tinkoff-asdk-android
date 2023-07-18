package common

import org.junit.Assert

/**
 * Created by Ivan Golovachev
 */
fun assertByClassName(expected: Any?, actual: Any?) {
    Assert.assertEquals(expected?.javaClass?.simpleName, actual?.javaClass?.simpleName)
}

inline fun <reified T : Any> assertViaClassName(expected: Class<out T>, actual: T) {
    Assert.assertEquals(expected.simpleName, actual::class.java.simpleName)
}