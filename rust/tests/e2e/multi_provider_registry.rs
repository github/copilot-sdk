use std::collections::HashMap;

use github_copilot_sdk::{
    CustomAgentConfig, MessageOptions, NamedProviderConfig, ProviderModelConfig,
};
use serde_json::Value;

use super::support::with_e2e_context;

const CATEGORY: &str = "multi_provider_registry";

fn headers(provider: &str) -> HashMap<String, String> {
    let mut map = HashMap::new();
    map.insert("X-Provider".to_string(), provider.to_string());
    map
}

#[tokio::test]
async fn should_register_multiple_providers_with_custom_agents_bound_to_their_models() {
    with_e2e_context(
        CATEGORY,
        "should_register_multiple_providers_with_custom_agents_bound_to_their_models",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;

                // A heterogeneous registry: two providers of different types,
                // with multiple models each. Provider-qualified selection ids
                // are alpha/sonnet, alpha/haiku, beta/opus, beta/haiku.
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_providers(vec![
                                NamedProviderConfig::new("alpha", "https://alpha.example.test/v1")
                                    .with_provider_type("openai")
                                    .with_wire_api("completions")
                                    .with_api_key("alpha-secret")
                                    .with_headers(headers("alpha")),
                                NamedProviderConfig::new("beta", "https://beta.example.test")
                                    .with_provider_type("anthropic")
                                    .with_bearer_token("beta-bearer")
                                    .with_headers(headers("beta")),
                            ])
                            .with_models(vec![
                                ProviderModelConfig::new("sonnet", "alpha")
                                    .with_wire_model("byok-gpt-4o")
                                    .with_max_prompt_tokens(111_111),
                                ProviderModelConfig::new("haiku", "alpha")
                                    .with_wire_model("byok-gpt-4o-mini"),
                                ProviderModelConfig::new("opus", "beta")
                                    .with_wire_model("byok-claude-3-opus"),
                                ProviderModelConfig::new("haiku", "beta")
                                    .with_wire_model("byok-claude-3-haiku"),
                            ])
                            .with_custom_agents([
                                CustomAgentConfig::new("orchestrator", "Plan and delegate.")
                                    .with_display_name("Orchestrator")
                                    .with_description("Top-level planner.")
                                    .with_model("alpha/sonnet"),
                                CustomAgentConfig::new("researcher", "Research thoroughly.")
                                    .with_display_name("Researcher")
                                    .with_description("Deep research subagent.")
                                    .with_model("beta/opus"),
                                CustomAgentConfig::new("fast-helper", "Answer quickly.")
                                    .with_display_name("Fast Helper")
                                    .with_description("Quick subagent.")
                                    .with_model("alpha/haiku"),
                                CustomAgentConfig::new("summarizer", "Summarize.")
                                    .with_display_name("Summarizer")
                                    .with_description("Summarizing subagent.")
                                    .with_model("beta/haiku"),
                            ]),
                    )
                    .await
                    .expect("create session");

                let result = session.rpc().agent().list().await.expect("agent list");

                // All four custom agents coexist in a single session.
                assert_eq!(result.agents.len(), 4, "expected 4 custom agents");

                // Each agent is bound to its configured provider-qualified model.
                let bound = |name: &str| {
                    result
                        .agents
                        .iter()
                        .find(|agent| agent.name == name)
                        .and_then(|agent| agent.model.clone())
                        .unwrap_or_default()
                };
                assert_eq!(bound("orchestrator"), "alpha/sonnet");
                assert_eq!(bound("researcher"), "beta/opus");
                assert_eq!(bound("fast-helper"), "alpha/haiku");
                assert_eq!(bound("summarizer"), "beta/haiku");

                // Models from BOTH providers are represented, proving the two
                // providers and their models coexist within the same session.
                let models: Vec<String> = result
                    .agents
                    .iter()
                    .filter_map(|agent| agent.model.clone())
                    .collect();
                assert!(
                    models.iter().any(|m| m.starts_with("alpha/")),
                    "expected an alpha-bound agent",
                );
                assert!(
                    models.iter().any(|m| m.starts_with("beta/")),
                    "expected a beta-bound agent",
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

async fn assert_routing(
    snapshot_name: &'static str,
    selection_id: &'static str,
    expected_wire_model: &'static str,
    expected_provider_header: &'static str,
) {
    with_e2e_context(CATEGORY, snapshot_name, move |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;

            // Two OpenAI-compatible providers, both pointed at the replay proxy
            // so their /chat/completions traffic is captured. They are
            // distinguished on the wire by their per-provider X-Provider
            // header. "alpha" carries two models (multiple models per
            // provider); "delta" carries one.
            let proxy_url = ctx.proxy_url().to_string();
            let session = client
                .create_session(
                    ctx.approve_all_session_config()
                        .with_model(selection_id)
                        .with_providers(vec![
                            NamedProviderConfig::new("alpha", proxy_url.clone())
                                .with_provider_type("openai")
                                .with_wire_api("completions")
                                .with_api_key("alpha-secret")
                                .with_headers(headers("alpha")),
                            NamedProviderConfig::new("delta", proxy_url.clone())
                                .with_provider_type("openai")
                                .with_wire_api("completions")
                                .with_api_key("delta-secret")
                                .with_headers(headers("delta")),
                        ])
                        .with_models(vec![
                            ProviderModelConfig::new("sonnet", "alpha")
                                .with_wire_model("byok-gpt-4o"),
                            ProviderModelConfig::new("haiku", "alpha")
                                .with_wire_model("byok-gpt-4o-mini"),
                            ProviderModelConfig::new("turbo", "delta")
                                .with_wire_model("byok-gpt-4-turbo"),
                        ]),
                )
                .await
                .expect("create session");

            session
                .send_and_wait(MessageOptions::new("What is 5+5?"))
                .await
                .expect("send");

            let exchanges = ctx.exchanges();
            assert_eq!(exchanges.len(), 1, "expected exactly one captured exchange");
            let exchange = &exchanges[0];

            // The wire model sent to the provider is the selected model's wire
            // model, not its provider-qualified selection id.
            let model = exchange
                .get("request")
                .and_then(|request| request.get("model"))
                .and_then(Value::as_str)
                .expect("request model");
            assert_eq!(model, expected_wire_model);

            let request_headers = exchange
                .get("requestHeaders")
                .and_then(Value::as_object)
                .expect("request headers");

            // The request carried the owning provider's custom header, proving
            // the turn was dispatched against the correct provider connection.
            let provider_header = request_headers
                .iter()
                .find(|(key, _)| key.eq_ignore_ascii_case("x-provider"))
                .and_then(|(_, value)| value.as_str())
                .expect("x-provider header");
            assert_eq!(provider_header, expected_provider_header);

            // The provider's API key was applied as an Authorization header.
            let has_authorization = request_headers
                .iter()
                .any(|(key, _)| key.eq_ignore_ascii_case("authorization"));
            assert!(has_authorization, "expected an Authorization header");

            // disconnect may fail since the BYOK provider URL is the proxy
            let _ = session.disconnect().await;
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_route_alpha_sonnet_turn_to_its_provider_and_wire_model() {
    assert_routing(
        "should_route_alpha_sonnet_turn_to_its_provider_and_wire_model",
        "alpha/sonnet",
        "byok-gpt-4o",
        "alpha",
    )
    .await;
}

#[tokio::test]
async fn should_route_alpha_haiku_turn_to_its_provider_and_wire_model() {
    assert_routing(
        "should_route_alpha_haiku_turn_to_its_provider_and_wire_model",
        "alpha/haiku",
        "byok-gpt-4o-mini",
        "alpha",
    )
    .await;
}

#[tokio::test]
async fn should_route_delta_turbo_turn_to_its_provider_and_wire_model() {
    assert_routing(
        "should_route_delta_turbo_turn_to_its_provider_and_wire_model",
        "delta/turbo",
        "byok-gpt-4-turbo",
        "delta",
    )
    .await;
}
