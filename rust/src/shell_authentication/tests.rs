// Copyright (c) Microsoft Corporation. All rights reserved.

#![cfg(test)]

use std::sync::Arc;
use std::sync::atomic::{AtomicBool, Ordering};
use std::time::Duration;

use async_trait::async_trait;
use serde_json::{Value, json};
use tokio::sync::Notify;
use tokio::time::timeout;
use tokio_util::sync::CancellationToken;

use super::{CANCEL_METHOD, PendingShellAuthenticationRequests, REQUEST_METHOD, answer};
use crate::JsonRpcRequest;
use crate::handler::{
    ShellAuthenticationHandler, ShellAuthenticationInputKind, ShellAuthenticationRequest,
    ShellAuthenticationResponse,
};
use crate::types::{RequestId, SessionId};

const TIMEOUT: Duration = Duration::from_secs(2);

fn prompt_params(request_id: &str) -> Value {
    json!({
        "sessionId": "s1",
        "requestId": request_id,
        "program": "git",
        "prompt": "Password for 'https://example.test': ",
        "inputKind": "secret",
        "command": "git fetch",
        "cwd": "/repo",
    })
}

fn rpc(method: &str, params: Value) -> JsonRpcRequest {
    JsonRpcRequest::new(1, method, Some(params))
}

fn cancel_params(request_id: &str) -> Value {
    json!({ "sessionId": "s1", "requestId": request_id })
}

fn session() -> SessionId {
    SessionId::from("s1")
}

struct Answer(ShellAuthenticationResponse);

#[async_trait]
impl ShellAuthenticationHandler for Answer {
    async fn handle(
        &self,
        _session_id: SessionId,
        _request: ShellAuthenticationRequest,
        _cancellation: CancellationToken,
    ) -> ShellAuthenticationResponse {
        self.0.clone()
    }
}

fn accept_handler() -> Arc<dyn ShellAuthenticationHandler> {
    Arc::new(Answer(ShellAuthenticationResponse::Accept(
        "hunter22".to_string(),
    )))
}

/// Waits until cancelled, recording what it saw.
#[derive(Default)]
struct WaitForCancel {
    started: Notify,
    request: parking_lot::Mutex<Option<ShellAuthenticationRequest>>,
    cancellation: parking_lot::Mutex<Option<CancellationToken>>,
}

impl WaitForCancel {
    fn was_cancelled(&self) -> bool {
        self.cancellation
            .lock()
            .as_ref()
            .is_some_and(CancellationToken::is_cancelled)
    }
}

#[async_trait]
impl ShellAuthenticationHandler for WaitForCancel {
    async fn handle(
        &self,
        _session_id: SessionId,
        request: ShellAuthenticationRequest,
        cancellation: CancellationToken,
    ) -> ShellAuthenticationResponse {
        *self.request.lock() = Some(request);
        *self.cancellation.lock() = Some(cancellation);
        self.started.notify_one();
        std::future::pending::<()>().await;
        ShellAuthenticationResponse::Accept("too late".to_string())
    }
}

struct Panics;

#[async_trait]
impl ShellAuthenticationHandler for Panics {
    async fn handle(
        &self,
        _session_id: SessionId,
        _request: ShellAuthenticationRequest,
        _cancellation: CancellationToken,
    ) -> ShellAuthenticationResponse {
        panic!("handler failed");
    }
}

/// Opens prompt `r1` and starts answering it with a handler that waits.
async fn open_waiting_prompt(
    pending: &PendingShellAuthenticationRequests,
    session_closed: &CancellationToken,
    connection_closed: CancellationToken,
    params: Value,
) -> (Arc<WaitForCancel>, tokio::task::JoinHandle<Value>) {
    let ticket = pending.before_dispatch(
        &session(),
        &rpc(REQUEST_METHOD, params.clone()),
        session_closed,
    );
    assert!(ticket.is_some());
    let handler = Arc::new(WaitForCancel::default());
    let dyn_handler: Arc<dyn ShellAuthenticationHandler> = handler.clone();
    let task = tokio::spawn(async move {
        answer(
            &session(),
            Some(&dyn_handler),
            Some(&params),
            ticket,
            connection_closed,
        )
        .await
    });
    timeout(TIMEOUT, handler.started.notified()).await.unwrap();
    (handler, task)
}

