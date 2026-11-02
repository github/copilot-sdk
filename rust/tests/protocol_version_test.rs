#![allow(clippy::unwrap_used)]

use github_copilot_sdk::Client;
use tokio::io::{AsyncReadExt, AsyncWrite, AsyncWriteExt, duplex};

#[tokio::test]
async fn custom_streams_forward_entra_and_reject_legacy_servers_without_ping() {
    use github_copilot_sdk::EntraAuthConfig;

    for supported in [true, false] {
        let (client_write, mut server_read) = duplex(8192);
        let (mut server_write, client_read) = duplex(8192);
        let client = Client::from_streams_with_entra(
            client_read,
            client_write,
            std::env::temp_dir(),
            Some(EntraAuthConfig {
                client_id: "custom-stream-client".to_owned(),
            }),
        )
        .unwrap();
        let server = async {
            let request = read_framed(&mut server_read).await;
            assert_eq!(request["method"], "connect");
            assert_eq!(
                request["params"]["entra"]["clientId"],
                "custom-stream-client"
            );
            let response = if supported {
                serde_json::json!({"jsonrpc":"2.0","id":request["id"],"result":{"ok":true,"protocolVersion":3,"version":"test"}})
            } else {
                serde_json::json!({"jsonrpc":"2.0","id":request["id"],"error":{"code":-32601,"message":"Method not found"}})
            };
            write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;
        };
        let result = tokio::time::timeout(std::time::Duration::from_secs(2), async {
            let (result, ()) = tokio::join!(client.verify_protocol_version(), server);
            result
        })
        .await
        .expect("handshake must settle without a legacy ping");
        if supported {
            result.unwrap();
            assert_eq!(client.protocol_version(), Some(3));
        } else {
            assert!(
                result
                    .unwrap_err()
                    .to_string()
                    .contains("does not support Entra client configuration")
            );
        }
        drop(client);
        assert_eq!(
            server_read.read(&mut [0]).await.unwrap(),
            0,
            "unexpected ping"
        );
    }
}

#[cfg(feature = "runtime")]
#[tokio::test]
async fn legacy_ping_fallback_requires_no_entra_configuration() {
    use github_copilot_sdk::{ClientOptions, EntraAuthConfig, Transport};

    for entra in [false, true] {
        let listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
        let address = listener.local_addr().unwrap();
        let server = tokio::spawn(async move {
            let (mut stream, _) = listener.accept().await.unwrap();
            let connect = read_framed(&mut stream).await;
            assert_eq!(connect["method"], "connect");
            write_framed(
                &mut stream,
                &serde_json::to_vec(&serde_json::json!({
                    "jsonrpc": "2.0", "id": connect["id"],
                    "error": {"code": -32601, "message": "Method not found"}
                }))
                .unwrap(),
            )
            .await;
            if !entra {
                let ping = read_framed(&mut stream).await;
                assert_eq!(ping["method"], "ping");
                write_framed(
                    &mut stream,
                    &serde_json::to_vec(&serde_json::json!({
                        "jsonrpc": "2.0", "id": ping["id"],
                        "result": {"protocolVersion": 3}
                    }))
                    .unwrap(),
                )
                .await;
            }
            let mut byte = [0];
            assert_eq!(
                stream.read(&mut byte).await.unwrap(),
                0,
                "unexpected request after handshake"
            );
        });
        let directory = tempfile::tempdir().unwrap();
        let mut options = ClientOptions::default()
            .with_base_directory(directory.path())
            .with_program(std::env::current_exe().unwrap())
            .with_transport(Transport::External {
                host: address.ip().to_string(),
                port: address.port(),
                connection_token: None,
            });
        if entra {
            options = options.with_entra(EntraAuthConfig {
                client_id: "host-entra-client".to_owned(),
            });
        }
        let result = Client::start(options).await;
        if entra {
            let error = result.expect_err("unsupported Entra configuration must fail");
            assert!(
                error
                    .to_string()
                    .contains("does not support Entra client configuration"),
                "{error}"
            );
        } else {
            let client = result.unwrap();
            assert_eq!(client.protocol_version(), Some(3));
            drop(client);
        }
        tokio::time::timeout(std::time::Duration::from_secs(5), server)
            .await
            .unwrap()
            .unwrap();
    }
}

#[tokio::test]
async fn dropping_external_client_closes_its_streams() {
    let (client_write, mut server_read) = duplex(8192);
    let (_server_write, client_read) = duplex(8192);
    let client = Client::from_streams(client_read, client_write, std::env::temp_dir()).unwrap();
    client
        .register_request_handler("host.shutdown", |_, client| async move {
            client.call("ping", None).await
        })
        .unwrap();
    drop(client);
    let mut byte = [0];
    let count = tokio::time::timeout(
        std::time::Duration::from_secs(2),
        server_read.read(&mut byte),
    )
    .await
    .unwrap()
    .unwrap();
    assert_eq!(count, 0);
}

