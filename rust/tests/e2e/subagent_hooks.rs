use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::hooks::{
    HookContext, PostToolUseInput, PostToolUseOutput, PreToolUseInput, PreToolUseOutput,
    SessionHooks,
};
use github_copilot_sdk::{
    CopilotHttpRequest, CopilotHttpResponse, CopilotRequestContext, CopilotRequestError,
    CopilotRequestHandler, forward_http,
};
use parking_lot::Mutex;

use super::support::with_e2e_context;

#[tokio::test]
async fn should_invoke_pretooluse_and_posttooluse_hooks_for_sub_agent_tool_calls() {
    if super::support::skip_inprocess("LLM inference providers are process-global in-process") {
        return;
    }
    with_e2e_context(
        "subagent_hooks",
        "should_invoke_pretooluse_and_posttooluse_hooks_for_sub_agent_tool_calls",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                std::fs::write(
                    ctx.work_dir().join("subagent-test.txt"),
                    "Hello from subagent test!",
                )
                .expect("write test file");

                let hook_log = Arc::new(Mutex::new(Vec::<HookEntry>::new()));
                let request_log = Arc::new(RecordingRequestHandler::default());

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
                        },
                    )))
                    .await
                    .expect("create session");

                session
                    .send_and_wait(
                        "Use the task tool to spawn an explore agent that reads the file \
                         subagent-test.txt in the current directory and reports its contents. \
                         You must use the task tool.",
                    )
                    .await
                    .expect("send");

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
                assert_subagent_request_metadata(&request_log.inference_records());

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
}

#[async_trait]
impl SessionHooks for RecordingHooks {
    async fn on_pre_tool_use(
        &self,
        input: PreToolUseInput,
        _ctx: HookContext,
    ) -> Option<PreToolUseOutput> {
        self.log.lock().push(HookEntry {
            kind: "pre".to_string(),
            tool_name: input.tool_name,
            session_id: input.session_id,
        });
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
