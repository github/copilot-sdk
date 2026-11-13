// Copyright (c) Microsoft Corporation. All rights reserved.
#![cfg(test)]

use github_copilot_sdk::{AutoModeRecoveryContext, ResumeSessionConfig, SessionErrorKind};
use serde_json::json;
use tokio::sync::{mpsc, oneshot};

use super::*;

struct LegacyHandler;

#[tokio::test]
async fn exact_hex_provider_resume_skips_host_lookup() {
    for recovery in [false, true] {
        let (client_write, server_read) = duplex(8192);
        let (server_write, client_read) = duplex(8192);
        let client = Client::from_streams_with_session_fs_config(
            client_read,
            client_write,
            std::env::temp_dir(),
            SessionFsConfig::new("/workspace", "/sessions", SessionFsConventions::Posix),
        )
        .unwrap();
        let mut server = FakeServer {
            read: server_read,
            write: server_write,
            session_id: "abcdef0".to_owned(),
        };
        let start = tokio::spawn(async move {
            client
                .resume_session(
                    ResumeSessionConfig::new(SessionId::from("abcdef0"))
                        .with_auto_mode_recovery(recovery)
                        .with_auto_mode_switch_handler(Arc::new(LegacyHandler))
                        .with_session_fs_provider(Arc::new(RecordingFsProvider::new())),
                )
                .await
        });
        let resume = timeout(TIMEOUT, server.read_request()).await.unwrap();
        assert_eq!(resume["method"], "session.resume");
        assert_eq!(resume["params"]["sessionId"], "abcdef0");
        server
            .respond(
                &resume,
                json!({"sessionId": "abcdef0", "capabilities": {"ui": {"autoModeRecovery": true}}}),
            )
            .await;
        let reload = timeout(TIMEOUT, server.read_request()).await.unwrap();
        assert_eq!(reload["method"], "session.skills.reload");
        server.respond(&reload, json!({})).await;
        let session = timeout(TIMEOUT, start).await.unwrap().unwrap().unwrap();
        assert_eq!(session.id().as_str(), "abcdef0");
        disconnect(session, server).await;
    }
}

#[tokio::test]
async fn uuid_prefix_resume_routes_early_callbacks_and_cleanup_to_canonical_id() {
    let canonical = "abcdef12-abcd-4123-8123-123456789abc";
    let (client, mut server) = auto_mode_recovery_client();
    let start = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .resume_session(
                    ResumeSessionConfig::new(SessionId::from("ABCDEF12"))
                        .with_config_directory("configured-home")
                        .with_auto_mode_switch_handler(Arc::new(LegacyHandler))
                        .with_auto_mode_recovery(true),
                )
                .await
        }
    });
    let lookup = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(lookup["method"], "sessions.findByPrefix");
    assert_eq!(lookup["params"]["prefix"], "ABCDEF12");
    assert_eq!(lookup["params"]["configDir"], "configured-home");
    assert_eq!(client.registered_session_count_for_test(), 0);
    server
        .respond(&lookup, json!({"sessionId": canonical}))
        .await;
    let resume = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(resume["method"], "session.resume");
    assert_eq!(resume["params"]["sessionId"], canonical);
    write_framed(
        &mut server.write,
        &serde_json::to_vec(&json!({
            "jsonrpc": "2.0", "id": 991,
            "method": "autoModeSwitch.request",
            "params": {"sessionId": canonical, "errorCode": "model_overloaded"}
        }))
        .unwrap(),
    )
    .await;
    let callback = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(callback["id"], 991);
    assert_eq!(callback["result"]["response"], "yes_always");
    server
        .respond(&resume, json!({"sessionId": canonical}))
        .await;
    let detach = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(detach["method"], "session.detach");
    assert_eq!(detach["params"]["sessionId"], canonical);
    server.respond(&detach, json!({"success": true})).await;
    let error = timeout(TIMEOUT, start)
        .await
        .unwrap()
        .unwrap()
        .err()
        .unwrap();
    assert_eq!(
        *error.kind(),
        ErrorKind::Session(SessionErrorKind::AutoModeRecoveryNotSupported)
    );
    assert_eq!(client.registered_session_count_for_test(), 0);
}

#[tokio::test]
async fn unresolved_uuid_prefix_leaves_callbacks_unregistered() {
    let (client, mut server) = auto_mode_recovery_client();
    let start = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .resume_session(
                    ResumeSessionConfig::new(SessionId::from("abcdef12"))
                        .with_auto_mode_switch_handler(Arc::new(LegacyHandler))
                        .with_auto_mode_recovery(true),
                )
                .await
        }
    });
    let lookup = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(lookup["method"], "sessions.findByPrefix");
    server.respond(&lookup, serde_json::json!({})).await;
    let error = timeout(TIMEOUT, start)
        .await
        .unwrap()
        .unwrap()
        .err()
        .unwrap();
    assert_eq!(
        *error.kind(),
        ErrorKind::Session(SessionErrorKind::NotFound(SessionId::from("abcdef12")))
    );
    assert_eq!(client.registered_session_count_for_test(), 0);
}

#[tokio::test]
async fn exact_hex_resume_uses_configured_storage_without_lookup() {
    let canonical = "abcdef0";
    let (client, mut server) = auto_mode_recovery_client();
    let start = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .resume_session(
                    ResumeSessionConfig::new(SessionId::from("abcdef0"))
                        .with_config_directory("configured-home"),
                )
                .await
        }
    });
    let resume = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(resume["method"], "session.resume");
    assert_eq!(resume["params"]["configDir"], "configured-home");
    assert_eq!(resume["params"]["sessionId"], canonical);
    server
        .respond(&resume, json!({"sessionId": canonical}))
        .await;
    let reload = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(reload["method"], "session.skills.reload");
    server.respond(&reload, json!({})).await;
    let session = timeout(TIMEOUT, start).await.unwrap().unwrap().unwrap();
    disconnect(session, server).await;
}

#[test]
fn ui_capabilities_keeps_the_existing_struct_literal() {
    let capabilities = github_copilot_sdk::UiCapabilities {
        elicitation: Some(true),
        mcp_apps: None,
        canvases: None,
    };
    assert_eq!(capabilities.elicitation, Some(true));

    let capabilities = github_copilot_sdk::session_events::CapabilitiesChangedUI {
        canvases: Some(true),
        elicitation: Some(false),
        mcp_apps: None,
    };
    let github_copilot_sdk::session_events::CapabilitiesChangedUI {
        canvases,
        elicitation,
        mcp_apps,
    } = capabilities;
    assert_eq!(
        (canvases, elicitation, mcp_apps),
        (Some(true), Some(false), None)
    );
}

#[test]
fn direct_registration_types_keep_legacy_struct_construction() {
    let result = github_copilot_sdk::rpc::UIRegisterDirectAutoModeSwitchHandlerResult {
        handle: "registration-1".to_owned(),
    };
    let github_copilot_sdk::rpc::UIRegisterDirectAutoModeSwitchHandlerResult { handle } = result;
    assert_eq!(handle, "registration-1");

    let result = github_copilot_sdk::rpc::SessionUiRegisterDirectAutoModeSwitchHandlerResult {
        handle: "registration-2".to_owned(),
    };
    let github_copilot_sdk::rpc::SessionUiRegisterDirectAutoModeSwitchHandlerResult { handle } =
        result;
    assert_eq!(handle, "registration-2");

    let params = github_copilot_sdk::rpc::SessionUiRegisterDirectAutoModeSwitchHandlerParams {
        session_id: SessionId::from("registration-session"),
    };
    let github_copilot_sdk::rpc::SessionUiRegisterDirectAutoModeSwitchHandlerParams { session_id } =
        params;
    assert_eq!(session_id, SessionId::from("registration-session"));

    let recovery_result: github_copilot_sdk::rpc::SessionUiRegisterDirectAutoModeSwitchHandlerResultWithAutoModeRecovery =
        serde_json::from_value(serde_json::json!({
            "handle": "registration-3",
            "autoModeRecovery": true
        }))
        .expect("recovery-aware result must retain its acknowledgment");
    assert_eq!(recovery_result.auto_mode_recovery, Some(true));
}

#[test]
fn capabilities_changed_event_exposes_recovery_without_changing_legacy_data() {
    let event = github_copilot_sdk::types::SessionEvent {
        id: "event-1".to_owned(),
        timestamp: "2026-11-04T00:00:00Z".to_owned(),
        parent_id: None,
        ephemeral: None,
        agent_id: None,
        debug_cli_received_at_ms: None,
        debug_ws_forwarded_at_ms: None,
        event_type: "capabilities.changed".to_owned(),
        data: serde_json::json!({
            "ui": {
                "autoModeRecovery": true,
                "canvases": true,
                "elicitation": false
            }
        }),
    };

    let legacy = event
        .typed_data::<github_copilot_sdk::session_events::CapabilitiesChangedData>()
        .expect("legacy capability payload must still deserialize");
    assert_eq!(
        legacy.ui.expect("legacy UI capabilities").canvases,
        Some(true)
    );

    let recovery = event
        .capabilities_changed_data_with_auto_mode_recovery()
        .expect("capability event must expose recovery-aware data");
    let ui = recovery
        .ui
        .as_ref()
        .expect("recovery-aware UI capabilities");
    assert_eq!(ui.auto_mode_recovery, Some(true));
    assert_eq!(ui.canvases, Some(true));
    assert_eq!(ui.elicitation, Some(false));
    assert_eq!(
        serde_json::to_value(&recovery).expect("recovery payload must serialize"),
        event.data
    );
    assert!(
        github_copilot_sdk::types::SessionEvent {
            event_type: "session.idle".to_owned(),
            ..event
        }
        .capabilities_changed_data_with_auto_mode_recovery()
        .is_none()
    );
}

