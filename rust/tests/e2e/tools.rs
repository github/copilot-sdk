use std::sync::Arc;

use github_copilot_sdk::handler::{ApproveAllHandler, PermissionResult, SessionHandler};
use github_copilot_sdk::tool::{ToolHandler, ToolHandlerRouter};
use github_copilot_sdk::{
    Error, PermissionRequestData, RequestId, SessionConfig, SessionId, Tool, ToolInvocation,
    ToolResult,
};
use serde_json::json;
use tokio::sync::mpsc;

use super::support::{assistant_message_content, recv_with_timeout, with_e2e_context};

#[tokio::test]
async fn invokes_built_in_tools() {
    with_e2e_context("tools", "invokes_built_in_tools", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            std::fs::write(
                ctx.work_dir().join("README.md"),
                "# ELIZA, the only chatbot you'll ever need",
            )
            .expect("write README");
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");

            let answer = session
                .send_and_wait("What's the first line of README.md in this directory?")
                .await
                .expect("send")
                .expect("assistant message");
            assert!(assistant_message_content(&answer).contains("ELIZA"));

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn invokes_custom_tool() {
    with_e2e_context("tools", "invokes_custom_tool", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let router = ToolHandlerRouter::new(
                vec![Box::new(EncryptStringTool)],
                Arc::new(ApproveAllHandler),
            );
            let tools = router.tools();
            let session = client
                .create_session(
                    SessionConfig::default()
                        .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                        .with_handler(Arc::new(router))
                        .with_tools(tools),
                )
                .await
                .expect("create session");

            let answer = session
                .send_and_wait("Use encrypt_string to encrypt this string: Hello")
                .await
                .expect("send")
                .expect("assistant message");
            assert!(assistant_message_content(&answer).contains("HELLO"));

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn handles_tool_calling_errors() {
    with_e2e_context("tools", "handles_tool_calling_errors", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let router =
                ToolHandlerRouter::new(vec![Box::new(ErrorTool)], Arc::new(ApproveAllHandler));
            let tools = router.tools();
            let session = client
                .create_session(
                    SessionConfig::default()
                        .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                        .with_handler(Arc::new(router))
                        .with_tools(tools),
                )
                .await
                .expect("create session");

            let answer = session
                .send_and_wait("What is my location? If you can't find out, just say 'unknown'.")
                .await
                .expect("send")
                .expect("assistant message");
            let content = assistant_message_content(&answer);
            assert!(!content.contains("Melbourne"));
            assert!(content.to_lowercase().contains("unknown"));

            let exchanges = ctx.exchanges();
            let tool_results: Vec<_> = exchanges
                .last()
                .and_then(|exchange| exchange.get("request"))
                .and_then(|request| request.get("messages"))
                .and_then(serde_json::Value::as_array)
                .expect("messages")
                .iter()
                .filter(|message| {
                    message.get("role").and_then(serde_json::Value::as_str) == Some("tool")
                })
                .collect();
            assert_eq!(tool_results.len(), 1);
            assert!(!tool_results[0].to_string().contains("Melbourne"));

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn can_receive_and_return_complex_types() {
    with_e2e_context("tools", "can_receive_and_return_complex_types", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let router = ToolHandlerRouter::new(
                vec![Box::new(DbQueryTool)],
                Arc::new(ApproveAllHandler),
            );
            let tools = router.tools();
            let session = client
                .create_session(
                    SessionConfig::default()
                        .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                        .with_handler(Arc::new(router))
                        .with_tools(tools),
                )
                .await
                .expect("create session");

            let answer = session
                .send_and_wait(
                    "Perform a DB query for the 'cities' table using IDs 12 and 19, sorting ascending. \
                     Reply only with lines of the form: [cityname] [population]",
                )
                .await
                .expect("send")
                .expect("assistant message");
            let content = assistant_message_content(&answer);
            assert!(content.contains("Passos"));
            assert!(content.contains("San Lorenzo"));
            assert!(content.replace(',', "").contains("135460"));
            assert!(content.replace(',', "").contains("204356"));

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn overrides_built_in_tool_with_custom_tool() {
    with_e2e_context("tools", "overrides_built_in_tool_with_custom_tool", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let router =
                ToolHandlerRouter::new(vec![Box::new(CustomGrepTool)], Arc::new(ApproveAllHandler));
            let tools = router.tools();
            let session = client
                .create_session(
                    SessionConfig::default()
                        .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                        .with_handler(Arc::new(router))
                        .with_tools(tools),
                )
                .await
                .expect("create session");

            let answer = session
                .send_and_wait("Use grep to search for the word 'hello'")
                .await
                .expect("send")
                .expect("assistant message");
            assert!(assistant_message_content(&answer).contains("CUSTOM_GREP_RESULT"));

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn skippermission_sent_in_tool_definition() {
    with_e2e_context("tools", "skippermission_sent_in_tool_definition", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let (permission_tx, mut permission_rx) = mpsc::unbounded_channel();
            let handler = Arc::new(RecordingPermissionHandler {
                permission_tx,
                decision: PermissionResult::Denied,
            });
            let router = ToolHandlerRouter::new(vec![Box::new(SafeLookupTool)], handler);
            let tools = router.tools();
            let session = client
                .create_session(
                    SessionConfig::default()
                        .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                        .with_handler(Arc::new(router))
                        .with_tools(tools),
                )
                .await
                .expect("create session");

            let answer = session
                .send_and_wait("Use safe_lookup to look up 'test123'")
                .await
                .expect("send")
                .expect("assistant message");
            assert!(assistant_message_content(&answer).contains("RESULT"));
            assert!(
                tokio::time::timeout(std::time::Duration::from_millis(100), permission_rx.recv())
                    .await
                    .is_err(),
                "skip_permission tool should not request permission"
            );

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[ignore = "Behaves as if no content was in the result. Binary tool results are not fully implemented yet."]
#[tokio::test]
async fn can_return_binary_result() {}

#[tokio::test]
async fn invokes_custom_tool_with_permission_handler() {
    with_e2e_context(
        "tools",
        "invokes_custom_tool_with_permission_handler",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let (permission_tx, mut permission_rx) = mpsc::unbounded_channel();
                let handler = Arc::new(RecordingPermissionHandler {
                    permission_tx,
                    decision: PermissionResult::Approved,
                });
                let router = ToolHandlerRouter::new(vec![Box::new(EncryptStringTool)], handler);
                let tools = router.tools();
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                            .with_handler(Arc::new(router))
                            .with_tools(tools),
                    )
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait("Use encrypt_string to encrypt this string: Hello")
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(assistant_message_content(&answer).contains("HELLO"));
                let request = recv_with_timeout(&mut permission_rx, "custom tool permission").await;
                assert!(request.extra.is_object() || request.kind.is_some());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn denies_custom_tool_when_permission_denied() {
    with_e2e_context(
        "tools",
        "denies_custom_tool_when_permission_denied",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let (call_tx, mut call_rx) = mpsc::unbounded_channel();
                let (permission_tx, _permission_rx) = mpsc::unbounded_channel();
                let handler = Arc::new(RecordingPermissionHandler {
                    permission_tx,
                    decision: PermissionResult::Denied,
                });
                let router = ToolHandlerRouter::new(
                    vec![Box::new(TrackedEncryptStringTool { call_tx })],
                    handler,
                );
                let tools = router.tools();
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                            .with_handler(Arc::new(router))
                            .with_tools(tools),
                    )
                    .await
                    .expect("create session");

                session
                    .send_and_wait("Use encrypt_string to encrypt this string: Hello")
                    .await
                    .expect("send");
                assert!(
                    tokio::time::timeout(std::time::Duration::from_millis(100), call_rx.recv())
                        .await
                        .is_err(),
                    "denied custom tool should not be invoked"
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_execute_multiple_custom_tools_in_parallel_single_turn() {
    with_e2e_context(
        "tools",
        "should_execute_multiple_custom_tools_in_parallel_single_turn",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let (city_tx, mut city_rx) = mpsc::unbounded_channel();
                let (country_tx, mut country_rx) = mpsc::unbounded_channel();
                let router = ToolHandlerRouter::new(
                    vec![
                        Box::new(LookupCityTool { call_tx: city_tx }),
                        Box::new(LookupCountryTool {
                            call_tx: country_tx,
                        }),
                    ],
                    Arc::new(ApproveAllHandler),
                );
                let tools = router.tools();
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                            .with_handler(Arc::new(router))
                            .with_tools(tools),
                    )
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait("Use lookup_city with 'Paris' and lookup_country with 'France' at the same time, then combine both results in your reply.")
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert_eq!(recv_with_timeout(&mut city_rx, "city tool").await, "Paris");
                assert_eq!(
                    recv_with_timeout(&mut country_rx, "country tool").await,
                    "France"
                );
                let content = assistant_message_content(&answer);
                assert!(content.contains("CITY_PARIS"));
                assert!(content.contains("COUNTRY_FRANCE"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_respect_availabletools_and_excludedtools_combined() {
    with_e2e_context(
        "tools",
        "should_respect_availabletools_and_excludedtools_combined",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let (excluded_tx, mut excluded_rx) = mpsc::unbounded_channel();
                let router = ToolHandlerRouter::new(
                    vec![
                        Box::new(AllowedTool),
                        Box::new(ExcludedTool {
                            call_tx: excluded_tx,
                        }),
                    ],
                    Arc::new(ApproveAllHandler),
                );
                let tools = router.tools();
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                            .with_handler(Arc::new(router))
                            .with_tools(tools)
                            .with_available_tools(["allowed_tool", "excluded_tool"])
                            .with_excluded_tools(["excluded_tool"]),
                    )
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait(
                        "Use the allowed_tool with input 'test'. Do NOT use excluded_tool.",
                    )
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(assistant_message_content(&answer).contains("ALLOWED_TEST"));
                assert!(
                    tokio::time::timeout(std::time::Duration::from_millis(100), excluded_rx.recv())
                        .await
                        .is_err(),
                    "excluded tool should not be invoked"
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

struct EncryptStringTool;

#[async_trait::async_trait]
impl ToolHandler for EncryptStringTool {
    fn tool(&self) -> Tool {
        Tool::new("encrypt_string")
            .with_description("Encrypts a string")
            .with_parameters(json!({
                "type": "object",
                "properties": {
                    "input": {
                        "type": "string",
                        "description": "String to encrypt"
                    }
                },
                "required": ["input"]
            }))
    }

    async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error> {
        let input = invocation
            .arguments
            .get("input")
            .and_then(serde_json::Value::as_str)
            .unwrap_or_default();
        Ok(ToolResult::Text(input.to_uppercase()))
    }
}

struct TrackedEncryptStringTool {
    call_tx: mpsc::UnboundedSender<()>,
}

#[async_trait::async_trait]
impl ToolHandler for TrackedEncryptStringTool {
    fn tool(&self) -> Tool {
        EncryptStringTool.tool()
    }

    async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error> {
        let _ = self.call_tx.send(());
        EncryptStringTool.call(invocation).await
    }
}

struct ErrorTool;

#[async_trait::async_trait]
impl ToolHandler for ErrorTool {
    fn tool(&self) -> Tool {
        Tool::new("get_user_location").with_description("Gets the user's location")
    }

    async fn call(&self, _invocation: ToolInvocation) -> Result<ToolResult, Error> {
        Ok(ToolResult::Text(
            "Failed to execute `get_user_location` tool with arguments: {} due to error: Error: Tool execution failed"
                .to_string(),
        ))
    }
}

struct CustomGrepTool;

#[async_trait::async_trait]
impl ToolHandler for CustomGrepTool {
    fn tool(&self) -> Tool {
        Tool::new("grep")
            .with_description("A custom grep implementation that overrides the built-in")
            .with_overrides_built_in_tool(true)
            .with_parameters(json!({
                "type": "object",
                "properties": {
                    "query": { "type": "string", "description": "Search query" }
                },
                "required": ["query"]
            }))
    }

    async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error> {
        let query = invocation
            .arguments
            .get("query")
            .and_then(serde_json::Value::as_str)
            .unwrap_or_default();
        Ok(ToolResult::Text(format!("CUSTOM_GREP_RESULT: {query}")))
    }
}

struct SafeLookupTool;

#[async_trait::async_trait]
impl ToolHandler for SafeLookupTool {
    fn tool(&self) -> Tool {
        Tool::new("safe_lookup")
            .with_description("A tool that skips permission")
            .with_skip_permission(true)
            .with_parameters(json!({
                "type": "object",
                "properties": {
                    "id": { "type": "string", "description": "Lookup ID" }
                },
                "required": ["id"]
            }))
    }

    async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error> {
        let id = invocation
            .arguments
            .get("id")
            .and_then(serde_json::Value::as_str)
            .unwrap_or_default();
        Ok(ToolResult::Text(format!("RESULT: {id}")))
    }
}

struct LookupCityTool {
    call_tx: mpsc::UnboundedSender<String>,
}

#[async_trait::async_trait]
impl ToolHandler for LookupCityTool {
    fn tool(&self) -> Tool {
        Tool::new("lookup_city")
            .with_description("Looks up city information")
            .with_parameters(json!({
                "type": "object",
                "properties": {
                    "city": { "type": "string", "description": "City name" }
                },
                "required": ["city"]
            }))
    }

    async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error> {
        let city = invocation
            .arguments
            .get("city")
            .and_then(serde_json::Value::as_str)
            .unwrap_or_default()
            .to_string();
        let _ = self.call_tx.send(city.clone());
        Ok(ToolResult::Text(format!("CITY_{}", city.to_uppercase())))
    }
}

struct LookupCountryTool {
    call_tx: mpsc::UnboundedSender<String>,
}

#[async_trait::async_trait]
impl ToolHandler for LookupCountryTool {
    fn tool(&self) -> Tool {
        Tool::new("lookup_country")
            .with_description("Looks up country information")
            .with_parameters(json!({
                "type": "object",
                "properties": {
                    "country": { "type": "string", "description": "Country name" }
                },
                "required": ["country"]
            }))
    }

    async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error> {
        let country = invocation
            .arguments
            .get("country")
            .and_then(serde_json::Value::as_str)
            .unwrap_or_default()
            .to_string();
        let _ = self.call_tx.send(country.clone());
        Ok(ToolResult::Text(format!(
            "COUNTRY_{}",
            country.to_uppercase()
        )))
    }
}

struct AllowedTool;

#[async_trait::async_trait]
impl ToolHandler for AllowedTool {
    fn tool(&self) -> Tool {
        Tool::new("allowed_tool")
            .with_description("An allowed tool")
            .with_parameters(json!({
                "type": "object",
                "properties": {
                    "input": { "type": "string", "description": "Input value" }
                },
                "required": ["input"]
            }))
    }

    async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error> {
        let input = invocation
            .arguments
            .get("input")
            .and_then(serde_json::Value::as_str)
            .unwrap_or_default();
        Ok(ToolResult::Text(format!(
            "ALLOWED_{}",
            input.to_uppercase()
        )))
    }
}

struct ExcludedTool {
    call_tx: mpsc::UnboundedSender<()>,
}

#[async_trait::async_trait]
impl ToolHandler for ExcludedTool {
    fn tool(&self) -> Tool {
        Tool::new("excluded_tool")
            .with_description("A tool that should be excluded")
            .with_parameters(json!({
                "type": "object",
                "properties": {
                    "input": { "type": "string", "description": "Input value" }
                },
                "required": ["input"]
            }))
    }

    async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error> {
        let _ = self.call_tx.send(());
        let input = invocation
            .arguments
            .get("input")
            .and_then(serde_json::Value::as_str)
            .unwrap_or_default();
        Ok(ToolResult::Text(format!(
            "EXCLUDED_{}",
            input.to_uppercase()
        )))
    }
}

struct RecordingPermissionHandler {
    permission_tx: mpsc::UnboundedSender<PermissionRequestData>,
    decision: PermissionResult,
}

#[async_trait::async_trait]
impl SessionHandler for RecordingPermissionHandler {
    async fn on_permission_request(
        &self,
        _session_id: SessionId,
        _request_id: RequestId,
        data: PermissionRequestData,
    ) -> PermissionResult {
        let _ = self.permission_tx.send(data);
        self.decision.clone()
    }
}

struct DbQueryTool;

#[async_trait::async_trait]
impl ToolHandler for DbQueryTool {
    fn tool(&self) -> Tool {
        Tool::new("db_query")
            .with_description("Performs a database query")
            .with_parameters(json!({
                "type": "object",
                "properties": {
                    "query": {
                        "type": "object",
                        "properties": {
                            "table": { "type": "string" },
                            "ids": {
                                "type": "array",
                                "items": { "type": "integer" }
                            },
                            "sortAscending": { "type": "boolean" }
                        },
                        "required": ["table", "ids", "sortAscending"]
                    }
                },
                "required": ["query"]
            }))
    }

    async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error> {
        let query = invocation.arguments.get("query").expect("query argument");
        assert_eq!(
            query.get("table").and_then(serde_json::Value::as_str),
            Some("cities")
        );
        assert_eq!(
            query.get("ids").and_then(serde_json::Value::as_array),
            Some(&vec![json!(12), json!(19)])
        );
        assert_eq!(
            query
                .get("sortAscending")
                .and_then(serde_json::Value::as_bool),
            Some(true)
        );
        Ok(ToolResult::Text(
            r#"[{"cityName":"Passos","countryId":19,"population":135460},{"cityName":"San Lorenzo","countryId":12,"population":204356}]"#
                .to_string(),
        ))
    }
}
