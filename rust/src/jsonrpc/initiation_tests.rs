/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use std::sync::OnceLock;
use std::sync::atomic::AtomicBool;
use std::time::Duration;

use serde_json::json;

use super::*;

struct Sink {
    frames: Mutex<Vec<Value>>,
    pending: OnceLock<Arc<RwLock<HashMap<u64, PendingRequest>>>>,
    replies: mpsc::UnboundedSender<Vec<u8>>,
    fail: AtomicBool,
    reply: bool,
}

struct Peer {
    rpc: JsonRpcClient,
    sink: Arc<Sink>,
    replies: Option<JoinHandle<()>>,
}

impl Peer {
    fn new(reply: bool) -> Self {
        let (reader, mut writer) = tokio::io::duplex(8192);
        let (tx, mut rx) = mpsc::unbounded_channel::<Vec<u8>>();
        let sink = Arc::new(Sink {
            frames: Mutex::new(Vec::new()),
            pending: OnceLock::new(),
            replies: tx,
            fail: AtomicBool::new(false),
            reply,
        });
        let sender = sink.clone();
        let (notification_tx, _) = broadcast::channel(16);
        let (request_tx, _) = mpsc::unbounded_channel();
        let rpc = JsonRpcClient::new_with_host_notifications(
            MessageWriter::Synchronous(Arc::new(move |frame| {
                let separator = frame
                    .windows(4)
                    .position(|bytes| bytes == b"\r\n\r\n")
                    .expect("complete frame header");
                let header = std::str::from_utf8(&frame[..separator]).unwrap();
                let body = &frame[separator + 4..];
                assert_eq!(header, format!("Content-Length: {}", body.len()));
                let value: Value = serde_json::from_slice(body).unwrap();
                if value.get("method").is_some()
                    && let Some(id) = value["id"].as_u64()
                {
                    assert!(
                        sender.pending.get().unwrap().read().contains_key(&id),
                        "response registration must precede synchronous submission"
                    );
                }
                sender.frames.lock().push(value.clone());
                if sender.fail.load(Ordering::SeqCst) {
                    return Err(std::io::Error::new(
                        std::io::ErrorKind::BrokenPipe,
                        "test synchronous write failure",
                    ));
                }
                if sender.reply
                    && value.get("method").is_some()
                    && let Some(id) = value["id"].as_u64()
                {
                    let response = JsonRpcResponse {
                        jsonrpc: "2.0".into(),
                        id,
                        result: Some(json!({"method": value["method"]})),
                        error: None,
                    };
                    sender
                        .replies
                        .send(encode_frame(&response).unwrap().as_bytes().to_vec())
                        .unwrap();
                }
                Ok(())
            })),
            reader,
            notification_tx,
            request_tx,
            None,
        );
        assert!(sink.pending.set(rpc.pending_requests.clone()).is_ok());
        let replies = tokio::spawn(async move {
            while let Some(frame) = rx.recv().await {
                if writer.write_all(&frame).await.is_err() {
                    break;
                }
            }
        });
        Self {
            rpc,
            sink,
            replies: Some(replies),
        }
    }

    async fn finish(mut self) {
        self.rpc.force_close();
        let task = self.replies.take().unwrap();
        task.abort();
        let _ = task.await;
    }
}

impl Drop for Peer {
    fn drop(&mut self) {
        self.rpc.force_close();
        if let Some(task) = self.replies.take() {
            task.abort();
        }
    }
}

#[tokio::test]
async fn initiation_submits_before_return_and_unpolled_drop_removes_registration() {
    let peer = Peer::new(false);
    let pending = peer
        .rpc
        .begin_request_with_inline_callback("first", Some(json!({"value": 1})), None)
        .unwrap();

    assert_eq!(peer.sink.frames.lock().len(), 1);
    assert_eq!(peer.sink.frames.lock()[0]["params"], json!({"value": 1}));
    assert_eq!(peer.rpc.pending_requests.read().len(), 1);
    drop(pending);
    assert!(peer.rpc.pending_requests.read().is_empty());
    assert_eq!(
        peer.sink.frames.lock().len(),
        1,
        "accepted frame remains sent"
    );
    peer.finish().await;
}

#[tokio::test]
async fn unpolled_send_and_wait_drop_cancels_only_its_completion_wait() {
    let peer = Peer::new(false);
    let pending = peer
        .rpc
        .begin_request_with_inline_callback("session.sendAndWait", None, None)
        .unwrap();
    let request_id = peer.sink.frames.lock()[0]["id"].clone();
    drop(pending);

    assert!(peer.rpc.pending_requests.read().is_empty());
    let frames = peer.sink.frames.lock().clone();
    assert_eq!(frames.len(), 2);
    assert_eq!(frames[0]["method"], "session.sendAndWait");
    assert_eq!(frames[1]["method"], "$/cancelRequest");
    assert_eq!(frames[1]["params"], json!({"id": request_id}));
    assert!(frames[1].get("id").is_none());
    peer.finish().await;
}

#[tokio::test]
async fn delivered_send_and_wait_reply_is_not_cancelled_by_unpolled_drop() {
    let peer = Peer::new(true);
    let (observed, callback) = oneshot::channel();
    let pending = peer
        .rpc
        .begin_request_with_inline_callback(
            "session.sendAndWait",
            None,
            Some(Box::new(move |_| {
                observed.send(()).unwrap();
                Ok(())
            })),
        )
        .unwrap();
    tokio::time::timeout(Duration::from_secs(5), callback)
        .await
        .unwrap()
        .unwrap();
    drop(pending);

    assert!(peer.rpc.pending_requests.read().is_empty());
    assert_eq!(peer.sink.frames.lock().len(), 1);
    peer.finish().await;
}

