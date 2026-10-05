/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use super::*;

struct TestHooks;

#[async_trait]
impl SessionHooks for TestHooks {
    async fn on_hook(&self, event: HookEvent) -> HookOutput {
        match event {
            HookEvent::PreToolUse { input, .. } => {
                if input.tool_name == "dangerous_tool" {
                    HookOutput::PreToolUse(PreToolUseOutput {
                        permission_decision: Some("deny".to_string()),
                        permission_decision_reason: Some("blocked by policy".to_string()),
                        ..Default::default()
                    })
                } else {
                    HookOutput::None
                }
            }
            HookEvent::UserPromptSubmitted { input, .. } => {
                HookOutput::UserPromptSubmitted(UserPromptSubmittedOutput {
                    modified_prompt: Some(format!("[prefixed] {}", input.prompt)),
                    ..Default::default()
                })
            }
            HookEvent::UserPromptTransformed { input, .. } => {
                HookOutput::UserPromptTransformed(UserPromptTransformedOutput {
                    modified_transformed_prompt: Some(format!(
                        "[transformed] {}",
                        input.transformed_prompt
                    )),
                })
            }
            _ => HookOutput::None,
        }
    }
}

#[tokio::test]
async fn dispatch_pre_tool_use_deny() {
    let hooks = TestHooks;
    let input = serde_json::json!({
        "sessionId": "sess-1",
        "timestamp": 1234567890,
        "cwd": "/tmp",
        "toolName": "dangerous_tool",
        "toolArgs": {}
    });
    let result = dispatch_hook(&hooks, &SessionId::new("sess-1"), "preToolUse", input)
        .await
        .unwrap();
    let output = &result["output"];
    assert_eq!(output["permissionDecision"], "deny");
    assert_eq!(output["permissionDecisionReason"], "blocked by policy");
}

#[tokio::test]
async fn dispatch_pre_tool_use_passthrough() {
    let hooks = TestHooks;
    let input = serde_json::json!({
        "sessionId": "sess-1",
        "timestamp": 1234567890,
        "cwd": "/tmp",
        "toolName": "safe_tool",
        "toolArgs": {"key": "value"}
    });
    let result = dispatch_hook(&hooks, &SessionId::new("sess-1"), "preToolUse", input)
        .await
        .unwrap();
    // No hook registered for this tool — output should be empty object
    assert_eq!(result["output"], serde_json::json!({}));
}

#[tokio::test]
async fn dispatch_user_prompt_submitted() {
    let hooks = TestHooks;
    let input = serde_json::json!({
        "sessionId": "sess-1",
        "timestamp": 1234567890,
        "cwd": "/tmp",
        "prompt": "hello world"
    });
    let result = dispatch_hook(
        &hooks,
        &SessionId::new("sess-1"),
        "userPromptSubmitted",
        input,
    )
    .await
    .unwrap();
    assert_eq!(result["output"]["modifiedPrompt"], "[prefixed] hello world");
}

#[tokio::test]
async fn dispatch_user_prompt_transformed() {
    let hooks = TestHooks;
    let input = serde_json::json!({
        "sessionId": "sess-1",
        "timestamp": 1234567890,
        "cwd": "/tmp",
        "prompt": "hello world",
        "transformedPrompt": "<current_datetime>now</current_datetime>\nhello world"
    });
    let result = dispatch_hook(
        &hooks,
        &SessionId::new("sess-1"),
        "userPromptTransformed",
        input,
    )
    .await
    .unwrap();
    assert_eq!(
        result["output"]["modifiedTransformedPrompt"],
        "[transformed] <current_datetime>now</current_datetime>\nhello world"
    );
}

#[tokio::test]
async fn dispatch_unregistered_hook_returns_empty() {
    let hooks = TestHooks;
    let input = serde_json::json!({
        "sessionId": "sess-1",
        "timestamp": 1234567890,
        "cwd": "/tmp",
        "reason": "complete"
    });
    // TestHooks doesn't handle SessionEnd
    let result = dispatch_hook(&hooks, &SessionId::new("sess-1"), "sessionEnd", input)
        .await
        .unwrap();
    assert_eq!(result["output"], serde_json::json!({}));
}

