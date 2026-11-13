// Copyright (c) Microsoft Corporation. All rights reserved.

#![cfg(test)]

use std::time::Duration;

use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::net::TcpListener;
use tokio::time::timeout;

use super::*;

#[cfg(target_os = "macos")]
async fn assert_tls13_client_hello(
    socket: &mut tokio::net::TcpStream,
    mut record: [u8; 5],
    expected_alpn: &[&[u8]],
) {
    let mut acceptor = rustls::server::Acceptor::default();
    loop {
        let mut bytes = record.to_vec();
        let length = usize::from(u16::from_be_bytes([record[3], record[4]]));
        bytes.resize(5 + length, 0);
        socket.read_exact(&mut bytes[5..]).await.unwrap();
        acceptor.read_tls(&mut std::io::Cursor::new(bytes)).unwrap();
        if let Some(accepted) = acceptor.accept().unwrap() {
            let hello = accepted.client_hello();
            for suite in [
                rustls::CipherSuite::TLS13_AES_128_GCM_SHA256,
                rustls::CipherSuite::TLS13_AES_256_GCM_SHA384,
                rustls::CipherSuite::TLS13_CHACHA20_POLY1305_SHA256,
                rustls::CipherSuite::TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                rustls::CipherSuite::TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
                rustls::CipherSuite::TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256,
                rustls::CipherSuite::TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                rustls::CipherSuite::TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
                rustls::CipherSuite::TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256,
            ] {
                assert!(hello.cipher_suites().contains(&suite), "missing {suite:?}");
            }
            for scheme in [
                rustls::SignatureScheme::ECDSA_NISTP256_SHA256,
                rustls::SignatureScheme::ECDSA_NISTP384_SHA384,
                rustls::SignatureScheme::ECDSA_NISTP521_SHA512,
                rustls::SignatureScheme::RSA_PSS_SHA256,
                rustls::SignatureScheme::RSA_PSS_SHA384,
                rustls::SignatureScheme::RSA_PSS_SHA512,
                rustls::SignatureScheme::RSA_PKCS1_SHA256,
                rustls::SignatureScheme::RSA_PKCS1_SHA384,
                rustls::SignatureScheme::RSA_PKCS1_SHA512,
                rustls::SignatureScheme::ED25519,
                rustls::SignatureScheme::ML_DSA_44,
                rustls::SignatureScheme::ML_DSA_65,
                rustls::SignatureScheme::ML_DSA_87,
            ] {
                assert!(
                    hello.signature_schemes().contains(&scheme),
                    "missing {scheme:?}"
                );
            }
            let groups = hello.named_groups().expect("missing supported groups");
            for group in [
                rustls::NamedGroup::X25519,
                rustls::NamedGroup::secp256r1,
                rustls::NamedGroup::secp384r1,
                rustls::NamedGroup::X25519MLKEM768,
            ] {
                assert!(groups.contains(&group), "missing {group:?}");
            }
            assert_eq!(
                hello
                    .alpn()
                    .map(|protocols| protocols.collect::<Vec<_>>())
                    .unwrap_or_default(),
                expected_alpn
            );
            return;
        }
        socket.read_exact(&mut record).await.unwrap();
    }
}

#[tokio::test]
async fn secure_http_rejects_a_plaintext_endpoint_without_panicking() {
    let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
    let address = listener.local_addr().unwrap();
    let server = async {
        let (mut socket, _) = listener.accept().await.unwrap();
        let mut record = [0; 5];
        socket.read_exact(&mut record).await.unwrap();
        assert_eq!(record[0], 0x16, "expected a TLS handshake: {record:?}");
        #[cfg(target_os = "macos")]
        assert_tls13_client_hello(&mut socket, record, &[b"h2", b"http/1.1"]).await;
        socket
            .write_all(b"HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n")
            .await
            .unwrap();
    };
    let request = CopilotHttpRequest {
        method: "GET".to_owned(),
        url: format!("https://{address}/"),
        headers: HeaderMap::new(),
        body: Vec::new(),
        cancel: CancellationToken::new(),
    };
    let connect = Box::pin(forward_http(request));
    let ((), result) = timeout(Duration::from_secs(5), async {
        tokio::join!(server, connect)
    })
    .await
    .expect("HTTPS rejection should finish promptly");
    assert!(
        matches!(result, Err(CopilotRequestError::Upstream(_))),
        "plaintext endpoint must produce an upstream TLS error"
    );
}

#[tokio::test]
async fn plaintext_http_still_forwards_requests_and_streams_responses() {
    let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
    let address = listener.local_addr().unwrap();
    let server = async {
        let (mut socket, _) = listener.accept().await.unwrap();
        let mut head = Vec::new();
        while !head.ends_with(b"\r\n\r\n") {
            head.push(socket.read_u8().await.unwrap());
        }
        assert!(head.starts_with(b"POST /forward HTTP/1.1\r\n"), "{head:?}");
        let mut body = [0; 7];
        socket.read_exact(&mut body).await.unwrap();
        assert_eq!(body, *b"payload");
        socket
            .write_all(b"HTTP/1.1 200 OK\r\nContent-Length: 8\r\nConnection: close\r\n\r\naccepted")
            .await
            .unwrap();
    };
    let forward = Box::pin(async {
        let request = CopilotHttpRequest {
            method: "POST".to_owned(),
            url: format!("http://{address}/forward"),
            headers: HeaderMap::new(),
            body: b"payload".to_vec(),
            cancel: CancellationToken::new(),
        };
        let mut response = forward_http(request).await.unwrap();
        assert_eq!(response.status, 200);
        let mut body = Vec::new();
        while let Some(chunk) = response.body.next().await {
            body.extend_from_slice(&chunk.unwrap());
        }
        assert_eq!(body, b"accepted");
    });
    timeout(Duration::from_secs(5), async {
        tokio::join!(server, forward)
    })
    .await
    .expect("HTTP forwarding should complete its round trip");
}