async fn finished(task: tokio::task::JoinHandle<Value>) -> Value {
    timeout(TIMEOUT, task).await.unwrap().unwrap()
}

#[tokio::test]
async fn accepted_answer_is_returned() {
    let pending = PendingShellAuthenticationRequests::default();
    let closed = CancellationToken::new();
    let params = prompt_params("r1");
    let ticket = pending.before_dispatch(&session(), &rpc(REQUEST_METHOD, params.clone()), &closed);

    let result = answer(
        &session(),
        Some(&accept_handler()),
        Some(&params),
        ticket,
        closed,
    )
    .await;

    assert_eq!(result, json!({ "action": "accept", "value": "hunter22" }));
    assert!(pending.open.lock().is_empty());
}

#[tokio::test]
async fn handler_receives_the_request_fields() {
    let pending = PendingShellAuthenticationRequests::default();
    let closed = CancellationToken::new();
    let (handler, task) = open_waiting_prompt(
        &pending,
        &closed,
        CancellationToken::new(),
        prompt_params("r1"),
    )
    .await;

    assert_eq!(
        handler.request.lock().clone(),
        Some(ShellAuthenticationRequest {
            request_id: RequestId::new("r1"),
            program: "git".to_string(),
            prompt: "Password for 'https://example.test': ".to_string(),
            input_kind: ShellAuthenticationInputKind::Secret,
            command: "git fetch".to_string(),
            cwd: "/repo".to_string(),
        })
    );
    pending.cancel_all();
    assert_eq!(finished(task).await, json!({ "action": "cancel" }));
}

#[tokio::test]
async fn cancel_request_closes_only_the_matching_prompt() {
    let pending = PendingShellAuthenticationRequests::default();
    let closed = CancellationToken::new();
    let (handler, task) = open_waiting_prompt(
        &pending,
        &closed,
        CancellationToken::new(),
        prompt_params("r1"),
    )
    .await;

    let other_session = SessionId::from("s2");
    pending.before_dispatch(
        &other_session,
        &rpc(CANCEL_METHOD, cancel_params("r1")),
        &closed,
    );
    pending.before_dispatch(
        &session(),
        &rpc(CANCEL_METHOD, cancel_params("r2")),
        &closed,
    );
    assert!(!handler.was_cancelled());

    pending.before_dispatch(
        &session(),
        &rpc(CANCEL_METHOD, cancel_params("r1")),
        &closed,
    );

    assert_eq!(finished(task).await, json!({ "action": "cancel" }));
    assert!(handler.was_cancelled());
    assert!(pending.open.lock().is_empty());
}

#[tokio::test]
async fn cancel_that_follows_its_request_wins_before_the_handler_runs() {
    struct Records(Arc<AtomicBool>);

    #[async_trait]
    impl ShellAuthenticationHandler for Records {
        async fn handle(
            &self,
            _session_id: SessionId,
            _request: ShellAuthenticationRequest,
            _cancellation: CancellationToken,
        ) -> ShellAuthenticationResponse {
            self.0.store(true, Ordering::SeqCst);
            ShellAuthenticationResponse::Accept("hunter22".to_string())
        }
    }

    let pending = PendingShellAuthenticationRequests::default();
    let closed = CancellationToken::new();
    let params = prompt_params("r1");
    let ticket = pending.before_dispatch(&session(), &rpc(REQUEST_METHOD, params.clone()), &closed);
    pending.before_dispatch(
        &session(),
        &rpc(CANCEL_METHOD, cancel_params("r1")),
        &closed,
    );
    let called = Arc::new(AtomicBool::new(false));
    let handler: Arc<dyn ShellAuthenticationHandler> = Arc::new(Records(called.clone()));

    let result = answer(&session(), Some(&handler), Some(&params), ticket, closed).await;

    assert_eq!(result, json!({ "action": "cancel" }));
    assert!(!called.load(Ordering::SeqCst));
}

