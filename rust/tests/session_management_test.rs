// Copyright (c) Microsoft Corporation. All rights reserved.

#![allow(clippy::unwrap_used)]

use std::sync::Arc;
use std::time::Duration;

use async_trait::async_trait;
use github_copilot_sdk::session::Session;
use github_copilot_sdk::session_management::{
    SessionManagementConfigureRequest, SessionManagementCreateHostRequest,
    SessionManagementCreateResult, SessionManagementScope,
};
use github_copilot_sdk::{Client, Error, ErrorKind, SessionConfig, SessionCreationHandler};
use serde_json::{Value, json};
use tokio::io::{AsyncReadExt, AsyncWriteExt, DuplexStream, duplex};
use tokio::sync::Notify;
use tokio::time::timeout;

const TIMEOUT: Duration = Duration::from_secs(2);

#[test]
fn session_management_scope_uses_the_canonical_typed_wire_values() {
    for (scope, wire) in [
        (SessionManagementScope::All, "all"),
        (SessionManagementScope::InProcess, "in-process"),
    ] {
        let request = SessionManagementConfigureRequest {
            scope: Some(scope),
            ..Default::default()
        };
        assert_eq!(
            serde_json::to_value(request).unwrap(),
            json!({ "scope": wire })
        );
    }
}

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
    .expect("SDK must respond to the reverse request")
}

struct Fixture {
    _client: Client,
    session: Session,
    reader: DuplexStream,
    writer: DuplexStream,
}

impl Fixture {
    async fn new() -> Self {
        let (client_write, mut reader) = duplex(8192);
        let (mut writer, client_read) = duplex(8192);
        let client = Client::from_streams(client_read, client_write, std::env::temp_dir()).unwrap();
        let create = tokio::spawn({
            let client = client.clone();
            async move {
                client
                    .create_session(SessionConfig::default())
                    .await
                    .unwrap()
            }
        });
        let request = read_frame(&mut reader).await;
        assert_eq!(request["method"], "session.create");
        write_frame(
            &mut writer,
            json!({
                "jsonrpc": "2.0", "id": request["id"],
                "result": { "sessionId": request["params"]["sessionId"] },
            }),
        )
        .await;
        let session = timeout(TIMEOUT, create).await.unwrap().unwrap();
        Self {
            _client: client,
            session,
            reader,
            writer,
        }
    }

    async fn set_handler(&mut self, handler: Option<Arc<dyn SessionCreationHandler>>) {
        let has_handler = handler.is_some();
        let configure = self.session.set_session_creation_handler(handler);
        tokio::pin!(configure);
        let request = tokio::select! {
            request = read_frame(&mut self.reader) => request,
            result = &mut configure => panic!("configure completed before its RPC: {result:?}"),
        };
        assert_eq!(request["method"], "session.management.configure");
        assert_eq!(
            request["params"],
            json!({ "sessionId": self.session.id(), "hasCreateHandler": has_handler })
        );
        write_frame(
            &mut self.writer,
            json!({
                "jsonrpc": "2.0", "id": request["id"],
                "result": { "enabled": false, "scope": "all", "list": true, "create": true, "message": true, "close": true },
            }),
        )
        .await;
        assert!(!timeout(TIMEOUT, configure).await.unwrap().unwrap().enabled);
    }

    async fn request_creation(&mut self, parent: &str) {
        write_frame(
            &mut self.writer,
            json!({
                "jsonrpc": "2.0", "id": 100, "method": "sessionManagement.create",
                "params": {
                    "sessionId": self.session.id(), "parentSessionId": parent,
                    "newSessionId": "new-managed-peer",
                    "cwd": "workspace", "configDir": "profile", "permissionMode": "manual",
                    "name": "Independent peer",
                },
            }),
        )
        .await;
    }
}

struct Creator {
    fail: bool,
    calls: Arc<std::sync::Mutex<Vec<SessionManagementCreateHostRequest>>>,
}

#[async_trait]
impl SessionCreationHandler for Creator {
    async fn create_session(
        &self,
        request: SessionManagementCreateHostRequest,
    ) -> Result<SessionManagementCreateResult, Error> {
        self.calls.lock().unwrap().push(request);
        if self.fail {
            Err(Error::with_message(
                ErrorKind::InvalidConfig,
                "private callback details",
            ))
        } else {
            Ok(SessionManagementCreateResult {
                session_id: "created-peer".into(),
            })
        }
    }
}

