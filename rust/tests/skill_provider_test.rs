// Copyright (c) Microsoft Corporation. All rights reserved.

#![allow(clippy::unwrap_used)]

use std::collections::HashMap;
use std::sync::Arc;
use std::time::Duration;

use async_trait::async_trait;
use github_copilot_sdk::skill_provider::{SkillProvider, SkillProviderDescriptor};
use github_copilot_sdk::types::{CloudSessionOptions, SessionConfig, SessionId, Tool};
use github_copilot_sdk::{Client, ClientMode, Error, ErrorKind, ResumeSessionConfig};
use serde_json::{Value, json};
use tokio::io::{AsyncRead, AsyncReadExt, AsyncWrite, AsyncWriteExt, DuplexStream, duplex};
use tokio::sync::Notify;
use tokio::time::timeout;

const TIMEOUT: Duration = Duration::from_secs(2);

async fn write_framed(writer: &mut (impl AsyncWrite + Unpin), value: &Value) {
    let body = serde_json::to_vec(value).unwrap();
    let header = format!("Content-Length: {}\r\n\r\n", body.len());
    writer.write_all(header.as_bytes()).await.unwrap();
    writer.write_all(&body).await.unwrap();
    writer.flush().await.unwrap();
}

async fn read_framed(reader: &mut (impl AsyncRead + Unpin)) -> Value {
    timeout(TIMEOUT, read_framed_untimed(reader)).await.unwrap()
}

