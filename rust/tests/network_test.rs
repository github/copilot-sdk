// Copyright (c) Microsoft Corporation. All rights reserved.

#![allow(clippy::unwrap_used)]

#[path = "support/fake_runtime.rs"]
mod fake_runtime;

use std::collections::BTreeMap;
use std::time::Duration;

use fake_runtime::{DropSignal, FakeRuntime, WAIT, read_frame, start, write_frame};
use github_copilot_sdk::network::{
    NetworkAuthenticateNegotiateRequest, NetworkConfig, NetworkResolveProxyRequest, NetworkScope,
};
use github_copilot_sdk::{
    CliProgram, Client, ClientOptions, Error, ErrorKind, Result, SessionConfig, Transport,
};
use serde_json::{Value, json};
use tokio::io::AsyncReadExt;
use tokio::sync::{mpsc, oneshot};
use tokio::time::timeout;

async fn direct(_request: NetworkResolveProxyRequest) -> Result<Option<String>> {
    Ok(None)
}

async fn unavailable(_request: NetworkAuthenticateNegotiateRequest) -> Result<Option<String>> {
    Ok(None)
}

async fn resolve_proxy(request: NetworkResolveProxyRequest) -> Result<Option<String>> {
    match request.url.as_str() {
        "https://api.example.com/" => Ok(Some("PROXY proxy.example:8080; DIRECT".to_string())),
        "http://intranet.example:8080/path?query" => {
            Ok(Some("HTTPS proxy.example:443".to_string()))
        }
        "https://failing.example/" => Err(Error::with_message(ErrorKind::Io, "lookup failed")),
        "https://panicking.example/" => panic!("lookup panicked"),
        _ => Ok(None),
    }
}

async fn authenticate_negotiate(
    request: NetworkAuthenticateNegotiateRequest,
) -> Result<Option<String>> {
    match request.spn.as_str() {
        "HTTP/unavailable" => Ok(None),
        "HTTP/failing" => Err(Error::with_message(ErrorKind::Io, "lookup failed")),
        "HTTP/panicking" => panic!("lookup panicked"),
        spn => Ok(Some(format!("token for {spn}"))),
    }
}

fn both_callbacks() -> ClientOptions {
    ClientOptions::new().with_network(
        NetworkConfig::new()
            .with_resolve_proxy(resolve_proxy)
            .with_authenticate_negotiate(authenticate_negotiate),
    )
}

/// Sends each `(method, params)` request with consecutive IDs from 1000 and
/// returns the responses in request order.
async fn send_requests(runtime: &mut FakeRuntime, requests: &[(&str, Value)]) -> Vec<Value> {
    for (id, (method, params)) in (1000..).zip(requests) {
        write_frame(
            &mut runtime.writer,
            json!({ "jsonrpc": "2.0", "id": id, "method": method, "params": params }),
        )
        .await;
    }
    let mut responses = BTreeMap::new();
    for _ in requests {
        let response = read_frame(&mut runtime.reader).await;
        responses.insert(response["id"].as_u64().unwrap(), response);
    }
    responses.into_values().collect()
}

/// Starts a client whose callbacks never answer, sends it `method` as request
/// `id`, and waits until the callback is running.
///
/// The returned receiver reports `"dropped"` when that callback future is dropped.
async fn start_stalled_request(
    method: &str,
    params: Value,
    id: u64,
) -> (Client, FakeRuntime, mpsc::UnboundedReceiver<&'static str>) {
    let (events_tx, mut events) = mpsc::unbounded_channel();
    let stalled = move || {
        let events = events_tx.clone();
        async move {
            let _dropped = DropSignal(events.clone());
            events.send("started").unwrap();
            std::future::pending::<Result<Option<String>>>().await
        }
    };
    let stalled_proxy = stalled.clone();
    let (client, mut runtime) = start(
        ClientOptions::new().with_network(
            NetworkConfig::new()
                .with_resolve_proxy(move |request: NetworkResolveProxyRequest| {
                    let stalled = stalled_proxy.clone();
                    async move {
                        if request.url == "https://stalled.example/" {
                            stalled().await
                        } else {
                            resolve_proxy(request).await
                        }
                    }
                })
                .with_authenticate_negotiate(
                    move |request: NetworkAuthenticateNegotiateRequest| {
                        let stalled = stalled.clone();
                        async move {
                            if request.spn == "HTTP/stalled" {
                                stalled().await
                            } else {
                                authenticate_negotiate(request).await
                            }
                        }
                    },
                ),
        ),
        vec![json!({ "result": null })],
    )
    .await;
    write_frame(
        &mut runtime.writer,
        json!({ "jsonrpc": "2.0", "id": id, "method": method, "params": params }),
    )
    .await;
    assert_eq!(timeout(WAIT, events.recv()).await.unwrap(), Some("started"));
    (client.unwrap(), runtime, events)
}