#[tokio::test]
async fn dispatch_unknown_hook_type() {
    let hooks = TestHooks;
    let input = serde_json::json!({});
    let result = dispatch_hook(&hooks, &SessionId::new("sess-1"), "unknownHook", input)
        .await
        .unwrap();
    assert_eq!(result["output"], serde_json::json!({}));
}

#[tokio::test]
async fn dispatch_subagent_hooks_with_typed_input_and_output() {
    struct SubagentHooks;
    #[async_trait]
    impl SessionHooks for SubagentHooks {
        async fn on_subagent_start(
            &self,
            input: SubagentStartInput,
            ctx: HookContext,
        ) -> Option<SubagentStartOutput> {
            assert_eq!(ctx.session_id, SessionId::new("sess-1"));
            assert_eq!(input.session_id, "sess-1");
            assert_eq!(input.timestamp, 1234567890.0);
            assert_eq!(input.working_directory, PathBuf::from("/tmp"));
            assert_eq!(
                input.transcript_path,
                PathBuf::from("/tmp/transcript.jsonl")
            );
            assert_eq!(input.agent_name, "explore");
            assert_eq!(input.agent_display_name.as_deref(), Some("Explore Agent"));
            assert_eq!(input.agent_description.as_deref(), Some("Read code"));
            Some(SubagentStartOutput {
                additional_context: Some("Follow the file".to_string()),
            })
        }

        async fn on_subagent_stop(
            &self,
            input: SubagentStopInput,
            ctx: HookContext,
        ) -> Option<SubagentStopOutput> {
            assert_eq!(ctx.session_id, SessionId::new("sess-1"));
            assert_eq!(input.session_id, "sess-1");
            assert_eq!(input.timestamp, 1234567891.0);
            assert_eq!(input.working_directory, PathBuf::from("/tmp"));
            assert_eq!(
                input.transcript_path,
                PathBuf::from("/tmp/transcript.jsonl")
            );
            assert_eq!(input.agent_name, "explore");
            assert_eq!(input.agent_type, "explore");
            assert_eq!(input.agent_id.as_deref(), Some("read-file"));
            assert_eq!(input.agent_display_name.as_deref(), Some("Explore Agent"));
            assert_eq!(input.agent_description.as_deref(), Some("Read code"));
            assert_eq!(input.stop_reason, "end_turn");
            assert_eq!(input.response, "original answer");
            Some(SubagentStopOutput {
                modified_response: Some("rewritten answer".to_string()),
                ..Default::default()
            })
        }
    }

    let common = serde_json::json!({
        "sessionId": "sess-1",
        "timestamp": 1234567890,
        "cwd": "/tmp",
        "transcriptPath": "/tmp/transcript.jsonl",
        "agentName": "explore",
        "agentDisplayName": "Explore Agent",
        "agentDescription": "Read code"
    });
    let start = dispatch_hook(
        &SubagentHooks,
        &SessionId::new("sess-1"),
        "subagentStart",
        common.clone(),
    )
    .await
    .unwrap();
    assert_eq!(
        start,
        serde_json::json!({ "output": { "additionalContext": "Follow the file" } })
    );

    let mut stop = common;
    let object = stop.as_object_mut().unwrap();
    object.insert("timestamp".to_string(), serde_json::json!(1234567891));
    object.insert("agentType".to_string(), serde_json::json!("explore"));
    object.insert("agentId".to_string(), serde_json::json!("read-file"));
    object.insert("stopReason".to_string(), serde_json::json!("end_turn"));
    object.insert("response".to_string(), serde_json::json!("original answer"));
    let stopped = dispatch_hook(
        &SubagentHooks,
        &SessionId::new("sess-1"),
        "subagentStop",
        stop,
    )
    .await
    .unwrap();
    assert_eq!(
        stopped,
        serde_json::json!({ "output": { "modifiedResponse": "rewritten answer" } })
    );
}

