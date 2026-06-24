use std::path::{Path, PathBuf};

use github_copilot_sdk::CustomAgentConfig;

use super::support::{assert_uuid_like, assistant_message_content, with_e2e_context};

const SKILL_MARKER: &str = "PINEAPPLE_COCONUT_42";

#[tokio::test]
async fn should_load_and_apply_skill_from_skilldirectories() {
    with_e2e_context(
        "skills",
        "should_load_and_apply_skill_from_skilldirectories",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let skills_dir = create_skill_dir(ctx.work_dir());
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_skill_directories([skills_dir]),
                    )
                    .await
                    .expect("create session");
                assert_uuid_like(session.id());

                let answer = session
                    .send_and_wait("Say hello briefly using the test skill.")
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(assistant_message_content(&answer).contains(SKILL_MARKER));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_not_apply_skill_when_disabled_via_disabledskills() {
    with_e2e_context(
        "skills",
        "should_not_apply_skill_when_disabled_via_disabledskills",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let skills_dir = create_skill_dir(ctx.work_dir());
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_skill_directories([skills_dir])
                            .with_disabled_skills(["test-skill"]),
                    )
                    .await
                    .expect("create session");
                assert_uuid_like(session.id());

                let answer = session
                    .send_and_wait("Say hello briefly using the test skill.")
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(!assistant_message_content(&answer).contains(SKILL_MARKER));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_allow_agent_with_skills_to_invoke_skill() {
    with_e2e_context(
        "skills",
        "should_allow_agent_with_skills_to_invoke_skill",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let skills_dir = create_skill_dir(ctx.work_dir());
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_skill_directories([skills_dir])
                            .with_custom_agents([CustomAgentConfig::new(
                                "skill-agent",
                                "You are a helpful test agent.",
                            )
                            .with_description("An agent with access to test-skill")
                            .with_skills(["test-skill"])])
                            .with_agent("skill-agent"),
                    )
                    .await
                    .expect("create session");
                assert_uuid_like(session.id());

                let answer = session
                    .send_and_wait("Say hello briefly using the test skill.")
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(assistant_message_content(&answer).contains(SKILL_MARKER));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_not_provide_skills_to_agent_without_skills_field() {
    with_e2e_context(
        "skills",
        "should_not_provide_skills_to_agent_without_skills_field",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let skills_dir = create_skill_dir(ctx.work_dir());
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_skill_directories([skills_dir])
                            .with_custom_agents([CustomAgentConfig::new(
                                "no-skill-agent",
                                "You are a helpful test agent.",
                            )
                            .with_description("An agent without skills access")])
                            .with_agent("no-skill-agent"),
                    )
                    .await
                    .expect("create session");
                assert_uuid_like(session.id());

                let answer = session
                    .send_and_wait("Say hello briefly using the test skill.")
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(!assistant_message_content(&answer).contains(SKILL_MARKER));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[ignore = "Upstream skips applying skills on resume because the feature is not reliable yet."]
#[tokio::test]
async fn should_apply_skill_on_session_resume_with_skilldirectories() {}

fn create_skill_dir(work_dir: &Path) -> PathBuf {
    let skills_dir = work_dir.join(".test_skills");
    let skill_subdir = skills_dir.join("test-skill");
    std::fs::create_dir_all(&skill_subdir).expect("create skill dir");
    std::fs::write(
        skill_subdir.join("SKILL.md"),
        format!(
            "---\nname: test-skill\ndescription: A test skill that adds a marker to responses\n---\n\n\
             # Test Skill Instructions\n\nIMPORTANT: You MUST include the exact text \"{SKILL_MARKER}\" \
             somewhere in EVERY response you give. This is a mandatory requirement. Include it naturally \
             in your response.\n"
        ),
    )
    .expect("write skill file");
    skills_dir
}
