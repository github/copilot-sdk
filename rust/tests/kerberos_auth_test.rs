// Copyright (c) Microsoft Corporation. All rights reserved.

#![allow(clippy::unwrap_used)]

use std::collections::BTreeMap;
use std::time::Duration;

use github_copilot_sdk::kerberos_auth::{
    KerberosAuthAcquireRequest, KerberosAuthConfig, KerberosAuthMode, KerberosAuthScope,
};
use github_copilot_sdk::{CliProgram, Client, ClientOptions, Error, ErrorKind, Result, Transport};
use serde_json::{Value, json};
use tokio::io::{AsyncRead, AsyncReadExt, AsyncWrite, AsyncWriteExt};
use tokio::net::TcpListener;
use tokio::net::tcp::{OwnedReadHalf, OwnedWriteHalf};
use tokio::sync::mpsc;
use tokio::time::timeout;

const WAIT: Duration = Duration::from_secs(5);

async fn write_frame(writer: &mut (impl AsyncWrite + Unpin), value: Value) {
    let body = serde_json::to_vec(&value).unwrap();
    writer
        .write_all(format!("Content-Length: {}\r\n\r\n", body.len()).as_bytes())
        .await
        .unwrap();
    writer.write_all(&body).await.unwrap();
    writer.flush().await.unwrap();
}

