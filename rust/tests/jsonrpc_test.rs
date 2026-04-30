#![cfg(feature = "test-support")]
#![allow(clippy::unwrap_used)]

use github_copilot_sdk::test_support::{JsonRpcClient, JsonRpcNotification, JsonRpcRequest};
use tokio::io::{AsyncWrite, AsyncWriteExt, duplex};
use tokio::sync::{broadcast, mpsc};

/// Write a Content-Length framed JSON-RPC message to a writer.
async fn write_framed(writer: &mut (impl AsyncWrite + Unpin), body: &[u8]) {
    let header = format!("Content-Length: {}\r\n\r\n", body.len());
    writer.write_all(header.as_bytes()).await.unwrap();
    writer.write_all(body).await.unwrap();
    writer.flush().await.unwrap();
}

#[tokio::test]
async fn request_response_round_trip() {
    // duplex: client_write → server_read, server_write → client_read
    let (client_write, mut server_read) = duplex(4096);
    let (mut server_write, client_read) = duplex(4096);

    let (notification_tx, _) = broadcast::channel(16);
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
async fn notification_broadcasting() {
    let (_client_write, _discard) = duplex(4096);
    let (mut server_write, client_read) = duplex(4096);

    let (notification_tx, mut notification_rx) = broadcast::channel(16);
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
    assert_eq!(received.params.unwrap()["session_id"], "s1");
}

#[tokio::test]
async fn server_request_forwarding() {
    let (_client_write, _discard) = duplex(4096);
    let (mut server_write, client_read) = duplex(4096);

    let (notification_tx, _) = broadcast::channel(16);
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

    let (notification_tx, _) = broadcast::channel(16);
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

#[tokio::test]
async fn read_loop_terminates_on_eof() {
    let (client_write, _discard) = duplex(4096);
    let (server_write, client_read) = duplex(4096);

    let (notification_tx, _) = broadcast::channel(16);
    let (request_tx, _) = mpsc::unbounded_channel();

    let _client = JsonRpcClient::new(client_write, client_read, notification_tx, request_tx);

    // Drop the server side — the read loop should see EOF and stop.
    drop(server_write);

    // Give the read loop time to notice EOF.
    tokio::time::sleep(std::time::Duration::from_millis(100)).await;
}

/// Cancel-safety regression: dropping a `write()` future after the actor has
/// committed to writing must NOT produce a partial frame on the wire.
///
/// Strategy: spawn a reader task that waits before draining the wire, so
/// the actor's `write_all` blocks waiting for room. Race the caller's
/// future against a sleep; when the sleep wins, the caller's future is
/// dropped while suspended on `ack_rx.await`. Release the reader and
/// verify both frames land on the wire intact.
///
/// Closes RFD-400 finding #1: `JsonRpcClient::write` was holding a Tokio
/// mutex across `write_all` + `flush`, so caller cancellation mid-frame
/// could desync the transport. The writer-actor refactor moves the I/O
/// onto a dedicated task that owns the writer; caller cancellation drops
/// the ack receiver but does not interrupt the in-flight write.
#[tokio::test]
async fn write_actor_completes_on_caller_cancel() {
    use std::sync::Arc;

    use tokio::sync::Notify;

    let (client_write, mut server_read) = duplex(8);
    let (_server_write, client_read) = duplex(8);

    let (notification_tx, _) = broadcast::channel(16);
    let (request_tx, _) = mpsc::unbounded_channel();
    let client = JsonRpcClient::new(client_write, client_read, notification_tx, request_tx);

    // Reader task that waits for `start` before draining; this gives us
    // a window where the actor's write_all is suspended waiting for room.
    let start = Arc::new(Notify::new());
    let start_clone = start.clone();
    let reader_task = tokio::spawn(async move {
        start_clone.notified().await;
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

    // First write: race the future against a sleep. With the reader
    // gated, the actor's write_all blocks at the 8-byte buffer boundary,
    // so the future stays suspended on `ack_rx.await`. The sleep wins
    // after 50ms, dropping the caller's future. The actor still owns the
    // write and must complete it once the reader drains.
    tokio::select! {
        _ = client.write(&frame_a) => panic!("write completed too quickly to test cancellation"),
        _ = tokio::time::sleep(std::time::Duration::from_millis(50)) => {}
    }

    // Enqueue the second write before releasing the reader. Both frames
    // are now in the actor's queue; the actor will drain them in order
    // once the reader starts pulling bytes.
    let second_handle = tokio::spawn({
        let frame_b = frame_b.clone();
        let client_arc = std::sync::Arc::new(client);
        let client_clone = client_arc.clone();
        async move { client_clone.write(&frame_b).await }
    });

    // Release the reader so both frames can flow through the actor.
    start.notify_one();

    let frames = reader_task.await.unwrap();
    second_handle.await.unwrap().unwrap();

    assert_eq!(frames.len(), 2);
    assert_eq!(frames[0].method, "first.write");
    assert_eq!(frames[0].id, 100);
    assert_eq!(frames[1].method, "second.write");
    assert_eq!(frames[1].id, 101);
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

    let (notification_tx, _) = broadcast::channel(16);
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
