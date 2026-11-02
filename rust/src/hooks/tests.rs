/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use super::*;

struct TestHooks;

#[test]
fn legacy_inputs_preserve_exact_literals_and_destructuring() {
    macro_rules! legacy_input {
        ($input:ident { $($field:ident: $value:expr),* $(,)? }) => {
            let input = $input {
                session_id: "root".to_owned(),
                timestamp: 1.0,
                working_directory: PathBuf::from("work"),
                $($field: $value,)*
            };
            let $input { session_id, timestamp, working_directory, $($field,)* } = input;
            assert_eq!(session_id, "root");
            assert_eq!(timestamp, 1.0);
            assert_eq!(working_directory, PathBuf::from("work"));
            let _ = ($($field,)*);
        };
    }
    legacy_input!(PreToolUseInput {
        tool_name: "probe".to_owned(),
        tool_args: serde_json::json!({})
    });
    legacy_input!(PreMcpToolCallInput {
        server_name: "probe".to_owned(),
        tool_name: "probe".to_owned(),
        arguments: serde_json::json!({}),
        tool_call_id: None,
        meta: None
    });
    legacy_input!(PostToolUseInput {
        tool_name: "probe".to_owned(),
        tool_args: serde_json::json!({}),
        tool_result: serde_json::json!({})
    });
    legacy_input!(PostToolUseFailureInput {
        tool_name: "probe".to_owned(),
        tool_args: serde_json::json!({}),
        error: "probe".to_owned()
    });
    legacy_input!(UserPromptSubmittedInput {
        prompt: "probe".to_owned()
    });
    legacy_input!(UserPromptTransformedInput {
        prompt: "probe".to_owned(),
        transformed_prompt: "probe".to_owned()
    });
    legacy_input!(SessionStartInput {
        source: "new".to_owned(),
        initial_prompt: None
    });
    legacy_input!(SessionEndInput {
        reason: "complete".to_owned(),
        final_message: None,
        error: None
    });
    legacy_input!(ErrorOccurredInput {
        error: "probe".to_owned(),
        error_context: "model_call".to_owned(),
        recoverable: false
    });
    legacy_input!(AgentStopInput {
        stop_reason: None,
        transcript_path: None,
        stop_hook_active: None
    });
    legacy_input!(SubagentStartInput {
        transcript_path: PathBuf::from("parent.jsonl"),
        agent_name: "explore".to_owned(),
        agent_display_name: None,
        agent_description: None
    });
    legacy_input!(SubagentStopInput {
        transcript_path: PathBuf::from("parent.jsonl"),
        agent_name: "explore".to_owned(),
        agent_type: "explore".to_owned(),
        agent_id: None,
        agent_display_name: None,
        agent_description: None,
        stop_reason: "end_turn".to_owned(),
        response: "complete".to_owned()
    });
}

#[tokio::test]
async fn typed_dispatch_retains_optional_child_identity_and_old_root_inputs() {
    struct Capture(std::sync::Mutex<Option<(String, HookActor)>>);
    #[async_trait]
    impl SessionHooks for Capture {
        async fn on_hook_with_actor(&self, event: HookEvent, actor: HookActor) -> HookOutput {
            *self.0.lock().unwrap() = Some((format!("{event:?}"), actor));
            HookOutput::None
        }
    }
    let hooks = Capture(std::sync::Mutex::new(None));
    for hook in HOOK_IDENTITY_TYPES {
        for enriched in [true, false] {
            dispatch_hook(
                &hooks,
                &SessionId::new("callback-owner"),
                hook,
                hook_identity_input(hook, enriched),
            )
            .await
            .unwrap();
            let (actual, actor) = hooks.0.lock().unwrap().take().unwrap();
            assert!(
                actual.contains("reported-session") && actual.contains("callback-owner"),
                "{hook}: {actual}"
            );
            assert_eq!(
                actor.agent_id(),
                enriched.then_some("child-actor"),
                "{hook}"
            );
            assert_eq!(
                actor.agent_type(),
                (enriched || hook == "subagentStop").then_some("explore"),
                "{hook}"
            );
            assert_eq!(
                actor.parent_agent_id(),
                enriched.then_some("parent-actor"),
                "{hook}"
            );
        }
    }
}