#[async_trait]
impl AutoModeSwitchHandler for LegacyHandler {
    async fn handle(
        &self,
        _session_id: SessionId,
        _error_code: Option<String>,
        _retry_after_seconds: Option<f64>,
    ) -> AutoModeSwitchResponse {
        AutoModeSwitchResponse::YesAlways
    }
}

fn auto_mode_recovery_client() -> (Client, FakeServer) {
    let (client_write, server_read) = duplex(8192);
    let (server_write, client_read) = duplex(8192);
    let client =
        Client::from_streams(client_read, client_write, std::env::current_dir().unwrap()).unwrap();
    (
        client,
        FakeServer {
            read: server_read,
            write: server_write,
            session_id: "recovery-session".to_owned(),
        },
    )
}

async fn open(
    resume: bool,
    enabled: bool,
    acknowledged: Option<bool>,
    handler: Arc<dyn AutoModeSwitchHandler>,
) -> (
    Result<github_copilot_sdk::session::Session, github_copilot_sdk::Error>,
    Client,
    FakeServer,
    Value,
) {
    let (client, mut server) = auto_mode_recovery_client();
    let start = tokio::spawn({
        let client = client.clone();
        async move {
            if resume {
                client
                    .resume_session(
                        ResumeSessionConfig::new(SessionId::from("recovery-session"))
                            .with_auto_mode_switch_handler(handler)
                            .with_auto_mode_recovery(enabled),
                    )
                    .await
            } else {
                client
                    .create_session(
                        SessionConfig::default()
                            .with_session_id("recovery-session")
                            .with_auto_mode_switch_handler(handler)
                            .with_auto_mode_recovery(enabled),
                    )
                    .await
            }
        }
    });
    let request = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(
        request["method"],
        if resume {
            "session.resume"
        } else {
            "session.create"
        }
    );
    let mut result = serde_json::json!({"sessionId": server.session_id});
    if let Some(acknowledged) = acknowledged {
        result["capabilities"] = serde_json::json!({"ui": {
            "autoModeRecovery": acknowledged, "elicitation": true,
        }});
    }
    server.respond(&request, result).await;
    if enabled && acknowledged != Some(true) {
        let detach = timeout(TIMEOUT, server.read_request()).await.unwrap();
        assert_eq!(detach["method"], "session.detach");
        assert_eq!(detach["params"]["sessionId"], server.session_id);
        server
            .respond(&detach, serde_json::json!({"success": true}))
            .await;
    } else if resume {
        let reload = timeout(TIMEOUT, server.read_request()).await.unwrap();
        assert_eq!(reload["method"], "session.skills.reload");
        server.respond(&reload, serde_json::json!({})).await;
    }
    let session = timeout(TIMEOUT, start).await.unwrap().unwrap();
    (session, client, server, request)
}

async fn disconnect(session: github_copilot_sdk::session::Session, mut server: FakeServer) {
    let (result, ()) = tokio::join!(session.disconnect(), async {
        let request = timeout(TIMEOUT, server.read_request()).await.unwrap();
        assert_eq!(request["method"], "session.detach");
        server
            .respond(&request, serde_json::json!({"success": true}))
            .await;
    },);
    result.unwrap();
}

#[tokio::test]
async fn enrollment_requires_handler_before_any_wire_request() {
    let (client, _server) = auto_mode_recovery_client();
    let error = client
        .create_session(SessionConfig::default().with_auto_mode_recovery(true))
        .await
        .err()
        .expect("missing handler must fail");
    assert_eq!(*error.kind(), ErrorKind::InvalidConfig);
    for session_id in ["session-1", "abcdef12"] {
        let error = timeout(
            TIMEOUT,
            client.resume_session(
                ResumeSessionConfig::new(SessionId::from(session_id)).with_auto_mode_recovery(true),
            ),
        )
        .await
        .expect("invalid enrollment must not wait for the runtime")
        .err()
        .expect("missing resume handler must fail");
        assert_eq!(*error.kind(), ErrorKind::InvalidConfig);
    }
    assert_eq!(client.registered_session_count_for_test(), 0);
}

#[tokio::test]
async fn missing_or_false_acknowledgment_detaches_and_unregisters() {
    for resume in [false, true] {
        for acknowledged in [None, Some(false)] {
            let (result, client, _server, request) =
                open(resume, true, acknowledged, Arc::new(LegacyHandler)).await;
            let error = result.err().expect("unsupported enrollment must fail");
            assert_eq!(
                *error.kind(),
                ErrorKind::Session(SessionErrorKind::AutoModeRecoveryNotSupported)
            );
            assert_eq!(request["params"]["requestAutoModeRecovery"], true);
            assert_eq!(client.registered_session_count_for_test(), 0);
        }
    }
}

async fn start_unsupported_attachment(
    client: &Client,
    server: &mut FakeServer,
    resume: bool,
) -> tokio::task::JoinHandle<Result<github_copilot_sdk::session::Session, github_copilot_sdk::Error>>
{
    let start = tokio::spawn({
        let client = client.clone();
        async move {
            if resume {
                client
                    .resume_session(
                        ResumeSessionConfig::new(SessionId::from("recovery-session"))
                            .with_auto_mode_switch_handler(Arc::new(LegacyHandler))
                            .with_auto_mode_recovery(true),
                    )
                    .await
            } else {
                client
                    .create_session(
                        SessionConfig::default()
                            .with_session_id("recovery-session")
                            .with_auto_mode_switch_handler(Arc::new(LegacyHandler))
                            .with_auto_mode_recovery(true),
                    )
                    .await
            }
        }
    });
    let request = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(
        request["method"],
        if resume {
            "session.resume"
        } else {
            "session.create"
        }
    );
    server
        .respond(
            &request,
            serde_json::json!({"sessionId": server.session_id}),
        )
        .await;
    start
}

async fn initialization_wire_fence(client: &Client, server: &mut FakeServer) {
    let marker = tokio::spawn({
        let client = client.clone();
        async move { client.call("ping", None).await }
    });
    let request = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(
        request["method"], "ping",
        "same-ID initialization reached the runtime before its predecessor settled"
    );
    server.respond(&request, serde_json::json!({})).await;
    marker.await.unwrap().unwrap();
}

#[derive(Clone, Copy, Debug)]
enum OrdinaryInitialization {
    CreateRpc,
    CreateInterest,
    CloudCreateInterest,
    ResumeInterest,
    ResumeReload,
    CreateOptions,
    ResumeOptions,
}

async fn assert_recovery_waits_for_ordinary_initialization(
    phase: OrdinaryInitialization,
    cancel: bool,
) {
    let (client, mut server) = auto_mode_recovery_client();
    let resume = matches!(
        phase,
        OrdinaryInitialization::ResumeInterest
            | OrdinaryInitialization::ResumeReload
            | OrdinaryInitialization::ResumeOptions
    );
    let interest = matches!(
        phase,
        OrdinaryInitialization::CreateInterest
            | OrdinaryInitialization::CloudCreateInterest
            | OrdinaryInitialization::ResumeInterest
    );
    let options = matches!(
        phase,
        OrdinaryInitialization::CreateOptions | OrdinaryInitialization::ResumeOptions
    );
    let mut ordinary = Box::pin(async {
        if resume {
            let mut config = ResumeSessionConfig::new(SessionId::from("recovery-session"));
            if interest {
                config = config.with_mcp_auth_handler(Arc::new(CancelMcpAuthHandler));
            }
            if options {
                config = config.with_coauthor_enabled(false);
            }
            client.resume_session(config).await
        } else {
            let mut config = if matches!(phase, OrdinaryInitialization::CloudCreateInterest) {
                SessionConfig::default().with_cloud(CloudSessionOptions::with_repository(
                    CloudSessionRepository::new("github", "copilot-sdk"),
                ))
            } else {
                SessionConfig::default().with_session_id("recovery-session")
            };
            if interest {
                config = config.with_mcp_auth_handler(Arc::new(CancelMcpAuthHandler));
            }
            if options {
                config = config.with_coauthor_enabled(false);
            }
            client.create_session(config).await
        }
    });
    assert!(futures_util::poll!(&mut ordinary).is_pending());
    let mut pending = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(
        pending["method"],
        if resume {
            "session.resume"
        } else {
            "session.create"
        }
    );
    if !matches!(phase, OrdinaryInitialization::CreateRpc) {
        server
            .respond(
                &pending,
                serde_json::json!({"sessionId": server.session_id}),
            )
            .await;
        pending = timeout(TIMEOUT, async {
            tokio::select! {
                request = server.read_request() => request,
                result = &mut ordinary => panic!("{phase:?} settled before setup: {:?}", result.map(|_| ())),
            }
        })
        .await
        .unwrap();
        if options && resume {
            assert_eq!(pending["method"], "session.skills.reload");
            server.respond(&pending, serde_json::json!({})).await;
            pending = timeout(TIMEOUT, async {
                tokio::select! {
                    request = server.read_request() => request,
                    result = &mut ordinary => panic!("{phase:?} settled before options: {:?}", result.map(|_| ())),
                }
            })
            .await
            .unwrap();
        }
        assert_eq!(
            pending["method"],
            if interest {
                "session.eventLog.registerInterest"
            } else if options {
                "session.options.update"
            } else {
                "session.skills.reload"
            }
        );
    }

    let mut recovery = Box::pin(
        client.resume_session(
            ResumeSessionConfig::new(SessionId::from("recovery-session"))
                .with_auto_mode_switch_handler(Arc::new(LegacyHandler))
                .with_auto_mode_recovery(true),
        ),
    );
    assert!(futures_util::poll!(&mut recovery).is_pending());
    initialization_wire_fence(&client, &mut server).await;

    let resident = if cancel {
        drop(ordinary);
        None
    } else if matches!(
        phase,
        OrdinaryInitialization::ResumeReload
            | OrdinaryInitialization::CreateOptions
            | OrdinaryInitialization::ResumeOptions
    ) {
        server
            .respond(&pending, serde_json::json!({"success": true}))
            .await;
        Some(
            timeout(TIMEOUT, ordinary)
                .await
                .unwrap_or_else(|error| panic!("{phase:?} did not complete: {error}"))
                .unwrap(),
        )
    } else {
        server
            .respond_error(&pending, -32003, "ordinary initialization rejected")
            .await;
        let error = timeout(TIMEOUT, ordinary).await.unwrap().err().unwrap();
        assert_eq!(*error.kind(), ErrorKind::Rpc { code: -32003 });
        None
    };
    let request = timeout(TIMEOUT, async {
        tokio::select! {
            request = server.read_request() => request,
            result = &mut recovery => panic!("recovery settled before resume: {:?}", result.map(|_| ())),
        }
    })
    .await
    .unwrap();
    assert_eq!(request["method"], "session.resume");
    server
        .respond(
            &request,
            serde_json::json!({"sessionId": server.session_id}),
        )
        .await;
    let error = timeout(TIMEOUT, async {
        if resident.is_none() {
            let detach = tokio::select! {
                request = server.read_request() => request,
                result = &mut recovery => panic!("nonresident recovery skipped detach: {:?}", result.map(|_| ())),
            };
            assert_eq!(detach["method"], "session.detach");
            server
                .respond(&detach, serde_json::json!({"success": true}))
                .await;
        }
        recovery.await
    })
    .await
    .unwrap()
    .err()
    .expect("unsupported recovery enrollment must fail");
    assert_eq!(
        *error.kind(),
        ErrorKind::Session(SessionErrorKind::AutoModeRecoveryNotSupported)
    );
    assert_eq!(
        client.registered_session_count_for_test(),
        usize::from(resident.is_some()),
        "phase: {phase:?}, cancel: {cancel}"
    );
    drop(resident);
    client.force_stop();
}

