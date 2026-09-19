/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

use std::sync::Arc;
use std::sync::atomic::{AtomicUsize, Ordering};

use futures_util::{SinkExt, StreamExt};
use github_copilot_sdk::ahp::{
    AhpConnectionOptions, AhpEndpoint, AhpEndpointOptions, AhpSessionIdentity,
};
use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::tool::ToolHandler;
use github_copilot_sdk::{Error, ErrorKind, SessionConfig, Tool, ToolInvocation, ToolResult};
use serde_json::json;
use tokio::net::{TcpListener, TcpStream};
use tokio::sync::Mutex;
use tokio_util::sync::CancellationToken;

use super::support::{DEFAULT_TEST_TOKEN, with_dedicated_e2e_context};

struct Encrypt(Arc<AtomicUsize>);

#[async_trait::async_trait]
impl ToolHandler for Encrypt {
    async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error> {
        assert_eq!(invocation.arguments["input"], "Hello");
        self.0.fetch_add(1, Ordering::SeqCst);
        Ok(ToolResult::Text("HELLO".into()))
    }
}

fn config(calls: Arc<AtomicUsize>) -> SessionConfig {
    SessionConfig::default()
        .with_github_token(DEFAULT_TEST_TOKEN)
        .with_permission_handler(Arc::new(ApproveAllHandler))
        .with_tools(vec![
            Tool::new("encrypt_string")
                .with_description("Encrypts a string")
                .with_parameters(json!({
                    "type": "object",
                    "properties": {"input": {"type": "string", "description": "String to encrypt"}},
                    "required": ["input"],
                }))
                .with_handler(Arc::new(Encrypt(calls))),
        ])
}

async fn serve(stream: TcpStream, endpoint: AhpEndpoint, stopped: CancellationToken) {
    let socket = tokio_tungstenite::accept_async(stream).await.unwrap();
    let (sink, mut source) = socket.split();
    let sink = Arc::new(Mutex::new(sink));
    let close_token = stopped.child_token();
    let on_close_token = close_token.clone();
    let connection = endpoint
        .open_connection(AhpConnectionOptions {
            on_message: Arc::new(move |message| {
                let sink = sink.clone();
                Box::pin(async move {
                    sink.lock()
                        .await
                        .send(message.into())
                        .await
                        .map_err(|e| Error::with_message(ErrorKind::Io, e.to_string()))
                })
            }),
            on_close: Some(Arc::new(move |_| on_close_token.cancel())),
        })
        .await
        .unwrap();
    loop {
        tokio::select! {
            _ = close_token.cancelled() => break,
            message = source.next() => match message {
                Some(Ok(tokio_tungstenite::tungstenite::Message::Text(message))) =>
                    connection.send(message).await.unwrap(),
                Some(Ok(tokio_tungstenite::tungstenite::Message::Close(_))) | None => break,
                Some(Err(error)) => panic!("AHP WebSocket failed: {error}"),
                _ => {}
            },
        }
    }
    connection.close().await.unwrap();
}

async fn drive(create: bool) {
    // Reuse the same recorded model exchanges as the ordinary SDK tool test.
    // Only the client transport changes; model calls still go through CapiProxy.
    with_dedicated_e2e_context("tools", "invokes_custom_tool", |ctx| {
        Box::pin(async move {
            let client = ctx.start_client().await;
            let calls = Arc::new(AtomicUsize::new(0));
            let creations = Arc::new(AtomicUsize::new(0));
            let sessions = Arc::new(Mutex::new(Vec::new()));
            let private = client
                .create_session(ctx.approve_all_session_config())
                .await
                .unwrap();
            let existing_id = if create {
                String::new()
            } else {
                let session = client.create_session(config(calls.clone())).await.unwrap();
                let id = session.id().to_string();
                sessions.lock().await.push(session);
                id
            };
            let endpoint = client
                .create_ahp_endpoint(AhpEndpointOptions {
                    allow_session_creation: Some(create),
                    on_create_session: Some(Arc::new({
                        let client = client.clone();
                        let sessions = sessions.clone();
                        let calls = calls.clone();
                        let creations = creations.clone();
                        move |request, _context| {
                            let client = client.clone();
                            let sessions = sessions.clone();
                            let calls = calls.clone();
                            let creations = creations.clone();
                            Box::pin(async move {
                                let session = client
                                    .create_session(
                                        config(calls).with_session_id(request.requested_session_id),
                                    )
                                    .await?;
                                let session_id = session.id().to_string();
                                sessions.lock().await.push(session);
                                creations.fetch_add(1, Ordering::SeqCst);
                                Ok(AhpSessionIdentity { session_id })
                            })
                        }
                    })),
                    on_list_sessions: Some(Arc::new({
                        let sessions = sessions.clone();
                        move |(), _context| {
                            let sessions = sessions.clone();
                            Box::pin(async move {
                                Ok(sessions
                                    .lock()
                                    .await
                                    .iter()
                                    .map(|session| AhpSessionIdentity {
                                        session_id: session.id().to_string(),
                                    })
                                    .collect())
                            })
                        }
                    })),
                    ..Default::default()
                })
                .await
                .unwrap();
            let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
            let url = format!("ws://{}", listener.local_addr().unwrap());
            let stopped = CancellationToken::new();
            let server = tokio::spawn({
                let endpoint = endpoint.clone();
                let stopped = stopped.clone();
                async move {
                    let mut connections = tokio::task::JoinSet::new();
                    loop {
                        tokio::select! {
                            _ = stopped.cancelled() => break,
                            accepted = listener.accept() => {
                                let (stream, _) = accepted.unwrap();
                                connections.spawn(serve(stream, endpoint.clone(), stopped.clone()));
                            }
                        }
                    }
                    while let Some(result) = connections.join_next().await {
                        result.unwrap();
                    }
                }
            });
            let output = tokio::process::Command::new("node")
                .arg(
                    ctx.repo_root()
                        .join("nodejs/test/fixtures/ahp-drive-agent.mjs"),
                )
                .args([url, existing_id, private.id().to_string()])
                .kill_on_drop(true)
                .output()
                .await
                .unwrap();
            stopped.cancel();
            server.await.unwrap();
            endpoint.dispose().await.unwrap();
            assert!(
                output.status.success(),
                "AHP client failed:\n{}\n{}",
                String::from_utf8_lossy(&output.stdout),
                String::from_utf8_lossy(&output.stderr)
            );
            assert!(
                String::from_utf8_lossy(&output.stdout).contains("AHP_TOOL_TURN_AND_RECONNECT_OK")
            );
            assert_eq!(calls.load(Ordering::SeqCst), 1);
            assert_eq!(creations.load(Ordering::SeqCst), usize::from(create));
            // Endpoint disposal must not destroy the application's owners.
            for session in sessions.lock().await.iter() {
                assert!(!session.get_events().await.unwrap().is_empty());
                session.disconnect().await.unwrap();
            }
            private.disconnect().await.unwrap();
            client.stop().await.unwrap();
        })
    })
    .await;
}

#[tokio::test]
async fn standard_ahp_client_drives_existing_rust_session() {
    drive(false).await;
}

#[tokio::test]
async fn standard_ahp_client_creates_and_drives_rust_session() {
    drive(true).await;
}
