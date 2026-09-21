use std::path::PathBuf;
use std::sync::atomic::{AtomicUsize, Ordering};
use std::time::Duration;

use serde_json::{Value, json};
use tokio::io::{AsyncReadExt, AsyncWriteExt, DuplexStream, duplex};
use tokio::sync::mpsc;
use tokio::time::timeout;

use super::*;

const TIMEOUT: Duration = Duration::from_secs(5);

// Same Content-Length duplex protocol fixture as tests/prepared_session_test.rs.
struct Peer {
    read: DuplexStream,
    write: DuplexStream,
}

impl Peer {
    async fn request(&mut self) -> Value {
        timeout(TIMEOUT, async {
            let mut header = Vec::new();
            while !header.ends_with(b"\r\n\r\n") {
                header.push(self.read.read_u8().await.unwrap());
            }
            let length: usize = std::str::from_utf8(&header)
                .unwrap()
                .trim()
                .strip_prefix("Content-Length: ")
                .unwrap()
                .parse()
                .unwrap();
            let mut body = vec![0; length];
            self.read.read_exact(&mut body).await.unwrap();
            serde_json::from_slice(&body).unwrap()
        })
        .await
        .unwrap()
    }

    async fn send(&mut self, value: Value) {
        let body = serde_json::to_vec(&value).unwrap();
        let header = format!("Content-Length: {}\r\n\r\n", body.len());
        self.write.write_all(header.as_bytes()).await.unwrap();
        self.write.write_all(&body).await.unwrap();
        self.write.flush().await.unwrap();
    }

    async fn respond(&mut self, request: &Value, result: Value) {
        self.send(json!({"jsonrpc": "2.0", "id": request["id"], "result": result}))
            .await;
    }

    async fn started(&mut self, request: &Value, token: Option<&str>) {
        let mut result = json!({
            "hostId": request["params"]["hostId"],
            "pid": 1234,
            "url": "http://127.0.0.1:4321"
        });
        if let Some(token) = token {
            result["token"] = json!(token);
        }
        self.respond(request, result).await;
    }

    async fn exited(&mut self, host_id: &str) {
        self.send(json!({
            "jsonrpc": "2.0", "method": "host.exited",
            "params": {"hostId": host_id, "reason": "exited", "exitCode": 17}
        }))
        .await;
    }
}

fn fixture() -> (Client, Peer) {
    let (client_write, read) = duplex(1 << 20);
    let (write, client_read) = duplex(1 << 20);
    (
        Client::from_streams(client_read, client_write, PathBuf::from(".")).unwrap(),
        Peer { read, write },
    )
}

fn start(
    client: &Client,
    options: AhpHostOptions,
) -> tokio::task::JoinHandle<Result<AhpHost, Error>> {
    let client = client.clone();
    tokio::spawn(async move { client.start_ahp_host(options).await })
}

fn callback_options() -> (AhpHostOptions, mpsc::UnboundedReceiver<AhpHostExit>) {
    let (tx, rx) = mpsc::unbounded_channel();
    (
        AhpHostOptions::default().with_on_exit(move |exit| {
            let _ = tx.send(exit);
        }),
        rx,
    )
}

#[tokio::test]
async fn forwards_only_generated_options_and_returns_runtime_fields() {
    let (client, mut peer) = fixture();
    let (options, _exits) = callback_options();
    let pending = start(
        &client,
        options
            .with_hostname("::1")
            .with_port(0)
            .with_token("explicit-token")
            .with_require_connection_token(true),
    );
    let request = peer.request().await;
    assert_eq!(request["method"], "host.start");
    let host_id = request["params"]["hostId"].as_str().unwrap();
    assert_eq!(uuid::Uuid::parse_str(host_id).unwrap().get_version_num(), 4);
    assert_eq!(
        request["params"],
        json!({
            "hostId": host_id, "hostname": "::1", "port": 0,
            "token": "explicit-token", "requireConnectionToken": true
        })
    );
    peer.started(&request, Some("runtime-token")).await;
    let host = pending.await.unwrap().unwrap();
    assert_eq!(host.host_id, host_id);
    assert_eq!(host.pid, 1234);
    assert_eq!(host.url, "http://127.0.0.1:4321");
    assert_eq!(host.token.as_deref(), Some("runtime-token"));
}

