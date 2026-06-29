use std::collections::HashMap;
use std::ffi::OsString;
use std::sync::Arc;

use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::rpc::{ProviderEndpointType, ProviderEndpointWireApi};
use github_copilot_sdk::{ProviderConfig, SessionConfig};

use super::support::{DEFAULT_TEST_TOKEN, with_e2e_context};

// session.provider.getEndpoint is gated behind COPILOT_ALLOW_GET_PROVIDER_ENDPOINT;
// the harness env passed to the CLI subprocess opts in for these tests.
fn opt_in_env() -> (OsString, OsString) {
    ("COPILOT_ALLOW_GET_PROVIDER_ENDPOINT".into(), "true".into())
}

#[tokio::test]
async fn byok_provider_endpoint_returns_configured_endpoint() {
    with_e2e_context(
        "provider-endpoint",
        "byok_provider_endpoint_returns_configured_endpoint",
        |ctx| {
            Box::pin(async move {
                let mut options = ctx.client_options();
                options.env.push(opt_in_env());
                let client = github_copilot_sdk::Client::start(options)
                    .await
                    .expect("start client");

                let mut headers = HashMap::new();
                headers.insert("X-Custom-Header".to_string(), "byok-yes".to_string());

                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_permission_handler(Arc::new(ApproveAllHandler))
                            .with_provider(
                                ProviderConfig::new("https://api.example.test/v1")
                                    .with_provider_type("openai")
                                    .with_wire_api("completions")
                                    .with_api_key("byok-secret")
                                    .with_headers(headers),
                            ),
                    )
                    .await
                    .expect("create session");

                let endpoint = session
                    .rpc()
                    .provider()
                    .get_endpoint()
                    .await
                    .expect("get_endpoint");

                assert!(
                    matches!(endpoint.r#type, ProviderEndpointType::Openai),
                    "expected type=openai, got {:?}",
                    endpoint.r#type,
                );
                assert!(
                    matches!(
                        endpoint.wire_api,
                        Some(ProviderEndpointWireApi::Completions)
                    ),
                    "expected wireApi=completions, got {:?}",
                    endpoint.wire_api,
                );
                assert_eq!(endpoint.base_url, "https://api.example.test/v1");
                assert_eq!(endpoint.api_key.as_deref(), Some("byok-secret"));
                assert_eq!(
                    endpoint.headers.get("X-Custom-Header").map(String::as_str),
                    Some("byok-yes"),
                );
                assert!(
                    endpoint.session_token.is_none(),
                    "BYOK sessions never issue a CAPI session token",
                );

                // disconnect may fail since the BYOK provider URL is fake
                let _ = session.disconnect().await;
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn capi_provider_endpoint_returns_resolved_credentials() {
    with_e2e_context(
        "provider-endpoint",
        "capi_provider_endpoint_returns_resolved_credentials",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let mut options = ctx.client_options().with_github_token(DEFAULT_TEST_TOKEN);
                options.env.push(opt_in_env());
                let client = github_copilot_sdk::Client::start(options)
                    .await
                    .expect("start client");

                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_permission_handler(Arc::new(ApproveAllHandler)),
                    )
                    .await
                    .expect("create session");

                let endpoint = session
                    .rpc()
                    .provider()
                    .get_endpoint()
                    .await
                    .expect("get_endpoint");

                assert!(
                    matches!(
                        endpoint.r#type,
                        ProviderEndpointType::Openai
                            | ProviderEndpointType::Azure
                            | ProviderEndpointType::Anthropic
                    ),
                    "expected type in {{openai, azure, anthropic}}, got {:?}",
                    endpoint.r#type,
                );
                if !matches!(endpoint.r#type, ProviderEndpointType::Anthropic) {
                    assert!(
                        matches!(
                            endpoint.wire_api,
                            Some(ProviderEndpointWireApi::Completions)
                                | Some(ProviderEndpointWireApi::Responses)
                        ),
                        "expected wireApi in {{completions, responses}}, got {:?}",
                        endpoint.wire_api,
                    );
                }

                assert!(
                    endpoint.base_url.starts_with("http://")
                        || endpoint.base_url.starts_with("https://"),
                    "expected http(s) baseUrl, got {}",
                    endpoint.base_url,
                );

                let api_key = endpoint
                    .api_key
                    .as_deref()
                    .expect("CAPI OAuth session must surface apiKey");
                assert!(!api_key.is_empty(), "apiKey must be non-empty");

                let integration_id = endpoint
                    .headers
                    .get("Copilot-Integration-Id")
                    .expect("Copilot-Integration-Id header");
                assert!(
                    !integration_id.is_empty(),
                    "Copilot-Integration-Id must be non-empty",
                );

                let user_agent = endpoint
                    .headers
                    .get("User-Agent")
                    .expect("User-Agent header");
                assert!(
                    user_agent.to_ascii_lowercase().contains("copilot"),
                    "expected User-Agent to mention Copilot, got {user_agent}",
                );

                let api_version = endpoint
                    .headers
                    .get("X-GitHub-Api-Version")
                    .expect("X-GitHub-Api-Version header");
                assert!(
                    !api_version.is_empty(),
                    "X-GitHub-Api-Version must be non-empty",
                );

                let interaction_id = endpoint
                    .headers
                    .get("X-Interaction-Id")
                    .expect("X-Interaction-Id header");
                let hex_count = interaction_id
                    .chars()
                    .filter(|c| c.is_ascii_hexdigit() || *c == '-')
                    .count();
                assert!(
                    hex_count >= 8,
                    "expected X-Interaction-Id to look like a hex/uuid value, got {interaction_id}",
                );

                let authorization = endpoint
                    .headers
                    .get("Authorization")
                    .expect("Authorization header");
                assert_eq!(authorization, &format!("Bearer {api_key}"));

                if let Some(session_token) = endpoint.session_token.as_ref() {
                    assert_eq!(session_token.header, "Copilot-Session-Token");
                    assert!(
                        !session_token.token.is_empty(),
                        "session token must be non-empty",
                    );
                    if let Some(expires_at) = session_token.expires_at.as_deref() {
                        assert!(!expires_at.is_empty(), "expected non-empty expiresAt",);
                    }
                }

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}