const HOOK_IDENTITY_TYPES: [&str; 12] = [
    "preToolUse",
    "preMcpToolCall",
    "postToolUse",
    "postToolUseFailure",
    "userPromptSubmitted",
    "userPromptTransformed",
    "sessionStart",
    "sessionEnd",
    "errorOccurred",
    "agentStop",
    "subagentStart",
    "subagentStop",
];

fn hook_identity_input(hook: &str, enriched: bool) -> Value {
    let mut input = serde_json::json!({
        "sessionId": "reported-session", "timestamp": 1, "cwd": "work",
        "toolName": "probe", "toolArgs": {}, "toolResult": {}, "error": "probe",
        "serverName": "probe", "arguments": {}, "prompt": "probe",
        "transformedPrompt": "probe", "source": "new", "reason": "complete",
        "errorContext": "tool_execution", "recoverable": false,
        "transcriptPath": "parent.jsonl", "agentName": "explore",
        "agentType": "explore", "stopReason": "end_turn", "response": "complete"
    });
    if enriched {
        input["agentId"] = "child-actor".into();
        input["parentAgentId"] = "parent-actor".into();
    } else if hook != "subagentStop" {
        input.as_object_mut().unwrap().remove("agentType");
    }
    input
}

#[tokio::test]
async fn default_actor_dispatch_preserves_legacy_handlers_for_all_hook_inputs() {
    struct Legacy(std::sync::Mutex<Vec<String>>);
    #[async_trait]
    impl SessionHooks for Legacy {
        async fn on_hook(&self, event: HookEvent) -> HookOutput {
            self.0.lock().unwrap().push(format!("{event:?}"));
            HookOutput::None
        }
    }
    let legacy = Legacy(std::sync::Mutex::new(Vec::new()));
    for hook in HOOK_IDENTITY_TYPES {
        for enriched in [true, false] {
            let result = dispatch_hook(
                &legacy,
                &SessionId::new("callback-owner"),
                hook,
                hook_identity_input(hook, enriched),
            )
            .await
            .unwrap();
            assert_eq!(result, serde_json::json!({"output": {}}), "{hook}");
        }
    }
    let events = legacy.0.lock().unwrap();
    assert_eq!(events.len(), 24);
    for event in events.iter() {
        assert!(
            event.contains("reported-session") && event.contains("callback-owner"),
            "{event}"
        );
    }
}

#[tokio::test]
async fn actor_metadata_does_not_use_tool_arguments_or_null_fields() {
    struct Actorless;
    #[async_trait]
    impl SessionHooks for Actorless {
        async fn on_hook_with_actor(&self, _event: HookEvent, actor: HookActor) -> HookOutput {
            assert_eq!(actor.agent_id(), None);
            assert_eq!(actor.agent_type(), None);
            assert_eq!(actor.parent_agent_id(), None);
            HookOutput::None
        }
    }
    for null_fields in [false, true] {
        let mut input = hook_identity_input("preToolUse", false);
        input["toolArgs"] = serde_json::json!({
            "agentId": "untrusted", "agentType": "untrusted", "parentAgentId": "untrusted"
        });
        if null_fields {
            for key in ["agentId", "agentType", "parentAgentId"] {
                input[key] = Value::Null;
            }
        }
        dispatch_hook(
            &Actorless,
            &SessionId::new("callback-owner"),
            "preToolUse",
            input,
        )
        .await
        .unwrap();
    }
}

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
async fn dispatch_user_prompt_submitted_block() {
    struct BlockHook;
    #[async_trait]
    impl SessionHooks for BlockHook {
        async fn on_user_prompt_submitted(
            &self,
            _input: UserPromptSubmittedInput,
            _ctx: HookContext,
        ) -> Option<UserPromptSubmittedOutput> {
            Some(UserPromptSubmittedOutput {
                decision: Some("block".to_string()),
                reason: Some("contains a credential".to_string()),
                ..Default::default()
            })
        }
    }

    let input = serde_json::json!({
        "sessionId": "sess-1",
        "timestamp": 1234567890,
        "cwd": "/tmp",
        "prompt": "hello"
    });
    let result = dispatch_hook(
        &BlockHook,
        &SessionId::new("sess-1"),
        "userPromptSubmitted",
        input,
    )
    .await
    .unwrap();
    assert_eq!(
        result,
        serde_json::json!({
            "output": { "decision": "block", "reason": "contains a credential" }
        })
    );
}

