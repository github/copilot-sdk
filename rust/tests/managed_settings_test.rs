#![allow(clippy::unwrap_used)]

use github_copilot_sdk::Client;
use tokio::io::{AsyncRead, AsyncReadExt, AsyncWrite, AsyncWriteExt, duplex};

async fn write_framed(writer: &mut (impl AsyncWrite + Unpin), body: &[u8]) {
    let header = format!("Content-Length: {}\r\n\r\n", body.len());
    writer.write_all(header.as_bytes()).await.unwrap();
    writer.write_all(body).await.unwrap();
    writer.flush().await.unwrap();
}

async fn read_framed(reader: &mut (impl AsyncRead + Unpin)) -> serde_json::Value {
    let mut header = String::new();
    loop {
        let mut byte = [0u8; 1];
        reader.read_exact(&mut byte).await.unwrap();
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
    reader.read_exact(&mut buf).await.unwrap();
    serde_json::from_slice(&buf).unwrap()
}

#[tokio::test]
async fn read_managed_settings_calls_server_scoped_method_before_session_create() {
    let tempdir = tempfile::tempdir().unwrap();
    let (client_write, mut server_read) = duplex(8192);
    let (mut server_write, client_read) = duplex(8192);
    let client = Client::from_streams(client_read, client_write, tempdir.path().to_path_buf())
        .expect("create client");

    let read_handle = tokio::spawn({
        let client = client.clone();
        async move { client.read_managed_settings().await }
    });

    let request = read_framed(&mut server_read).await;
    assert_eq!(request["method"], "managedSettings.read");
    assert_eq!(request["params"], serde_json::json!({}));

    let payload = serde_json::json!({
        "source": "device",
        "canonical": true,
        "policies": {
            "bypassPermissions": ["shell.read"]
        }
    });
    let response = serde_json::json!({
        "jsonrpc": "2.0",
        "id": request["id"],
        "result": payload,
    });
    write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;

    let result = tokio::time::timeout(std::time::Duration::from_secs(2), read_handle)
        .await
        .unwrap()
        .unwrap()
        .expect("read managed settings");
    assert_eq!(
        result,
        serde_json::json!({
            "source": "device",
            "canonical": true,
            "policies": {
                "bypassPermissions": ["shell.read"]
            }
        })
    );
}