#[tokio::test]
async fn session_management_creation_callback_is_typed_and_registration_does_not_enable_tools() {
    let mut fixture = Fixture::new().await;
    let calls = Arc::new(std::sync::Mutex::new(Vec::new()));
    fixture
        .set_handler(Some(Arc::new(Creator {
            fail: false,
            calls: calls.clone(),
        })))
        .await;
    let owner = fixture.session.id().to_string();
    fixture.request_creation(&owner).await;
    assert_eq!(
        read_frame(&mut fixture.reader).await,
        json!({ "jsonrpc": "2.0", "id": 100, "result": { "sessionId": "created-peer" } })
    );
    let calls = calls.lock().unwrap();
    assert_eq!(calls.len(), 1);
    assert_eq!(calls[0].parent_session_id, owner);
    assert_eq!(calls[0].new_session_id, "new-managed-peer");
    assert_eq!(calls[0].cwd, "workspace");
    assert_eq!(calls[0].config_dir, "profile");
    assert_eq!(calls[0].name.as_deref(), Some("Independent peer"));
}

#[tokio::test]
async fn session_management_creation_rejects_wrong_owner_and_removed_handler() {
    let mut fixture = Fixture::new().await;
    let calls = Arc::new(std::sync::Mutex::new(Vec::new()));
    fixture
        .set_handler(Some(Arc::new(Creator {
            fail: false,
            calls: calls.clone(),
        })))
        .await;
    fixture.request_creation("different-owner").await;
    assert_eq!(
        read_frame(&mut fixture.reader).await["error"]["code"],
        -32602
    );
    assert!(calls.lock().unwrap().is_empty());
    fixture.set_handler(None).await;
    let owner = fixture.session.id().to_string();
    fixture.request_creation(&owner).await;
    assert_eq!(
        read_frame(&mut fixture.reader).await["error"]["code"],
        -32602
    );
}

#[tokio::test]
async fn session_management_creation_hides_callback_error_details() {
    let mut fixture = Fixture::new().await;
    fixture
        .set_handler(Some(Arc::new(Creator {
            fail: true,
            calls: Arc::default(),
        })))
        .await;
    let owner = fixture.session.id().to_string();
    fixture.request_creation(&owner).await;
    let response = read_frame(&mut fixture.reader).await;
    assert_eq!(response["error"]["code"], -32603);
    assert_eq!(
        response["error"]["message"],
        "Session creation callback failed"
    );
    assert!(!response.to_string().contains("private callback details"));
}

struct PendingCreator {
    entered: Arc<Notify>,
    dropped: Arc<Notify>,
}

struct NotifyOnDrop(Arc<Notify>);

impl Drop for NotifyOnDrop {
    fn drop(&mut self) {
        self.0.notify_one();
    }
}

#[async_trait]
impl SessionCreationHandler for PendingCreator {
    async fn create_session(
        &self,
        _request: SessionManagementCreateHostRequest,
    ) -> Result<SessionManagementCreateResult, Error> {
        let _guard = NotifyOnDrop(self.dropped.clone());
        self.entered.notify_one();
        std::future::pending().await
    }
}

#[tokio::test]
async fn session_management_creation_cancellation_drops_the_host_future() {
    let mut fixture = Fixture::new().await;
    let entered = Arc::new(Notify::new());
    let dropped = Arc::new(Notify::new());
    fixture
        .set_handler(Some(Arc::new(PendingCreator {
            entered: entered.clone(),
            dropped: dropped.clone(),
        })))
        .await;
    let owner = fixture.session.id().to_string();
    fixture.request_creation(&owner).await;
    timeout(TIMEOUT, entered.notified()).await.unwrap();
    write_frame(
        &mut fixture.writer,
        json!({ "jsonrpc": "2.0", "method": "$/cancelRequest", "params": { "id": 100 } }),
    )
    .await;
    assert_eq!(
        read_frame(&mut fixture.reader).await["error"]["code"],
        -32800
    );
    timeout(TIMEOUT, dropped.notified()).await.unwrap();
}
