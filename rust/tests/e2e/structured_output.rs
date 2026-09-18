use std::collections::HashMap;
use std::sync::Arc;
use std::sync::atomic::{AtomicUsize, Ordering};
use std::time::Duration;

use async_trait::async_trait;
use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::hooks::{AgentStopInput, AgentStopOutput, HookContext, SessionHooks};
use github_copilot_sdk::rpc::SendMessagesRequest;
use github_copilot_sdk::tool::{define_tool, schema_for};
use github_copilot_sdk::{DeliveryMode, MessageOptions, ProviderConfig, SessionConfig, ToolResult};
use schemars::JsonSchema;
use serde::Deserialize;
use serde_json::json;
use tokio::sync::Semaphore;

use super::support::{DEFAULT_TEST_TOKEN, assistant_message_content, collect_until_idle};

#[derive(Debug, PartialEq, Deserialize, JsonSchema)]
#[serde(deny_unknown_fields)]
struct Inventory {
    count: i32,
    color: String,
}

#[derive(Deserialize, JsonSchema)]
#[serde(deny_unknown_fields)]
struct Answer {
    answer: i32,
}

#[derive(Debug, PartialEq, Deserialize, JsonSchema)]
#[serde(deny_unknown_fields)]
struct ToolAnswer {
    answer: i32,
    contract: String,
}

#[derive(Deserialize, JsonSchema)]
#[serde(deny_unknown_fields)]
struct First {
    first: i32,
}

#[derive(Deserialize, JsonSchema)]
#[serde(deny_unknown_fields)]
struct Second {
    second: i32,
}

#[derive(Deserialize, JsonSchema)]
struct NoArgs {}

fn config(proxy: &str) -> SessionConfig {
    SessionConfig::default()
        .with_model("gpt-4.1")
        .with_permission_handler(Arc::new(ApproveAllHandler))
        .with_available_tools(Vec::<String>::new())
        .with_provider(
            ProviderConfig::new(proxy)
                .with_provider_type("openai")
                .with_wire_api("completions")
                .with_api_key(DEFAULT_TEST_TOKEN)
                .with_model_id("gpt-4.1")
                .with_wire_model("gpt-4.1")
                .with_headers(HashMap::from([
                    (
                        "Copilot-Integration-Id".into(),
                        "copilot-developer-cli".into(),
                    ),
                    ("Copilot-Harness-Id".into(), "copilot-sdk".into()),
                    ("X-GitHub-Api-Version".into(), "2026-08-01".into()),
                ])),
        )
}

#[tokio::test]
async fn infers_typed_result_after_custom_tool() {
    super::support::with_shared_e2e_context(
        &E2E,
        "structured_output",
        "infers_typed_result_after_custom_tool",
        |ctx| {
            Box::pin(async move {
                let calls = Arc::new(AtomicUsize::new(0));
                let counter = calls.clone();
                let tool = define_tool(
                    "get_inventory",
                    "Get the current widget inventory.",
                    move |_inv, _: NoArgs| {
                        counter.fetch_add(1, Ordering::SeqCst);
                        async {
                            Ok(ToolResult::Text(
                                "The inventory contains 42 red widgets.".into(),
                            ))
                        }
                    },
                );
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        config(ctx.proxy_url())
                            .with_tools(vec![tool])
                            .with_streaming(true),
                    )
                    .await
                    .unwrap();
                let events = session.subscribe();
                let result: Inventory = session
                    .send_and_wait_typed(
                        "Call get_inventory, then report the widget count and color.",
                    )
                    .await
                    .unwrap();
                assert_eq!(
                    result,
                    Inventory {
                        count: 42,
                        color: "red".into()
                    }
                );
                assert!(calls.load(Ordering::SeqCst) > 0);
                assert!(
                    collect_until_idle(events).await.iter().any(|event| event.event_type == "assistant.message_delta"),
                    "Typed wait must preserve streaming text updates"
                );
                let ordinary = session
                    .send_and_wait("Now reply with exactly the plain text HELLO, not JSON.")
                    .await
                    .unwrap()
                    .unwrap();
                assert_eq!(assistant_message_content(&ordinary).trim(), "HELLO");
                let exchanges = ctx.exchanges();
                assert!(exchanges.len() >= 3);
                for exchange in &exchanges[..exchanges.len() - 1] {
                    assert_eq!(exchange["request"]["response_format"], json!({
                        "type": "json_schema",
                        "json_schema": {"name": "response", "strict": true, "schema": schema_for::<Inventory>()}
                    }));
                }
                assert!(exchanges.last().unwrap()["request"].get("response_format").is_none());
                session.disconnect().await.unwrap();
                client.stop().await.unwrap();
            })
        },
    )
    .await;
}