#[tokio::test]
async fn registers_both_callbacks_and_answers_concurrent_lookups() {
    let (client, mut runtime) = start(both_callbacks(), vec![json!({ "result": null })]).await;
    let client = client.unwrap();
    let responses = send_requests(
        &mut runtime,
        &[
            (
                "network.resolveProxy",
                json!({ "url": "https://api.example.com/" }),
            ),
            (
                "network.resolveProxy",
                json!({ "url": "http://intranet.example:8080/path?query" }),
            ),
            (
                "network.authenticateNegotiate",
                json!({ "spn": "HTTP/proxy.example" }),
            ),
            (
                "network.authenticateNegotiate",
                json!({ "spn": "ldap/directory.example@EXAMPLE.COM" }),
            ),
        ],
    )
    .await;
    client.force_stop();
    assert_eq!(
        (runtime.registrations(), responses),
        (
            vec![json!({
                "method": "network.setProvider",
                "params": { "resolveProxy": {}, "authenticateNegotiate": {} }
            })],
            vec![
                json!({
                    "jsonrpc": "2.0", "id": 1000,
                    "result": { "proxy": "PROXY proxy.example:8080; DIRECT" }
                }),
                json!({
                    "jsonrpc": "2.0", "id": 1001,
                    "result": { "proxy": "HTTPS proxy.example:443" }
                }),
                json!({
                    "jsonrpc": "2.0", "id": 1002,
                    "result": { "token": "token for HTTP/proxy.example" }
                }),
                json!({
                    "jsonrpc": "2.0", "id": 1003,
                    "result": { "token": "token for ldap/directory.example@EXAMPLE.COM" }
                }),
            ],
        )
    );
}

#[tokio::test]
async fn registers_only_configured_callbacks_and_scope() {
    let cases = [
        (ClientOptions::new(), vec![]),
        (
            ClientOptions::new().with_network(NetworkConfig::new()),
            vec![],
        ),
        (
            ClientOptions::new()
                .with_network(NetworkConfig::new().with_scope(NetworkScope::Runtime)),
            vec![],
        ),
        (
            ClientOptions::new().with_network(NetworkConfig::new().with_resolve_proxy(direct)),
            vec![json!({ "method": "network.setProvider", "params": { "resolveProxy": {} } })],
        ),
        (
            ClientOptions::new().with_network(
                NetworkConfig::new()
                    .with_scope(NetworkScope::Runtime)
                    .with_authenticate_negotiate(unavailable),
            ),
            vec![json!({
                "method": "network.setProvider",
                "params": { "scope": "runtime", "authenticateNegotiate": {} }
            })],
        ),
        (
            ClientOptions::new().with_network(
                NetworkConfig::new()
                    .with_scope(NetworkScope::Connection)
                    .with_resolve_proxy(direct),
            ),
            vec![json!({
                "method": "network.setProvider",
                "params": { "scope": "connection", "resolveProxy": {} }
            })],
        ),
    ];
    let mut registrations = Vec::new();
    let mut expected = Vec::new();
    for (options, registration) in cases {
        let replies = registration
            .iter()
            .map(|_| json!({ "result": null }))
            .collect();
        let (client, runtime) = start(options, replies).await;
        client.unwrap().force_stop();
        registrations.push(runtime.registrations());
        expected.push(registration);
    }
    assert_eq!(registrations, expected);
}

