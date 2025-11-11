// src/main/kotlin/org/example/app/App.kt
package org.example.app

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

// Importamos los tipos reformados de StellarService
import org.example.app.StellarService
import org.example.app.Task
import org.example.app.TaskStatus

// --- PARÁMETROS DE INICIO Y ESTADO ---
const val DEFAULT_NETWORK_URL = "https://soroban-testnet.stellar.org:443"

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Soroban To-Do List DApp (REAL)") {
        MaterialTheme {
            val initialService = remember { StellarService(DEFAULT_NETWORK_URL, "CA...") }
            ToDoListAppLayout(initialService)
        }
    }
}

// --- ESTRUCTURA DE DATOS para el estado de la transferencia ---
data class TransferData(
    val taskId: UInt, 
    val newOwner: String, 
    val fee: ULong
)

// --- Composable Principal ---

@Composable
@Preview
fun ToDoListAppLayout(initialService: StellarService) {
    
    // --- CONFIGURACIÓN DE RED Y LLAVES ---
    var networkUrl by remember { mutableStateOf(DEFAULT_NETWORK_URL) }
    var contractId by remember { mutableStateOf("CA...") } 
    var secretKey by remember { mutableStateOf("") } 
    var connectedAddress by remember { mutableStateOf("") } 
    
    val stellarService by remember(networkUrl, contractId) { 
        mutableStateOf(StellarService(networkUrl, contractId)) 
    }
    
    val tasks: SnapshotStateList<Task> = remember { mutableStateListOf() }
    
    // --- ESTADO PARA EL DIÁLOGO DE CONFIRMACIÓN (NUEVO) ---
    var transferParams: TransferData? by remember { mutableStateOf(null) } // Si no es null, muestra el diálogo

    // Lógica de conexión y refresco (código sin cambios)
    val connectWallet: () -> Unit = { /* ... */ }
    val refreshTasks: () -> Unit = { /* ... */ }

    LaunchedEffect(connectedAddress) { refreshTasks() }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        
        // ... (ConnectionPanel y TaskCreationPanel) ...
        ConnectionPanel(networkUrl, { networkUrl = it }, contractId, { contractId = it }, secretKey, { secretKey = it }, connectWallet, connectedAddress)
        Spacer(Modifier.height(24.dp))
        
        if (connectedAddress.startsWith("G")) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                
                TaskCreationPanel(modifier = Modifier.weight(0.7f).fillMaxHeight(), ownerAddress = connectedAddress, 
                    onAddTask = { desc -> if (stellarService.addTask(secretKey, connectedAddress, desc)) refreshTasks() })
                
                Spacer(Modifier.width(20.dp))

                Column(modifier = Modifier.weight(1.3f).fillMaxHeight()) {
                    TaskListPanel(tasks = tasks, connectedAddress = connectedAddress, stellarService = stellarService, secretKey = secretKey, onRefreshTasks = refreshTasks, modifier = Modifier.weight(1f))
                    Spacer(Modifier.height(16.dp))

                    AdvancedControlsPanel(
                        modifier = Modifier.fillMaxWidth(),
                        // Cuando se solicita la transferencia, guardamos los datos en transferParams para mostrar el diálogo
                        onTransferRequested = { taskId, newOwner, fee ->
                            transferParams = TransferData(taskId, newOwner, fee)
                        }
                    )
                }
            }
        } else {
            Text("Por favor, introduce la clave secreta, ID del contrato y conéctate.", style = MaterialTheme.typography.h6)
        }
    }
    
    // --- GESTIÓN DEL DIÁLOGO DE CONFIRMACIÓN DE LA TRANSFERENCIA ---
    if (transferParams != null) {
        TransferConfirmationDialog(
            data = transferParams!!,
            callerAddress = connectedAddress,
            onDismiss = { transferParams = null }, // Ocultar si cancela
            onConfirm = { data ->
                // Llama al servicio al confirmar la firma
                stellarService.transferOwnership(
                    secretKey = secretKey, 
                    taskId = data.taskId, 
                    callerAddress = connectedAddress, 
                    newOwner = data.newOwner, 
                    fee = data.fee
                )
                transferParams = null // Cerrar el diálogo después de enviar
            }
        )
    }
}