#[tokio::test]
async fn sends_explicit_schema_for_message_and_batch() {
    super::support::with_shared_e2e_context(&E2E, "structured_output", "sends_explicit_schema_for_message_and_batch", |ctx| {
        Box::pin(async move {
            let client = ctx.start_client().await;
            let session = client.create_session(config(ctx.proxy_url())).await.unwrap();
            let mut events = session.subscribe();
            let request: SendMessagesRequest = serde_json::from_value(json!({
                "messages": [
                    {"prompt":"There are 42 red widgets in stock."},
                    {"prompt":"Report the widget count and color."}
                ],
                "responseFormat": {
                    "type":"json_schema",
                    "jsonSchema":{"name":"inventory","strict":true,"schema":schema_for::<Inventory>()}
                }
            })).unwrap();
            let accepted = session.rpc().send_messages(request).await.unwrap();
            let mut final_message = None;
            loop {
                let event = tokio::time::timeout(Duration::from_secs(30), events.recv()).await.unwrap().unwrap();
                if event.agent_id.is_some() { continue; }
                match event.event_type.as_str() {
                    "assistant.message" if event.data["originatingMessageId"] == *accepted.message_ids.last().unwrap() => final_message = Some(event),
                    "session.idle" => break,
                    "session.error" => panic!("session error: {:?}", event.data),
                    _ => {}
                }
            }
            let batch: Inventory = serde_json::from_str(&assistant_message_content(&final_message.unwrap())).unwrap();
            assert_eq!(batch, Inventory { count: 42, color: "red".into() });
            let raw = session.send_and_wait(MessageOptions::new(
                "The inventory now has 21 blue widgets. Report the new count and color."
            ).with_response_schema(schema_for::<Inventory>())).await.unwrap().unwrap();
            let updated: Inventory = serde_json::from_str(&assistant_message_content(&raw)).unwrap();
            assert_eq!(updated, Inventory { count: 21, color: "blue".into() });
            session.disconnect().await.unwrap();
            client.stop().await.unwrap();
        })
    }).await;
}

struct CorrectionHook {
    calls: AtomicUsize,
    reason: &'static str,
}

#[async_trait]
impl SessionHooks for CorrectionHook {
    async fn on_agent_stop(
        &self,
        _input: AgentStopInput,
        _ctx: HookContext,
    ) -> Option<AgentStopOutput> {
        (self.calls.fetch_add(1, Ordering::SeqCst) == 0).then(|| AgentStopOutput {
            decision: Some("block".into()),
            reason: Some(self.reason.into()),
        })
    }
}

#[tokio::test]
async fn typed_wait_returns_stop_hook_correction_after_terminal_tool() {
    super::support::with_shared_e2e_context(&E2E, "structured_output", "typed_wait_returns_stop_hook_correction_after_terminal_tool", |ctx| {
        Box::pin(async move {
            let calls = Arc::new(AtomicUsize::new(0));
            let counter = calls.clone();
            let mut tool = define_tool("lookup_number", "Return the number needed for the calculation.", move |_inv, _: NoArgs| {
                counter.fetch_add(1, Ordering::SeqCst);
                async { Ok(ToolResult::Text("58".into())) }
            });
            tool.is_terminal = true;
            tool.skip_permission = true;
            let hooks = Arc::new(CorrectionHook {
                calls: AtomicUsize::new(0),
                reason: "Correct the answer to 99, not 63. Do not use tools.",
            });
            let client = ctx.start_client().await;
            let session = client.create_session(config(ctx.proxy_url()).with_tools(vec![tool]).with_hooks(hooks.clone())).await.unwrap();
            let result: Answer = session.send_and_wait_typed(
                "Call lookup_number exactly once, then add 5 to the returned number. Do not guess its result."
            ).await.unwrap();
            assert_eq!(result.answer, 99);
            assert_eq!(calls.load(Ordering::SeqCst), 1);
            assert_eq!(hooks.calls.load(Ordering::SeqCst), 2);
            session.disconnect().await.unwrap();
            client.stop().await.unwrap();
        })
    }).await;
}