async fn read_framed_untimed(reader: &mut (impl AsyncRead + Unpin)) -> Value {
    let mut header = String::new();
    loop {
        let mut byte = [0u8; 1];
        reader.read_exact(&mut byte).await.unwrap();
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
    let mut body = vec![0; length];
    reader.read_exact(&mut body).await.unwrap();
    serde_json::from_slice(&body).unwrap()
}

fn make_client() -> (Client, FakeServer) {
    make_client_with_mode(ClientMode::default())
}

fn make_client_with_mode(mode: ClientMode) -> (Client, FakeServer) {
    let (client_write, server_read) = duplex(8192);
    let (server_write, client_read) = duplex(8192);
    let client = Client::from_streams_with_mode_for_test(
        client_read,
        client_write,
        std::env::temp_dir(),
        mode,
    )
    .unwrap();
    (
        client,
        FakeServer {
            read: server_read,
            write: server_write,
            session_id: String::new(),
        },
    )
}

struct FakeServer {
    read: DuplexStream,
    write: DuplexStream,
    session_id: String,
}

impl FakeServer {
    async fn read_request(&mut self) -> Value {
        read_framed(&mut self.read).await
    }

    async fn respond(&mut self, request: &Value, result: Value) {
        let response = json!({
            "jsonrpc": "2.0",
            "id": request["id"].as_u64().unwrap(),
            "result": result,
        });
        write_framed(&mut self.write, &response).await;
    }

    async fn respond_error(&mut self, request: &Value, code: i64, message: &str) {
        let response = json!({
            "jsonrpc": "2.0",
            "id": request["id"].as_u64().unwrap(),
            "error": { "code": code, "message": message },
        });
        write_framed(&mut self.write, &response).await;
    }

    async fn send_request(&mut self, id: u64, method: &str, params: Value) {
        let request = json!({
            "jsonrpc": "2.0",
            "id": id,
            "method": method,
            "params": params,
        });
        write_framed(&mut self.write, &request).await;
    }

    async fn send_notification(&mut self, method: &str, params: Value) {
        let notification = json!({
            "jsonrpc": "2.0",
            "method": method,
            "params": params,
        });
        write_framed(&mut self.write, &notification).await;
    }

    async fn read_response(&mut self) -> Value {
        read_framed(&mut self.read).await
    }
}

fn descriptor(name: &str, description: &str) -> SkillProviderDescriptor {
    SkillProviderDescriptor {
        name: name.to_string(),
        description: description.to_string(),
        ..Default::default()
    }
}

#[derive(Clone, Default)]
struct CallLog(Arc<std::sync::Mutex<Vec<String>>>);

impl CallLog {
    fn push(&self, call: impl Into<String>) {
        self.0.lock().unwrap().push(call.into());
    }

    fn snapshot(&self) -> Vec<String> {
        self.0.lock().unwrap().clone()
    }
}

struct RecordingProvider {
    skills: Vec<SkillProviderDescriptor>,
    markdown: HashMap<String, String>,
    calls: CallLog,
}

impl RecordingProvider {
    fn new() -> Self {
        Self {
            skills: vec![descriptor("review", "Reviews code")],
            markdown: HashMap::from([("review".to_string(), "Review carefully.".to_string())]),
            calls: CallLog::default(),
        }
    }

    fn empty() -> Self {
        Self {
            skills: Vec::new(),
            markdown: HashMap::new(),
            calls: CallLog::default(),
        }
    }
}

#[async_trait]
impl SkillProvider for RecordingProvider {
    async fn list_skills(&self) -> std::result::Result<Vec<SkillProviderDescriptor>, Error> {
        self.calls.push("list");
        Ok(self.skills.clone())
    }

    async fn read_skill(&self, name: &str) -> std::result::Result<Option<String>, Error> {
        self.calls.push(format!("read:{name}"));
        Ok(self.markdown.get(name).cloned())
    }
}

struct FailingProvider {
    fail_list: bool,
    secret: &'static str,
}

#[async_trait]
impl SkillProvider for FailingProvider {
    async fn list_skills(&self) -> std::result::Result<Vec<SkillProviderDescriptor>, Error> {
        if self.fail_list {
            Err(Error::with_message(ErrorKind::InvalidConfig, self.secret))
        } else {
            Ok(vec![descriptor("review", "Reviews code")])
        }
    }

    async fn read_skill(&self, _name: &str) -> std::result::Result<Option<String>, Error> {
        Err(Error::with_message(ErrorKind::InvalidConfig, self.secret))
    }
}

struct NotifyOnDrop(Arc<Notify>);

impl Drop for NotifyOnDrop {
    fn drop(&mut self) {
        self.0.notify_one();
    }
}

/// Blocks every call until the SDK drops its future.
struct BlockingProvider {
    entered: Arc<Notify>,
    dropped: Arc<Notify>,
}

#[async_trait]
impl SkillProvider for BlockingProvider {
    async fn list_skills(&self) -> std::result::Result<Vec<SkillProviderDescriptor>, Error> {
        let _dropped = NotifyOnDrop(self.dropped.clone());
        self.entered.notify_one();
        std::future::pending().await
    }

    async fn read_skill(&self, _name: &str) -> std::result::Result<Option<String>, Error> {
        let _dropped = NotifyOnDrop(self.dropped.clone());
        self.entered.notify_one();
        std::future::pending().await
    }
}

fn requested_session_id(request: &Value) -> &str {
    request["params"]["sessionId"].as_str().unwrap()
}

async fn create_session(
    client: &Client,
    server: &mut FakeServer,
    config: SessionConfig,
) -> github_copilot_sdk::session::Session {
    let handle = tokio::spawn({
        let client = client.clone();
        async move { client.create_session(config).await.unwrap() }
    });
    let request = server.read_request().await;
    assert_eq!(request["method"], "session.create");
    server.session_id = requested_session_id(&request).to_string();
    server
        .respond(
            &request,
            json!({
                "sessionId": server.session_id,
                "workspacePath": "/tmp/workspace",
            }),
        )
        .await;
    timeout(TIMEOUT, handle).await.unwrap().unwrap()
}

async fn respond_to_resume(server: &mut FakeServer) {
    let request = server.read_request().await;
    assert_eq!(request["method"], "session.resume");
    server
        .respond(
            &request,
            json!({
                "sessionId": requested_session_id(&request),
                "workspacePath": "/tmp/workspace",
            }),
        )
        .await;

    let reload = server.read_request().await;
    assert_eq!(reload["method"], "session.skills.reload");
    server.respond(&reload, json!({})).await;
}

async fn disconnect_session(
    session: &github_copilot_sdk::session::Session,
    server: &mut FakeServer,
) {
    let disconnect = session.disconnect();
    tokio::pin!(disconnect);
    let detach = tokio::select! {
        request = server.read_request() => request,
        result = &mut disconnect => panic!("disconnect finished before detach request: {result:?}"),
    };
    assert_eq!(detach["method"], "session.detach");
    server
        .respond(&detach, json!({ "success": true, "error": null }))
        .await;
    timeout(TIMEOUT, disconnect).await.unwrap().unwrap();
}

fn assert_no_provider(response: &Value, session_id: &str) {
    assert_eq!(response["error"]["code"], -32603);
    assert_eq!(
        response["error"]["message"],
        format!("No skill provider for session: {session_id}")
    );
    assert!(response["error"].get("data").is_none());
}

fn assert_session_not_found(response: &Value, session_id: &str) {
    assert_eq!(response["error"]["code"], -32603);
    assert_eq!(
        response["error"]["message"],
        format!("Session not found: {session_id}")
    );
}

#[test]
fn descriptor_omits_unset_optional_fields_and_config_debug_redacts_provider() {
    let value = serde_json::to_value(descriptor("review", "Reviews code")).unwrap();
    assert_eq!(
        value,
        json!({ "name": "review", "description": "Reviews code" })
    );

    let provider: Arc<dyn SkillProvider> = Arc::new(RecordingProvider::new());
    let config = SessionConfig::default().with_skill_provider(provider.clone());
    let cloned = config.clone();
    assert!(Arc::ptr_eq(
        config.skill_provider.as_ref().unwrap(),
        cloned.skill_provider.as_ref().unwrap()
    ));
    let debug = format!("{config:?}");
    assert!(debug.contains("skill_provider"));
    assert!(debug.contains("<set>"));

    let resume =
        ResumeSessionConfig::new(SessionId::new("resume-id")).with_skill_provider(provider.clone());
    let cloned_resume = resume.clone();
    assert!(Arc::ptr_eq(
        resume.skill_provider.as_ref().unwrap(),
        cloned_resume.skill_provider.as_ref().unwrap()
    ));
    assert!(format!("{resume:?}").contains("skill_provider"));
}

#[tokio::test]
async fn create_and_resume_payloads_flag_only_when_provider_is_supplied() {
    let (client, mut server) = make_client();

    let session = create_session(&client, &mut server, SessionConfig::default()).await;
    let resume = tokio::spawn({
        let client = client.clone();
        let session_id = session.id().clone();
        async move {
            client
                .resume_session(ResumeSessionConfig::new(session_id))
                .await
                .unwrap()
        }
    });
    respond_to_resume(&mut server).await;
    let _resumed = timeout(TIMEOUT, resume).await.unwrap().unwrap();

    let provider: Arc<dyn SkillProvider> = Arc::new(RecordingProvider::new());
    let create = tokio::spawn({
        let client = client.clone();
        let provider = provider.clone();
        async move {
            client
                .create_session(SessionConfig::default().with_skill_provider(provider))
                .await
                .unwrap()
        }
    });
    let create_request = server.read_request().await;
    assert_eq!(create_request["params"]["hasSkillProvider"], true);
    assert!(create_request["params"].get("skillProvider").is_none());
    server.session_id = requested_session_id(&create_request).to_string();
    server
        .respond(
            &create_request,
            json!({ "sessionId": server.session_id, "workspacePath": "/tmp/workspace" }),
        )
        .await;
    let session_with_provider = timeout(TIMEOUT, create).await.unwrap().unwrap();

    let resume_with_provider = tokio::spawn({
        let client = client.clone();
        let provider = Arc::new(RecordingProvider::new());
        let session_id = session_with_provider.id().clone();
        async move {
            client
                .resume_session(ResumeSessionConfig::new(session_id).with_skill_provider(provider))
                .await
                .unwrap()
        }
    });
    let resume_request = server.read_request().await;
    assert_eq!(resume_request["method"], "session.resume");
    assert_eq!(resume_request["params"]["hasSkillProvider"], true);
    assert!(resume_request["params"].get("skillProvider").is_none());
    server
        .respond(
            &resume_request,
            json!({ "sessionId": requested_session_id(&resume_request), "workspacePath": "/tmp/workspace" }),
        )
        .await;
    let reload = server.read_request().await;
    server.respond(&reload, json!({})).await;
    let _ = timeout(TIMEOUT, resume_with_provider)
        .await
        .unwrap()
        .unwrap();
}

#[tokio::test]
async fn empty_mode_keeps_enable_skills_default_with_provider() {
    let (client, mut server) = make_client_with_mode(ClientMode::Empty);
    let provider: Arc<dyn SkillProvider> = Arc::new(RecordingProvider::new());
    let create = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session(
                    SessionConfig::default()
                        .with_available_tools(Vec::<String>::new())
                        .with_tools(Vec::<Tool>::new())
                        .with_skill_provider(provider),
                )
                .await
                .unwrap()
        }
    });
    let request = server.read_request().await;
    assert_eq!(request["params"]["enableSkills"], false);
    assert_eq!(request["params"]["hasSkillProvider"], true);
    server.session_id = requested_session_id(&request).to_string();
    server
        .respond(
            &request,
            json!({ "sessionId": server.session_id, "workspacePath": "/tmp/workspace" }),
        )
        .await;
    let update = server.read_request().await;
    assert_eq!(update["method"], "session.options.update");
    server.respond(&update, json!({ "success": true })).await;
    let _ = timeout(TIMEOUT, create).await.unwrap().unwrap();
}