#[tokio::test]
async fn default_options_are_omitted_and_token_can_be_absent() {
    let (client, mut peer) = fixture();
    let pending = start(&client, AhpHostOptions::default());
    let request = peer.request().await;
    assert_eq!(request["params"].as_object().unwrap().len(), 1);
    peer.started(&request, None).await;
    assert!(pending.await.unwrap().unwrap().token.is_none());
    assert!(client.inner.ahp_host_callbacks.lock().is_empty());
}

#[tokio::test]
async fn listener_validation_is_left_to_the_runtime() {
    let (client, mut peer) = fixture();
    let pending = start(
        &client,
        AhpHostOptions::default()
            .with_hostname("")
            .with_port(-1)
            .with_token("")
            .with_require_connection_token(false),
    );
    let request = peer.request().await;
    assert_eq!(request["params"]["port"], -1);
    assert_eq!(request["params"]["hostname"], "");
    assert_eq!(request["params"]["token"], "");
    assert_eq!(request["params"]["requireConnectionToken"], false);
    peer.send(json!({
        "jsonrpc": "2.0", "id": request["id"],
        "error": {"code": -32602, "message": "invalid listener"}
    }))
    .await;
    let error = pending.await.unwrap().unwrap_err();
    assert!(matches!(error.kind(), ErrorKind::Rpc { code: -32602 }));
    assert!(error.to_string().contains("invalid listener"));
}

#[tokio::test]
async fn correlates_early_exit_and_delivers_at_most_once() {
    let (client, mut peer) = fixture();
    let (options, mut exits) = callback_options();
    let pending = start(&client, options);
    let request = peer.request().await;
    let host_id = request["params"]["hostId"].as_str().unwrap();
    peer.exited("unrelated-host").await;
    peer.exited(host_id).await;
    peer.exited(host_id).await;
    let exit = timeout(TIMEOUT, exits.recv()).await.unwrap().unwrap();
    assert_eq!(exit.host_id, host_id);
    assert_eq!(exit.reason, AhpHostExitReason::Exited);
    assert_eq!(exit.exit_code, Some(17));
    assert!(timeout(TIMEOUT, exits.recv()).await.unwrap().is_none());
    assert!(client.inner.ahp_host_callbacks.lock().is_empty());
    peer.started(&request, None).await;
    assert_eq!(pending.await.unwrap().unwrap().host_id, host_id);
}

#[tokio::test]
async fn concurrent_hosts_have_independent_callbacks_and_ids() {
    let (client, mut peer) = fixture();
    let (first_options, mut first_exits) = callback_options();
    let first = start(&client, first_options);
    let first_request = peer.request().await;
    let (second_options, mut second_exits) = callback_options();
    let second = start(&client, second_options);
    let second_request = peer.request().await;
    let first_id = first_request["params"]["hostId"].as_str().unwrap();
    let second_id = second_request["params"]["hostId"].as_str().unwrap();
    assert_ne!(first_id, second_id);
    peer.started(&first_request, None).await;
    peer.started(&second_request, None).await;
    first.await.unwrap().unwrap();
    second.await.unwrap().unwrap();
    peer.exited(second_id).await;
    assert_eq!(
        timeout(TIMEOUT, second_exits.recv())
            .await
            .unwrap()
            .unwrap()
            .host_id,
        second_id
    );
    assert!(first_exits.try_recv().is_err());
    peer.exited(first_id).await;
    assert_eq!(
        timeout(TIMEOUT, first_exits.recv())
            .await
            .unwrap()
            .unwrap()
            .host_id,
        first_id
    );
}

