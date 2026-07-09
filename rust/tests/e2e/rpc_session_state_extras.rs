use std::collections::HashMap;

use github_copilot_sdk::Client;
use github_copilot_sdk::rpc::{
    CompletionsRequestRequest, MetadataContextHeaviestMessagesRequest, ModelSwitchToRequest,
    NamedProviderConfig, PermissionsSetAllowAllRequest, ProviderAddRequest, ProviderConfigType,
    ProviderConfigWireApi, ProviderModelConfig, SessionVisibilityStatus, SubagentSettingsEntry,
    SubagentSettingsEntryContextTier, UpdateSubagentSettingsRequest,
    UpdateSubagentSettingsRequestSubagents, VisibilitySetRequest,
};

use super::support::{assistant_message_content, with_e2e_context};

const MODEL_ID: &str = "claude-sonnet-4.5";

#[tokio::test]
async fn should_list_models_for_session() {
    with_e2e_context(
        "rpc_session_state_extras",
        "should_list_models_for_session",
        |ctx| {
            Box::pin(async move {
                let token = "rpc-session-model-list-token";
                ctx.set_copilot_user_by_token_with_login(token, "rpc-session-extras-user");
                let client = Client::start(ctx.client_options().with_github_token(token))
                    .await
                    .expect("start authenticated client");
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_github_token(token)
                            .with_model(MODEL_ID),
                    )
                    .await
                    .expect("create session");

                let result = session.rpc().model().list().await.expect("list models");

                assert!(!result.list.is_empty());
                assert!(
                    result
                        .list
                        .iter()
                        .any(|model| model.to_string().contains(MODEL_ID))
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_report_session_activity_when_idle() {
    with_e2e_context(
        "rpc_session_state_extras",
        "should_report_session_activity_when_idle",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let activity = session
                    .rpc()
                    .metadata()
                    .activity()
                    .await
                    .expect("get activity");

                assert!(!activity.has_active_work);
                assert!(!activity.abortable);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_get_and_set_allowall_permissions() {
    with_e2e_context(
        "rpc_session_state_extras",
        "should_get_and_set_allowall_permissions",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let initial = session
                    .rpc()
                    .permissions()
                    .get_allow_all()
                    .await
                    .expect("get initial allow-all");
                assert!(!initial.enabled);

                let enable = session
                    .rpc()
                    .permissions()
                    .set_allow_all(PermissionsSetAllowAllRequest {
                        enabled: Some(true),
                        mode: None,
                        model: None,
                        source: None,
                    })
                    .await
                    .expect("enable allow-all");
                assert!(enable.success);
                assert!(enable.enabled);
                assert!(
                    session
                        .rpc()
                        .permissions()
                        .get_allow_all()
                        .await
                        .expect("get enabled allow-all")
                        .enabled
                );

                let disable = session
                    .rpc()
                    .permissions()
                    .set_allow_all(PermissionsSetAllowAllRequest {
                        enabled: Some(false),
                        mode: None,
                        model: None,
                        source: None,
                    })
                    .await
                    .expect("disable allow-all");
                assert!(disable.success);
                assert!(!disable.enabled);
                assert!(
                    !session
                        .rpc()
                        .permissions()
                        .get_allow_all()
                        .await
                        .expect("get disabled allow-all")
                        .enabled
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_read_empty_sql_todos_for_fresh_session() {
    with_e2e_context(
        "rpc_session_state_extras",
        "should_read_empty_sql_todos_for_fresh_session",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let result = session
                    .rpc()
                    .plan()
                    .read_sql_todos()
                    .await
                    .expect("read SQL todos");

                assert!(result.rows.is_empty());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_get_telemetry_engagement_id() {
    with_e2e_context(
        "rpc_session_state_extras",
        "should_get_telemetry_engagement_id",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let _result = session
                    .rpc()
                    .telemetry()
                    .get_engagement_id()
                    .await
                    .expect("get telemetry engagement id");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_get_current_tool_metadata_after_initialization() {
    with_e2e_context(
        "rpc_session_state_extras",
        "should_get_current_tool_metadata_after_initialization",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait("What is 2+2?")
                    .await
                    .expect("send prompt")
                    .expect("assistant message");
                assert!(!assistant_message_content(&answer).trim().is_empty());

                let result = session
                    .rpc()
                    .tools()
                    .get_current_metadata()
                    .await
                    .expect("get current tool metadata");

                let tools = result.tools.expect("current tool metadata");
                assert!(!tools.is_empty());
                assert!(tools.iter().all(|tool| !tool.name.trim().is_empty()));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_add_byok_provider_and_model_at_runtime() {
    with_e2e_context(
        "rpc_session_state_extras",
        "should_add_byok_provider_and_model_at_runtime",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let result = session
                    .rpc()
                    .provider()
                    .add(ProviderAddRequest {
                        providers: Some(vec![NamedProviderConfig {
                            api_key: Some("provider-key".to_string()),
                            azure: None,
                            base_url: "https://models.example.test/v1".to_string(),
                            bearer_token: None,
                            has_bearer_token_provider: None,
                            headers: Some(HashMap::from([(
                                "x-provider".to_string(),
                                "rust".to_string(),
                            )])),
                            name: "rust-e2e-provider".to_string(),
                            transport: None,
                            r#type: Some(ProviderConfigType::Openai),
                            wire_api: Some(ProviderConfigWireApi::Completions),
                        }]),
                        models: Some(vec![ProviderModelConfig {
                            capabilities: None,
                            id: "small".to_string(),
                            max_context_window_tokens: None,
                            max_output_tokens: None,
                            max_prompt_tokens: Some(4096.0),
                            model_id: None,
                            name: Some("Rust Added Model".to_string()),
                            provider: "rust-e2e-provider".to_string(),
                            wire_model: None,
                        }]),
                    })
                    .await
                    .expect("add provider model");
                assert_eq!(result.models.len(), 1);

                let selection_id = "rust-e2e-provider/small";
                session
                    .rpc()
                    .model()
                    .switch_to(ModelSwitchToRequest {
                        context_tier: None,
                        model_capabilities: None,
                        model_id: selection_id.to_string(),
                        reasoning_effort: None,
                        reasoning_summary: None,
                        verbosity: None,
                    })
                    .await
                    .expect("switch to added model");
                let current = session
                    .rpc()
                    .model()
                    .get_current()
                    .await
                    .expect("get current model");
                assert_eq!(current.model_id.as_deref(), Some(selection_id));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_return_empty_completions_when_host_does_not_provide_them() {
    with_e2e_context(
        "rpc_session_state_extras",
        "should_return_empty_completions_when_host_does_not_provide_them",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let result = session
                    .rpc()
                    .completions()
                    .request(CompletionsRequestRequest {
                        offset: 5,
                        text: "Use @ to mention context".to_string(),
                    })
                    .await
                    .expect("request completions");
                assert!(result.items.is_empty());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_report_visibility_as_unsynced_for_local_session() {
    with_e2e_context(
        "rpc_session_state_extras",
        "should_report_visibility_as_unsynced_for_local_session",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let set = session
                    .rpc()
                    .visibility()
                    .set(VisibilitySetRequest {
                        status: SessionVisibilityStatus::Unshared,
                    })
                    .await
                    .expect("set visibility");
                assert!(!set.synced);
                assert!(set.status.is_none());
                assert!(set.share_url.is_none());
                let get = session
                    .rpc()
                    .visibility()
                    .get()
                    .await
                    .expect("get visibility");
                assert!(!get.synced);
                assert!(get.status.is_none());
                assert!(get.share_url.is_none());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_get_context_attribution_and_heaviest_messages_after_turn() {
    with_e2e_context(
        "rpc_session_state_extras",
        "should_get_context_attribution_and_heaviest_messages_after_turn",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait("Say CONTEXT_METADATA_OK exactly.")
                    .await
                    .expect("send prompt")
                    .expect("assistant message");
                assert!(assistant_message_content(&answer).contains("CONTEXT_METADATA_OK"));

                let attribution = session
                    .rpc()
                    .metadata()
                    .get_context_attribution()
                    .await
                    .expect("get context attribution");
                assert!(attribution.context_attribution.is_some());
                let heaviest = session
                    .rpc()
                    .metadata()
                    .get_context_heaviest_messages(MetadataContextHeaviestMessagesRequest {
                        limit: Some(5),
                    })
                    .await
                    .expect("get heaviest messages");
                assert!(heaviest.total_tokens >= 0);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_update_and_clear_live_subagent_settings() {
    with_e2e_context(
        "rpc_session_state_extras",
        "should_update_and_clear_live_subagent_settings",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                session
                    .rpc()
                    .tools()
                    .update_subagent_settings(UpdateSubagentSettingsRequest {
                        subagents: Some(UpdateSubagentSettingsRequestSubagents {
                            agents: Some(HashMap::from([(
                                "general-purpose".to_string(),
                                SubagentSettingsEntry {
                                    context_tier: Some(
                                        SubagentSettingsEntryContextTier::LongContext,
                                    ),
                                    effort_level: Some("low".to_string()),
                                    model: Some("gpt-5-mini".to_string()),
                                },
                            )])),
                            disabled_subagents: Some(vec!["legacy-agent".to_string()]),
                            max_concurrency: None,
                            max_depth: None,
                        }),
                    })
                    .await
                    .expect("update subagent settings");
                session
                    .rpc()
                    .tools()
                    .update_subagent_settings(UpdateSubagentSettingsRequest { subagents: None })
                    .await
                    .expect("clear subagent settings");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_reload_session_plugins() {
    with_e2e_context(
        "rpc_session_state_extras",
        "should_reload_session_plugins",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                session
                    .rpc()
                    .plugins()
                    .reload()
                    .await
                    .expect("reload session plugins");

                let plugins = session
                    .rpc()
                    .plugins()
                    .list()
                    .await
                    .expect("list session plugins");
                assert!(
                    plugins
                        .plugins
                        .iter()
                        .all(|plugin| !plugin.name.trim().is_empty())
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}