#[tokio::test]
async fn serves_early_create_and_resume_callbacks() {
    let (client, mut server) = make_client();
    let provider: Arc<dyn SkillProvider> = Arc::new(RecordingProvider::new());
    let create = tokio::spawn({
        let client = client.clone();
        let provider = provider.clone();
        async move {
            client
                .create_session(
                    SessionConfig::default()
                        .with_session_id("early-create")
                        .with_skill_provider(provider),
                )
                .await
                .unwrap()
        }
    });
    let create_request = server.read_request().await;
    assert_eq!(create_request["method"], "session.create");
    server
        .send_request(
            10,
            "skillProvider.list",
            json!({ "sessionId": "early-create" }),
        )
        .await;
    let list_response = server.read_response().await;
    assert_eq!(list_response["result"]["skills"][0]["name"], "review");
    server
        .respond(
            &create_request,
            json!({ "sessionId": "early-create", "workspacePath": "/tmp/workspace" }),
        )
        .await;
    let _ = timeout(TIMEOUT, create).await.unwrap().unwrap();

    let resume_provider: Arc<dyn SkillProvider> = Arc::new(RecordingProvider::new());
    let resume = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .resume_session(
                    ResumeSessionConfig::new(SessionId::new("early-resume"))
                        .with_skill_provider(resume_provider),
                )
                .await
                .unwrap()
        }
    });
    let resume_request = server.read_request().await;
    assert_eq!(resume_request["method"], "session.resume");
    server
        .send_request(
            11,
            "skillProvider.list",
            json!({ "sessionId": "early-resume" }),
        )
        .await;
    let list_response = server.read_response().await;
    assert_eq!(list_response["result"]["skills"][0]["name"], "review");
    server
        .respond(
            &resume_request,
            json!({ "sessionId": "early-resume", "workspacePath": "/tmp/workspace" }),
        )
        .await;
    let reload = server.read_request().await;
    server.respond(&reload, json!({})).await;
    let _ = timeout(TIMEOUT, resume).await.unwrap().unwrap();
}

