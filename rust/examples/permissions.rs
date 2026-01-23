//! Example showing permission handling

use github_copilot_sdk::{
    Client, ClientOptions, PermissionInvocation, PermissionRequest, PermissionRequestResult,
    SessionConfig,
};
use std::sync::Arc;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    println!("Permission Handler Example");

    // Create client
    let client = Client::new(ClientOptions::default()).await?;

    // Create session
    let session = client
        .create_session(SessionConfig {
            model: Some("gpt-4o".to_string()),
            ..Default::default()
        })
        .await?;

    // Set custom permission handler
    session
        .set_permission_handler(Arc::new(
            |request: PermissionRequest, _invocation: PermissionInvocation| {
                println!("Permission requested: {:?}", request.kind);

                // Custom permission logic
                match request.kind.as_str() {
                    "file.read" => {
                        // Allow reading files
                        println!("  -> Allowing file read");
                        Ok(PermissionRequestResult {
                            kind: "allow".to_string(),
                            rules: None,
                        })
                    }
                    "file.write" => {
                        // Deny writing files
                        println!("  -> Denying file write");
                        Ok(PermissionRequestResult {
                            kind: "deny".to_string(),
                            rules: None,
                        })
                    }
                    "web.request" => {
                        // Allow web requests
                        println!("  -> Allowing web request");
                        Ok(PermissionRequestResult {
                            kind: "allow".to_string(),
                            rules: None,
                        })
                    }
                    _ => {
                        // Default: allow
                        println!("  -> Default: allowing");
                        Ok(PermissionRequestResult {
                            kind: "allow".to_string(),
                            rules: None,
                        })
                    }
                }
            },
        ))
        .await;

    println!("Permission handler registered!");

    // Send a message that might trigger permission requests
    println!("\nAsking Copilot to check files...");
    let response = session
        .send_and_wait("List the files in the current directory")
        .await?;

    println!("Response: {}", response);

    // Clean up
    client.stop().await?;

    Ok(())
}
