import org.junit.Test
import ru.tinkoff.acquiring.sdk.utils.BankIssuer
import ru.tinkoff.acquiring.sdk.utils.BankIssuer.ALFABANK
import ru.tinkoff.acquiring.sdk.utils.BankIssuer.GAZPROMBANK
import ru.tinkoff.acquiring.sdk.utils.BankIssuer.OTHER
import ru.tinkoff.acquiring.sdk.utils.BankIssuer.RAIFFEISEN
import ru.tinkoff.acquiring.sdk.utils.BankIssuer.SBERBANK
import ru.tinkoff.acquiring.sdk.utils.BankIssuer.TINKOFF
import ru.tinkoff.acquiring.sdk.utils.BankIssuer.UNKNOWN
import ru.tinkoff.acquiring.sdk.utils.BankIssuer.VTB

class BankIssuerTests {

    @Test
    fun verifyBankIssuers() {
        cardNumbers.forEach { (number, bankIssuer) ->
            assert(BankIssuer.resolve(number) == bankIssuer)
        }
    }

    companion object {

        val cardNumbers = listOf(
            "" to UNKNOWN,
            "4274" to UNKNOWN,
            "4274029" to SBERBANK,
            "427402" to SBERBANK,
            "427920213213" to SBERBANK,
            "54792721479214724" to SBERBANK,
            "515775" to VTB,
            "5257873423" to VTB,
            "5543633423213" to VTB,
            "415428" to ALFABANK,
            "43141732432" to ALFABANK,
            "477960324323532" to ALFABANK,
            "220070" to TINKOFF,
            "51890112412" to TINKOFF,
            "5389942143435" to TINKOFF,
            "402178" to RAIFFEISEN,
            "46272923432" to RAIFFEISEN,
            "5288093842487532" to RAIFFEISEN,
            "404136" to GAZPROMBANK,
            "48741521423" to GAZPROMBANK,
            "529278345353" to GAZPROMBANK,
            "247626" to OTHER,
            "789384" to OTHER,
            "218426" to OTHER,
        )
    }
}