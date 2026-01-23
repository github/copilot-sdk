//! Tests for JSON-RPC implementation

use github_copilot_sdk::jsonrpc::JsonRpcClient;
use std::collections::HashMap;

#[tokio::test]
async fn test_jsonrpc_request_response() {
    // Create in-memory duplex streams for testing
    let (client_stream, server_stream) = tokio::io::duplex(1024);
    let (server_read, server_write) = tokio::io::split(server_stream);
    let (client_read, client_write) = tokio::io::split(client_stream);

    // Create client
    let client = JsonRpcClient::new(client_read, client_write);

    // Spawn a simple server that echoes requests
    tokio::spawn(async move {
        let server_client = JsonRpcClient::new(server_read, server_write);

        // Register a simple handler that echoes the input
        server_client
            .register_request_handler("echo".to_string(), std::sync::Arc::new(Ok))
            .await;

        // Keep server alive
        tokio::time::sleep(tokio::time::Duration::from_secs(2)).await;
    });

    // Give server time to start
    tokio::time::sleep(tokio::time::Duration::from_millis(100)).await;

    // Send request
    let mut params = HashMap::new();
    params.insert("message".to_string(), serde_json::json!("hello"));

    let result = client
        .request("echo".to_string(), params.clone())
        .await
        .unwrap();

    assert_eq!(result.get("message").unwrap(), &serde_json::json!("hello"));
}

#[tokio::test]
async fn test_jsonrpc_notification() {
    let (client_stream, server_stream) = tokio::io::duplex(1024);
    let (server_read, server_write) = tokio::io::split(server_stream);
    let (client_read, client_write) = tokio::io::split(client_stream);

    let client = JsonRpcClient::new(client_read, client_write);

    // Spawn server
    tokio::spawn(async move {
        let _server_client = JsonRpcClient::new(server_read, server_write);
        tokio::time::sleep(tokio::time::Duration::from_secs(1)).await;
    });

    tokio::time::sleep(tokio::time::Duration::from_millis(100)).await;

    // Send notification (should not error)
    let mut params = HashMap::new();
    params.insert("event".to_string(), serde_json::json!("test"));

    let result = client.notify("test_event".to_string(), params).await;
    assert!(result.is_ok());
}

#[test]
fn test_jsonrpc_error_serialization() {
    use github_copilot_sdk::jsonrpc::JsonRpcError;

    let error = JsonRpcError {
        code: -32600,
        message: "Invalid Request".to_string(),
        data: None,
    };

    let json = serde_json::to_string(&error).unwrap();
    let deserialized: JsonRpcError = serde_json::from_str(&json).unwrap();

    assert_eq!(deserialized.code, -32600);
    assert_eq!(deserialized.message, "Invalid Request");
}
