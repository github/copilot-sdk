#![cfg(feature = "test-support")]
#![allow(clippy::unwrap_used)]

use copilot::test_support::{JsonRpcClient, JsonRpcNotification, JsonRpcRequest};
use tokio::io::{AsyncWrite, AsyncWriteExt, duplex};
use tokio::sync::{broadcast, mpsc};

/// Write a Content-Length framed JSON-RPC message to a writer.
async fn write_framed(writer: &mut (impl AsyncWrite + Unpin), body: &[u8]) {
    let header = format!("Content-Length: {}\r\n\r\n", body.len());
    writer.write_all(header.as_bytes()).await.unwrap();
    writer.write_all(body).await.unwrap();
    writer.flush().await.unwrap();
}

#[tokio::test]
async fn request_response_round_trip() {
    // duplex: client_write → server_read, server_write → client_read
    let (client_write, mut server_read) = duplex(4096);
    let (mut server_write, client_read) = duplex(4096);

    let (notification_tx, _) = broadcast::channel(16);
    let (_request_tx, _request_rx) = mpsc::unbounded_channel();
    let request_tx = _request_tx;

    let client = JsonRpcClient::new(client_write, client_read, notification_tx, request_tx);

    // Spawn a task that reads the request from the server side and sends a response.
    let server_handle = tokio::spawn(async move {
        let mut buf = Vec::new();
        // Read the Content-Length header
        let mut header = String::new();
        loop {
            let mut byte = [0u8; 1];
            tokio::io::AsyncReadExt::read_exact(&mut server_read, &mut byte)
                .await
                .unwrap();
            header.push(byte[0] as char);
            if header.ends_with("\r\n\r\n") {
                break;
            }
        }
        let length: usize = header
            .trim()
            .strip_prefix("Content-Length: ")
            .unwrap()
            .parse()
            .unwrap();
        buf.resize(length, 0);
        tokio::io::AsyncReadExt::read_exact(&mut server_read, &mut buf)
            .await
            .unwrap();

        let request: JsonRpcRequest = serde_json::from_slice(&buf).unwrap();
        assert_eq!(request.method, "test.echo");
        assert_eq!(request.jsonrpc, "2.0");

        // Send response
        let response = serde_json::json!({
            "jsonrpc": "2.0",
            "id": request.id,
            "result": { "echoed": true }
        });
        write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;

        request.id
    });

    let response = client
        .send_request("test.echo", Some(serde_json::json!({"hello": "world"})))
        .await
        .unwrap();

    let request_id = server_handle.await.unwrap();
    assert_eq!(response.id, request_id);
    assert!(!response.is_error());
    assert_eq!(response.result.unwrap()["echoed"], serde_json::json!(true));
}

#[tokio::test]
async fn notification_broadcasting() {
    let (_client_write, _discard) = duplex(4096);
    let (mut server_write, client_read) = duplex(4096);

    let (notification_tx, mut notification_rx) = broadcast::channel(16);
    let (request_tx, _request_rx) = mpsc::unbounded_channel();

    let _client = JsonRpcClient::new(_client_write, client_read, notification_tx, request_tx);

    // Server sends a notification (no id field).
    let notification = serde_json::json!({
        "jsonrpc": "2.0",
        "method": "session.event",
        "params": { "session_id": "s1", "event": "started" }
    });
    write_framed(
        &mut server_write,
        &serde_json::to_vec(&notification).unwrap(),
    )
    .await;

    let received: JsonRpcNotification =
        tokio::time::timeout(std::time::Duration::from_secs(2), notification_rx.recv())
            .await
            .expect("timed out waiting for notification")
            .unwrap();

    assert_eq!(received.method, "session.event");
    assert_eq!(received.params.unwrap()["session_id"], "s1");
}

#[tokio::test]
async fn server_request_forwarding() {
    let (_client_write, _discard) = duplex(4096);
    let (mut server_write, client_read) = duplex(4096);

    let (notification_tx, _) = broadcast::channel(16);
    let (request_tx, mut request_rx) = mpsc::unbounded_channel();

    let _client = JsonRpcClient::new(_client_write, client_read, notification_tx, request_tx);

    // Server sends a request (has both id and method).
    let request = serde_json::json!({
        "jsonrpc": "2.0",
        "id": 42,
        "method": "permission.request",
        "params": { "kind": "shell" }
    });
    write_framed(&mut server_write, &serde_json::to_vec(&request).unwrap()).await;

    let received: JsonRpcRequest =
        tokio::time::timeout(std::time::Duration::from_secs(2), request_rx.recv())
            .await
            .expect("timed out waiting for request")
            .unwrap();

    assert_eq!(received.method, "permission.request");
    assert_eq!(received.id, 42);
}

#[tokio::test]
async fn error_response_round_trip() {
    let (client_write, mut server_read) = duplex(4096);
    let (mut server_write, client_read) = duplex(4096);

    let (notification_tx, _) = broadcast::channel(16);
    let (request_tx, _) = mpsc::unbounded_channel();

    let client = JsonRpcClient::new(client_write, client_read, notification_tx, request_tx);

    let server_handle = tokio::spawn(async move {
        // Read request
        let mut header = String::new();
        loop {
            let mut byte = [0u8; 1];
            tokio::io::AsyncReadExt::read_exact(&mut server_read, &mut byte)
                .await
                .unwrap();
            header.push(byte[0] as char);
            if header.ends_with("\r\n\r\n") {
                break;
            }
        }
        let length: usize = header
            .trim()
            .strip_prefix("Content-Length: ")
            .unwrap()
            .parse()
            .unwrap();
        let mut buf = vec![0u8; length];
        tokio::io::AsyncReadExt::read_exact(&mut server_read, &mut buf)
            .await
            .unwrap();
        let request: JsonRpcRequest = serde_json::from_slice(&buf).unwrap();

        // Send error response
        let error_response = serde_json::json!({
            "jsonrpc": "2.0",
            "id": request.id,
            "error": { "code": -32600, "message": "Invalid Request" }
        });
        write_framed(
            &mut server_write,
            &serde_json::to_vec(&error_response).unwrap(),
        )
        .await;
    });

    let response = client.send_request("bad.method", None).await.unwrap();
    server_handle.await.unwrap();

    assert!(response.is_error());
    let error = response.error.unwrap();
    assert_eq!(error.code, -32600);
    assert_eq!(error.message, "Invalid Request");
}

#[tokio::test]
async fn read_loop_terminates_on_eof() {
    let (client_write, _discard) = duplex(4096);
    let (server_write, client_read) = duplex(4096);

    let (notification_tx, _) = broadcast::channel(16);
    let (request_tx, _) = mpsc::unbounded_channel();

    let _client = JsonRpcClient::new(client_write, client_read, notification_tx, request_tx);

    // Drop the server side — the read loop should see EOF and stop.
    drop(server_write);

    // Give the read loop time to notice EOF.
    tokio::time::sleep(std::time::Duration::from_millis(100)).await;
}
