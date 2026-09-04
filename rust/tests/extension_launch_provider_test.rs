#![allow(clippy::unwrap_used)]

use std::collections::HashMap;
use std::sync::Arc;
use std::sync::atomic::{AtomicUsize, Ordering};
use std::time::Duration;

use async_trait::async_trait;
use github_copilot_sdk::extension_launch_provider::{
    ExtensionLaunchProfile, ExtensionLaunchProvider, ExtensionLaunchProviderResolveRequest,
    ExtensionLaunchProviderResolveResult,
};
use github_copilot_sdk::rpc::ExtensionSource;
use github_copilot_sdk::{Client, ClientOptions, Error, ErrorKind, Transport};
use serde_json::{Value, json};
use tokio::io::{AsyncRead, AsyncReadExt, AsyncWrite, AsyncWriteExt, duplex};
use tokio::net::{TcpListener, TcpStream};
use tokio::sync::{Notify, mpsc};
use tokio::time::timeout;

const TEST_TIMEOUT: Duration = Duration::from_secs(2);

async fn write_framed(writer: &mut (impl AsyncWrite + Unpin), value: &Value) {
    let body = serde_json::to_vec(value).unwrap();
    let header = format!("Content-Length: {}\r\n\r\n", body.len());
    writer.write_all(header.as_bytes()).await.unwrap();
    writer.write_all(&body).await.unwrap();
    writer.flush().await.unwrap();
}

async fn read_framed(reader: &mut (impl AsyncRead + Unpin)) -> Option<Value> {
    let mut header = String::new();
    loop {
        let mut byte = [0u8; 1];
        match reader.read_exact(&mut byte).await {
            Ok(_) => {}
            Err(error) if error.kind() == std::io::ErrorKind::UnexpectedEof => return None,
            Err(error) => panic!("failed to read frame header: {error}"),
        }
        header.push(byte[0] as char);
        if header.ends_with("\r\n\r\n") {
            break;
        }
    }

    let length = header
        .trim()
        .strip_prefix("Content-Length: ")
        .unwrap()
        .parse()
        .unwrap();
    let mut body = vec![0; length];
    reader.read_exact(&mut body).await.unwrap();
    Some(serde_json::from_slice(&body).unwrap())
}

fn resolve_params() -> Value {
    json!({
        "id": "project:legacy-extension",
        "modulePath": "/extensions/legacy/index.js",
        "name": "Legacy extension",
        "source": "project"
    })
}

fn app_launch_result(executable: &str) -> ExtensionLaunchProviderResolveResult {
    ExtensionLaunchProviderResolveResult {
        launch: Some(ExtensionLaunchProfile {
            executable: executable.to_string(),
            args: vec!["/app/preloads/extension_bootstrap.mjs".to_string()],
            env: HashMap::from([
                ("COPILOT_AUTO_UPDATE".to_string(), "false".to_string()),
                (
                    "EXTENSION_PATH".to_string(),
                    "/extensions/legacy/index.js".to_string(),
                ),
            ]),
        }),
    }
}

struct AppProvider {
    executable: String,
    observed: Option<mpsc::UnboundedSender<ExtensionLaunchProviderResolveRequest>>,
    release: Option<Arc<Notify>>,
    calls: Arc<AtomicUsize>,
}

#[async_trait]
impl ExtensionLaunchProvider for AppProvider {
    async fn resolve(
        &self,
        request: ExtensionLaunchProviderResolveRequest,
    ) -> github_copilot_sdk::Result<ExtensionLaunchProviderResolveResult> {
        self.calls.fetch_add(1, Ordering::SeqCst);
        if let Some(observed) = &self.observed {
            observed.send(request).unwrap();
        }
        if let Some(release) = &self.release {
            release.notified().await;
        }
        Ok(app_launch_result(&self.executable))
    }
}

struct FailingProvider;

#[async_trait]
impl ExtensionLaunchProvider for FailingProvider {
    async fn resolve(
        &self,
        _request: ExtensionLaunchProviderResolveRequest,
    ) -> github_copilot_sdk::Result<ExtensionLaunchProviderResolveResult> {
        Err(Error::with_message(
            ErrorKind::InvalidConfig,
            "extension profile lookup failed",
        ))
    }
}

#[test]
fn launch_profile_request_and_result_serialize_exactly() {
    let request: ExtensionLaunchProviderResolveRequest =
        serde_json::from_value(resolve_params()).unwrap();
    assert_eq!(request.id, "project:legacy-extension");
    assert_eq!(request.module_path, "/extensions/legacy/index.js");
    assert_eq!(request.name, "Legacy extension");
    assert_eq!(request.source, ExtensionSource::Project);
    assert_eq!(serde_json::to_value(request).unwrap(), resolve_params());

    let result = app_launch_result("/app/copilot");
    assert_eq!(
        serde_json::to_value(&result).unwrap(),
        json!({
            "launch": {
                "executable": "/app/copilot",
                "args": ["/app/preloads/extension_bootstrap.mjs"],
                "env": {
                    "COPILOT_AUTO_UPDATE": "false",
                    "EXTENSION_PATH": "/extensions/legacy/index.js"
                }
            }
        })
    );

    let round_trip: ExtensionLaunchProviderResolveResult =
        serde_json::from_value(serde_json::to_value(result).unwrap()).unwrap();
    let launch = round_trip.launch.unwrap();
    assert_eq!(launch.executable, "/app/copilot");
    assert_eq!(launch.args, vec!["/app/preloads/extension_bootstrap.mjs"]);
    assert_eq!(
        launch.env,
        HashMap::from([
            ("COPILOT_AUTO_UPDATE".to_string(), "false".to_string()),
            (
                "EXTENSION_PATH".to_string(),
                "/extensions/legacy/index.js".to_string()
            )
        ])
    );
}