#[tokio::test]
async fn failed_start_releases_callback_without_synthetic_exit() {
    let (client, mut peer) = fixture();
    let (options, mut exits) = callback_options();
    let pending = start(&client, options);
    let request = peer.request().await;
    assert_eq!(client.inner.ahp_host_callbacks.lock().len(), 1);
    peer.send(json!({
        "jsonrpc": "2.0", "id": request["id"],
        "error": {"code": -32000, "message": "could not start"}
    }))
    .await;
    assert!(pending.await.unwrap().is_err());
    assert!(client.inner.ahp_host_callbacks.lock().is_empty());
    assert!(timeout(TIMEOUT, exits.recv()).await.unwrap().is_none());
    peer.exited(request["params"]["hostId"].as_str().unwrap())
        .await;
}

#[tokio::test]
async fn cancelled_start_releases_callback_without_dispose_rpc() {
    let (client, mut peer) = fixture();
    let (options, mut exits) = callback_options();
    let pending = start(&client, options);
    peer.request().await;
    pending.abort();
    assert!(pending.await.unwrap_err().is_cancelled());
    assert!(client.inner.ahp_host_callbacks.lock().is_empty());
    assert!(timeout(TIMEOUT, exits.recv()).await.unwrap().is_none());
    assert!(
        timeout(Duration::from_millis(50), peer.read.read_u8())
            .await
            .is_err()
    );
}

#[tokio::test]
async fn dispose_forwards_concurrent_repeated_calls_and_runtime_errors() {
    let (client, mut peer) = fixture();
    let (options, mut exits) = callback_options();
    let pending = start(&client, options);
    let request = peer.request().await;
    peer.started(&request, None).await;
    let host = pending.await.unwrap().unwrap();
    let first = tokio::spawn({
        let host = host.clone();
        async move { host.dispose().await }
    });
    let second = tokio::spawn({
        let host = host.clone();
        async move { host.dispose().await }
    });
    let first_request = peer.request().await;
    let second_request = peer.request().await;
    assert_ne!(first_request["id"], second_request["id"]);
    for request in [&first_request, &second_request] {
        assert_eq!(request["method"], "host.dispose");
        assert_eq!(request["params"], json!({"hostId": host.host_id}));
    }
    peer.respond(&second_request, json!({})).await;
    peer.respond(&first_request, json!({})).await;
    first.await.unwrap().unwrap();
    second.await.unwrap().unwrap();
    assert_eq!(client.inner.ahp_host_callbacks.lock().len(), 1);
    assert!(
        exits.try_recv().is_err(),
        "dispose must not synthesize exit"
    );

    let third = tokio::spawn({
        let host = host.clone();
        async move { host.dispose().await }
    });
    let request = peer.request().await;
    assert_eq!(request["method"], "host.dispose");
    peer.send(json!({
        "jsonrpc": "2.0", "id": request["id"],
        "error": {"code": -32001, "message": "runtime disposal error"}
    }))
    .await;
    assert!(matches!(
        third.await.unwrap().unwrap_err().kind(),
        ErrorKind::Rpc { code: -32001 }
    ));
    peer.exited(&host.host_id).await;
    timeout(TIMEOUT, exits.recv()).await.unwrap().unwrap();
    let fourth = tokio::spawn(async move { host.dispose().await });
    let request = peer.request().await;
    assert_eq!(request["method"], "host.dispose");
    peer.respond(&request, json!({})).await;
    fourth.await.unwrap().unwrap();
}

#[tokio::test]
async fn disconnect_notifies_once_without_claiming_reaping() {
    let (client, mut peer) = fixture();
    let (options, mut exits) = callback_options();
    let pending = start(&client, options);
    let request = peer.request().await;
    peer.started(&request, None).await;
    let host = pending.await.unwrap().unwrap();
    drop(peer);
    let exit = timeout(TIMEOUT, exits.recv()).await.unwrap().unwrap();
    assert_owner_disconnected(&exit, &host.host_id);
    client.force_stop();
    assert!(timeout(TIMEOUT, exits.recv()).await.unwrap().is_none());
    assert!(client.inner.ahp_host_callbacks.lock().is_empty());
    assert!(host.dispose().await.is_err());
}

