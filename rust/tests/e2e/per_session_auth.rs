use std::sync::Arc;

use github_copilot_sdk::SessionConfig;
use github_copilot_sdk::handler::ApproveAllHandler;

use super::support::with_e2e_context;

#[tokio::test]
async fn session_uses_client_token_when_no_session_token_is_supplied() {
    with_e2e_context(
        "per-session-auth",
        "session_uses_client_token_when_no_session_token_is_supplied",
        |ctx| {
            Box::pin(async move {
                let token = "alice-token";
                ctx.set_copilot_user_by_token_with_login(token, "alice");
                let client = github_copilot_sdk::Client::start(
                    ctx.client_options().with_github_token(token),
                )
                .await
                .expect("start client");

                let session = client
                    .create_session(
                        SessionConfig::default().with_handler(Arc::new(ApproveAllHandler)),
                    )
                    .await
                    .expect("create session");
                let status = session
                    .rpc()
                    .auth()
                    .get_status()
                    .await
                    .expect("auth status");

                assert!(status.is_authenticated);
                assert_eq!(status.login.as_deref(), Some("alice"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn session_token_overrides_client_token() {
    with_e2e_context(
        "per-session-auth",
        "session_token_overrides_client_token",
        |ctx| {
            Box::pin(async move {
                ctx.set_copilot_user_by_token_with_login("alice-token", "alice");
                ctx.set_copilot_user_by_token_with_login("bob-token", "bob");
                let client = github_copilot_sdk::Client::start(
                    ctx.client_options().with_github_token("alice-token"),
                )
                .await
                .expect("start client");

                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_handler(Arc::new(ApproveAllHandler))
                            .with_github_token("bob-token"),
                    )
                    .await
                    .expect("create session");
                let status = session
                    .rpc()
                    .auth()
                    .get_status()
                    .await
                    .expect("auth status");

                assert!(status.is_authenticated);
                assert_eq!(status.login.as_deref(), Some("bob"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn session_auth_status_is_unauthenticated_without_token() {
    with_e2e_context(
        "per-session-auth",
        "session_auth_status_is_unauthenticated_without_token",
        |ctx| {
            Box::pin(async move {
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        SessionConfig::default().with_handler(Arc::new(ApproveAllHandler)),
                    )
                    .await
                    .expect("create session");
                let status = session
                    .rpc()
                    .auth()
                    .get_status()
                    .await
                    .expect("auth status");

                assert!(!status.is_authenticated);
                assert!(status.login.is_none());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn session_fails_with_invalid_token() {
    with_e2e_context(
        "per-session-auth",
        "session_fails_with_invalid_token",
        |ctx| {
            Box::pin(async move {
                ctx.set_copilot_user_by_token_with_login("valid-token", "valid-user");
                let client = ctx.start_client().await;

                let err = match client
                    .create_session(
                        SessionConfig::default()
                            .with_handler(Arc::new(ApproveAllHandler))
                            .with_github_token("invalid-token"),
                    )
                    .await
                {
                    Ok(_) => panic!("invalid token should fail session create"),
                    Err(err) => err,
                };

                assert!(
                    err.to_string().contains("401") || err.to_string().contains("Unauthorized"),
                    "expected unauthorized error, got {err}"
                );
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}
