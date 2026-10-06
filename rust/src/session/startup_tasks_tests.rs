// Copyright (c) Microsoft Corporation. All rights reserved.

#![cfg(test)]

use std::sync::Arc;
use std::time::Duration;

use async_trait::async_trait;
use tokio::io::AsyncReadExt;
use tokio::sync::oneshot;
use tokio::time::timeout;

use super::{
    PendingSessionRegistration, SessionHandlers, StartupTaskState, StartupTasks,
    spawn_startup_tracked,
};

struct PendingNestedCallback(tokio::sync::mpsc::UnboundedSender<()>);

#[async_trait]
impl crate::handler::ElicitationHandler for PendingNestedCallback {
    async fn handle(
        &self,
        _session_id: crate::SessionId,
        _request_id: crate::RequestId,
        _request: crate::types::ElicitationRequest,
    ) -> crate::types::ElicitationResult {
        self.0.send(()).unwrap();
        std::future::pending().await
    }
}

#[async_trait]
impl crate::handler::McpAuthHandler for PendingNestedCallback {
    async fn handle(
        &self,
        _session_id: crate::SessionId,
        _request_id: crate::RequestId,
        _request: crate::handler::McpAuthRequest,
    ) -> crate::handler::McpAuthResult {
        self.0.send(()).unwrap();
        std::future::pending().await
    }
}

#[tokio::test(flavor = "multi_thread", worker_threads = 2)]
async fn aborted_nested_dispatches_terminate_before_wire_fence() {
    check_aborted_nested_dispatches(false).await;
}

#[tokio::test(flavor = "multi_thread", worker_threads = 2)]
async fn cancelled_nested_handlers_do_not_send_fallback_before_outer_abort() {
    check_aborted_nested_dispatches(true).await;
}