#[tokio::test]
async fn recovery_waits_for_ordinary_create_rpc_failure_or_cancellation() {
    for cancel in [false, true] {
        assert_recovery_waits_for_ordinary_initialization(
            OrdinaryInitialization::CreateRpc,
            cancel,
        )
        .await;
    }
}

#[tokio::test]
async fn recovery_waits_for_ordinary_resume_post_rpc_setup() {
    for phase in [
        OrdinaryInitialization::ResumeInterest,
        OrdinaryInitialization::ResumeReload,
        OrdinaryInitialization::ResumeOptions,
    ] {
        for cancel in [false, true] {
            assert_recovery_waits_for_ordinary_initialization(phase, cancel).await;
        }
    }
}

#[tokio::test]
async fn recovery_waits_for_ordinary_create_post_rpc_setup() {
    for phase in [
        OrdinaryInitialization::CreateInterest,
        OrdinaryInitialization::CloudCreateInterest,
        OrdinaryInitialization::CreateOptions,
    ] {
        for cancel in [false, true] {
            assert_recovery_waits_for_ordinary_initialization(phase, cancel).await;
        }
    }
}

#[tokio::test]
async fn cancelled_displaced_startup_is_not_restored_as_a_resident_session() {
    let (client, mut server) = auto_mode_recovery_client();
    let mut create = Box::pin(
        client.create_session(SessionConfig::default().with_session_id("recovery-session")),
    );
    assert!(futures_util::poll!(&mut create).is_pending());
    let request = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(request["method"], "session.create");
    let mut resume = Box::pin(
        client.resume_session(ResumeSessionConfig::new(SessionId::from(
            "recovery-session",
        ))),
    );
    assert!(futures_util::poll!(&mut resume).is_pending());
    let request = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(request["method"], "session.resume");

    // Drop both without yielding: their event-loop channels can still be open,
    // but neither cancelled registration represents a resident session.
    drop(create);
    drop(resume);
    assert_eq!(client.registered_session_count_for_test(), 0);

    let recovery = start_unsupported_attachment(&client, &mut server, true).await;
    let detach = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(detach["method"], "session.detach");
    server
        .respond(&detach, serde_json::json!({"success": true}))
        .await;
    let error = timeout(TIMEOUT, recovery)
        .await
        .unwrap()
        .unwrap()
        .err()
        .expect("nonresident recovery must reject enrollment");
    assert_eq!(
        *error.kind(),
        ErrorKind::Session(SessionErrorKind::AutoModeRecoveryNotSupported)
    );
    assert_eq!(client.registered_session_count_for_test(), 0);
    client.force_stop();
}

#[tokio::test]
async fn pending_recovery_cleanup_blocks_same_id_initialization() {
    for initial_resume in [false, true] {
        for retry_resume in [false, true] {
            for cancel_caller in [false, true] {
                let (client, mut server) = auto_mode_recovery_client();
                let start =
                    start_unsupported_attachment(&client, &mut server, initial_resume).await;
                let detach = timeout(TIMEOUT, server.read_request()).await.unwrap();
                assert_eq!(detach["method"], "session.detach");
                let start = if cancel_caller {
                    start.abort();
                    assert!(
                        start
                            .await
                            .err()
                            .expect("caller must be cancelled")
                            .is_cancelled()
                    );
                    None
                } else {
                    Some(start)
                };
                let mut retry = Box::pin(async {
                    if retry_resume {
                        client
                            .resume_session(ResumeSessionConfig::new(SessionId::from(
                                "recovery-session",
                            )))
                            .await
                    } else {
                        client
                            .create_session(
                                SessionConfig::default().with_session_id("recovery-session"),
                            )
                            .await
                    }
                });
                assert!(futures_util::poll!(&mut retry).is_pending());
                initialization_wire_fence(&client, &mut server).await;
                server
                    .respond(&detach, serde_json::json!({"success": true}))
                    .await;
                if let Some(start) = start {
                    let error = start.await.unwrap().err().expect("enrollment must fail");
                    assert_eq!(
                        *error.kind(),
                        ErrorKind::Session(SessionErrorKind::AutoModeRecoveryNotSupported)
                    );
                }
                let (session, ()) = tokio::join!(retry, async {
                    let request = timeout(TIMEOUT, server.read_request()).await.unwrap();
                    assert_eq!(
                        request["method"],
                        if retry_resume {
                            "session.resume"
                        } else {
                            "session.create"
                        }
                    );
                    server
                        .respond(
                            &request,
                            serde_json::json!({"sessionId": server.session_id}),
                        )
                        .await;
                    if retry_resume {
                        let reload = timeout(TIMEOUT, server.read_request()).await.unwrap();
                        assert_eq!(reload["method"], "session.skills.reload");
                        server.respond(&reload, serde_json::json!({})).await;
                    }
                });
                assert_eq!(client.registered_session_count_for_test(), 1);
                disconnect(session.unwrap(), server).await;
            }
        }
    }
}

#[tokio::test(start_paused = true)]
async fn failed_recovery_cleanup_fences_same_id_after_failure_timeout_or_cancellation() {
    for initial_resume in [false, true] {
        for cancel_caller in [false, true] {
            for detach_times_out in [false, true] {
                let (client, mut server) = auto_mode_recovery_client();
                let start =
                    start_unsupported_attachment(&client, &mut server, initial_resume).await;
                let detach = timeout(TIMEOUT, server.read_request()).await.unwrap();
                assert_eq!(detach["method"], "session.detach");
                let start = if cancel_caller {
                    start.abort();
                    assert!(
                        start
                            .await
                            .err()
                            .expect("caller must be cancelled")
                            .is_cancelled()
                    );
                    None
                } else {
                    Some(start)
                };
                let mut waiting_resume = Box::pin(client.resume_session(ResumeSessionConfig::new(
                    SessionId::from("recovery-session"),
                )));
                assert!(futures_util::poll!(&mut waiting_resume).is_pending());
                initialization_wire_fence(&client, &mut server).await;
                if detach_times_out {
                    // Reading detach proves admission to the existing ten-second cleanup.
                    tokio::time::advance(Duration::from_secs(11)).await;
                } else {
                    server
                        .respond_error(&detach, -32000, "detach rejected")
                        .await;
                }
                if let Some(start) = start {
                    let error = start.await.unwrap().err().expect("enrollment must fail");
                    assert_eq!(
                        *error.kind(),
                        ErrorKind::Session(SessionErrorKind::AutoModeRecoveryNotSupported)
                    );
                    assert!(
                        std::error::Error::source(&error).is_some(),
                        "cleanup failure must remain available: {error:?}"
                    );
                }
                let error = timeout(TIMEOUT, waiting_resume)
                    .await
                    .expect("waiting resume must settle after cleanup")
                    .err()
                    .expect("ambiguous cleanup must block reuse");
                assert_eq!(
                    *error.kind(),
                    ErrorKind::Session(SessionErrorKind::AutoModeRecoveryNotSupported)
                );
                for retry_resume in [false, true] {
                    let result = if retry_resume {
                        client
                            .resume_session(ResumeSessionConfig::new(SessionId::from(
                                "recovery-session",
                            )))
                            .await
                    } else {
                        client
                            .create_session(
                                SessionConfig::default().with_session_id("recovery-session"),
                            )
                            .await
                    };
                    assert_eq!(
                        *result
                            .err()
                            .expect("failed cleanup must fence later retries")
                            .kind(),
                        ErrorKind::Session(SessionErrorKind::AutoModeRecoveryNotSupported)
                    );
                }
                assert_eq!(client.registered_session_count_for_test(), 0);
                initialization_wire_fence(&client, &mut server).await;
                client.force_stop();
            }
        }
    }
}

