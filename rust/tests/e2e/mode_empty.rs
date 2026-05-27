//! E2E coverage for `ClientMode::Empty` + `ToolSet` patterns.
//!
//! The runtime is mode-agnostic — these tests verify the SDK's
//! translation reaches the runtime correctly by inspecting the
//! resulting CapiProxy chat-completion request (the LLM only sees
//! tools the runtime exposed for the session) and end-to-end behavior.
//!
//! Mirrors `nodejs/test/e2e/mode_empty.e2e.test.ts` and shares the
//! same recorded cassettes under `test/snapshots/mode_empty/`.

use std::sync::Arc;

use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::types::SystemMessageConfig;
use github_copilot_sdk::{
    BUILTIN_TOOLS_ISOLATED, Client, ClientMode, SessionConfig, ToolSet,
};
use serde_json::Value;

use super::support::{assistant_message_content, with_e2e_context};

const SHELL_TOOL_NAME: &str = if cfg!(windows) { "powershell" } else { "bash" };

fn isolated_tool_set() -> Vec<String> {
    ToolSet::new()
        .add_builtin_many(BUILTIN_TOOLS_ISOLATED.iter().copied())
        .expect("isolated tool set should be valid")
        .into()
}

fn star_builtin_tool_set() -> Vec<String> {
    ToolSet::new()
        .add_builtin("*")
        .expect("builtin wildcard should be valid")
        .into()
}

fn tool_names_from_request(exchange: &Value) -> Vec<String> {
    let Some(tools) = exchange
        .get("request")
        .and_then(|r| r.get("tools"))
        .and_then(|t| t.as_array())
    else {
        return Vec::new();
    };
    tools
        .iter()
        .filter_map(|t| {
            let type_ok = t.get("type").and_then(Value::as_str) == Some("function");
            if !type_ok {
                return None;
            }
            t.get("function")
                .and_then(|f| f.get("name"))
                .and_then(Value::as_str)
                .map(str::to_owned)
        })
        .collect()
}

fn system_message_from_request(exchange: &Value) -> String {
    let Some(messages) = exchange
        .get("request")
        .and_then(|r| r.get("messages"))
        .and_then(|m| m.as_array())
    else {
        return String::new();
    };
    for m in messages {
        if m.get("role").and_then(Value::as_str) != Some("system") {
            continue;
        }
        let content = m.get("content");
        if let Some(text) = content.and_then(Value::as_str) {
            return text.to_owned();
        }
        if let Some(parts) = content.and_then(Value::as_array) {
            return parts
                .iter()
                .filter_map(|p| p.get("text").and_then(Value::as_str))
                .collect::<Vec<_>>()
                .join("\n");
        }
    }
    String::new()
}

