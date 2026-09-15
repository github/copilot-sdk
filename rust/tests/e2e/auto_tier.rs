use std::sync::Arc;
use std::time::Duration;

use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::rpc::{EventLogReadRequest, ModelSwitchAutoTierStatus};
use github_copilot_sdk::session::Session;
use github_copilot_sdk::session_events::{
    AutoTier, AutoTierSwitchFailureReason, SessionAutoTierSwitchFailedData, SessionEventType,
    SessionModelChangeData,
};
use github_copilot_sdk::{
    CapiSessionOptions, MessageOptions, ResumeSessionConfig, SessionConfig, SessionId,
    SetModelOptions,
};
use serde_json::json;

use super::support::{DEFAULT_TEST_TOKEN, wait_for_event, with_dedicated_e2e_context};

const MODEL_ID: &str = "auto";

/// End-to-end coverage for staging and resetting an Auto routing preference
/// (snapshot category "auto_tier").
///
/// The runtime stages an Auto routing preference instead of applying it immediately: a
/// request stays unclaimed until a later turn using the `auto` model mints a usable model
/// and token pair. These tests observe that staged state through `model().get_current()`,
/// so they assert what the runtime actually recorded rather than what the SDK serialized.
async fn pending_auto_tier(session: &Session) -> Option<AutoTier> {
    session
        .rpc()
        .model()
        .get_current()
        .await
        .expect("get current model")
        .pending_auto_tier
}

fn auto_session_config(tier: Option<AutoTier>) -> SessionConfig {
    let capi = CapiSessionOptions::new().with_enable_web_socket_responses(false);
    let capi = match tier {
        Some(tier) => capi.with_auto_tier(tier),
        None => capi,
    };
    SessionConfig::default()
        .with_model(MODEL_ID)
        .with_capi(capi)
        .with_permission_handler(Arc::new(ApproveAllHandler))
        .with_github_token(DEFAULT_TEST_TOKEN)
}

fn auto_resume_config(session_id: SessionId) -> ResumeSessionConfig {
    ResumeSessionConfig::new(session_id)
        .with_permission_handler(Arc::new(ApproveAllHandler))
        .with_github_token(DEFAULT_TEST_TOKEN)
}

async fn send_prompt(session: &Session, prompt: &str) {
    session
        .send_and_wait(MessageOptions::new(prompt).with_wait_timeout(Duration::from_secs(120)))
        .await
        .expect("send prompt");
}

