// main.rs — Cliente limpio y compatible para un contrato de Soroban

use std::env;
use std::time::Duration;

use anyhow::Result;
use soroban_sdk::xdr::{
    HostFunction, InvokeContractArgs, ScAddress, ScSymbol, ScVal, Hash,
};
use soroban_rpc::{Client as RpcClient, Network, TransactionStatus};
use soroban_sdk::{
    transaction::TransactionBuilder, Keypair, Network as SdkNetwork,
};
use stellar_strkey::Contract;

// ===================== CONFIGURACIÓN =====================
const RPC_URL: &str = "https://soroban-testnet.org";
const CONTRACT_ID: &str = "CAJN25XAZLTZEVS7ZFLNZ3HWREJRQHKUU265CK67ED2ASJ22TDQ5Y4PL";
const BASE_FEE: u32 = 100;
// ======================================================

/// Convierte un ScVal a un String de Rust.
fn scval_to_string(val: &ScVal) -> Option<String> {
    match val {
        ScVal::ScvString(s) => String::from_utf8(s.0.clone()).ok(),
        ScVal::ScvSymbol(sym) => String::from_utf8(sym.0.clone()).ok(),
        _ => None,
    }
}

/// Convierte un String de Rust a un ScVal.
fn to_scval_string(s: &str) -> ScVal {
    ScVal::ScvString(s.into())
}

/// Lee el mensaje del contrato simulando la llamada.
async fn get_message(rpc: &RpcClient, kp: &Keypair) -> Result<String> {
    println!("\n🔍 get_message() (desde Rust)");
    let account = rpc.load_account(&kp.public_key()).await?;

    let contract = Contract::from_string(CONTRACT_ID)?;
    let contract_address = ScAddress::Contract(Hash(contract.0));

    let host_fn = HostFunction::InvokeContract(InvokeContractArgs {
        contract_address,
        function_name: ScSymbol::try_from("get_message")?,
        args: vec![],
    });

    let op = soroban_sdk::operation::InvokeHostFunction::new(host_fn);
    let tx = TransactionBuilder::new()
        .with_source_account(account.account_id())
        .with_base_fee(BASE_FEE)
        .with_operation(op)
        .with_timeout(30)
        .build()?;

    let sim = rpc.simulate_transaction(&tx).await?;
    let retval = sim.result.and_then(|r| r.retval).ok_or("Simulación sin retorno")?;
    let message = scval_to_string(&retval).unwrap_or_default();

    println!("📨 Mensaje actual: {}", message);
    Ok(message)
}

/// Escribe un nuevo mensaje en el contrato.
async fn set_message(rpc: &RpcClient, kp: &Keypair, new_msg: &str) -> Result<()> {
    println!("\n✏️ set_message(\"{}\") (desde Rust)", new_msg);
    let account = rpc.load_account(&kp.public_key()).await?;

    let contract = Contract::from_string(CONTRACT_ID)?;
    let contract_address = ScAddress::Contract(Hash(contract.0));

    let host_fn = HostFunction::InvokeContract(InvokeContractArgs {
        contract_address,
        function_name: ScSymbol::try_from("set_message")?,
        args: vec![to_scval_string(new_msg)],
    });

    let op = soroban_sdk::operation::InvokeHostFunction::new(host_fn);
    let mut tx = TransactionBuilder::new()
        .with_source_account(account.account_id())
        .with_base_fee(BASE_FEE)
        .with_operation(op)
        .with_timeout(60)
        .build()?;

    tx = rpc.prepare_transaction(&tx).await?;
    tx.sign(kp, &SdkNetwork::testnet());

    let sent = rpc.send_transaction(&tx).await?;
    if let Some(err) = sent.error_result_xdr {
        anyhow::bail!("Rechazada al enviar: {}", err);
    }

    let short = &sent.hash[..8];
    println!("⏳ Esperando confirmación de la transacción {}...", short);

    loop {
        let res = rpc.get_transaction(&sent.hash).await?;
        match res.status {
            TransactionStatus::Success => {
                println!("✅ Transacción confirmada con éxito.");
                break;
            }
            TransactionStatus::Failed => {
                anyhow::bail!("Falló on-chain: {:?}", res);
            }
            _ => tokio::time::sleep(Duration::from_millis(1200)).await,
        }
    }
    Ok(())
}

#[tokio::main]
async fn main() -> Result<()> {
    println!("--- Iniciando Demo del Contrato de Mensajes ---");

    let secret = env::var("USER_SECRET").unwrap_or_else(|_| {
        "SDQK5C2WQ67VM4HQ3S3JAQ4XIJED7SJVTGKMDAVS7R4YCT7NJ34TLLKJ".to_string()
    });
    let kp = Keypair::from_secret(&secret)?;
    let rpc = RpcClient::new(RPC_URL, Network::Testnet);

    get_message(&rpc, &kp).await?;
    set_message(&rpc, &kp, "Hola desde Rust 🦀").await?;
    get_message(&rpc, &kp).await?;

    println!("\n--- Demo Finalizada ---");
    Ok(())
}