/// A client created from streams takes no options, so it registers its callbacks explicitly, and
/// can retry a registration the runtime rejected.
#[tokio::test]
async fn stream_clients_register_callbacks_explicitly() {
    let (client_io, runtime_io) = tokio::io::duplex(64 * 1024);
    let (client_reader, client_writer) = tokio::io::split(client_io);
    let (mut reader, mut writer) = tokio::io::split(runtime_io);
    let runtime = tokio::spawn(async move {
        let mut frames = Vec::new();
        for reply in [
            json!({ "result": { "ok": true, "protocolVersion": 3, "version": "test" } }),
            json!({ "error": { "code": -32603, "message": "runtime scope is held" } }),
            json!({ "result": null }),
        ] {
            let request = read_frame(&mut reader).await;
            let mut response = json!({ "jsonrpc": "2.0", "id": request["id"] });
            response
                .as_object_mut()
                .unwrap()
                .extend(reply.as_object().unwrap().clone());
            write_frame(&mut writer, response).await;
            frames.push(json!({ "method": request["method"], "params": request["params"] }));
        }
        write_frame(
            &mut writer,
            json!({
                "jsonrpc": "2.0", "id": 1000, "method": "network.resolveProxy",
                "params": { "url": "https://api.example.com/" }
            }),
        )
        .await;
        frames.push(read_frame(&mut reader).await);
        frames
    });
    let client = Client::from_streams(client_reader, client_writer, std::env::temp_dir()).unwrap();
    client.verify_protocol_version().await.unwrap();
    let rejected = client
        .register_network(
            NetworkConfig::new()
                .with_scope(NetworkScope::Runtime)
                .with_resolve_proxy(resolve_proxy)
                .with_authenticate_negotiate(authenticate_negotiate),
        )
        .await;
    let retried = client
        .register_network(NetworkConfig::new().with_resolve_proxy(resolve_proxy))
        .await;
    let frames = timeout(WAIT, runtime).await.unwrap().unwrap();
    client.force_stop();
    assert_eq!(
        (rejected.is_err(), retried.is_ok(), &frames[1..]),
        (
            true,
            true,
            &[
                json!({
                    "method": "network.setProvider",
                    "params": {
                        "scope": "runtime", "resolveProxy": {}, "authenticateNegotiate": {}
                    }
                }),
                json!({ "method": "network.setProvider", "params": { "resolveProxy": {} } }),
                json!({
                    "jsonrpc": "2.0", "id": 1000,
                    "result": { "proxy": "PROXY proxy.example:8080; DIRECT" }
                }),
            ][..]
        )
    );
}

/// A registration the caller stopped waiting for still completes: the runtime's rejection rolls
/// back its handlers, so a later registration on the same connection succeeds.
#[tokio::test]
async fn dropped_registration_is_rolled_back_when_rejected() {
    let (client_io, runtime_io) = tokio::io::duplex(64 * 1024);
    let (client_reader, client_writer) = tokio::io::split(client_io);
    let (mut reader, mut writer) = tokio::io::split(runtime_io);
    let (reject, rejected) = oneshot::channel::<()>();
    let runtime = tokio::spawn(async move {
        let connect = read_frame(&mut reader).await;
        write_frame(
            &mut writer,
            json!({
                "jsonrpc": "2.0", "id": connect["id"],
                "result": { "ok": true, "protocolVersion": 3, "version": "test" }
            }),
        )
        .await;
        let mut registrations = Vec::new();
        for (reply, wait) in [
            (
                json!({ "error": { "code": -32603, "message": "runtime scope is held" } }),
                Some(rejected),
            ),
            (json!({ "result": null }), None),
        ] {
            let request = read_frame(&mut reader).await;
            if let Some(wait) = wait {
                wait.await.unwrap();
            }
            let mut response = json!({ "jsonrpc": "2.0", "id": request["id"] });
            response
                .as_object_mut()
                .unwrap()
                .extend(reply.as_object().unwrap().clone());
            write_frame(&mut writer, response).await;
            registrations.push(json!({ "method": request["method"], "params": request["params"] }));
        }
        registrations
    });
    let client = Client::from_streams(client_reader, client_writer, std::env::temp_dir()).unwrap();
    client.verify_protocol_version().await.unwrap();
    let dropped = timeout(
        Duration::from_millis(50),
        client.register_network(
            NetworkConfig::new()
                .with_scope(NetworkScope::Runtime)
                .with_resolve_proxy(resolve_proxy)
                .with_authenticate_negotiate(authenticate_negotiate),
        ),
    )
    .await;
    reject.send(()).unwrap();
    // The rejected registration's handlers are removed in the background.
    let retried = timeout(WAIT, async {
        loop {
            match client
                .register_network(NetworkConfig::new().with_resolve_proxy(resolve_proxy))
                .await
            {
                Err(error) if error.to_string().contains("already registered") => {
                    tokio::time::sleep(Duration::from_millis(10)).await;
                }
                result => break result,
            }
        }
    })
    .await;
    let registrations = timeout(WAIT, runtime).await.unwrap().unwrap();
    client.force_stop();
    assert_eq!(
        (
            dropped.is_err(),
            matches!(retried, Ok(Ok(()))),
            registrations
        ),
        (
            true,
            true,
            vec![
                json!({
                    "method": "network.setProvider",
                    "params": {
                        "scope": "runtime", "resolveProxy": {}, "authenticateNegotiate": {}
                    }
                }),
                json!({ "method": "network.setProvider", "params": { "resolveProxy": {} } }),
            ]
        )
    );
}

