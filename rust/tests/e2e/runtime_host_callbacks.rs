use std::sync::Arc;
use std::sync::atomic::{AtomicUsize, Ordering};

use async_trait::async_trait;
use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::hooks::{HookContext, PreToolUseInput, PreToolUseOutput, SessionHooks};
use github_copilot_sdk::session::Session;
use github_copilot_sdk::tool::ToolHandler;
use github_copilot_sdk::{
    AhpSessionRequest, AhpSessionResumeRequest, Error, SystemMessageConfig, Tool, ToolInvocation,
    ToolResult,
};
use tokio::sync::mpsc;
use tokio_util::sync::CancellationToken;

use super::*;

const TOOL_SNAPSHOT: &str = "both_clients_see_tool_request_and_completion_events";
const TOOL_PROMPT: &str = "Use the magic_number tool with seed 'hello' and tell me the result";
const MARKER: &str = "RUST_APPLICATION_OWNED_AHP_PROMPT";

#[tokio::test]
#[ignore = "requires local runtime host artifacts"]
#[serial_test::serial]
async fn callback_resume_composes_tools_after_application_session_runtime_restart() {
    run_snapshot("runtime_host", "app_resume_callback_composes_tools_after_history", |ctx| {
        Box::pin(async move {
            let first_owner = start(ctx).await;
            let old_pid = first_owner.pid().unwrap();
            let first_callbacks = Arc::new(AppCallbacks::default());
            let (options, mut created, mut first_released) = options_with_app(first_callbacks.clone());
            let first = first_owner.start_ahp_host(options).await.unwrap();
            let ahp = connect(&first).await;
            // Restore the persisted client's tools, not dynamically register a new tool on resume.
            let client_id = ahp.client_id.clone();
            let client_tools = json!([{
                "name":"client_echo","description":"Echoes text from the AHP client",
                "inputSchema":{"type":"object","properties":{"text":{"type":"string"}},"required":["text"]}
            }]);
            let (uri, chat, subscription) = create_with_tools(&ahp, ctx, client_tools.clone()).await;
            let original = deadline(created.recv()).await.unwrap();
            assert!(created.try_recv().is_err());
            let session_id = original.id();
            assert_ne!(session_id.as_str(), uri.strip_prefix("ahp-session:/").unwrap());
            topology(&first, &first_owner, &home(ctx));
            turn(&ahp, &chat, subscription).await;
            assert_eq!(first_callbacks.tools.load(Ordering::SeqCst), 0);
            first.dispose().await.unwrap();
            stopped(&first, &ahp, &first_owner).await;
            assert!(Arc::ptr_eq(&original, &deadline(first_released.recv()).await.unwrap()));
            assert!(!original.get_events().await.unwrap().is_empty());
            original.disconnect().await.unwrap();
            ahp.client.shutdown().await;
            first_owner.stop().await.unwrap();
            reaped(old_pid).await;

            let owner = start(ctx).await;
            let callbacks = Arc::new(AppCallbacks {
                expected_session_id: Some(session_id.clone()),
                ..Default::default()
            });
            let app = callbacks.clone();
            let resume_calls = Arc::new(AtomicUsize::new(0));
            let resumes = resume_calls.clone();
            let create_calls = Arc::new(AtomicUsize::new(0));
            let creates = create_calls.clone();
            let (resumed_tx, mut resumed) = mpsc::unbounded_channel();
            let (released_tx, mut released) = mpsc::unbounded_channel();
            let expected_id = session_id.clone();
            let expected_directory = ctx.work_dir().canonicalize().unwrap();
            let options = local_options()
                .with_create_session(move |_: AhpSessionRequest, _: Client| {
                    creates.fetch_add(1, Ordering::SeqCst);
                    async { Err::<Arc<Session>, Error>(std::io::Error::other("resume must not create").into()) }
                })
                .with_resume_session(move |request: AhpSessionResumeRequest, client: Client| {
                    let app = app.clone();
                    let resumed_tx = resumed_tx.clone();
                    assert_eq!(request.config.session_id, expected_id);
                    assert_eq!(request.config.continue_pending_work, Some(false));
                    assert_eq!(request.config.working_directory.as_ref().unwrap().canonicalize().unwrap(), expected_directory);
                    assert!(!request.cancellation_token.is_cancelled());
                    resumes.fetch_add(1, Ordering::SeqCst);
                    async move {
                        let tool = Tool::new("magic_number")
                            .with_description("Returns a magic number")
                            .with_parameters(json!({
                                "type":"object",
                                "properties":{"seed":{"type":"string","description":"A seed value"}},
                                "required":["seed"]
                            }))
                            .with_handler(app.clone());
                        let original = Arc::new(client.resume_session(
                            request.config
                                .with_permission_handler(Arc::new(ApproveAllHandler))
                                .with_system_message(SystemMessageConfig::new().with_mode("append").with_content(MARKER))
                                .with_tools(vec![tool]).with_hooks(app)
                        ).await?);
                        resumed_tx.send(original.clone()).unwrap();
                        Ok(original)
                    }
                })
                .with_on_session_released(move |session| { released_tx.send(session).unwrap(); });
            let host = owner.start_ahp_host(options).await.unwrap();
            let ahp = connect_url_as(host.url.as_deref().expect("local listener URL"), host.token.as_deref(), client_id).await;
            topology(&host, &owner, &home(ctx));
            resume(&ahp, &uri, None).await;
            let restored = deadline(resumed.recv()).await.unwrap();
            assert_eq!(restored.id(), session_id);
            assert!(!Arc::ptr_eq(&original, &restored));
            let (session, _) = ahp.client.subscribe(uri.clone()).await.unwrap();
            let session = serde_json::to_value(session).unwrap();
            let chat_uri = session["snapshot"]["state"]["defaultChat"].as_str().unwrap();
            let (history, subscription) = ahp.client.subscribe(chat_uri.into()).await.unwrap();
            let history = serde_json::to_value(history).unwrap();
            let turns = history["snapshot"]["state"]["turns"].as_array().unwrap();
            assert_eq!(turns.len(), 1);
            assert_eq!(turns[0]["message"]["text"], PROMPT);
            ahp.client.dispatch(uri.clone(), serde_json::from_value(json!({
                "type":"session/activeClientSet",
                "activeClient":{"clientId":ahp.client_id, "displayName":"Resumed tool owner",
                    "tools":client_tools}
            })).unwrap()).await.unwrap();
            deadline(async {
                loop {
                    let (session, _) = ahp.client.subscribe(uri.clone()).await.unwrap();
                    let state = serde_json::to_value(session).unwrap();
                    if state["snapshot"]["state"]["activeClients"].as_array().unwrap().iter()
                        .any(|entry| entry["clientId"] == ahp.client_id) { break; }
                    tokio::task::yield_now().await;
                }
            }).await;
            let client_calls = AtomicUsize::new(0);
            let client_tool = |name: &str, input: Value| {
                assert_eq!(name, "client_echo");
                assert_eq!(input, json!({"text":"ping"}));
                client_calls.fetch_add(1, Ordering::SeqCst);
                "CLIENT_ECHO_ping".to_owned()
            };
            turn_with_client_tools(&ahp, chat_uri, subscription,
                "Call magic_number with seed 'hello' and client_echo with text 'ping', then report both results",
                "Both tools ran: MAGIC_hello_42 and CLIENT_ECHO_ping", Some(&client_tool)).await;
            assert_eq!(callbacks.tools.load(Ordering::SeqCst), 1);
            assert_eq!(first_callbacks.tools.load(Ordering::SeqCst), 0);
            assert!(callbacks.hooks.load(Ordering::SeqCst) > 0);
            assert_eq!(client_calls.load(Ordering::SeqCst), 1);
            let exchanges = ctx.exchanges();
            let composed: Vec<_> = exchanges.iter().filter(|exchange|
                exchange["request"]["messages"].to_string().contains("Call magic_number")).collect();
            assert_eq!(composed.len(), 2);
            for exchange in &composed {
                let request = &exchange["request"];
                let tools = request["tools"].as_array().unwrap();
                for name in ["magic_number", "client_echo"] {
                    assert_eq!(tools.iter().filter(|tool| tool["function"]["name"] == name).count(), 1);
                }
                let messages = request["messages"].to_string();
                assert!(messages.contains(MARKER));
                assert!(messages.contains(PROMPT));
            }
            let mut results: Vec<_> = composed.last().unwrap()["request"]["messages"].as_array().unwrap()
                .iter().filter(|message| message["role"] == "tool")
                .map(|message| message["content"].as_str().unwrap()).collect();
            results.sort();
            assert_eq!(results, ["CLIENT_ECHO_ping", "MAGIC_hello_42"]);
            ahp.client.subscribe(uri.clone()).await.unwrap();
            assert_eq!(resume_calls.load(Ordering::SeqCst), 1);
            assert_eq!(create_calls.load(Ordering::SeqCst), 0);
            assert!(released.try_recv().is_err());
            host.dispose().await.unwrap();
            stopped(&host, &ahp, &owner).await;
            assert!(Arc::ptr_eq(&restored, &deadline(released.recv()).await.unwrap()));
            assert!(!restored.get_events().await.unwrap().is_empty());
            host.dispose().await.unwrap();
            assert!(released.try_recv().is_err());
            ahp.client.shutdown().await;
            restored.disconnect().await.unwrap();
            owner.stop().await.unwrap();
        })
    }).await;
}

