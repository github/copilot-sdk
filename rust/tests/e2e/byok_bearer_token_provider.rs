/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

use std::sync::Arc;
use std::sync::atomic::{AtomicUsize, Ordering};

use async_trait::async_trait;
use bytes::Bytes;
use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::{
    BearerTokenError, CopilotHttpRequest, CopilotHttpResponse, CopilotRequestContext,
    CopilotRequestError, CopilotRequestHandler, MessageOptions, NamedProviderConfig,
    ProviderModelConfig, ProviderTokenArgs, SessionConfig,
};
use http::HeaderMap;

use super::support::with_e2e_context_no_snapshot;

const PRIMARY_BASE_URL: &str = "https://byok-endpoint.invalid/v1";
const RED_HOST: &str = "byok-red.invalid";
const RED_BASE_URL: &str = "https://byok-red.invalid/v1";
const BLUE_HOST: &str = "byok-blue.invalid";
const BLUE_BASE_URL: &str = "https://byok-blue.invalid/v1";

#[derive(Debug, Clone)]
struct CapturedRequest {
    host: String,
    authorization: Option<String>,
}

#[derive(Default)]
struct CapturingRequestHandler {
    captures: std::sync::Mutex<Vec<CapturedRequest>>,
}

impl CapturingRequestHandler {
    fn auth_headers(&self) -> Vec<String> {
        self.captures
            .lock()
            .unwrap()
            .iter()
            .filter_map(|capture| capture.authorization.clone())
            .collect()
    }

    fn auth_header_for_host(&self, host: &str) -> Option<String> {
        self.captures
            .lock()
            .unwrap()
            .iter()
            .find(|capture| capture.host == host)
            .and_then(|capture| capture.authorization.clone())
    }

    fn reset(&self) {
        self.captures.lock().unwrap().clear();
    }
}

#[async_trait]
impl CopilotRequestHandler for CapturingRequestHandler {
    async fn send_request(
        &self,
        request: CopilotHttpRequest,
        _ctx: &CopilotRequestContext,
    ) -> Result<CopilotHttpResponse, CopilotRequestError> {
        let uri: http::Uri = request
            .url
            .parse()
            .map_err(|error| CopilotRequestError::message(format!("invalid URL: {error}")))?;
        if let Some(host) = uri.host()
            && host.ends_with(".invalid")
        {
            let authorization = request
                .headers
                .get("authorization")
                .and_then(|value| value.to_str().ok())
                .map(str::to_string);
            self.captures.lock().unwrap().push(CapturedRequest {
                host: host.to_string(),
                authorization,
            });
            return Ok(json_response(
                404,
                br#"{"error":{"message":"fake byok endpoint"}}"#.to_vec(),
            ));
        }

        Ok(synth_non_inference_response(&request.url))
    }
}

fn json_response(status: u16, body: Vec<u8>) -> CopilotHttpResponse {
    let mut headers = HeaderMap::new();
    headers.insert(
        "content-type",
        http::HeaderValue::from_static("application/json"),
    );
    let body = futures_util::stream::iter([Ok::<Bytes, CopilotRequestError>(Bytes::from(body))]);
    CopilotHttpResponse::new(status, None, headers, Box::pin(body))
}

