/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/
#![cfg(feature = "test-support")]
#![allow(clippy::unwrap_used)]

use std::sync::Arc;
use std::sync::atomic::{AtomicUsize, Ordering};
use std::time::Duration;

use github_copilot_sdk::ahp::*;
use github_copilot_sdk::test_support::{
    JsonRpcClient, JsonRpcNotification, JsonRpcRequest, JsonRpcResponse,
};
use github_copilot_sdk::{Client, Error, ErrorKind};
use serde_json::{Value, json};
use tokio::sync::{Mutex, Notify, broadcast, mpsc};
use tokio::time::timeout;

const WAIT: Duration = Duration::from_secs(3);

struct Peer {
    client: Client,
    rpc: Arc<JsonRpcClient>,
    calls: Arc<Mutex<Vec<(String, Value)>>>,
    hold_method: Arc<Mutex<Option<String>>>,
    send_started: Arc<Notify>,
    release_send: Arc<Notify>,
}

impl Peer {
    fn new(unsupported: bool) -> Self {
        let (sdk, runtime) = tokio::io::duplex(1024 * 1024);
        let (read, write) = tokio::io::split(sdk);
        let client = Client::from_streams(read, write, std::env::temp_dir()).unwrap();
        let (read, write) = tokio::io::split(runtime);
        let (requests, mut rx) = mpsc::unbounded_channel();
        let rpc = Arc::new(JsonRpcClient::new(
            write,
            read,
            broadcast::channel(64).0,
            requests,
        ));
        let calls = Arc::new(Mutex::new(Vec::new()));
        let hold_method = Arc::new(Mutex::new(None));
        let send_started = Arc::new(Notify::new());
        let release_send = Arc::new(Notify::new());
        tokio::spawn({
            let rpc = rpc.clone();
            let calls = calls.clone();
            let hold_method = hold_method.clone();
            let send_started = send_started.clone();
            let release_send = release_send.clone();
            async move {
                let mut next_id = 0;
                while let Some(request) = rx.recv().await {
                    calls.lock().await.push((
                        request.method.clone(),
                        request.params.clone().unwrap_or(Value::Null),
                    ));
                    let held = hold_method.lock().await.clone();
                    if held.as_deref() == Some(request.method.as_str()) {
                        send_started.notify_one();
                        release_send.notified().await;
                    }
                    let result = match request.method.as_str() {
                        "ahp.registerEndpoint" => json!({"endpointId": "endpoint"}),
                        "ahp.openConnection" => {
                            next_id += 1;
                            json!({"connectionId": format!("connection-{next_id}")})
                        }
                        "ping" => {
                            json!({"message": "pong", "timestamp": "2026-01-01T00:00:00Z", "protocolVersion": 3})
                        }
                        _ => Value::Null,
                    };
                    let error = if unsupported {
                        Some(github_copilot_sdk::test_support::JsonRpcResponse {
                            jsonrpc: "2.0".into(),
                            id: request.id,
                            result: None,
                            error: serde_json::from_value(
                                json!({"code": -32601, "message": "not found"}),
                            )
                            .unwrap(),
                        })
                    } else {
                        None
                    };
                    rpc.write(&error.unwrap_or(JsonRpcResponse {
                        jsonrpc: "2.0".into(),
                        id: request.id,
                        result: Some(result),
                        error: None,
                    }))
                    .await
                    .unwrap();
                }
            }
        });
        Self {
            client,
            rpc,
            calls,
            hold_method,
            send_started,
            release_send,
        }
    }

    async fn callback(&self, method: &str, params: Value) -> JsonRpcResponse {
        timeout(WAIT, self.rpc.send_request(method, Some(params)))
            .await
            .unwrap()
            .unwrap()
    }

    async fn endpoint(&self, options: AhpEndpointOptions) -> AhpEndpoint {
        timeout(WAIT, self.client.create_ahp_endpoint(options))
            .await
            .unwrap()
            .unwrap()
    }
}

impl Drop for Peer {
    fn drop(&mut self) {
        self.client.force_stop();
    }
}

