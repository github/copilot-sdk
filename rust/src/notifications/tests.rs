// Copyright (c) Microsoft Corporation. All rights reserved.

#![cfg(test)]
#![allow(clippy::unwrap_used)]

use std::time::Duration;

use serde_json::{Value, json};
use tokio::io::{AsyncReadExt, AsyncWriteExt, DuplexStream, duplex};
use tokio::sync::{mpsc, oneshot};
use tokio::time::timeout;

use super::*;

const WAIT: Duration = Duration::from_secs(2);

struct Review {
    method: &'static str,
    request: Value,
    context: NotificationContext,
    reply: oneshot::Sender<Result<Value>>,
}

struct Handler(mpsc::UnboundedSender<Review>);

impl Handler {
    async fn invoke(
        &self,
        method: &'static str,
        request: impl serde::Serialize,
        context: NotificationContext,
    ) -> Result<Value> {
        let (reply, result) = oneshot::channel();
        self.0
            .send(Review {
                method,
                request: serde_json::to_value(request).unwrap(),
                context,
                reply,
            })
            .unwrap_or_else(|_| panic!("review receiver closed"));
        result.await.unwrap()
    }
}

#[async_trait]
impl NotificationHandler for Handler {
    async fn get_capabilities(
        &self,
        request: NotificationHostRequest,
        context: NotificationContext,
    ) -> Result<NotificationCapabilitiesResult> {
        Ok(serde_json::from_value(
            self.invoke("getCapabilities", request, context).await?,
        )?)
    }

    async fn request_permission(
        &self,
        request: NotificationHostRequest,
        context: NotificationContext,
    ) -> Result<NotificationPermissionResult> {
        Ok(serde_json::from_value(
            self.invoke("requestPermission", request, context).await?,
        )?)
    }

    async fn show(
        &self,
        request: NotificationHostShowParams,
        context: NotificationContext,
    ) -> Result<NotificationHostShowResult> {
        Ok(serde_json::from_value(
            self.invoke("show", request, context).await?,
        )?)
    }
}

struct Peer {
    client: Client,
    reader: DuplexStream,
    writer: DuplexStream,
    reviews: mpsc::UnboundedReceiver<Review>,
    _directory: tempfile::TempDir,
}

impl Peer {
    fn new(configured: bool, start_router: bool) -> Self {
        let (client_writer, reader) = duplex(32768);
        let (writer, client_reader) = duplex(32768);
        let directory = tempfile::tempdir().unwrap();
        let client =
            Client::from_streams(client_reader, client_writer, directory.path().into()).unwrap();
        let (sender, reviews) = mpsc::unbounded_channel();
        if configured {
            client
                .inner
                .notifications
                .set_handler(Some(Arc::new(Handler(sender))));
            client
                .inner
                .notifications
                .registered
                .store(true, Ordering::Release);
        }
        if start_router {
            client.inner.router.ensure_started(&client.inner);
        }
        Self {
            client,
            reader,
            writer,
            reviews,
            _directory: directory,
        }
    }

    async fn send(&mut self, value: Value) {
        let body = serde_json::to_vec(&value).unwrap();
        self.writer
            .write_all(format!("Content-Length: {}\r\n\r\n", body.len()).as_bytes())
            .await
            .unwrap();
        self.writer.write_all(&body).await.unwrap();
        self.writer.flush().await.unwrap();
    }

    async fn request(&mut self, id: u64, method: &str, params: Value) {
        self.send(json!({ "jsonrpc": "2.0", "id": id, "method": method, "params": params }))
            .await;
    }

    async fn receive(&mut self) -> Value {
        timeout(WAIT, async {
            let mut header = Vec::new();
            while !header.ends_with(b"\r\n\r\n") {
                header.push(self.reader.read_u8().await.unwrap());
            }
            let length = String::from_utf8(header)
                .unwrap()
                .trim()
                .strip_prefix("Content-Length: ")
                .unwrap()
                .parse()
                .unwrap();
            let mut body = vec![0; length];
            self.reader.read_exact(&mut body).await.unwrap();
            serde_json::from_slice(&body).unwrap()
        })
        .await
        .unwrap()
    }

    async fn review(&mut self) -> Review {
        timeout(WAIT, self.reviews.recv()).await.unwrap().unwrap()
    }
}

impl Drop for Peer {
    fn drop(&mut self) {
        self.client.force_stop();
    }
}

fn origin() -> Value {
    json!({
        "sessionId": "synthetic-session",
        "attachmentId": "synthetic-attachment",
        "extension": {
            "id": "project:synthetic",
            "name": "Synthetic",
            "source": "project",
            "modulePath": "/synthetic/extension.mjs"
        }
    })
}

fn show() -> Value {
    json!({
        "context": origin(),
        "notificationId": "synthetic-notification",
        "title": "Synthetic title",
        "body": "Synthetic body",
        "sound": { "kind": "none" },
        "onClick": { "kind": "open-url", "url": "https://example.test/review" }
    })
}

