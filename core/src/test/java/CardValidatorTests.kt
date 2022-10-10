import org.junit.Test
import ru.tinkoff.acquiring.sdk.utils.CardValidator
import java.math.BigInteger

class CardValidatorTests {

    @Test
    fun validateCardNumbers() {
        cardNumbers.forEach { (number, valid) ->
            assert(CardValidator.validateNumber(number) == valid)

            if (valid) {
                val plusOne = BigInteger(number.filter { it.isDigit() }).plus(BigInteger.ONE)
                assert(!CardValidator.validateNumber(plusOne.toString()))
            }
        }
    }

    companion object {

        val cardNumbers = listOf(
            "2200700153794291" to true,
            "2200700137244116" to true,
            "5536913760764585" to true,
            "4377723770680589" to true,
            "2200700138507339" to true,
            "5536913848833717" to true,
            "4377727800905171" to true,
            "5536913873139972" to true,
            "4377723774934321" to true,
            "4377723785417910" to true,
            "2200700123091844" to true,
            "5536917714445613" to true,
            "5536917720166674" to true,
            "5536914174072839" to true,
            "2200700166478452" to true,
            "2200700161211247" to true,
            "5536914173066212" to true,
            "5536913796810709" to true,
            "5536913820381214" to true,
            "2200700141902550" to true,
            "4377723741224384" to true,
            "5536914175642002" to true,
            "5536914129278226" to true,
            "5536914011155144" to true,
            "2200700131493594" to true,
            "4377723772000471" to true,
            "6263015600827361" to true,
            "6263015600955329" to true,
            "2200700127017126" to true,
            "4377723788520868" to true,
            "5536914171108172" to true,
            "4377723788385296" to true,
            "2200700158268044" to true,
            "2200700130267361" to true,
            "6263015600105925" to true,
            "6263015600443961" to true,
            "6263015600015132" to true,
            "6263015600016577" to true,
            "6759649826438453" to true,
            "6331101999990073" to true,
            "633478111298873700" to true,
            "6799990100000000019" to true,
            "6304939304310009610" to true,
            "123456789015" to false,
            "1234567890151" to true,
            "1234567890123456789012345678" to true,
            "12345678901234567890123456788" to false,
            "0000000000000000" to false,
        )
    }
}