// Copyright (c) Microsoft Corporation. All rights reserved.

//! Dispatch for shell password prompts (`shellAuthentication.request` and
//! `shellAuthentication.cancel`). Logs only session and request IDs, never prompt
//! text or answers.

use std::collections::HashMap;
use std::panic::AssertUnwindSafe;
use std::sync::Arc;

use futures_util::FutureExt;
use parking_lot::Mutex;
use serde_json::{Value, json};
use tokio_util::sync::CancellationToken;
use tracing::error;

use crate::JsonRpcRequest;
use crate::handler::{
    ShellAuthenticationHandler, ShellAuthenticationInputKind, ShellAuthenticationRequest,
    ShellAuthenticationResponse,
};
use crate::types::{RequestId, SessionId};

pub(crate) const REQUEST_METHOD: &str = "shellAuthentication.request";
pub(crate) const CANCEL_METHOD: &str = "shellAuthentication.cancel";

type PromptKey = (SessionId, RequestId);
type OpenPrompts = HashMap<PromptKey, Arc<CancellationToken>>;

/// Open prompts on one connection, keyed by session and request, so stopping
/// the client can close them all.
#[derive(Default)]
pub(crate) struct PendingShellAuthenticationRequests {
    open: Arc<Mutex<OpenPrompts>>,
}

/// One open prompt. Dropping it closes the prompt.
pub(crate) struct ShellAuthenticationTicket {
    key: PromptKey,
    cancelled: Arc<CancellationToken>,
    open: Arc<Mutex<OpenPrompts>>,
}

impl Drop for ShellAuthenticationTicket {
    fn drop(&mut self) {
        self.cancelled.cancel();
        let mut open = self.open.lock();
        if open
            .get(&self.key)
            .is_some_and(|cancelled| Arc::ptr_eq(cancelled, &self.cancelled))
        {
            open.remove(&self.key);
        }
    }
}

impl PendingShellAuthenticationRequests {
    /// Runs in the session's request loop before the request is spawned, so
    /// a cancellation that follows its request on the wire cannot overtake
    /// it. Returns the open prompt for a new `shellAuthentication.request`; a
    /// duplicate or malformed request gets none and is answered `cancel`.
    pub(crate) fn before_dispatch(
        &self,
        session_id: &SessionId,
        request: &JsonRpcRequest,
        session_closed: &CancellationToken,
    ) -> Option<ShellAuthenticationTicket> {
        let method = request.method.as_str();
        if method != REQUEST_METHOD && method != CANCEL_METHOD {
            return None;
        }
        let request_id = request.params.as_ref()?.get("requestId")?.as_str()?;
        let key = (session_id.clone(), RequestId::new(request_id));
        if method == CANCEL_METHOD {
            let cancelled = self.open.lock().get(&key).cloned();
            if let Some(cancelled) = cancelled {
                cancelled.cancel();
            }
            return None;
        }
        let mut open = self.open.lock();
        if open.contains_key(&key) {
            return None;
        }
        let cancelled = Arc::new(session_closed.child_token());
        open.insert(key.clone(), cancelled.clone());
        Some(ShellAuthenticationTicket {
            key,
            cancelled,
            open: self.open.clone(),
        })
    }

    /// Closes every open prompt, for example when the client stops.
    pub(crate) fn cancel_all(&self) {
        let open = std::mem::take(&mut *self.open.lock());
        for cancelled in open.values() {
            cancelled.cancel();
        }
    }
}

/// Asks the session's handler and returns the wire result. Any failure, a
/// malformed request, or a cancellation answers `cancel`, so the program
/// that asked fails instead of receiving a wrong value.
pub(crate) async fn answer(
    session_id: &SessionId,
    handler: Option<&Arc<dyn ShellAuthenticationHandler>>,
    params: Option<&Value>,
    ticket: Option<ShellAuthenticationTicket>,
    connection_closed: CancellationToken,
) -> Value {
    let response = match (handler, ticket, params.and_then(parse_request)) {
        (Some(handler), Some(ticket), Some(request)) => {
            ask(session_id, handler, request, &ticket, &connection_closed).await
        }
        _ => ShellAuthenticationResponse::Cancel,
    };
    match response {
        ShellAuthenticationResponse::Accept(value) => json!({ "action": "accept", "value": value }),
        ShellAuthenticationResponse::Cancel => json!({ "action": "cancel" }),
    }
}

async fn ask(
    session_id: &SessionId,
    handler: &Arc<dyn ShellAuthenticationHandler>,
    request: ShellAuthenticationRequest,
    ticket: &ShellAuthenticationTicket,
    connection_closed: &CancellationToken,
) -> ShellAuthenticationResponse {
    let cancelled = &ticket.cancelled;
    if cancelled.is_cancelled() || connection_closed.is_cancelled() {
        return ShellAuthenticationResponse::Cancel;
    }
    let request_id = request.request_id.clone();
    let handled =
        AssertUnwindSafe(handler.handle(session_id.clone(), request, cancelled.child_token()))
            .catch_unwind();
    // A handler that never returns after its prompt is cancelled must not
    // keep the request open.
    let response = tokio::select! {
        biased;
        _ = cancelled.cancelled() => ShellAuthenticationResponse::Cancel,
        _ = connection_closed.cancelled() => ShellAuthenticationResponse::Cancel,
        outcome = handled => outcome.unwrap_or_else(|_| {
            error!(
                session_id = %session_id,
                request_id = %request_id,
                "shell authentication handler panicked"
            );
            ShellAuthenticationResponse::Cancel
        }),
    };
    if cancelled.is_cancelled() {
        ShellAuthenticationResponse::Cancel
    } else {
        response
    }
}

fn parse_request(params: &Value) -> Option<ShellAuthenticationRequest> {
    let text = |name: &str| params.get(name).and_then(Value::as_str);
    let input_kind = match text("inputKind")? {
        "secret" => ShellAuthenticationInputKind::Secret,
        "text" => ShellAuthenticationInputKind::Text,
        "confirm" => ShellAuthenticationInputKind::Confirm,
        _ => return None,
    };
    Some(ShellAuthenticationRequest {
        request_id: RequestId::new(text("requestId")?),
        program: text("program")?.to_owned(),
        prompt: text("prompt")?.to_owned(),
        input_kind,
        command: text("command").unwrap_or_default().to_owned(),
        cwd: text("cwd").unwrap_or_default().to_owned(),
    })
}

#[cfg(test)]
mod tests;