async fn check_aborted_nested_dispatches(nested_first: bool) {
    let (client_write, mut server_read) = tokio::io::duplex(8192);
    let (_server_write, client_read) = tokio::io::duplex(8192);
    let client =
        crate::Client::from_streams(client_read, client_write, std::env::temp_dir()).unwrap();
    let (entered_tx, mut entered_rx) = tokio::sync::mpsc::unbounded_channel();
    let handler = Arc::new(PendingNestedCallback(entered_tx));
    let handlers = SessionHandlers {
        permission: None,
        managed_settings_enabled: false,
        elicitation: Some(handler.clone()),
        mcp_auth: Some(handler),
        user_input: None,
        exit_plan_mode: None,
        auto_mode_switch: None,
        skill_provider: None,
        tools: Arc::new(Default::default()),
    };
    let startup_tasks = Arc::new(StartupTasks::default());
    let shutdown = tokio_util::sync::CancellationToken::new();
    let external_tools_shutdown = tokio_util::sync::CancellationToken::new();
    let (event_tx, _) = tokio::sync::broadcast::channel(8);
    let session_id = crate::SessionId::new("nested-shutdown");
    for (event_type, data) in [
        (
            "elicitation.requested",
            serde_json::json!({
                "requestId": "nested-elicitation", "message": "Confirm"
            }),
        ),
        (
            "mcp.oauth_required",
            serde_json::json!({
                "requestId": "nested-oauth", "reason": "initial",
                "serverName": "test-server", "serverUrl": "https://example.com/mcp"
            }),
        ),
    ] {
        let notification = serde_json::from_value(serde_json::json!({
            "sessionId": session_id,
            "event": {
                "id": event_type, "timestamp": "2026-09-25T00:00:00Z",
                "type": event_type, "data": data
            }
        }))
        .unwrap();
        super::handle_notification(
            &session_id,
            &client,
            &handlers,
            &Arc::new(Default::default()),
            notification,
            &Arc::new(parking_lot::Mutex::new(None)),
            &Arc::new(parking_lot::RwLock::new(Default::default())),
            &Arc::new(parking_lot::RwLock::new(Vec::new())),
            &event_tx,
            None,
            &shutdown,
            &external_tools_shutdown,
            &Arc::new(parking_lot::Mutex::new(Default::default())),
            Some(&startup_tasks),
        )
        .await;
        timeout(Duration::from_secs(2), entered_rx.recv())
            .await
            .unwrap()
            .unwrap();
    }
    let tasks = match &*startup_tasks.state.lock() {
        StartupTaskState::Pending(tasks) => tasks.clone(),
        _ => panic!("startup must still own the callback tasks"),
    };
    assert_eq!(
        tasks.len(),
        4,
        "both outer dispatches and nested callbacks must be tracked"
    );
    let late_reply = if nested_first {
        // Freeze cleanup after aborting the nested callbacks but before aborting
        // their parents, deterministically exposing the fallback-response race.
        *startup_tasks.state.lock() = StartupTaskState::Aborted;
        for nested in tasks.iter().skip(1).step_by(2) {
            nested.abort();
        }
        let outcome = timeout(Duration::from_secs(2), async {
            tokio::select! {
                _ = async {
                    while tasks.iter().any(|task| !task.is_finished()) {
                        tokio::task::yield_now().await;
                    }
                } => false,
                byte = server_read.read_u8() => {
                    byte.expect("client transport must remain open");
                    true
                }
            }
        })
        .await;
        // Clean up the still-waiting dispatches even when the regression fails.
        for task in &tasks {
            task.abort();
        }
        outcome
    } else {
        startup_tasks.abort();
        Ok(false)
    };
    timeout(Duration::from_secs(2), async {
        while tasks.iter().any(|task| !task.is_finished()) {
            tokio::task::yield_now().await;
        }
    })
    .await
    .expect("all dispatch futures must terminate before fencing the writer");
    assert!(
        !late_reply.expect("cancelled nested handlers must terminate their dispatches"),
        "cancelled nested handler emitted a fallback RPC before outer abort"
    );

    // is_finished covers both layers, so no woken parent can enqueue a fallback
    // after this marker. The writer actor preserves previously enqueued frames.
    let marker = crate::JsonRpcResponse {
        jsonrpc: "2.0".into(),
        id: 999,
        result: Some(serde_json::json!({ "marker": "dispatches-finished" })),
        error: None,
    };
    client.send_response(&marker).await.unwrap();
    let body = serde_json::to_vec(&marker).unwrap();
    let expected = format!(
        "Content-Length: {}\r\n\r\n{}",
        body.len(),
        String::from_utf8(body).unwrap()
    );
    let mut received = vec![0; expected.len()];
    timeout(
        Duration::from_secs(2),
        server_read.read_exact(&mut received),
    )
    .await
    .unwrap()
    .unwrap();
    assert_eq!(
        received,
        expected.as_bytes(),
        "aborted dispatch emitted a late fallback RPC"
    );
}

#[tokio::test]
async fn deferred_callback_after_guard_drop_cannot_register_or_replace_retry() {
    for retry in [false, true] {
        let (client_write, _server_read) = tokio::io::duplex(8192);
        let (_server_write, client_read) = tokio::io::duplex(8192);
        let client =
            crate::Client::from_streams(client_read, client_write, std::env::temp_dir()).unwrap();
        let stash = Arc::new(parking_lot::Mutex::new(None));
        let (guard, callback) = PendingSessionRegistration::deferred(
            client.clone(),
            stash.clone(),
            tokio_util::sync::CancellationToken::new(),
            tokio_util::sync::CancellationToken::new(),
        );

        // The reader owns the dequeued callback, but has not run it when the caller drops.
        drop(guard);
        let session_id = crate::SessionId::new("deferred-retry");
        let registration = retry.then(|| client.register_session(&session_id));
        let response = crate::JsonRpcResponse {
            jsonrpc: "2.0".into(),
            id: 1,
            result: Some(serde_json::json!({ "sessionId": session_id })),
            error: None,
        };
        assert!(
            callback(&response).is_err(),
            "cancelled callback registered a session"
        );
        assert!(stash.lock().is_none());
        if let Some(registration) = registration {
            client.unregister_session_owned(&session_id, registration.token);
        }
        assert_eq!(client.registered_session_count_for_test(), 0);
    }
}