#[tokio::test]
async fn cloud_sessions_reject_provider_before_rpc() {
    let (client, mut server) = make_client();
    let provider = Arc::new(RecordingProvider::new());
    let calls = provider.calls.clone();
    let error = match client
        .create_session(
            SessionConfig::default()
                .with_skill_provider(provider)
                .with_cloud(CloudSessionOptions::default()),
        )
        .await
    {
        Ok(_) => panic!("cloud session unexpectedly accepted a skill provider"),
        Err(error) => error,
    };

    assert_eq!(
        error.to_string(),
        "Skill providers are not supported for cloud sessions."
    );
    assert_eq!(calls.snapshot(), Vec::<String>::new());
    assert!(
        timeout(Duration::from_millis(100), server.read_request())
            .await
            .is_err()
    );
}

#[tokio::test]
async fn dispatches_list_read_empty_and_not_found() {
    let (client, mut server) = make_client();
    let provider = Arc::new(RecordingProvider {
        skills: vec![
            descriptor("review", "Reviews code"),
            SkillProviderDescriptor {
                name: "deploy".to_string(),
                description: "Deploys service".to_string(),
                argument_hint: Some("[environment]".to_string()),
                disable_model_invocation: Some(true),
                user_invocable: Some(false),
            },
        ],
        markdown: HashMap::from([("deploy".to_string(), "# deploy".to_string())]),
        calls: CallLog::default(),
    });
    let session = create_session(
        &client,
        &mut server,
        SessionConfig::default().with_skill_provider(provider),
    )
    .await;
    let session_id = session.id().to_string();

    server
        .send_request(20, "skillProvider.list", json!({ "sessionId": session_id }))
        .await;
    let response = server.read_response().await;
    assert_eq!(
        response["result"]["skills"][0],
        json!({
            "name": "review",
            "description": "Reviews code",
        })
    );
    assert_eq!(
        response["result"]["skills"][1]["argumentHint"],
        "[environment]"
    );
    assert_eq!(
        response["result"]["skills"][1]["disableModelInvocation"],
        true
    );
    assert_eq!(response["result"]["skills"][1]["userInvocable"], false);

    server
        .send_request(
            21,
            "skillProvider.read",
            json!({ "sessionId": session_id, "name": "deploy" }),
        )
        .await;
    let response = server.read_response().await;
    assert_eq!(response["result"], json!({ "markdown": "# deploy" }));

    server
        .send_request(
            22,
            "skillProvider.read",
            json!({ "sessionId": session_id, "name": "missing" }),
        )
        .await;
    let response = server.read_response().await;
    assert_eq!(response["result"], json!({ "markdown": null }));

    let empty = Arc::new(RecordingProvider::empty());
    let empty_session = create_session(
        &client,
        &mut server,
        SessionConfig::default()
            .with_session_id("empty-catalog")
            .with_skill_provider(empty),
    )
    .await;
    server
        .send_request(
            23,
            "skillProvider.list",
            json!({ "sessionId": empty_session.id() }),
        )
        .await;
    let response = server.read_response().await;
    assert_eq!(response["result"], json!({ "skills": [] }));
}