#[tokio::test]
async fn force_stop_notifies_once_without_dispose_loop() {
    let (client, mut peer) = fixture();
    let (options, mut exits) = callback_options();
    let pending = start(&client, options);
    let request = peer.request().await;
    peer.started(&request, None).await;
    let host = pending.await.unwrap().unwrap();
    client.force_stop();
    client.force_stop();
    let exit = timeout(TIMEOUT, exits.recv()).await.unwrap().unwrap();
    assert_owner_disconnected(&exit, &host.host_id);
    assert!(timeout(TIMEOUT, exits.recv()).await.unwrap().is_none());
    assert!(client.inner.ahp_host_callbacks.lock().is_empty());
    assert_eq!(
        timeout(TIMEOUT, peer.read.read_u8())
            .await
            .unwrap()
            .unwrap_err()
            .kind(),
        std::io::ErrorKind::UnexpectedEof
    );
}

fn assert_owner_disconnected(exit: &AhpHostExit, host_id: &str) {
    assert_eq!(exit.host_id, host_id);
    assert_eq!(exit.reason, AhpHostExitReason::OwnerDisconnected);
    assert_eq!(exit.exit_code, None);
    assert!(
        exit.error
            .as_deref()
            .unwrap()
            .contains("runtime cleanup cannot be acknowledged")
    );
}

#[tokio::test]
async fn queued_real_exit_precedes_disconnect_and_is_not_duplicated() {
    let (client, mut peer) = fixture();
    let (options, mut exits) = callback_options();
    let pending = start(&client, options);
    let request = peer.request().await;
    peer.started(&request, None).await;
    let host = pending.await.unwrap().unwrap();
    let (options, mut remaining_exits) = callback_options();
    let pending = start(&client, options);
    let request = peer.request().await;
    peer.started(&request, None).await;
    let remaining = pending.await.unwrap().unwrap();

    // Queue notifications and close without yielding so both select branches
    // are ready when the dispatcher next runs.
    for _ in 0..2 {
        client
            .inner
            .notification_tx
            .send(
                serde_json::from_value(json!({
                    "jsonrpc": "2.0", "method": "host.exited",
                    "params": {"hostId": host.host_id, "reason": "exited", "exitCode": 17}
                }))
                .unwrap(),
            )
            .unwrap();
    }
    client.force_stop();
    let exit = timeout(TIMEOUT, exits.recv()).await.unwrap().unwrap();
    assert_eq!(exit.host_id, host.host_id);
    assert_eq!(exit.reason, AhpHostExitReason::Exited);
    assert_eq!(exit.exit_code, Some(17));
    assert_eq!(exit.error, None);
    assert!(timeout(TIMEOUT, exits.recv()).await.unwrap().is_none());
    let exit = timeout(TIMEOUT, remaining_exits.recv())
        .await
        .unwrap()
        .unwrap();
    assert_owner_disconnected(&exit, &remaining.host_id);
    assert!(
        timeout(TIMEOUT, remaining_exits.recv())
            .await
            .unwrap()
            .is_none()
    );
    assert!(client.inner.ahp_host_callbacks.lock().is_empty());
}

#[tokio::test]
async fn wire_exit_immediately_before_eof_is_not_replaced_by_disconnect() {
    let (client, mut peer) = fixture();
    let (options, mut exits) = callback_options();
    let pending = start(&client, options);
    let request = peer.request().await;
    peer.started(&request, None).await;
    let host = pending.await.unwrap().unwrap();
    peer.exited(&host.host_id).await;
    drop(peer);
    let exit = timeout(TIMEOUT, exits.recv()).await.unwrap().unwrap();
    assert_eq!(exit.reason, AhpHostExitReason::Exited);
    assert_eq!(exit.exit_code, Some(17));
    assert!(timeout(TIMEOUT, exits.recv()).await.unwrap().is_none());
}