#[tokio::test]
#[ignore = "requires local runtime host artifacts"]
#[serial_test::serial]
async fn callback_owner_drop_reclaims_original_closes_listener_and_reaps_runtime() {
    run(|ctx| {
        Box::pin(async move {
            let owner = start(ctx).await;
            let runtime_pid = owner.pid().unwrap();
            let (options, mut created, _) = options_with_app(Arc::default());
            let (released_tx, mut released) = mpsc::unbounded_channel();
            let host = owner
                .start_ahp_host(options.with_on_session_released(move |session| {
                    released_tx.send(Arc::downgrade(&session)).unwrap();
                }))
                .await
                .unwrap();
            let ahp = connect(&host).await;
            create(&ahp, ctx).await;
            let original = deadline(created.recv()).await.unwrap();
            let session = Arc::downgrade(&original);
            drop(original);
            drop(created);
            drop(owner);
            deadline(released.recv()).await.unwrap();
            deadline(async {
                while session.upgrade().is_some() {
                    tokio::task::yield_now().await;
                }
            })
            .await;
            listener_closed(&host, &ahp).await;
            reaped(runtime_pid).await;
            assert!(released.recv().await.is_none());
            ahp.client.shutdown().await;
        })
    })
    .await;
}

#[tokio::test]
#[ignore = "requires local runtime host artifacts"]
#[serial_test::serial]
async fn callback_pending_disposal_releases_without_transport_drain_timeout() {
    run(|ctx| {
        Box::pin(async move {
            let owner = start(ctx).await;
            let (ready_tx, mut ready) = mpsc::unbounded_channel();
            let (released_tx, mut released) = mpsc::unbounded_channel();
            let finish = CancellationToken::new();
            let gate = finish.clone();
            let options = local_options()
                .with_create_session(move |request: AhpSessionRequest, client: Client| {
                    let ready_tx = ready_tx.clone();
                    let gate = gate.clone();
                    async move {
                        let original = Arc::new(client.create_session(request.config).await?);
                        ready_tx
                            .send((original.clone(), request.cancellation_token))
                            .unwrap();
                        gate.cancelled().await;
                        Ok(original)
                    }
                })
                .with_on_session_released(move |session| {
                    released_tx.send(session).unwrap();
                });
            let host = owner.start_ahp_host(options).await.unwrap();
            let ahp = connect(&host).await;
            authenticate(&ahp).await;
            let params =
                create_params(&ahp, ctx, &format!("ahp-session:/{}", uuid::Uuid::new_v4()));
            let creation = ahp.client.request::<_, Value>("createSession", params);
            let check = async {
                let (original, cancellation) = deadline(ready.recv()).await.unwrap();
                host.dispose().await.unwrap();
                deadline(cancellation.cancelled()).await;
                finish.cancel();
                assert!(Arc::ptr_eq(
                    &original,
                    &deadline(released.recv()).await.unwrap()
                ));
                original.get_events().await.unwrap();
                original.disconnect().await.unwrap();
            };
            let (result, ()) = tokio::join!(creation, check);
            assert!(result.is_err());
            stopped(&host, &ahp, &owner).await;
            let disposal = host.dispose().await;
            assert!(released.try_recv().is_err());
            ahp.client.shutdown().await;
            owner.stop().await.unwrap();
            disposal.expect("listener disposal must cancel pending handoff before SDK drain");
        })
    })
    .await;
}

