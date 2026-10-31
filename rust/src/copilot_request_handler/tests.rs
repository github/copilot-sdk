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

#[tokio::test]
async fn plaintext_websocket_still_forwards_messages() {
    let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
    let address = listener.local_addr().unwrap();
    let response = CopilotWebSocketResponse::new(Arc::new(CopilotRequestExchange::new(
        "plaintext-websocket".to_string(),
        Weak::new(),
    )));
    let server = async {
        let (socket, _) = listener.accept().await.unwrap();
        let mut websocket = tokio_tungstenite::accept_async(socket).await.unwrap();
        assert_eq!(
            websocket.next().await.unwrap().unwrap(),
            Message::Text("plaintext-websocket-message".into()),
        );
        assert!(matches!(
            websocket.next().await.unwrap().unwrap(),
            Message::Close(_),
        ));
    };
    let forward = Box::pin(async {
        let forwarder =
            CopilotWebSocketForwarder::builder(format!("ws://{address}/"), HeaderMap::new())
                .connect(response)
                .await
                .unwrap();
        forwarder
            .send_request_message(CopilotWebSocketMessage {
                data: b"plaintext-websocket-message".to_vec(),
                binary: false,
            })
            .await
            .unwrap();
        forwarder.close().await.unwrap();
    });
    timeout(Duration::from_secs(5), async {
        tokio::join!(server, forward)
    })
    .await
    .expect("plaintext WebSocket should complete its round trip");
}