async fn read_frame(reader: &mut (impl AsyncRead + Unpin)) -> Value {
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
struct FakeRuntime {
    startup_requests: Vec<Value>,
    reader: OwnedReadHalf,
    writer: OwnedWriteHalf,
}

/// Starts a client against a fake runtime that answers `connect`, then each
/// further startup request with the next `result` or `error` member of `replies`.
async fn start(options: ClientOptions, replies: Vec<Value>) -> (Result<Client>, FakeRuntime) {
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

async fn unavailable(_request: KerberosAuthAcquireRequest) -> Result<Option<String>> {
    Ok(None)
}

async fn acquire(request: KerberosAuthAcquireRequest) -> Result<Option<String>> {
    match request.spn.as_str() {
        "HTTP/unavailable" => Ok(None),
        "HTTP/failing" => Err(Error::with_message(ErrorKind::Io, "lookup failed")),
        spn => Ok(Some(format!("token for {spn}"))),
    }
}

/// Reports `"dropped"` when the provider future owning it is dropped.
struct DropSignal(mpsc::UnboundedSender<&'static str>);

impl Drop for DropSignal {
    fn drop(&mut self) {
        let _ = self.0.send("dropped");
    }
}

/// Starts a client whose provider never answers `HTTP/stalled`, sends it that
/// acquisition as request `id`, and waits until the provider is running.
///
/// The returned receiver reports `"dropped"` when that provider future is dropped.
async fn start_stalled_acquisition(
    id: u64,
) -> (Client, FakeRuntime, mpsc::UnboundedReceiver<&'static str>) {
    let (events_tx, mut events) = mpsc::unbounded_channel();
    let provider = move |request: KerberosAuthAcquireRequest| {
        let events = events_tx.clone();
        async move {
            if request.spn != "HTTP/stalled" {
                return Ok(Some(format!("token for {}", request.spn)));
            }
            let _dropped = DropSignal(events.clone());
            events.send("started").unwrap();
            std::future::pending::<Result<Option<String>>>().await
        }
    };
    let (client, mut runtime) = start(
        ClientOptions::new().with_kerberos_auth(KerberosAuthMode::Always, provider),
        vec![json!({ "result": null })],
    )
    .await;
    write_frame(
        &mut runtime.writer,
        json!({
            "jsonrpc": "2.0", "id": id, "method": "kerberosAuth.acquire",
            "params": { "spn": "HTTP/stalled" }
        }),
    )
    .await;
    assert_eq!(timeout(WAIT, events.recv()).await.unwrap(), Some("started"));
    (client.unwrap(), runtime, events)
}

#[tokio::test]
async fn start_registers_the_configured_mode_and_scope_only_when_set() {
    let mut registrations = Vec::new();
    let mut runtime_scope = ClientOptions::new();
    runtime_scope.kerberos_auth = Some(
        KerberosAuthConfig::new(KerberosAuthMode::Fallback, unavailable)
            .with_scope(KerberosAuthScope::Runtime),
    );
    for options in [
        ClientOptions::new(),
        ClientOptions::new().with_kerberos_auth(KerberosAuthMode::Fallback, unavailable),
        ClientOptions::new().with_kerberos_auth(KerberosAuthMode::Always, unavailable),
        runtime_scope,
    ] {
        let mut replies = Vec::new();
        if options.kerberos_auth.is_some() {
            replies.push(json!({ "result": null }));
        }
        let (client, runtime) = start(options, replies).await;
        client.unwrap().force_stop();
        registrations.push(
            runtime.startup_requests[1..]
                .iter()
                .map(|request| json!({ "method": request["method"], "params": request["params"] }))
                .collect::<Vec<_>>(),
        );
    }
    assert_eq!(
        registrations,
        vec![
            vec![],
            vec![json!({ "method": "kerberosAuth.setProvider", "params": { "mode": "fallback" } })],
            vec![json!({ "method": "kerberosAuth.setProvider", "params": { "mode": "always" } })],
            vec![json!({
                "method": "kerberosAuth.setProvider",
                "params": { "mode": "fallback", "scope": "runtime" }
            })],
        ]
    );
}

#[tokio::test]
async fn failed_registration_fails_start() {
    let (client, _runtime) = start(
        ClientOptions::new().with_kerberos_auth(KerberosAuthMode::Always, unavailable),
        vec![json!({ "error": { "code": -32603, "message": "already registered" } })],
    )
    .await;
    let error = client.unwrap_err();
    assert_eq!(
        (error.rpc_code(), error.to_string()),
        (
            Some(-32603),
            "RPC error -32603: already registered".to_string()
        )
    );
}

#[tokio::test]
async fn acquire_requests_are_answered_by_the_provider() {
    let (client, mut runtime) = start(
        ClientOptions::new().with_kerberos_auth(KerberosAuthMode::Always, acquire),
        vec![json!({ "result": null })],
    )
    .await;
    let client = client.unwrap();
    let spns = [
        "HTTP/proxy.example",
        "ldap/directory.example@EXAMPLE.COM",
        "HTTP/unavailable",
        "HTTP/failing",
    ];
    for (id, spn) in (1000..).zip(spns) {
        write_frame(
            &mut runtime.writer,
            json!({
                "jsonrpc": "2.0", "id": id, "method": "kerberosAuth.acquire",
                "params": { "spn": spn }
            }),
        )
        .await;
    }
    let mut responses = BTreeMap::new();
    for _ in spns {
        let response = read_frame(&mut runtime.reader).await;
        responses.insert(response["id"].as_u64().unwrap(), response);
    }
    client.force_stop();
    assert_eq!(
        responses.into_values().collect::<Vec<_>>(),
        vec![
            json!({
                "jsonrpc": "2.0", "id": 1000,
                "result": { "token": "token for HTTP/proxy.example" }
            }),
            json!({
                "jsonrpc": "2.0", "id": 1001,
                "result": { "token": "token for ldap/directory.example@EXAMPLE.COM" }
            }),
            json!({ "jsonrpc": "2.0", "id": 1002, "result": {} }),
            json!({
                "jsonrpc": "2.0", "id": 1003,
                "error": { "code": -32603, "message": "lookup failed" }
            }),
        ]
    );
}

#[tokio::test]
async fn cancelled_acquisition_drops_the_provider_future() {
    let (client, mut runtime, mut events) = start_stalled_acquisition(1000).await;
    write_frame(
        &mut runtime.writer,
        json!({ "jsonrpc": "2.0", "method": "$/cancelRequest", "params": { "id": 1000 } }),
    )
    .await;
    let dropped = timeout(WAIT, events.recv()).await.unwrap();
    let cancelled = read_frame(&mut runtime.reader).await;
    // The next frame answers a later acquisition, not the cancelled one again.
    write_frame(
        &mut runtime.writer,
        json!({
            "jsonrpc": "2.0", "id": 1001, "method": "kerberosAuth.acquire",
            "params": { "spn": "HTTP/proxy.example" }
        }),
    )
    .await;
    let answered = read_frame(&mut runtime.reader).await;
    client.force_stop();
    assert_eq!(
        (dropped, cancelled, answered),
        (
            Some("dropped"),
            json!({
                "jsonrpc": "2.0", "id": 1000,
                "error": { "code": -32800, "message": "Request cancelled" }
            }),
            json!({
                "jsonrpc": "2.0", "id": 1001,
                "result": { "token": "token for HTTP/proxy.example" }
            }),
        )
    );
}

#[tokio::test]
async fn closed_connection_drops_a_pending_acquisition() {
    let (client, runtime, mut events) = start_stalled_acquisition(1000).await;
    drop(runtime);
    let dropped = timeout(WAIT, events.recv()).await.unwrap();
    client.force_stop();
    assert_eq!(dropped, Some("dropped"));
}