fn synth_non_inference_response(url: &str) -> CopilotHttpResponse {
    let lower = url.to_lowercase();
    if lower.ends_with("/models") {
        return json_response(
            200,
            br#"{"data":[{"id":"gpt-4o","name":"GPT-4o","object":"model","vendor":"OpenAI","version":"1","preview":false,"model_picker_enabled":true,"capabilities":{"type":"chat","family":"gpt-4o","tokenizer":"o200k_base","limits":{"max_context_window_tokens":128000,"max_output_tokens":4096},"supports":{"streaming":true,"tool_calls":true,"parallel_tool_calls":true}}}]}"#
                .to_vec(),
        );
    }
    if lower.contains("/models/session") {
        return json_response(200, b"{}".to_vec());
    }
    if lower.contains("/policy") {
        return json_response(200, br#"{"state":"enabled"}"#.to_vec());
    }
    json_response(200, b"{}".to_vec())
}

async fn run_turn(
    client: &github_copilot_sdk::Client,
    providers: Vec<NamedProviderConfig>,
    models: Vec<ProviderModelConfig>,
    selection_id: &str,
    prompt: &str,
) {
    let session = client
        .create_session(
            SessionConfig::default()
                .with_permission_handler(Arc::new(ApproveAllHandler))
                .with_model(selection_id)
                .with_providers(providers)
                .with_models(models),
        )
        .await
        .expect("create session");
    let _ = session.send_and_wait(MessageOptions::new(prompt)).await;
    let _ = session.disconnect().await;
}

#[tokio::test]
async fn callback_token_is_applied_as_authorization_header() {
    // The runtime's LLM inference provider slot is process-global and is never released
    // when the registering connection disconnects (runtime `shared_api/llm_inference.rs`).
    // Over the in-process transport all clients share this process's runtime, so once a
    // BYOK provider is registered here and the client stops, the dangling registration
    // routes every later model-inference request (list-models, tool-using turns, hooks,
    // …) to the dead connection and hangs them. Registering a BYOK provider in-process
    // therefore poisons the shared runtime for the rest of the suite. The BYOK bearer-token
    // wiring is covered over stdio (a separate child process per test); the SDK-side
    // request/response plumbing is transport-agnostic.
    if super::support::skip_inprocess(
        "registering a BYOK LLM inference provider is process-global in-process and is never \
         released on disconnect, poisoning later model-inference tests",
    ) {
        return;
    }
    with_e2e_context_no_snapshot(|ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let handler = Arc::new(CapturingRequestHandler::default());
            let client = ctx.start_llm_client(handler.clone(), &[]).await;
            handler.reset();

            let calls = Arc::new(AtomicUsize::new(0));
            let callback_calls = calls.clone();
            let providers = vec![
                NamedProviderConfig::new("mi", PRIMARY_BASE_URL)
                    .with_provider_type("openai")
                    .with_wire_api("completions")
                    .with_bearer_token_provider(Arc::new(move |_args: ProviderTokenArgs| {
                        let callback_calls = callback_calls.clone();
                        async move {
                            callback_calls.fetch_add(1, Ordering::SeqCst);
                            Ok::<_, BearerTokenError>("sentinel-bearer-token-abc123".to_string())
                        }
                    })),
            ];
            let models =
                vec![ProviderModelConfig::new("default", "mi").with_wire_model("byok-gpt-4o")];

            run_turn(&client, providers, models, "mi/default", "What is 5+5?").await;

            assert!(
                calls.load(Ordering::SeqCst) >= 1,
                "expected callback to be invoked"
            );
            // Validate the captured Authorization header is the final assertion.
            assert!(
                handler
                    .auth_headers()
                    .contains(&"Bearer sentinel-bearer-token-abc123".to_string()),
                "expected captured Authorization headers to include the sentinel token, got {:?}",
                handler.auth_headers()
            );

            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn reacquires_a_fresh_token_for_each_request() {
    // The runtime registers the LLM inference provider per connection and, by design,
    // never releases the slot on disconnect (runtime `shared_api/llm_inference.rs`). Over
    // the in-process transport every client shares this process's runtime, so a second
    // provider-registering client is refused ("Another client is already the LLM
    // inference provider"). The BYOK bearer-token behavior over the in-process transport
    // is covered by `callback_token_is_applied_as_authorization_header`; this scenario's
    // provider-dispatch logic is transport-agnostic and is covered over stdio.
    if super::support::skip_inprocess(
        "llmInference.setProvider is process-global in-process; a second provider client is refused",
    ) {
        return;
    }
    with_e2e_context_no_snapshot(|ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let handler = Arc::new(CapturingRequestHandler::default());
            let client = ctx.start_llm_client(handler.clone(), &[]).await;
            handler.reset();

            let calls = Arc::new(AtomicUsize::new(0));
            let callback_calls = calls.clone();
            let providers = vec![
                NamedProviderConfig::new("mi", PRIMARY_BASE_URL)
                    .with_provider_type("openai")
                    .with_wire_api("completions")
                    .with_bearer_token_provider(Arc::new(move |_args: ProviderTokenArgs| {
                        let callback_calls = callback_calls.clone();
                        async move {
                            let call = callback_calls.fetch_add(1, Ordering::SeqCst) + 1;
                            Ok::<_, BearerTokenError>(format!("rotating-token-{call}"))
                        }
                    })),
            ];
            let models =
                vec![ProviderModelConfig::new("default", "mi").with_wire_model("byok-gpt-4o")];

            run_turn(
                &client,
                providers.clone(),
                models.clone(),
                "mi/default",
                "What is 1+1?",
            )
            .await;
            run_turn(&client, providers, models, "mi/default", "What is 2+2?").await;

            let auths = handler.auth_headers();
            assert!(
                auths.len() >= 2,
                "expected at least 2 captured Authorization headers, got {auths:?}"
            );
            assert!(
                auths[0].starts_with("Bearer rotating-token-")
                    && auths[1].starts_with("Bearer rotating-token-"),
                "expected rotating-token bearer headers, got {auths:?}"
            );
            assert!(
                calls.load(Ordering::SeqCst) >= 2,
                "expected callback to be invoked at least twice"
            );
            // Validate the captured Authorization header is the final assertion.
            assert_ne!(
                auths[0], auths[1],
                "expected distinct tokens per request, both were {:?}",
                auths[0]
            );

            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn dispatches_token_acquisition_per_provider() {
    // See `reacquires_a_fresh_token_for_each_request`: in-process, the process-global LLM
    // inference provider registration is not released on disconnect, so this additional
    // provider-registering client is refused. The BYOK transport path is covered in-process
    // by `callback_token_is_applied_as_authorization_header`; the per-provider dispatch
    // logic exercised here is transport-agnostic and covered over stdio.
    if super::support::skip_inprocess(
        "llmInference.setProvider is process-global in-process; a second provider client is refused",
    ) {
        return;
    }
    with_e2e_context_no_snapshot(|ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let handler = Arc::new(CapturingRequestHandler::default());
            let client = ctx.start_llm_client(handler.clone(), &[]).await;
            handler.reset();

            let acquired_for = Arc::new(std::sync::Mutex::new(Vec::new()));
            let make_provider =
                |name: &'static str, base_url: &'static str, token: &'static str| {
                    let acquired_for = acquired_for.clone();
                    NamedProviderConfig::new(name, base_url)
                        .with_provider_type("openai")
                        .with_wire_api("completions")
                        .with_bearer_token_provider(Arc::new(move |args: ProviderTokenArgs| {
                            let acquired_for = acquired_for.clone();
                            async move {
                                assert_eq!(args.provider_name, name);
                                assert!(
                                    !args.session_id.is_empty(),
                                    "expected a non-empty session id in token args"
                                );
                                acquired_for.lock().unwrap().push(name.to_string());
                                Ok::<_, BearerTokenError>(token.to_string())
                            }
                        }))
                };
            let providers = vec![
                make_provider("red", RED_BASE_URL, "token-for-red"),
                make_provider("blue", BLUE_BASE_URL, "token-for-blue"),
            ];
            let models = vec![
                ProviderModelConfig::new("default", "red").with_wire_model("byok-gpt-4o"),
                ProviderModelConfig::new("default", "blue").with_wire_model("byok-gpt-4o"),
            ];

            run_turn(
                &client,
                providers.clone(),
                models.clone(),
                "red/default",
                "What is 3+3?",
            )
            .await;
            run_turn(&client, providers, models, "blue/default", "What is 4+4?").await;

            let acquired = acquired_for.lock().unwrap().clone();
            assert!(acquired.contains(&"red".to_string()));
            assert!(acquired.contains(&"blue".to_string()));
            assert_eq!(
                handler.auth_header_for_host(RED_HOST).as_deref(),
                Some("Bearer token-for-red")
            );
            // Validate the captured Authorization header is the final assertion.
            assert_eq!(
                handler.auth_header_for_host(BLUE_HOST).as_deref(),
                Some("Bearer token-for-blue")
            );

            client.stop().await.expect("stop client");
        })
    })
    .await;
}
