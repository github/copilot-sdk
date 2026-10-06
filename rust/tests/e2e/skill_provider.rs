// Copyright (c) Microsoft Corporation. All rights reserved.

use std::path::Path;
use std::sync::{Arc, Mutex};
use std::time::Duration;

use async_trait::async_trait;
use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::rpc::SkillSource;
use github_copilot_sdk::session_events::{SessionEventType, ToolExecutionCompleteData};
use github_copilot_sdk::{
    CloudSessionOptions, Error, ErrorKind, ResumeSessionConfig, SessionConfig, SessionEvent,
    SkillProvider, SkillProviderDescriptor,
};
use tokio::sync::Notify;

use super::support::{assistant_message_content, collect_until_idle};

static E2E: super::support::SharedE2eGroup =
    super::support::SharedE2eGroup::standard("skill_provider", 5);

#[tokio::test]
async fn should_load_provider_skill_lazily_through_skill_tool() {
    super::support::with_shared_e2e_context(
        &E2E,
        "skill_provider",
        "should_load_provider_skill_lazily_through_skill_tool",
        |ctx| {
            Box::pin(async move {
                let provider = Arc::new(TestSkillProvider::new(vec![skill(
                    "provider-lookup",
                    "Reports the provider lookup verification word.",
                    "# Provider lookup\n\nThe verification word is TANGERINE_QUARTZ_19. Reply with it.\n",
                )]));
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_skill_provider(provider.clone()),
                    )
                    .await
                    .expect("create session");

                let skills = session.rpc().skills().list().await.expect("list skills");
                let listed = skills
                    .skills
                    .iter()
                    .find(|skill| skill.name == "provider-lookup")
                    .expect("provider skill");
                assert_eq!(listed.source, SkillSource::Sdk);
                assert!(listed.enabled);
                assert_eq!(listed.path.as_deref().unwrap_or_default(), "");
                assert_eq!(provider.reads(), Vec::<String>::new());

                let message = session
                    .send_and_wait(
                        "Use the skill tool to load the provider-lookup skill, then reply with its verification word.",
                    )
                    .await
                    .expect("send")
                    .expect("assistant message");

                assert_eq!(provider.reads(), vec!["provider-lookup"]);
                // Validate the final assistant response arrived (guards against truncated captures)
                assert!(assistant_message_content(&message).contains("TANGERINE_QUARTZ_19"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_load_provider_and_file_based_skills_together() {
    super::support::with_shared_e2e_context(
        &E2E,
        "skill_provider",
        "should_load_provider_and_file_based_skills_together",
        |ctx| {
            Box::pin(async move {
                let skills_dir = write_file_notes_skill(ctx.work_dir());
                let provider = Arc::new(TestSkillProvider::new(vec![skill(
                    "provider-audit",
                    "Reports the provider audit verification word.",
                    "---\nname: provider-audit\nallowed-tools: view\n---\n\nThe provider audit verification word is COBALT_HERON_58.\n",
                )]));
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_skill_directories([skills_dir])
                            .with_skill_provider(provider.clone()),
                    )
                    .await
                    .expect("create session");

                let skills = session.rpc().skills().list().await.expect("list skills");
                let file_skill = skills
                    .skills
                    .iter()
                    .find(|skill| skill.name == "file-notes")
                    .expect("file skill");
                let provider_skill = skills
                    .skills
                    .iter()
                    .find(|skill| skill.name == "provider-audit")
                    .expect("provider skill");
                assert_ne!(file_skill.source, SkillSource::Sdk);
                assert!(file_skill.path.as_deref().is_some_and(|path| !path.is_empty()));
                assert_eq!(provider_skill.source, SkillSource::Sdk);

                let message = session
                    .send_and_wait(
                        "Use the skill tool to load the file-notes skill and the provider-audit skill, then reply with both verification words.",
                    )
                    .await
                    .expect("send")
                    .expect("assistant message");

                assert_eq!(provider.reads(), vec!["provider-audit"]);
                // Validate the final assistant response arrived (guards against truncated captures)
                let content = assistant_message_content(&message);
                assert!(content.contains("MAPLE_FALCON_27"));
                assert!(content.contains("COBALT_HERON_58"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_rebind_skill_provider_on_resume() {
    super::support::with_shared_e2e_context(
        &E2E,
        "skill_provider",
        "should_rebind_skill_provider_on_resume",
        |ctx| {
            Box::pin(async move {
                let original = Arc::new(TestSkillProvider::new(vec![skill(
                    "rebind-check",
                    "Reports the rebind verification word.",
                    "The rebind verification word is AMBER_ALPHA_11.\n",
                )]));
                let replacement = Arc::new(TestSkillProvider::new(vec![skill(
                    "rebind-check",
                    "Reports the rebind verification word.",
                    "The rebind verification word is BRONZE_BETA_22.\n",
                )]));
                let client = ctx.start_client().await;
                let first = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_skill_provider(original.clone()),
                    )
                    .await
                    .expect("create session");
                let session_id = first.id().clone();
                let ready = first
                    .send_and_wait(
                        "Without using any tools or skills, reply with exactly REBIND_READY.",
                    )
                    .await
                    .expect("send readiness turn")
                    .expect("assistant message");
                assert!(assistant_message_content(&ready).contains("REBIND_READY"));
                first.disconnect().await.expect("disconnect first session");
                assert_eq!(original.reads(), Vec::<String>::new());
                let original_calls_before_resume = original.calls().len();

                let session = client
                    .resume_session(
                        ResumeSessionConfig::new(session_id)
                            .with_permission_handler(Arc::new(ApproveAllHandler))
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                            .with_skill_provider(replacement.clone()),
                    )
                    .await
                    .expect("resume session");

                let message = session
                    .send_and_wait(
                        "Use the skill tool to load the rebind-check skill, then reply with its verification word.",
                    )
                    .await
                    .expect("send")
                    .expect("assistant message");

                assert_eq!(replacement.reads(), vec!["rebind-check"]);
                assert_eq!(original.calls().len(), original_calls_before_resume);
                // Validate the final assistant response arrived (guards against truncated captures)
                let content = assistant_message_content(&message);
                assert!(content.contains("BRONZE_BETA_22"));
                assert!(!content.contains("AMBER_ALPHA_11"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_report_provider_read_failure_without_leaking_details() {
    super::support::with_shared_e2e_context(
        &E2E,
        "skill_provider",
        "should_report_provider_read_failure_without_leaking_details",
        |ctx| {
            Box::pin(async move {
                let secret = "PROVIDER_SECRET_7F3A9C";
                let provider = Arc::new(TestSkillProvider::new(vec![ProvidedSkill::new(
                    "broken-lookup",
                    "Reports the broken lookup verification word.",
                    SkillRead::Error(format!("database unavailable: {secret}")),
                )]));
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_skill_provider(provider.clone()),
                    )
                    .await
                    .expect("create session");
                let events = session.subscribe();

                let message = session
                    .send_and_wait(
                        "Use the skill tool to load the broken-lookup skill. If loading fails, reply with exactly LOAD_FAILED.",
                    )
                    .await
                    .expect("send")
                    .expect("assistant message");
                let observed = collect_until_idle(events).await;

                assert!(provider.reads().contains(&"broken-lookup".to_string()));
                let failures = failed_tool_executions(&observed);
                assert_eq!(failures.len(), 1, "expected one failed tool execution");
                assert_no_secret(&observed, secret);
                // Validate the final assistant response arrived (guards against truncated captures)
                assert!(assistant_message_content(&message).contains("LOAD_FAILED"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_report_missing_provider_skill_as_not_found() {
    super::support::with_shared_e2e_context(
        &E2E,
        "skill_provider",
        "should_report_missing_provider_skill_as_not_found",
        |ctx| {
            Box::pin(async move {
                let provider = Arc::new(TestSkillProvider::new(vec![ProvidedSkill::new(
                    "vanished-lookup",
                    "Reports the vanished lookup verification word.",
                    SkillRead::Missing,
                )]));
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_skill_provider(provider.clone()),
                    )
                    .await
                    .expect("create session");
                let events = session.subscribe();

                let message = session
                    .send_and_wait(
                        "Use the skill tool to load the vanished-lookup skill. If loading fails, reply with exactly LOAD_FAILED.",
                    )
                    .await
                    .expect("send")
                    .expect("assistant message");
                let observed = collect_until_idle(events).await;

                assert!(provider.reads().contains(&"vanished-lookup".to_string()));
                let failures = failed_tool_executions(&observed);
                assert_eq!(failures.len(), 1, "expected one failed tool execution");
                let failure = serde_json::to_string(&failures[0]).expect("serialize failure");
                assert!(
                    failure.to_ascii_lowercase().contains("not found"),
                    "expected not found failure, got {failure}"
                );
                // Validate the final assistant response arrived (guards against truncated captures)
                assert!(assistant_message_content(&message).contains("LOAD_FAILED"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_keep_provider_dormant_when_skills_disabled() {
    super::support::with_e2e_context_no_snapshot(|ctx| {
        Box::pin(async move {
            let provider = Arc::new(TestSkillProvider::new(vec![skill(
                "dormant-lookup",
                "Never listed.",
                "Never read.\n",
            )]));
            let client = ctx.start_client().await;
            let session = client
                .create_session(
                    ctx.approve_all_session_config()
                        .with_enable_skills(false)
                        .with_skill_provider(provider.clone()),
                )
                .await
                .expect("create session");

            session
                .rpc()
                .skills()
                .ensure_loaded()
                .await
                .expect("ensure skills loaded");
            let skills = session.rpc().skills().list().await.expect("list skills");

            assert_no_sdk_skills(&skills.skills);
            assert_eq!(provider.calls(), Vec::<String>::new());

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_unbind_provider_when_resumed_without_one() {
    super::support::with_e2e_context_no_snapshot(|ctx| {
        Box::pin(async move {
            let provider = Arc::new(TestSkillProvider::new(vec![skill(
                "unbound-lookup",
                "Reports the unbound lookup word.",
                "Unbound.\n",
            )]));
            let client = ctx.start_client().await;
            let first = client
                .create_session(
                    ctx.approve_all_session_config()
                        .with_skill_provider(provider.clone()),
                )
                .await
                .expect("create session");
            let before = first.rpc().skills().list().await.expect("list skills");
            assert!(
                before
                    .skills
                    .iter()
                    .any(|skill| skill.name == "unbound-lookup")
            );
            let calls_before_resume = provider.calls().len();

            let session = client
                .resume_session(
                    ResumeSessionConfig::new(first.id().clone())
                        .with_permission_handler(Arc::new(ApproveAllHandler))
                        .with_github_token(super::support::DEFAULT_TEST_TOKEN),
                )
                .await
                .expect("resume session");

            session
                .rpc()
                .skills()
                .reload()
                .await
                .expect("reload skills");
            let skills = session.rpc().skills().list().await.expect("list skills");

            assert_no_sdk_skills(&skills.skills);
            assert_eq!(provider.calls().len(), calls_before_resume);

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
            drop(first);
        })
    })
    .await;
}

#[tokio::test]
async fn should_cancel_a_blocked_provider_call_when_the_session_disconnects() {
    super::support::with_e2e_context_no_snapshot(|ctx| {
        Box::pin(async move {
            let entered = Arc::new(Notify::new());
            let dropped = Arc::new(Notify::new());
            let client = ctx.start_client().await;
            let session = client
                .create_session(
                    ctx.approve_all_session_config()
                        .with_skill_provider(Arc::new(BlockingSkillProvider {
                            entered: entered.clone(),
                            dropped: dropped.clone(),
                        })),
                )
                .await
                .expect("create session");

            // The list RPC fails once the binding is removed; only the provider's
            // cancellation matters here.
            let rpc = session.rpc();
            let skills = rpc.skills();
            let list = skills.list();
            tokio::pin!(list);
            tokio::select! {
                _ = entered.notified() => {}
                _ = &mut list => panic!("skills.list finished before the provider was called"),
                _ = tokio::time::sleep(Duration::from_secs(30)) => {
                    panic!("provider list_skills was not called");
                }
            }

            session.disconnect().await.expect("disconnect session");
            tokio::time::timeout(Duration::from_secs(10), dropped.notified())
                .await
                .expect("provider future was not dropped after disconnect");

            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_reject_skill_provider_for_cloud_sessions() {
    super::support::with_e2e_context_no_snapshot(|ctx| {
        Box::pin(async move {
            let provider = Arc::new(TestSkillProvider::new(vec![skill(
                "cloud-lookup",
                "Never listed.",
                "Never read.\n",
            )]));
            let client = ctx.start_client().await;

            let result = client
                .create_session(
                    SessionConfig::default()
                        .with_permission_handler(Arc::new(ApproveAllHandler))
                        .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                        .with_cloud(CloudSessionOptions::default())
                        .with_skill_provider(provider.clone()),
                )
                .await;

            match result {
                Ok(session) => {
                    let _ = session.disconnect().await;
                    panic!("cloud session with skill provider unexpectedly succeeded");
                }
                Err(error) => {
                    assert!(
                        error
                            .to_string()
                            .contains("Skill providers are not supported for cloud sessions."),
                        "unexpected error: {error}"
                    );
                }
            }
            assert_eq!(provider.calls(), Vec::<String>::new());

            client.stop().await.expect("stop client");
        })
    })
    .await;
}

struct ProvidedSkill {
    descriptor: SkillProviderDescriptor,
    read: SkillRead,
}

impl ProvidedSkill {
    fn new(name: &str, description: &str, read: SkillRead) -> Self {
        Self {
            descriptor: descriptor(name, description),
            read,
        }
    }
}

enum SkillRead {
    Markdown(&'static str),
    Missing,
    Error(String),
}

struct TestSkillProvider {
    skills: Vec<ProvidedSkill>,
    calls: Mutex<Vec<String>>,
}

impl TestSkillProvider {
    fn new(skills: Vec<ProvidedSkill>) -> Self {
        Self {
            skills,
            calls: Mutex::new(Vec::new()),
        }
    }

    fn calls(&self) -> Vec<String> {
        self.calls.lock().expect("provider call log").clone()
    }

    fn reads(&self) -> Vec<String> {
        self.calls()
            .into_iter()
            .filter_map(|call| call.strip_prefix("read:").map(str::to_string))
            .collect()
    }

    fn push_call(&self, call: impl Into<String>) {
        self.calls
            .lock()
            .expect("provider call log")
            .push(call.into());
    }
}

#[async_trait]
impl SkillProvider for TestSkillProvider {
    async fn list_skills(&self) -> std::result::Result<Vec<SkillProviderDescriptor>, Error> {
        self.push_call("list");
        Ok(self
            .skills
            .iter()
            .map(|skill| skill.descriptor.clone())
            .collect())
    }

    async fn read_skill(&self, name: &str) -> std::result::Result<Option<String>, Error> {
        self.push_call(format!("read:{name}"));
        let Some(skill) = self
            .skills
            .iter()
            .find(|skill| skill.descriptor.name == name)
        else {
            return Ok(None);
        };

        match &skill.read {
            SkillRead::Markdown(markdown) => Ok(Some((*markdown).to_string())),
            SkillRead::Missing => Ok(None),
            SkillRead::Error(message) => Err(Error::with_message(
                ErrorKind::InvalidConfig,
                message.clone(),
            )),
        }
    }
}

fn skill(name: &str, description: &str, markdown: &'static str) -> ProvidedSkill {
    ProvidedSkill::new(name, description, SkillRead::Markdown(markdown))
}

struct NotifyOnDrop(Arc<Notify>);

impl Drop for NotifyOnDrop {
    fn drop(&mut self) {
        self.0.notify_one();
    }
}

/// Blocks `list_skills` until the SDK drops its future.
struct BlockingSkillProvider {
    entered: Arc<Notify>,
    dropped: Arc<Notify>,
}

#[async_trait]
impl SkillProvider for BlockingSkillProvider {
    async fn list_skills(&self) -> std::result::Result<Vec<SkillProviderDescriptor>, Error> {
        let _dropped = NotifyOnDrop(self.dropped.clone());
        self.entered.notify_one();
        std::future::pending().await
    }

    async fn read_skill(&self, _name: &str) -> std::result::Result<Option<String>, Error> {
        Ok(None)
    }
}

fn descriptor(name: &str, description: &str) -> SkillProviderDescriptor {
    SkillProviderDescriptor {
        name: name.to_string(),
        description: description.to_string(),
        ..Default::default()
    }
}

fn write_file_notes_skill(work_dir: &Path) -> std::path::PathBuf {
    let skills_dir = work_dir.join("file-skills");
    let skill_dir = skills_dir.join("file-notes");
    std::fs::create_dir_all(&skill_dir).expect("create skill directory");
    std::fs::write(
        skill_dir.join("SKILL.md"),
        "---\nname: file-notes\ndescription: Reports the file notes verification word.\n---\n\nThe file notes verification word is MAPLE_FALCON_27.\n",
    )
    .expect("write file skill");
    skills_dir
}

fn failed_tool_executions(events: &[SessionEvent]) -> Vec<ToolExecutionCompleteData> {
    events
        .iter()
        .filter(|event| event.parsed_type() == SessionEventType::ToolExecutionComplete)
        .filter_map(|event| event.typed_data::<ToolExecutionCompleteData>())
        .filter(|data| !data.success)
        .collect()
}

fn assert_no_secret(events: &[SessionEvent], secret: &str) {
    let events_json = serde_json::to_string(events).expect("serialize events");
    assert!(
        !events_json.contains(secret),
        "provider secret leaked into events: {events_json}"
    );
}

fn assert_no_sdk_skills(skills: &[github_copilot_sdk::rpc::Skill]) {
    assert!(
        skills.iter().all(|skill| skill.source != SkillSource::Sdk),
        "expected no SDK skills, got {skills:?}"
    );
}