// --- Composable: TransferConfirmationDialog (NUEVO) ---
@Composable
fun TransferConfirmationDialog(
    data: TransferData,
    callerAddress: String,
    onDismiss: () -> Unit,
    onConfirm: (TransferData) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🔒 Confirmar Transferencia de Propiedad") },
        text = {
            Column {
                Text(
                    "¿Desea firmar y enviar esta transacción a la red Stellar?", 
                    fontWeight = FontWeight.Bold
                )
                Divider(Modifier.padding(vertical = 8.dp))
                
                // Detalle de la Transacción
                RowItem(label = "Acción a Realizar", value = "transfer_ownership")
                RowItem(label = "ID de la Tarea", value = data.taskId.toString())
                RowItem(label = "Propietario Actual", value = callerAddress.take(8) + "...")
                RowItem(label = "Nuevo Propietario", value = data.newOwner.take(8) + "...", color = Color.Blue)
                RowItem(label = "Tarifa Estimada (Fee)", value = "${data.fee} Stroops", color = Color.Red)
                
                Spacer(Modifier.height(10.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = "Advertencia", tint = Color.Yellow.copy(alpha = 0.8f))
                    Spacer(Modifier.width(8.dp))
                    Text("Al firmar, autoriza el gasto del Fee.", style = MaterialTheme.typography.caption)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(data) }) {
                Text("Firmar y Enviar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// Helper para el diálogo
@Composable
fun RowItem(label: String, value: String, color: Color = Color.Unspecified) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("$label:", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
        Text(value, modifier = Modifier.weight(2f), color = color)
    }
}


// --- Composable: AdvancedControlsPanel (REFORMADO para pedir confirmación) ---

@Composable
fun AdvancedControlsPanel(modifier: Modifier, onTransferRequested: (UInt, String, ULong) -> Unit) {
    // onTransferRequested es el nuevo callback que inicia el diálogo
    
    var taskId by remember { mutableStateOf("") }
    var newOwner by remember { mutableStateOf("") }
    var feeInput by remember { mutableStateOf("100") } 

    Card(modifier = modifier, elevation = 4.dp) {
        Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
            Text("⚙️ Transferir Propiedad", style = MaterialTheme.typography.h6)
            Spacer(Modifier.height(12.dp))
            
            // Fila 1: ID de Tarea y Nuevo Propietario
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = taskId, onValueChange = { taskId = it.filter { it.isDigit() } }, 
                    label = { Text("ID de Tarea") },
                    modifier = Modifier.weight(0.5f).padding(end = 8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = newOwner, onValueChange = { newOwner = it }, 
                    label = { Text("Nueva Dirección (G...)") },
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    singleLine = true
                )
            }
            Spacer(Modifier.height(8.dp))
            
            // Fila 2: Fee y Botón de Transferencia
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = feeInput, onValueChange = { feeInput = it.filter { it.isDigit() } }, 
                    label = { Text("Tarifa (Stroops)") },
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                val id = taskId.toUIntOrNull()
                val fee = feeInput.toULongOrNull() 
                
                Button(
                    // Al hacer clic, llamamos a onTransferRequested para mostrar el diálogo
                    onClick = { 
                        if (id != null && fee != null) {
                            onTransferRequested(id, newOwner, fee) 
                        }
                    },
                    // Habilitar si todos los campos son válidos
                    enabled = id != null && newOwner.startsWith("G") && newOwner.length > 10 && fee != null,
                    modifier = Modifier.weight(0.5f).height(56.dp) 
                ) {
                    Text("Solicitar Firma")
                }
            }
        }
    }
}

// El resto de los composables (ConnectionPanel, TaskCreationPanel, TaskListPanel, TaskItem, EditDescriptionDialog)
// se mantienen igual a la versión anterior o fueron adaptados ligeramente para usar los nuevos tipos UInt/ULong.