#[tokio::test]
async fn dispatch_subagent_stop_block_output() {
    struct BlockHook;
    #[async_trait]
    impl SessionHooks for BlockHook {
        async fn on_subagent_stop(
            &self,
            _input: SubagentStopInput,
            _ctx: HookContext,
        ) -> Option<SubagentStopOutput> {
            Some(SubagentStopOutput {
                decision: Some("block".to_string()),
                reason: Some("Keep researching".to_string()),
                ..Default::default()
            })
        }
    }

    let input = serde_json::json!({
        "sessionId": "sess-1",
        "timestamp": 1234567890,
        "cwd": "/tmp",
        "transcriptPath": "/tmp/transcript.jsonl",
        "agentName": "explore",
        "agentType": "explore",
        "stopReason": "end_turn",
        "response": "draft answer"
    });
    let result = dispatch_hook(&BlockHook, &SessionId::new("sess-1"), "subagentStop", input)
        .await
        .unwrap();
    assert_eq!(
        result,
        serde_json::json!({ "output": { "decision": "block", "reason": "Keep researching" } })
    );
}

#[tokio::test]
async fn dispatch_mismatched_output_returns_empty() {
    struct MismatchHooks;
    #[async_trait]
    impl SessionHooks for MismatchHooks {
        async fn on_hook(&self, _event: HookEvent) -> HookOutput {
            // Always return SessionEnd output regardless of event type
            HookOutput::SessionEnd(SessionEndOutput {
                session_summary: Some("oops".to_string()),
                ..Default::default()
            })
        }
    }

    let hooks = MismatchHooks;
    let input = serde_json::json!({
        "sessionId": "sess-1",
        "timestamp": 1234567890,
        "cwd": "/tmp",
        "toolName": "some_tool",
        "toolArgs": {}
    });
    // preToolUse event gets a SessionEnd output — should be treated as empty
    let result = dispatch_hook(&hooks, &SessionId::new("sess-1"), "preToolUse", input)
        .await
        .unwrap();
    assert_eq!(result["output"], serde_json::json!({}));
}

#[tokio::test]
async fn dispatch_post_tool_use_default() {
    let hooks = TestHooks;
    let input = serde_json::json!({
        "sessionId": "sess-1",
        "timestamp": 1234567890,
        "cwd": "/tmp",
        "toolName": "some_tool",
        "toolArgs": {},
        "toolResult": "success"
    });
    let result = dispatch_hook(&hooks, &SessionId::new("sess-1"), "postToolUse", input)
        .await
        .unwrap();
    assert_eq!(result["output"], serde_json::json!({}));
}

#[tokio::test]
async fn dispatch_post_tool_use_failure_default() {
    // No handler override — should return an empty output object.
    let hooks = TestHooks;
    let input = serde_json::json!({
        "sessionId": "sess-1",
        "timestamp": 1234567890,
        "cwd": "/tmp",
        "toolName": "some_tool",
        "toolArgs": {"key": "value"},
        "error": "boom"
    });
    let result = dispatch_hook(
        &hooks,
        &SessionId::new("sess-1"),
        "postToolUseFailure",
        input,
    )
    .await
    .unwrap();
    assert_eq!(result["output"], serde_json::json!({}));
}

#[tokio::test]
async fn dispatch_post_tool_use_failure_returns_additional_context() {
    struct FailureHooks;
    #[async_trait]
    impl SessionHooks for FailureHooks {
        async fn on_post_tool_use_failure(
            &self,
            input: PostToolUseFailureInput,
            _ctx: HookContext,
        ) -> Option<PostToolUseFailureOutput> {
            assert_eq!(input.session_id, "sess-1");
            assert_eq!(input.tool_name, "some_tool");
            assert_eq!(input.error, "boom");
            assert_eq!(input.working_directory, PathBuf::from("/tmp"));
            Some(PostToolUseFailureOutput {
                additional_context: Some(format!(
                    "tool {} failed: {}",
                    input.tool_name, input.error
                )),
            })
        }
    }

    let input = serde_json::json!({
        "sessionId": "sess-1",
        "timestamp": 1234567890,
        "cwd": "/tmp",
        "toolName": "some_tool",
        "toolArgs": {},
        "error": "boom"
    });
    let result = dispatch_hook(
        &FailureHooks,
        &SessionId::new("sess-1"),
        "postToolUseFailure",
        input,
    )
    .await
    .unwrap();
    assert_eq!(
        result["output"]["additionalContext"],
        "tool some_tool failed: boom"
    );
}