#[tokio::test]
async fn disconnect_notifies_all_callbacks_even_when_they_panic() {
    let (client, mut peer) = fixture();
    let (tx, mut exits) = mpsc::unbounded_channel();
    let mut host_ids = Vec::new();
    for _ in 0..3 {
        let tx = tx.clone();
        let pending = start(
            &client,
            AhpHostOptions::new().with_on_exit(move |exit| {
                tx.send(exit).unwrap();
                panic!("test disconnect callback panic");
            }),
        );
        let request = peer.request().await;
        peer.started(&request, None).await;
        host_ids.push(pending.await.unwrap().unwrap().host_id);
    }
    drop(tx);
    client.force_stop();
    for _ in 0..3 {
        let exit = timeout(TIMEOUT, exits.recv()).await.unwrap().unwrap();
        let index = host_ids.iter().position(|id| id == &exit.host_id).unwrap();
        assert_owner_disconnected(&exit, &host_ids.remove(index));
    }
    assert!(timeout(TIMEOUT, exits.recv()).await.unwrap().is_none());
    assert!(client.inner.ahp_host_callbacks.lock().is_empty());
}

#[tokio::test]
async fn callback_panic_does_not_break_other_callbacks_or_rpc() {
    let (client, mut peer) = fixture();
    let calls = Arc::new(AtomicUsize::new(0));
    let pending = start(
        &client,
        AhpHostOptions::default().with_on_exit({
            let calls = calls.clone();
            move |_| {
                calls.fetch_add(1, Ordering::SeqCst);
                panic!("test callback panic");
            }
        }),
    );
    let request = peer.request().await;
    peer.started(&request, None).await;
    let first = pending.await.unwrap().unwrap();
    let (options, mut exits) = callback_options();
    let pending = start(&client, options);
    let request = peer.request().await;
    peer.started(&request, None).await;
    let second = pending.await.unwrap().unwrap();
    peer.exited(&first.host_id).await;
    peer.exited(&first.host_id).await;
    peer.exited(&second.host_id).await;
    timeout(TIMEOUT, exits.recv()).await.unwrap().unwrap();
    assert_eq!(calls.load(Ordering::SeqCst), 1);
    assert!(client.inner.ahp_host_callbacks.lock().is_empty());
    let dispose = tokio::spawn(async move { second.dispose().await });
    let request = peer.request().await;
    peer.respond(&request, json!({})).await;
    dispose.await.unwrap().unwrap();
}

#[tokio::test]
async fn handle_and_dispatcher_do_not_keep_client_alive() {
    let (client, mut peer) = fixture();
    let client_weak = Arc::downgrade(&client.inner);
    let callbacks_weak = Arc::downgrade(&client.inner.ahp_host_callbacks);
    let (options, mut exits) = callback_options();
    let pending = start(&client, options);
    let request = peer.request().await;
    peer.started(&request, None).await;
    let host = pending.await.unwrap().unwrap();
    drop(client);
    assert!(client_weak.upgrade().is_none());
    assert!(callbacks_weak.upgrade().is_none());
    assert!(timeout(TIMEOUT, exits.recv()).await.unwrap().is_none());
    assert!(host.dispose().await.is_err());
}

#[tokio::test]
async fn dropping_handle_does_not_dispose_or_unregister_callback() {
    let (client, mut peer) = fixture();
    let (options, mut exits) = callback_options();
    let pending = start(&client, options);
    let request = peer.request().await;
    peer.started(&request, None).await;
    drop(pending.await.unwrap().unwrap());
    assert_eq!(client.inner.ahp_host_callbacks.lock().len(), 1);
    assert!(
        timeout(Duration::from_millis(50), peer.read.read_u8())
            .await
            .is_err()
    );
    peer.exited(request["params"]["hostId"].as_str().unwrap())
        .await;
    timeout(TIMEOUT, exits.recv()).await.unwrap().unwrap();
    assert!(client.inner.ahp_host_callbacks.lock().is_empty());
}