#[tokio::test]
async fn configured_async_provider_dispatches_without_a_session() {
    let (client_write, mut server_read) = duplex(8192);
    let (mut server_write, client_read) = duplex(8192);
    let temp = tempfile::tempdir().unwrap();
    let (observed_tx, mut observed_rx) = mpsc::unbounded_channel();
    let release = Arc::new(Notify::new());
    let calls = Arc::new(AtomicUsize::new(0));
    let client = Client::from_streams_with_extension_launch_provider(
        client_read,
        client_write,
        temp.path().to_path_buf(),
        Arc::new(AppProvider {
            executable: "/app/copilot".to_string(),
            observed: Some(observed_tx),
            release: Some(release.clone()),
            calls: calls.clone(),
        }),
    )
    .unwrap();
    client.start_router_for_test();

    write_framed(
        &mut server_write,
        &json!({
            "jsonrpc": "2.0",
            "id": 41,
            "method": "extensionLaunchProvider.resolve",
            "params": resolve_params()
        }),
    )
    .await;

    let observed = timeout(TEST_TIMEOUT, observed_rx.recv())
        .await
        .unwrap()
        .unwrap();
    assert_eq!(observed.id, "project:legacy-extension");
    assert!(
        timeout(Duration::from_millis(25), read_framed(&mut server_read))
            .await
            .is_err()
    );

    release.notify_one();
    let response = timeout(TEST_TIMEOUT, read_framed(&mut server_read))
        .await
        .unwrap()
        .unwrap();
    assert_eq!(response["id"], 41);
    assert_eq!(
        response["result"],
        serde_json::to_value(app_launch_result("/app/copilot")).unwrap()
    );
    assert_eq!(calls.load(Ordering::SeqCst), 1);
}

#[tokio::test]
async fn missing_provider_returns_client_global_handler_error() {
    let (client_write, mut server_read) = duplex(8192);
    let (mut server_write, client_read) = duplex(8192);
    let temp = tempfile::tempdir().unwrap();
    let client =
        Client::from_streams(client_read, client_write, temp.path().to_path_buf()).unwrap();
    client.start_router_for_test();

    write_framed(
        &mut server_write,
        &json!({
            "jsonrpc": "2.0",
            "id": 42,
            "method": "extensionLaunchProvider.resolve",
            "params": resolve_params()
        }),
    )
    .await;

    let response = timeout(TEST_TIMEOUT, read_framed(&mut server_read))
        .await
        .unwrap()
        .unwrap();
    assert_eq!(response["id"], 42);
    assert_eq!(response["error"]["code"], -32603);
    assert_eq!(
        response["error"]["message"],
        "No extensionLaunchProvider client-global handler registered"
    );
}

#[tokio::test]
async fn provider_error_is_returned_as_json_rpc_error() {
    let (client_write, mut server_read) = duplex(8192);
    let (mut server_write, client_read) = duplex(8192);
    let temp = tempfile::tempdir().unwrap();
    let client = Client::from_streams_with_extension_launch_provider(
        client_read,
        client_write,
        temp.path().to_path_buf(),
        Arc::new(FailingProvider),
    )
    .unwrap();
    client.start_router_for_test();

    write_framed(
        &mut server_write,
        &json!({
            "jsonrpc": "2.0",
            "id": 43,
            "method": "extensionLaunchProvider.resolve",
            "params": resolve_params()
        }),
    )
    .await;

    let response = timeout(TEST_TIMEOUT, read_framed(&mut server_read))
        .await
        .unwrap()
        .unwrap();
    assert_eq!(response["id"], 43);
    assert_eq!(response["error"]["code"], -32603);
    assert!(
        response["error"]["message"]
            .as_str()
            .unwrap()
            .contains("extension profile lookup failed")
    );
}

async fn respond_to_connect(
    reader: &mut (impl AsyncRead + Unpin),
    writer: &mut (impl AsyncWrite + Unpin),
) {
    let request = read_framed(reader).await.unwrap();
    assert_eq!(request["method"], "connect");
    write_framed(
        writer,
        &json!({
            "jsonrpc": "2.0",
            "id": request["id"],
            "result": {
                "ok": true,
                "protocolVersion": 3,
                "version": "test"
            }
        }),
    )
    .await;
}

async fn read_registration(reader: &mut (impl AsyncRead + Unpin)) -> Value {
    let request = read_framed(reader).await.unwrap();
    assert_eq!(request["method"], "registerExtensionLaunchProvider");
    assert_eq!(request["params"], json!({}));
    request
}

