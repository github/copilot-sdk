use std::collections::HashMap;

use github_copilot_sdk::generated::api_types::{
    ModelCapabilitiesOverride, ModelCapabilitiesOverrideSupports,
};
use github_copilot_sdk::generated::session_events::{SessionEventType, SessionStartData};
use github_copilot_sdk::{
    Attachment, MessageOptions, ProviderConfig, ResumeSessionConfig, SessionConfig, SessionId,
    SetModelOptions, SystemMessageConfig,
};

use super::support::{
    assistant_message_content, get_system_message, get_tool_names, with_e2e_context,
};

const PROVIDER_HEADER_NAME: &str = "x-copilot-sdk-provider-header";
const CLIENT_NAME: &str = "rust-public-surface-client";
const VIEW_IMAGE_PROMPT: &str =
    "Use the view tool to look at the file test.png and describe what you see";
const PNG_1X1_BASE64: &str = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

#[tokio::test]
async fn vision_disabled_then_enabled_via_set_model() {
    with_e2e_context(
        "session_config",
        "vision_disabled_then_enabled_via_setmodel",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                std::fs::write(
                    ctx.work_dir().join("test.png"),
                    decode_base64(PNG_1X1_BASE64),
                )
                .expect("write image");

                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_model("claude-sonnet-4.5")
                            .with_model_capabilities(vision_capabilities(false)),
                    )
                    .await
                    .expect("create session");

                session
                    .send_and_wait(VIEW_IMAGE_PROMPT)
                    .await
                    .expect("send");
                let traffic_after_t1 = ctx.exchanges();
                assert!(
                    !has_image_url_content(&traffic_after_t1),
                    "expected no image_url content when vision is disabled"
                );

                session
                    .set_model(
                        "claude-sonnet-4.5",
                        Some(
                            SetModelOptions::default()
                                .with_model_capabilities(vision_capabilities(true)),
                        ),
                    )
                    .await
                    .expect("set model");

                session
                    .send_and_wait(VIEW_IMAGE_PROMPT)
                    .await
                    .expect("send");
                let traffic_after_t2 = ctx.exchanges();
                let new_exchanges = &traffic_after_t2[traffic_after_t1.len()..];
                assert!(!new_exchanges.is_empty());
                assert!(
                    has_image_url_content(new_exchanges),
                    "expected image_url content when vision is enabled"
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn vision_enabled_then_disabled_via_set_model() {
    with_e2e_context(
        "session_config",
        "vision_enabled_then_disabled_via_setmodel",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                std::fs::write(
                    ctx.work_dir().join("test.png"),
                    decode_base64(PNG_1X1_BASE64),
                )
                .expect("write image");

                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_model("claude-sonnet-4.5")
                            .with_model_capabilities(vision_capabilities(true)),
                    )
                    .await
                    .expect("create session");

                session
                    .send_and_wait(VIEW_IMAGE_PROMPT)
                    .await
                    .expect("send");
                let traffic_after_t1 = ctx.exchanges();
                assert!(
                    has_image_url_content(&traffic_after_t1),
                    "expected image_url content when vision is enabled"
                );

                session
                    .set_model(
                        "claude-sonnet-4.5",
                        Some(
                            SetModelOptions::default()
                                .with_model_capabilities(vision_capabilities(false)),
                        ),
                    )
                    .await
                    .expect("set model");

                session
                    .send_and_wait(VIEW_IMAGE_PROMPT)
                    .await
                    .expect("send");
                let traffic_after_t2 = ctx.exchanges();
                let new_exchanges = &traffic_after_t2[traffic_after_t1.len()..];
                assert!(!new_exchanges.is_empty());
                assert!(
                    !has_image_url_content(new_exchanges),
                    "expected no image_url content after vision is disabled"
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_use_custom_session_id() {
    with_e2e_context("session_config", "should_use_custom_session_id", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let requested_session_id = SessionId::from("11111111-2222-3333-4444-555555555555");
            let client = ctx.start_client().await;
            let session = client
                .create_session(
                    ctx.approve_all_session_config()
                        .with_session_id(requested_session_id.clone()),
                )
                .await
                .expect("create session");

            assert_eq!(session.id(), &requested_session_id);
            let messages = session.get_messages().await.expect("messages");
            let start_event = messages
                .iter()
                .find(|event| event.parsed_type() == SessionEventType::SessionStart)
                .expect("session.start event");
            let data = start_event
                .typed_data::<SessionStartData>()
                .expect("session.start data");
            assert_eq!(data.session_id, requested_session_id);

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_apply_reasoning_effort_on_session_create() {
    with_e2e_context(
        "session_config",
        "should_apply_reasoning_effort_on_session_create",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        approve_all_without_token()
                            .with_model("custom-reasoning-model")
                            .with_provider(provider(ctx.proxy_url(), "create-reasoning"))
                            .with_reasoning_effort("high"),
                    )
                    .await
                    .expect("create session");

                let start_event = session
                    .get_messages()
                    .await
                    .expect("messages")
                    .into_iter()
                    .find(|event| event.parsed_type() == SessionEventType::SessionStart)
                    .expect("session.start event");
                let data = start_event
                    .typed_data::<SessionStartData>()
                    .expect("session.start data");
                assert_eq!(
                    data.selected_model.as_deref(),
                    Some("custom-reasoning-model")
                );
                assert_eq!(data.reasoning_effort.as_deref(), Some("high"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_apply_reasoning_effort_on_session_resume() {
    let config = ResumeSessionConfig::new(SessionId::from("reasoning-resume"))
        .with_reasoning_effort("medium");

    assert_eq!(config.reasoning_effort.as_deref(), Some("medium"));
}

#[tokio::test]
async fn should_apply_all_reasoning_effort_values_on_session_create() {
    with_e2e_context(
        "session_config",
        "should_apply_all_reasoning_effort_values_on_session_create",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;

                for effort in ["low", "medium", "high"] {
                    let session = client
                        .create_session(
                            approve_all_without_token()
                                .with_model("custom-reasoning-model")
                                .with_provider(provider(
                                    ctx.proxy_url(),
                                    &format!("reasoning-{effort}"),
                                ))
                                .with_reasoning_effort(effort),
                        )
                        .await
                        .unwrap_or_else(|err| panic!("create session with effort {effort}: {err}"));

                    let start_event = session
                        .get_messages()
                        .await
                        .expect("messages")
                        .into_iter()
                        .find(|event| event.parsed_type() == SessionEventType::SessionStart)
                        .expect("session.start event");
                    let data = start_event
                        .typed_data::<SessionStartData>()
                        .expect("session.start data");
                    assert_eq!(
                        data.selected_model.as_deref(),
                        Some("custom-reasoning-model")
                    );
                    assert_eq!(data.reasoning_effort.as_deref(), Some(effort));

                    session.disconnect().await.expect("disconnect session");
                }

                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_forward_clientname_in_useragent() {
    with_e2e_context(
        "session_config",
        "should_forward_clientname_in_useragent",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_client_name(CLIENT_NAME),
                    )
                    .await
                    .expect("create session");

                session.send_and_wait("What is 1+1?").await.expect("send");

                let exchange = only_exchange(ctx.exchanges());
                assert_header_contains(&exchange, "user-agent", CLIENT_NAME);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_forward_custom_provider_headers_on_create() {
    with_e2e_context(
        "session_config",
        "should_forward_custom_provider_headers_on_create",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        approve_all_without_token()
                            .with_model("claude-sonnet-4.5")
                            .with_provider(provider(ctx.proxy_url(), "create-provider-header")),
                    )
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait("What is 1+1?")
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(assistant_message_content(&answer).contains('2'));

                let exchange = only_exchange(ctx.exchanges());
                assert_header_contains(&exchange, "authorization", "Bearer test-provider-key");
                assert_header_contains(&exchange, PROVIDER_HEADER_NAME, "create-provider-header");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_forward_custom_provider_headers_on_resume() {
    with_e2e_context(
        "session_config",
        "should_forward_custom_provider_headers_on_resume",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session1 = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create first session");
                let session2 = client
                    .resume_session(
                        ResumeSessionConfig::new(session1.id().clone())
                            .with_handler(std::sync::Arc::new(
                                github_copilot_sdk::handler::ApproveAllHandler,
                            ))
                            .with_model_capabilities(vision_capabilities(false))
                            .with_provider(
                                provider(ctx.proxy_url(), "resume-provider-header")
                                    .with_model_id("claude-sonnet-4.5"),
                            ),
                    )
                    .await
                    .expect("resume session");

                let answer = session2
                    .send_and_wait("What is 2+2?")
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(assistant_message_content(&answer).contains('4'));

                let exchange = only_exchange(ctx.exchanges());
                assert_header_contains(&exchange, "authorization", "Bearer test-provider-key");
                assert_header_contains(&exchange, PROVIDER_HEADER_NAME, "resume-provider-header");

                session2.disconnect().await.expect("disconnect resumed");
                session1.disconnect().await.expect("disconnect original");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_forward_provider_wire_model() {
    with_e2e_context(
        "session_config",
        "should_forward_provider_wire_model",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        approve_all_without_token()
                            .with_model("claude-sonnet-4.5")
                            .with_provider(
                                ProviderConfig::new(ctx.proxy_url())
                                    .with_provider_type("openai")
                                    .with_api_key("test-provider-key")
                                    .with_wire_model("test-wire-model")
                                    .with_max_output_tokens(1024),
                            ),
                    )
                    .await
                    .expect("create session");

                session.send_and_wait("What is 1+1?").await.expect("send");

                let exchange = only_exchange(ctx.exchanges());
                assert_eq!(request_model(&exchange), Some("test-wire-model"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_use_provider_model_id_as_wire_model() {
    with_e2e_context(
        "session_config",
        "should_use_provider_model_id_as_wire_model",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        approve_all_without_token().with_provider(
                            ProviderConfig::new(ctx.proxy_url())
                                .with_provider_type("openai")
                                .with_api_key("test-provider-key")
                                .with_model_id("claude-sonnet-4.5"),
                        ),
                    )
                    .await
                    .expect("create session");

                session.send_and_wait("What is 1+1?").await.expect("send");

                let exchange = only_exchange(ctx.exchanges());
                assert_eq!(request_model(&exchange), Some("claude-sonnet-4.5"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_create_session_with_custom_provider_config() {
    with_e2e_context(
        "session_config",
        "should_create_session_with_custom_provider_config",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(approve_all_without_token().with_provider(
                        ProviderConfig::new("https://api.example.com/v1").with_api_key("test-key"),
                    ))
                    .await
                    .expect("create session");

                assert!(!session.id().as_ref().is_empty());
                let _ = session.disconnect().await;
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_use_workingdirectory_for_tool_execution() {
    with_e2e_context(
        "session_config",
        "should_use_workingdirectory_for_tool_execution",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let sub_dir = ctx.work_dir().join("subproject");
                std::fs::create_dir_all(&sub_dir).expect("create subproject");
                std::fs::write(sub_dir.join("marker.txt"), "I am in the subdirectory")
                    .expect("write marker");

                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_working_directory(sub_dir),
                    )
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait("Read the file marker.txt and tell me what it says")
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(assistant_message_content(&answer).contains("subdirectory"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_apply_workingdirectory_on_session_resume() {
    with_e2e_context(
        "session_config",
        "should_apply_workingdirectory_on_session_resume",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let sub_dir = ctx.work_dir().join("resume-subproject");
                std::fs::create_dir_all(&sub_dir).expect("create resume subproject");
                std::fs::write(
                    sub_dir.join("resume-marker.txt"),
                    "I am in the resume working directory",
                )
                .expect("write resume marker");

                let client = ctx.start_client().await;
                let session1 = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create first session");
                let session2 = client
                    .resume_session(
                        ResumeSessionConfig::new(session1.id().clone())
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                            .with_handler(std::sync::Arc::new(
                                github_copilot_sdk::handler::ApproveAllHandler,
                            ))
                            .with_working_directory(sub_dir),
                    )
                    .await
                    .expect("resume session");

                let answer = session2
                    .send_and_wait("Read the file resume-marker.txt and tell me what it says")
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(assistant_message_content(&answer).contains("resume working directory"));

                session2.disconnect().await.expect("disconnect resumed");
                session1.disconnect().await.expect("disconnect original");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_apply_systemmessage_on_session_resume() {
    with_e2e_context(
        "session_config",
        "should_apply_systemmessage_on_session_resume",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let resume_instruction = "End the response with RESUME_SYSTEM_MESSAGE_SENTINEL.";
                let client = ctx.start_client().await;
                let session1 = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create first session");
                let session2 = client
                    .resume_session(
                        ResumeSessionConfig::new(session1.id().clone())
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                            .with_handler(std::sync::Arc::new(
                                github_copilot_sdk::handler::ApproveAllHandler,
                            ))
                            .with_system_message(
                                SystemMessageConfig::new()
                                    .with_mode("append")
                                    .with_content(resume_instruction),
                            ),
                    )
                    .await
                    .expect("resume session");

                let answer = session2
                    .send_and_wait("What is 1+1?")
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(
                    assistant_message_content(&answer).contains("RESUME_SYSTEM_MESSAGE_SENTINEL")
                );

                let exchange = only_exchange(ctx.exchanges());
                assert!(get_system_message(&exchange).contains(resume_instruction));

                session2.disconnect().await.expect("disconnect resumed");
                session1.disconnect().await.expect("disconnect original");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_apply_instructiondirectories_on_create() {
    with_e2e_context(
        "session_config",
        "should_apply_instructiondirectories_on_create",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let project_dir = ctx.work_dir().join("instruction-create-project");
                let instruction_dir = ctx.work_dir().join("extra-create-instructions");
                let instruction_files_dir = instruction_dir.join(".github").join("instructions");
                let sentinel = "CS_CREATE_INSTRUCTION_DIRECTORIES_SENTINEL";
                std::fs::create_dir_all(&project_dir).expect("create project dir");
                std::fs::create_dir_all(&instruction_files_dir).expect("create instruction dir");
                std::fs::write(
                    instruction_files_dir.join("extra.instructions.md"),
                    format!("Always include {sentinel}."),
                )
                .expect("write instructions");

                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_working_directory(project_dir)
                            .with_instruction_directories([instruction_dir]),
                    )
                    .await
                    .expect("create session");

                session.send_and_wait("What is 1+1?").await.expect("send");

                let exchange = only_exchange(ctx.exchanges());
                assert!(get_system_message(&exchange).contains(sentinel));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_apply_instructiondirectories_on_resume() {
    with_e2e_context(
        "session_config",
        "should_apply_instructiondirectories_on_resume",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let project_dir = ctx.work_dir().join("instruction-resume-project");
                let instruction_dir = ctx.work_dir().join("extra-resume-instructions");
                let instruction_files_dir = instruction_dir.join(".github").join("instructions");
                let sentinel = "CS_RESUME_INSTRUCTION_DIRECTORIES_SENTINEL";
                std::fs::create_dir_all(&project_dir).expect("create project dir");
                std::fs::create_dir_all(&instruction_files_dir).expect("create instruction dir");
                std::fs::write(
                    instruction_files_dir.join("extra.instructions.md"),
                    format!("Always include {sentinel}."),
                )
                .expect("write instructions");

                let client = ctx.start_client().await;
                let session1 = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_working_directory(project_dir.clone()),
                    )
                    .await
                    .expect("create first session");
                let session2 = client
                    .resume_session(
                        ResumeSessionConfig::new(session1.id().clone())
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                            .with_handler(std::sync::Arc::new(
                                github_copilot_sdk::handler::ApproveAllHandler,
                            ))
                            .with_working_directory(project_dir)
                            .with_instruction_directories([instruction_dir]),
                    )
                    .await
                    .expect("resume session");

                session2.send_and_wait("What is 1+1?").await.expect("send");

                let exchange = only_exchange(ctx.exchanges());
                assert!(get_system_message(&exchange).contains(sentinel));

                session2.disconnect().await.expect("disconnect resumed");
                session1.disconnect().await.expect("disconnect original");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_apply_availabletools_on_session_resume() {
    with_e2e_context(
        "session_config",
        "should_apply_availabletools_on_session_resume",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session1 = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create first session");
                let session2 = client
                    .resume_session(
                        ResumeSessionConfig::new(session1.id().clone())
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                            .with_handler(std::sync::Arc::new(
                                github_copilot_sdk::handler::ApproveAllHandler,
                            ))
                            .with_available_tools(["view"]),
                    )
                    .await
                    .expect("resume session");

                session2.send_and_wait("What is 1+1?").await.expect("send");

                let exchange = only_exchange(ctx.exchanges());
                assert_eq!(get_tool_names(&exchange), vec!["view".to_string()]);

                session2.disconnect().await.expect("disconnect resumed");
                session1.disconnect().await.expect("disconnect original");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_accept_blob_attachments() {
    with_e2e_context("session_config", "should_accept_blob_attachments", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            std::fs::write(
                ctx.work_dir().join("pixel.png"),
                decode_base64(PNG_1X1_BASE64),
            )
            .expect("write pixel");

            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");

            session
                .send_and_wait(
                    MessageOptions::new("What color is this pixel? Reply in one word.")
                        .with_attachments(vec![Attachment::Blob {
                            data: PNG_1X1_BASE64.to_string(),
                            mime_type: "image/png".to_string(),
                            display_name: Some("pixel.png".to_string()),
                        }]),
                )
                .await
                .expect("send");

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_accept_message_attachments() {
    with_e2e_context(
        "session_config",
        "should_accept_message_attachments",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let attached_path = ctx.work_dir().join("attached.txt");
                std::fs::write(&attached_path, "This file is attached").expect("write attachment");

                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                session
                    .send_and_wait(
                        MessageOptions::new("Summarize the attached file").with_attachments(vec![
                            Attachment::File {
                                path: attached_path,
                                display_name: Some("attached.txt".to_string()),
                                line_range: None,
                            },
                        ]),
                    )
                    .await
                    .expect("send");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

fn provider(proxy_url: &str, header_value: &str) -> ProviderConfig {
    ProviderConfig::new(proxy_url)
        .with_provider_type("openai")
        .with_api_key("test-provider-key")
        .with_headers(HashMap::from([(
            PROVIDER_HEADER_NAME.to_string(),
            header_value.to_string(),
        )]))
}

fn approve_all_without_token() -> SessionConfig {
    SessionConfig::default().with_handler(std::sync::Arc::new(
        github_copilot_sdk::handler::ApproveAllHandler,
    ))
}

fn vision_capabilities(vision: bool) -> ModelCapabilitiesOverride {
    ModelCapabilitiesOverride {
        limits: None,
        supports: Some(ModelCapabilitiesOverrideSupports {
            reasoning_effort: None,
            vision: Some(vision),
        }),
    }
}

fn only_exchange(exchanges: Vec<serde_json::Value>) -> serde_json::Value {
    assert_eq!(exchanges.len(), 1, "expected exactly one exchange");
    exchanges.into_iter().next().expect("exchange")
}

fn has_image_url_content(exchanges: &[serde_json::Value]) -> bool {
    exchanges
        .iter()
        .filter_map(|exchange| exchange.get("request"))
        .filter_map(|request| request.get("messages"))
        .filter_map(serde_json::Value::as_array)
        .flatten()
        .filter(|message| {
            message
                .get("role")
                .and_then(serde_json::Value::as_str)
                .is_some_and(|role| role == "user")
        })
        .filter_map(|message| message.get("content"))
        .filter_map(serde_json::Value::as_array)
        .flatten()
        .any(|part| {
            part.get("type")
                .and_then(serde_json::Value::as_str)
                .is_some_and(|part_type| part_type == "image_url")
        })
}

fn request_model(exchange: &serde_json::Value) -> Option<&str> {
    exchange
        .get("request")
        .and_then(|request| request.get("model"))
        .and_then(serde_json::Value::as_str)
}

fn assert_header_contains(exchange: &serde_json::Value, name: &str, expected_value: &str) {
    let headers = exchange
        .get("requestHeaders")
        .and_then(serde_json::Value::as_object)
        .expect("requestHeaders");
    let actual = headers
        .iter()
        .find_map(|(key, value)| key.eq_ignore_ascii_case(name).then(|| header_value(value)))
        .unwrap_or_else(|| panic!("missing header {name}; actual headers: {headers:?}"));
    assert!(
        actual.contains(expected_value),
        "header {name} value {actual:?} did not contain {expected_value:?}"
    );
}

fn header_value(value: &serde_json::Value) -> String {
    match value {
        serde_json::Value::String(value) => value.clone(),
        serde_json::Value::Array(values) => values
            .iter()
            .map(header_value)
            .collect::<Vec<_>>()
            .join(","),
        other => other.to_string(),
    }
}

fn decode_base64(input: &str) -> Vec<u8> {
    let mut output = Vec::new();
    let mut buffer = 0u32;
    let mut bits = 0u8;
    for byte in input.bytes().filter(|byte| !byte.is_ascii_whitespace()) {
        let value = match byte {
            b'A'..=b'Z' => byte - b'A',
            b'a'..=b'z' => byte - b'a' + 26,
            b'0'..=b'9' => byte - b'0' + 52,
            b'+' => 62,
            b'/' => 63,
            b'=' => break,
            _ => panic!("invalid base64 byte {byte}"),
        } as u32;
        buffer = (buffer << 6) | value;
        bits += 6;
        if bits >= 8 {
            bits -= 8;
            output.push(((buffer >> bits) & 0xff) as u8);
        }
    }
    output
}