#[tokio::test]
async fn secure_websocket_rejects_a_plaintext_endpoint_without_panicking() {
    let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
    let address = listener.local_addr().unwrap();
    let response = CopilotWebSocketResponse::new(Arc::new(CopilotRequestExchange::new(
        "secure-websocket".to_string(),
        Weak::new(),
    )));
    let server = async {
        let (mut socket, _) = listener.accept().await.unwrap();
        let mut record = [0; 5];
        socket.read_exact(&mut record).await.unwrap();
        assert_eq!(record[0], 0x16, "expected a TLS handshake: {record:?}");
        #[cfg(target_os = "macos")]
        assert_tls13_client_hello(&mut socket, record, &[]).await;
        socket
            .write_all(b"HTTP/1.1 101 Switching Protocols\r\n\r\n")
            .await
            .unwrap();
    };
    let connect = Box::pin(
        CopilotWebSocketForwarder::builder(format!("wss://{address}/"), HeaderMap::new())
            .connect(response),
    );
    let ((), result) = timeout(Duration::from_secs(5), async {
        tokio::join!(server, connect)
    })
    .await
    .expect("TLS rejection should finish promptly");
    match result {
        Err(CopilotRequestError::Upstream(message)) => assert!(
            message.contains("websocket connect failed"),
            "unexpected transport error: {message}",
        ),
        Err(error) => panic!("expected an upstream TLS rejection, got {error}"),
        Ok(_) => panic!("secure WebSocket accepted a plaintext endpoint"),
    }
}

#[derive(Default)]
struct EmptyResponseHandler {
    calls: std::sync::atomic::AtomicUsize,
}

#[async_trait]
impl CopilotRequestHandler for EmptyResponseHandler {
    async fn send_request(
        &self,
        _request: CopilotHttpRequest,
        _ctx: &CopilotRequestContext,
    ) -> Result<CopilotHttpResponse, CopilotRequestError> {
        self.calls.fetch_add(1, std::sync::atomic::Ordering::SeqCst);
        Ok(CopilotHttpResponse::new(
            200,
            None,
            HeaderMap::new(),
            Box::pin(futures_util::stream::empty()),
        ))
    }
}

fn inference_frame(id: u64, method: &str, params: serde_json::Value) -> JsonRpcRequest {
    JsonRpcRequest {
        jsonrpc: "2.0".to_string(),
        id,
        method: method.to_string(),
        params: Some(params),
    }
}

async fn start_request(dispatcher: &Arc<CopilotRequestDispatcher>, request_id: &str) {
    dispatcher
        .dispatch(inference_frame(
            1,
            METHOD_HTTP_REQUEST_START,
            serde_json::json!({"requestId": request_id, "method": "GET", "url": "http://unused.test", "headers": {}}),
        ))
        .await;
}

async fn send_chunk(
    dispatcher: &Arc<CopilotRequestDispatcher>,
    request_id: &str,
    frame: serde_json::Value,
) {
    let mut params = frame;
    params["requestId"] = serde_json::json!(request_id);
    dispatcher
        .dispatch(inference_frame(2, METHOD_HTTP_REQUEST_CHUNK, params))
        .await;
}

async fn finish_request(dispatcher: &Arc<CopilotRequestDispatcher>, request_id: &str) {
    start_request(dispatcher, request_id).await;
    send_chunk(
        dispatcher,
        request_id,
        serde_json::json!({"data": "", "end": true}),
    )
    .await;
    timeout(Duration::from_secs(5), async {
        while !dispatcher.pending.lock().is_empty() {
            tokio::time::sleep(Duration::from_millis(1)).await;
        }
    })
    .await
    .expect("the finished request should be removed");
}

#[tokio::test]
async fn frames_after_request_finished_are_ignored() {
    let handler = Arc::new(EmptyResponseHandler::default());
    let dispatcher = Arc::new(CopilotRequestDispatcher::new(handler.clone()));
    finish_request(&dispatcher, "finished").await;

    // Frames the runtime can still send, such as its cancel of a response it
    // rejected over the size limit after the handler ended it.
    send_chunk(
        &dispatcher,
        "finished",
        serde_json::json!({"data": "", "cancel": true, "end": true}),
    )
    .await;
    send_chunk(&dispatcher, "finished", serde_json::json!({"data": "late"})).await;
    assert!(dispatcher.pending.lock().is_empty());

    start_request(&dispatcher, "finished").await;
    tokio::time::sleep(Duration::from_millis(50)).await;
    assert!(dispatcher.pending.lock().is_empty());
    assert_eq!(
        handler.calls.load(std::sync::atomic::Ordering::SeqCst),
        1,
        "a start for a finished request must not run the handler again"
    );
}

#[tokio::test]
async fn finished_request_ids_forget_the_oldest_past_the_bound() {
    let dispatcher = Arc::new(CopilotRequestDispatcher::new(Arc::new(
        EmptyResponseHandler::default(),
    )));
    for index in 0..=MAX_FINISHED_REQUEST_IDS {
        finish_request(&dispatcher, &format!("request-{index}")).await;
    }

    send_chunk(
        &dispatcher,
        "request-1",
        serde_json::json!({"data": "late"}),
    )
    .await;
    assert!(dispatcher.pending.lock().is_empty());
    send_chunk(
        &dispatcher,
        "request-0",
        serde_json::json!({"data": "late"}),
    )
    .await;
    let keys: Vec<String> = dispatcher.pending.lock().keys().cloned().collect();
    assert_eq!(keys, ["request-0"]);
}