#[tokio::test]
async fn server_assigned_create_cannot_replace_pending_or_failed_cleanup() {
    let cloud_config = || {
        SessionConfig::default().with_cloud(CloudSessionOptions::with_repository(
            CloudSessionRepository::new("github", "copilot-sdk"),
        ))
    };
    let (client, mut server) = auto_mode_recovery_client();
    let mut start = Box::pin(
        client.create_session(
            cloud_config()
                .with_auto_mode_switch_handler(Arc::new(LegacyHandler))
                .with_auto_mode_recovery(true),
        ),
    );
    assert!(futures_util::poll!(&mut start).is_pending());
    let request = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(request["method"], "session.create");
    assert!(request["params"].get("sessionId").is_none());
    server
        .respond(
            &request,
            serde_json::json!({"sessionId": server.session_id}),
        )
        .await;
    let detach = timeout(TIMEOUT, async {
        tokio::select! {
            request = server.read_request() => request,
            _ = &mut start => panic!("enrollment completed before detach settled"),
        }
    })
    .await
    .unwrap();
    assert_eq!(detach["method"], "session.detach");
    assert_eq!(detach["params"]["sessionId"], server.session_id);
    for cleanup_pending in [true, false] {
        let mut retry = Box::pin(client.create_session(cloud_config()));
        assert!(futures_util::poll!(&mut retry).is_pending());
        let request = timeout(TIMEOUT, server.read_request()).await.unwrap();
        assert_eq!(request["method"], "session.create");
        server
            .respond(
                &request,
                serde_json::json!({"sessionId": server.session_id}),
            )
            .await;
        let error = timeout(TIMEOUT, retry)
            .await
            .unwrap()
            .err()
            .expect("server-assigned registration must respect the cleanup fence");
        assert_eq!(*error.kind(), ErrorKind::Rpc { code: -32603 });
        assert!(
            error.to_string().contains(if cleanup_pending {
                "Session initialization or cleanup is still in progress"
            } else {
                "Session cleanup after rejected Auto recovery enrollment did not complete"
            }),
            "{error:?}"
        );
        assert_eq!(
            client.registered_session_count_for_test(),
            usize::from(cleanup_pending)
        );
        if cleanup_pending {
            server
                .respond_error(&detach, -32000, "detach rejected")
                .await;
            let delete = timeout(TIMEOUT, server.read_request())
                .await
                .expect("delete must still be attempted after detach failure");
            assert_eq!(delete["method"], "session.delete");
            server
                .respond(&delete, serde_json::json!({"success": true}))
                .await;
            let error = timeout(TIMEOUT, &mut start)
                .await
                .unwrap()
                .err()
                .expect("enrollment must fail");
            assert_eq!(
                *error.kind(),
                ErrorKind::Session(SessionErrorKind::AutoModeRecoveryNotSupported)
            );
        }
    }
    initialization_wire_fence(&client, &mut server).await;
    client.force_stop();
}

#[tokio::test]
async fn cancelled_cloud_post_create_setup_owns_fenced_rollback() {
    for cancel_interest in [false, true] {
        for cleanup_failure in [false, true] {
            let (client, mut server) = auto_mode_recovery_client();
            let start = tokio::spawn({
                let client = client.clone();
                async move {
                    client
                        .create_session(
                            SessionConfig::default()
                                .with_cloud(CloudSessionOptions::with_repository(
                                    CloudSessionRepository::new("github", "copilot-sdk"),
                                ))
                                .with_auto_mode_switch_handler(Arc::new(LegacyHandler))
                                .with_auto_mode_recovery(true)
                                .with_mcp_auth_handler(Arc::new(CancelMcpAuthHandler))
                                .with_skip_custom_instructions(true),
                        )
                        .await
                }
            });
            let create = timeout(TIMEOUT, server.read_request()).await.unwrap();
            server
                .respond(
                    &create,
                    json!({
                        "sessionId": server.session_id,
                        "capabilities": {"ui": {"autoModeRecovery": true}}
                    }),
                )
                .await;
            let interest = timeout(TIMEOUT, server.read_request()).await.unwrap();
            assert_eq!(interest["method"], "session.eventLog.registerInterest");
            let pending = if cancel_interest {
                interest
            } else {
                server.respond(&interest, json!({})).await;
                let patch = timeout(TIMEOUT, server.read_request()).await.unwrap();
                assert_eq!(patch["method"], "session.options.update");
                patch
            };
            start.abort();
            assert!(
                start
                    .await
                    .err()
                    .expect("creation must be cancelled")
                    .is_cancelled()
            );
            let detach = timeout(TIMEOUT, server.read_request())
                .await
                .expect("cancelled creation must detach its cloud session");
            assert_eq!(detach["method"], "session.detach");
            assert_eq!(client.registered_session_count_for_test(), 1);
            let mut retry = tokio::spawn({
                let client = client.clone();
                let session_id = server.session_id.clone();
                async move {
                    client
                        .resume_session(ResumeSessionConfig::new(SessionId::from(session_id)))
                        .await
                }
            });
            assert!(futures_util::poll!(&mut retry).is_pending());
            initialization_wire_fence(&client, &mut server).await;
            server.respond(&pending, json!({})).await;
            server.respond(&detach, json!({"success": true})).await;
            let delete = timeout(TIMEOUT, server.read_request()).await.unwrap();
            assert_eq!(delete["method"], "session.delete");
            assert_eq!(delete["params"]["sessionId"], server.session_id);
            assert!(futures_util::poll!(&mut retry).is_pending());
            initialization_wire_fence(&client, &mut server).await;
            server
                .respond(
                    &delete,
                    json!({
                        "success": !cleanup_failure, "error": "cloud rollback rejected"
                    }),
                )
                .await;
            if cleanup_failure {
                assert!(timeout(TIMEOUT, retry).await.unwrap().unwrap().is_err());
                assert_eq!(client.registered_session_count_for_test(), 0);
            } else {
                assert!(futures_util::poll!(&mut retry).is_pending());
                let resume = timeout(TIMEOUT, async {
                    tokio::select! {
                        request = server.read_request() => request,
                        result = &mut retry => panic!("resume finished without an RPC: {:?}", result.err()),
                    }
                }).await.unwrap();
                assert_eq!(resume["method"], "session.resume");
                server
                    .respond(&resume, json!({"sessionId": server.session_id}))
                    .await;
                let reload = timeout(TIMEOUT, server.read_request()).await.unwrap();
                assert_eq!(reload["method"], "session.skills.reload");
                server.respond(&reload, json!({})).await;
                let session = timeout(TIMEOUT, retry).await.unwrap().unwrap().unwrap();
                drop(session);
                assert_eq!(client.registered_session_count_for_test(), 0);
            }
            client.force_stop();
        }
    }
}

#[tokio::test]
async fn cloud_post_create_failure_is_deleted_before_returning_error() {
    for (fail_interest, cleanup_failure) in
        [(false, false), (true, false), (false, true), (true, true)]
    {
        let (client, mut server) = auto_mode_recovery_client();
        let mut start = tokio::spawn({
            let client = client.clone();
            async move {
                client
                    .create_session(
                        SessionConfig::default()
                            .with_cloud(CloudSessionOptions::with_repository(
                                CloudSessionRepository::new("github", "copilot-sdk"),
                            ))
                            .with_auto_mode_switch_handler(Arc::new(LegacyHandler))
                            .with_auto_mode_recovery(true)
                            .with_mcp_auth_handler(Arc::new(CancelMcpAuthHandler))
                            .with_skip_custom_instructions(true),
                    )
                    .await
            }
        });
        let create = timeout(TIMEOUT, server.read_request()).await.unwrap();
        server
            .respond(
                &create,
                json!({
                    "sessionId": server.session_id,
                    "capabilities": {"ui": {"autoModeRecovery": true}}
                }),
            )
            .await;
        let interest = timeout(TIMEOUT, server.read_request()).await.unwrap();
        assert_eq!(interest["method"], "session.eventLog.registerInterest");
        if fail_interest {
            server
                .respond_error(&interest, -32603, "post-create setup failed")
                .await;
        } else {
            server.respond(&interest, json!({})).await;
            let patch = timeout(TIMEOUT, server.read_request()).await.unwrap();
            assert_eq!(patch["method"], "session.options.update");
            server
                .respond_error(&patch, -32603, "post-create setup failed")
                .await;
        }
        let detach = tokio::select! {
            request = server.read_request() => request,
            result = &mut start => panic!("creation returned without cloud rollback: {:?}", result.unwrap().err()),
        };
        assert_eq!(detach["method"], "session.detach");
        server.respond(&detach, json!({"success": true})).await;
        let delete = timeout(TIMEOUT, server.read_request()).await.unwrap();
        assert_eq!(delete["method"], "session.delete");
        assert_eq!(delete["params"]["sessionId"], server.session_id);
        server
            .respond(
                &delete,
                json!({
                    "success": !cleanup_failure, "error": "cloud rollback rejected"
                }),
            )
            .await;
        let error = timeout(TIMEOUT, start)
            .await
            .unwrap()
            .unwrap()
            .err()
            .unwrap();
        assert_eq!(*error.kind(), ErrorKind::Rpc { code: -32603 });
        assert!(error.to_string().contains("post-create setup failed"));
        assert_eq!(client.registered_session_count_for_test(), 0);
        if cleanup_failure {
            assert!(error.to_string().contains("cloud rollback rejected"));
            let retry = timeout(
                TIMEOUT,
                client.resume_session(ResumeSessionConfig::new(SessionId::from(server.session_id))),
            )
            .await
            .unwrap();
            assert!(
                retry.is_err(),
                "failed cloud rollback must block same-ID reuse"
            );
        }
    }
}

