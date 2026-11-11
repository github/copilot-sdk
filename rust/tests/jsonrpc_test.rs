#![cfg(feature = "test-support")]
#![allow(clippy::unwrap_used)]

use github_copilot_sdk::test_support::{JsonRpcClient, JsonRpcNotification, JsonRpcRequest};
use github_copilot_sdk::{ErrorKind, ProtocolErrorKind};
use tokio::io::{AsyncRead, AsyncReadExt, AsyncWrite, AsyncWriteExt, duplex};
use tokio::sync::mpsc;

/// Write a Content-Length framed JSON-RPC message to a writer.
async fn write_framed(writer: &mut (impl AsyncWrite + Unpin), body: &[u8]) {
    let header = format!("Content-Length: {}\r\n\r\n", body.len());
    writer.write_all(header.as_bytes()).await.unwrap();
    writer.write_all(body).await.unwrap();
    writer.flush().await.unwrap();
}

async fn read_framed(reader: &mut (impl AsyncRead + Unpin)) -> Vec<u8> {
    let mut header = String::new();
    loop {
        let mut byte = [0u8; 1];
        reader.read_exact(&mut byte).await.unwrap();
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
    let mut body = vec![0u8; length];
    reader.read_exact(&mut body).await.unwrap();
    body
}

#[tokio::test]
async fn request_response_round_trip() {
    // duplex: client_write → server_read, server_write → client_read
    let (client_write, mut server_read) = duplex(4096);
    let (mut server_write, client_read) = duplex(4096);

    let (notification_tx, _) = mpsc::unbounded_channel();
    let (_request_tx, _request_rx) = mpsc::unbounded_channel();
    let request_tx = _request_tx;

    let client = JsonRpcClient::new(client_write, client_read, notification_tx, request_tx);

    // Spawn a task that reads the request from the server side and sends a response.
    let server_handle = tokio::spawn(async move {
        let mut buf = Vec::new();
        // Read the Content-Length header
        let mut header = String::new();
        loop {
            let mut byte = [0u8; 1];
            tokio::io::AsyncReadExt::read_exact(&mut server_read, &mut byte)
                .await
                .unwrap();
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
        buf.resize(length, 0);
        tokio::io::AsyncReadExt::read_exact(&mut server_read, &mut buf)
            .await
            .unwrap();

        let request: JsonRpcRequest = serde_json::from_slice(&buf).unwrap();
        assert_eq!(request.method, "test.echo");
        assert_eq!(request.jsonrpc, "2.0");

        // Send response
        let response = serde_json::json!({
            "jsonrpc": "2.0",
            "id": request.id,
            "result": { "echoed": true }
        });
        write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;

        request.id
    });

    let response = client
        .send_request("test.echo", Some(serde_json::json!({"hello": "world"})))
        .await
        .unwrap();

    let request_id = server_handle.await.unwrap();
    assert_eq!(response.id, request_id);
    assert!(!response.is_error());
    assert_eq!(response.result.unwrap()["echoed"], serde_json::json!(true));
}

#[tokio::test]
async fn unreadable_error_fails_request_and_keeps_reading() {
    for (error_json, extension_json) in [
        "null",
        r#""not an object""#,
        r#"{"code":-32603,"message":42}"#,
        r#"{"code":9223372036854775808,"message":"overflow"}"#,
        r#"{"code":true,"message":"\ud83d"}"#,
        r#"{"code":-32603,"message":"readable","data":{"detail":true}}"#,
    ]
    .into_iter()
    .flat_map(|error| ["", r#","peerExtension":true"#].map(|extension| (error, extension)))
    {
        let (client_write, mut server_read) = duplex(4096);
        let (mut server_write, client_read) = duplex(4096);
        let (notification_tx, _) = mpsc::unbounded_channel();
        let (request_tx, _request_rx) = mpsc::unbounded_channel();
        let client = JsonRpcClient::new(client_write, client_read, notification_tx, request_tx);

        let peer = async {
            for i in 0..2 {
                let request: JsonRpcRequest =
                    serde_json::from_slice(&read_framed(&mut server_read).await).unwrap();
                let payload = if i == 0 {
                    format!(r#""error":{error_json}"#)
                } else {
                    r#""result":{"recovered":true}"#.to_string()
                };
                let body = format!(
                    r#"{{"jsonrpc":"2.0","id":{}{extension_json},{payload}}}"#,
                    request.id
                );
                write_framed(&mut server_write, body.as_bytes()).await;
            }
        };
        let requests = async {
            let first = client.send_request("bad", None).await;
            if error_json.contains("readable") {
                let error = first.unwrap().error.unwrap();
                assert_eq!(error.message, "readable");
                assert_eq!(error.data, Some(serde_json::json!({"detail": true})));
            } else {
                let error = first.expect_err("malformed error must fail its request");
                assert!(matches!(error.kind(), ErrorKind::Json), "{error:?}");
            }
            let next = client.send_request("next", None).await.unwrap();
            assert_eq!(next.result, Some(serde_json::json!({"recovered": true})));
        };
        tokio::time::timeout(std::time::Duration::from_secs(5), async {
            tokio::join!(peer, requests);
        })
        .await
        .expect("requests did not settle while the peer remained connected");
    }
}

#[tokio::test]
async fn notification_broadcasting() {
    let (_client_write, _discard) = duplex(4096);
    let (mut server_write, client_read) = duplex(4096);

    let (notification_tx, mut notification_rx) = mpsc::unbounded_channel();
    let (request_tx, _request_rx) = mpsc::unbounded_channel();

    let _client = JsonRpcClient::new(_client_write, client_read, notification_tx, request_tx);

    // Server sends a notification (no id field).
    let notification = serde_json::json!({
        "jsonrpc": "2.0",
        "method": "session.event",
        "params": { "session_id": "s1", "event": "started" }
    });
    write_framed(
        &mut server_write,
        &serde_json::to_vec(&notification).unwrap(),
    )
    .await;

    let received: JsonRpcNotification =
        tokio::time::timeout(std::time::Duration::from_secs(2), notification_rx.recv())
            .await
            .expect("timed out waiting for notification")
            .unwrap();

    assert_eq!(received.method, "session.event");
    assert_eq!(received.params.as_ref().unwrap()["session_id"], "s1");
}

#[tokio::test]
async fn server_request_forwarding() {
    let (_client_write, _discard) = duplex(4096);
    let (mut server_write, client_read) = duplex(4096);

    let (notification_tx, _) = mpsc::unbounded_channel();
    let (request_tx, mut request_rx) = mpsc::unbounded_channel();

    let _client = JsonRpcClient::new(_client_write, client_read, notification_tx, request_tx);

    // Server sends a request (has both id and method).
    let request = serde_json::json!({
        "jsonrpc": "2.0",
        "id": 42,
        "method": "permission.request",
        "params": { "kind": "shell" }
    });
    write_framed(&mut server_write, &serde_json::to_vec(&request).unwrap()).await;

    let received: JsonRpcRequest =
        tokio::time::timeout(std::time::Duration::from_secs(2), request_rx.recv())
            .await
            .expect("timed out waiting for request")
            .unwrap();

    assert_eq!(received.method, "permission.request");
    assert_eq!(received.id, 42);
}

#[tokio::test]
async fn error_response_round_trip() {
    let (client_write, mut server_read) = duplex(4096);
    let (mut server_write, client_read) = duplex(4096);

    let (notification_tx, _) = mpsc::unbounded_channel();
    let (request_tx, _) = mpsc::unbounded_channel();

    let client = JsonRpcClient::new(client_write, client_read, notification_tx, request_tx);

    let server_handle = tokio::spawn(async move {
        // Read request
        let mut header = String::new();
        loop {
            let mut byte = [0u8; 1];
            tokio::io::AsyncReadExt::read_exact(&mut server_read, &mut byte)
                .await
                .unwrap();
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
        tokio::io::AsyncReadExt::read_exact(&mut server_read, &mut buf)
            .await
            .unwrap();
        let request: JsonRpcRequest = serde_json::from_slice(&buf).unwrap();

        // Send error response
        let error_response = serde_json::json!({
            "jsonrpc": "2.0",
            "id": request.id,
            "error": { "code": -32600, "message": "Invalid Request" }
        });
        write_framed(
            &mut server_write,
            &serde_json::to_vec(&error_response).unwrap(),
        )
        .await;
    });

    let response = client.send_request("bad.method", None).await.unwrap();
    server_handle.await.unwrap();

    assert!(response.is_error());
    let error = response.error.unwrap();
    assert_eq!(error.code, -32600);
    assert_eq!(error.message, "Invalid Request");
}

/// Cancel-safety regression: dropping a `write()` future after the actor has
/// committed to writing must NOT produce a partial frame on the wire.
///
/// Strategy: read a prefix to acknowledge that writing has begun, then
/// cancel the caller while the rest of the frame cannot fit in the wire.
/// Drain the wire and verify both frames land on it intact.
///
/// Closes RFD-400 finding #1: `JsonRpcClient::write` was holding a Tokio
/// mutex across `write_all` + `flush`, so caller cancellation mid-frame
/// could desync the transport. The writer-actor refactor moves the I/O
/// onto a dedicated task that owns the writer; caller cancellation drops
/// the ack receiver but does not interrupt the in-flight write.
#[tokio::test]
async fn write_actor_completes_on_caller_cancel() {
    use std::sync::Arc;

    let (client_write, mut server_read) = duplex(8);
    let (_server_write, client_read) = duplex(8);

    let (notification_tx, _) = mpsc::unbounded_channel();
    let (request_tx, _) = mpsc::unbounded_channel();
    let client = JsonRpcClient::new(client_write, client_read, notification_tx, request_tx);

    let frame_a = JsonRpcRequest {
        jsonrpc: "2.0".to_string(),
        id: 100,
        method: "first.write".to_string(),
        params: None,
    };
    let frame_b = JsonRpcRequest {
        jsonrpc: "2.0".to_string(),
        id: 101,
        method: "second.write".to_string(),
        params: None,
    };

    let mut prefix = [0; 8];
    {
        let writing = client.write(&frame_a);
        tokio::pin!(writing);
        tokio::select! {
            _ = &mut writing => panic!("write completed before the wire was drained"),
            result = server_read.read_exact(&mut prefix) => { result.unwrap(); }
        }
        // The entire frame cannot fit in the eight-byte wire even after this
        // prefix is read, so cancellation is inside a committed, incomplete write.
    }

    let reader_task = tokio::spawn(async move {
        let mut server_read = std::io::Cursor::new(prefix).chain(server_read);
        let mut frames = Vec::new();
        for _ in 0..2 {
            let mut header = String::new();
            loop {
                let mut byte = [0u8; 1];
                tokio::io::AsyncReadExt::read_exact(&mut server_read, &mut byte)
                    .await
                    .unwrap();
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
            let mut body = vec![0u8; length];
            tokio::io::AsyncReadExt::read_exact(&mut server_read, &mut body)
                .await
                .unwrap();
            let req: JsonRpcRequest = serde_json::from_slice(&body).unwrap();
            frames.push(req);
        }
        frames
    });

    // The actor must finish the cancelled caller's frame before this next one.
    let second_handle = tokio::spawn({
        let frame_b = frame_b.clone();
        let client_arc = Arc::new(client);
        let client_clone = client_arc.clone();
        async move { client_clone.write(&frame_b).await }
    });

    let frames = reader_task.await.unwrap();
    second_handle.await.unwrap().unwrap();

    assert_eq!(frames.len(), 2);
    assert_eq!(frames[0].method, "first.write");
    assert_eq!(frames[0].id, 100);
    assert_eq!(frames[1].method, "second.write");
    assert_eq!(frames[1].id, 101);
}

#[tokio::test]
async fn write_failure_stops_writer_and_preserves_pending_response() {
    use std::error::Error as _;
    use std::future::Future;
    use std::io;
    use std::pin::Pin;
    use std::sync::Arc;
    use std::sync::atomic::{AtomicUsize, Ordering};
    use std::task::{Context, Poll};
    use tokio::sync::oneshot;

    struct FailingWriter {
        inner: tokio::io::DuplexStream,
        first_frame_flushed: bool,
        partial_written: bool,
        failed: bool,
        partial_tx: Option<oneshot::Sender<()>>,
        release_rx: oneshot::Receiver<()>,
        dropped_tx: Option<oneshot::Sender<()>>,
        writes_after_failure: Arc<AtomicUsize>,
    }

    impl AsyncWrite for FailingWriter {
        fn poll_write(
            mut self: Pin<&mut Self>,
            cx: &mut Context<'_>,
            buf: &[u8],
        ) -> Poll<io::Result<usize>> {
            if self.failed {
                self.writes_after_failure.fetch_add(1, Ordering::SeqCst);
                return Poll::Ready(Err(io::Error::other("write attempted after failure")));
            }
            if !self.first_frame_flushed {
                return Pin::new(&mut self.inner).poll_write(cx, buf);
            }
            if !self.partial_written {
                let result = Pin::new(&mut self.inner).poll_write(cx, &buf[..1]);
                if matches!(result, Poll::Ready(Ok(1))) {
                    self.partial_written = true;
                    self.partial_tx.take().unwrap().send(()).unwrap();
                }
                return result;
            }
            match Pin::new(&mut self.release_rx).poll(cx) {
                Poll::Pending => Poll::Pending,
                Poll::Ready(result) => {
                    result.unwrap();
                    self.failed = true;
                    Poll::Ready(Err(io::Error::new(
                        io::ErrorKind::ConnectionReset,
                        "injected partial-frame failure",
                    )))
                }
            }
        }

        fn poll_flush(mut self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<io::Result<()>> {
            let result = Pin::new(&mut self.inner).poll_flush(cx);
            if matches!(result, Poll::Ready(Ok(()))) {
                self.first_frame_flushed = true;
            }
            result
        }

        fn poll_shutdown(mut self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<io::Result<()>> {
            Pin::new(&mut self.inner).poll_shutdown(cx)
        }
    }

    impl Drop for FailingWriter {
        fn drop(&mut self) {
            let _ = self.dropped_tx.take().unwrap().send(());
        }
    }

    let (client_write, mut server_read) = duplex(4096);
    let (mut server_write, client_read) = duplex(4096);
    let (partial_tx, partial_rx) = oneshot::channel();
    let (release_tx, release_rx) = oneshot::channel();
    let (dropped_tx, dropped_rx) = oneshot::channel();
    let writes_after_failure = Arc::new(AtomicUsize::new(0));
    let writer = FailingWriter {
        inner: client_write,
        first_frame_flushed: false,
        partial_written: false,
        failed: false,
        partial_tx: Some(partial_tx),
        release_rx,
        dropped_tx: Some(dropped_tx),
        writes_after_failure: writes_after_failure.clone(),
    };
    let (notification_tx, _) = mpsc::unbounded_channel();
    let (request_tx, _request_rx) = mpsc::unbounded_channel();
    let client = JsonRpcClient::new(writer, client_read, notification_tx, request_tx);

    tokio::time::timeout(std::time::Duration::from_secs(5), async {
        let first = client.send_request("test.first", None);
        tokio::pin!(first);
        assert!(futures_util::poll!(&mut first).is_pending());
        let request: JsonRpcRequest =
            serde_json::from_slice(&read_framed(&mut server_read).await).unwrap();
        assert_eq!(request.method, "test.first");

        let second = client.send_request("test.second", None);
        tokio::pin!(second);
        assert!(futures_util::poll!(&mut second).is_pending());
        partial_rx.await.expect("second frame partially written");

        // Polling enqueues the third frame while the writer is held at the failure.
        let third = client.send_request("test.third", None);
        tokio::pin!(third);
        assert!(futures_util::poll!(&mut third).is_pending());
        release_tx.send(()).unwrap();

        let error = second.await.expect_err("partial-frame write must fail");
        assert_eq!(error.kind(), &ErrorKind::Io);
        let source = error.source().unwrap().downcast_ref::<io::Error>().unwrap();
        assert_eq!(source.kind(), io::ErrorKind::ConnectionReset);
        assert_eq!(source.to_string(), "injected partial-frame failure");

        let queued = third.await.expect_err("queued frame must not be written");
        assert_eq!(queued.kind(), &ErrorKind::Io);
        assert_eq!(
            queued
                .source()
                .unwrap()
                .downcast_ref::<io::Error>()
                .unwrap()
                .kind(),
            io::ErrorKind::BrokenPipe
        );
        dropped_rx.await.expect("failed writer must be dropped");
        assert_eq!(writes_after_failure.load(Ordering::SeqCst), 0);

        let late = client.send_request("test.late", None).await.unwrap_err();
        assert_eq!(
            late.source()
                .unwrap()
                .downcast_ref::<io::Error>()
                .unwrap()
                .kind(),
            io::ErrorKind::BrokenPipe
        );

        // Writer failure must not cancel the reader or discard already-sent replies.
        let response = serde_json::json!({
            "jsonrpc": "2.0",
            "id": request.id,
            "result": { "drained": true }
        });
        write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;
        let response = first.await.expect("pending response must still be drained");
        assert_eq!(
            response.result,
            Some(serde_json::json!({ "drained": true }))
        );

        let mut remaining = Vec::new();
        server_read.read_to_end(&mut remaining).await.unwrap();
        assert_eq!(
            remaining, b"C",
            "only the second frame's prefix was written"
        );
    })
    .await
    .expect("writer failure and pending response must settle");
}

/// Cancel-safety regression: cancelling a `send_request` future before the
/// response arrives must NOT leak the pending-requests entry. The RAII
/// `PendingGuard` removes the entry on drop.
///
/// Strategy: spawn `send_request`, drop the JoinHandle immediately so the
/// future is cancelled. The CLI eventually sends a response for the
/// cancelled request id; the read loop logs a warning and discards it
/// (the pending entry was already removed by the guard). The next
/// `send_request` should work normally and not collide with the orphan.
///
/// Closes RFD-400 finding #4.
#[tokio::test]
async fn send_request_cancellation_does_not_leak_pending() {
    let (client_write, mut server_read) = duplex(4096);
    let (mut server_write, client_read) = duplex(4096);

    let (notification_tx, _) = mpsc::unbounded_channel();
    let (request_tx, _) = mpsc::unbounded_channel();
    let client = JsonRpcClient::new(client_write, client_read, notification_tx, request_tx);
    let client = std::sync::Arc::new(client);

    // First request: cancel before the server replies.
    let cancelled = tokio::spawn({
        let client = client.clone();
        async move {
            // Will await the response oneshot; the JoinHandle abort
            // below cancels this future.
            let _ = client.send_request("first", None).await;
        }
    });

    // Read the first request off the wire so we know it was sent.
    async fn read_one_method(reader: &mut tokio::io::DuplexStream) -> (u64, String) {
        let mut header = String::new();
        loop {
            let mut byte = [0u8; 1];
            tokio::io::AsyncReadExt::read_exact(reader, &mut byte)
                .await
                .unwrap();
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
        let mut body = vec![0u8; length];
        tokio::io::AsyncReadExt::read_exact(reader, &mut body)
            .await
            .unwrap();
        let req: JsonRpcRequest = serde_json::from_slice(&body).unwrap();
        (req.id, req.method)
    }

    let (first_id, first_method) = read_one_method(&mut server_read).await;
    assert_eq!(first_method, "first");

    // Now cancel the in-flight request.
    cancelled.abort();
    let _ = cancelled.await;

    // Send a (late) response for the cancelled id. The read loop should
    // log a warning and not blow up.
    let stale_resp = serde_json::json!({
        "jsonrpc": "2.0",
        "id": first_id,
        "result": {"echo": "ignored"}
    });
    write_framed(&mut server_write, &serde_json::to_vec(&stale_resp).unwrap()).await;

    // Second request: should succeed normally without collision.
    let server_task = tokio::spawn(async move {
        let (id, method) = read_one_method(&mut server_read).await;
        assert_eq!(method, "second");
        let resp = serde_json::json!({
            "jsonrpc": "2.0",
            "id": id,
            "result": {"ok": true}
        });
        write_framed(&mut server_write, &serde_json::to_vec(&resp).unwrap()).await;
    });

    let response = client.send_request("second", None).await.unwrap();
    assert_eq!(response.result.unwrap()["ok"], true);
    server_task.await.unwrap();
}

#[tokio::test]
async fn lone_surrogate_frame_is_recovered_without_closing_connection() {
    let (client_write, mut server_read) = duplex(4096);
    let (mut server_write, client_read) = duplex(4096);
    let (notification_tx, _) = mpsc::unbounded_channel();
    let (request_tx, _) = mpsc::unbounded_channel();
    let client = JsonRpcClient::new(client_write, client_read, notification_tx, request_tx);

    let server_task = tokio::spawn(async move {
        let request: JsonRpcRequest =
            serde_json::from_slice(&read_framed(&mut server_read).await).unwrap();
        let response = format!(
            r#"{{"jsonrpc":"2.0","id":{},"result":{{"name":"invalid \ud83d value"}}}}"#,
            request.id
        );
        write_framed(&mut server_write, response.as_bytes()).await;

        let request: JsonRpcRequest =
            serde_json::from_slice(&read_framed(&mut server_read).await).unwrap();
        let response = serde_json::json!({
            "jsonrpc": "2.0",
            "id": request.id,
            "result": {"name": "still connected"}
        });
        write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;
    });

    let response = client.send_request("models.list", None).await.unwrap();
    assert_eq!(
        response.result.unwrap()["name"],
        serde_json::json!("invalid \u{FFFD} value")
    );

    let response = client.send_request("account.getQuota", None).await.unwrap();
    assert_eq!(
        response.result.unwrap()["name"],
        serde_json::json!("still connected")
    );
    server_task.await.unwrap();
}

#[tokio::test]
async fn unrepairable_frame_remains_fatal() {
    let (client_write, mut server_read) = duplex(4096);
    let (mut server_write, client_read) = duplex(4096);
    let (notification_tx, _) = mpsc::unbounded_channel();
    let (request_tx, _) = mpsc::unbounded_channel();
    let client = JsonRpcClient::new(client_write, client_read, notification_tx, request_tx);

    let server_task = tokio::spawn(async move {
        let request: JsonRpcRequest =
            serde_json::from_slice(&read_framed(&mut server_read).await).unwrap();
        let response = format!(
            r#"{{"jsonrpc":"2.0","id":{},"result":{{"surrogate":"\ud83d","escape":"\q"}}}}"#,
            request.id
        );
        write_framed(&mut server_write, response.as_bytes()).await;
    });

    let error = tokio::time::timeout(
        std::time::Duration::from_secs(2),
        client.send_request("models.list", None),
    )
    .await
    .expect("unrepairable frame did not terminate the pending request")
    .unwrap_err();

    assert_eq!(error.to_string(), "request cancelled");
    assert!(error.is_transport_failure());
    server_task.await.unwrap();
}

/// The CLI answers a request it could not read, for example one its parser
/// refused for its nesting depth, with an error whose `id` is null (#24583).
/// That request's caller gets the CLI's rejection instead of waiting forever,
/// and the connection keeps serving the next request.
#[tokio::test]
async fn null_id_error_fails_the_pending_request_and_keeps_the_connection() {
    let (client_write, mut server_read) = duplex(4096);
    let (mut server_write, client_read) = duplex(4096);
    let (notification_tx, _) = mpsc::unbounded_channel();
    let (request_tx, _) = mpsc::unbounded_channel();
    let client = JsonRpcClient::new(client_write, client_read, notification_tx, request_tx);

    let server_task = tokio::spawn(async move {
        // The CLI reads the request but cannot identify it.
        read_framed(&mut server_read).await;
        write_framed(
            &mut server_write,
            br#"{"jsonrpc":"2.0","id":null,"error":{"code":-32700,"message":"recursion limit exceeded at line 1 column 513"}}"#,
        )
        .await;
        let request: JsonRpcRequest =
            serde_json::from_slice(&read_framed(&mut server_read).await).unwrap();
        let response = serde_json::json!({
            "jsonrpc": "2.0",
            "id": request.id,
            "result": {"name": "still connected"}
        });
        write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;
    });

    let error = tokio::time::timeout(
        std::time::Duration::from_secs(5),
        client.send_request("session.send", None),
    )
    .await
    .expect("the rejected request completes")
    .unwrap_err();
    assert_eq!(
        error.kind(),
        &ErrorKind::Protocol(ProtocolErrorKind::UncorrelatedRejection {
            code: -32700,
            message: "recursion limit exceeded at line 1 column 513".to_string(),
        })
    );
    assert!(!error.is_transport_failure());

    let response = tokio::time::timeout(
        std::time::Duration::from_secs(5),
        client.send_request("models.list", None),
    )
    .await
    .expect("the next request is answered")
    .unwrap();
    assert_eq!(
        response.result.unwrap()["name"],
        serde_json::json!("still connected")
    );
    server_task.await.unwrap();
}

/// With several requests pending, a null-id error cannot say which one the CLI
/// rejected, so every pending request fails with the rejection and none
/// waits. The rejection repeats the CLI's message without its quoted values.
#[tokio::test]
async fn null_id_error_fails_every_pending_request() {
    let (client_write, mut server_read) = duplex(4096);
    let (mut server_write, client_read) = duplex(4096);
    let (notification_tx, _) = mpsc::unbounded_channel();
    let (request_tx, _) = mpsc::unbounded_channel();
    let client = JsonRpcClient::new(client_write, client_read, notification_tx, request_tx);

    let server_task = tokio::spawn(async move {
        read_framed(&mut server_read).await;
        read_framed(&mut server_read).await;
        write_framed(
            &mut server_write,
            br#"{"jsonrpc":"2.0","id":null,"error":{"code":-32600,"message":"invalid type: string \"ghp_secret\", expected u64"}}"#,
        )
        .await;
        let request: JsonRpcRequest =
            serde_json::from_slice(&read_framed(&mut server_read).await).unwrap();
        let response = serde_json::json!({
            "jsonrpc": "2.0",
            "id": request.id,
            "result": {"name": "still connected"}
        });
        write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;
    });

    let (first, second) = tokio::time::timeout(std::time::Duration::from_secs(5), async {
        tokio::join!(
            client.send_request("session.send", None),
            client.send_request("models.list", None)
        )
    })
    .await
    .expect("both pending requests complete");
    for result in [first, second] {
        let error = result.unwrap_err();
        assert_eq!(
            error.kind(),
            &ErrorKind::Protocol(ProtocolErrorKind::UncorrelatedRejection {
                code: -32600,
                message: "invalid type: string \"…\", expected u64".to_string(),
            })
        );
        assert!(!error.to_string().contains("ghp_secret"), "{error}");
    }

    let response = tokio::time::timeout(
        std::time::Duration::from_secs(5),
        client.send_request("account.getQuota", None),
    )
    .await
    .expect("the next request is answered")
    .unwrap();
    assert_eq!(
        response.result.unwrap()["name"],
        serde_json::json!("still connected")
    );
    server_task.await.unwrap();
}

/// A null-id error that arrives with nothing pending fails nothing, and the
/// connection keeps serving.
#[tokio::test]
async fn null_id_error_with_nothing_pending_keeps_the_connection() {
    let (client_write, mut server_read) = duplex(4096);
    let (mut server_write, client_read) = duplex(4096);
    let (notification_tx, mut notifications) = mpsc::unbounded_channel();
    let (request_tx, _) = mpsc::unbounded_channel();
    let client = JsonRpcClient::new(client_write, client_read, notification_tx, request_tx);

    write_framed(
        &mut server_write,
        br#"{"jsonrpc":"2.0","id":null,"error":{"code":-32700,"message":"Parse error"}}"#,
    )
    .await;
    // The read loop handles frames in order: once this notification arrives,
    // the error before it has been handled.
    write_framed(
        &mut server_write,
        br#"{"jsonrpc":"2.0","method":"session.event","params":{}}"#,
    )
    .await;
    let notification: JsonRpcNotification =
        tokio::time::timeout(std::time::Duration::from_secs(5), notifications.recv())
            .await
            .expect("the notification after the error arrives")
            .unwrap();
    assert_eq!(notification.method, "session.event");

    let server_task = tokio::spawn(async move {
        let request: JsonRpcRequest =
            serde_json::from_slice(&read_framed(&mut server_read).await).unwrap();
        let response = serde_json::json!({
            "jsonrpc": "2.0",
            "id": request.id,
            "result": {"name": "still connected"}
        });
        write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;
    });
    let response = tokio::time::timeout(
        std::time::Duration::from_secs(5),
        client.send_request("models.list", None),
    )
    .await
    .expect("the request after the null-id error is answered")
    .unwrap();
    assert_eq!(
        response.result.unwrap()["name"],
        serde_json::json!("still connected")
    );
    server_task.await.unwrap();
}

#[tokio::test]
async fn valid_pairs_and_escaped_backslashes_are_untouched() {
    let (client_write, mut server_read) = duplex(4096);
    let (mut server_write, client_read) = duplex(4096);
    let (notification_tx, _) = mpsc::unbounded_channel();
    let (request_tx, _) = mpsc::unbounded_channel();
    let client = JsonRpcClient::new(client_write, client_read, notification_tx, request_tx);

    let server_task = tokio::spawn(async move {
        let request: JsonRpcRequest =
            serde_json::from_slice(&read_framed(&mut server_read).await).unwrap();
        let response = format!(
            r#"{{"jsonrpc":"2.0","id":{},"result":{{"emoji":"\ud83d\ude00","path":"C:\\ud83d","invalid":"\ud83d"}}}}"#,
            request.id
        );
        write_framed(&mut server_write, response.as_bytes()).await;
    });

    let result = client
        .send_request("models.list", None)
        .await
        .unwrap()
        .result
        .unwrap();

    assert_eq!(result["emoji"], serde_json::json!("😀"));
    assert_eq!(result["path"], serde_json::json!(r"C:\ud83d"));
    assert_eq!(result["invalid"], serde_json::json!("\u{FFFD}"));
    server_task.await.unwrap();
}

/// Once the peer has closed its side, the read loop has ended and failed the
/// requests waiting for a response. A request sent after that fails at once and
/// is never written, though the transport may still take writes, as a TCP
/// socket does after the peer's FIN.
#[tokio::test]
async fn request_after_the_peer_closed_fails_at_once() {
    let (client_write, mut server_read) = duplex(4096);
    let (server_write, client_read) = duplex(4096);
    let (notification_tx, _) = mpsc::unbounded_channel();
    let (request_tx, _request_rx) = mpsc::unbounded_channel();
    let client = std::sync::Arc::new(JsonRpcClient::new(
        client_write,
        client_read,
        notification_tx,
        request_tx,
    ));

    let waiting = tokio::spawn({
        let client = client.clone();
        async move { client.send_request("test.wait", None).await }
    });
    let request: JsonRpcRequest =
        serde_json::from_slice(&read_framed(&mut server_read).await).unwrap();
    assert_eq!(request.method, "test.wait");

    // The peer closes its side: the read loop ends and fails the waiting request.
    drop(server_write);
    let drained = tokio::time::timeout(std::time::Duration::from_secs(5), waiting)
        .await
        .expect("the waiting request was not failed")
        .unwrap()
        .unwrap_err();
    assert_eq!(drained.to_string(), "request cancelled");

    let late = tokio::time::timeout(
        std::time::Duration::from_secs(5),
        client.send_request("test.late", None),
    )
    .await
    .expect("the request after the close was not refused")
    .unwrap_err();
    assert_eq!(late.kind(), &ErrorKind::Io);
    assert!(late.is_transport_failure());

    // Nothing was written for it: once the client is gone, the pipe holds no frame.
    drop(client);
    let mut written = Vec::new();
    tokio::time::timeout(
        std::time::Duration::from_secs(5),
        server_read.read_to_end(&mut written),
    )
    .await
    .expect("the writer was not released")
    .unwrap();
    assert!(written.is_empty(), "frames written: {written:?}");
}

/// A `session.sendAndWait` refused because the connection is closed is never
/// written, and dropping its request does not queue a `$/cancelRequest` for the
/// id that was never sent: the pipe holds no frame at all.
#[tokio::test]
async fn send_and_wait_after_the_peer_closed_writes_no_cancel_frame() {
    let (client_write, mut server_read) = duplex(4096);
    let (server_write, client_read) = duplex(4096);
    let (notification_tx, _) = mpsc::unbounded_channel();
    let (request_tx, _request_rx) = mpsc::unbounded_channel();
    let client = std::sync::Arc::new(JsonRpcClient::new(
        client_write,
        client_read,
        notification_tx,
        request_tx,
    ));

    let waiting = tokio::spawn({
        let client = client.clone();
        async move { client.send_request("test.wait", None).await }
    });
    let request: JsonRpcRequest =
        serde_json::from_slice(&read_framed(&mut server_read).await).unwrap();
    assert_eq!(request.method, "test.wait");

    // The peer closes its side: the read loop ends and fails the waiting request.
    drop(server_write);
    tokio::time::timeout(std::time::Duration::from_secs(5), waiting)
        .await
        .expect("the waiting request was not failed")
        .unwrap()
        .unwrap_err();

    let late = tokio::time::timeout(
        std::time::Duration::from_secs(5),
        client.send_request("session.sendAndWait", None),
    )
    .await
    .expect("the request after the close was not refused")
    .unwrap_err();
    assert_eq!(late.kind(), &ErrorKind::Io);

    // A frame queued for the writer arrives within moments; none may.
    if let Ok(frame) = tokio::time::timeout(
        std::time::Duration::from_millis(300),
        read_framed(&mut server_read),
    )
    .await
    {
        panic!("a frame was written: {}", String::from_utf8_lossy(&frame));
    }

    drop(client);
    let mut written = Vec::new();
    tokio::time::timeout(
        std::time::Duration::from_secs(5),
        server_read.read_to_end(&mut written),
    )
    .await
    .expect("the writer was not released")
    .unwrap();
    assert!(written.is_empty(), "frames written: {written:?}");
}