fn external_options(port: u16, provider: AppProvider) -> ClientOptions {
    ClientOptions::new()
        .with_transport(Transport::External {
            host: "127.0.0.1".to_string(),
            port,
            connection_token: None,
        })
        .with_extension_launch_provider(provider)
}

#[tokio::test]
async fn registration_precedes_session_work_and_routes_during_start() {
    let listener = TcpListener::bind(("127.0.0.1", 0)).await.unwrap();
    let port = listener.local_addr().unwrap().port();
    let calls = Arc::new(AtomicUsize::new(0));
    let server = tokio::spawn(async move {
        let (stream, _) = listener.accept().await.unwrap();
        let (mut reader, mut writer) = stream.into_split();
        respond_to_connect(&mut reader, &mut writer).await;
        let registration = read_registration(&mut reader).await;

        write_framed(
            &mut writer,
            &json!({
                "jsonrpc": "2.0",
                "id": 501,
                "method": "extensionLaunchProvider.resolve",
                "params": resolve_params()
            }),
        )
        .await;
        let callback_response = read_framed(&mut reader).await.unwrap();
        assert_eq!(callback_response["id"], 501);
        assert_eq!(
            callback_response["result"],
            serde_json::to_value(app_launch_result("/app/copilot")).unwrap()
        );

        write_framed(
            &mut writer,
            &json!({
                "jsonrpc": "2.0",
                "id": registration["id"],
                "result": null
            }),
        )
        .await;
        assert!(
            timeout(Duration::from_millis(50), read_framed(&mut reader))
                .await
                .is_err()
        );
    });

    let client = Client::start(external_options(
        port,
        AppProvider {
            executable: "/app/copilot".to_string(),
            observed: None,
            release: None,
            calls: calls.clone(),
        },
    ))
    .await
    .unwrap();

    timeout(TEST_TIMEOUT, server).await.unwrap().unwrap();
    assert_eq!(calls.load(Ordering::SeqCst), 1);
    client.stop().await.unwrap();
}

async fn serve_registered_client(stream: TcpStream, request_id: u64, expected_executable: &str) {
    let (mut reader, mut writer) = stream.into_split();
    respond_to_connect(&mut reader, &mut writer).await;
    let registration = read_registration(&mut reader).await;
    write_framed(
        &mut writer,
        &json!({
            "jsonrpc": "2.0",
            "id": registration["id"],
            "result": null
        }),
    )
    .await;

    write_framed(
        &mut writer,
        &json!({
            "jsonrpc": "2.0",
            "id": request_id,
            "method": "extensionLaunchProvider.resolve",
            "params": resolve_params()
        }),
    )
    .await;
    let response = read_framed(&mut reader).await.unwrap();
    assert_eq!(response["id"], request_id);
    assert_eq!(
        response["result"]["launch"]["executable"],
        expected_executable
    );
    assert_eq!(read_framed(&mut reader).await, None);
}

#[tokio::test]
async fn shutdown_and_restart_do_not_reuse_or_duplicate_providers() {
    let listener = TcpListener::bind(("127.0.0.1", 0)).await.unwrap();
    let port = listener.local_addr().unwrap().port();
    let first_calls = Arc::new(AtomicUsize::new(0));
    let second_calls = Arc::new(AtomicUsize::new(0));
    let (served_tx, mut served_rx) = mpsc::unbounded_channel();

    let server = tokio::spawn(async move {
        let (first, _) = listener.accept().await.unwrap();
        serve_registered_client(first, 601, "/app/first-copilot").await;
        served_tx.send(()).unwrap();

        let (second, _) = listener.accept().await.unwrap();
        serve_registered_client(second, 602, "/app/second-copilot").await;
        served_tx.send(()).unwrap();
    });

    let first = Client::start(external_options(
        port,
        AppProvider {
            executable: "/app/first-copilot".to_string(),
            observed: None,
            release: None,
            calls: first_calls.clone(),
        },
    ))
    .await
    .unwrap();
    timeout(TEST_TIMEOUT, async {
        while first_calls.load(Ordering::SeqCst) == 0 {
            tokio::task::yield_now().await;
        }
    })
    .await
    .unwrap();
    first.stop().await.unwrap();
    timeout(TEST_TIMEOUT, served_rx.recv())
        .await
        .unwrap()
        .unwrap();

    let second = Client::start(external_options(
        port,
        AppProvider {
            executable: "/app/second-copilot".to_string(),
            observed: None,
            release: None,
            calls: second_calls.clone(),
        },
    ))
    .await
    .unwrap();
    timeout(TEST_TIMEOUT, async {
        while second_calls.load(Ordering::SeqCst) == 0 {
            tokio::task::yield_now().await;
        }
    })
    .await
    .unwrap();
    second.stop().await.unwrap();
    timeout(TEST_TIMEOUT, served_rx.recv())
        .await
        .unwrap()
        .unwrap();
    timeout(TEST_TIMEOUT, server).await.unwrap().unwrap();

    assert_eq!(first_calls.load(Ordering::SeqCst), 1);
    assert_eq!(second_calls.load(Ordering::SeqCst), 1);
}
