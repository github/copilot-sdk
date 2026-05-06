#![allow(clippy::unwrap_used)]

use github_copilot_sdk::Client;
use tokio::io::{AsyncReadExt, AsyncWrite, AsyncWriteExt, duplex};

async fn write_framed(writer: &mut (impl AsyncWrite + Unpin), body: &[u8]) {
    let header = format!("Content-Length: {}\r\n\r\n", body.len());
    writer.write_all(header.as_bytes()).await.unwrap();
    writer.write_all(body).await.unwrap();
    writer.flush().await.unwrap();
}

async fn read_framed(reader: &mut (impl tokio::io::AsyncRead + Unpin)) -> serde_json::Value {
    let mut header = String::new();
    loop {
        let mut byte = [0u8; 1];
        AsyncReadExt::read_exact(reader, &mut byte).await.unwrap();
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
    AsyncReadExt::read_exact(reader, &mut buf).await.unwrap();
    serde_json::from_slice(&buf).unwrap()
}

/// Verify protocol version against a fake server. Mimics a legacy server
/// that lacks the `connect` JSON-RPC method (-32601 MethodNotFound),
/// forcing the client to fall back to `ping` — the canonical
/// backward-compatibility path documented on `verify_protocol_version`.
async fn verify_with_result(
    result: serde_json::Value,
) -> (Result<(), github_copilot_sdk::Error>, Option<u32>) {
    let (client_write, server_read) = duplex(8192);
    let (server_write, client_read) = duplex(8192);
    let client = Client::from_streams(client_read, client_write, std::env::temp_dir()).unwrap();

    let mut server_read = server_read;
    let mut server_write = server_write;

    let verify_handle = tokio::spawn({
        let client = client.clone();
        async move { client.verify_protocol_version().await }
    });

    // 1. Client sends `connect` first; respond with MethodNotFound so the
    //    client falls back to `ping` (the legacy-server compatibility path).
    let connect_req = read_framed(&mut server_read).await;
    assert_eq!(connect_req["method"], "connect");
    let not_found = serde_json::json!({
        "jsonrpc": "2.0",
        "id": connect_req["id"],
        "error": { "code": -32601, "message": "Method not found" },
    });
    write_framed(&mut server_write, &serde_json::to_vec(&not_found).unwrap()).await;

    // 2. Client falls back to `ping`; respond with the requested result.
    let req = read_framed(&mut server_read).await;
    assert_eq!(req["method"], "ping");
    let response = serde_json::json!({
        "jsonrpc": "2.0",
        "id": req["id"],
        "result": result,
    });
    write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;

    let res = tokio::time::timeout(std::time::Duration::from_secs(2), verify_handle)
        .await
        .unwrap()
        .unwrap();
    let version = client.protocol_version();
    (res, version)
}

#[tokio::test]
async fn accepted_when_version_in_range() {
    let (res, version) = verify_with_result(serde_json::json!({ "protocolVersion": 3 })).await;
    assert!(res.is_ok());
    assert_eq!(version, Some(3));
}

#[tokio::test]
async fn rejected_when_version_out_of_range() {
    let (res, version) = verify_with_result(serde_json::json!({ "protocolVersion": 1 })).await;
    let err = res.unwrap_err();
    assert!(matches!(
        err,
        github_copilot_sdk::Error::Protocol(github_copilot_sdk::ProtocolError::VersionMismatch {
            server: 1,
            ..
        })
    ));
    assert_eq!(version, None);
}

#[tokio::test]
async fn succeeds_when_version_missing() {
    let (res, version) = verify_with_result(serde_json::json!({ "message": "pong" })).await;
    assert!(res.is_ok());
    assert_eq!(version, None);
}

/// New `connect` handshake path: when the server supports `connect` (modern
/// CLIs do), the client uses it directly without falling back to `ping`.
/// Validates the protocolVersion negotiated through the new RPC.
#[tokio::test]
async fn connect_handshake_supplies_protocol_version() {
    let (client_write, server_read) = duplex(8192);
    let (server_write, client_read) = duplex(8192);
    let client = Client::from_streams(client_read, client_write, std::env::temp_dir()).unwrap();

    let mut server_read = server_read;
    let mut server_write = server_write;

    let verify_handle = tokio::spawn({
        let client = client.clone();
        async move { client.verify_protocol_version().await }
    });

    let req = read_framed(&mut server_read).await;
    assert_eq!(req["method"], "connect");
    // Token is None for the from_streams entry point (no transport spawn).
    assert!(req["params"].get("token").is_none());
    let response = serde_json::json!({
        "jsonrpc": "2.0",
        "id": req["id"],
        "result": { "ok": true, "protocolVersion": 3, "version": "test-1.0.0" },
    });
    write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;

    let res = tokio::time::timeout(std::time::Duration::from_secs(2), verify_handle)
        .await
        .unwrap()
        .unwrap();
    assert!(res.is_ok());
    assert_eq!(client.protocol_version(), Some(3));
}

/// Positive coverage for token forwarding on the `connect` handshake. A
/// client constructed with a preset `effective_connection_token` MUST
/// place the exact token string in the outbound `connect` request's
/// `token` param. This is the wire-side hand-off that authenticates
/// the SDK to a CLI server started with `COPILOT_CONNECTION_TOKEN`.
#[tokio::test]
async fn connect_handshake_forwards_explicit_token() {
    let (client_write, server_read) = duplex(8192);
    let (server_write, client_read) = duplex(8192);
    let client = Client::from_streams_with_connection_token(
        client_read,
        client_write,
        std::env::temp_dir(),
        Some("explicit-token-abc".to_string()),
    )
    .unwrap();

    let mut server_read = server_read;
    let mut server_write = server_write;

    let verify_handle = tokio::spawn({
        let client = client.clone();
        async move { client.verify_protocol_version().await }
    });

    let req = read_framed(&mut server_read).await;
    assert_eq!(req["method"], "connect");
    assert_eq!(req["params"]["token"], "explicit-token-abc");

    let response = serde_json::json!({
        "jsonrpc": "2.0",
        "id": req["id"],
        "result": { "ok": true, "protocolVersion": 3, "version": "test-1.0.0" },
    });
    write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;

    tokio::time::timeout(std::time::Duration::from_secs(2), verify_handle)
        .await
        .unwrap()
        .unwrap()
        .unwrap();
}

/// Auto-generated tokens (the codepath that fires when the SDK spawns
/// its own CLI in TCP mode and the consumer didn't supply one) must
/// reach the wire too. Builds a token via the SDK's exposed test helper
/// and verifies the same string lands in the outbound `connect`.
#[tokio::test]
async fn connect_handshake_forwards_auto_generated_token() {
    let token = Client::generate_connection_token_for_test();
    // Sanity-check the generated shape: 32-char lowercase hex (16 bytes,
    // 128 bits of entropy). A regression in the helper would silently
    // weaken loopback authentication.
    assert_eq!(token.len(), 32, "expected 32-char hex, got {token:?}");
    assert!(
        token
            .chars()
            .all(|c| c.is_ascii_hexdigit() && !c.is_uppercase()),
        "expected lowercase hex, got {token:?}",
    );

    let (client_write, server_read) = duplex(8192);
    let (server_write, client_read) = duplex(8192);
    let client = Client::from_streams_with_connection_token(
        client_read,
        client_write,
        std::env::temp_dir(),
        Some(token.clone()),
    )
    .unwrap();

    let mut server_read = server_read;
    let mut server_write = server_write;

    let verify_handle = tokio::spawn({
        let client = client.clone();
        async move { client.verify_protocol_version().await }
    });

    let req = read_framed(&mut server_read).await;
    assert_eq!(req["method"], "connect");
    assert_eq!(req["params"]["token"], token);

    let response = serde_json::json!({
        "jsonrpc": "2.0",
        "id": req["id"],
        "result": { "ok": true, "protocolVersion": 3, "version": "test-1.0.0" },
    });
    write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;

    tokio::time::timeout(std::time::Duration::from_secs(2), verify_handle)
        .await
        .unwrap()
        .unwrap()
        .unwrap();
}