#[tokio::test]
async fn session_close_connection_close_and_cancel_all_close_open_prompts() {
    for close in ["session", "connection", "all"] {
        let pending = PendingShellAuthenticationRequests::default();
        let session_closed = CancellationToken::new();
        let connection_closed = CancellationToken::new();
        let (handler, task) = open_waiting_prompt(
            &pending,
            &session_closed,
            connection_closed.clone(),
            prompt_params("r1"),
        )
        .await;

        match close {
            "session" => session_closed.cancel(),
            "connection" => connection_closed.cancel(),
            _ => pending.cancel_all(),
        }

        assert_eq!(
            finished(task).await,
            json!({ "action": "cancel" }),
            "{close}"
        );
        assert!(handler.was_cancelled(), "{close}");
        assert!(pending.open.lock().is_empty(), "{close}");
    }
}

#[tokio::test]
async fn duplicate_request_is_cancelled_without_closing_the_first() {
    let pending = PendingShellAuthenticationRequests::default();
    let closed = CancellationToken::new();
    let params = prompt_params("r1");
    let first = pending.before_dispatch(&session(), &rpc(REQUEST_METHOD, params.clone()), &closed);
    let duplicate =
        pending.before_dispatch(&session(), &rpc(REQUEST_METHOD, params.clone()), &closed);
    assert!(first.is_some());
    assert!(duplicate.is_none());
    let handler = accept_handler();

    let result = answer(
        &session(),
        Some(&handler),
        Some(&params),
        duplicate,
        closed.clone(),
    )
    .await;

    assert_eq!(result, json!({ "action": "cancel" }));
    assert_eq!(pending.open.lock().len(), 1);
    assert_eq!(
        answer(&session(), Some(&handler), Some(&params), first, closed).await,
        json!({ "action": "accept", "value": "hunter22" })
    );
    assert!(pending.open.lock().is_empty());
}

#[tokio::test]
async fn missing_handler_panic_and_malformed_requests_cancel() {
    let closed = CancellationToken::new();
    let accept = accept_handler();
    let panics: Arc<dyn ShellAuthenticationHandler> = Arc::new(Panics);
    let mut unknown_kind = prompt_params("r1");
    unknown_kind["inputKind"] = json!("otp");
    let mut no_program = prompt_params("r1");
    no_program.as_object_mut().unwrap().remove("program");
    let cases: [(Option<&Arc<dyn ShellAuthenticationHandler>>, Value); 4] = [
        (None, prompt_params("r1")),
        (Some(&panics), prompt_params("r1")),
        (Some(&accept), unknown_kind),
        (Some(&accept), no_program),
    ];

    for (handler, params) in cases {
        let pending = PendingShellAuthenticationRequests::default();
        let ticket =
            pending.before_dispatch(&session(), &rpc(REQUEST_METHOD, params.clone()), &closed);
        let result = answer(&session(), handler, Some(&params), ticket, closed.clone()).await;
        assert_eq!(result, json!({ "action": "cancel" }));
        assert!(pending.open.lock().is_empty());
    }
}

#[tokio::test]
async fn optional_fields_default_and_other_methods_are_ignored() {
    let pending = PendingShellAuthenticationRequests::default();
    let closed = CancellationToken::new();
    let params = json!({
        "sessionId": "s1",
        "requestId": "r1",
        "program": "ssh",
        "prompt": "Are you sure you want to continue connecting?",
        "inputKind": "confirm",
        "command": 7,
    });
    assert!(
        pending
            .before_dispatch(
                &session(),
                &rpc("userInput.request", params.clone()),
                &closed
            )
            .is_none()
    );
    assert!(pending.open.lock().is_empty());

    let (handler, task) =
        open_waiting_prompt(&pending, &closed, CancellationToken::new(), params).await;

    let request = handler.request.lock().clone().unwrap();
    assert_eq!(request.input_kind, ShellAuthenticationInputKind::Confirm);
    assert_eq!(request.command, "");
    assert_eq!(request.cwd, "");
    pending.cancel_all();
    assert_eq!(finished(task).await, json!({ "action": "cancel" }));
}

#[test]
fn response_debug_never_prints_the_answer() {
    let accepted = format!(
        "{:?}",
        ShellAuthenticationResponse::Accept("hunter22".to_string())
    );
    assert_eq!(accepted, "Accept(<redacted>)");
    assert_eq!(
        format!("{:?}", ShellAuthenticationResponse::Cancel),
        "Cancel"
    );
}