#[tokio::test]
async fn connection_handler_can_await_sdk_replies_before_responding() {
    let (client_write, mut server_read) = duplex(8192);
    let (mut server_write, client_read) = duplex(8192);
    let client = Client::from_streams(client_read, client_write, std::env::temp_dir()).unwrap();
    client
        .register_request_handler("host.shutdown", |_, client| async move {
            client.call("ping", None).await
        })
        .unwrap();
    let request = serde_json::json!({"jsonrpc":"2.0","id":41,"method":"host.shutdown","params":{}});
    write_framed(&mut server_write, &serde_json::to_vec(&request).unwrap()).await;
    let ping = tokio::time::timeout(
        std::time::Duration::from_secs(2),
        read_framed(&mut server_read),
    )
    .await
    .unwrap();
    assert_eq!(ping["method"], "ping");
    let response = serde_json::json!({"jsonrpc":"2.0","id":ping["id"],"result":{"drained":true}});
    write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;
    let shutdown = tokio::time::timeout(
        std::time::Duration::from_secs(2),
        read_framed(&mut server_read),
    )
    .await
    .unwrap();
    assert_eq!(shutdown["id"], 41);
    assert_eq!(shutdown["result"], serde_json::json!({"drained":true}));
    drop(client);
    let mut byte = [0];
    assert_eq!(
        tokio::time::timeout(
            std::time::Duration::from_secs(2),
            server_read.read(&mut byte),
        )
        .await
        .unwrap()
        .unwrap(),
        0
    );
}

#[tokio::test(flavor = "multi_thread", worker_threads = 2)]
async fn synchronous_handler_setup_does_not_block_the_reader() {
    let (client_write, mut server_read) = duplex(8192);
    let (mut server_write, client_read) = duplex(8192);
    let client = Client::from_streams(client_read, client_write, ".".into()).unwrap();
    let (started_tx, mut started) = tokio::sync::mpsc::unbounded_channel();
    let (unblock, gate) = std::sync::mpsc::channel();
    let gate = std::sync::Mutex::new(gate);
    client
        .register_request_handler("blocking.setup", move |_, _| {
            started_tx.send(()).unwrap();
            gate.lock()
                .unwrap()
                .recv_timeout(std::time::Duration::from_secs(10))
                .unwrap();
            async { Ok(serde_json::json!({})) }
        })
        .unwrap();
    let request = serde_json::json!({"jsonrpc":"2.0","id":44,"method":"blocking.setup"});
    write_framed(&mut server_write, &serde_json::to_vec(&request).unwrap()).await;
    tokio::time::timeout(std::time::Duration::from_secs(2), started.recv())
        .await
        .unwrap()
        .unwrap();
    let ping = tokio::spawn({
        let client = client.clone();
        async move { client.call("ping", None).await }
    });
    let request = read_framed(&mut server_read).await;
    let response = serde_json::json!({"jsonrpc":"2.0","id":request["id"],"result":{}});
    write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;
    tokio::time::timeout(std::time::Duration::from_secs(2), ping)
        .await
        .unwrap()
        .unwrap()
        .unwrap();
    unblock.send(()).unwrap();
    assert_eq!(read_framed(&mut server_read).await["id"], 44);
    client.force_stop();
}

#[tokio::test]
async fn connection_handlers_reject_duplicate_registration_and_report_errors() {
    let (client_write, mut server_read) = duplex(8192);
    let (mut server_write, client_read) = duplex(8192);
    let client = Client::from_streams(client_read, client_write, std::env::temp_dir()).unwrap();
    client
        .register_request_handler("host.shutdown", |_, _| async {
            Err(github_copilot_sdk::Error::with_message(
                github_copilot_sdk::ErrorKind::InvalidConfig,
                "drain failed",
            ))
        })
        .unwrap();
    assert!(
        client
            .register_request_handler("host.shutdown", |_, _| async { Ok(serde_json::json!({})) })
            .is_err()
    );
    assert!(
        client
            .register_request_handler("", |_, _| async { Ok(serde_json::json!({})) })
            .is_err()
    );
    let request = serde_json::json!({"jsonrpc":"2.0","id":42,"method":"host.shutdown","params":{}});
    write_framed(&mut server_write, &serde_json::to_vec(&request).unwrap()).await;
    let response = tokio::time::timeout(
        std::time::Duration::from_secs(2),
        read_framed(&mut server_read),
    )
    .await
    .unwrap();
    assert_eq!(response["error"]["code"], -32603);
    assert_eq!(response["error"]["message"], "drain failed");
    client.force_stop();
}