#[derive(Default)]
struct AppCallbacks {
    tools: AtomicUsize,
    hooks: AtomicUsize,
    expected_session_id: Option<SessionId>,
}

#[async_trait]
impl ToolHandler for AppCallbacks {
    async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error> {
        assert_eq!(invocation.tool_name, "magic_number");
        if let Some(session_id) = &self.expected_session_id {
            assert_eq!(&invocation.session_id, session_id);
        }
        assert_eq!(invocation.arguments["seed"], "hello");
        self.tools.fetch_add(1, Ordering::SeqCst);
        Ok(ToolResult::Text("MAGIC_hello_42".into()))
    }
}

#[async_trait]
impl SessionHooks for AppCallbacks {
    async fn on_pre_tool_use(
        &self,
        _input: PreToolUseInput,
        _ctx: HookContext,
    ) -> Option<PreToolUseOutput> {
        self.hooks.fetch_add(1, Ordering::SeqCst);
        None
    }
}

fn options_with_app(
    callbacks: Arc<AppCallbacks>,
) -> (
    AhpHostOptions,
    mpsc::UnboundedReceiver<Arc<Session>>,
    mpsc::UnboundedReceiver<Arc<Session>>,
) {
    let (created_tx, created) = mpsc::unbounded_channel();
    let (released_tx, released) = mpsc::unbounded_channel();
    let options = local_options()
        .with_create_session(move |request: AhpSessionRequest, client: Client| {
            let callbacks = callbacks.clone();
            let created_tx = created_tx.clone();
            async move {
                assert!(!request.cancellation_token.is_cancelled());
                let requested_id = request
                    .config
                    .session_id
                    .clone()
                    .expect("host-requested runtime ID");
                let tool = Tool::new("magic_number")
                    .with_description("Returns a magic number")
                    .with_parameters(json!({
                        "type": "object",
                        "properties": {"seed": {"type": "string", "description": "A seed value"}},
                        "required": ["seed"]
                    }))
                    .with_handler(callbacks.clone());
                let original = Arc::new(
                    client
                        .create_session(
                            request
                                .config
                                .with_permission_handler(Arc::new(ApproveAllHandler))
                                .with_system_message(
                                    SystemMessageConfig::new()
                                        .with_mode("append")
                                        .with_content(MARKER),
                                )
                                .with_tools(vec![tool])
                                .with_hooks(callbacks),
                        )
                        .await?,
                );
                assert_eq!(original.id(), requested_id);
                created_tx.send(original.clone()).unwrap();
                Ok(original)
            }
        })
        .with_on_session_released(move |session| {
            released_tx.send(session).unwrap();
        });
    (options, created, released)
}