#[tokio::test]
async fn concurrent_typed_sends_return_their_own_results() {
    super::support::with_shared_e2e_context(
        &E2E,
        "structured_output",
        "concurrent_typed_sends_return_their_own_results",
        |ctx| {
            Box::pin(async move {
                let entered = Arc::new(Semaphore::new(0));
                let release = Arc::new(Semaphore::new(0));
                let tool_entered = entered.clone();
                let tool_release = release.clone();
                let tool = define_tool(
                    "first_number",
                    "Get the number for the first question.",
                    move |_inv, _: NoArgs| {
                        let entered = tool_entered.clone();
                        let release = tool_release.clone();
                        async move {
                            entered.add_permits(1);
                            release.acquire().await.unwrap().forget();
                            Ok(ToolResult::Text("42".into()))
                        }
                    },
                );
                let client = ctx.start_client().await;
                let session = client
                    .create_session(config(ctx.proxy_url()).with_tools(vec![tool]))
                    .await
                    .unwrap();
                let first = session.send_and_wait_typed::<First>(
                    "Call first_number exactly once and report its returned number.",
                );
                let second = async {
                    entered.acquire().await.unwrap().forget();
                    let waiting =
                        session.send_and_wait_typed::<Second>("What is 30 + 7? Do not use tools.");
                    let release_queued = async {
                        loop {
                            let queue = session.rpc().queue().pending_items().await.unwrap();
                            if !queue.items.is_empty() {
                                break;
                            }
                            tokio::time::sleep(Duration::from_millis(10)).await;
                        }
                        release.add_permits(1);
                    };
                    let (result, ()) = tokio::join!(waiting, release_queued);
                    result
                };
                let (first, second) = tokio::join!(first, second);
                assert_eq!(first.unwrap().first, 42);
                assert_eq!(second.unwrap().second, 37);
                session.disconnect().await.unwrap();
                client.stop().await.unwrap();
            })
        },
    )
    .await;
}

#[tokio::test]
async fn typed_wait_returns_stop_hook_correction() {
    super::support::with_shared_e2e_context(
        &E2E,
        "structured_output",
        "typed_wait_returns_stop_hook_correction",
        |ctx| {
            Box::pin(async move {
                let hooks = Arc::new(CorrectionHook {
                    calls: AtomicUsize::new(0),
                    reason: "Correct the answer to 99, not 42. Do not use tools.",
                });
                let client = ctx.start_client().await;
                let session = client
                    .create_session(config(ctx.proxy_url()).with_hooks(hooks.clone()))
                    .await
                    .unwrap();
                let events = session.subscribe();
                let result: Answer = session
                    .send_and_wait_typed("What is 19 + 23? Do not use tools.")
                    .await
                    .unwrap();
                assert_eq!(result.answer, 99);
                assert_eq!(hooks.calls.load(Ordering::SeqCst), 2);
                let replies: Vec<_> = collect_until_idle(events)
                    .await
                    .into_iter()
                    .filter(|event| {
                        event.agent_id.is_none() && event.event_type == "assistant.message"
                    })
                    .collect();
                assert_eq!(replies.len(), 2);
                assert!(
                    !replies[0].data["originatingMessageId"]
                        .as_str()
                        .unwrap()
                        .is_empty()
                );
                assert_eq!(
                    replies[0].data["originatingMessageId"],
                    replies[1].data["originatingMessageId"]
                );
                for (reply, expected) in replies.iter().zip([42, 99]) {
                    assert_eq!(
                        serde_json::from_str::<Answer>(&assistant_message_content(reply))
                            .unwrap()
                            .answer,
                        expected
                    );
                }
                session.disconnect().await.unwrap();
                client.stop().await.unwrap();
            })
        },
    )
    .await;
}

struct StopGate {
    entered: Semaphore,
    release: Semaphore,
    calls: AtomicUsize,
}

impl StopGate {
    fn new() -> Self {
        Self {
            entered: Semaphore::new(0),
            release: Semaphore::new(0),
            calls: AtomicUsize::new(0),
        }
    }
}

#[async_trait]
impl SessionHooks for StopGate {
    async fn on_agent_stop(
        &self,
        _input: AgentStopInput,
        _ctx: HookContext,
    ) -> Option<AgentStopOutput> {
        if self.calls.fetch_add(1, Ordering::SeqCst) == 0 {
            self.entered.add_permits(1);
            tokio::time::timeout(Duration::from_secs(30), self.release.acquire())
                .await
                .unwrap()
                .unwrap()
                .forget();
        }
        None
    }
}