#[tokio::test]
async fn should_stage_and_reset_auto_tier_preference() {
    with_dedicated_e2e_context(
        "auto_tier",
        "should_stage_and_reset_auto_tier_preference",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config().with_model(MODEL_ID))
                    .await
                    .expect("create session");

                assert_eq!(pending_auto_tier(&session).await, None);

                let staged = session
                    .set_auto_tier(Some(AutoTier::Efficiency))
                    .await
                    .expect("stage efficiency");
                assert_eq!(staged.status, ModelSwitchAutoTierStatus::Pending);
                assert_eq!(staged.pending_auto_tier, Some(AutoTier::Efficiency));
                assert_eq!(
                    pending_auto_tier(&session).await,
                    Some(AutoTier::Efficiency)
                );

                // A second request replaces the first and reports the one it displaced.
                let superseded = session
                    .set_auto_tier(Some(AutoTier::Fast))
                    .await
                    .expect("stage fast");
                assert_eq!(superseded.status, ModelSwitchAutoTierStatus::Pending);
                assert_eq!(superseded.pending_auto_tier, Some(AutoTier::Fast));
                assert_eq!(superseded.superseded_auto_tier, Some(AutoTier::Efficiency));
                assert_eq!(pending_auto_tier(&session).await, Some(AutoTier::Fast));

                let replaced_fast = session
                    .set_auto_tier(Some(AutoTier::Intelligence))
                    .await
                    .expect("stage intelligence");
                assert_eq!(replaced_fast.status, ModelSwitchAutoTierStatus::Pending);
                assert_eq!(
                    replaced_fast.pending_auto_tier,
                    Some(AutoTier::Intelligence)
                );
                assert_eq!(replaced_fast.superseded_auto_tier, Some(AutoTier::Fast));
                assert_eq!(
                    pending_auto_tier(&session).await,
                    Some(AutoTier::Intelligence)
                );

                // `None` returns the session to provider-default routing. The status is
                // `Unchanged` because provider-default was already the committed
                // preference; the request's effect is cancelling the staged one.
                let reset = session.set_auto_tier(None).await.expect("reset tier");
                assert_eq!(reset.status, ModelSwitchAutoTierStatus::Unchanged);
                assert_eq!(reset.superseded_auto_tier, Some(AutoTier::Intelligence));
                assert_eq!(pending_auto_tier(&session).await, None);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_preserve_auto_tier_when_set_model_omits_it() {
    with_dedicated_e2e_context(
        "auto_tier",
        "should_preserve_auto_tier_when_set_model_omits_it",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config().with_model(MODEL_ID))
                    .await
                    .expect("create session");

                session
                    .set_auto_tier(Some(AutoTier::Balance))
                    .await
                    .expect("stage balance");
                assert_eq!(pending_auto_tier(&session).await, Some(AutoTier::Balance));

                // Omitting the preference leaves the staged one alone.
                session
                    .set_model(MODEL_ID, None)
                    .await
                    .expect("set model without a tier");
                assert_eq!(pending_auto_tier(&session).await, Some(AutoTier::Balance));

                // Supplying a tier replaces it.
                session
                    .set_model(
                        MODEL_ID,
                        Some(SetModelOptions::default().with_auto_tier(AutoTier::Fast)),
                    )
                    .await
                    .expect("set model with a tier");
                assert_eq!(pending_auto_tier(&session).await, Some(AutoTier::Fast));

                // Requesting a reset clears it. Omission, a tier, and a reset are three
                // distinct outcomes, which `AutoTierPreference` makes explicit.
                session
                    .set_model(
                        MODEL_ID,
                        Some(SetModelOptions::default().with_reset_auto_tier()),
                    )
                    .await
                    .expect("set model with a reset");
                assert_eq!(pending_auto_tier(&session).await, None);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_restore_and_override_fast_auto_tier_on_cold_resume() {
    with_dedicated_e2e_context(
        "auto_tier",
        "should_restore_and_override_fast_auto_tier_on_cold_resume",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let fast_session = client
                    .create_session(auto_session_config(Some(AutoTier::Fast)))
                    .await
                    .expect("create fast session");
                let tierless_session = client
                    .create_session(auto_session_config(None))
                    .await
                    .expect("create tierless session");
                let fast_session_id = fast_session.id().clone();
                let tierless_session_id = tierless_session.id().clone();

                send_prompt(
                    &fast_session,
                    "Reply with exactly AUTO_TIER_COLD_RESUME_READY.",
                )
                .await;
                send_prompt(
                    &tierless_session,
                    "Reply with exactly AUTO_TIER_TIERLESS_READY.",
                )
                .await;

                let fast_current = fast_session
                    .rpc()
                    .model()
                    .get_current()
                    .await
                    .expect("get fast current model");
                assert_eq!(fast_current.auto_tier, Some(AutoTier::Fast));
                let tierless_current = tierless_session
                    .rpc()
                    .model()
                    .get_current()
                    .await
                    .expect("get tierless current model");
                assert_eq!(tierless_current.auto_tier, None);

                fast_session
                    .disconnect()
                    .await
                    .expect("disconnect fast session");
                tierless_session
                    .disconnect()
                    .await
                    .expect("disconnect tierless session");
                client.stop().await.expect("stop initial client");

                let restored_client = ctx.start_client().await;
                let restored_fast = restored_client
                    .resume_session(auto_resume_config(fast_session_id.clone()))
                    .await
                    .expect("resume fast session");
                let restored_tierless = restored_client
                    .resume_session(auto_resume_config(tierless_session_id))
                    .await
                    .expect("resume tierless session");
                let restored_fast_current = restored_fast
                    .rpc()
                    .model()
                    .get_current()
                    .await
                    .expect("get restored fast current model");
                assert_eq!(restored_fast_current.auto_tier, Some(AutoTier::Fast));
                let restored_tierless_current = restored_tierless
                    .rpc()
                    .model()
                    .get_current()
                    .await
                    .expect("get restored tierless current model");
                assert_eq!(restored_tierless_current.auto_tier, None);

                restored_fast
                    .disconnect()
                    .await
                    .expect("disconnect restored fast session");
                restored_tierless
                    .disconnect()
                    .await
                    .expect("disconnect restored tierless session");
                restored_client.stop().await.expect("stop restored client");

                let override_client = ctx.start_client().await;
                let overridden = override_client
                    .resume_session(
                        auto_resume_config(fast_session_id)
                            .with_model(MODEL_ID)
                            .with_capi(
                                CapiSessionOptions::new()
                                    .with_auto_tier(AutoTier::Balance)
                                    .with_enable_web_socket_responses(false),
                            ),
                    )
                    .await
                    .expect("resume session with balance override");
                let overridden_current = overridden
                    .rpc()
                    .model()
                    .get_current()
                    .await
                    .expect("get overridden current model");
                assert_eq!(overridden_current.auto_tier, Some(AutoTier::Balance));

                overridden
                    .disconnect()
                    .await
                    .expect("disconnect overridden session");
                override_client.stop().await.expect("stop override client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_commit_fast_auto_tier_after_successful_turn() {
    with_dedicated_e2e_context(
        "auto_tier",
        "should_commit_fast_auto_tier_after_successful_turn",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(auto_session_config(Some(AutoTier::Efficiency)))
                    .await
                    .expect("create session");

                let model_change = wait_for_event(
                    session.subscribe(),
                    "Fast Auto tier model change",
                    |event| {
                        event.parsed_type() == SessionEventType::SessionModelChange
                            && event
                                .typed_data::<SessionModelChangeData>()
                                .is_some_and(|data| data.auto_tier == Some(AutoTier::Fast))
                    },
                );

                let staged = session
                    .set_auto_tier(Some(AutoTier::Fast))
                    .await
                    .expect("stage fast");
                assert_eq!(staged.status, ModelSwitchAutoTierStatus::Pending);
                assert_eq!(staged.effective_auto_tier, Some(AutoTier::Efficiency));
                assert_eq!(staged.pending_auto_tier, Some(AutoTier::Fast));

                let before_turn = session
                    .rpc()
                    .model()
                    .get_current()
                    .await
                    .expect("get current model before turn");
                assert_eq!(before_turn.auto_tier, Some(AutoTier::Efficiency));
                assert_eq!(before_turn.pending_auto_tier, Some(AutoTier::Fast));

                send_prompt(&session, "Reply with exactly AUTO_TIER_FAST_COMMITTED.").await;

                let model_change = model_change.await;
                let data = model_change
                    .typed_data::<SessionModelChangeData>()
                    .expect("typed model change data");
                assert_eq!(data.previous_model.as_deref(), Some(MODEL_ID));
                assert_eq!(data.new_model, MODEL_ID);
                assert_eq!(data.previous_auto_tier, Some(AutoTier::Efficiency));
                assert_eq!(data.auto_tier, Some(AutoTier::Fast));

                let committed = session
                    .rpc()
                    .model()
                    .get_current()
                    .await
                    .expect("get current model after turn");
                assert_eq!(committed.auto_tier, Some(AutoTier::Fast));
                assert_eq!(committed.pending_auto_tier, None);
                assert_eq!(committed.activating_auto_tier, None);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_preserve_effective_tier_when_fast_activation_fails() {
    with_dedicated_e2e_context(
        "auto_tier",
        "should_preserve_effective_tier_when_fast_activation_fails",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(auto_session_config(Some(AutoTier::Efficiency)))
                    .await
                    .expect("create session");
                let session_id = session.id().clone();
                send_prompt(&session, "Reply with exactly AUTO_TIER_INITIAL_READY.").await;

                let failure = wait_for_event(
                    session.subscribe(),
                    "Fast Auto tier activation failure",
                    |event| event.parsed_type() == SessionEventType::SessionAutoTierSwitchFailed,
                );
                let mut model_changes = session.subscribe();

                let staged = session
                    .set_auto_tier(Some(AutoTier::Fast))
                    .await
                    .expect("stage fast");
                assert_eq!(staged.status, ModelSwitchAutoTierStatus::Pending);
                assert_eq!(staged.effective_auto_tier, Some(AutoTier::Efficiency));
                assert_eq!(staged.pending_auto_tier, Some(AutoTier::Fast));

                send_prompt(&session, "Reply with exactly AUTO_TIER_FAILURE_RECOVERED.").await;

                let failure = failure.await;
                assert_eq!(failure.ephemeral, Some(true));
                let data = failure
                    .typed_data::<SessionAutoTierSwitchFailedData>()
                    .expect("typed Auto tier failure data");
                assert_eq!(data.effective_auto_tier, Some(AutoTier::Efficiency));
                assert_eq!(data.requested_auto_tier, Some(AutoTier::Fast));
                assert_eq!(data.reason, AutoTierSwitchFailureReason::RequestFailed);

                while let Ok(Ok(event)) =
                    tokio::time::timeout(Duration::from_millis(25), model_changes.recv()).await
                {
                    assert!(
                        event.parsed_type() != SessionEventType::SessionModelChange
                            || !event
                                .typed_data::<SessionModelChangeData>()
                                .is_some_and(|data| data.auto_tier == Some(AutoTier::Fast)),
                        "Fast tier committed after failed activation"
                    );
                }

                let current = session
                    .rpc()
                    .model()
                    .get_current()
                    .await
                    .expect("get current model after failure");
                assert_eq!(current.auto_tier, Some(AutoTier::Efficiency));
                assert_eq!(current.pending_auto_tier, None);
                assert_eq!(current.activating_auto_tier, None);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop initial client");

                let resumed_client = ctx.start_client().await;
                let resumed = resumed_client
                    .resume_session(auto_resume_config(session_id))
                    .await
                    .expect("resume session");
                let resumed_current = resumed
                    .rpc()
                    .model()
                    .get_current()
                    .await
                    .expect("get current model after resume");
                assert_eq!(resumed_current.auto_tier, Some(AutoTier::Efficiency));
                assert_eq!(resumed_current.pending_auto_tier, None);
                assert_eq!(resumed_current.activating_auto_tier, None);

                let persisted = resumed
                    .rpc()
                    .event_log()
                    .read(EventLogReadRequest {
                        agent_ids: None,
                        agent_scope: None,
                        cursor: None,
                        direction: None,
                        include_ephemeral: Some(false),
                        max: Some(100),
                        types: Some(json!("*")),
                        wait_ms: Some(0),
                    })
                    .await
                    .expect("read event log");
                assert!(
                    persisted.events.iter().all(|event| {
                        event.parsed_type() != SessionEventType::SessionAutoTierSwitchFailed
                    }),
                    "ephemeral failure event was replayed after cold resume"
                );

                resumed
                    .disconnect()
                    .await
                    .expect("disconnect resumed session");
                resumed_client.stop().await.expect("stop resumed client");
            })
        },
    )
    .await;
}
