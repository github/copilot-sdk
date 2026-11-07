// Copyright (c) Microsoft Corporation. All rights reserved.

//! A fake runtime on the other end of an external transport, for tests of
//! connection-level callbacks registered during `Client::start`.

use std::time::Duration;

use github_copilot_sdk::{CliProgram, Client, ClientOptions, Result, Transport};
use serde_json::{Value, json};
use tokio::io::{AsyncRead, AsyncReadExt, AsyncWrite, AsyncWriteExt};
use tokio::net::TcpListener;
use tokio::net::tcp::{OwnedReadHalf, OwnedWriteHalf};
use tokio::sync::mpsc;
use tokio::time::timeout;

pub const WAIT: Duration = Duration::from_secs(5);

pub async fn write_frame(writer: &mut (impl AsyncWrite + Unpin), value: Value) {
    let body = serde_json::to_vec(&value).unwrap();
    writer
        .write_all(format!("Content-Length: {}\r\n\r\n", body.len()).as_bytes())
        .await
        .unwrap();
    writer.write_all(&body).await.unwrap();
    writer.flush().await.unwrap();
}

pub async fn read_frame(reader: &mut (impl AsyncRead + Unpin)) -> Value {
    timeout(WAIT, async {
        let mut header = Vec::new();
        while !header.ends_with(b"\r\n\r\n") {
            header.push(reader.read_u8().await.unwrap());
        }
        let length = String::from_utf8(header)
            .unwrap()
            .trim()
            .strip_prefix("Content-Length: ")
            .unwrap()
            .parse()
            .unwrap();
        let mut body = vec![0; length];
        reader.read_exact(&mut body).await.unwrap();
        serde_json::from_slice(&body).unwrap()
    })
    .await
    .unwrap()
}

/// The runtime side of a connection after its startup requests were answered.
pub struct FakeRuntime {
    pub startup_requests: Vec<Value>,
    pub reader: OwnedReadHalf,
    pub writer: OwnedWriteHalf,
}

impl FakeRuntime {
    /// The method and params of each startup request after `connect`.
    pub fn registrations(&self) -> Vec<Value> {
        self.startup_requests[1..]
            .iter()
            .map(|request| json!({ "method": request["method"], "params": request["params"] }))
            .collect()
    }
}

/// Starts a client against a fake runtime that answers `connect`, then each
/// further startup request with the next `result` or `error` member of `replies`.
pub async fn start(options: ClientOptions, replies: Vec<Value>) -> (Result<Client>, FakeRuntime) {
    let listener = TcpListener::bind(("127.0.0.1", 0)).await.unwrap();
    let port = listener.local_addr().unwrap().port();
    let runtime = tokio::spawn(async move {
        let (stream, _) = listener.accept().await.unwrap();
        let (mut reader, mut writer) = stream.into_split();
        let connect = read_frame(&mut reader).await;
        write_frame(
            &mut writer,
            json!({
                "jsonrpc": "2.0", "id": connect["id"],
                "result": { "ok": true, "protocolVersion": 3, "version": "test" }
            }),
        )
        .await;
        let mut startup_requests = vec![connect];
        for reply in replies {
            let request = read_frame(&mut reader).await;
            let mut response = json!({ "jsonrpc": "2.0", "id": request["id"] });
            response
                .as_object_mut()
                .unwrap()
                .extend(reply.as_object().unwrap().clone());
            write_frame(&mut writer, response).await;
            startup_requests.push(request);
        }
        FakeRuntime {
            startup_requests,
            reader,
            writer,
        }
    });
    let options = options
        .with_program(CliProgram::Path("unused-external-transport".into()))
        .with_transport(Transport::External {
            host: "127.0.0.1".to_string(),
            port,
            connection_token: None,
        });
    let client = timeout(WAIT, Client::start(options)).await.unwrap();
    let runtime = timeout(WAIT, runtime).await.unwrap().unwrap();
    (client, runtime)
}

/// Reports `"dropped"` when the provider future owning it is dropped.
pub struct DropSignal(pub mpsc::UnboundedSender<&'static str>);

impl Drop for DropSignal {
    fn drop(&mut self) {
        let _ = self.0.send("dropped");
    }
}
