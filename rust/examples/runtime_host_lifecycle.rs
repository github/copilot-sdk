//! Run with `--no-default-features --features local-runtime` and a prepared
//! same-checkout `COPILOT_CLI_PATH`. This example disables telemetry uploads.

use github_copilot_sdk::{Client, ClientOptions, GetStatusResponse, Result, Transport};

async fn demonstrate(client: &Client) -> Result<()> {
    let pending = client.begin_call("status.get", None)?;
    println!("Status request queued before awaiting its response");
    let status: GetStatusResponse = serde_json::from_value(pending.await?)?;
    println!("Runtime version: {}", status.version);
    println!("Protocol version: {}", status.protocol_version);
    Ok(())
}

#[tokio::main]
async fn main() -> std::result::Result<(), Box<dyn std::error::Error>> {
    let home = tempfile::tempdir()?;
    let options = ClientOptions::new()
        .with_transport(Transport::Stdio)
        .with_base_directory(home.path())
        .with_github_token("")
        .with_use_logged_in_user(false)
        .with_env([
            ("COPILOT_GITHUB_TELEMETRY_DISABLED", "true"),
            ("COPILOT_TELEMETRY_DISABLED", "true"),
            ("DO_NOT_TRACK", "1"),
        ]);
    let client = Client::start(options).await?;
    println!("Runtime started through the SDK");
    let result = demonstrate(&client).await;
    let stopped = client.stop().await;
    if let Err(error) = result {
        if let Err(shutdown_error) = stopped {
            eprintln!("Shutdown also failed: {shutdown_error}");
        }
        return Err(error.into());
    }
    stopped?;
    println!("Runtime shutdown complete");
    Ok(())
}