fn messages() -> (AhpConnectionOptions, mpsc::Receiver<String>) {
    let (tx, rx) = mpsc::channel(128);
    (
        AhpConnectionOptions {
            on_message: Arc::new(move |message| {
                let tx = tx.clone();
                Box::pin(async move {
                    tx.send(message)
                        .await
                        .map_err(|e| Error::with_message(ErrorKind::Io, e.to_string()))
                })
            }),
            on_close: None,
        },
        rx,
    )
}

#[tokio::test]
async fn opaque_transport_and_generated_wire_contracts() {
    let peer = Peer::new(false);
    let endpoint = peer.endpoint(AhpEndpointOptions::default()).await;
    let (options, mut received) = messages();
    let connection = endpoint.open_connection(options).await.unwrap();
    let opaque = "{\"notDecoded\":\"🚀\"}";
    connection.send(opaque).await.unwrap();
    let response = peer
        .callback(
            "ahp.message",
            json!({
                "endpointId": endpoint.id(), "connectionId": connection.id(), "message": opaque,
            }),
        )
        .await;
    assert!(response.error.is_none());
    assert_eq!(received.recv().await.unwrap(), opaque);
    endpoint
        .set_capabilities(json!({"custom": true}))
        .await
        .unwrap();
    endpoint.refresh_exposure().await.unwrap();
    connection.close().await.unwrap();
    connection.close().await.unwrap();
    endpoint.dispose().await.unwrap();
    endpoint.dispose().await.unwrap();
    assert!(connection.send("late").await.is_err());
    let calls = peer.calls.lock().await;
    assert_eq!(
        calls[0].1,
        json!({"callbacks": {
            "createSession": false, "resumeSession": false, "listSessions": false, "sessionControl": false,
        }})
    );
    assert_eq!(
        calls.iter().find(|c| c.0 == "ahp.send").unwrap().1["message"],
        opaque
    );
    assert_eq!(
        calls
            .iter()
            .filter(|c| c.0 == "ahp.closeConnection")
            .count(),
        1
    );
    assert_eq!(
        calls
            .iter()
            .filter(|c| c.0 == "ahp.disposeEndpoint")
            .count(),
        1
    );
}

#[tokio::test]
async fn callbacks_are_reentrant_and_keep_policy_local() {
    let peer = Peer::new(false);
    let client = peer.client.clone();
    let endpoint = peer
        .endpoint(AhpEndpointOptions {
            on_create_session: Some(Arc::new(move |request, _| {
                let client = client.clone();
                Box::pin(async move {
                    client.ping(None).await?;
                    assert!(
                        client
                            .create_ahp_endpoint(AhpEndpointOptions::default())
                            .await
                            .is_err()
                    );
                    Ok(AhpSessionIdentity {
                        session_id: request.requested_session_id,
                    })
                })
            })),
            on_resume_session: Some(Arc::new(|request, _| {
                Box::pin(async move {
                    Ok(AhpSessionIdentity {
                        session_id: request.session_id,
                    })
                })
            })),
            on_list_sessions: Some(Arc::new(|(), _| {
                Box::pin(async {
                    Ok(vec![AhpSessionIdentity {
                        session_id: "visible".into(),
                    }])
                })
            })),
            on_session_control: Some(Arc::new(|request, _| {
                Box::pin(async move {
                    assert_eq!(request.kind, "setRemoteControl");
                    Ok(AhpSessionControlResult {
                        applied: false,
                        reason: Some("owner refused".into()),
                        result: Some(json!({"steerable": false})),
                    })
                })
            })),
            ..Default::default()
        })
        .await;
    let connection = endpoint.open_connection(messages().0).await.unwrap();
    for (method, extra) in [
        (
            "ahp.createSession",
            json!({"requestedSessionId": "visible"}),
        ),
        ("ahp.resumeSession", json!({"sessionId": "visible"})),
    ] {
        let mut params = extra;
        params["endpointId"] = json!(endpoint.id());
        params["connectionId"] = json!(connection.id());
        let response = peer.callback(method, params).await;
        assert_eq!(response.result.unwrap()["sessionId"], "visible");
    }
    assert_eq!(
        peer.callback("ahp.listSessions", json!({"endpointId": endpoint.id()}))
            .await
            .result
            .unwrap(),
        json!({"sessionIds": ["visible"]})
    );
    assert_eq!(peer.callback("ahp.sessionControl", json!({
        "endpointId": endpoint.id(), "sessionId": "visible", "kind": "setRemoteControl", "payload": {},
    })).await.result.unwrap()["result"]["steerable"], false);
    endpoint.dispose().await.unwrap();
    assert!(
        peer.callback("ahp.listSessions", json!({"endpointId": endpoint.id()}))
            .await
            .error
            .is_some()
    );
}

