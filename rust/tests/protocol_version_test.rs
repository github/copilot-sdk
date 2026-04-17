#![allow(clippy::unwrap_used)]

use copilot::Client;
use tokio::io::{duplex, AsyncReadExt, AsyncWrite, AsyncWriteExt};

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

/// Verify protocol version against a fake server that responds with `result`.
async fn verify_with_result(
    result: serde_json::Value,
) -> (Result<(), copilot::Error>, Option<u32>) {
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
        copilot::Error::Protocol(copilot::ProtocolError::VersionMismatch { server: 1, .. })
    ));
    assert_eq!(version, None);
}

#[tokio::test]
async fn succeeds_when_version_missing() {
    let (res, version) = verify_with_result(serde_json::json!({ "message": "pong" })).await;
    assert!(res.is_ok());
    assert_eq!(version, None);
}