#[tokio::test]
#[ignore = "requires local runtime host artifacts"]
#[serial_test::serial]
async fn resident_publication_preserves_original_without_invoking_factories() {
    use github_copilot_sdk::rpc::ModeSetRequest;
    use github_copilot_sdk::session_events::SessionMode;

    run_snapshot("multi_client", TOOL_SNAPSHOT, |ctx| {
        Box::pin(async move {
            let owner = start(ctx).await;
            let callbacks = Arc::new(AppCallbacks::default());
            let tool = Tool::new("magic_number")
                .with_description("Returns a magic number")
                .with_parameters(json!({
                    "type": "object",
                    "properties": {"seed": {"type": "string", "description": "A seed value"}},
                    "required": ["seed"]
                }))
                .with_handler(callbacks.clone());
            let original = owner
                .create_session(
                    ctx.approve_all_session_config()
                        .with_model("claude-sonnet-5")
                        .with_system_message(
                            SystemMessageConfig::new()
                                .with_mode("append")
                                .with_content(MARKER),
                        )
                        .with_excluded_tools(["bash"])
                        .with_tools(vec![tool])
                        .with_hooks(callbacks.clone()),
                )
                .await
                .unwrap();
            original
                .rpc()
                .mode()
                .set(ModeSetRequest {
                    mode: SessionMode::Plan,
                    ..Default::default()
                })
                .await
                .unwrap();
            let creates = Arc::new(AtomicUsize::new(0));
            let resumes = Arc::new(AtomicUsize::new(0));
            let releases = Arc::new(AtomicUsize::new(0));
            let (created, resumed, released) = (creates.clone(), resumes.clone(), releases.clone());
            let host = owner
                .start_ahp_host(
                    local_options()
                        .with_create_session(move |_: AhpSessionRequest, _: Client| {
                            created.fetch_add(1, Ordering::SeqCst);
                            async {
                                Err::<Arc<Session>, Error>(
                                    std::io::Error::other("publication must not create").into(),
                                )
                            }
                        })
                        .with_resume_session(move |_: AhpSessionResumeRequest, _: Client| {
                            resumed.fetch_add(1, Ordering::SeqCst);
                            async {
                                Err::<Arc<Session>, Error>(
                                    std::io::Error::other("resident publication must not resume")
                                        .into(),
                                )
                            }
                        })
                        .with_on_session_released(move |_| {
                            released.fetch_add(1, Ordering::SeqCst);
                        }),
                )
                .await
                .unwrap();
            let ahp = connect(&host).await;
            let published = host.publish_session(original.id().as_str()).await.unwrap();
            assert_eq!(published.session_id, original.id());
            authenticate(&ahp).await;
            let attachment = ahp.client.subscribe(published.session_uri).await;
            assert_eq!(creates.load(Ordering::SeqCst), 0);
            assert_eq!(
                resumes.load(Ordering::SeqCst),
                0,
                "attaching an already-resident publication must not invoke its resume factory"
            );
            let (session, _) = attachment.expect("attach original resident session");
            assert_eq!(
                serde_json::to_value(session).unwrap()["snapshot"]["state"]["lifecycle"],
                "ready"
            );
            assert_eq!(
                original.rpc().mode().get().await.unwrap(),
                SessionMode::Plan
            );
            original
                .rpc()
                .mode()
                .set(ModeSetRequest {
                    mode: SessionMode::Interactive,
                    ..Default::default()
                })
                .await
                .unwrap();
            let response = original.send_and_wait(TOOL_PROMPT).await.unwrap().unwrap();
            assert!(
                response.data["content"]
                    .as_str()
                    .unwrap()
                    .contains("MAGIC_hello_42")
            );
            assert_eq!(callbacks.tools.load(Ordering::SeqCst), 1);
            assert!(callbacks.hooks.load(Ordering::SeqCst) > 0);
            let exchanges = ctx.exchanges();
            let request = &exchanges.last().expect("application inference")["request"];
            assert!(request["messages"].to_string().contains(MARKER));
            let tools = request["tools"].as_array().unwrap();
            assert!(
                tools
                    .iter()
                    .any(|tool| tool["function"]["name"] == "magic_number")
            );
            assert!(!tools.iter().any(|tool| tool["function"]["name"] == "bash"));
            host.dispose().await.unwrap();
            stopped(&host, &ahp, &owner).await;
            assert_eq!(creates.load(Ordering::SeqCst), 0);
            assert_eq!(resumes.load(Ordering::SeqCst), 0);
            assert_eq!(releases.load(Ordering::SeqCst), 0);
            assert!(!original.get_events().await.unwrap().is_empty());
            ahp.client.shutdown().await;
            original.disconnect().await.unwrap();
            owner.stop().await.unwrap();
        })
    })
    .await;
}

