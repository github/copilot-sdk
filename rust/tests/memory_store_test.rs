// Copyright (c) Microsoft Corporation. All rights reserved.

#![allow(clippy::unwrap_used)]

use std::sync::{Arc, Mutex};
use std::time::Duration;

use async_trait::async_trait;
use github_copilot_sdk::session::Session;
use github_copilot_sdk::types::MemoryConfiguration;
use github_copilot_sdk::{
    Client, Error, ErrorKind, MemoryStore, ResumeSessionConfig, SessionConfig, SessionId,
    StoredMemory,
};
use serde_json::{Value, json};
use tokio::io::{AsyncReadExt, AsyncWriteExt, DuplexStream, duplex};
use tokio::time::timeout;

const TIMEOUT: Duration = Duration::from_secs(2);

async fn write_frame(writer: &mut DuplexStream, message: Value) {
    let body = serde_json::to_vec(&message).unwrap();
    writer
        .write_all(format!("Content-Length: {}\r\n\r\n", body.len()).as_bytes())
        .await
        .unwrap();
    writer.write_all(&body).await.unwrap();
}

async fn read_frame(reader: &mut DuplexStream) -> Value {
    timeout(TIMEOUT, async {
        let mut header = Vec::new();
        while !header.ends_with(b"\r\n\r\n") {
            header.push(reader.read_u8().await.unwrap());
        }
        let length = String::from_utf8(header)
            .unwrap()
            .trim()
            .strip_prefix("Content-Length: ")
            .unwrap()
            .parse()
            .unwrap();
        let mut body = vec![0; length];
        reader.read_exact(&mut body).await.unwrap();
        serde_json::from_slice(&body).unwrap()
    })
    .await
    .expect("SDK must send a frame")
}

struct Store {
    fail: bool,
    calls: Arc<Mutex<Vec<StoredMemory>>>,
}

#[async_trait]
impl MemoryStore for Store {
    async fn store(&self, memory: StoredMemory) -> Result<(), Error> {
        self.calls.lock().unwrap().push(memory);
        if self.fail {
            Err(Error::with_message(
                ErrorKind::InvalidConfig,
                "secret backend detail",
            ))
        } else {
            Ok(())
        }
    }
}

struct Fixture {
    client: Client,
    session: Session,
    reader: DuplexStream,
    writer: DuplexStream,
    calls: Arc<Mutex<Vec<StoredMemory>>>,
}

fn store(fail: bool, calls: &Arc<Mutex<Vec<StoredMemory>>>) -> Arc<dyn MemoryStore> {
    Arc::new(Store {
        fail,
        calls: calls.clone(),
    })
}

fn store_request(id: u64, session_id: &str) -> Value {
    json!({
        "jsonrpc": "2.0", "id": id, "method": "memoryStore.store",
        "params": {
            "sessionId": session_id, "subject": "package manager",
            "fact": "Use pnpm.", "citations": "package.json", "reason": "The workspace uses pnpm.",
        },
    })
}

impl Fixture {
    async fn new(fail: bool) -> Self {
        let calls = Arc::new(Mutex::new(Vec::new()));
        Self::with_store(store(fail, &calls), calls).await
    }

    async fn with_store(
        memory_store: Arc<dyn MemoryStore>,
        calls: Arc<Mutex<Vec<StoredMemory>>>,
    ) -> Self {
        let (client_write, mut reader) = duplex(8192);
        let (mut writer, client_read) = duplex(8192);
        let client = Client::from_streams(client_read, client_write, std::env::temp_dir()).unwrap();
        let config = SessionConfig::default()
            .with_memory(MemoryConfiguration::enabled())
            .with_memory_store(memory_store);
        let create = tokio::spawn({
            let client = client.clone();
            async move { client.create_session(config).await.unwrap() }
        });

        let request = read_frame(&mut reader).await;
        assert_eq!(request["method"], "session.create");
        assert_eq!(request["params"]["memory"], json!({ "enabled": true }));
        assert_eq!(request["params"]["hasMemoryStore"], true);
        let session_id = request["params"]["sessionId"].clone();
        write_frame(
            &mut writer,
            json!({ "jsonrpc": "2.0", "id": request["id"], "result": { "sessionId": session_id } }),
        )
        .await;

        let session = timeout(TIMEOUT, create).await.unwrap().unwrap();
        Self {
            client,
            session,
            reader,
            writer,
            calls,
        }
    }