/// An answer that does not block leaves both fields off the wire.
#[tokio::test]
async fn dispatch_user_prompt_submitted_omits_unset_block_fields() {
    let input = serde_json::json!({
        "sessionId": "sess-1",
        "timestamp": 1234567890,
        "cwd": "/tmp",
        "prompt": "hello world"
    });
    let result = dispatch_hook(
        &TestHooks,
        &SessionId::new("sess-1"),
        "userPromptSubmitted",
        input,
    )
    .await
    .unwrap();
    assert_eq!(
        result,
        serde_json::json!({ "output": { "modifiedPrompt": "[prefixed] hello world" } })
    );
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

/// The object-shaped `error` the CLI sends reaches the hook as its `message`.
#[tokio::test]
async fn dispatch_error_occurred_with_the_clis_error_object() {
    struct Recorder(std::sync::Mutex<Option<ErrorOccurredInput>>);
    #[async_trait]
    impl SessionHooks for Recorder {
        async fn on_error_occurred(
            &self,
            input: ErrorOccurredInput,
            _ctx: HookContext,
        ) -> Option<ErrorOccurredOutput> {
            *self.0.lock().unwrap() = Some(input);
            None
        }
    }

    let hooks = Recorder(std::sync::Mutex::new(None));
    let input = serde_json::json!({
        "sessionId": "sess-1",
        "error": {"name": "Error", "message": "model timeout", "stack": "Error: model timeout"},
        "errorContext": "model_call",
        "recoverable": true,
        "timestamp": 1790464212617_u64,
        "cwd": "/tmp"
    });
    let result = dispatch_hook(&hooks, &SessionId::new("sess-1"), "errorOccurred", input)
        .await
        .unwrap();
    assert_eq!(result["output"], serde_json::json!({}));
    let seen = hooks.0.lock().unwrap().take().expect("hook delivered");
    assert_eq!(seen.error, "model timeout");
    assert_eq!(seen.error_context, "model_call");
}

#[test]
fn session_end_error_decodes_from_object_string_null_and_absent() {
    let base = serde_json::json!({
        "reason": "error",
        "sessionId": "sess-1",
        "timestamp": 1790464235720_u64,
        "cwd": "/tmp"
    });
    let with = |error: Option<Value>| {
        let mut value = base.clone();
        if let Some(error) = error {
            value["error"] = error;
        }
        serde_json::from_value::<SessionEndInput>(value)
            .unwrap()
            .error
    };
    assert_eq!(
        with(Some(serde_json::json!({"message": "retries exhausted"}))).as_deref(),
        Some("retries exhausted")
    );
    assert_eq!(
        with(Some(Value::String("boom".to_string()))).as_deref(),
        Some("boom")
    );
    assert_eq!(with(Some(Value::Null)), None);
    assert_eq!(with(None), None);
    assert_eq!(
        with(Some(serde_json::json!({"code": 7}))).as_deref(),
        Some(r#"{"code":7}"#)
    );
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