#[tokio::test]
async fn connection_eof_cancels_inbound_handlers() {
    use std::sync::Arc;

    use tokio::sync::Notify;

    struct OnDrop(Arc<Notify>);
    impl Drop for OnDrop {
        fn drop(&mut self) {
            self.0.notify_one();
        }
    }
    let (client_write, _server_read) = duplex(8192);
    let (mut server_write, client_read) = duplex(8192);
    let client = Client::from_streams(client_read, client_write, std::env::temp_dir()).unwrap();
    let started = Arc::new(Notify::new());
    let cancelled = Arc::new(Notify::new());
    let callback_started = started.clone();
    let callback_cancelled = cancelled.clone();
    client
        .register_request_handler("host.shutdown", move |_, client| {
            let started = callback_started.clone();
            let cancelled = callback_cancelled.clone();
            async move {
                let _guard = OnDrop(cancelled);
                let _client = client;
                started.notify_one();
                std::future::pending().await
            }
        })
        .unwrap();
    let request = serde_json::json!({"jsonrpc":"2.0","id":43,"method":"host.shutdown","params":{}});
    write_framed(&mut server_write, &serde_json::to_vec(&request).unwrap()).await;
    tokio::time::timeout(std::time::Duration::from_secs(2), started.notified())
        .await
        .unwrap();
    drop(client);
    server_write.shutdown().await.unwrap();
    tokio::time::timeout(std::time::Duration::from_secs(2), cancelled.notified())
        .await
        .unwrap();
}

#[cfg(not(feature = "runtime"))]
#[tokio::test]
async fn external_stream_build_cannot_launch_or_discover_a_runtime() {
    let error = Client::start(github_copilot_sdk::ClientOptions::default())
        .await
        .unwrap_err();
    assert!(
        error
            .to_string()
            .contains("requires the `runtime` Cargo feature")
    );
}

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
        err.kind(),
        github_copilot_sdk::ErrorKind::Protocol(
            github_copilot_sdk::ProtocolErrorKind::VersionMismatch { server: 1, .. }
        )
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
    assert!(req["params"].get("clientInfo").is_none());
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

/// Positive coverage for application-identity forwarding on the `connect`
/// handshake. A client constructed with a [`ClientInfo`] MUST serialize it
/// (camelCase) into the outbound `connect` request's `clientInfo` param so
/// the runtime attributes this connection's telemetry to the application.
#[tokio::test]
async fn connect_handshake_forwards_client_info() {
    let (client_write, server_read) = duplex(8192);
    let (server_write, client_read) = duplex(8192);
    let client = Client::from_streams_with_client_info(
        client_read,
        client_write,
        std::env::temp_dir(),
        Some(
            github_copilot_sdk::ClientInfo::new()
                .with_application_name("acme-developer-portal")
                .with_application_version("2.4.0")
                .with_integration_name("copilot-assistant")
                .with_integration_version("1.5.0"),
        ),
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
    let client_info = &req["params"]["clientInfo"];
    assert_eq!(client_info["editorName"], "acme-developer-portal");
    assert_eq!(client_info["editorVersion"], "2.4.0");
    assert_eq!(client_info["extensionName"], "copilot-assistant");
    assert_eq!(client_info["extensionVersion"], "1.5.0");

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

/// A [`ClientInfo`] with only some fields set must omit the empty ones from
/// the wire, and a fully-empty one must drop `clientInfo` entirely so the
/// runtime keeps its default attribution.
#[tokio::test]
async fn connect_handshake_omits_empty_client_info_fields() {
    let (client_write, server_read) = duplex(8192);
    let (server_write, client_read) = duplex(8192);
    let client = Client::from_streams_with_client_info(
        client_read,
        client_write,
        std::env::temp_dir(),
        Some(
            github_copilot_sdk::ClientInfo::new()
                .with_application_name("example-app")
                .with_application_version(""),
        ),
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
    let client_info = &req["params"]["clientInfo"];
    assert_eq!(client_info["editorName"], "example-app");
    assert!(client_info.get("editorVersion").is_none());
    assert!(client_info.get("extensionName").is_none());
    assert!(client_info.get("extensionVersion").is_none());

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

/// A [`ClientInfo`] whose every field is empty must drop `clientInfo` from the
/// handshake entirely so the runtime keeps its default attribution.
#[tokio::test]
async fn connect_handshake_omits_all_empty_client_info() {
    let (client_write, server_read) = duplex(8192);
    let (server_write, client_read) = duplex(8192);
    let client = Client::from_streams_with_client_info(
        client_read,
        client_write,
        std::env::temp_dir(),
        Some(
            github_copilot_sdk::ClientInfo::new()
                .with_application_name("")
                .with_application_version("")
                .with_integration_name("")
                .with_integration_version(""),
        ),
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
    assert!(
        req["params"].get("clientInfo").is_none(),
        "an all-empty clientInfo must be omitted from the handshake"
    );

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