#[tokio::test]
async fn rejected_server_assigned_cloud_create_is_deleted_after_detach() {
    let (client, mut server) = auto_mode_recovery_client();
    let start = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session(
                    SessionConfig::default()
                        .with_cloud(CloudSessionOptions::with_repository(
                            CloudSessionRepository::new("github", "copilot-sdk"),
                        ))
                        .with_auto_mode_switch_handler(Arc::new(LegacyHandler))
                        .with_auto_mode_recovery(true),
                )
                .await
        }
    });

    let create = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(create["method"], "session.create");
    assert!(create["params"].get("sessionId").is_none());
    server
        .respond(&create, serde_json::json!({"sessionId": server.session_id}))
        .await;

    let detach = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(detach["method"], "session.detach");
    assert_eq!(detach["params"]["sessionId"], server.session_id);
    server
        .respond(&detach, serde_json::json!({"success": true}))
        .await;

    let delete = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(delete["method"], "session.delete");
    assert_eq!(delete["params"]["sessionId"], server.session_id);
    server
        .respond(&delete, serde_json::json!({"success": true}))
        .await;

    let error = timeout(TIMEOUT, start)
        .await
        .unwrap()
        .unwrap()
        .err()
        .expect("unsupported enrollment must fail");
    assert_eq!(
        *error.kind(),
        ErrorKind::Session(SessionErrorKind::AutoModeRecoveryNotSupported)
    );
    assert_eq!(client.registered_session_count_for_test(), 0);
}

#[tokio::test]
async fn server_assigned_cloud_cleanup_deletes_even_when_detach_fails_and_keeps_both_errors() {
    let (client, mut server) = auto_mode_recovery_client();
    let start = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session(
                    SessionConfig::default()
                        .with_cloud(CloudSessionOptions::with_repository(
                            CloudSessionRepository::new("github", "copilot-sdk"),
                        ))
                        .with_auto_mode_switch_handler(Arc::new(LegacyHandler))
                        .with_auto_mode_recovery(true),
                )
                .await
        }
    });

    let create = timeout(TIMEOUT, server.read_request()).await.unwrap();
    server
        .respond(&create, serde_json::json!({"sessionId": server.session_id}))
        .await;
    let detach = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(detach["method"], "session.detach");
    server
        .respond_error(&detach, -32000, "detach rejected")
        .await;

    let delete = timeout(TIMEOUT, server.read_request())
        .await
        .expect("delete must still be attempted after detach failure");
    assert_eq!(delete["method"], "session.delete");
    server
        .respond(
            &delete,
            serde_json::json!({"success": false, "error": "delete rejected"}),
        )
        .await;

    let error = timeout(TIMEOUT, start)
        .await
        .unwrap()
        .unwrap()
        .err()
        .expect("unsupported enrollment cleanup must fail");
    assert_eq!(
        *error.kind(),
        ErrorKind::Session(SessionErrorKind::AutoModeRecoveryNotSupported)
    );
    let cleanup_errors = std::error::Error::source(&error)
        .expect("both cleanup failures must remain available")
        .to_string();
    assert!(
        cleanup_errors.contains("detach rejected"),
        "{cleanup_errors}"
    );
    assert!(
        cleanup_errors.contains("delete rejected"),
        "{cleanup_errors}"
    );
    assert_eq!(client.registered_session_count_for_test(), 0);

    let mut retry = Box::pin(client.create_session(SessionConfig::default().with_cloud(
        CloudSessionOptions::with_repository(CloudSessionRepository::new("github", "copilot-sdk")),
    )));
    assert!(futures_util::poll!(&mut retry).is_pending());
    let retry_request = timeout(TIMEOUT, server.read_request()).await.unwrap();
    server
        .respond(
            &retry_request,
            serde_json::json!({"sessionId": server.session_id}),
        )
        .await;
    let error = timeout(TIMEOUT, retry)
        .await
        .unwrap()
        .err()
        .expect("failed cleanup must keep the same ID fenced");
    assert_eq!(*error.kind(), ErrorKind::Rpc { code: -32603 });
    assert!(
        error
            .to_string()
            .contains("Session cleanup after rejected Auto recovery enrollment did not complete")
    );
    client.force_stop();
}

#[tokio::test(start_paused = true)]
async fn server_assigned_cloud_cleanup_attempts_delete_after_detach_timeout() {
    let (client, mut server) = auto_mode_recovery_client();
    let start = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session(
                    SessionConfig::default()
                        .with_cloud(CloudSessionOptions::with_repository(
                            CloudSessionRepository::new("github", "copilot-sdk"),
                        ))
                        .with_auto_mode_switch_handler(Arc::new(LegacyHandler))
                        .with_auto_mode_recovery(true),
                )
                .await
        }
    });

    let create = timeout(TIMEOUT, server.read_request()).await.unwrap();
    server
        .respond(&create, serde_json::json!({"sessionId": server.session_id}))
        .await;
    let detach = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(detach["method"], "session.detach");
    tokio::time::advance(Duration::from_secs(11)).await;

    let delete = timeout(TIMEOUT, server.read_request())
        .await
        .expect("delete must still be attempted after detach timeout");
    assert_eq!(delete["method"], "session.delete");
    server
        .respond(&delete, serde_json::json!({"success": true}))
        .await;

    let error = timeout(TIMEOUT, start)
        .await
        .unwrap()
        .unwrap()
        .err()
        .expect("timed-out detach must keep enrollment rejected");
    assert_eq!(
        *error.kind(),
        ErrorKind::Session(SessionErrorKind::AutoModeRecoveryNotSupported)
    );
    assert!(
        std::error::Error::source(&error)
            .expect("detach timeout must be preserved")
            .to_string()
            .contains("session.detach timed out")
    );
    assert_eq!(client.registered_session_count_for_test(), 0);

    let mut retry = Box::pin(client.create_session(SessionConfig::default().with_cloud(
        CloudSessionOptions::with_repository(CloudSessionRepository::new("github", "copilot-sdk")),
    )));
    assert!(futures_util::poll!(&mut retry).is_pending());
    let retry_request = timeout(TIMEOUT, server.read_request()).await.unwrap();
    server
        .respond(
            &retry_request,
            serde_json::json!({"sessionId": server.session_id}),
        )
        .await;
    let error = timeout(TIMEOUT, retry)
        .await
        .unwrap()
        .err()
        .expect("ambiguous detach must keep the same ID fenced");
    assert_eq!(*error.kind(), ErrorKind::Rpc { code: -32603 });
    assert!(
        error
            .to_string()
            .contains("Session cleanup after rejected Auto recovery enrollment did not complete")
    );
    client.force_stop();
}

#[tokio::test]
async fn public_delete_is_idempotent_when_runtime_reports_unknown_session() {
    let (client, mut server) = auto_mode_recovery_client();
    let delete = tokio::spawn({
        let client = client.clone();
        let session_id = SessionId::from(server.session_id.clone());
        async move { client.delete_session(&session_id).await }
    });

    let request = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(request["method"], "session.delete");
    server
        .respond(
            &request,
            serde_json::json!({"success": false, "error": "session not found"}),
        )
        .await;

    timeout(TIMEOUT, delete)
        .await
        .unwrap()
        .unwrap()
        .expect("public delete preserves its idempotent behavior");
    client.force_stop();
}

type ReentrantCreateOutcomeSender = oneshot::Sender<(Result<(), String>, Result<(), String>)>;

struct ReentrantSessionFsProvider {
    client: Client,
    spawn: bool,
    outcomes: tokio::sync::Mutex<Option<ReentrantCreateOutcomeSender>>,
}

#[async_trait]
impl github_copilot_sdk::session_fs::SessionFsProvider for ReentrantSessionFsProvider {
    async fn write_file(
        &self,
        _path: &str,
        _content: &str,
        _mode: Option<i64>,
    ) -> Result<(), github_copilot_sdk::session_fs::FsError> {
        let client = self.client.clone();
        let create = async move {
            client
                .create_session(SessionConfig::default().with_session_id("recovery-session"))
                .await
                .map(|_| ())
                .map_err(|error| error.to_string())
        };
        let same_id = if self.spawn {
            tokio::spawn(create)
                .await
                .expect("child create must not panic")
        } else {
            create.await
        };
        let other_id = self
            .client
            .create_session(SessionConfig::default().with_session_id("independent-session"))
            .await
            .map(|_| ())
            .map_err(|error| error.to_string());
        if let Some(outcomes) = self.outcomes.lock().await.take() {
            let _ = outcomes.send((same_id, other_id));
        }
        Ok(())
    }
}

#[tokio::test]
async fn session_fs_callback_rejects_same_id_reentry_but_allows_other_ids() {
    assert_session_fs_callback_reentry(false).await;
}

#[tokio::test]
async fn spawned_session_fs_callback_rejects_same_id_reentry_but_allows_other_ids() {
    assert_session_fs_callback_reentry(true).await;
}

