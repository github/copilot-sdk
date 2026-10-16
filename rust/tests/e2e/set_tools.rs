use std::sync::Arc;

use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::tool::ToolHandler;
use github_copilot_sdk::{Error, ResumeSessionConfig, Tool, ToolInvocation, ToolResult};
use serde_json::json;

use super::support::assistant_message_content;

// Shared with the other SDKs' set_tools E2E tests, which replay the same
// snapshots.
const FRUIT_PROMPT: &str = "Use lookup_fruit to find the fruit for code 42.";
const FRUIT_AND_VEGETABLE_PROMPT: &str = "Use lookup_fruit to find the fruit for code 42 again, and use lookup_vegetable to find the vegetable for code 7.";
const VEGETABLE_PROMPT: &str = "Use lookup_vegetable to find the vegetable for code 7.";

/// Answers with a fixed result and records the codes it was called with.
struct LookupTool {
    result: &'static str,
    calls: Arc<parking_lot::Mutex<Vec<i64>>>,
}

#[async_trait::async_trait]
impl ToolHandler for LookupTool {
    async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error> {
        if let Some(code) = invocation
            .arguments
            .get("code")
            .and_then(|code| code.as_i64())
        {
            self.calls.lock().push(code);
        }
        Ok(ToolResult::Text(self.result.to_string()))
    }
}

type Calls = Arc<parking_lot::Mutex<Vec<i64>>>;

fn lookup_tool(
    name: &str,
    description: &str,
    code_description: &str,
    result: &'static str,
) -> (Tool, Calls) {
    let calls = Calls::default();
    let tool = Tool::new(name)
        .with_description(description)
        .with_parameters(json!({
            "type": "object",
            "properties": {
                "code": { "type": "integer", "description": code_description }
            },
            "required": ["code"]
        }))
        .with_handler(Arc::new(LookupTool {
            result,
            calls: calls.clone(),
        }));
    (tool, calls)
}

fn lookup_fruit(fruit: &'static str) -> (Tool, Calls) {
    lookup_tool(
        "lookup_fruit",
        "Looks up the fruit for a numeric code",
        "Fruit code",
        fruit,
    )
}

fn lookup_vegetable() -> (Tool, Calls) {
    lookup_tool(
        "lookup_vegetable",
        "Looks up the vegetable for a numeric code",
        "Vegetable code",
        "carrot",
    )
}

fn calls(calls: &Calls) -> Vec<i64> {
    calls.lock().clone()
}

/// A tool with no parameters that answers with a fixed result.
fn plain_tool(name: &str, description: &str, result: &'static str) -> Tool {
    Tool::new(name)
        .with_description(description)
        .with_handler(Arc::new(LookupTool {
            result,
            calls: Calls::default(),
        }))
}

/// Index of the first model request that carries `prompt` as a user message.
fn index_of_prompt(exchanges: &[serde_json::Value], prompt: &str) -> Option<usize> {
    exchanges.iter().position(|exchange| {
        exchange["request"]["messages"]
            .as_array()
            .into_iter()
            .flatten()
            .any(|message| {
                let content = &message["content"];
                message["role"] == "user"
                    && content.as_str().map_or_else(
                        || content.to_string().contains(prompt),
                        |text| text.contains(prompt),
                    )
            })
    })
}

/// Asserts that every model request offered the tools in `offered` and none of
/// the tools in `not_offered`.
fn assert_offered(exchanges: &[serde_json::Value], offered: &[&str], not_offered: &[&str]) {
    for exchange in exchanges {
        let tools: Vec<&str> = exchange["request"]["tools"]
            .as_array()
            .into_iter()
            .flatten()
            .filter_map(|tool| tool["function"]["name"].as_str())
            .collect();
        for name in offered {
            assert!(tools.contains(name), "offered {tools:?}, expected {name}");
        }
        for name in not_offered {
            assert!(
                !tools.contains(name),
                "offered {tools:?}, expected no {name}"
            );
        }
    }
}