#[tokio::test]
async fn output_ack_waits_for_delivery_and_connections_are_independent() {
    let peer = Peer::new(false);
    let endpoint = peer.endpoint(AhpEndpointOptions::default()).await;
    let entered = Arc::new(Notify::new());
    let release = Arc::new(Notify::new());
    let first = endpoint
        .open_connection(AhpConnectionOptions {
            on_message: Arc::new({
                let entered = entered.clone();
                let release = release.clone();
                move |_| {
                    let entered = entered.clone();
                    let release = release.clone();
                    Box::pin(async move {
                        entered.notify_one();
                        release.notified().await;
                        Ok(())
                    })
                }
            }),
            on_close: None,
        })
        .await
        .unwrap();
    let (options, mut messages) = messages();
    let second = endpoint.open_connection(options).await.unwrap();
    let delivery = tokio::spawn({
        let rpc = peer.rpc.clone();
        let params =
            json!({"endpointId": endpoint.id(), "connectionId": first.id(), "message": "held"});
        async move { rpc.send_request("ahp.message", Some(params)).await.unwrap() }
    });
    timeout(WAIT, entered.notified()).await.unwrap();
    assert!(!delivery.is_finished());
    assert!(
        peer.callback(
            "ahp.message",
            json!({
                "endpointId": endpoint.id(), "connectionId": second.id(), "message": "independent",
            })
        )
        .await
        .error
        .is_none()
    );
    assert_eq!(messages.recv().await.unwrap(), "independent");
    release.notify_one();
    assert!(
        timeout(WAIT, delivery)
            .await
            .unwrap()
            .unwrap()
            .error
            .is_none()
    );
    endpoint.dispose().await.unwrap();
}

#[tokio::test]
async fn input_bounds_include_inflight_utf8_bytes_and_close_only_that_connection() {
    let peer = Peer::new(false);
    let endpoint = peer
        .endpoint(AhpEndpointOptions {
            limits: AhpLimits {
                max_message_bytes: 4,
                max_queued_messages: 1,
                max_buffered_bytes: 4,
            },
            ..Default::default()
        })
        .await;
    let first = endpoint.open_connection(messages().0).await.unwrap();
    let second = endpoint.open_connection(messages().0).await.unwrap();
    *peer.hold_method.lock().await = Some("ahp.send".into());
    let send = tokio::spawn({
        let first = first.clone();
        async move { first.send("🚀").await }
    });
    timeout(WAIT, peer.send_started.notified()).await.unwrap();
    assert!(
        first
            .send("a")
            .await
            .unwrap_err()
            .to_string()
            .contains("buffer limit")
    );
    assert!(timeout(WAIT, send).await.unwrap().unwrap().is_err());
    *peer.hold_method.lock().await = None;
    peer.release_send.notify_one();
    second.send("ok").await.unwrap();
    assert!(
        second
            .send("🚀x")
            .await
            .unwrap_err()
            .to_string()
            .contains("size limit")
    );
    endpoint.dispose().await.unwrap();
}

#[tokio::test]
async fn cancellation_and_endpoint_shutdown_abort_policy_without_blocking_router() {
    let peer = Peer::new(false);
    let entered = Arc::new(Notify::new());
    let cancellation = Arc::new(Mutex::new(None));
    let endpoint = peer
        .endpoint(AhpEndpointOptions {
            on_list_sessions: Some(Arc::new({
                let entered = entered.clone();
                let cancellation = cancellation.clone();
                move |(), context| {
                    let entered = entered.clone();
                    let cancellation = cancellation.clone();
                    Box::pin(async move {
                        *cancellation.lock().await = Some(context.cancellation);
                        entered.notify_one();
                        std::future::pending().await
                    })
                }
            })),
            ..Default::default()
        })
        .await;
    let pending = tokio::spawn({
        let rpc = peer.rpc.clone();
        async move {
            rpc.send_request("ahp.listSessions", Some(json!({"endpointId": "endpoint"})))
                .await
                .unwrap()
        }
    });
    timeout(WAIT, entered.notified()).await.unwrap();
    peer.rpc
        .write(&JsonRpcNotification {
            jsonrpc: "2.0".into(),
            method: "$/cancelRequest".into(),
            params: Some(json!({"id": 1})),
        })
        .await
        .unwrap();
    let response = timeout(WAIT, pending).await.unwrap().unwrap();
    assert_eq!(response.error.unwrap().code, -32800);
    assert!(cancellation.lock().await.as_ref().unwrap().is_cancelled());
    endpoint.dispose().await.unwrap();
}