    async fn store(&mut self, session_id: &str) -> Value {
        write_frame(&mut self.writer, store_request(100, session_id)).await;
        read_frame(&mut self.reader).await
    }
}

#[tokio::test]
async fn memory_store_serves_requests_while_session_create_is_pending() {
    let (client_write, mut reader) = duplex(8192);
    let (mut writer, client_read) = duplex(8192);
    let client = Client::from_streams(client_read, client_write, std::env::temp_dir()).unwrap();
    let calls = Arc::new(Mutex::new(Vec::new()));
    let config = SessionConfig::default()
        .with_memory(MemoryConfiguration::enabled())
        .with_memory_store(store(false, &calls));
    let create = tokio::spawn({
        let client = client.clone();
        async move { client.create_session(config).await.unwrap() }
    });

    let request = read_frame(&mut reader).await;
    assert_eq!(request["method"], "session.create");
    assert_eq!(request["params"]["hasMemoryStore"], true);
    assert!(request["params"]["memory"].get("storeMemory").is_none());
    let session_id = request["params"]["sessionId"].as_str().unwrap().to_string();
    write_frame(&mut writer, store_request(100, &session_id)).await;
    assert_eq!(
        read_frame(&mut reader).await,
        json!({ "jsonrpc": "2.0", "id": 100, "result": null })
    );
    write_frame(
        &mut writer,
        json!({ "jsonrpc": "2.0", "id": request["id"], "result": { "sessionId": session_id } }),
    )
    .await;
    timeout(TIMEOUT, create).await.unwrap().unwrap();

    let calls = calls.lock().unwrap();
    assert_eq!(calls.len(), 1);
    assert_eq!(calls[0].session_id, session_id);
    assert_eq!(calls[0].subject, "package manager");
    assert_eq!(calls[0].fact, "Use pnpm.");
    assert_eq!(calls[0].citations, "package.json");
    assert_eq!(calls[0].reason, "The workspace uses pnpm.");
}

#[tokio::test]
async fn memory_store_is_rebound_by_session_resume() {
    let mut fixture = Fixture::new(false).await;
    let session_id = fixture.session.id().clone();
    let resume_calls = Arc::new(Mutex::new(Vec::new()));
    let resume = tokio::spawn({
        let client = fixture.client.clone();
        let config = ResumeSessionConfig::new(session_id.clone())
            .with_memory(MemoryConfiguration::enabled())
            .with_memory_store(store(false, &resume_calls));
        async move { client.resume_session(config).await.unwrap() }
    });

    let request = read_frame(&mut fixture.reader).await;
    assert_eq!(request["method"], "session.resume");
    assert_eq!(request["params"]["memory"], json!({ "enabled": true }));
    assert_eq!(request["params"]["hasMemoryStore"], true);
    let response = fixture.store(session_id.as_str()).await;
    assert_eq!(
        response,
        json!({ "jsonrpc": "2.0", "id": 100, "result": null })
    );
    write_frame(
        &mut fixture.writer,
        json!({ "jsonrpc": "2.0", "id": request["id"], "result": { "sessionId": session_id } }),
    )
    .await;
    let reload = read_frame(&mut fixture.reader).await;
    assert_eq!(reload["method"], "session.skills.reload");
    write_frame(
        &mut fixture.writer,
        json!({ "jsonrpc": "2.0", "id": reload["id"], "result": {} }),
    )
    .await;
    timeout(TIMEOUT, resume).await.unwrap().unwrap();

    assert_eq!(resume_calls.lock().unwrap().len(), 1);
    assert!(fixture.calls.lock().unwrap().is_empty());
}