async fn assert_session_fs_callback_reentry(spawn: bool) {
    let (client, mut server) = auto_mode_recovery_client();
    let (outcomes_tx, outcomes_rx) = oneshot::channel();
    let provider = Arc::new(ReentrantSessionFsProvider {
        client: client.clone(),
        spawn,
        outcomes: tokio::sync::Mutex::new(Some(outcomes_tx)),
    });
    let start = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session(
                    SessionConfig::default()
                        .with_session_id("recovery-session")
                        .with_auto_mode_switch_handler(Arc::new(LegacyHandler))
                        .with_auto_mode_recovery(true)
                        .with_session_fs_provider(provider),
                )
                .await
        }
    });

    let create = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(create["method"], "session.create");
    server
        .send_request(
            70,
            "sessionFs.writeFile",
            serde_json::json!({
                "sessionId": "recovery-session",
                "path": "/workspace/.copilot",
                "content": "metadata"
            }),
        )
        .await;

    let independent_create = timeout(TIMEOUT, server.read_request())
        .await
        .expect("same-ID SessionFS re-entry must not block an independent create");
    assert_eq!(independent_create["method"], "session.create");
    assert_eq!(
        independent_create["params"]["sessionId"],
        "independent-session"
    );
    server
        .respond(
            &independent_create,
            serde_json::json!({"sessionId": "independent-session"}),
        )
        .await;

    let response = timeout(TIMEOUT, server.read_response())
        .await
        .expect("SessionFS callback must complete after rejecting same-ID re-entry");
    assert_eq!(response["id"], 70);
    let (same_id, other_id) = timeout(TIMEOUT, outcomes_rx).await.unwrap().unwrap();
    let same_id_error = same_id.expect_err("same-ID callback re-entry must be rejected");
    assert!(
        same_id_error.contains("active session callback"),
        "unexpected same-ID error: {same_id_error}"
    );
    assert!(other_id.is_ok(), "different-ID create failed: {other_id:?}");

    server
        .respond(
            &create,
            serde_json::json!({
                "sessionId": "recovery-session",
                "capabilities": {"ui": {"autoModeRecovery": true}}
            }),
        )
        .await;
    assert!(timeout(TIMEOUT, start).await.unwrap().unwrap().is_ok());
    client.force_stop();
}

#[tokio::test]
async fn recovery_resume_waits_for_an_ordinary_resume_rpc_before_replacing_registration() {
    let (client, mut server) = auto_mode_recovery_client();
    let ordinary_resume = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .resume_session(ResumeSessionConfig::new(SessionId::from(
                    "recovery-session",
                )))
                .await
        }
    });

    let ordinary_request = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(ordinary_request["method"], "session.resume");

    let (recovery_polled_tx, recovery_polled_rx) = oneshot::channel();
    let recovery_resume = tokio::spawn({
        let client = client.clone();
        async move {
            let recovery = client.resume_session(
                ResumeSessionConfig::new(SessionId::from("recovery-session"))
                    .with_auto_mode_switch_handler(Arc::new(LegacyHandler))
                    .with_auto_mode_recovery(true),
            );
            tokio::pin!(recovery);
            assert!(
                futures_util::poll!(&mut recovery).is_pending(),
                "recovery resume must wait for its RPC or the initialization lock"
            );
            let _ = recovery_polled_tx.send(());
            recovery.await
        }
    });
    timeout(TIMEOUT, recovery_polled_rx)
        .await
        .expect("recovery resume must reach its first pending point")
        .unwrap();
    let early_recovery_request = timeout(Duration::from_millis(50), server.read_request())
        .await
        .ok();
    let recovery_was_sent_early = early_recovery_request.is_some();

    server
        .respond_error(&ordinary_request, -32000, "ordinary resume failed")
        .await;
    let ordinary_error = timeout(TIMEOUT, ordinary_resume)
        .await
        .unwrap()
        .unwrap()
        .err()
        .expect("ordinary resume must retain its failed RPC result");
    assert!(
        ordinary_error
            .to_string()
            .contains("ordinary resume failed")
    );

    let recovery_request = match early_recovery_request {
        Some(request) => request,
        None => timeout(TIMEOUT, server.read_request())
            .await
            .expect("recovery resume must proceed after the ordinary RPC completes"),
    };
    assert_eq!(recovery_request["method"], "session.resume");
    server
        .respond(
            &recovery_request,
            serde_json::json!({"sessionId": server.session_id}),
        )
        .await;

    let detach = timeout(TIMEOUT, server.read_request())
        .await
        .expect("unacknowledged recovery must detach its own registration");
    assert_eq!(detach["method"], "session.detach");
    assert_eq!(detach["params"]["sessionId"], server.session_id);
    server
        .respond(&detach, serde_json::json!({"success": true}))
        .await;

    let recovery_error = timeout(TIMEOUT, recovery_resume)
        .await
        .unwrap()
        .unwrap()
        .err()
        .expect("unsupported recovery enrollment must fail");
    assert_eq!(
        *recovery_error.kind(),
        ErrorKind::Session(SessionErrorKind::AutoModeRecoveryNotSupported)
    );
    assert!(
        !recovery_was_sent_early,
        "recovery resume must not send its RPC while an ordinary resume is pending"
    );
    assert_eq!(client.registered_session_count_for_test(), 0);
    client.force_stop();
}

#[tokio::test]
async fn session_end_hook_reentry_during_rejected_recovery_does_not_wait_on_cleanup() {
    assert_session_end_hook_reentry(false).await;
}

#[tokio::test]
async fn spawned_session_end_hook_reentry_does_not_wait_on_cleanup() {
    assert_session_end_hook_reentry(true).await;
}

async fn assert_session_end_hook_reentry(spawn: bool) {
    use github_copilot_sdk::hooks::{HookContext, SessionEndInput, SessionEndOutput, SessionHooks};

    struct ReenteringHook {
        client: Client,
        spawn: bool,
        outcome: tokio::sync::Mutex<Option<oneshot::Sender<Result<(), String>>>>,
    }

    #[async_trait]
    impl SessionHooks for ReenteringHook {
        async fn on_session_end(
            &self,
            input: SessionEndInput,
            _ctx: HookContext,
        ) -> Option<SessionEndOutput> {
            let client = self.client.clone();
            let resume = async move {
                client
                    .resume_session(ResumeSessionConfig::new(SessionId::from(input.session_id)))
                    .await
                    .map(|_| ())
                    .map_err(|error| error.to_string())
            };
            let result = if self.spawn {
                tokio::spawn(resume)
                    .await
                    .expect("child resume must not panic")
            } else {
                resume.await
            };
            if let Some(outcome) = self.outcome.lock().await.take() {
                let _ = outcome.send(result);
            }
            None
        }
    }

    let (client, mut server) = auto_mode_recovery_client();
    let (outcome_tx, outcome_rx) = oneshot::channel();
    let hooks = Arc::new(ReenteringHook {
        client: client.clone(),
        spawn,
        outcome: tokio::sync::Mutex::new(Some(outcome_tx)),
    });
    let resume = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .resume_session(
                    ResumeSessionConfig::new(SessionId::from("recovery-session"))
                        .with_auto_mode_switch_handler(Arc::new(LegacyHandler))
                        .with_auto_mode_recovery(true)
                        .with_hooks(hooks),
                )
                .await
        }
    });

    let request = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(request["method"], "session.resume");
    server
        .respond(
            &request,
            serde_json::json!({"sessionId": server.session_id}),
        )
        .await;
    let detach = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(detach["method"], "session.detach");

    server
        .send_request(
            71,
            "hooks.invoke",
            serde_json::json!({
                "sessionId": server.session_id,
                "hookType": "sessionEnd",
                "input": {
                    "sessionId": server.session_id,
                    "timestamp": 1234567890,
                    "cwd": "/workspace",
                    "reason": "error"
                }
            }),
        )
        .await;
    let hook_response = match timeout(TIMEOUT, server.read_response()).await {
        Ok(response) => response,
        Err(timeout_error) => {
            server
                .respond(&detach, serde_json::json!({"success": true}))
                .await;
            client.force_stop();
            panic!("sessionEnd re-entry waited on cleanup: {timeout_error}");
        }
    };
    server
        .respond(&detach, serde_json::json!({"success": true}))
        .await;
    assert_eq!(hook_response["id"], 71);
    let nested_result = timeout(TIMEOUT, outcome_rx).await.unwrap().unwrap();
    let nested_error = nested_result.expect_err("same-ID hook re-entry must be rejected");
    assert!(
        nested_error.contains("same ID"),
        "unexpected re-entry error: {nested_error}"
    );

    let resume_error = timeout(TIMEOUT, resume)
        .await
        .unwrap()
        .unwrap()
        .err()
        .expect("unsupported recovery enrollment must fail");
    assert_eq!(
        *resume_error.kind(),
        ErrorKind::Session(SessionErrorKind::AutoModeRecoveryNotSupported)
    );
    client.force_stop();
}

struct PendingRecoveryConsentHandler {
    client: Client,
    reenter: bool,
    entered: Arc<Notify>,
    release: Arc<Notify>,
    outcomes: mpsc::UnboundedSender<Result<(), String>>,
}

#[async_trait]
impl AutoModeSwitchHandler for PendingRecoveryConsentHandler {
    async fn handle(
        &self,
        _session_id: SessionId,
        _error_code: Option<String>,
        _retry_after_seconds: Option<f64>,
    ) -> AutoModeSwitchResponse {
        panic!("recovery consent must use its dedicated handler");
    }

    async fn handle_auto_mode_recovery(
        &self,
        session_id: SessionId,
        _error_code: Option<String>,
        _retry_after_seconds: Option<f64>,
        _context: AutoModeRecoveryContext,
    ) -> AutoModeSwitchResponse {
        self.entered.notify_one();
        self.release.notified().await;
        if self.reenter {
            let result = self
                .client
                .resume_session(ResumeSessionConfig::new(session_id))
                .await
                .map(|_| ())
                .map_err(|error| error.to_string());
            self.outcomes.send(result).unwrap();
        }
        AutoModeSwitchResponse::Yes
    }
}