#[tokio::test]
async fn provider_errors_are_generic_and_invalid_params_are_rejected() {
    let (client, mut server) = make_client();
    let secret = "db-password-in-error";
    let session = create_session(
        &client,
        &mut server,
        SessionConfig::default().with_skill_provider(Arc::new(FailingProvider {
            fail_list: true,
            secret,
        })),
    )
    .await;
    let session_id = session.id().to_string();

    server
        .send_request(30, "skillProvider.list", json!({ "sessionId": session_id }))
        .await;
    let response = server.read_response().await;
    assert_eq!(response["error"]["code"], -32603);
    assert_eq!(
        response["error"]["message"],
        "Skill provider listSkills failed"
    );
    assert!(response["error"].get("data").is_none());
    assert!(!response.to_string().contains(secret));

    let read_session = create_session(
        &client,
        &mut server,
        SessionConfig::default()
            .with_session_id("failing-read")
            .with_skill_provider(Arc::new(FailingProvider {
                fail_list: false,
                secret,
            })),
    )
    .await;
    server
        .send_request(
            31,
            "skillProvider.read",
            json!({ "sessionId": read_session.id(), "name": "review" }),
        )
        .await;
    let response = server.read_response().await;
    assert_eq!(response["error"]["code"], -32603);
    assert_eq!(
        response["error"]["message"],
        "Skill provider readSkill failed"
    );
    assert!(response["error"].get("data").is_none());
    assert!(!response.to_string().contains(secret));

    server
        .send_request(
            32,
            "skillProvider.read",
            json!({ "sessionId": read_session.id() }),
        )
        .await;
    let response = server.read_response().await;
    assert_eq!(response["error"]["code"], -32602);
}

