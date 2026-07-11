use std::collections::HashMap;

use github_copilot_sdk::{SectionOverride, SystemMessageConfig};

use super::support::{assistant_message_content, with_e2e_context};

#[tokio::test]
async fn should_use_replaced_identity_section_in_response() {
    with_e2e_context(
        "system_message_sections",
        "should_use_replaced_identity_section_in_response",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let mut sections = HashMap::new();
                sections.insert(
                    "identity".to_string(),
                    SectionOverride {
                        action: Some("replace".to_string()),
                        content: Some(
                            "You are a helpful gardening assistant called Botanica. \
                             You only answer questions about plants and gardening."
                                .to_string(),
                        ),
                    },
                );
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config().with_system_message(
                            SystemMessageConfig::new()
                                .with_mode("customize")
                                .with_sections(sections),
                        ),
                    )
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait("Who are you?")
                    .await
                    .expect("send")
                    .expect("assistant message");
                let content = assistant_message_content(&answer).to_lowercase();
                assert!(
                    content.contains("botanica")
                        || content.contains("garden")
                        || content.contains("plant"),
                    "Expected response to reflect the replaced identity section, but got: {}",
                    assistant_message_content(&answer)
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_use_replaced_preamble_section_in_response() {
    with_e2e_context(
        "system_message_sections",
        "should_use_replaced_preamble_section_in_response",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let mut sections = HashMap::new();
                sections.insert(
                    "preamble".to_string(),
                    SectionOverride {
                        action: Some("replace".to_string()),
                        content: Some(
                            "You are a helpful gardening assistant called Botanica. \
                             You only answer questions about plants and gardening."
                                .to_string(),
                        ),
                    },
                );
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config().with_system_message(
                            SystemMessageConfig::new()
                                .with_mode("customize")
                                .with_sections(sections),
                        ),
                    )
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait("Who are you?")
                    .await
                    .expect("send")
                    .expect("assistant message");
                let content = assistant_message_content(&answer).to_lowercase();
                assert!(
                    content.contains("botanica")
                        || content.contains("garden")
                        || content.contains("plant"),
                    "Expected response to reflect the replaced preamble section, but got: {}",
                    assistant_message_content(&answer)
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}
