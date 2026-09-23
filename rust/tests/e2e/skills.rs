use std::path::{Path, PathBuf};
use std::sync::Arc;

use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::{CustomAgentConfig, ResumeSessionConfig};

use super::support::{assert_uuid_like, assistant_message_content};

const SKILL_MARKER: &str = "PINEAPPLE_COCONUT_42";

#[tokio::test]
async fn should_load_and_apply_skill_from_skilldirectories() {
    super::support::with_shared_e2e_context(
        &E2E,
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
    super::support::with_shared_e2e_context(
        &E2E,
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
    super::support::with_shared_e2e_context(
        &E2E,
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
    super::support::with_shared_e2e_context(
        &E2E,
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

#[tokio::test]
async fn should_reload_replaced_skill_and_replay_it_on_resume() {
    super::support::with_dedicated_e2e_context(
        "scenario_testing_skills_and_agents",
        "should_reload_atomically_replaced_skill_and_replay_it_on_resume",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let skill_name = "scenario-reloadable-skill";
                let skills_dir = ctx.work_dir().join("scenario-reloadable-skills");
                let skill_file = write_versioned_skill(
                    &skills_dir,
                    skill_name,
                    "Scenario skill version one.",
                    "SCENARIO_SKILL_VERSION_ONE",
                );
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_enable_session_store(true)
                            .with_skill_directories([skills_dir.clone()]),
                    )
                    .await
                    .expect("create session");
                let session_id = session.id().clone();

                assert_versioned_skill(
                    session.rpc().skills().list().await.expect("list v1"),
                    skill_name,
                    "Scenario skill version one.",
                    &skill_file,
                );

                let replacement = skill_file.with_file_name("SKILL.replacement.md");
                std::fs::write(
                    &replacement,
                    skill_contents(
                        skill_name,
                        "Scenario skill version two.",
                        "SCENARIO_SKILL_VERSION_TWO",
                    ),
                )
                .expect("write replacement skill");
                std::fs::rename(&replacement, &skill_file).expect("replace skill");
                session
                    .rpc()
                    .skills()
                    .reload()
                    .await
                    .expect("reload replaced skill");
                assert_versioned_skill(
                    session.rpc().skills().list().await.expect("list v2"),
                    skill_name,
                    "Scenario skill version two.",
                    &skill_file,
                );

                session
                    .log("SCENARIO_SKILL_RELOAD_READY", None)
                    .await
                    .expect("persist skill session");
                client
                    .rpc()
                    .sessions()
                    .save(github_copilot_sdk::rpc::SessionsSaveRequest {
                        session_id: session_id.clone(),
                    })
                    .await
                    .expect("save session");
                session.rpc().suspend().await.expect("suspend session");
                session.stop_event_loop().await;
                drop(session);

                let resumed = client
                    .resume_session(
                        ResumeSessionConfig::new(session_id)
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                            .with_permission_handler(Arc::new(ApproveAllHandler))
                            .with_continue_pending_work(false)
                            .with_skill_directories([skills_dir]),
                    )
                    .await
                    .expect("resume session");
                assert_versioned_skill(
                    resumed
                        .rpc()
                        .skills()
                        .list()
                        .await
                        .expect("list resumed skill"),
                    skill_name,
                    "Scenario skill version two.",
                    &skill_file,
                );

                resumed
                    .disconnect()
                    .await
                    .expect("disconnect resumed session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

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

fn write_versioned_skill(
    skills_dir: &Path,
    name: &str,
    description: &str,
    marker: &str,
) -> PathBuf {
    let skill_dir = skills_dir.join(name);
    std::fs::create_dir_all(&skill_dir).expect("create versioned skill dir");
    let skill_file = skill_dir.join("SKILL.md");
    std::fs::write(&skill_file, skill_contents(name, description, marker))
        .expect("write versioned skill");
    skill_file
}

fn skill_contents(name: &str, description: &str, marker: &str) -> String {
    format!(
        "---\nname: {name}\ndescription: {description}\n---\n\n\
         # Scenario Reloadable Skill\n\nUse {marker}.\n"
    )
}

fn assert_versioned_skill(
    list: github_copilot_sdk::rpc::SkillList,
    name: &str,
    description: &str,
    path: &Path,
) {
    let skill = list
        .skills
        .iter()
        .find(|skill| skill.name == name)
        .expect("versioned skill");
    assert!(skill.enabled);
    assert_eq!(skill.description, description);
    assert_eq!(
        skill.path.as_deref().map(Path::new),
        Some(path),
        "unexpected skill path"
    );
}
static E2E: super::support::SharedE2eGroup = super::support::SharedE2eGroup::standard("skills", 4);
