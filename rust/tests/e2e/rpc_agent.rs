use github_copilot_sdk::CustomAgentConfig;
use github_copilot_sdk::generated::api_types::{AgentInfo, AgentSelectRequest};
use github_copilot_sdk::generated::session_events::SessionEventType;
use serde_json::json;

use super::support::{wait_for_event, with_e2e_context};

#[tokio::test]
async fn should_list_available_custom_agents() {
    with_e2e_context("rpc_agents", "should_list_available_custom_agents", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let session = client
                .create_session(
                    ctx.approve_all_session_config()
                        .with_custom_agents(create_custom_agents()),
                )
                .await
                .expect("create session");

            let result = session.rpc().agent().list().await.expect("agent list");
            assert_agent(&result.agents, "test-agent", "Test Agent", "A test agent");
            assert_agent(
                &result.agents,
                "another-agent",
                "Another Agent",
                "Another test agent",
            );

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_return_null_when_no_agent_is_selected() {
    with_e2e_context(
        "rpc_agents",
        "should_return_null_when_no_agent_is_selected",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_custom_agents([create_custom_agents().remove(0)]),
                    )
                    .await
                    .expect("create session");

                let value = client
                    .call(
                        "session.agent.getCurrent",
                        Some(json!({ "sessionId": session.id() })),
                    )
                    .await
                    .expect("get current agent");
                assert!(value.get("agent").is_some_and(serde_json::Value::is_null));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_select_and_get_current_agent() {
    with_e2e_context("rpc_agents", "should_select_and_get_current_agent", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let session = client
                .create_session(
                    ctx.approve_all_session_config()
                        .with_custom_agents([create_custom_agents().remove(0)]),
                )
                .await
                .expect("create session");

            let selected = session
                .rpc()
                .agent()
                .select(AgentSelectRequest {
                    name: "test-agent".to_string(),
                })
                .await
                .expect("select agent");
            assert_eq!(selected.agent.name, "test-agent");
            assert_eq!(selected.agent.display_name, "Test Agent");

            let current = session
                .rpc()
                .agent()
                .get_current()
                .await
                .expect("get selected agent");
            assert_eq!(current.agent.name, "test-agent");

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_emit_subagent_selected_and_deselected_events() {
    with_e2e_context(
        "rpc_agents",
        "should_emit_subagent_selected_and_deselected_events",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_custom_agents([create_custom_agents().remove(0)]),
                    )
                    .await
                    .expect("create session");

                let selected_event =
                    wait_for_event(session.subscribe(), "subagent selected", |event| {
                        event.parsed_type() == SessionEventType::SubagentSelected
                    });
                session
                    .rpc()
                    .agent()
                    .select(AgentSelectRequest {
                        name: "test-agent".to_string(),
                    })
                    .await
                    .expect("select agent");
                let selected = selected_event.await;
                assert_eq!(
                    selected
                        .data
                        .get("agentName")
                        .and_then(serde_json::Value::as_str),
                    Some("test-agent")
                );
                assert_eq!(
                    selected
                        .data
                        .get("agentDisplayName")
                        .and_then(serde_json::Value::as_str),
                    Some("Test Agent")
                );

                let deselected_event =
                    wait_for_event(session.subscribe(), "subagent deselected", |event| {
                        event.parsed_type() == SessionEventType::SubagentDeselected
                    });
                session
                    .rpc()
                    .agent()
                    .deselect()
                    .await
                    .expect("deselect agent");
                deselected_event.await;

                let value = client
                    .call(
                        "session.agent.getCurrent",
                        Some(json!({ "sessionId": session.id() })),
                    )
                    .await
                    .expect("get current agent after deselect");
                assert!(value.get("agent").is_some_and(serde_json::Value::is_null));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_deselect_current_agent() {
    with_e2e_context("rpc_agents", "should_deselect_current_agent", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let session = client
                .create_session(
                    ctx.approve_all_session_config()
                        .with_custom_agents([create_custom_agents().remove(0)]),
                )
                .await
                .expect("create session");

            session
                .rpc()
                .agent()
                .select(AgentSelectRequest {
                    name: "test-agent".to_string(),
                })
                .await
                .expect("select agent");
            session
                .rpc()
                .agent()
                .deselect()
                .await
                .expect("deselect agent");
            let value = client
                .call(
                    "session.agent.getCurrent",
                    Some(json!({ "sessionId": session.id() })),
                )
                .await
                .expect("get current agent");
            assert!(value.get("agent").is_some_and(serde_json::Value::is_null));

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_return_empty_list_when_no_custom_agents_configured() {
    with_e2e_context(
        "rpc_agents",
        "should_return_empty_list_when_no_custom_agents_configured",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let result = session.rpc().agent().list().await.expect("agent list");
                assert!(result.agents.is_empty());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_call_agent_reload() {
    with_e2e_context("rpc_agents", "should_call_agent_reload", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let reload_agent =
                CustomAgentConfig::new("reload-test-agent-rust", "You are a reload test agent.")
                    .with_display_name("Reload Test Agent")
                    .with_description("Used by the agent reload RPC test.");
            let client = ctx.start_client().await;
            let session = client
                .create_session(
                    ctx.approve_all_session_config()
                        .with_custom_agents([reload_agent.clone()]),
                )
                .await
                .expect("create session");

            assert_agent(
                &session
                    .rpc()
                    .agent()
                    .list()
                    .await
                    .expect("list before")
                    .agents,
                "reload-test-agent-rust",
                "Reload Test Agent",
                "Used by the agent reload RPC test.",
            );
            let reloaded = session.rpc().agent().reload().await.expect("reload agents");
            let current = session.rpc().agent().list().await.expect("list after");
            assert_eq!(
                agent_names(&reloaded.agents),
                agent_names(&current.agents),
                "reload result should match current list"
            );

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

fn create_custom_agents() -> Vec<CustomAgentConfig> {
    vec![
        CustomAgentConfig::new("test-agent", "You are a test agent.")
            .with_display_name("Test Agent")
            .with_description("A test agent"),
        CustomAgentConfig::new("another-agent", "You are another agent.")
            .with_display_name("Another Agent")
            .with_description("Another test agent"),
    ]
}

fn assert_agent(agents: &[AgentInfo], name: &str, display_name: &str, description: &str) {
    let agent = agents
        .iter()
        .find(|agent| agent.name == name)
        .unwrap_or_else(|| panic!("missing agent {name}; actual agents: {agents:?}"));
    assert_eq!(agent.display_name, display_name);
    assert_eq!(agent.description, description);
}

fn agent_names(agents: &[AgentInfo]) -> Vec<&str> {
    let mut names: Vec<_> = agents.iter().map(|agent| agent.name.as_str()).collect();
    names.sort_unstable();
    names
}
