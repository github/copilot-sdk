use github_copilot_sdk::SetModelOptions;
use github_copilot_sdk::rpc::ModelSwitchAutoTierStatus;
use github_copilot_sdk::session::Session;
use github_copilot_sdk::session_events::AutoTier;

use super::support::with_dedicated_e2e_context;

const MODEL_ID: &str = "auto";

/// Mirrors nodejs/test/e2e/auto_tier.e2e.test.ts (snapshot category "auto_tier").
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
                    .set_auto_tier(Some(AutoTier::Intelligence))
                    .await
                    .expect("stage intelligence");
                assert_eq!(superseded.status, ModelSwitchAutoTierStatus::Pending);
                assert_eq!(superseded.pending_auto_tier, Some(AutoTier::Intelligence));
                assert_eq!(superseded.superseded_auto_tier, Some(AutoTier::Efficiency));
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
                        Some(SetModelOptions::default().with_auto_tier(AutoTier::Intelligence)),
                    )
                    .await
                    .expect("set model with a tier");
                assert_eq!(
                    pending_auto_tier(&session).await,
                    Some(AutoTier::Intelligence)
                );

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