#[tokio::test]
async fn memory_store_requires_memory_enabled_before_any_request() {
    let (client_write, mut reader) = duplex(8192);
    let (_writer, client_read) = duplex(8192);
    let client = Client::from_streams(client_read, client_write, std::env::temp_dir()).unwrap();
    let calls = Arc::new(Mutex::new(Vec::new()));

    let Err(error) = client
        .create_session(SessionConfig::default().with_memory_store(store(false, &calls)))
        .await
    else {
        panic!("create must fail");
    };
    assert_eq!(
        error.to_string(),
        "memory_store requires memory to be enabled with with_memory(...)."
    );
    let Err(error) = client
        .resume_session(
            ResumeSessionConfig::new(SessionId::new("resume-id"))
                .with_memory(MemoryConfiguration::disabled())
                .with_memory_store(store(false, &calls)),
        )
        .await
    else {
        panic!("resume must fail");
    };
    assert_eq!(
        error.to_string(),
        "memory_store requires memory to be enabled with with_memory(...)."
    );

    let probe = tokio::spawn({
        let client = client.clone();
        async move { client.create_session(SessionConfig::default()).await }
    });
    let request = read_frame(&mut reader).await;
    assert_eq!(request["method"], "session.create");
    assert!(request["params"].get("hasMemoryStore").is_none());
    probe.abort();
}

#[tokio::test]
async fn memory_store_failures_are_reported_generically() {
    let mut fixture = Fixture::new(true).await;
    let session_id = fixture.session.id().to_string();

    let response = fixture.store(&session_id).await;
    assert_eq!(response["error"]["message"], "Memory store callback failed");
    assert!(!response.to_string().contains("secret backend detail"));
    assert_eq!(fixture.calls.lock().unwrap().len(), 1);
}

#[tokio::test]
async fn memory_store_requests_for_unknown_sessions_fail() {
    let mut fixture = Fixture::new(false).await;

    let response = fixture.store("unknown-session").await;
    assert_eq!(
        response["error"]["message"],
        "Memory store owner is unavailable"
    );
    assert!(fixture.calls.lock().unwrap().is_empty());
}

struct PendingStore {
    entered: tokio::sync::mpsc::UnboundedSender<()>,
    dropped: tokio::sync::mpsc::UnboundedSender<()>,
}

struct DropSignal(tokio::sync::mpsc::UnboundedSender<()>);

impl Drop for DropSignal {
    fn drop(&mut self) {
        let _ = self.0.send(());
    }
}

#[async_trait]
impl MemoryStore for PendingStore {
    async fn store(&self, _memory: StoredMemory) -> Result<(), Error> {
        let _signal = DropSignal(self.dropped.clone());
        let _ = self.entered.send(());
        std::future::pending().await
    }
}

#[tokio::test]
async fn memory_store_runtime_cancellation_drops_the_store_future() {
    let (entered, mut entered_rx) = tokio::sync::mpsc::unbounded_channel();
    let (dropped, mut dropped_rx) = tokio::sync::mpsc::unbounded_channel();
    let mut fixture = Fixture::with_store(
        Arc::new(PendingStore { entered, dropped }),
        Arc::new(Mutex::new(Vec::new())),
    )
    .await;
    let session_id = fixture.session.id().to_string();

    write_frame(&mut fixture.writer, store_request(100, &session_id)).await;
    timeout(TIMEOUT, entered_rx.recv()).await.unwrap().unwrap();
    write_frame(
        &mut fixture.writer,
        json!({ "jsonrpc": "2.0", "method": "$/cancelRequest", "params": { "id": 100 } }),
    )
    .await;

    let response = read_frame(&mut fixture.reader).await;
    assert_eq!(response["id"], 100);
    assert_eq!(response["error"]["code"], -32800);
    timeout(TIMEOUT, dropped_rx.recv()).await.unwrap().unwrap();
}
