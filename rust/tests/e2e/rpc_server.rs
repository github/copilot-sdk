use github_copilot_sdk::Client;
use github_copilot_sdk::generated::api_types::{
    McpDiscoverRequest, PingRequest, SkillsConfigSetDisabledSkillsRequest, SkillsDiscoverRequest,
    ToolsListRequest,
};
use serde_json::json;

use super::support::with_e2e_context;

#[tokio::test]
async fn should_call_rpc_ping_with_typed_params_and_result() {
    with_e2e_context(
        "rpc_server",
        "should_call_rpc_ping_with_typed_params_and_result",
        |ctx| {
            Box::pin(async move {
                let client = ctx.start_client().await;

                let result = client
                    .rpc()
                    .ping(PingRequest {
                        message: Some("typed rpc test".to_string()),
                    })
                    .await
                    .expect("ping");

                assert_eq!(result.message, "pong: typed rpc test");
                assert!(result.timestamp >= 0);
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_call_rpc_models_list_with_typed_result() {
    with_e2e_context(
        "rpc_server",
        "should_call_rpc_models_list_with_typed_result",
        |ctx| {
            Box::pin(async move {
                let token = "rpc-models-token";
                ctx.set_copilot_user_by_token_with_login(token, "rpc-user");
                let client = Client::start(ctx.client_options().with_github_token(token))
                    .await
                    .expect("start client");

                let result = client.rpc().models().list().await.expect("models list");

                assert!(
                    result
                        .models
                        .iter()
                        .any(|model| model.id == "claude-sonnet-4.5")
                );
                assert!(result.models.iter().all(|model| !model.name.is_empty()));
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_call_rpc_account_get_quota_when_authenticated() {
    with_e2e_context(
        "rpc_server",
        "should_call_rpc_account_get_quota_when_authenticated",
        |ctx| {
            Box::pin(async move {
                let token = "rpc-quota-token";
                ctx.set_copilot_user_by_token_with_login_and_quota(
                    token,
                    "rpc-user",
                    Some(json!({
                        "chat": {
                            "entitlement": 100,
                            "overage_count": 2,
                            "overage_permitted": true,
                            "percent_remaining": 75,
                            "timestamp_utc": "2026-04-30T00:00:00Z"
                        }
                    })),
                );
                let client = Client::start(ctx.client_options().with_github_token(token))
                    .await
                    .expect("start client");

                let result = client.rpc().account().get_quota().await.expect("quota");
                let chat = result.quota_snapshots.get("chat").expect("chat quota");

                assert_eq!(chat.entitlement_requests, 100);
                assert_eq!(chat.used_requests, 25);
                assert_eq!(chat.remaining_percentage, 75.0);
                assert_eq!(chat.overage, 2.0);
                assert!(chat.usage_allowed_with_exhausted_quota);
                assert!(chat.overage_allowed_with_exhausted_quota);
                assert_eq!(chat.reset_date.as_deref(), Some("2026-04-30T00:00:00Z"));
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_call_rpc_tools_list_with_typed_result() {
    with_e2e_context(
        "rpc_server",
        "should_call_rpc_tools_list_with_typed_result",
        |ctx| {
            Box::pin(async move {
                let client = ctx.start_client().await;

                let result = client
                    .rpc()
                    .tools()
                    .list(ToolsListRequest { model: None })
                    .await
                    .expect("tools list");

                assert!(!result.tools.is_empty());
                assert!(result.tools.iter().all(|tool| !tool.name.is_empty()));
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_discover_server_mcp_and_skills() {
    with_e2e_context(
        "rpc_server",
        "should_discover_server_mcp_and_skills",
        |ctx| {
            Box::pin(async move {
                let skill_name = "server-rpc-skill-rust";
                let skill_directory = create_skill_directory(
                    ctx.work_dir(),
                    skill_name,
                    "Skill discovered by server-scoped RPC tests.",
                );
                let client = ctx.start_client().await;

                let mcp = client
                    .rpc()
                    .mcp()
                    .discover(McpDiscoverRequest {
                        working_directory: Some(ctx.work_dir().to_string_lossy().to_string()),
                    })
                    .await
                    .expect("mcp discover");
                assert!(mcp.servers.iter().all(|server| !server.name.is_empty()));

                let skills = client
                    .rpc()
                    .skills()
                    .discover(SkillsDiscoverRequest {
                        project_paths: Vec::new(),
                        skill_directories: vec![skill_directory.to_string_lossy().to_string()],
                    })
                    .await
                    .expect("skills discover");
                let discovered = assert_server_skill(skills, skill_name, true);
                assert_eq!(
                    discovered.description,
                    "Skill discovered by server-scoped RPC tests."
                );

                client
                    .rpc()
                    .skills()
                    .config()
                    .set_disabled_skills(SkillsConfigSetDisabledSkillsRequest {
                        disabled_skills: vec![skill_name.to_string()],
                    })
                    .await
                    .expect("disable skill globally");
                let disabled_skills = client
                    .rpc()
                    .skills()
                    .discover(SkillsDiscoverRequest {
                        project_paths: Vec::new(),
                        skill_directories: vec![skill_directory.to_string_lossy().to_string()],
                    })
                    .await
                    .expect("skills discover disabled");
                assert_server_skill(disabled_skills, skill_name, false);

                client
                    .rpc()
                    .skills()
                    .config()
                    .set_disabled_skills(SkillsConfigSetDisabledSkillsRequest {
                        disabled_skills: Vec::new(),
                    })
                    .await
                    .expect("clear disabled skills");
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
    let skills_dir = work_dir.join("server-rpc-skills");
    let skill_dir = skills_dir.join(skill_name);
    std::fs::create_dir_all(&skill_dir).expect("create skill dir");
    std::fs::write(
        skill_dir.join("SKILL.md"),
        format!(
            "---\nname: {skill_name}\ndescription: {description}\n---\n\n# {skill_name}\n\nThis skill is used by RPC E2E tests.\n"
        ),
    )
    .expect("write skill");
    skills_dir
}

fn assert_server_skill(
    list: github_copilot_sdk::generated::api_types::ServerSkillList,
    skill_name: &str,
    enabled: bool,
) -> github_copilot_sdk::generated::api_types::ServerSkill {
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