#[tokio::test]
#[ignore = "requires local runtime host artifacts"]
#[serial_test::serial]
async fn callback_handoff_preserves_tools_hooks_prompt_and_original_arc() {
    run_snapshot("multi_client", TOOL_SNAPSHOT, |ctx| {
        Box::pin(async move {
            let owner = start(ctx).await;
            let callbacks = Arc::new(AppCallbacks::default());
            let (options, mut created, mut released) = options_with_app(callbacks.clone());
            let host = owner.start_ahp_host(options).await.unwrap();
            let ahp = connect(&host).await;
            let (uri, chat, subscription) = create(&ahp, ctx).await;
            let original = deadline(created.recv()).await.unwrap();
            assert_ne!(
                original.id().as_str(),
                uri.strip_prefix("ahp-session:/").unwrap()
            );
            topology(&host, &owner, &home(ctx));
            turn_with_prompt(&ahp, &chat, subscription, TOOL_PROMPT, "MAGIC_hello_42").await;
            assert_eq!(callbacks.tools.load(Ordering::SeqCst), 1);
            assert!(callbacks.hooks.load(Ordering::SeqCst) > 0);
            let exchanges = ctx.exchanges();
            assert!(serde_json::to_string(&exchanges).unwrap().contains(MARKER));
            assert!(released.try_recv().is_err());
            host.dispose().await.unwrap();
            stopped(&host, &ahp, &owner).await;
            assert!(Arc::ptr_eq(
                &original,
                &deadline(released.recv()).await.unwrap()
            ));
            assert!(!original.get_events().await.unwrap().is_empty());
            host.dispose().await.unwrap();
            assert!(released.try_recv().is_err());
            ahp.client.shutdown().await;
            original.disconnect().await.unwrap();
            owner.stop().await.unwrap();
        })
    })
    .await;
}

