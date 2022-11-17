import org.junit.Test
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.MAESTRO
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.MASTER_CARD
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.MIR
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.UNION_PAY
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.VISA

class CardPaymentSystemTests {

    @Test
    fun verifyPaymentSystems() {
        cardNumbers.forEach { (number, paymentSystem) ->
            assert(CardPaymentSystem.resolve(number) == paymentSystem)
        }
    }

    companion object {

        val cardNumbers = listOf(
            "2200700153794291" to MIR,
            "2200700137244116" to MIR,
            "5536913760764585" to MASTER_CARD,
            "4377723770680589" to VISA,
            "2200700138507339" to MIR,
            "5536913848833717" to MASTER_CARD,
            "4377727800905171" to VISA,
            "5536913873139972" to MASTER_CARD,
            "4377723774934321" to VISA,
            "4377723785417910" to VISA,
            "2200700123091844" to MIR,
            "5536917714445613" to MASTER_CARD,
            "5536917720166674" to MASTER_CARD,
            "5536914174072839" to MASTER_CARD,
            "2200700166478452" to MIR,
            "2200700161211247" to MIR,
            "5536914173066212" to MASTER_CARD,
            "5536913796810709" to MASTER_CARD,
            "5536913820381214" to MASTER_CARD,
            "2200700141902550" to MIR,
            "4377723741224384" to VISA,
            "5536914175642002" to MASTER_CARD,
            "5536914129278226" to MASTER_CARD,
            "5536914011155144" to MASTER_CARD,
            "2200700131493594" to MIR,
            "4377723772000471" to VISA,
            "6263015600827361" to UNION_PAY,
            "6263015600955329" to UNION_PAY,
            "2200700127017126" to MIR,
            "4377723788520868" to VISA,
            "5536914171108172" to MASTER_CARD,
            "4377723788385296" to VISA,
            "2200700158268044" to MIR,
            "2200700130267361" to MIR,
            "6263015600105925" to UNION_PAY,
            "6263015600443961" to UNION_PAY,
            "6263015600015132" to UNION_PAY,
            "6263015600016577" to UNION_PAY,
            "6759649826438453" to MAESTRO,
            "6331101999990073" to MAESTRO,
            "633478111298873700" to MAESTRO,
            "6799990100000000019" to MAESTRO,
            "6304939304310009610" to MAESTRO,
        )
    }
}