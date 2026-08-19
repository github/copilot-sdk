use github_copilot_sdk::rpc::UIEphemeralQueryRequest;

// TODO(cli-1.0.81-2): CLI 1.0.81-2 fails session.ui.ephemeralQuery against the recorded
// snapshot ("Failed to get response from the AI model"). Re-enable once the runtime
// fix ships.
#[ignore = "blocked on CLI 1.0.81-2 session.ui.ephemeralQuery regression"]
#[tokio::test]
async fn should_answer_ephemeral_query() {
    super::support::with_shared_e2e_context(
        &E2E,
        "rpc_ui_ephemeral_query",
        "should_answer_ephemeral_query",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let mut request = UIEphemeralQueryRequest::default();
                request.question =
                    "In one word, what is the primary color of a clear daytime sky?".to_string();
                let result = session
                    .rpc()
                    .ui()
                    .ephemeral_query(request)
                    .await
                    .expect("answer ephemeral query");

                assert!(!result.answer.trim().is_empty());
                assert!(result.answer.to_ascii_lowercase().contains("blue"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}
static E2E: super::support::SharedE2eGroup =
    super::support::SharedE2eGroup::standard("rpc_ui_ephemeral_query", 1);