#[tokio::test]
async fn shutdown_notifies_once_and_releases_pending_delivery() {
    let peer = Peer::new(false);
    let endpoint = peer.endpoint(AhpEndpointOptions::default()).await;
    let closed = Arc::new(AtomicUsize::new(0));
    let entered = Arc::new(Notify::new());
    let connection = endpoint
        .open_connection(AhpConnectionOptions {
            on_message: Arc::new({
                let entered = entered.clone();
                move |_| {
                    let entered = entered.clone();
                    Box::pin(async move {
                        entered.notify_one();
                        std::future::pending().await
                    })
                }
            }),
            on_close: Some(Arc::new({
                let closed = closed.clone();
                move |_| {
                    closed.fetch_add(1, Ordering::SeqCst);
                }
            })),
        })
        .await
        .unwrap();
    let pending = tokio::spawn({
        let rpc = peer.rpc.clone();
        let params = json!({"endpointId": endpoint.id(), "connectionId": connection.id(), "message": "pending"});
        async move { rpc.send_request("ahp.message", Some(params)).await.unwrap() }
    });
    timeout(WAIT, entered.notified()).await.unwrap();
    endpoint.dispose().await.unwrap();
    assert!(
        timeout(WAIT, pending)
            .await
            .unwrap()
            .unwrap()
            .error
            .is_some()
    );
    connection.close().await.unwrap();
    assert_eq!(closed.load(Ordering::SeqCst), 1);
}

#[tokio::test]
async fn rejects_invalid_limits_and_reports_unsupported_runtime() {
    let peer = Peer::new(true);
    let result = peer
        .client
        .create_ahp_endpoint(AhpEndpointOptions {
            limits: AhpLimits {
                max_queued_messages: 129,
                ..Default::default()
            },
            ..Default::default()
        })
        .await;
    assert!(result.is_err());
    assert!(peer.calls.lock().await.is_empty());
    let result = peer
        .client
        .create_ahp_endpoint(AhpEndpointOptions::default())
        .await;
    assert!(
        result
            .err()
            .unwrap()
            .to_string()
            .contains("does not support native AHP")
    );
}

#[tokio::test(flavor = "multi_thread", worker_threads = 4)]
async fn output_delivery_preserves_wire_order() {
    let peer = Peer::new(false);
    let endpoint = peer.endpoint(AhpEndpointOptions::default()).await;
    let (options, mut messages) = messages();
    let connection = endpoint.open_connection(options).await.unwrap();
    for id in 100..200 {
        peer.rpc.write(&JsonRpcRequest::new(id, "ahp.message", Some(json!({
            "endpointId": endpoint.id(), "connectionId": connection.id(), "message": id.to_string(),
        })))).await.unwrap();
    }
    for id in 100..200 {
        assert_eq!(
            timeout(WAIT, messages.recv()).await.unwrap().unwrap(),
            id.to_string()
        );
    }
    endpoint.dispose().await.unwrap();
}