#[tokio::test]
async fn pending_recovery_consent_allows_same_id_initialization_without_a_lock_wait() {
    for reenter in [false, true] {
        let (client, mut server) = auto_mode_recovery_client();
        let entered = Arc::new(Notify::new());
        let release = Arc::new(Notify::new());
        let (outcomes, mut outcome_rx) = mpsc::unbounded_channel();
        let handler = Arc::new(PendingRecoveryConsentHandler {
            client: client.clone(),
            reenter,
            entered: entered.clone(),
            release: release.clone(),
            outcomes,
        });
        let start = tokio::spawn({
            let client = client.clone();
            async move {
                client
                    .create_session(
                        SessionConfig::default()
                            .with_session_id("recovery-session")
                            .with_auto_mode_switch_handler(handler)
                            .with_auto_mode_recovery(true),
                    )
                    .await
            }
        });
        let create = timeout(TIMEOUT, server.read_request()).await.unwrap();
        server
            .respond(
                &create,
                serde_json::json!({
                    "sessionId": server.session_id,
                    "capabilities": {"ui": {"autoModeRecovery": true}}
                }),
            )
            .await;
        let _session = timeout(TIMEOUT, start).await.unwrap().unwrap().unwrap();
        server
            .send_request(
                72,
                "autoModeSwitch.request",
                serde_json::json!({
                    "sessionId": server.session_id,
                    "autoModeRecovery": {
                        "requestId": "pending-offer",
                        "originalModel": "explicit-model",
                        "allowAlways": false
                    }
                }),
            )
            .await;
        timeout(TIMEOUT, entered.notified()).await.unwrap();

        let independent = if reenter {
            release.notify_one();
            None
        } else {
            Some(tokio::spawn({
                let client = client.clone();
                async move {
                    client
                        .resume_session(
                            ResumeSessionConfig::new(SessionId::from("recovery-session"))
                                .with_auto_mode_switch_handler(Arc::new(LegacyHandler))
                                .with_auto_mode_recovery(true),
                        )
                        .await
                }
            }))
        };
        let resume = timeout(TIMEOUT, server.read_request())
            .await
            .expect("an active consent callback alone must not block same-ID resume");
        assert_eq!(
            resume["method"], "session.resume",
            "same-ID resume was rejected even though no initialization wait was needed"
        );
        assert_eq!(resume["params"]["sessionId"], server.session_id);
        server
            .respond(
                &resume,
                serde_json::json!({
                    "sessionId": server.session_id,
                    "capabilities": {"ui": {"autoModeRecovery": true}}
                }),
            )
            .await;
        let reload = timeout(TIMEOUT, server.read_request()).await.unwrap();
        assert_eq!(reload["method"], "session.skills.reload");
        server.respond(&reload, serde_json::json!({})).await;
        if let Some(independent) = independent {
            assert!(
                timeout(TIMEOUT, independent)
                    .await
                    .unwrap()
                    .unwrap()
                    .is_ok()
            );
            release.notify_one();
        } else {
            assert_eq!(
                timeout(TIMEOUT, outcome_rx.recv()).await.unwrap().unwrap(),
                Ok(())
            );
        }
        let response = timeout(TIMEOUT, server.read_response()).await.unwrap();
        assert_eq!(response["id"], 72);
        assert_eq!(response["result"]["response"], "yes");
        client.force_stop();
    }
}

#[tokio::test]
async fn direct_registration_with_params_retains_recovery_acknowledgment() {
    use github_copilot_sdk::rpc::UIRegisterDirectAutoModeSwitchHandlerRequest;

    for acknowledged in [None, Some(false), Some(true)] {
        let (session, client, mut server, _) =
            open(false, false, None, Arc::new(LegacyHandler)).await;
        let session = Arc::new(session.unwrap());
        let registration = tokio::spawn({
            let session = session.clone();
            async move {
                session
                    .rpc()
                    .ui()
                    .register_direct_auto_mode_switch_handler_with_params(
                        UIRegisterDirectAutoModeSwitchHandlerRequest {
                            auto_mode_recovery: Some(true),
                        },
                    )
                    .await
            }
        });
        let request = timeout(TIMEOUT, server.read_request()).await.unwrap();
        assert_eq!(
            request["method"],
            "session.ui.registerDirectAutoModeSwitchHandler"
        );
        assert_eq!(
            request["params"],
            serde_json::json!({"sessionId": server.session_id, "autoModeRecovery": true})
        );
        let mut response = serde_json::json!({"handle": "direct-registration"});
        if let Some(acknowledged) = acknowledged {
            response["autoModeRecovery"] = serde_json::json!(acknowledged);
        }
        server.respond(&request, response.clone()).await;
        let result: github_copilot_sdk::rpc::UIRegisterDirectAutoModeSwitchHandlerResultWithAutoModeRecovery =
            timeout(TIMEOUT, registration)
            .await
            .unwrap()
            .unwrap()
            .expect("registration RPC must succeed");
        assert_eq!(result.auto_mode_recovery, acknowledged);
        assert_eq!(
            serde_json::to_value(result).unwrap(),
            response,
            "the enrollment API must not drop the runtime acknowledgment"
        );
        client.force_stop();
    }
}

#[tokio::test]
async fn parameterless_direct_registration_keeps_its_legacy_result_and_stays_unenrolled() {
    let (session, client, mut server, _) = open(false, false, None, Arc::new(LegacyHandler)).await;
    let session = Arc::new(session.unwrap());
    let registration = tokio::spawn({
        let session = session.clone();
        async move {
            session
                .rpc()
                .ui()
                .register_direct_auto_mode_switch_handler()
                .await
        }
    });
    let request = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(
        request["method"],
        "session.ui.registerDirectAutoModeSwitchHandler"
    );
    assert_eq!(
        request["params"],
        serde_json::json!({"sessionId": server.session_id})
    );
    server
        .respond(
            &request,
            serde_json::json!({"handle": "legacy-registration"}),
        )
        .await;
    let result: github_copilot_sdk::rpc::UIRegisterDirectAutoModeSwitchHandlerResult =
        timeout(TIMEOUT, registration)
            .await
            .unwrap()
            .unwrap()
            .expect("legacy registration RPC must succeed");
    let github_copilot_sdk::rpc::UIRegisterDirectAutoModeSwitchHandlerResult { handle } = result;
    assert_eq!(handle, "legacy-registration");
    client.force_stop();
}

#[tokio::test]
async fn rejected_explicit_id_cloud_create_is_not_deleted() {
    let (client, mut server) = auto_mode_recovery_client();
    let start = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session(
                    SessionConfig::default()
                        .with_session_id("explicit-cloud-session")
                        .with_cloud(CloudSessionOptions::with_repository(
                            CloudSessionRepository::new("github", "copilot-sdk"),
                        ))
                        .with_auto_mode_switch_handler(Arc::new(LegacyHandler))
                        .with_auto_mode_recovery(true),
                )
                .await
        }
    });

    let create = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(create["method"], "session.create");
    assert_eq!(create["params"]["sessionId"], "explicit-cloud-session");
    server
        .respond(
            &create,
            serde_json::json!({"sessionId": "explicit-cloud-session"}),
        )
        .await;
    let detach = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(detach["method"], "session.detach");
    server
        .respond(&detach, serde_json::json!({"success": true}))
        .await;

    let error = timeout(TIMEOUT, start)
        .await
        .unwrap()
        .unwrap()
        .err()
        .expect("unsupported enrollment must fail");
    assert_eq!(
        *error.kind(),
        ErrorKind::Session(SessionErrorKind::AutoModeRecoveryNotSupported)
    );
    assert_eq!(client.registered_session_count_for_test(), 0);
}

#[tokio::test(start_paused = true)]
async fn failed_cloud_deletion_keeps_same_id_fenced_after_caller_cancellation() {
    let (client, mut server) = auto_mode_recovery_client();
    let start = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session(
                    SessionConfig::default()
                        .with_cloud(CloudSessionOptions::with_repository(
                            CloudSessionRepository::new("github", "copilot-sdk"),
                        ))
                        .with_auto_mode_switch_handler(Arc::new(LegacyHandler))
                        .with_auto_mode_recovery(true),
                )
                .await
        }
    });

    let create = timeout(TIMEOUT, server.read_request()).await.unwrap();
    server
        .respond(&create, serde_json::json!({"sessionId": server.session_id}))
        .await;
    let detach = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(detach["method"], "session.detach");
    server
        .respond(&detach, serde_json::json!({"success": true}))
        .await;
    let delete = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(delete["method"], "session.delete");

    start.abort();
    assert!(
        start
            .await
            .err()
            .expect("caller must be cancelled")
            .is_cancelled()
    );

    let mut retry = Box::pin(client.create_session(SessionConfig::default().with_cloud(
        CloudSessionOptions::with_repository(CloudSessionRepository::new("github", "copilot-sdk")),
    )));
    assert!(futures_util::poll!(&mut retry).is_pending());
    let retry_request = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(retry_request["method"], "session.create");
    server
        .respond(
            &retry_request,
            serde_json::json!({"sessionId": server.session_id}),
        )
        .await;
    let error = timeout(TIMEOUT, retry)
        .await
        .unwrap()
        .err()
        .expect("create must not reuse an ID while deletion is pending");
    assert_eq!(*error.kind(), ErrorKind::Rpc { code: -32603 });
    assert!(
        error
            .to_string()
            .contains("initialization or cleanup is still in progress")
    );

    tokio::time::advance(Duration::from_secs(11)).await;
    let mut retry = Box::pin(client.create_session(SessionConfig::default().with_cloud(
        CloudSessionOptions::with_repository(CloudSessionRepository::new("github", "copilot-sdk")),
    )));
    assert!(futures_util::poll!(&mut retry).is_pending());
    let retry_request = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(retry_request["method"], "session.create");
    server
        .respond(
            &retry_request,
            serde_json::json!({"sessionId": server.session_id}),
        )
        .await;
    let error = timeout(TIMEOUT, retry)
        .await
        .unwrap()
        .err()
        .expect("ambiguous deletion must keep the ID fenced");
    assert_eq!(*error.kind(), ErrorKind::Rpc { code: -32603 });
    assert!(
        error
            .to_string()
            .contains("Session cleanup after rejected Auto recovery enrollment did not complete")
    );
    assert_eq!(client.registered_session_count_for_test(), 0);
    client.force_stop();
}

