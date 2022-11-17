package common

import org.junit.Assert

/**
 * Created by Ivan Golovachev
 */
fun assertByClassName(expected: Any?, actual: Any?) {
    Assert.assertEquals(expected?.javaClass?.simpleName, actual?.javaClass?.simpleName)
}

fun assertByClassName(expected: Class<*>, actual: Class<*>) {
    Assert.assertEquals(expected?.javaClass?.simpleName, actual?.javaClass?.simpleName)
}