#[tokio::test]
async fn replaces_tools_on_a_created_session() {
    super::support::with_shared_e2e_context(
        &E2E,
        "set_tools",
        "replaces_tools_on_a_created_session",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let (original, original_calls) = lookup_fruit("apple");
                let retired = plain_tool("retired_lookup", "Looks up a retired value", "retired");
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_tools(vec![original, retired]),
                    )
                    .await
                    .expect("create session");

                let first = session
                    .send_and_wait(FRUIT_PROMPT)
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(assistant_message_content(&first).contains("apple"));

                let (replacement, replacement_calls) = lookup_fruit("dragonfruit");
                let (vegetable, vegetable_calls) = lookup_vegetable();
                session
                    .set_tools([replacement, vegetable])
                    .await
                    .expect("replace tools");

                let second = session
                    .send_and_wait(FRUIT_AND_VEGETABLE_PROMPT)
                    .await
                    .expect("send")
                    .expect("assistant message");
                let content = assistant_message_content(&second);
                assert!(content.contains("dragonfruit"), "{content}");
                assert!(content.contains("carrot"), "{content}");
                assert_eq!(calls(&original_calls), [42]);
                assert_eq!(calls(&replacement_calls), [42]);
                assert_eq!(calls(&vegetable_calls), [7]);

                // Model requests after the replacement offer exactly the new tool set.
                let exchanges = ctx.exchanges();
                let replaced_from = index_of_prompt(&exchanges, FRUIT_AND_VEGETABLE_PROMPT)
                    .expect("a model request carries the replacement prompt");
                assert!(
                    replaced_from > 0,
                    "expected model requests before the replacement"
                );
                let (before, after) = exchanges.split_at(replaced_from);
                assert_offered(
                    before,
                    &["lookup_fruit", "retired_lookup"],
                    &["lookup_vegetable"],
                );
                assert_offered(
                    after,
                    &["lookup_fruit", "lookup_vegetable"],
                    &["retired_lookup"],
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn replaces_tools_on_a_resumed_session() {
    super::support::with_shared_e2e_context(
        &E2E,
        "set_tools",
        "replaces_tools_on_a_resumed_session",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let (created_tool, created_calls) = lookup_fruit("apple");
                let created = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_tools(vec![created_tool]),
                    )
                    .await
                    .expect("create session");
                let session_id = created.id().clone();
                let first = created
                    .send_and_wait(FRUIT_PROMPT)
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(assistant_message_content(&first).contains("apple"));
                assert_eq!(calls(&created_calls), [42]);
                created.disconnect().await.expect("disconnect session");

                let (fruit, fruit_calls) = lookup_fruit("apple");
                let resumed = client
                    .resume_session(
                        ResumeSessionConfig::new(session_id)
                            .with_permission_handler(Arc::new(ApproveAllHandler))
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                            .with_tools(vec![fruit]),
                    )
                    .await
                    .expect("resume session");
                let (vegetable, vegetable_calls) = lookup_vegetable();
                resumed.set_tools([vegetable]).await.expect("replace tools");

                let answer = resumed
                    .send_and_wait(VEGETABLE_PROMPT)
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(assistant_message_content(&answer).contains("carrot"));
                assert_eq!(calls(&vegetable_calls), [7]);
                assert!(calls(&fruit_calls).is_empty());

                let exchanges = ctx.exchanges();
                let replaced_from = index_of_prompt(&exchanges, VEGETABLE_PROMPT)
                    .expect("a model request carries the replacement prompt");
                assert!(
                    replaced_from > 0,
                    "expected model requests before the replacement"
                );
                let (_, after) = exchanges.split_at(replaced_from);
                assert_offered(after, &["lookup_vegetable"], &["lookup_fruit"]);

                resumed.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn keeps_the_previous_tools_when_a_replacement_is_rejected() {
    super::support::with_shared_e2e_context(
        &E2E,
        "set_tools",
        "keeps_the_previous_tools_when_a_replacement_is_rejected",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let (original, original_calls) = lookup_fruit("apple");
                let session = client
                    .create_session(ctx.approve_all_session_config().with_tools(vec![original]))
                    .await
                    .expect("create session");

                let (replacement, replacement_calls) = lookup_fruit("dragonfruit");
                let invalid = plain_tool("invalid.tool", "Has a name the runtime rejects", "never");
                session
                    .set_tools([replacement, invalid])
                    .await
                    .expect_err("the runtime rejects an invalid tool name");

                let answer = session
                    .send_and_wait(FRUIT_PROMPT)
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(assistant_message_content(&answer).contains("apple"));
                assert_eq!(calls(&original_calls), [42]);
                assert!(calls(&replacement_calls).is_empty());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

static E2E: super::support::SharedE2eGroup =
    super::support::SharedE2eGroup::standard("set_tools", 3);