fn capabilities() -> Value {
    json!({
        "status": "available",
        "platform": "darwin",
        "permission": { "extension": "granted", "os": "granted" },
        "onClick": ["open-url", "focus-canvas"],
        "sounds": { "default": true, "none": true, "named": ["Glass"] }
    })
}

#[tokio::test]
async fn callbacks_dispatch_without_a_session_and_preserve_the_wire_shape() {
    let mut peer = Peer::new(true, true);
    for (id, method, params, result) in [
        (
            1,
            "getCapabilities",
            json!({ "context": origin() }),
            capabilities(),
        ),
        (
            2,
            "requestPermission",
            json!({ "context": origin() }),
            json!({
                "status": "completed",
                "permission": { "extension": "denied", "os": "not-required" }
            }),
        ),
        (3, "show", show(), json!({ "status": "accepted" })),
    ] {
        peer.request(id, &format!("notifications.{method}"), params.clone())
            .await;
        let review = peer.review().await;
        assert_eq!(review.method, method);
        assert_eq!(review.request, params);
        review.reply.send(Ok(result.clone())).unwrap();
        let response = peer.receive().await;
        assert_eq!(response["id"], id);
        assert_eq!(response["result"], result);
        assert!(response.get("error").is_none());
    }
}

#[tokio::test]
async fn callback_failures_are_explicit_and_do_not_echo_content() {
    let mut peer = Peer::new(true, true);
    peer.request(1, SHOW_METHOD, show()).await;
    peer.review()
        .await
        .reply
        .send(Err(Error::with_message(
            ErrorKind::InvalidConfig,
            "synthetic-private-error",
        )))
        .unwrap();
    let response = peer.receive().await;
    assert_eq!(response["error"]["code"], error_codes::INTERNAL_ERROR);
    assert!(!response.to_string().contains("synthetic-private-error"));
    assert!(!response.to_string().contains("Synthetic title"));
}

#[tokio::test]
async fn malformed_requests_never_reach_the_handler() {
    let mut peer = Peer::new(true, true);
    let mut request = show();
    request["sound"] = json!({ "kind": "synthetic-private-invalid-kind" });
    peer.request(1, SHOW_METHOD, request).await;
    let response = peer.receive().await;
    assert_eq!(response["error"]["code"], error_codes::INVALID_PARAMS);
    assert!(!response.to_string().contains("synthetic-private"));
    assert!(peer.reviews.try_recv().is_err());
}

#[tokio::test]
async fn missing_handler_returns_an_error_instead_of_success() {
    let mut peer = Peer::new(false, true);
    peer.request(1, SHOW_METHOD, show()).await;
    let response = peer.receive().await;
    assert_eq!(response["error"]["code"], error_codes::METHOD_NOT_FOUND);
}

#[tokio::test]
async fn callbacks_are_disabled_until_host_registration_is_acknowledged() {
    let mut peer = Peer::new(true, true);
    peer.client
        .inner
        .notifications
        .registered
        .store(false, Ordering::Release);
    peer.request(1, SHOW_METHOD, show()).await;
    let response = peer.receive().await;
    assert_eq!(response["error"]["code"], error_codes::METHOD_NOT_FOUND);
    assert!(peer.reviews.try_recv().is_err());
}

#[tokio::test]
async fn cancellation_retires_a_pending_handler_and_signals_spawned_work() {
    let mut peer = Peer::new(true, true);
    peer.request(1, SHOW_METHOD, show()).await;
    let review = peer.review().await;
    let cancellation = review.context.cancellation();
    peer.send(json!({ "jsonrpc": "2.0", "method": "$/cancelRequest", "params": { "id": 1 } }))
        .await;
    let response = peer.receive().await;
    assert_eq!(response["error"]["code"], error_codes::REQUEST_CANCELLED);
    assert!(cancellation.is_cancelled());
    assert!(
        review
            .reply
            .send(Ok(json!({ "status": "accepted" })))
            .is_err()
    );
}

#[tokio::test]
async fn cancellation_before_routing_never_invokes_the_handler() {
    let mut peer = Peer::new(true, false);
    peer.request(1, SHOW_METHOD, show()).await;
    peer.send(json!({ "jsonrpc": "2.0", "method": "$/cancelRequest", "params": { "id": 1 } }))
        .await;

    let client = peer.client.clone();
    let barrier = tokio::spawn(async move { client.call("test.barrier", None).await });
    let request = peer.receive().await;
    peer.send(json!({ "jsonrpc": "2.0", "id": request["id"], "result": {} }))
        .await;
    barrier.await.unwrap().unwrap();
    peer.client.inner.router.ensure_started(&peer.client.inner);

    assert_eq!(
        peer.receive().await["error"]["code"],
        error_codes::REQUEST_CANCELLED
    );
    assert!(peer.reviews.try_recv().is_err());
}

#[tokio::test]
async fn one_pending_callback_does_not_block_another() {
    let mut peer = Peer::new(true, true);
    peer.request(1, SHOW_METHOD, show()).await;
    let pending = peer.review().await;
    peer.request(2, GET_CAPABILITIES_METHOD, json!({ "context": origin() }))
        .await;
    peer.review().await.reply.send(Ok(capabilities())).unwrap();
    assert_eq!(peer.receive().await["id"], 2);
    pending
        .reply
        .send(Ok(json!({ "status": "denied" })))
        .unwrap();
    assert_eq!(peer.receive().await["id"], 1);
}