#[tokio::test]
async fn cancel_request_drops_provider_future_and_reports_cancellation() {
    let (client, mut server) = make_client();
    let entered = Arc::new(Notify::new());
    let dropped = Arc::new(Notify::new());
    let session = create_session(
        &client,
        &mut server,
        SessionConfig::default().with_skill_provider(Arc::new(BlockingProvider {
            entered: entered.clone(),
            dropped: dropped.clone(),
        })),
    )
    .await;

    for (id, method, params) in [
        (
            60,
            "skillProvider.list",
            json!({ "sessionId": session.id() }),
        ),
        (
            61,
            "skillProvider.read",
            json!({ "sessionId": session.id(), "name": "review" }),
        ),
    ] {
        server.send_request(id, method, params).await;
        timeout(TIMEOUT, entered.notified()).await.unwrap();
        server
            .send_notification("$/cancelRequest", json!({ "id": id }))
            .await;

        let response = server.read_response().await;
        assert_eq!(response["id"], id);
        assert_eq!(response["error"]["code"], -32800);
        timeout(TIMEOUT, dropped.notified()).await.unwrap();
    }
}

#[tokio::test]
async fn connection_close_drops_provider_future() {
    let (client, mut server) = make_client();
    let entered = Arc::new(Notify::new());
    let dropped = Arc::new(Notify::new());
    let session = create_session(
        &client,
        &mut server,
        SessionConfig::default().with_skill_provider(Arc::new(BlockingProvider {
            entered: entered.clone(),
            dropped: dropped.clone(),
        })),
    )
    .await;

    server
        .send_request(
            62,
            "skillProvider.list",
            json!({ "sessionId": session.id() }),
        )
        .await;
    timeout(TIMEOUT, entered.notified()).await.unwrap();
    drop(server);

    timeout(TIMEOUT, dropped.notified()).await.unwrap();
}

#[tokio::test]
async fn missing_provider_reports_generic_error() {
    let (client, mut server) = make_client();
    let session = create_session(&client, &mut server, SessionConfig::default()).await;
    let session_id = session.id().to_string();
    server
        .send_request(
            41,
            "skillProvider.read",
            json!({ "sessionId": session_id, "name": "review" }),
        )
        .await;
    let response = server.read_response().await;
    assert_no_provider(&response, &session_id);
}

#[tokio::test]
async fn teardown_and_failed_open_stop_serving_provider() {
    let (client, mut server) = make_client();
    let provider = Arc::new(RecordingProvider::new());
    let calls = provider.calls.clone();
    let session = create_session(
        &client,
        &mut server,
        SessionConfig::default().with_skill_provider(provider),
    )
    .await;
    let session_id = session.id().to_string();
    disconnect_session(&session, &mut server).await;

    let failed_provider = Arc::new(RecordingProvider::new());
    let failed_calls = failed_provider.calls.clone();
    let failed_create = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session(
                    SessionConfig::default()
                        .with_session_id("failed-open")
                        .with_skill_provider(failed_provider),
                )
                .await
        }
    });
    let request = server.read_request().await;
    assert_eq!(request["method"], "session.create");
    server
        .respond_error(&request, -32603, "create failed")
        .await;
    let error = match timeout(TIMEOUT, failed_create).await.unwrap().unwrap() {
        Ok(_) => panic!("failed-open session unexpectedly succeeded"),
        Err(error) => error,
    };
    assert!(error.to_string().contains("create failed"));
    server
        .send_request(50, "skillProvider.list", json!({ "sessionId": session_id }))
        .await;
    let response = server.read_response().await;
    assert_eq!(response["id"], 50);
    assert_session_not_found(&response, &session_id);
    server
        .send_request(
            51,
            "skillProvider.list",
            json!({ "sessionId": "failed-open" }),
        )
        .await;
    let response = server.read_response().await;
    assert_eq!(response["id"], 51);
    assert_session_not_found(&response, "failed-open");
    assert_eq!(calls.snapshot(), Vec::<String>::new());
    assert_eq!(failed_calls.snapshot(), Vec::<String>::new());
}

