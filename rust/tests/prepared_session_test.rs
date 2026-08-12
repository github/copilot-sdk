//! Early event subscription via `Client::prepare_session` /
//! `Client::prepare_resume_session`.
//!
//! Every test drives the SDK over an in-memory duplex transport and a
//! hand-rolled JSON-RPC peer, so event ordering is deterministic. Timeouts
//! are failure backstops only — no test sleeps to "let things settle".

#![allow(clippy::unwrap_used)]

use std::marker::PhantomData;
use std::time::Duration;

use github_copilot_sdk::session::PreparedSession;
use github_copilot_sdk::subscription::{EventSubscription, RecvErrorKind};
use github_copilot_sdk::types::{
    CloudSessionOptions, CloudSessionRepository, ResumeSessionConfig, SessionConfig, SessionId,
};
use github_copilot_sdk::{Client, ErrorKind, SessionErrorKind};
use serde_json::{Value, json};
use tokio::io::{AsyncRead, AsyncWrite, AsyncWriteExt, duplex};
use tokio::time::timeout;

/// Failure backstop for operations that must complete promptly.
const TIMEOUT: Duration = Duration::from_secs(5);
/// Backstop for asserting that something does *not* happen.
const QUIET: Duration = Duration::from_millis(150);
/// Size of the pre-response event burst. Mirrors the copilot-host startup
/// burst that motivated the API.
const BURST: usize = 600;

// ---------------------------------------------------------------------------
// Transport harness
// ---------------------------------------------------------------------------

async fn write_framed(writer: &mut (impl AsyncWrite + Unpin), body: &[u8]) {
    let header = format!("Content-Length: {}\r\n\r\n", body.len());
    writer.write_all(header.as_bytes()).await.unwrap();
    writer.write_all(body).await.unwrap();
    writer.flush().await.unwrap();
}

async fn read_framed(reader: &mut (impl AsyncRead + Unpin)) -> Value {
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
    let mut buf = vec![0u8; length];
    tokio::io::AsyncReadExt::read_exact(reader, &mut buf)
        .await
        .unwrap();
    serde_json::from_slice(&buf).unwrap()
}

struct FakeServer {
    read: tokio::io::DuplexStream,
    write: tokio::io::DuplexStream,
}

impl FakeServer {
    async fn read_request(&mut self) -> Value {
        timeout(TIMEOUT, read_framed(&mut self.read)).await.unwrap()
    }

    async fn expect_quiet(&mut self) {
        assert!(
            timeout(QUIET, read_framed(&mut self.read)).await.is_err(),
            "expected no wire traffic"
        );
    }

    async fn respond(&mut self, request: &Value, result: Value) {
        let id = request["id"].as_u64().unwrap();
        let response = json!({ "jsonrpc": "2.0", "id": id, "result": result });
        write_framed(&mut self.write, &serde_json::to_vec(&response).unwrap()).await;
    }

    async fn respond_error(&mut self, request: &Value, code: i64, message: &str) {
        let id = request["id"].as_u64().unwrap();
        let response = json!({
            "jsonrpc": "2.0",
            "id": id,
            "error": { "code": code, "message": message },
        });
        write_framed(&mut self.write, &serde_json::to_vec(&response).unwrap()).await;
    }

    async fn send_event(&mut self, session_id: &str, id: &str, event_type: &str, ephemeral: bool) {
        let notification = json!({
            "jsonrpc": "2.0",
            "method": "session.event",
            "params": {
                "sessionId": session_id,
                "event": {
                    "id": id,
                    "timestamp": "2025-01-01T00:00:00Z",
                    "ephemeral": ephemeral,
                    "type": event_type,
                    "data": {},
                },
            },
        });
        write_framed(&mut self.write, &serde_json::to_vec(&notification).unwrap()).await;
    }

    /// Emit the startup burst the host cares about: `BURST` ordered events
    /// followed by an ephemeral `session.idle` that `getMessages` could
    /// never recover.
    async fn send_startup_burst(&mut self, session_id: &str) {
        for i in 0..BURST {
            self.send_event(
                session_id,
                &format!("evt-{i}"),
                "assistant.message_delta",
                false,
            )
            .await;
        }
        self.send_event(session_id, "evt-idle", "session.idle", true)
            .await;
    }

    /// Answer the best-effort `session.skills.reload` that follows a resume.
    async fn answer_skills_reload(&mut self) {
        let request = self.read_request().await;
        assert_eq!(request["method"], "session.skills.reload");
        self.respond(&request, json!({})).await;
    }
}

