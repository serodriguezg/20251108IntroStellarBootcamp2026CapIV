import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.request.*
import kotlinx.serialization.*

import org.stellar.sdk.*
import org.stellar.sdk.responses.SubmitTransactionResponse

// La URL de Horizon para la red de prueba (Testnet)
val HORIZON_SERVER = Server("https://horizon-testnet.stellar.org")
val STELLAR_NETWORK = Network.useTestNetwork()

// NOTA: En un proyecto real, la clave secreta vendría de una fuente segura.
// USANDO UNA CLAVE DE PRUEBA:
val SOURCE_SECRET_SEED = "SBFN..." // Reemplazar con una clave real y segura
val SOURCE_KEYPAIR = KeyPair.fromSecretSeed(SOURCE_SECRET_SEED.toCharArray())


// Modelo de datos para la solicitud del frontend
@Serializable
data class StellarTransferRequest(
    val destinationAccountId: String,
    val amount: String
)

fun main() {
    embeddedServer(Netty, port = 8080) {
        // Plugin para manejar JSON
        install(ContentNegotiation) {
            json()
        }
        
        routing {
            post("/api/send") {
                val request = call.receive<StellarTransferRequest>()

                try {
                    val response = submitTransaction(
                        request.destinationAccountId, 
                        request.amount
                    )

                    if (response.isSuccess) {
                        call.respond(mapOf("status" to "success", "hash" to response.hash))
                    } else {
                        // Manejo de errores de Stellar más detallado
                        val resultCode = response.extras.resultCodes.transactionResultCode
                        call.respondText("Fallo en la transacción: $resultCode", status = io.ktor.http.HttpStatusCode.BadRequest)
                    }
                } catch (e: Exception) {
                    call.respondText("Error interno del servidor: ${e.message}", status = io.ktor.http.HttpStatusCode.InternalServerError)
                }
            }
        }
    }.start(wait = true)
}

// Lógica de transacción (similar a la anterior)
suspend fun submitTransaction(destinationAccountId: String, amount: String): SubmitTransactionResponse {
    // 1. Cargar la cuenta de origen
    val account = HORIZON_SERVER.accounts().account(SOURCE_KEYPAIR.accountId)

    // 2. Crear la operación de pago (en XLM nativo)
    val paymentOperation = PaymentOperation.Builder(
        destinationAccountId,
        AssetTypeNative(),
        amount
    ).build()

    // 3. Construir, firmar y enviar la transacción
    val transaction = Transaction.Builder(account, Network.getCurrent())
        .addOperation(paymentOperation)
        .setTimeout(30)
        .setBaseFee(Transaction.MIN_BASE_FEE)
        .build()

    transaction.sign(SOURCE_KEYPAIR)

    return HORIZON_SERVER.submitTransaction(transaction)
}