#[tokio::test]
async fn registers_before_other_startup_requests_and_sessions() {
    let plugins = std::env::temp_dir();
    let (client, mut runtime) = start(
        ClientOptions::new()
            .with_builtin_plugin_directories([&plugins])
            .with_network(NetworkConfig::new().with_authenticate_negotiate(unavailable)),
        vec![json!({ "result": null }), json!({ "result": null })],
    )
    .await;
    let client = client.unwrap();
    let session = tokio::spawn({
        let client = client.clone();
        async move { client.create_session(SessionConfig::default()).await }
    });
    let session_request = read_frame(&mut runtime.reader).await;
    client.force_stop();
    session.abort();
    assert_eq!(
        (runtime.registrations(), &session_request["method"]),
        (
            vec![
                json!({
                    "method": "network.setProvider",
                    "params": { "authenticateNegotiate": {} }
                }),
                json!({
                    "method": "plugins.builtin.set",
                    "params": { "paths": [plugins.to_str().unwrap()] }
                }),
            ],
            &json!("session.create"),
        )
    );
}

#[tokio::test]
async fn failed_registration_fails_start() {
    let (client, _runtime) = start(
        ClientOptions::new().with_network(NetworkConfig::new().with_resolve_proxy(direct)),
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

/// Cancelling `Client::start` while the runtime has not answered the registration closes the
/// connection: the registration still in flight does not keep the abandoned client alive.
#[tokio::test]
async fn cancelled_start_closes_the_connection_during_registration() {
    let listener = tokio::net::TcpListener::bind(("127.0.0.1", 0))
        .await
        .unwrap();
    let port = listener.local_addr().unwrap().port();
    let (registering, registration) = oneshot::channel();
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
        let request = read_frame(&mut reader).await;
        registering.send(request["method"].clone()).unwrap();
        // Never answer the registration; only the connection closing ends this read.
        timeout(WAIT, reader.read_to_end(&mut Vec::new()))
            .await
            .is_ok()
    });
    let starting = tokio::spawn(Client::start(
        ClientOptions::new()
            .with_network(NetworkConfig::new().with_resolve_proxy(direct))
            .with_program(CliProgram::Path("unused-external-transport".into()))
            .with_transport(Transport::External {
                host: "127.0.0.1".to_string(),
                port,
                connection_token: None,
            }),
    ));
    let method = timeout(WAIT, registration).await.unwrap().unwrap();
    starting.abort();
    let _ = starting.await;
    let closed = timeout(WAIT * 2, runtime).await.unwrap().unwrap();
    assert_eq!((method, closed), (json!("network.setProvider"), true));
}