fn make_client() -> (Client, FakeServer) {
    let (client_write, server_read) = duplex(1 << 20);
    let (server_write, client_read) = duplex(1 << 20);
    let client = Client::from_streams(client_read, client_write, std::env::temp_dir()).unwrap();
    (
        client,
        FakeServer {
            read: server_read,
            write: server_write,
        },
    )
}

fn cloud_options() -> CloudSessionOptions {
    CloudSessionOptions::with_repository(CloudSessionRepository::new("octocat", "hello-world"))
}

fn create_result(session_id: &str) -> Value {
    json!({ "sessionId": session_id, "workspacePath": "/tmp/workspace" })
}

/// Collect the startup burst, asserting each event arrives exactly once and
/// in emission order.
async fn expect_startup_burst(events: &mut EventSubscription) {
    for i in 0..BURST {
        let event = timeout(TIMEOUT, events.recv())
            .await
            .unwrap_or_else(|_| panic!("timed out waiting for event {i}"))
            .unwrap_or_else(|error| panic!("event {i} not delivered: {error}"));
        assert_eq!(event.id.as_str(), format!("evt-{i}"), "out-of-order event");
    }
    let idle = timeout(TIMEOUT, events.recv()).await.unwrap().unwrap();
    assert_eq!(idle.id.as_str(), "evt-idle");
    assert_eq!(idle.event_type, "session.idle");
    assert_eq!(idle.ephemeral, Some(true));
}

/// Unwrap the error arm of a result whose `Ok` type is not `Debug`.
fn expect_error<T>(result: Result<T, github_copilot_sdk::Error>) -> github_copilot_sdk::Error {
    match result {
        Ok(_) => panic!("expected an error"),
        Err(error) => error,
    }
}

/// Poll (bounded) until the client's router has no registered sessions.
///
/// Diagnostics report how many registrations are outstanding rather than
/// which ones: session IDs are not written to test output.
async fn await_no_registrations(client: &Client) {
    let deadline = tokio::time::Instant::now() + TIMEOUT;
    loop {
        let outstanding = client.registered_session_ids_for_test().len();
        if outstanding == 0 {
            return;
        }
        assert!(
            tokio::time::Instant::now() < deadline,
            "{outstanding} session registration(s) were never cleaned up"
        );
        tokio::task::yield_now().await;
    }
}

