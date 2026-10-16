// Copyright (c) Microsoft Corporation. All rights reserved.

use github_copilot_sdk::rpc::PingRequest;
use github_copilot_sdk::session_events::SessionMode;

static E2E: super::support::SharedE2eGroup =
    super::support::SharedE2eGroup::standard("rpc_server", 1);

#[tokio::test]
async fn should_round_trip_generated_server_and_session_rpc() {
    super::support::with_shared_e2e_context(
        &E2E,
        "rpc_server",
        "should_call_rpc_ping_with_typed_params_and_result",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let pong = client
                    .rpc()
                    .ping(PingRequest {
                        message: Some("typed rpc test".to_string()),
                    })
                    .await
                    .expect("ping");
                assert_eq!(pong.message, "pong: typed rpc test");
                assert!(!pong.timestamp.is_empty());

                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                assert_eq!(
                    session.rpc().mode().get().await.expect("get mode"),
                    SessionMode::Interactive
                );
                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}
