use std::collections::HashMap;
use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::transforms::{SystemMessageTransform, TransformContext};
use github_copilot_sdk::{SectionOverride, SessionConfig, SystemMessageConfig};
use tokio::sync::mpsc;

use super::support::{DEFAULT_TEST_TOKEN, get_system_message, recv_with_timeout, with_e2e_context};

#[tokio::test]
async fn should_invoke_transform_callbacks_with_section_content() {
    with_e2e_context(
        "system_message_transform",
        "should_invoke_transform_callbacks_with_section_content",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                std::fs::write(ctx.work_dir().join("test.txt"), "Hello transform!")
                    .expect("write test file");
                let (section_tx, mut section_rx) = mpsc::unbounded_channel();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_handler(Arc::new(github_copilot_sdk::handler::ApproveAllHandler))
                            .with_transform(Arc::new(RecordingTransform {
                                section_ids: vec!["identity", "tone"],
                                suffix: None,
                                section_tx,
                            })),
                    )
                    .await
                    .expect("create session");

                session
                    .send_and_wait("Read the contents of test.txt and tell me what it says")
                    .await
                    .expect("send");

                let first = recv_with_timeout(&mut section_rx, "first transform").await;
                let second = recv_with_timeout(&mut section_rx, "second transform").await;
                assert!(first.1 > 0);
                assert!(second.1 > 0);
                let sections = [first.0, second.0];
                assert!(sections.contains(&"identity".to_string()));
                assert!(sections.contains(&"tone".to_string()));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_apply_transform_modifications_to_section_content() {
    with_e2e_context(
        "system_message_transform",
        "should_apply_transform_modifications_to_section_content",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                std::fs::write(ctx.work_dir().join("hello.txt"), "Hello!")
                    .expect("write hello file");
                let (section_tx, _section_rx) = mpsc::unbounded_channel();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_handler(Arc::new(github_copilot_sdk::handler::ApproveAllHandler))
                            .with_transform(Arc::new(RecordingTransform {
                                section_ids: vec!["identity"],
                                suffix: Some("\nAlways end your reply with TRANSFORM_MARKER"),
                                section_tx,
                            })),
                    )
                    .await
                    .expect("create session");

                session
                    .send_and_wait("Read the contents of hello.txt")
                    .await
                    .expect("send");

                let exchanges = ctx.exchanges();
                assert!(!exchanges.is_empty());
                assert!(get_system_message(&exchanges[0]).contains("TRANSFORM_MARKER"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_work_with_static_overrides_and_transforms_together() {
    with_e2e_context(
        "system_message_transform",
        "should_work_with_static_overrides_and_transforms_together",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                std::fs::write(ctx.work_dir().join("combo.txt"), "Combo test!")
                    .expect("write combo file");
                let (section_tx, mut section_rx) = mpsc::unbounded_channel();
                let mut sections = HashMap::new();
                sections.insert(
                    "safety".to_string(),
                    SectionOverride {
                        action: Some("remove".to_string()),
                        content: None,
                    },
                );
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_handler(Arc::new(github_copilot_sdk::handler::ApproveAllHandler))
                            .with_system_message(
                                SystemMessageConfig::new()
                                    .with_mode("customize")
                                    .with_sections(sections),
                            )
                            .with_transform(Arc::new(RecordingTransform {
                                section_ids: vec!["identity"],
                                suffix: None,
                                section_tx,
                            })),
                    )
                    .await
                    .expect("create session");

                session
                    .send_and_wait("Read the contents of combo.txt and tell me what it says")
                    .await
                    .expect("send");

                let (section, content_len) =
                    recv_with_timeout(&mut section_rx, "identity transform").await;
                assert_eq!(section, "identity");
                assert!(content_len > 0);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

struct RecordingTransform {
    section_ids: Vec<&'static str>,
    suffix: Option<&'static str>,
    section_tx: mpsc::UnboundedSender<(String, usize)>,
}

#[async_trait]
impl SystemMessageTransform for RecordingTransform {
    fn section_ids(&self) -> Vec<String> {
        self.section_ids
            .iter()
            .map(|section| (*section).to_string())
            .collect()
    }

    async fn transform_section(
        &self,
        section_id: &str,
        content: &str,
        _ctx: TransformContext,
    ) -> Option<String> {
        let _ = self
            .section_tx
            .send((section_id.to_string(), content.len()));
        Some(match self.suffix {
            Some(suffix) => format!("{content}{suffix}"),
            None => content.to_string(),
        })
    }
}