#[tokio::test]
async fn cancelling_a_context_child_does_not_cancel_the_request() {
    let mut peer = Peer::new(true, true);
    peer.request(1, SHOW_METHOD, show()).await;
    let review = peer.review().await;
    review.context.cancellation().cancel();
    assert!(!review.context.cancellation().is_cancelled());
    review
        .reply
        .send(Ok(json!({ "status": "accepted" })))
        .unwrap();
    assert_eq!(peer.receive().await["result"]["status"], "accepted");
}

#[tokio::test]
async fn force_stop_cancels_pending_host_work() {
    let mut peer = Peer::new(true, true);
    peer.request(1, SHOW_METHOD, show()).await;
    let review = peer.review().await;
    let cancellation = review.context.cancellation();
    peer.client.force_stop();
    timeout(WAIT, cancellation.cancelled()).await.unwrap();
}

#[tokio::test]
async fn registration_acknowledgement_is_explicit() {
    let mut peer = Peer::new(true, true);
    let client = peer.client.clone();
    let registration = tokio::spawn(async move { client.register_notification_host().await });
    let request = peer.receive().await;
    assert_eq!(request["method"], "notifications.registerHost");
    assert!(peer.client.notification_host_registration().is_none());
    peer.send(json!({
        "jsonrpc": "2.0",
        "id": request["id"],
        "result": { "status": "registered" }
    }))
    .await;
    registration.await.unwrap().unwrap();
    assert_eq!(
        serde_json::to_value(peer.client.notification_host_registration()).unwrap(),
        json!({ "status": "registered" })
    );
}

#[tokio::test]
async fn old_runtime_registration_is_explicitly_unsupported() {
    let mut peer = Peer::new(true, true);
    let client = peer.client.clone();
    let registration = tokio::spawn(async move { client.register_notification_host().await });
    let request = peer.receive().await;
    peer.send(json!({
        "jsonrpc": "2.0",
        "id": request["id"],
        "error": { "code": -32601, "message": "Method not found" }
    }))
    .await;
    registration.await.unwrap().unwrap();
    assert_eq!(
        serde_json::to_value(peer.client.notification_host_registration()).unwrap(),
        json!({ "status": "unsupported" })
    );
}

#[tokio::test]
async fn unregisters_before_closing_a_registered_host() {
    let mut peer = Peer::new(true, true);
    let client = peer.client.clone();
    let registration = tokio::spawn(async move { client.register_notification_host().await });
    let request = peer.receive().await;
    peer.send(json!({
        "jsonrpc": "2.0", "id": request["id"], "result": { "status": "registered" }
    }))
    .await;
    registration.await.unwrap().unwrap();

    let client = peer.client.clone();
    let stop = tokio::spawn(async move { client.stop().await });
    let request = peer.receive().await;
    assert_eq!(request["method"], "notifications.unregisterHost");
    peer.send(json!({
        "jsonrpc": "2.0", "id": request["id"], "result": { "status": "unregistered" }
    }))
    .await;
    stop.await.unwrap().unwrap();
}

#[tokio::test]
async fn focus_activation_uses_the_original_session_not_the_current_session() {
    let mut peer = Peer::new(true, true);
    let mut request = show();
    request["onClick"] = json!({ "kind": "focus-canvas", "activationId": "synthetic-focus" });
    peer.request(1, SHOW_METHOD, request).await;
    let review = peer.review().await;
    review
        .reply
        .send(Ok(json!({ "status": "accepted" })))
        .unwrap();
    peer.receive().await;

    let activation =
        tokio::spawn(async move { review.context.activate_canvas("synthetic-focus").await });
    let request = peer.receive().await;
    assert_eq!(request["method"], "session.notifications.activate");
    assert_eq!(
        request["params"],
        json!({ "sessionId": "synthetic-session", "activationId": "synthetic-focus" })
    );
    peer.send(json!({
        "jsonrpc": "2.0", "id": request["id"], "result": { "status": "unavailable" }
    }))
    .await;
    assert_eq!(
        serde_json::to_value(activation.await.unwrap().unwrap()).unwrap(),
        json!({ "status": "unavailable" })
    );
}

#[tokio::test]
async fn retained_focus_context_does_not_keep_the_client_alive() {
    let mut peer = Peer::new(true, true);
    peer.request(1, SHOW_METHOD, show()).await;
    let review = peer.review().await;
    review
        .reply
        .send(Ok(json!({ "status": "accepted" })))
        .unwrap();
    peer.receive().await;
    drop(peer);

    timeout(WAIT, async {
        while review.context.client.upgrade().is_some() {
            tokio::task::yield_now().await;
        }
    })
    .await
    .unwrap();
    assert!(
        review
            .context
            .activate_canvas("synthetic-focus")
            .await
            .is_err()
    );
}