#[tokio::test(flavor = "multi_thread", worker_threads = 2)]
async fn aborted_startup_rejects_callback_without_polling() {
    let tasks = Arc::new(StartupTasks::default());
    let (completed_tx, completed_rx) = oneshot::channel();
    tasks.abort();
    let task = spawn_startup_tracked(
        async move {
            completed_tx.send(()).unwrap();
        },
        Some(&tasks),
    );
    assert!(task.is_none(), "aborted startup must not spawn a callback");
    assert!(
        timeout(Duration::from_secs(2), completed_rx)
            .await
            .unwrap()
            .is_err(),
        "rejected callback must be dropped without polling"
    );
}

#[tokio::test(flavor = "multi_thread", worker_threads = 2)]
async fn abort_cancels_registered_callbacks() {
    let tasks = Arc::new(StartupTasks::default());
    let (entered_tx, entered_rx) = oneshot::channel();
    let (mut release_tx, release_rx) = oneshot::channel::<()>();
    let task = spawn_startup_tracked(
        async move {
            entered_tx.send(()).unwrap();
            release_rx.await.unwrap();
        },
        Some(&tasks),
    )
    .unwrap();
    timeout(Duration::from_secs(2), entered_rx)
        .await
        .unwrap()
        .unwrap();
    tasks.abort();
    timeout(Duration::from_secs(2), release_tx.closed())
        .await
        .expect("registered callback must be aborted without releasing it");
    assert!(
        timeout(Duration::from_secs(2), task)
            .await
            .unwrap()
            .unwrap_err()
            .is_cancelled()
    );
}

#[tokio::test(flavor = "multi_thread", worker_threads = 2)]
async fn abort_rejects_nested_spawn_from_an_already_polling_parent() {
    let tasks = Arc::new(StartupTasks::default());
    let (entered_tx, entered_rx) = oneshot::channel();
    let (release_tx, release_rx) = std::sync::mpsc::channel();
    let (rejected_tx, rejected_rx) = oneshot::channel();
    let (completed_tx, completed_rx) = oneshot::channel();
    let nested_tasks = tasks.clone();
    let parent = spawn_startup_tracked(
        async move {
            entered_tx.send(()).unwrap();
            // Keep this poll in flight while the other worker aborts startup.
            // An async gate would let Tokio drop the parent before its nested spawn.
            release_rx.recv().unwrap();
            let nested = spawn_startup_tracked(
                async move {
                    completed_tx.send(()).unwrap();
                },
                Some(&nested_tasks),
            );
            rejected_tx.send(nested.is_none()).unwrap();
        },
        Some(&tasks),
    )
    .unwrap();
    timeout(Duration::from_secs(2), entered_rx)
        .await
        .unwrap()
        .unwrap();
    tasks.abort();
    release_tx.send(()).unwrap();
    let rejected = timeout(Duration::from_secs(2), rejected_rx)
        .await
        .unwrap()
        .unwrap();
    // Join before asserting so a failing regression also releases the worker.
    let _ = timeout(Duration::from_secs(2), parent).await.unwrap();
    assert!(
        rejected,
        "already-aborted startup admitted a nested callback"
    );
    assert!(
        timeout(Duration::from_secs(2), completed_rx)
            .await
            .unwrap()
            .is_err(),
        "nested callback ran after startup was aborted"
    );
}

#[tokio::test(flavor = "multi_thread", worker_threads = 2)]
async fn disarm_preserves_pending_and_later_tasks() {
    let tasks = Arc::new(StartupTasks::default());
    let mut gates = Vec::new();
    for after_disarm in [false, true] {
        if after_disarm {
            tasks.disarm();
        }
        let (release_tx, release_rx) = oneshot::channel::<()>();
        let (completed_tx, completed_rx) = oneshot::channel();
        let task = spawn_startup_tracked(
            async move {
                release_rx.await.unwrap();
                completed_tx.send(()).unwrap();
            },
            Some(&tasks),
        )
        .unwrap();
        gates.push((release_tx, completed_rx, task));
    }
    for (release, completed, task) in gates {
        release
            .send(())
            .expect("successful startup must not abort callbacks");
        timeout(Duration::from_secs(2), completed)
            .await
            .unwrap()
            .unwrap();
        timeout(Duration::from_secs(2), task)
            .await
            .unwrap()
            .unwrap();
    }
}