struct ResumedHandler {
    _on_drop: NotifyOnDrop,
}

#[async_trait]
impl AutoModeSwitchHandler for ResumedHandler {
    async fn handle(
        &self,
        _session_id: SessionId,
        _error_code: Option<String>,
        _retry_after_seconds: Option<f64>,
    ) -> AutoModeSwitchResponse {
        AutoModeSwitchResponse::No
    }
}

async fn assert_resume_callback_owner(acknowledged: Option<bool>, rpc_rejected: bool) {
    let (resident, client, mut server, _) = open(false, false, None, Arc::new(LegacyHandler)).await;
    let resident = resident.unwrap();
    let handler_dropped = Arc::new(Notify::new());
    let mut resume = Box::pin(
        client.resume_session(
            ResumeSessionConfig::new(SessionId::from("recovery-session"))
                .with_auto_mode_switch_handler(Arc::new(ResumedHandler {
                    _on_drop: NotifyOnDrop(handler_dropped.clone()),
                }))
                .with_auto_mode_recovery(true),
        ),
    );
    let request = timeout(TIMEOUT, async {
        tokio::select! {
            request = server.read_request() => request,
            _ = &mut resume => panic!("resume completed before the runtime replied"),
        }
    })
    .await
    .expect("resume request should arrive");
    assert_eq!(request["method"], "session.resume");
    if rpc_rejected {
        server
            .respond_error(&request, -32003, "resume rejected")
            .await;
    } else {
        let mut result = serde_json::json!({"sessionId": server.session_id});
        if let Some(acknowledged) = acknowledged {
            result["capabilities"] = serde_json::json!({"ui": {"autoModeRecovery": acknowledged}});
        }
        server.respond(&request, result).await;
    }
    let (result, detached) = timeout(TIMEOUT, async {
        tokio::select! {
            result = &mut resume => (result, false),
            request = server.read_request() => {
                assert_eq!(request["method"], "session.detach");
                server.respond(&request, serde_json::json!({"success": true})).await;
                (resume.await, true)
            }
        }
    })
    .await
    .expect("unsupported enrollment should settle");
    let error = result.err().expect("resume must fail");
    if rpc_rejected {
        assert!(error.to_string().contains("resume rejected"), "{error}");
    } else {
        assert_eq!(
            *error.kind(),
            ErrorKind::Session(SessionErrorKind::AutoModeRecoveryNotSupported)
        );
    }
    assert!(
        !detached,
        "rejected resume detached the resident runtime session"
    );
    assert_eq!(client.registered_session_count_for_test(), 1);
    server
        .send_request(
            20,
            "autoModeSwitch.request",
            serde_json::json!({
                "sessionId": server.session_id, "errorCode": "user_weekly_rate_limited",
            }),
        )
        .await;
    assert_eq!(
        timeout(TIMEOUT, server.read_response()).await.unwrap()["result"]["response"],
        if rpc_rejected { "yes_always" } else { "no" }
    );
    if !rpc_rejected {
        drop(resident);
        assert_eq!(client.registered_session_count_for_test(), 1);
    }
    let (result, ()) = tokio::join!(client.stop(), async {
        let request = timeout(TIMEOUT, server.read_request()).await.unwrap();
        assert_eq!(request["method"], "session.detach");
        server
            .respond(&request, serde_json::json!({"success": true}))
            .await;
    });
    result.unwrap();
    assert_eq!(client.registered_session_count_for_test(), 0);
    timeout(TIMEOUT, handler_dropped.notified())
        .await
        .expect("client shutdown must release the resumed callback owner");
}

#[tokio::test]
async fn missing_acknowledgment_preserves_the_accepted_resume_callback_owner() {
    assert_resume_callback_owner(None, false).await;
}

#[tokio::test]
async fn false_acknowledgment_preserves_the_accepted_resume_callback_owner() {
    assert_resume_callback_owner(Some(false), false).await;
}

#[tokio::test]
async fn rpc_rejection_restores_the_resident_callback_owner() {
    assert_resume_callback_owner(None, true).await;
}

#[tokio::test]
async fn legacy_handler_stays_unenrolled_and_never_silently_accepts_auto_mode_recovery() {
    for resume in [false, true] {
        let (session, _client, mut server, request) =
            open(resume, false, None, Arc::new(LegacyHandler)).await;
        let session = session.unwrap();
        assert_eq!(request["params"]["requestAutoModeSwitch"], true);
        assert!(request["params"].get("requestAutoModeRecovery").is_none());
        server
            .send_request(
                10,
                "autoModeSwitch.request",
                serde_json::json!({
                    "sessionId": server.session_id, "errorCode": "user_weekly_rate_limited",
                }),
            )
            .await;
        assert_eq!(
            timeout(TIMEOUT, server.read_response()).await.unwrap()["result"]["response"],
            "yes_always"
        );
        server.send_request(11, "autoModeSwitch.request", serde_json::json!({
            "sessionId": server.session_id, "errorCode": "model_overloaded",
            "autoModeRecovery": {"requestId": "offer-1", "originalModel": "explicit-model", "allowAlways": false},
        })).await;
        assert_eq!(
            timeout(TIMEOUT, server.read_response()).await.unwrap()["result"]["response"],
            "no"
        );
        disconnect(session, server).await;
    }
}

type Observation = (
    SessionId,
    Option<String>,
    Option<f64>,
    AutoModeRecoveryContext,
);

struct AutoModeRecoveryHandler {
    observed: mpsc::UnboundedSender<Observation>,
}

#[async_trait]
impl AutoModeSwitchHandler for AutoModeRecoveryHandler {
    async fn handle(
        &self,
        _session_id: SessionId,
        _error_code: Option<String>,
        _retry_after_seconds: Option<f64>,
    ) -> AutoModeSwitchResponse {
        panic!("recovery reached the legacy handler");
    }

    async fn handle_auto_mode_recovery(
        &self,
        session_id: SessionId,
        error_code: Option<String>,
        retry_after_seconds: Option<f64>,
        auto_mode_recovery: AutoModeRecoveryContext,
    ) -> AutoModeSwitchResponse {
        self.observed
            .send((
                session_id,
                error_code,
                retry_after_seconds,
                auto_mode_recovery,
            ))
            .unwrap();
        AutoModeSwitchResponse::Yes
    }
}

#[tokio::test]
async fn complete_context_round_trips_to_the_auto_mode_recovery_handler() {
    for resume in [false, true] {
        let (observed, mut observations) = mpsc::unbounded_channel();
        let handler = Arc::new(AutoModeRecoveryHandler { observed });
        let (session, _client, mut server, request) = open(resume, true, Some(true), handler).await;
        let session = session.unwrap();
        assert_eq!(request["params"]["requestAutoModeRecovery"], true);
        assert!(session.auto_mode_recovery_supported());
        let context = serde_json::json!({
            "requestId": "offer-1", "originalModel": "explicit-model", "allowAlways": false,
            "interactionId": "interaction-1", "originatingMessageId": "user-1",
        });
        server.send_request(12, "autoModeSwitch.request", serde_json::json!({
            "sessionId": server.session_id, "errorCode": "model_overloaded", "retryAfterSeconds": 3.5,
            "autoModeRecovery": context,
        })).await;
        let (session_id, code, retry_after, auto_mode_recovery) =
            observations.recv().await.unwrap();
        assert_eq!(session_id, *session.id());
        assert_eq!(code.as_deref(), Some("model_overloaded"));
        assert_eq!(retry_after, Some(3.5));
        assert_eq!(serde_json::to_value(auto_mode_recovery).unwrap(), context);
        let reply = timeout(TIMEOUT, server.read_response()).await.unwrap();
        assert_eq!(reply["result"]["response"], "yes");
        disconnect(session, server).await;
    }
}

#[tokio::test]
async fn capability_updates_preserve_and_revoke_acknowledgment() {
    let (session, _client, mut server, _) =
        open(false, true, Some(true), Arc::new(LegacyHandler)).await;
    let session = session.unwrap();
    let mut events = session.subscribe();
    server
        .send_event(
            "capabilities.changed",
            serde_json::json!({"ui": {"elicitation": false}}),
        )
        .await;
    timeout(TIMEOUT, events.recv()).await.unwrap().unwrap();
    let caps = session.capabilities().ui.unwrap();
    assert_eq!(caps.elicitation, Some(false));
    assert!(session.auto_mode_recovery_supported());
    server
        .send_event(
            "capabilities.changed",
            serde_json::json!({"ui": {"autoModeRecovery": false}}),
        )
        .await;
    timeout(TIMEOUT, events.recv()).await.unwrap().unwrap();
    assert!(!session.auto_mode_recovery_supported());
    disconnect(session, server).await;
}