#[tokio::test]
async fn empty_mode_isolated_set_shell_tool_is_not_exposed() {
    with_e2e_context(
        "mode_empty",
        "empty_mode_isolated_set_shell_tool_is_not_exposed",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let options = ctx
                    .client_options()
                    .with_mode(ClientMode::Empty)
                    .with_base_directory(ctx.work_dir().to_path_buf());
                let client = Client::start(options).await.expect("start client");
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_permission_handler(Arc::new(ApproveAllHandler))
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                            .with_available_tools(isolated_tool_set()),
                    )
                    .await
                    .expect("create session");

                let _ = session.send_and_wait("Say hi.").await;

                let exchanges = ctx.exchanges();
                assert!(!exchanges.is_empty(), "expected at least one exchange");
                let tool_names = tool_names_from_request(exchanges.last().unwrap());
                for banned in ["bash", "powershell", "edit", "grep", "web_fetch"] {
                    assert!(
                        !tool_names.iter().any(|n| n == banned),
                        "isolated set must not expose {banned:?}, got {tool_names:?}"
                    );
                }
                let any_isolated = BUILTIN_TOOLS_ISOLATED
                    .iter()
                    .any(|n| tool_names.iter().any(|t| t == n));
                assert!(
                    any_isolated,
                    "expected at least one isolated tool to be registered, got {tool_names:?}"
                );

                session.disconnect().await.expect("disconnect");
                client.stop().await.expect("stop");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn empty_mode_builtin_star_exposes_all_built_in_tools() {
    with_e2e_context(
        "mode_empty",
        "empty_mode_builtin_star_exposes_all_built_in_tools",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let options = ctx
                    .client_options()
                    .with_mode(ClientMode::Empty)
                    .with_base_directory(ctx.work_dir().to_path_buf());
                let client = Client::start(options).await.expect("start client");
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_permission_handler(Arc::new(ApproveAllHandler))
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                            .with_available_tools(star_builtin_tool_set()),
                    )
                    .await
                    .expect("create session");

                let _ = session.send_and_wait("Say hi.").await;

                let exchanges = ctx.exchanges();
                let tool_names = tool_names_from_request(exchanges.last().unwrap());
                assert!(
                    tool_names.iter().any(|n| n == SHELL_TOOL_NAME),
                    "builtin:* should expose {SHELL_TOOL_NAME}, got {tool_names:?}"
                );

                session.disconnect().await.expect("disconnect");
                client.stop().await.expect("stop");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn empty_mode_excluded_tools_subtracts_from_available_tools() {
    with_e2e_context(
        "mode_empty",
        "empty_mode_excluded_tools_subtracts_from_available_tools",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let options = ctx
                    .client_options()
                    .with_mode(ClientMode::Empty)
                    .with_base_directory(ctx.work_dir().to_path_buf());
                let client = Client::start(options).await.expect("start client");
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_permission_handler(Arc::new(ApproveAllHandler))
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                            .with_available_tools(star_builtin_tool_set())
                            .with_excluded_tools(vec![format!("builtin:{SHELL_TOOL_NAME}")]),
                    )
                    .await
                    .expect("create session");

                let _ = session.send_and_wait("Say hi.").await;

                let exchanges = ctx.exchanges();
                let tool_names = tool_names_from_request(exchanges.last().unwrap());
                assert!(
                    !tool_names.iter().any(|n| n == SHELL_TOOL_NAME),
                    "excluded {SHELL_TOOL_NAME} must not be exposed, got {tool_names:?}"
                );
                assert!(!tool_names.is_empty());

                session.disconnect().await.expect("disconnect");
                client.stop().await.expect("stop");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn empty_mode_strips_environment_context_from_the_system_message_by_default() {
    with_e2e_context(
        "mode_empty",
        "empty_mode_strips_environment_context_from_the_system_message_by_default",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let options = ctx
                    .client_options()
                    .with_mode(ClientMode::Empty)
                    .with_base_directory(ctx.work_dir().to_path_buf());
                let client = Client::start(options).await.expect("start client");
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_permission_handler(Arc::new(ApproveAllHandler))
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                            .with_available_tools(isolated_tool_set())
                            .with_system_message(
                                SystemMessageConfig::new()
                                    .with_mode("customize")
                                    .with_content(
                                        "If the user asks you to name an element, reply with exactly the single word ARGON in all caps and nothing else.",
                                    ),
                            ),
                    )
                    .await
                    .expect("create session");

                let event = session
                    .send_and_wait("Name an element.")
                    .await
                    .expect("send")
                    .expect("assistant message");
                let content = assistant_message_content(&event);
                assert!(content.contains("ARGON"), "expected ARGON in reply, got {content:?}");

                let exchanges = ctx.exchanges();
                let system_message = system_message_from_request(exchanges.last().unwrap());
                assert!(
                    !system_message.to_lowercase().contains("current working directory:"),
                    "env context should be stripped, got: {system_message}"
                );
                assert!(
                    !system_message.to_lowercase().contains("operating system:"),
                    "env context should be stripped, got: {system_message}"
                );

                session.disconnect().await.expect("disconnect");
                client.stop().await.expect("stop");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn empty_mode_system_message_replace_llm_follows_caller_content_verbatim() {
    with_e2e_context(
        "mode_empty",
        "empty_mode_system_message_replace_llm_follows_caller_content_verbatim",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let options = ctx
                    .client_options()
                    .with_mode(ClientMode::Empty)
                    .with_base_directory(ctx.work_dir().to_path_buf());
                let client = Client::start(options).await.expect("start client");
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_permission_handler(Arc::new(ApproveAllHandler))
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                            .with_available_tools(isolated_tool_set())
                            .with_system_message(
                                SystemMessageConfig::new()
                                    .with_mode("replace")
                                    .with_content(
                                        "You are a test fixture. Whenever the user asks anything, reply with exactly the single word KRYPTON in all caps and nothing else.",
                                    ),
                            ),
                    )
                    .await
                    .expect("create session");

                let event = session
                    .send_and_wait("Hello.")
                    .await
                    .expect("send")
                    .expect("assistant message");
                let content = assistant_message_content(&event);
                assert!(content.contains("KRYPTON"), "expected KRYPTON in reply, got {content:?}");

                session.disconnect().await.expect("disconnect");
                client.stop().await.expect("stop");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn empty_mode_append_caller_instruction_takes_effect_and_env_context_stripped() {
    with_e2e_context(
        "mode_empty",
        "empty_mode_append_caller_instruction_takes_effect_and_env_context_stripped",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let options = ctx
                    .client_options()
                    .with_mode(ClientMode::Empty)
                    .with_base_directory(ctx.work_dir().to_path_buf());
                let client = Client::start(options).await.expect("start client");
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_permission_handler(Arc::new(ApproveAllHandler))
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                            .with_available_tools(isolated_tool_set())
                            .with_system_message(
                                SystemMessageConfig::new()
                                    .with_mode("append")
                                    .with_content(
                                        "If the user asks you to name a noble gas, reply with exactly the single word XENON in all caps and nothing else.",
                                    ),
                            ),
                    )
                    .await
                    .expect("create session");

                let event = session
                    .send_and_wait("Name a noble gas.")
                    .await
                    .expect("send")
                    .expect("assistant message");
                let content = assistant_message_content(&event);
                assert!(content.contains("XENON"), "expected XENON in reply, got {content:?}");

                let exchanges = ctx.exchanges();
                let system_message = system_message_from_request(exchanges.last().unwrap());
                assert!(
                    !system_message.to_lowercase().contains("current working directory:"),
                    "env context should be stripped, got: {system_message}"
                );
                assert!(
                    !system_message.to_lowercase().contains("operating system:"),
                    "env context should be stripped, got: {system_message}"
                );

                session.disconnect().await.expect("disconnect");
                client.stop().await.expect("stop");
            })
        },
    )
    .await;
}