#[tokio::test]
async fn output_overflow_cancels_inflight_delivery_and_notifies_once() {
    let peer = Peer::new(false);
    let endpoint = peer
        .endpoint(AhpEndpointOptions {
            limits: AhpLimits {
                max_queued_messages: 1,
                ..Default::default()
            },
            ..Default::default()
        })
        .await;
    let entered = Arc::new(Notify::new());
    let closes = Arc::new(AtomicUsize::new(0));
    let connection = endpoint
        .open_connection(AhpConnectionOptions {
            on_message: Arc::new({
                let entered = entered.clone();
                move |_| {
                    let entered = entered.clone();
                    Box::pin(async move {
                        entered.notify_one();
                        std::future::pending().await
                    })
                }
            }),
            on_close: Some(Arc::new({
                let closes = closes.clone();
                move |error| {
                    assert!(error.unwrap().contains("buffer limit"));
                    closes.fetch_add(1, Ordering::SeqCst);
                }
            })),
        })
        .await
        .unwrap();
    let pending = tokio::spawn({
        let rpc = peer.rpc.clone();
        let params = json!({"endpointId": endpoint.id(), "connectionId": connection.id(), "message": "held"});
        async move { rpc.send_request("ahp.message", Some(params)).await.unwrap() }
    });
    timeout(WAIT, entered.notified()).await.unwrap();
    assert!(
        peer.callback(
            "ahp.message",
            json!({
                "endpointId": endpoint.id(), "connectionId": connection.id(), "message": "overflow",
            })
        )
        .await
        .error
        .unwrap()
        .message
        .contains("buffer limit")
    );
    assert!(
        timeout(WAIT, pending)
            .await
            .unwrap()
            .unwrap()
            .error
            .is_some()
    );
    endpoint.dispose().await.unwrap();
    assert_eq!(closes.load(Ordering::SeqCst), 1);
}

#[tokio::test]
async fn abandoned_registration_and_open_are_cleaned_up() {
    let peer = Peer::new(false);
    for method in ["ahp.registerEndpoint", "ahp.openConnection"] {
        let endpoint = if method == "ahp.openConnection" {
            Some(peer.endpoint(AhpEndpointOptions::default()).await)
        } else {
            None
        };
        *peer.hold_method.lock().await = Some(method.into());
        let abandoned = tokio::spawn({
            let client = peer.client.clone();
            let endpoint = endpoint.clone();
            async move {
                if let Some(endpoint) = endpoint {
                    endpoint.open_connection(messages().0).await.unwrap();
                } else {
                    client
                        .create_ahp_endpoint(AhpEndpointOptions::default())
                        .await
                        .unwrap();
                }
            }
        });
        timeout(WAIT, peer.send_started.notified()).await.unwrap();
        abandoned.abort();
        assert!(abandoned.await.unwrap_err().is_cancelled());
        *peer.hold_method.lock().await = None;
        peer.release_send.notify_one();
        let cleanup = if endpoint.is_some() {
            "ahp.closeConnection"
        } else {
            "ahp.disposeEndpoint"
        };
        timeout(WAIT, async {
            loop {
                if peer.calls.lock().await.iter().any(|c| c.0 == cleanup) {
                    break;
                }
                tokio::task::yield_now().await;
            }
        })
        .await
        .unwrap();
        if let Some(endpoint) = endpoint {
            endpoint.dispose().await.unwrap();
        }
    }
}

#[tokio::test]
async fn back_to_back_cancellation_preserves_request_order() {
    let peer = Peer::new(false);
    let endpoint = peer
        .endpoint(AhpEndpointOptions {
            on_list_sessions: Some(Arc::new(|(), _| Box::pin(std::future::pending()))),
            ..Default::default()
        })
        .await;
    let mut pending = Box::pin(peer.rpc.send_request(
        "ahp.listSessions",
        Some(json!({"endpointId": endpoint.id()})),
    ));
    // Queue the request before the cancellation without waiting for the handler.
    assert!(futures_util::poll!(pending.as_mut()).is_pending());
    peer.rpc
        .write(&JsonRpcNotification {
            jsonrpc: "2.0".into(),
            method: "$/cancelRequest".into(),
            params: Some(json!({"id": 1})),
        })
        .await
        .unwrap();
    let response = timeout(WAIT, pending).await.unwrap().unwrap();
    assert_eq!(response.error.unwrap().code, -32800);
    // A following policy request still works and cannot be blocked by the cancelled one.
    let result = peer.callback("ahp.sessionControl", json!({
        "endpointId": endpoint.id(), "sessionId": "session", "kind": "dispose", "payload": {},
    })).await;
    assert!(
        result
            .error
            .unwrap()
            .message
            .contains("No AHP control callback")
    );
    endpoint.dispose().await.unwrap();
}
