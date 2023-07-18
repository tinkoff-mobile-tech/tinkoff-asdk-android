import common.TestHelper.tryToCreateCardNumber
import common.TestHelper.getParametersFromResources
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.tinkoff.acquiring.sdk.utils.BankIssuer

@RunWith(Parameterized::class)
class BankIssuerTests(
    private val expectedBank: BankIssuer,
    private val bin: String?
) {
    @Test
    fun verifyBankIssuers() {
        val testCardNumber = tryToCreateCardNumber(bin)

        assertEquals(expectedBank, BankIssuer.resolve(testCardNumber))
    }
    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun getParameters(): MutableList<Array<Any?>> {
            val allStringParams = getParametersFromResources("bankIssuerPayload")
            val convertedParams = mutableListOf<Array<Any?>>()

            if (allStringParams != null) {
                for (testParams in allStringParams) {
                    val firstParam = testParams[0]?.let { BankIssuer.valueOf(it) }
                    val secondParam = testParams[1]?.replace("\"", "")

                    convertedParams.add(arrayOf(firstParam, secondParam))
                }

            }

            return convertedParams
        }
    }
}