#[tokio::test]
async fn typed_wait_returns_late_steering_response() {
    super::support::with_shared_e2e_context(
        &E2E,
        "structured_output",
        "typed_wait_returns_late_steering_response",
        |ctx| {
            Box::pin(async move {
                let gate = Arc::new(StopGate::new());
                let client = ctx.start_client().await;
                let session = client
                    .create_session(config(ctx.proxy_url()).with_hooks(gate.clone()))
                    .await
                    .unwrap();
                let events = session.subscribe();
                let waiting =
                    session.send_and_wait_typed::<Answer>("What is 19 + 23? Do not use tools.");
                let steer = async {
                    tokio::time::timeout(Duration::from_secs(30), gate.entered.acquire())
                        .await
                        .unwrap()
                        .unwrap()
                        .forget();
                    let origin = session
                        .send(
                            MessageOptions::new("Change the answer to 99. Do not use tools.")
                                .with_mode(DeliveryMode::Immediate),
                        )
                        .await
                        .unwrap();
                    gate.release.add_permits(1);
                    origin
                };
                let (result, steering_id) = tokio::join!(waiting, steer);
                assert_eq!(result.unwrap().answer, 99);
                assert_eq!(gate.calls.load(Ordering::SeqCst), 2);
                let replies: Vec<_> = collect_until_idle(events)
                    .await
                    .into_iter()
                    .filter(|event| {
                        event.agent_id.is_none() && event.event_type == "assistant.message"
                    })
                    .collect();
                assert_eq!(replies.len(), 2);
                assert_ne!(replies[0].data["originatingMessageId"], steering_id);
                assert!(
                    !replies[0].data["originatingMessageId"]
                        .as_str()
                        .unwrap()
                        .is_empty()
                );
                assert_eq!(
                    replies[0].data["originatingMessageId"],
                    replies[1].data["originatingMessageId"]
                );
                for (reply, expected) in replies.iter().zip([42, 99]) {
                    assert_eq!(
                        serde_json::from_str::<Answer>(&assistant_message_content(reply))
                            .unwrap()
                            .answer,
                        expected
                    );
                }
                session.disconnect().await.unwrap();
                client.stop().await.unwrap();
            })
        },
    )
    .await;
}

#[tokio::test]
async fn send_selects_correlated_response_after_idle() {
    super::support::with_shared_e2e_context(
        &E2E, "structured_output", "send_selects_correlated_response_after_idle", |ctx| {
            Box::pin(async move {
                let gate = Arc::new(StopGate::new());
                let tool = define_tool("read_inventory", "Read the current widget count and color.",
                    |_inv, _: NoArgs| async { Ok(ToolResult::Text("The inventory contains 42 red widgets.".into())) });
                let client = ctx.start_client().await;
                let session = client.create_session(config(ctx.proxy_url()).with_hooks(gate.clone()).with_tools(vec![tool])).await.unwrap();
                let events = session.subscribe();
                let origin = session.send(MessageOptions::new("Call read_inventory once, then report the current widget count and color.")
                    .with_response_schema(schema_for::<Inventory>())).await.unwrap();
                let collecting = collect_until_idle(events);
                tokio::pin!(collecting);
                tokio::select! {
                    biased;
                    _ = &mut collecting => panic!("Idle must wait for the stop hook"),
                    permit = tokio::time::timeout(Duration::from_secs(30), gate.entered.acquire()) => permit.unwrap().unwrap().forget(),
                }
                gate.release.add_permits(1);
                let replies: Vec<_> = collecting.await.into_iter()
                    .filter(|event| event.agent_id.is_none() && event.event_type == "assistant.message")
                    .collect();
                let reply = replies.last().unwrap();
                assert_eq!(reply.data["originatingMessageId"], origin);
                assert!(reply.data["toolRequests"].as_array().is_none_or(|tools| tools.is_empty()));
                assert!(replies.iter().any(|reply| reply.data["toolRequests"].as_array().is_some_and(|tools| !tools.is_empty())));
                assert_eq!(serde_json::from_str::<Inventory>(&assistant_message_content(reply)).unwrap(), Inventory { count: 42, color: "red".into() });
                session.disconnect().await.unwrap();
                client.stop().await.unwrap();
            })
        },
    ).await;
}

