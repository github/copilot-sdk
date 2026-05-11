use std::collections::HashMap;

use github_copilot_sdk::generated::api_types::{
    ExtensionsDisableRequest, ExtensionsEnableRequest, McpDisableRequest, McpEnableRequest,
    McpOauthLoginRequest, SkillsDisableRequest, SkillsEnableRequest,
};
use github_copilot_sdk::{McpServerConfig, McpStdioServerConfig};

use super::support::with_e2e_context;

#[tokio::test]
async fn should_list_and_toggle_session_skills() {
    with_e2e_context(
        "rpc_mcp_and_skills",
        "should_list_and_toggle_session_skills",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let skill_name = "session-rpc-skill-rust";
                let skills_dir = create_skill_directory(
                    ctx.work_dir(),
                    skill_name,
                    "Session skill controlled by RPC.",
                );
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_skill_directories([skills_dir])
                            .with_disabled_skills([skill_name]),
                    )
                    .await
                    .expect("create session");

                assert_skill(
                    session.rpc().skills().list().await.expect("list disabled"),
                    skill_name,
                    false,
                );
                session
                    .rpc()
                    .skills()
                    .enable(SkillsEnableRequest {
                        name: skill_name.to_string(),
                    })
                    .await
                    .expect("enable skill");
                assert_skill(
                    session.rpc().skills().list().await.expect("list enabled"),
                    skill_name,
                    true,
                );
                session
                    .rpc()
                    .skills()
                    .disable(SkillsDisableRequest {
                        name: skill_name.to_string(),
                    })
                    .await
                    .expect("disable skill");
                assert_skill(
                    session
                        .rpc()
                        .skills()
                        .list()
                        .await
                        .expect("list disabled again"),
                    skill_name,
                    false,
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_reload_session_skills() {
    with_e2e_context(
        "rpc_mcp_and_skills",
        "should_reload_session_skills",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let skills_dir = ctx.work_dir().join("reloadable-rpc-skills");
                std::fs::create_dir_all(&skills_dir).expect("create skills dir");
                let skill_name = "reload-rpc-skill-rust";
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_skill_directories([skills_dir.clone()]),
                    )
                    .await
                    .expect("create session");

                let before = session.rpc().skills().list().await.expect("list before");
                assert!(!before.skills.iter().any(|skill| skill.name == skill_name));

                create_skill(
                    &skills_dir,
                    skill_name,
                    "Skill added after session creation.",
                );
                session
                    .rpc()
                    .skills()
                    .reload()
                    .await
                    .expect("reload skills");
                let after = session.rpc().skills().list().await.expect("list after");
                let skill = assert_skill(after, skill_name, true);
                assert_eq!(skill.description, "Skill added after session creation.");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_list_mcp_servers_with_configured_server() {
    with_e2e_context(
        "rpc_mcp_and_skills",
        "should_list_mcp_servers_with_configured_server",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let server_name = "rpc-list-mcp-server";
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_mcp_servers(test_mcp_servers(server_name)),
                    )
                    .await
                    .expect("create session");

                let result = session.rpc().mcp().list().await.expect("mcp list");
                assert!(
                    result
                        .servers
                        .iter()
                        .any(|server| server.name == server_name)
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_list_plugins() {
    with_e2e_context("rpc_mcp_and_skills", "should_list_plugins", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");

            let result = session.rpc().plugins().list().await.expect("plugins list");
            assert!(
                result.plugins.iter().all(|plugin| !plugin.name.is_empty()),
                "plugins should have names: {:?}",
                result.plugins
            );

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_list_extensions() {
    with_e2e_context("rpc_mcp_and_skills", "should_list_extensions", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client =
                github_copilot_sdk::Client::start(ctx.client_options().with_extra_args(["--yolo"]))
                    .await
                    .expect("start yolo client");
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");

            let result = session
                .rpc()
                .extensions()
                .list()
                .await
                .expect("extensions list");
            assert!(
                result
                    .extensions
                    .iter()
                    .all(|extension| !extension.id.is_empty() && !extension.name.is_empty()),
                "extensions should have ids and names: {:?}",
                result.extensions
            );

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_report_error_when_mcp_host_is_not_initialized() {
    with_e2e_context(
        "rpc_mcp_and_skills",
        "should_report_error_when_mcp_host_is_not_initialized",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                expect_err_contains(
                    session.rpc().mcp().enable(McpEnableRequest {
                        server_name: "missing-server".to_string(),
                    }),
                    "No MCP host initialized",
                )
                .await;
                expect_err_contains(
                    session.rpc().mcp().disable(McpDisableRequest {
                        server_name: "missing-server".to_string(),
                    }),
                    "No MCP host initialized",
                )
                .await;
                expect_err_contains(
                    session.rpc().mcp().reload(),
                    "MCP config reload not available",
                )
                .await;
                expect_err_contains(
                    session.rpc().mcp().oauth().login(McpOauthLoginRequest {
                        server_name: "missing-server".to_string(),
                        callback_success_message: None,
                        client_name: None,
                        force_reauth: None,
                    }),
                    "MCP host is not available",
                )
                .await;

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_report_error_when_mcp_oauth_server_is_not_configured() {
    with_e2e_context(
        "rpc_mcp_and_skills",
        "should_report_error_when_mcp_oauth_server_is_not_configured",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_mcp_servers(test_mcp_servers("configured-stdio-server")),
                    )
                    .await
                    .expect("create session");

                expect_err_contains(
                    session.rpc().mcp().oauth().login(McpOauthLoginRequest {
                        server_name: "missing-server".to_string(),
                        callback_success_message: None,
                        client_name: None,
                        force_reauth: None,
                    }),
                    "is not configured",
                )
                .await;

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_report_error_when_mcp_oauth_server_is_not_remote() {
    with_e2e_context(
        "rpc_mcp_and_skills",
        "should_report_error_when_mcp_oauth_server_is_not_remote",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let server_name = "configured-stdio-server";
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_mcp_servers(test_mcp_servers(server_name)),
                    )
                    .await
                    .expect("create session");

                expect_err_contains(
                    session.rpc().mcp().oauth().login(McpOauthLoginRequest {
                        server_name: server_name.to_string(),
                        callback_success_message: Some("Done".to_string()),
                        client_name: Some("SDK E2E".to_string()),
                        force_reauth: Some(true),
                    }),
                    "not a remote server",
                )
                .await;

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_report_error_when_extensions_are_not_available() {
    with_e2e_context(
        "rpc_mcp_and_skills",
        "should_report_error_when_extensions_are_not_available",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = github_copilot_sdk::Client::start(
                    ctx.client_options().with_extra_args(["--yolo"]),
                )
                .await
                .expect("start client");
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                expect_err_contains(
                    session.rpc().extensions().enable(ExtensionsEnableRequest {
                        id: "missing-extension".to_string(),
                    }),
                    "Extensions not available",
                )
                .await;
                expect_err_contains(
                    session
                        .rpc()
                        .extensions()
                        .disable(ExtensionsDisableRequest {
                            id: "missing-extension".to_string(),
                        }),
                    "Extensions not available",
                )
                .await;
                expect_err_contains(
                    session.rpc().extensions().reload(),
                    "Extensions not available",
                )
                .await;

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

fn create_skill_directory(
    work_dir: &std::path::Path,
    skill_name: &str,
    description: &str,
) -> std::path::PathBuf {
    let skills_dir = work_dir.join("session-rpc-skills");
    create_skill(&skills_dir, skill_name, description);
    skills_dir
}

fn create_skill(skills_dir: &std::path::Path, skill_name: &str, description: &str) {
    let skill_dir = skills_dir.join(skill_name);
    std::fs::create_dir_all(&skill_dir).expect("create skill dir");
    std::fs::write(
        skill_dir.join("SKILL.md"),
        format!(
            "---\nname: {skill_name}\ndescription: {description}\n---\n\n# {skill_name}\n\nThis skill is used by RPC E2E tests.\n"
        ),
    )
    .expect("write skill");
}

fn assert_skill(
    list: github_copilot_sdk::generated::api_types::SkillList,
    skill_name: &str,
    enabled: bool,
) -> github_copilot_sdk::generated::api_types::Skill {
    let skill = list
        .skills
        .into_iter()
        .find(|skill| skill.name == skill_name)
        .unwrap_or_else(|| panic!("skill {skill_name} not found"));
    assert_eq!(skill.enabled, enabled);
    assert!(
        skill
            .path
            .as_deref()
            .is_some_and(|path| path.contains(skill_name) && path.ends_with("SKILL.md"))
    );
    skill
}

fn test_mcp_servers(message: &str) -> HashMap<String, McpServerConfig> {
    HashMap::from([(
        message.to_string(),
        McpServerConfig::Stdio(McpStdioServerConfig {
            tools: vec!["*".to_string()],
            command: echo_command(),
            args: echo_args(message),
            ..McpStdioServerConfig::default()
        }),
    )])
}

async fn expect_err_contains<T>(
    future: impl std::future::Future<Output = Result<T, github_copilot_sdk::Error>>,
    expected: &str,
) {
    let err = match future.await {
        Ok(_) => panic!("expected RPC failure"),
        Err(err) => err,
    };
    assert!(
        err.to_string()
            .to_ascii_lowercase()
            .contains(&expected.to_ascii_lowercase()),
        "expected error to contain {expected:?}, got {err}"
    );
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