#[tokio::test]
async fn answers_missing_values_and_returns_callback_failures_and_panics() {
    let (client, mut runtime) = start(both_callbacks(), vec![json!({ "result": null })]).await;
    let client = client.unwrap();
    let responses = send_requests(
        &mut runtime,
        &[
            (
                "network.resolveProxy",
                json!({ "url": "http://direct.example/path" }),
            ),
            (
                "network.authenticateNegotiate",
                json!({ "spn": "HTTP/unavailable" }),
            ),
            (
                "network.resolveProxy",
                json!({ "url": "https://failing.example/" }),
            ),
            (
                "network.authenticateNegotiate",
                json!({ "spn": "HTTP/failing" }),
            ),
            (
                "network.resolveProxy",
                json!({ "url": "https://panicking.example/" }),
            ),
            (
                "network.authenticateNegotiate",
                json!({ "spn": "HTTP/panicking" }),
            ),
        ],
    )
    .await;
    client.force_stop();
    assert_eq!(
        responses,
        vec![
            json!({ "jsonrpc": "2.0", "id": 1000, "result": {} }),
            json!({ "jsonrpc": "2.0", "id": 1001, "result": {} }),
            json!({
                "jsonrpc": "2.0", "id": 1002,
                "error": { "code": -32603, "message": "lookup failed" }
            }),
            json!({
                "jsonrpc": "2.0", "id": 1003,
                "error": { "code": -32603, "message": "lookup failed" }
            }),
            json!({
                "jsonrpc": "2.0", "id": 1004,
                "error": { "code": -32603, "message": "request handler panicked" }
            }),
            json!({
                "jsonrpc": "2.0", "id": 1005,
                "error": { "code": -32603, "message": "request handler panicked" }
            }),
        ]
    );
}

#[tokio::test]
async fn cancelled_requests_drop_the_callback_future() {
    let mut outcomes = Vec::new();
    for (method, stalled, later) in [
        (
            "network.resolveProxy",
            json!({ "url": "https://stalled.example/" }),
            json!({ "url": "https://api.example.com/" }),
        ),
        (
            "network.authenticateNegotiate",
            json!({ "spn": "HTTP/stalled" }),
            json!({ "spn": "HTTP/proxy.example" }),
        ),
    ] {
        let (client, mut runtime, mut events) = start_stalled_request(method, stalled, 1000).await;
        write_frame(
            &mut runtime.writer,
            json!({ "jsonrpc": "2.0", "method": "$/cancelRequest", "params": { "id": 1000 } }),
        )
        .await;
        let dropped = timeout(WAIT, events.recv()).await.unwrap();
        let cancelled = read_frame(&mut runtime.reader).await;
        // The next frame answers a later request, not the cancelled one again.
        write_frame(
            &mut runtime.writer,
            json!({ "jsonrpc": "2.0", "id": 1001, "method": method, "params": later }),
        )
        .await;
        let answered = read_frame(&mut runtime.reader).await;
        client.force_stop();
        outcomes.push((method, dropped, cancelled, answered));
    }
    let cancelled = json!({
        "jsonrpc": "2.0", "id": 1000,
        "error": { "code": -32800, "message": "Request cancelled" }
    });
    assert_eq!(
        outcomes,
        [
            (
                "network.resolveProxy",
                Some("dropped"),
                cancelled.clone(),
                json!({
                    "jsonrpc": "2.0", "id": 1001,
                    "result": { "proxy": "PROXY proxy.example:8080; DIRECT" }
                }),
            ),
            (
                "network.authenticateNegotiate",
                Some("dropped"),
                cancelled,
                json!({
                    "jsonrpc": "2.0", "id": 1001,
                    "result": { "token": "token for HTTP/proxy.example" }
                }),
            ),
        ]
    );
}

#[tokio::test]
async fn closed_connection_drops_pending_callback_futures() {
    let mut dropped = Vec::new();
    for (method, params) in [
        (
            "network.resolveProxy",
            json!({ "url": "https://stalled.example/" }),
        ),
        (
            "network.authenticateNegotiate",
            json!({ "spn": "HTTP/stalled" }),
        ),
    ] {
        let (client, runtime, mut events) = start_stalled_request(method, params, 1000).await;
        drop(runtime);
        dropped.push((method, timeout(WAIT, events.recv()).await.unwrap()));
        client.force_stop();
    }
    assert_eq!(
        dropped,
        [
            ("network.resolveProxy", Some("dropped")),
            ("network.authenticateNegotiate", Some("dropped")),
        ]
    );
}