/// Assert the subscription is closed (producer gone), tolerating any events
/// buffered before the close.
async fn expect_closed(events: &mut EventSubscription) {
    loop {
        match timeout(TIMEOUT, events.recv()).await.unwrap() {
            Ok(_) => continue,
            Err(error) => {
                assert!(
                    matches!(error.kind(), RecvErrorKind::Closed),
                    "expected Closed, got {:?}",
                    error.kind()
                );
                return;
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 1 + 4. Loss-free startup events on create
// ---------------------------------------------------------------------------

/// Subscription installed before `start()` is polled, drained concurrently:
/// the full pre-response burst plus the ephemeral `session.idle` arrives.
#[tokio::test]
async fn prepared_create_delivers_pre_response_burst_to_concurrent_drain() {
    let (client, mut server) = make_client();
    let session_id = SessionId::new("prepared-create-concurrent");

    let prepared = client
        .prepare_session(
            SessionConfig::default()
                .with_session_id(session_id.clone())
                .with_event_buffer_capacity(2048),
        )
        .unwrap();
    let mut events = prepared.subscribe();
    let drain = tokio::spawn(async move {
        expect_startup_burst(&mut events).await;
    });

    let start = tokio::spawn(prepared.start());

    let create_req = server.read_request().await;
    assert_eq!(create_req["method"], "session.create");
    server.send_startup_burst(session_id.as_str()).await;
    server
        .respond(&create_req, create_result(session_id.as_str()))
        .await;

    let session = timeout(TIMEOUT, start).await.unwrap().unwrap().unwrap();
    timeout(TIMEOUT, drain).await.unwrap().unwrap();
    drop(session);
}

/// A large configured buffer retains the whole burst even when the consumer
/// does not read anything until `start()` has returned.
#[tokio::test]
async fn prepared_create_retains_burst_for_deferred_consumer() {
    let (client, mut server) = make_client();
    let session_id = SessionId::new("prepared-create-deferred");

    let prepared = client
        .prepare_session(
            SessionConfig::default()
                .with_session_id(session_id.clone())
                .with_event_buffer_capacity(2048),
        )
        .unwrap();
    let mut events = prepared.subscribe();
    let start = tokio::spawn(prepared.start());

    let create_req = server.read_request().await;
    server.send_startup_burst(session_id.as_str()).await;
    server
        .respond(&create_req, create_result(session_id.as_str()))
        .await;

    let session = timeout(TIMEOUT, start).await.unwrap().unwrap().unwrap();
    // Only now does the consumer start reading.
    expect_startup_burst(&mut events).await;
    drop(session);
}

// ---------------------------------------------------------------------------
// 2. Loss-free startup events on resume
// ---------------------------------------------------------------------------

#[tokio::test]
async fn prepared_resume_delivers_pre_response_burst() {
    let (client, mut server) = make_client();
    let session_id = SessionId::new("prepared-resume");

    let prepared = client
        .prepare_resume_session(
            ResumeSessionConfig::new(session_id.clone())
                .with_continue_pending_work(true)
                .with_event_buffer_capacity(2048),
        )
        .unwrap();
    let mut events = prepared.subscribe();
    let start = tokio::spawn(prepared.start());

    let resume_req = server.read_request().await;
    assert_eq!(resume_req["method"], "session.resume");
    assert_eq!(resume_req["params"]["continuePendingWork"], true);
    server.send_startup_burst(session_id.as_str()).await;
    server
        .respond(&resume_req, json!({ "sessionId": session_id.as_str() }))
        .await;
    server.answer_skills_reload().await;

    let session = timeout(TIMEOUT, start).await.unwrap().unwrap().unwrap();
    expect_startup_burst(&mut events).await;
    drop(session);
}

// ---------------------------------------------------------------------------
// 3. Lag is observable, never silent
// ---------------------------------------------------------------------------

/// An undersized buffer with a consumer that does not drain surfaces
/// `Lagged` rather than silently losing events, and the live tail stays
/// consumable afterwards.
#[tokio::test]
async fn undersized_buffer_reports_lag_and_keeps_live_tail() {
    let (client, mut server) = make_client();
    let session_id = SessionId::new("prepared-lag");

    let prepared = client
        .prepare_session(
            SessionConfig::default()
                .with_session_id(session_id.clone())
                .with_event_buffer_capacity(8),
        )
        .unwrap();
    let mut events = prepared.subscribe();
    let start = tokio::spawn(prepared.start());

    let create_req = server.read_request().await;
    server.send_startup_burst(session_id.as_str()).await;
    server
        .respond(&create_req, create_result(session_id.as_str()))
        .await;
    let session = timeout(TIMEOUT, start).await.unwrap().unwrap().unwrap();

    // Drain until lag is reported. Every delivered event is still in order,
    // and the loss is explicit rather than silent.
    let mut lagged = None;
    let mut last_index: Option<usize> = None;
    while lagged.is_none() {
        match timeout(TIMEOUT, events.recv()).await.unwrap() {
            Ok(event) => {
                if let Some(index) = event.id.as_str().strip_prefix("evt-")
                    && let Ok(index) = index.parse::<usize>()
                {
                    if let Some(previous) = last_index {
                        assert!(index > previous, "delivered events must stay ordered");
                    }
                    last_index = Some(index);
                }
            }
            Err(error) => match error.kind() {
                RecvErrorKind::Lagged(lag) => lagged = Some(lag.skipped()),
                other => panic!("expected lag, got {other:?}"),
            },
        }
    }
    assert!(lagged.unwrap() > 0, "lag must report the skipped count");

    // The live tail is still consumable after a lag.
    server
        .send_event(session_id.as_str(), "evt-live", "assistant.message", false)
        .await;
    let live = loop {
        match timeout(TIMEOUT, events.recv()).await.unwrap() {
            Ok(event) if event.id.as_str() == "evt-live" => break event,
            Ok(_) => continue,
            Err(error) => match error.kind() {
                RecvErrorKind::Lagged(_) => continue,
                other => panic!("subscription ended before the live tail: {other:?}"),
            },
        }
    };
    assert_eq!(live.event_type, "assistant.message");
    drop(session);
}

// ---------------------------------------------------------------------------
// 5 + 6. Inertness before start, and drop of an unstarted handle
// ---------------------------------------------------------------------------

#[tokio::test]
async fn prepare_is_inert_until_start_is_polled() {
    let (client, mut server) = make_client();
    let session_id = SessionId::new("prepared-inert");

    let tasks_before = tokio::runtime::Handle::current()
        .metrics()
        .num_alive_tasks();
    let prepared = client
        .prepare_session(SessionConfig::default().with_session_id(session_id.clone()))
        .unwrap();
    let _events = prepared.subscribe();

    // No wire traffic, no router registration, no spawned task.
    server.expect_quiet().await;
    assert!(client.registered_session_ids_for_test().is_empty());
    assert_eq!(
        tokio::runtime::Handle::current()
            .metrics()
            .num_alive_tasks(),
        tasks_before,
        "prepare must not spawn a task"
    );

    // Constructing the future is still inert; only polling it does work.
    let start = prepared.start();
    server.expect_quiet().await;
    assert!(client.registered_session_ids_for_test().is_empty());

    let start = tokio::spawn(start);
    let create_req = server.read_request().await;
    assert_eq!(create_req["method"], "session.create");
    server
        .respond(&create_req, create_result(session_id.as_str()))
        .await;
    let session = timeout(TIMEOUT, start).await.unwrap().unwrap().unwrap();
    drop(session);
}

#[tokio::test]
async fn dropping_unstarted_prepared_session_leaves_no_state() {
    let (client, mut server) = make_client();
    let session_id = SessionId::new("prepared-dropped");

    let prepared = client
        .prepare_session(SessionConfig::default().with_session_id(session_id.clone()))
        .unwrap();
    let mut events = prepared.subscribe();
    drop(prepared);

    assert!(matches!(
        timeout(TIMEOUT, events.recv())
            .await
            .unwrap()
            .unwrap_err()
            .kind(),
        RecvErrorKind::Closed
    ));
    assert!(client.registered_session_ids_for_test().is_empty());
    server.expect_quiet().await;
}

// ---------------------------------------------------------------------------
// 7. Cancelling a polled startup
// ---------------------------------------------------------------------------

#[tokio::test]
async fn cancelled_prepared_create_cleans_up_and_allows_retry() {
    let (client, mut server) = make_client();
    let session_id = SessionId::new("prepared-create-cancel");

    let prepared = client
        .prepare_session(SessionConfig::default().with_session_id(session_id.clone()))
        .unwrap();
    let mut events = prepared.subscribe();
    let start = tokio::spawn(prepared.start());

    // The request is on the wire; cancel before responding.
    let create_req = server.read_request().await;
    assert_eq!(create_req["method"], "session.create");
    start.abort();
    let _ = start.await;

    await_no_registrations(&client).await;
    expect_closed(&mut events).await;

    // A retry with the same session ID succeeds.
    let retry = tokio::spawn(
        client
            .prepare_session(SessionConfig::default().with_session_id(session_id.clone()))
            .unwrap()
            .start(),
    );
    let retry_req = server.read_request().await;
    assert_eq!(retry_req["method"], "session.create");
    server
        .respond(&retry_req, create_result(session_id.as_str()))
        .await;
    let session = timeout(TIMEOUT, retry).await.unwrap().unwrap().unwrap();
    assert_eq!(session.id(), &session_id);
    drop(session);
}

#[tokio::test]
async fn cancelled_prepared_resume_cleans_up_and_allows_retry() {
    let (client, mut server) = make_client();
    let session_id = SessionId::new("prepared-resume-cancel");

    let prepared = client
        .prepare_resume_session(ResumeSessionConfig::new(session_id.clone()))
        .unwrap();
    let mut events = prepared.subscribe();
    let start = tokio::spawn(prepared.start());

    let resume_req = server.read_request().await;
    assert_eq!(resume_req["method"], "session.resume");
    start.abort();
    let _ = start.await;

    await_no_registrations(&client).await;
    expect_closed(&mut events).await;

    let retry = tokio::spawn(
        client
            .prepare_resume_session(ResumeSessionConfig::new(session_id.clone()))
            .unwrap()
            .start(),
    );
    let retry_req = server.read_request().await;
    assert_eq!(retry_req["method"], "session.resume");
    server
        .respond(&retry_req, json!({ "sessionId": session_id.as_str() }))
        .await;
    server.answer_skills_reload().await;
    let session = timeout(TIMEOUT, retry).await.unwrap().unwrap().unwrap();
    assert_eq!(session.id(), &session_id);
    drop(session);
}

// ---------------------------------------------------------------------------
// 8. Startup failures preserve error kinds and clean up
// ---------------------------------------------------------------------------

#[tokio::test]
async fn create_rpc_error_preserves_kind_and_cleans_up() {
    let (client, mut server) = make_client();
    let session_id = SessionId::new("prepared-create-rpc-error");

    let prepared = client
        .prepare_session(SessionConfig::default().with_session_id(session_id.clone()))
        .unwrap();
    let mut events = prepared.subscribe();
    let start = tokio::spawn(prepared.start());

    let create_req = server.read_request().await;
    server
        .respond_error(&create_req, -32000, "session create failed")
        .await;

    let error = expect_error(timeout(TIMEOUT, start).await.unwrap().unwrap());
    assert!(
        matches!(error.kind(), ErrorKind::Rpc { code: -32000 }),
        "unexpected error kind: {:?}",
        error.kind()
    );
    await_no_registrations(&client).await;
    expect_closed(&mut events).await;
}

#[tokio::test]
async fn create_session_id_mismatch_preserves_kind_and_cleans_up() {
    let (client, mut server) = make_client();
    let session_id = SessionId::new("prepared-mismatch");

    let prepared = client
        .prepare_session(SessionConfig::default().with_session_id(session_id.clone()))
        .unwrap();
    let mut events = prepared.subscribe();
    let start = tokio::spawn(prepared.start());

    let create_req = server.read_request().await;
    server
        .respond(&create_req, create_result("some-other-id"))
        .await;

    let error = expect_error(timeout(TIMEOUT, start).await.unwrap().unwrap());
    let ErrorKind::Session(SessionErrorKind::SessionIdMismatch {
        requested,
        returned,
    }) = error.kind()
    else {
        panic!("unexpected error kind: {:?}", error.kind());
    };
    assert_eq!(requested, &session_id);
    assert_eq!(returned.as_str(), "some-other-id");

    await_no_registrations(&client).await;
    expect_closed(&mut events).await;
}

#[tokio::test]
async fn resume_session_id_mismatch_preserves_kind_and_cleans_up() {
    let (client, mut server) = make_client();
    let session_id = SessionId::new("prepared-resume-mismatch");

    let prepared = client
        .prepare_resume_session(ResumeSessionConfig::new(session_id.clone()))
        .unwrap();
    let mut events = prepared.subscribe();
    let start = tokio::spawn(prepared.start());

    let resume_req = server.read_request().await;
    server
        .respond(&resume_req, json!({ "sessionId": "another-session" }))
        .await;

    let error = expect_error(timeout(TIMEOUT, start).await.unwrap().unwrap());
    assert!(
        matches!(
            error.kind(),
            ErrorKind::Session(SessionErrorKind::SessionIdMismatch { .. })
        ),
        "unexpected error kind: {:?}",
        error.kind()
    );
    await_no_registrations(&client).await;
    expect_closed(&mut events).await;
}

#[tokio::test]
async fn zero_event_buffer_capacity_is_invalid_config() {
    let (client, _server) = make_client();

    let error = expect_error(
        client.prepare_session(SessionConfig::default().with_event_buffer_capacity(0)),
    );
    assert!(matches!(error.kind(), ErrorKind::InvalidConfig));

    let error = expect_error(client.prepare_resume_session(
        ResumeSessionConfig::new(SessionId::new("zero")).with_event_buffer_capacity(0),
    ));
    assert!(matches!(error.kind(), ErrorKind::InvalidConfig));

    // The compatibility wrappers surface the same error.
    let error = expect_error(
        client
            .create_session(SessionConfig::default().with_event_buffer_capacity(0))
            .await,
    );
    assert!(matches!(error.kind(), ErrorKind::InvalidConfig));
}

// ---------------------------------------------------------------------------
// 9. Early and late subscribers share one event loop
// ---------------------------------------------------------------------------

#[tokio::test]
async fn early_and_late_subscribers_share_one_event_loop() {
    let (client, mut server) = make_client();
    let session_id = SessionId::new("prepared-two-subscribers");

    let prepared = client
        .prepare_session(
            SessionConfig::default()
                .with_session_id(session_id.clone())
                .with_event_buffer_capacity(2048),
        )
        .unwrap();
    let mut early = prepared.subscribe();
    let start = tokio::spawn(prepared.start());

    let create_req = server.read_request().await;
    server
        .send_event(session_id.as_str(), "evt-early", "assistant.message", false)
        .await;
    server
        .respond(&create_req, create_result(session_id.as_str()))
        .await;
    let session = timeout(TIMEOUT, start).await.unwrap().unwrap().unwrap();

    let mut late = session.subscribe();
    server
        .send_event(session_id.as_str(), "evt-late", "assistant.message", false)
        .await;

    // The early subscriber sees both events, once each.
    assert_eq!(
        timeout(TIMEOUT, early.recv())
            .await
            .unwrap()
            .unwrap()
            .id
            .as_str(),
        "evt-early"
    );
    assert_eq!(
        timeout(TIMEOUT, early.recv())
            .await
            .unwrap()
            .unwrap()
            .id
            .as_str(),
        "evt-late"
    );
    // The late subscriber only sees what was emitted after it subscribed —
    // exactly once, which would be twice if a second event loop existed.
    assert_eq!(
        timeout(TIMEOUT, late.recv())
            .await
            .unwrap()
            .unwrap()
            .id
            .as_str(),
        "evt-late"
    );
    assert!(
        timeout(QUIET, late.recv()).await.is_err(),
        "duplicate delivery implies more than one event loop"
    );
    assert!(
        timeout(QUIET, early.recv()).await.is_err(),
        "duplicate delivery implies more than one event loop"
    );
    drop(session);
}

// ---------------------------------------------------------------------------
// 10. Compatibility wrappers
// ---------------------------------------------------------------------------

#[tokio::test]
async fn create_session_wrapper_keeps_rpc_sequence() {
    let (client, mut server) = make_client();

    let start = tokio::spawn({
        let client = client.clone();
        async move { client.create_session(SessionConfig::default()).await }
    });

    let create_req = server.read_request().await;
    assert_eq!(create_req["method"], "session.create");
    let session_id = create_req["params"]["sessionId"]
        .as_str()
        .unwrap()
        .to_string();
    server
        .respond(&create_req, create_result(&session_id))
        .await;

    let session = timeout(TIMEOUT, start).await.unwrap().unwrap().unwrap();
    assert_eq!(session.id().as_str(), session_id);
    server.expect_quiet().await;
    drop(session);
}

#[tokio::test]
async fn resume_session_wrapper_keeps_rpc_sequence() {
    let (client, mut server) = make_client();
    let session_id = SessionId::new("wrapper-resume");

    let start = tokio::spawn({
        let client = client.clone();
        let session_id = session_id.clone();
        async move {
            client
                .resume_session(ResumeSessionConfig::new(session_id))
                .await
        }
    });

    let resume_req = server.read_request().await;
    assert_eq!(resume_req["method"], "session.resume");
    assert_eq!(resume_req["params"]["sessionId"], session_id.as_str());
    server
        .respond(&resume_req, json!({ "sessionId": session_id.as_str() }))
        .await;
    server.answer_skills_reload().await;

    let session = timeout(TIMEOUT, start).await.unwrap().unwrap().unwrap();
    assert_eq!(session.id(), &session_id);
    server.expect_quiet().await;
    drop(session);
}

// ---------------------------------------------------------------------------
// 11. Type-level guarantees
// ---------------------------------------------------------------------------

/// Detects `Clone` without requiring it: the inherent method wins whenever
/// `T: Clone`, otherwise the blanket trait method is selected.
struct CloneProbe<T>(PhantomData<T>);

impl<T: Clone> CloneProbe<T> {
    fn is_clone(&self) -> bool {
        true
    }
}

trait MaybeClone {
    fn is_clone(&self) -> bool {
        false
    }
}

impl<T> MaybeClone for CloneProbe<T> {}

#[test]
fn prepared_session_is_send_static_and_not_clone() {
    fn assert_send_static<T: Send + 'static>() {}
    assert_send_static::<PreparedSession>();

    // Sanity-check the probe against a type that is `Clone` ...
    assert!(CloneProbe::<String>(PhantomData).is_clone());
    // ... then assert `PreparedSession` deliberately is not, so a prepared
    // session can never be started twice.
    assert!(!CloneProbe::<PreparedSession>(PhantomData).is_clone());
}

// ---------------------------------------------------------------------------
// Registration ownership: a stale startup guard must never unregister a
// newer registration that reused the same session ID.
// ---------------------------------------------------------------------------

/// Drive a startup future until it parks awaiting its RPC response.
///
/// The duration is a bound, not a correctness sleep: whether the future
/// actually reached the wire is asserted afterwards by reading the request,
/// which fails loudly on its own timeout if it did not.
const DRIVE: Duration = Duration::from_millis(50);

#[tokio::test]
async fn stale_create_guard_does_not_unregister_same_id_retry() {
    let (client, mut server) = make_client();
    let session_id = SessionId::new("stale-create-guard");

    // First attempt: registers, sends `session.create`, then parks.
    let mut first = Box::pin(
        client
            .prepare_session(SessionConfig::default().with_session_id(session_id.clone()))
            .unwrap()
            .start(),
    );
    let _ = timeout(DRIVE, &mut first).await;
    let first_req = server.read_request().await;
    assert_eq!(first_req["method"], "session.create");

    // Second attempt with the same pinned ID, started before the first is
    // dropped, so it replaces the first attempt's router registration.
    let prepared = client
        .prepare_session(
            SessionConfig::default()
                .with_session_id(session_id.clone())
                .with_event_buffer_capacity(64),
        )
        .unwrap();
    let mut events = prepared.subscribe();
    let mut second = Box::pin(prepared.start());
    let _ = timeout(DRIVE, &mut second).await;
    let second_req = server.read_request().await;
    assert_eq!(second_req["method"], "session.create");

    // The stale guard runs now. It must not touch the live registration.
    drop(first);
    assert_eq!(
        client.registered_session_ids_for_test(),
        vec![session_id.clone()],
        "a stale startup guard unregistered the live retry"
    );

    server
        .respond(&second_req, create_result(session_id.as_str()))
        .await;
    let session = timeout(TIMEOUT, &mut second).await.unwrap().unwrap();

    // Events must still route to the surviving registration.
    server
        .send_event(
            session_id.as_str(),
            "evt-after-stale",
            "assistant.message",
            false,
        )
        .await;
    let event = timeout(TIMEOUT, events.recv()).await.unwrap().unwrap();
    assert_eq!(event.id.as_str(), "evt-after-stale");
    drop(session);
}

#[tokio::test]
async fn stale_resume_guard_does_not_unregister_same_id_retry() {
    let (client, mut server) = make_client();
    let session_id = SessionId::new("stale-resume-guard");

    let mut first = Box::pin(
        client
            .prepare_resume_session(ResumeSessionConfig::new(session_id.clone()))
            .unwrap()
            .start(),
    );
    let _ = timeout(DRIVE, &mut first).await;
    let first_req = server.read_request().await;
    assert_eq!(first_req["method"], "session.resume");

    let prepared = client
        .prepare_resume_session(
            ResumeSessionConfig::new(session_id.clone()).with_event_buffer_capacity(64),
        )
        .unwrap();
    let mut events = prepared.subscribe();
    let mut second = Box::pin(prepared.start());
    let _ = timeout(DRIVE, &mut second).await;
    let second_req = server.read_request().await;
    assert_eq!(second_req["method"], "session.resume");

    drop(first);
    assert_eq!(
        client.registered_session_ids_for_test(),
        vec![session_id.clone()],
        "a stale startup guard unregistered the live retry"
    );

    // Hand the surviving startup to a task: resume issues a follow-up
    // `session.skills.reload` that only makes progress while it is polled.
    let second = tokio::spawn(second);
    server
        .respond(&second_req, json!({ "sessionId": session_id.as_str() }))
        .await;
    server.answer_skills_reload().await;
    let session = timeout(TIMEOUT, second).await.unwrap().unwrap().unwrap();

    server
        .send_event(
            session_id.as_str(),
            "evt-after-stale",
            "assistant.message",
            false,
        )
        .await;
    let event = timeout(TIMEOUT, events.recv()).await.unwrap().unwrap();
    assert_eq!(event.id.as_str(), "evt-after-stale");
    drop(session);
}

/// A disconnected session must not unregister a same-ID session that
/// replaced it.
#[tokio::test]
async fn dropping_superseded_session_does_not_unregister_its_replacement() {
    let (client, mut server) = make_client();
    let session_id = SessionId::new("superseded-session");

    let first = tokio::spawn(
        client
            .prepare_session(SessionConfig::default().with_session_id(session_id.clone()))
            .unwrap()
            .start(),
    );
    let first_req = server.read_request().await;
    server
        .respond(&first_req, create_result(session_id.as_str()))
        .await;
    let first_session = timeout(TIMEOUT, first).await.unwrap().unwrap().unwrap();

    let prepared = client
        .prepare_session(
            SessionConfig::default()
                .with_session_id(session_id.clone())
                .with_event_buffer_capacity(64),
        )
        .unwrap();
    let mut events = prepared.subscribe();
    let second = tokio::spawn(prepared.start());
    let second_req = server.read_request().await;
    server
        .respond(&second_req, create_result(session_id.as_str()))
        .await;
    let second_session = timeout(TIMEOUT, second).await.unwrap().unwrap().unwrap();

    // The superseded handle goes away; the live session must survive.
    drop(first_session);
    assert_eq!(
        client.registered_session_ids_for_test(),
        vec![session_id.clone()],
        "dropping a superseded Session unregistered its replacement"
    );

    server
        .send_event(
            session_id.as_str(),
            "evt-survivor",
            "assistant.message",
            false,
        )
        .await;
    let event = timeout(TIMEOUT, events.recv()).await.unwrap().unwrap();
    assert_eq!(event.id.as_str(), "evt-survivor");
    drop(second_session);
}

// ---------------------------------------------------------------------------
// Deferred (server-assigned ID) create cancellation
// ---------------------------------------------------------------------------

/// Cancelling a cloud create before the response arrives must leave no
/// registration behind, even though the session ID is only known to the
/// inline response callback.
#[tokio::test]
async fn cancelled_deferred_create_leaves_no_registration() {
    let (client, mut server) = make_client();

    let prepared = client
        .prepare_session(SessionConfig::default().with_cloud(cloud_options()))
        .unwrap();
    let mut events = prepared.subscribe();
    let start = tokio::spawn(prepared.start());

    let create_req = server.read_request().await;
    assert_eq!(create_req["method"], "session.create");
    assert!(create_req["params"]["sessionId"].is_null());

    // Cancel before the server answers, then answer: the response carries
    // the server-assigned ID the inline callback would register.
    start.abort();
    let _ = start.await;
    server
        .respond(&create_req, create_result("server-assigned-id"))
        .await;

    expect_closed(&mut events).await;
    await_no_registrations(&client).await;
    // Nothing may appear after the response has been fully processed.
    server.expect_quiet().await;
    assert!(
        client.registered_session_ids_for_test().is_empty(),
        "a cancelled deferred create left a registration behind"
    );

    // A fresh cloud create still works afterwards.
    let retry = tokio::spawn(
        client
            .prepare_session(SessionConfig::default().with_cloud(cloud_options()))
            .unwrap()
            .start(),
    );
    let retry_req = server.read_request().await;
    server
        .respond(&retry_req, create_result("server-assigned-retry"))
        .await;
    let session = timeout(TIMEOUT, retry).await.unwrap().unwrap().unwrap();
    assert_eq!(session.id().as_str(), "server-assigned-retry");
    drop(session);
}

/// Poll (bounded) until `session_id` shows up on the client's router.
///
/// The failure message names the expectation, not the ID: session IDs are
/// not written to test output.
async fn await_registered(client: &Client, session_id: &str) {
    let deadline = tokio::time::Instant::now() + TIMEOUT;
    while !client
        .registered_session_ids_for_test()
        .iter()
        .any(|id| id.as_str() == session_id)
    {
        assert!(
            tokio::time::Instant::now() < deadline,
            "inline callback never registered the expected session"
        );
        tokio::task::yield_now().await;
    }
}

/// The other half of the deferred-create window: cancellation lands *after*
/// the inline response callback has already registered the server-assigned
/// ID. The startup guard owns that registration and must remove it.
///
/// Deterministic by construction — the start future is parked on its
/// response and never polled again, so the callback (which runs on the
/// JSON-RPC read task, independently of the caller) is guaranteed to have
/// registered before the future is dropped.
#[tokio::test]
async fn deferred_create_cancelled_after_callback_registered_is_cleaned_up() {
    let (client, mut server) = make_client();

    let prepared = client
        .prepare_session(SessionConfig::default().with_cloud(cloud_options()))
        .unwrap();
    let mut events = prepared.subscribe();
    let mut start = Box::pin(prepared.start());

    // Drive to the wire, then park.
    let _ = timeout(DRIVE, &mut start).await;
    let create_req = server.read_request().await;
    assert_eq!(create_req["method"], "session.create");

    // The read task runs the inline callback and registers the ID while the
    // caller's future stays unpolled.
    server
        .respond(&create_req, create_result("registered-then-cancelled"))
        .await;
    await_registered(&client, "registered-then-cancelled").await;

    // Cancellation now lands on a slot that already owns a registration.
    drop(start);

    expect_closed(&mut events).await;
    await_no_registrations(&client).await;
    server.expect_quiet().await;

    // The same server-assigned ID can be handed out again without the dead
    // attempt's cleanup interfering.
    let retry = tokio::spawn(
        client
            .prepare_session(SessionConfig::default().with_cloud(cloud_options()))
            .unwrap()
            .start(),
    );
    let retry_req = server.read_request().await;
    server
        .respond(&retry_req, create_result("registered-then-cancelled"))
        .await;
    let session = timeout(TIMEOUT, retry).await.unwrap().unwrap().unwrap();
    assert_eq!(session.id().as_str(), "registered-then-cancelled");
    assert_eq!(
        client.registered_session_ids_for_test().len(),
        1,
        "retry must hold exactly one registration"
    );
    drop(session);
}