#[tokio::test]
async fn dispatch_post_tool_use_failure_invalid_input_errors() {
    // Missing required `error` field — dispatcher should surface the
    // deserialization error rather than dispatching with empty input.
    let hooks = TestHooks;
    let input = serde_json::json!({
        "sessionId": "sess-1",
        "timestamp": 1234567890,
        "cwd": "/tmp",
        "toolName": "some_tool",
        "toolArgs": {}
    });
    let err = dispatch_hook(
        &hooks,
        &SessionId::new("sess-1"),
        "postToolUseFailure",
        input,
    )
    .await
    .unwrap_err();
    let msg = err.to_string().to_ascii_lowercase();
    assert!(
        msg.contains("error") || msg.contains("missing field"),
        "unexpected error: {msg}"
    );
}

#[tokio::test]
async fn dispatch_session_start() {
    struct StartHooks;
    #[async_trait]
    impl SessionHooks for StartHooks {
        async fn on_hook(&self, event: HookEvent) -> HookOutput {
            match event {
                HookEvent::SessionStart { .. } => HookOutput::SessionStart(SessionStartOutput {
                    additional_context: Some("extra context".to_string()),
                    ..Default::default()
                }),
                _ => HookOutput::None,
            }
        }
    }

    let hooks = StartHooks;
    let input = serde_json::json!({
        "sessionId": "sess-1",
        "timestamp": 1234567890,
        "cwd": "/tmp",
        "source": "new"
    });
    let result = dispatch_hook(&hooks, &SessionId::new("sess-1"), "sessionStart", input)
        .await
        .unwrap();
    assert_eq!(result["output"]["additionalContext"], "extra context");
}

#[tokio::test]
async fn dispatch_error_occurred() {
    struct ErrorHooks;
    #[async_trait]
    impl SessionHooks for ErrorHooks {
        async fn on_hook(&self, event: HookEvent) -> HookOutput {
            match event {
                HookEvent::ErrorOccurred { .. } => HookOutput::ErrorOccurred(ErrorOccurredOutput {
                    error_handling: Some("retry".to_string()),
                    retry_count: Some(3),
                    ..Default::default()
                }),
                _ => HookOutput::None,
            }
        }
    }

    let hooks = ErrorHooks;
    let input = serde_json::json!({
        "sessionId": "sess-1",
        "timestamp": 1234567890,
        "cwd": "/tmp",
        "error": "model timeout",
        "errorContext": "model_call",
        "recoverable": true
    });
    let result = dispatch_hook(&hooks, &SessionId::new("sess-1"), "errorOccurred", input)
        .await
        .unwrap();
    assert_eq!(result["output"]["errorHandling"], "retry");
    assert_eq!(result["output"]["retryCount"], 3);
}

#[tokio::test]
async fn dispatch_agent_stop_block() {
    struct AgentStopHooks;
    #[async_trait]
    impl SessionHooks for AgentStopHooks {
        async fn on_agent_stop(
            &self,
            input: AgentStopInput,
            ctx: HookContext,
        ) -> Option<AgentStopOutput> {
            assert_eq!(ctx.session_id, SessionId::new("sess-1"));
            assert_eq!(input.session_id, "sess-1");
            assert_eq!(input.stop_reason.as_deref(), Some("end_turn"));
            assert_eq!(
                input.transcript_path,
                Some(PathBuf::from("/tmp/transcript.jsonl"))
            );
            assert_eq!(input.stop_hook_active, Some(true));
            Some(AgentStopOutput {
                decision: Some("block".to_string()),
                reason: Some("finish the remaining work".to_string()),
            })
        }
    }

    let input = serde_json::json!({
        "sessionId": "sess-1",
        "timestamp": 1234567890,
        "cwd": "/tmp",
        "stopReason": "end_turn",
        "transcriptPath": "/tmp/transcript.jsonl",
        "stop_hook_active": true
    });
    let result = dispatch_hook(
        &AgentStopHooks,
        &SessionId::new("sess-1"),
        "agentStop",
        input,
    )
    .await
    .unwrap();

    assert_eq!(result["output"]["decision"], "block");
    assert_eq!(result["output"]["reason"], "finish the remaining work");
}