#[tokio::test]
async fn dropped_session_answers_provider_callbacks_with_an_error() {
    let (client, mut server) = make_client();
    let provider = Arc::new(RecordingProvider::new());
    let calls = provider.calls.clone();
    let session = create_session(
        &client,
        &mut server,
        SessionConfig::default().with_skill_provider(provider),
    )
    .await;
    let session_id = session.id().to_string();
    drop(session);

    server
        .send_request(60, "skillProvider.list", json!({ "sessionId": session_id }))
        .await;
    let response = server.read_response().await;
    assert_eq!(response["id"], 60);
    assert_session_not_found(&response, &session_id);

    server
        .send_request(61, "skillProvider.read", json!({ "name": "review" }))
        .await;
    let response = server.read_response().await;
    assert_eq!(response["id"], 61);
    assert_eq!(response["error"]["code"], -32602);
    assert_eq!(calls.snapshot(), Vec::<String>::new());
}

#[tokio::test]
async fn resume_rebinds_provider() {
    let (client, mut server) = make_client();
    let original = Arc::new(RecordingProvider::new());
    let original_calls = original.calls.clone();
    let session = create_session(
        &client,
        &mut server,
        SessionConfig::default().with_skill_provider(original),
    )
    .await;
    let session_id = session.id().clone();
    disconnect_session(&session, &mut server).await;

    let replacement = Arc::new(RecordingProvider::new());
    let replacement_calls = replacement.calls.clone();
    let resume = tokio::spawn({
        let client = client.clone();
        let session_id = session_id.clone();
        async move {
            client
                .resume_session(
                    ResumeSessionConfig::new(session_id).with_skill_provider(replacement),
                )
                .await
                .unwrap()
        }
    });
    respond_to_resume(&mut server).await;
    let resumed = timeout(TIMEOUT, resume).await.unwrap().unwrap();
    server
        .send_request(
            70,
            "skillProvider.list",
            json!({ "sessionId": resumed.id() }),
        )
        .await;
    let response = server.read_response().await;
    assert_eq!(response["result"]["skills"][0]["name"], "review");
    assert_eq!(original_calls.snapshot(), Vec::<String>::new());
    assert_eq!(replacement_calls.snapshot(), vec!["list"]);
}

#[tokio::test]
async fn failed_resume_restores_the_resident_provider() {
    let (client, mut server) = make_client();
    let resident = Arc::new(RecordingProvider::new());
    let resident_calls = resident.calls.clone();
    let session = create_session(
        &client,
        &mut server,
        SessionConfig::default().with_skill_provider(resident),
    )
    .await;
    let session_id = session.id().clone();

    let replacement = Arc::new(RecordingProvider::new());
    let replacement_calls = replacement.calls.clone();
    let resume = tokio::spawn({
        let client = client.clone();
        let session_id = session_id.clone();
        async move {
            client
                .resume_session(
                    ResumeSessionConfig::new(session_id).with_skill_provider(replacement),
                )
                .await
        }
    });
    let request = server.read_request().await;
    assert_eq!(request["method"], "session.resume");
    server
        .respond_error(&request, -32603, "resume failed")
        .await;
    let error = match timeout(TIMEOUT, resume).await.unwrap().unwrap() {
        Ok(_) => panic!("resume unexpectedly succeeded"),
        Err(error) => error,
    };
    assert!(error.to_string().contains("resume failed"));

    // The runtime keeps the resident binding when a resume fails.
    server
        .send_request(80, "skillProvider.list", json!({ "sessionId": session_id }))
        .await;
    let response = server.read_response().await;
    assert_eq!(response["id"], 80);
    assert_eq!(response["result"]["skills"][0]["name"], "review");
    assert_eq!(resident_calls.snapshot(), vec!["list"]);
    assert_eq!(replacement_calls.snapshot(), Vec::<String>::new());
    drop(session);
}