#[tokio::test]
#[ignore = "requires local runtime host artifacts"]
#[serial_test::serial]
async fn callback_attach_failure_releases_original_without_losing_app_tool() {
    run_snapshot("multi_client", TOOL_SNAPSHOT, |ctx| {
        Box::pin(async move {
            let owner = start(ctx).await;
            let callbacks = Arc::new(AppCallbacks::default());
            let (options, mut created, mut released) = options_with_app(callbacks.clone());
            let host = owner.start_ahp_host(options).await.unwrap();
            let ahp = connect(&host).await;
            authenticate(&ahp).await;
            let uri = format!("ahp-session:/{}", uuid::Uuid::new_v4());
            let mut params = create_params(&ahp, ctx, &uri);
            params["activeClient"]["tools"] = json!([{
                "name":"magic_number", "description":"clashing participant",
                "inputSchema":{"type":"object","properties":{}}
            }]);
            let error = ahp
                .client
                .request::<_, Value>("createSession", params)
                .await
                .unwrap_err();
            assert!(error.to_string().contains("attach failed"), "{error}");
            let original = deadline(created.recv()).await.unwrap();
            assert!(Arc::ptr_eq(
                &original,
                &deadline(released.recv()).await.unwrap()
            ));
            let response = original.send_and_wait(TOOL_PROMPT).await.unwrap().unwrap();
            assert!(
                response.data["content"]
                    .as_str()
                    .unwrap()
                    .contains("MAGIC_hello_42")
            );
            assert_eq!(callbacks.tools.load(Ordering::SeqCst), 1);
            host.dispose().await.unwrap();
            assert!(released.try_recv().is_err());
            ahp.client.shutdown().await;
            original.disconnect().await.unwrap();
            owner.stop().await.unwrap();
        })
    })
    .await;
}

