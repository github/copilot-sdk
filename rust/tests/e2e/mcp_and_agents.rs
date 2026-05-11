use std::collections::HashMap;

use github_copilot_sdk::{
    CustomAgentConfig, McpServerConfig, McpStdioServerConfig, ResumeSessionConfig,
};

use super::support::{assistant_message_content, with_e2e_context};

#[tokio::test]
async fn accept_mcp_server_config_on_create() {
    with_e2e_context(
        "mcp_and_agents",
        "accept_mcp_server_config_on_create",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_mcp_servers(test_mcp_servers("hello")),
                    )
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait("What is 2+2?")
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(assistant_message_content(&answer).contains('4'));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn accept_mcp_server_config_on_resume() {
    with_e2e_context(
        "mcp_and_agents",
        "accept_mcp_server_config_on_resume",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session1 = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create first session");
                let session_id = session1.id().clone();
                session1
                    .send_and_wait("What is 1+1?")
                    .await
                    .expect("send first");
                session1.disconnect().await.expect("disconnect first");

                let session2 = client
                    .resume_session(
                        ResumeSessionConfig::new(session_id.clone())
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                            .with_handler(std::sync::Arc::new(
                                github_copilot_sdk::handler::ApproveAllHandler,
                            ))
                            .with_mcp_servers(test_mcp_servers("hello")),
                    )
                    .await
                    .expect("resume session");
                assert_eq!(session2.id(), &session_id);

                let answer = session2
                    .send_and_wait("What is 3+3?")
                    .await
                    .expect("send resumed")
                    .expect("assistant message");
                assert!(assistant_message_content(&answer).contains('6'));

                session2.disconnect().await.expect("disconnect resumed");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn accept_custom_agent_config_on_create() {
    with_e2e_context(
        "mcp_and_agents",
        "accept_custom_agent_config_on_create",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_custom_agents([test_agent("test-agent", "Test Agent")]),
                    )
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait("What is 5+5?")
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(assistant_message_content(&answer).contains("10"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn accept_custom_agent_config_on_resume() {
    with_e2e_context(
        "mcp_and_agents",
        "accept_custom_agent_config_on_resume",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session1 = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create first session");
                let session_id = session1.id().clone();
                session1
                    .send_and_wait("What is 1+1?")
                    .await
                    .expect("send first");
                session1.disconnect().await.expect("disconnect first");

                let session2 = client
                    .resume_session(
                        ResumeSessionConfig::new(session_id.clone())
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                            .with_handler(std::sync::Arc::new(
                                github_copilot_sdk::handler::ApproveAllHandler,
                            ))
                            .with_custom_agents([test_agent("resume-agent", "Resume Agent")]),
                    )
                    .await
                    .expect("resume session");
                assert_eq!(session2.id(), &session_id);

                let answer = session2
                    .send_and_wait("What is 6+6?")
                    .await
                    .expect("send resumed")
                    .expect("assistant message");
                assert!(assistant_message_content(&answer).contains("12"));

                session2.disconnect().await.expect("disconnect resumed");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_handle_multiple_mcp_servers() {
    with_e2e_context(
        "mcp_and_agents",
        "should_handle_multiple_mcp_servers",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_mcp_servers(multiple_mcp_servers()),
                    )
                    .await
                    .expect("create session");

                assert!(!session.id().as_str().is_empty());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_handle_custom_agent_with_tools_configuration() {
    with_e2e_context(
        "mcp_and_agents",
        "should_handle_custom_agent_with_tools_configuration",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let agent = test_agent("tool-agent", "Tool Agent").with_tools(["bash", "edit"]);
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config().with_custom_agents([agent]))
                    .await
                    .expect("create session");

                let listed = session.rpc().agent().list().await.expect("list agents");
                assert!(listed.agents.iter().any(|agent| agent.name == "tool-agent"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_handle_custom_agent_with_mcp_servers() {
    with_e2e_context(
        "mcp_and_agents",
        "should_handle_custom_agent_with_mcp_servers",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let agent = test_agent("mcp-agent", "MCP Agent")
                    .with_mcp_servers(test_mcp_servers("agent-mcp"));
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config().with_custom_agents([agent]))
                    .await
                    .expect("create session");

                let listed = session.rpc().agent().list().await.expect("list agents");
                assert!(listed.agents.iter().any(|agent| agent.name == "mcp-agent"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_handle_multiple_custom_agents() {
    with_e2e_context(
        "mcp_and_agents",
        "should_handle_multiple_custom_agents",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config().with_custom_agents([
                        test_agent("agent1", "Agent One"),
                        test_agent("agent2", "Agent Two").with_infer(false),
                    ]))
                    .await
                    .expect("create session");

                let listed = session.rpc().agent().list().await.expect("list agents");
                assert!(listed.agents.iter().any(|agent| agent.name == "agent1"));
                assert!(listed.agents.iter().any(|agent| agent.name == "agent2"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_accept_both_mcp_servers_and_custom_agents() {
    with_e2e_context(
        "mcp_and_agents",
        "should_accept_both_mcp_servers_and_custom_agents",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_mcp_servers(test_mcp_servers("session-mcp"))
                            .with_custom_agents([test_agent("combined-agent", "Combined Agent")]),
                    )
                    .await
                    .expect("create session");

                let agents = session.rpc().agent().list().await.expect("list agents");
                assert!(
                    agents
                        .agents
                        .iter()
                        .any(|agent| agent.name == "combined-agent")
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_pass_literal_env_values_to_mcp_server_subprocess() {
    let config = McpStdioServerConfig {
        command: echo_command(),
        args: echo_args("env"),
        env: HashMap::from([("MCP_LITERAL".to_string(), "literal-value".to_string())]),
        ..McpStdioServerConfig::default()
    };

    assert_eq!(
        config.env.get("MCP_LITERAL"),
        Some(&"literal-value".to_string())
    );
}

#[tokio::test]
async fn should_round_trip_mcp_server_elicitation_request() {
    let payload = serde_json::json!({
        "action": "accept",
        "content": { "value": "selected" }
    });

    assert_eq!(payload["action"], "accept");
    assert_eq!(payload["content"]["value"], "selected");
}

fn test_agent(name: &str, display_name: &str) -> CustomAgentConfig {
    CustomAgentConfig::new(name, "You are a helpful test agent.")
        .with_display_name(display_name)
        .with_description("A test agent for SDK testing")
        .with_infer(true)
}

fn multiple_mcp_servers() -> HashMap<String, McpServerConfig> {
    let mut servers = test_mcp_servers("server1");
    servers.insert(
        "server2".to_string(),
        McpServerConfig::Stdio(McpStdioServerConfig {
            tools: vec!["*".to_string()],
            command: echo_command(),
            args: echo_args("server2"),
            ..McpStdioServerConfig::default()
        }),
    );
    servers
}

fn test_mcp_servers(message: &str) -> HashMap<String, McpServerConfig> {
    HashMap::from([(
        "test-server".to_string(),
        McpServerConfig::Stdio(McpStdioServerConfig {
            tools: vec!["*".to_string()],
            command: echo_command(),
            args: echo_args(message),
            ..McpStdioServerConfig::default()
        }),
    )])
}

#[cfg(windows)]
fn echo_command() -> String {
    "cmd".to_string()
}

#[cfg(not(windows))]
fn echo_command() -> String {
    "echo".to_string()
}

#[cfg(windows)]
fn echo_args(message: &str) -> Vec<String> {
    vec!["/C".to_string(), "echo".to_string(), message.to_string()]
}

#[cfg(not(windows))]
fn echo_args(message: &str) -> Vec<String> {
    vec![message.to_string()]
}
