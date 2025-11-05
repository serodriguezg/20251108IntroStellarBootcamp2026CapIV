#![no_std]
use soroban_sdk::{contract, contractimpl, symbol_short, Env, String, Symbol};

// Clave para almacenar el mensaje en el storage del contrato
const MESSAGE: Symbol = symbol_short!("Message");

// TTL (Time To Live) de 10 minutos expresado en ledgers
// En Stellar, cada ledger tarda aproximadamente 5 segundos
// 120 ledgers * 5 segundos = 600 segundos = 10 minutos
const TEN_MINUTES_IN_LEDGERS: u32 = 120;

/// Estructura principal del contrato de mensajes
#[contract]
pub struct MessageContract;

#[contractimpl]
impl MessageContract {
    /// Constructor que se ejecuta automáticamente al desplegar el contrato
    /// 
    /// # Parámetros
    /// - `env`: Entorno de ejecución de Soroban
    /// Este constructor inicializa el contrato con un mensaje predeterminado
    pub fn __constructor(env: Env){
        // Clonamos env porque set_message toma ownership del Env
        // Inicializa el contrato con un mensaje de bienvenida
        Self::set_message(env.clone(), String::from_str(&env, "Mi primer mensaje 🚀"));
    }

    /// Almacena un mensaje en el storage del contrato con un TTL de 10 minutos
    /// 
    /// # Parámetros
    /// - `env`: Entorno de ejecución de Soroban
    /// - `message`: El mensaje a almacenar
    /// 
    /// El mensaje expirará automáticamente después de aproximadamente 10 minutos
    /// debido a la configuración del TTL
    pub fn set_message(env: Env, message: String) {
        // Guarda el mensaje en el instance storage usando la clave MESSAGE
        env.storage().instance().set(&MESSAGE, &message);
        
        // Extiende el TTL (tiempo de vida) del storage
        // Primer parámetro: threshold - cuando quedan estos ledgers, se puede extender
        // Segundo parámetro: extend_to - extiende el TTL hasta este número de ledgers
        env.storage()
            .instance()
            .extend_ttl(TEN_MINUTES_IN_LEDGERS, TEN_MINUTES_IN_LEDGERS);
    }

    /// Recupera el mensaje almacenado en el contrato
    /// 
    /// # Parámetros
    /// - `env`: Entorno de ejecución de Soroban
    /// 
    /// # Retorna
    /// El mensaje almacenado, o "Default Message" si no existe ningún mensaje
    /// o si el TTL ha expirado
    pub fn get_message(env: Env) -> String {
        // Intenta obtener el mensaje del storage
        env.storage()
            .instance()
            .get(&MESSAGE)
            // Si no existe o expiró, retorna un mensaje por defecto
            .unwrap_or(String::from_str(&env, "Default Message"))
    }
}