#[tokio::test]
#[ignore = "requires local runtime host artifacts"]
#[serial_test::serial]
async fn callback_disposal_and_owner_termination_release_once() {
    run(|ctx| {
        Box::pin(async move {
            for owner_loss in [false, true] {
                let port = unused_port();
                let runtime = Client::start(options(ctx).with_transport(Transport::Tcp {
                    port,
                    connection_token: Some(RUNTIME_TOKEN.into()),
                }))
                .await
                .unwrap();
                let owner = Client::start(options(ctx).with_transport(Transport::External {
                    host: "127.0.0.1".into(),
                    port,
                    connection_token: Some(RUNTIME_TOKEN.into()),
                }))
                .await
                .unwrap();
                let (options, mut created, mut released) = options_with_app(Arc::default());
                let host = owner.start_ahp_host(options).await.unwrap();
                let ahp = connect(&host).await;
                create(&ahp, ctx).await;
                let original = deadline(created.recv()).await.unwrap();
                if owner_loss {
                    owner.force_stop();
                } else {
                    host.dispose().await.unwrap();
                }
                assert!(Arc::ptr_eq(
                    &original,
                    &deadline(released.recv()).await.unwrap()
                ));
                if owner_loss {
                    stopped_after_owner_disconnect(&host, &ahp, &runtime).await;
                } else {
                    stopped(&host, &ahp, &runtime).await;
                }
                if !owner_loss {
                    original.get_events().await.unwrap();
                    original.disconnect().await.unwrap();
                    host.dispose().await.unwrap();
                    owner.stop().await.unwrap();
                }
                assert!(released.try_recv().is_err());
                ahp.client.shutdown().await;
                runtime.stop().await.unwrap();
            }
        })
    })
    .await;
}

#[tokio::test]
#[ignore = "requires local runtime host artifacts"]
#[serial_test::serial]
async fn callback_cancellation_releases_late_session_after_host_disposal() {
    run(|ctx| {
        Box::pin(async move {
            let owner = start(ctx).await;
            let (ready_tx, mut ready) = mpsc::unbounded_channel();
            let (released_tx, mut released) = mpsc::unbounded_channel();
            let finish = CancellationToken::new();
            let gate = finish.clone();
            let options = local_options()
                .with_create_session(move |request: AhpSessionRequest, client: Client| {
                    let ready_tx = ready_tx.clone();
                    let gate = gate.clone();
                    async move {
                        let session = Arc::new(client.create_session(request.config).await?);
                        ready_tx
                            .send((session.clone(), request.cancellation_token))
                            .unwrap();
                        gate.cancelled().await;
                        Ok(session)
                    }
                })
                .with_on_session_released(move |session| {
                    released_tx.send(session).unwrap();
                });
            let host = owner.start_ahp_host(options).await.unwrap();
            let ahp = connect(&host).await;
            authenticate(&ahp).await;
            let params =
                create_params(&ahp, ctx, &format!("ahp-session:/{}", uuid::Uuid::new_v4()));
            let creation = ahp.client.request::<_, Value>("createSession", params);
            let check = async {
                let (original, cancellation) = deadline(ready.recv()).await.unwrap();
                let disposal = host.dispose();
                let finish_callback = async {
                    deadline(cancellation.cancelled()).await;
                    assert!(released.try_recv().is_err());
                    finish.cancel();
                    assert!(Arc::ptr_eq(
                        &original,
                        &deadline(released.recv()).await.unwrap()
                    ));
                    original.get_events().await.unwrap();
                    original.disconnect().await.unwrap();
                };
                let (result, ()) = tokio::join!(disposal, finish_callback);
                result.unwrap();
            };
            let (result, ()) = tokio::join!(creation, check);
            assert!(result.is_err());
            stopped(&host, &ahp, &owner).await;
            host.dispose().await.unwrap();
            assert!(released.try_recv().is_err());
            ahp.client.shutdown().await;
            owner.stop().await.unwrap();
        })
    })
    .await;
}