#[tokio::test]
async fn rejects_invalid_formats_before_admission() {
    super::support::with_shared_e2e_context(
        &E2E, "structured_output", "rejects_invalid_formats_before_admission", |ctx| {
            Box::pin(async move {
                let client = ctx.start_client().await;
                let session = client.create_session(config(ctx.proxy_url())).await.unwrap();
                let immediate = session.send_and_wait_typed::<Answer>(
                    MessageOptions::new("Must not be admitted").with_mode(DeliveryMode::Immediate)
                ).await.err().unwrap();
                assert!(immediate.to_string().contains("immediate"));
                let schema = json!({"type": "object", "description": "x".repeat(32 * 1024 * 1024)});
                let oversized = session.send_and_wait(MessageOptions::new("Must not be admitted")
                    .with_response_schema(schema.clone())).await.err().unwrap();
                assert!(oversized.to_string().contains("32 MiB"), "{oversized}");
                let request: SendMessagesRequest = serde_json::from_value(json!({
                    "messages": [],
                    "responseFormat": {"type": "json_schema", "jsonSchema": {"name": "response", "schema": schema}}
                })).unwrap();
                let batch = session.rpc().send_messages(request).await.err().unwrap();
                assert!(batch.to_string().contains("32 MiB"), "{batch}");
                assert!(session.rpc().queue().pending_items().await.unwrap().items.is_empty());
                for event in session.get_events().await.unwrap() {
                    assert_ne!(event.event_type, "user.message");
                    assert_ne!(event.event_type, "session.error");
                }
                assert!(ctx.exchanges().is_empty());
                session.disconnect().await.unwrap();
                client.stop().await.unwrap();
            })
        },
    ).await;
}

#[tokio::test]
async fn typed_result_after_terminal_tool_and_steering() {
    super::support::with_shared_e2e_context(
        &E2E, "structured_output", "typed_result_after_terminal_tool_and_steering", |ctx| {
            Box::pin(async move {
                let entered = Arc::new(Semaphore::new(0));
                let release = Arc::new(Semaphore::new(0));
                let calls = Arc::new(AtomicUsize::new(0));
                let (tool_entered, tool_release, tool_calls) = (entered.clone(), release.clone(), calls.clone());
                let mut tool = define_tool("lookup_number", "Return the number needed for the calculation.",
                    move |_inv, _: NoArgs| {
                        let (entered, release, calls) = (tool_entered.clone(), tool_release.clone(), tool_calls.clone());
                        async move {
                            calls.fetch_add(1, Ordering::SeqCst);
                            entered.add_permits(1);
                            tokio::time::timeout(Duration::from_secs(30), release.acquire()).await.unwrap().unwrap().forget();
                            Ok(ToolResult::Text("58".into()))
                        }
                    });
                tool.is_terminal = true;
                tool.skip_permission = true;
                let client = ctx.start_client().await;
                let session = client.create_session(config(ctx.proxy_url()).with_tools(vec![tool])).await.unwrap();
                let waiting = session.send_and_wait_typed::<ToolAnswer>(
                    "Call lookup_number exactly once, then add 5 to the returned number. Do not guess its result.");
                let steer = async {
                    tokio::time::timeout(Duration::from_secs(30), entered.acquire()).await.unwrap().unwrap().forget();
                    session.send(MessageOptions::new("Continue with the original calculation. Do not call any more tools.")
                        .with_mode(DeliveryMode::Immediate)).await.unwrap();
                    release.add_permits(1);
                };
                let (result, ()) = tokio::join!(waiting, steer);
                assert_eq!(result.unwrap(), ToolAnswer { answer: 63, contract: "typed_tool".into() });
                assert_eq!(calls.load(Ordering::SeqCst), 1);
                let exchanges = ctx.exchanges();
                assert!(exchanges.len() >= 2);
                for exchange in &exchanges[1..] {
                    assert_eq!(exchange["request"]["tool_choice"], "none");
                }
                for exchange in &exchanges {
                    assert_eq!(exchange["request"]["response_format"]["json_schema"]["schema"], schema_for::<ToolAnswer>());
                }
                session.disconnect().await.unwrap();
                client.stop().await.unwrap();
            })
        },
    ).await;
}

static E2E: super::support::SharedE2eGroup =
    super::support::SharedE2eGroup::standard("structured_output", 9);