#[tokio::test]
async fn regular_request_remains_lazy() {
    let peer = Peer::new(false);
    let pending = peer
        .rpc
        .send_request_with_inline_callback("lazy", None, None);
    assert!(peer.sink.frames.lock().is_empty());
    assert!(peer.rpc.pending_requests.read().is_empty());
    drop(pending);
    assert!(peer.sink.frames.lock().is_empty());
    peer.finish().await;
}

#[tokio::test]
async fn inline_reply_runs_callback_before_completion_is_polled() {
    let peer = Peer::new(true);
    let (observed, callback) = oneshot::channel();
    let pending = peer
        .rpc
        .begin_request_with_inline_callback(
            "inline",
            None,
            Some(Box::new(move |response| {
                observed.send(response.result.clone()).unwrap();
                Ok(())
            })),
        )
        .unwrap();

    assert_eq!(
        tokio::time::timeout(Duration::from_secs(5), callback)
            .await
            .unwrap()
            .unwrap(),
        Some(json!({"method": "inline"}))
    );
    assert_eq!(
        pending.await.unwrap().result,
        Some(json!({"method": "inline"}))
    );
    assert!(peer.rpc.pending_requests.read().is_empty());
    peer.finish().await;
}

#[tokio::test]
async fn immediate_write_error_removes_pending_registration() {
    let peer = Peer::new(false);
    peer.sink.fail.store(true, Ordering::SeqCst);
    let error = match peer
        .rpc
        .begin_request_with_inline_callback("failed", None, None)
    {
        Ok(_) => panic!("synchronous submission must fail before returning a future"),
        Err(error) => error,
    };
    assert_eq!(error.kind(), &ErrorKind::Io);
    assert!(peer.rpc.pending_requests.read().is_empty());
    assert_eq!(peer.sink.frames.lock().len(), 1);
    peer.finish().await;
}

#[tokio::test]
async fn all_frame_kinds_use_the_same_synchronous_sender() {
    let peer = Peer::new(true);
    let first = peer
        .rpc
        .begin_request_with_inline_callback("first", None, None)
        .unwrap();
    peer.rpc
        .enqueue_response(&JsonRpcResponse {
            jsonrpc: "2.0".into(),
            id: 42,
            result: Some(json!("response")),
            error: None,
        })
        .unwrap();
    peer.rpc
        .write(&JsonRpcNotification {
            jsonrpc: "2.0".into(),
            method: "notification".into(),
            params: None,
        })
        .await
        .unwrap();
    peer.rpc.send_request("last", None).await.unwrap();
    first.await.unwrap();
    let frames = peer.sink.frames.lock().clone();
    assert_eq!(frames.len(), 4);
    assert_eq!(frames[0]["method"], "first");
    assert_eq!(frames[1]["result"], "response");
    assert_eq!(frames[2]["method"], "notification");
    assert_eq!(frames[3]["method"], "last");
    peer.finish().await;
}

#[tokio::test]
async fn close_cancels_completion_and_rejects_new_submission() {
    let peer = Peer::new(false);
    let pending = peer
        .rpc
        .begin_request_with_inline_callback("pending", None, None)
        .unwrap();
    peer.rpc.force_close();
    assert!(
        peer.rpc
            .begin_request_with_inline_callback("closed", None, None)
            .is_err()
    );
    let error = pending.await.unwrap_err();
    assert_eq!(
        error.kind(),
        &ErrorKind::Protocol(ProtocolErrorKind::RequestCancelled)
    );
    assert_eq!(peer.sink.frames.lock().len(), 1);
    assert!(peer.rpc.pending_requests.read().is_empty());
    peer.finish().await;
}

#[tokio::test]
async fn stream_initiation_enqueues_without_polling_completion() {
    let (writer, reader) = tokio::io::duplex(8192);
    let (_peer_writer, client_reader) = tokio::io::duplex(8192);
    let (notification_tx, _) = broadcast::channel(16);
    let (request_tx, _) = mpsc::unbounded_channel();
    let rpc = JsonRpcClient::new(writer, client_reader, notification_tx, request_tx);
    let pending = rpc
        .begin_request_with_inline_callback("queued", None, None)
        .unwrap();
    let mut reader = BufReader::new(reader);
    let request = tokio::time::timeout(
        Duration::from_secs(5),
        JsonRpcClient::read_message(&mut reader),
    )
    .await
    .unwrap()
    .unwrap()
    .unwrap();
    assert!(matches!(request, JsonRpcMessage::Request(request) if request.method == "queued"));
    drop(pending);
    assert!(rpc.pending_requests.read().is_empty());
    rpc.force_close();
}

#[tokio::test]
async fn unpolled_public_completion_cleans_up_without_retracting_stream_frame() {
    let (writer, reader) = tokio::io::duplex(8192);
    let (_peer_writer, client_reader) = tokio::io::duplex(8192);
    let client =
        crate::Client::from_streams(client_reader, writer, std::path::PathBuf::from(".")).unwrap();
    let pending = client.begin_call("accepted", None).unwrap();
    assert_eq!(client.inner.rpc.pending_requests.read().len(), 1);
    drop(pending);
    assert!(client.inner.rpc.pending_requests.read().is_empty());

    let mut reader = BufReader::new(reader);
    let request = tokio::time::timeout(
        Duration::from_secs(5),
        JsonRpcClient::read_message(&mut reader),
    )
    .await
    .unwrap()
    .unwrap()
    .unwrap();
    assert!(matches!(request, JsonRpcMessage::Request(request) if request.method == "accepted"));
    client.force_stop();
}
