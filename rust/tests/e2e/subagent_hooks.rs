use std::cell::Cell;
use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::hooks::{
    HookContext, PostToolUseInput, PostToolUseOutput, PreToolUseInput, PreToolUseOutput,
    SessionHooks, SubagentStartInput, SubagentStartOutput, SubagentStopInput, SubagentStopOutput,
};
use github_copilot_sdk::session_events::SessionEventType;
use github_copilot_sdk::{
    CopilotHttpRequest, CopilotHttpResponse, CopilotRequestContext, CopilotRequestError,
    CopilotRequestHandler, forward_http,
};
use parking_lot::Mutex;
use tokio::sync::watch;

use super::support::{assistant_message_content, wait_for_event, with_e2e_context};

const CHILD_CONTEXT: &str = "Subagent start hook verified: read the requested file.";
const STOP_RESPONSE_PREFIX: &str = "Subagent stop hook verified: ";

#[tokio::test]
async fn should_apply_subagent_lifecycle_hook_outputs() {
    if super::support::skip_inprocess("LLM inference providers are process-global in-process") {
        return;
    }
    with_e2e_context(
        "subagent_hooks",
        "should_apply_subagent_lifecycle_hook_outputs",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                std::fs::write(
                    ctx.work_dir().join("subagent-test.txt"),
                    "Hello from subagent test!",
                )
                .expect("write test file");

                let hook_log = Arc::new(Mutex::new(Vec::<HookEntry>::new()));
                let subagent_starts = Arc::new(Mutex::new(Vec::new()));
                let subagent_stops = Arc::new(Mutex::new(Vec::new()));
                let request_log = Arc::new(RecordingRequestHandler::default());
                let waiting_text = "I've launched an explore agent to read subagent-test.txt. Waiting for it to complete...";
                let final_text = "The explore agent successfully read the file. The contents of **subagent-test.txt** are:\n\n```\nHello from subagent test!\n```";
                let (parent_reply, parent_reply_observed) = watch::channel(false);

                let client = ctx
                    .start_llm_client(
                        Arc::clone(&request_log),
                        &[("COPILOT_EXP_COPILOT_CLI_SESSION_BASED_SUBAGENTS", "true")],
                    )
                    .await;

                let session = client
                    .create_session(ctx.approve_all_session_config().with_hooks(Arc::new(
                        RecordingHooks {
                            log: Arc::clone(&hook_log),
                            parent_reply_observed,
                            subagent_starts: Arc::clone(&subagent_starts),
                            subagent_stops: Arc::clone(&subagent_stops),
                        },
                    )))
                    .await
                    .expect("create session");

                let saw_final_response = Cell::new(false);
                let completion = wait_for_event(
                    session.subscribe(),
                    "parent waiting reply and subagent result followed by session.idle",
                    |event| {
                        if !event.agent_id.as_deref().is_none_or(str::is_empty) {
                            return false;
                        }
                        if event.parsed_type() == SessionEventType::AssistantMessage {
                            let content = assistant_message_content(event);
                            if content == waiting_text {
                                parent_reply
                                    .send(true)
                                    .expect("sub-agent hook should await the parent reply");
                            }
                            if content == final_text {
                                saw_final_response.set(true);
                            }
                        }
                        event.parsed_type() == SessionEventType::SessionIdle
                            && saw_final_response.get()
                    },
                );
                let (send_result, _) = tokio::join!(
                    session.send_and_wait(
                        "Use the task tool to spawn an explore agent that reads the file \
                         subagent-test.txt in the current directory and reports its contents. \
                         You must use the task tool.",
                    ),
                    completion,
                );
                send_result.expect("send");
                let history = session.get_events().await.expect("get durable history");
                let replies: Vec<_> = history.iter()
                    .filter(|event| event.agent_id.as_deref().is_none_or(str::is_empty)
                        && event.parsed_type() == SessionEventType::AssistantMessage)
                    .map(assistant_message_content)
                    .filter(|content| *content == waiting_text || *content == final_text)
                    .collect();
                assert_eq!(replies, [waiting_text, final_text],
                    "durable history must contain the waiting reply before the final reply");

                let log = hook_log.lock().clone();

                // Parent tool hooks fire for "task"
                let task_pre = log
                    .iter()
                    .find(|h| h.kind == "pre" && h.tool_name == "task");
                assert!(
                    task_pre.is_some(),
                    "preToolUse should fire for the parent's 'task' tool call"
                );

                // Sub-agent tool hooks fire for "view"
                let view_pre: Vec<_> = log
                    .iter()
                    .filter(|h| h.kind == "pre" && h.tool_name == "view")
                    .collect();
                let view_post: Vec<_> = log
                    .iter()
                    .filter(|h| h.kind == "post" && h.tool_name == "view")
                    .collect();
                assert!(
                    !view_pre.is_empty(),
                    "preToolUse should fire for the sub-agent's 'view' tool call"
                );
                assert!(
                    !view_post.is_empty(),
                    "postToolUse should fire for the sub-agent's 'view' tool call"
                );

                // input.session_id distinguishes parent from sub-agent
                assert_ne!(
                    view_pre[0].session_id,
                    task_pre.unwrap().session_id,
                    "Sub-agent tool hooks should have a different sessionId than parent tool hooks"
                );
                let requests = request_log.inference_records();
                assert_subagent_request_metadata(&requests);
                let context_and_task = format!("{CHILD_CONTEXT}\n\nRead the file \"subagent-test.txt\"");
                let prompt_has_context = |prompt: &str| prompt.contains(context_and_task.as_str());
                assert!(
                    requests
                        .iter()
                        .filter(|request| request.parent_agent_id.is_some())
                        .any(|request| {
                            let body: serde_json::Value = serde_json::from_str(&request.body)
                                .expect("child inference request body");
                            body["messages"].as_array().is_some_and(|messages| {
                                messages.iter().any(|message| {
                                    message["role"] == "user"
                                        && (message["content"]
                                            .as_str()
                                            .is_some_and(prompt_has_context)
                                            || message["content"].as_array().is_some_and(|parts| {
                                                parts.iter().any(|part| {
                                                    part["type"] == "text"
                                                        && part["text"]
                                                            .as_str()
                                                            .is_some_and(prompt_has_context)
                                                })
                                            }))
                                })
                            })
                        }),
                    "start hook context should be prepended to the child inference prompt"
                );
                assert!(
                    requests.iter().any(|request| {
                        request.parent_agent_id.is_none()
                            && request.body.contains(STOP_RESPONSE_PREFIX)
                    }),
                    "rewritten stop response should reach a parent inference request"
                );

                {
                    let starts = subagent_starts.lock();
                    assert_eq!(starts.len(), 1, "one subagentStart per launched child");
                    let start = &starts[0];
                    assert_eq!(start.session_id, session.id().as_str());
                    assert!(start.timestamp > 0.0);
                    assert_eq!(
                        start
                            .working_directory
                            .canonicalize()
                            .expect("canonical hook working directory"),
                        ctx.work_dir().canonicalize().expect("canonical test directory")
                    );
                    assert_eq!(start.agent_name, "explore");
                    assert_eq!(start.agent_display_name, None);
                    assert_eq!(start.agent_description, None);

                    let stops = subagent_stops.lock();
                    assert_eq!(stops.len(), 1, "one subagentStop per completed child");
                    let stop = &stops[0];
                    assert_eq!(stop.session_id, start.session_id);
                    assert!(stop.timestamp >= start.timestamp);
                    assert_eq!(stop.working_directory, start.working_directory);
                    assert_eq!(stop.transcript_path, start.transcript_path);
                    assert_eq!(stop.agent_name, start.agent_name);
                    assert_eq!(stop.agent_type, "explore");
                    assert!(stop.agent_id.as_ref().is_some_and(|id| !id.is_empty()));
                    assert_eq!(stop.agent_display_name, start.agent_display_name);
                    assert_eq!(stop.agent_description, start.agent_description);
                    assert_eq!(stop.stop_reason, "end_turn");
                    assert!(stop.response.contains("Hello from subagent test!"));
                }

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[derive(Clone, Debug)]
struct HookEntry {
    kind: String,
    tool_name: String,
    session_id: String,
}

#[derive(Clone, Debug)]
struct RequestEntry {
    url: String,
    agent_id: Option<String>,
    parent_agent_id: Option<String>,
    interaction_type: Option<String>,
    body: String,
}

#[derive(Default)]
struct RecordingRequestHandler {
    log: Mutex<Vec<RequestEntry>>,
}

impl RecordingRequestHandler {
    fn inference_records(&self) -> Vec<RequestEntry> {
        self.log
            .lock()
            .iter()
            .filter(|entry| is_inference_url(&entry.url))
            .cloned()
            .collect()
    }
}

#[async_trait]
impl CopilotRequestHandler for RecordingRequestHandler {
    async fn send_request(
        &self,
        request: CopilotHttpRequest,
        ctx: &CopilotRequestContext,
    ) -> Result<CopilotHttpResponse, CopilotRequestError> {
        self.log.lock().push(RequestEntry {
            url: request.url.clone(),
            agent_id: ctx.agent_id.clone(),
            parent_agent_id: ctx.parent_agent_id.clone(),
            interaction_type: ctx.interaction_type.clone(),
            body: if is_inference_url(&request.url) {
                std::str::from_utf8(&request.body)
                    .expect("inference request body is UTF-8")
                    .to_owned()
            } else {
                String::new()
            },
        });
        forward_http(request).await
    }
}

fn is_inference_url(url: &str) -> bool {
    let url = url.to_lowercase();
    url.ends_with("/chat/completions")
        || url.ends_with("/responses")
        || url.ends_with("/v1/messages")
        || url.ends_with("/messages")
}

fn assert_subagent_request_metadata(records: &[RequestEntry]) {
    assert!(
        !records.is_empty(),
        "request handler should observe inference requests"
    );
    let subagent_request = records
        .iter()
        .find(|entry| {
            entry
                .parent_agent_id
                .as_deref()
                .is_some_and(|id| !id.is_empty())
        })
        .expect("sub-agent inference request should carry a parentAgentId");
    assert!(
        subagent_request
            .agent_id
            .as_deref()
            .is_some_and(|id| !id.is_empty()),
        "sub-agent inference request should carry an agentId"
    );
    assert!(
        subagent_request
            .interaction_type
            .as_deref()
            .is_some_and(|kind| !kind.is_empty()),
        "sub-agent inference request should carry an interactionType"
    );
    assert_ne!(
        subagent_request.parent_agent_id.as_deref(),
        subagent_request.agent_id.as_deref(),
        "sub-agent inference request should have distinct parent and child agent ids"
    );
}

struct RecordingHooks {
    log: Arc<Mutex<Vec<HookEntry>>>,
    parent_reply_observed: watch::Receiver<bool>,
    subagent_starts: Arc<Mutex<Vec<SubagentStartInput>>>,
    subagent_stops: Arc<Mutex<Vec<SubagentStopInput>>>,
}

fn is_subagent_view(tool_name: &str, session_id: &str, log: &[HookEntry]) -> bool {
    tool_name == "view"
        && log
            .iter()
            .find(|entry| entry.kind == "pre" && entry.tool_name == "task")
            .is_some_and(|entry| entry.session_id != session_id)
}

#[test]
fn only_child_view_waits_for_parent_reply() {
    let parent_task = [HookEntry {
        kind: "pre".to_string(),
        tool_name: "task".to_string(),
        session_id: "parent".to_string(),
    }];
    assert!(!is_subagent_view("view", "parent", &[]));
    assert!(!is_subagent_view("view", "parent", &parent_task));
    assert!(!is_subagent_view("task", "child", &parent_task));
    assert!(is_subagent_view("view", "child", &parent_task));
}

#[async_trait]
impl SessionHooks for RecordingHooks {
    async fn on_subagent_start(
        &self,
        input: SubagentStartInput,
        _ctx: HookContext,
    ) -> Option<SubagentStartOutput> {
        self.subagent_starts.lock().push(input);
        Some(SubagentStartOutput {
            additional_context: Some(CHILD_CONTEXT.to_string()),
        })
    }

    async fn on_subagent_stop(
        &self,
        input: SubagentStopInput,
        _ctx: HookContext,
    ) -> Option<SubagentStopOutput> {
        let response = format!("{STOP_RESPONSE_PREFIX}{}", input.response);
        self.subagent_stops.lock().push(input);
        Some(SubagentStopOutput {
            modified_response: Some(response),
            ..SubagentStopOutput::default()
        })
    }

    async fn on_pre_tool_use(
        &self,
        input: PreToolUseInput,
        _ctx: HookContext,
    ) -> Option<PreToolUseOutput> {
        let is_subagent_view = {
            let mut log = self.log.lock();
            let is_subagent_view = is_subagent_view(&input.tool_name, &input.session_id, &log);
            log.push(HookEntry {
                kind: "pre".to_string(),
                tool_name: input.tool_name,
                session_id: input.session_id,
            });
            is_subagent_view
        };
        if is_subagent_view {
            // Keep the background agent from completing before the parent's waiting reply.
            let mut parent_reply = self.parent_reply_observed.clone();
            parent_reply
                .wait_for(|observed| *observed)
                .await
                .expect("parent waiting reply should arrive before sub-agent view");
        }
        Some(PreToolUseOutput {
            permission_decision: Some("allow".to_string()),
            ..PreToolUseOutput::default()
        })
    }

    async fn on_post_tool_use(
        &self,
        input: PostToolUseInput,
        _ctx: HookContext,
    ) -> Option<PostToolUseOutput> {
        self.log.lock().push(HookEntry {
            kind: "post".to_string(),
            tool_name: input.tool_name,
            session_id: input.session_id,
        });
        None
    }
}
