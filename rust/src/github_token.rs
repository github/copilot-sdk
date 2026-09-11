//! Session-scoped GitHub token provider callbacks.

use std::collections::HashMap;
use std::future::Future;
use std::panic::AssertUnwindSafe;
use std::sync::{Arc, OnceLock, Weak};

use async_trait::async_trait;
use futures_util::FutureExt;
use parking_lot::Mutex;
use serde_json::Value;
use tokio::sync::mpsc;
use tokio::task::JoinHandle;

use crate::generated::api_types::{
    GitHubTokenAcquireReason, GitHubTokenAcquireRequest, GitHubTokenAcquireResult,
    GitHubTokenAcquireResultCancelled, GitHubTokenAcquireResultToken,
};
use crate::{Client, ClientInner, JsonRpcError, JsonRpcRequest, JsonRpcResponse, error_codes};

/// Why the runtime is requesting a GitHub token.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum GitHubTokenRequestReason {
    /// The session needs its initial token.
    Initial,
    /// The session needs a refreshed token.
    Refresh,
}

/// Context supplied when the runtime needs a GitHub token for a session.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct GitHubTokenProviderArgs {
    /// Effective GitHub host for which a token is required.
    pub host: String,
    /// Session receiving the token, when the runtime has assigned its ID.
    pub session_id: Option<crate::SessionId>,
    /// Whether this is the initial token acquisition or a refresh.
    pub reason: GitHubTokenRequestReason,
}

/// A GitHub access token returned by a session token provider.
///
/// `expires_in_seconds` is the positive remaining lifetime when the callback
/// completes. Production GitHub tokens typically last eight hours.
pub struct GitHubToken {
    access_token: String,
    expires_in_seconds: i64,
    token_type: Option<String>,
}

impl GitHubToken {
    /// Construct a token response with its remaining lifetime in seconds.
    pub fn new(access_token: impl Into<String>, expires_in_seconds: i64) -> Self {
        Self {
            access_token: access_token.into(),
            expires_in_seconds,
            token_type: None,
        }
    }

    /// Override the OAuth token type. The runtime defaults to `bearer` when unset.
    pub fn with_token_type(mut self, token_type: impl Into<String>) -> Self {
        self.token_type = Some(token_type.into());
        self
    }

    fn into_wire(self) -> GitHubTokenAcquireResultToken {
        GitHubTokenAcquireResultToken {
            access_token: self.access_token,
            expires_in: self.expires_in_seconds,
            kind: Default::default(),
            token_type: self.token_type,
        }
    }
}

impl std::fmt::Debug for GitHubToken {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("GitHubToken")
            .field("access_token", &"<redacted>")
            .field("expires_in_seconds", &self.expires_in_seconds)
            .field("token_type", &self.token_type)
            .finish()
    }
}

/// Result of acquiring a session-scoped GitHub token.
pub enum GitHubTokenProviderResult {
    /// A token was acquired.
    Token(GitHubToken),
    /// The host cancelled acquisition.
    Cancelled,
}

impl std::fmt::Debug for GitHubTokenProviderResult {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::Token(token) => f.debug_tuple("Token").field(token).finish(),
            Self::Cancelled => f.write_str("Cancelled"),
        }
    }
}

/// Async callback used to acquire GitHub tokens for one session.
#[async_trait]
pub trait GitHubTokenProvider: Send + Sync {
    /// Acquire a token or explicitly cancel the request.
    ///
    /// Initial cancellation, errors, and invalid token responses reject session
    /// creation or resume instead of falling back to ambient authentication.
    async fn get_token(
        &self,
        args: GitHubTokenProviderArgs,
    ) -> Result<GitHubTokenProviderResult, crate::Error>;
}

#[async_trait]
impl<F, Fut> GitHubTokenProvider for F
where
    F: Fn(GitHubTokenProviderArgs) -> Fut + Send + Sync,
    Fut: Future<Output = Result<GitHubTokenProviderResult, crate::Error>> + Send,
{
    async fn get_token(
        &self,
        args: GitHubTokenProviderArgs,
    ) -> Result<GitHubTokenProviderResult, crate::Error> {
        (self)(args).await
    }
}

struct ProviderRegistration {
    provider: Arc<dyn GitHubTokenProvider>,
    worker: Option<TokenWorker>,
}

struct TokenWorker {
    requests: mpsc::UnboundedSender<JsonRpcRequest>,
    task: JoinHandle<()>,
}

impl Drop for TokenWorker {
    fn drop(&mut self) {
        self.task.abort();
    }
}

#[derive(Default)]
struct RegistryState {
    providers: HashMap<String, ProviderRegistration>,
    session_owners: HashMap<crate::SessionId, String>,
}

pub(crate) struct GitHubTokenRegistry {
    state: Mutex<RegistryState>,
    client: OnceLock<Weak<ClientInner>>,
}

impl GitHubTokenRegistry {
    pub(crate) fn new() -> Self {
        Self {
            state: Mutex::new(RegistryState::default()),
            client: OnceLock::new(),
        }
    }

    pub(crate) fn set_client(&self, client: Weak<ClientInner>) {
        let _ = self.client.set(client);
    }

    pub(crate) fn register(&self, provider: Arc<dyn GitHubTokenProvider>) -> String {
        let registration_id = uuid::Uuid::new_v4().to_string();
        self.state.lock().providers.insert(
            registration_id.clone(),
            ProviderRegistration {
                provider,
                worker: None,
            },
        );
        registration_id
    }

    pub(crate) fn claim(&self, registration_id: &str, session_id: crate::SessionId) {
        let mut state = self.state.lock();
        if let Some(previous) = state
            .session_owners
            .insert(session_id, registration_id.to_string())
            && previous != registration_id
        {
            state.providers.remove(&previous);
        }
    }

    pub(crate) fn unregister(&self, registration_id: &str) {
        let mut state = self.state.lock();
        state.providers.remove(registration_id);
        state
            .session_owners
            .retain(|_, owned| owned != registration_id);
    }

    pub(crate) fn retire_session(&self, session_id: &crate::SessionId) {
        let mut state = self.state.lock();
        if let Some(registration_id) = state.session_owners.remove(session_id) {
            state.providers.remove(&registration_id);
        }
    }

    pub(crate) fn clear(&self) {
        let mut state = self.state.lock();
        state.providers.clear();
        state.session_owners.clear();
    }

    pub(crate) fn dispatch(&self, request: JsonRpcRequest) {
        let Some(client) = self.client.get().cloned() else {
            return;
        };
        let registration_id = request
            .params
            .as_ref()
            .and_then(|params| params.get("registrationId"))
            .and_then(Value::as_str);
        let mut state = self.state.lock();
        if let Some(registration) = registration_id.and_then(|id| state.providers.get_mut(id)) {
            let worker = registration.worker.get_or_insert_with(|| {
                let provider = registration.provider.clone();
                let (requests, mut rx) = mpsc::unbounded_channel();
                let task = tokio::spawn(async move {
                    // One worker per registration preserves callback order without
                    // holding up requests for other providers or sessions.
                    while let Some(request) = rx.recv().await {
                        Self::handle_request(&client, Some(provider.as_ref()), request).await;
                    }
                });
                TokenWorker { requests, task }
            });
            let _ = worker.requests.send(request);
        } else {
            // Invalid/retired registrations still receive the normal RPC error.
            tokio::spawn(async move {
                Self::handle_request(&client, None, request).await;
            });
        }
    }

    async fn handle_request(
        client: &Weak<ClientInner>,
        provider: Option<&dyn GitHubTokenProvider>,
        request: JsonRpcRequest,
    ) {
        let params = request
            .params
            .clone()
            .unwrap_or(Value::Object(serde_json::Map::new()));
        let params: GitHubTokenAcquireRequest = match serde_json::from_value(params) {
            Ok(params) => params,
            Err(error) => {
                send_error(
                    client,
                    request.id,
                    error_codes::INVALID_PARAMS,
                    &format!("invalid params: {error}"),
                )
                .await;
                return;
            }
        };
        let Some(provider) = provider else {
            send_error(
                client,
                request.id,
                error_codes::INTERNAL_ERROR,
                "unknown GitHub token provider registration",
            )
            .await;
            return;
        };

        let reason = match params.reason {
            GitHubTokenAcquireReason::Initial => GitHubTokenRequestReason::Initial,
            GitHubTokenAcquireReason::Refresh => GitHubTokenRequestReason::Refresh,
            GitHubTokenAcquireReason::Unknown => {
                send_error(
                    client,
                    request.id,
                    error_codes::INVALID_PARAMS,
                    "unknown GitHub token acquisition reason",
                )
                .await;
                return;
            }
        };

        let result = AssertUnwindSafe(async {
            provider
                .get_token(GitHubTokenProviderArgs {
                    host: params.host,
                    session_id: params.session_id,
                    reason,
                })
                .await
        })
        .catch_unwind()
        .await;
        let result = match result {
            Ok(result) => result,
            Err(_) => {
                send_error(
                    client,
                    request.id,
                    error_codes::INTERNAL_ERROR,
                    "GitHub token provider panicked",
                )
                .await;
                return;
            }
        };
        match result {
            Ok(GitHubTokenProviderResult::Token(token)) => {
                respond(
                    client,
                    request.id,
                    GitHubTokenAcquireResult::Token(token.into_wire()),
                )
                .await;
            }
            Ok(GitHubTokenProviderResult::Cancelled) => {
                respond(
                    client,
                    request.id,
                    GitHubTokenAcquireResult::Cancelled(GitHubTokenAcquireResultCancelled {
                        kind: Default::default(),
                    }),
                )
                .await;
            }
            Err(error) => {
                send_error(
                    client,
                    request.id,
                    error_codes::INTERNAL_ERROR,
                    &format!("GitHub token provider failed: {error}"),
                )
                .await;
            }
        }
    }
}

pub(crate) struct GitHubTokenRegistration {
    registry: Arc<GitHubTokenRegistry>,
    id: String,
}

impl GitHubTokenRegistration {
    pub(crate) fn new(registry: Arc<GitHubTokenRegistry>, id: String) -> Self {
        Self { registry, id }
    }

    pub(crate) fn id(&self) -> &str {
        &self.id
    }

    pub(crate) fn claim(&self, session_id: crate::SessionId) {
        self.registry.claim(&self.id, session_id);
    }
}

impl Drop for GitHubTokenRegistration {
    fn drop(&mut self) {
        self.registry.unregister(&self.id);
    }
}

async fn respond(client: &Weak<ClientInner>, request_id: u64, result: GitHubTokenAcquireResult) {
    match serde_json::to_value(result) {
        Ok(result) => {
            let Some(inner) = client.upgrade() else {
                return;
            };
            let _ = Client::from_inner(inner)
                .send_response(&JsonRpcResponse {
                    jsonrpc: "2.0".to_string(),
                    id: request_id,
                    result: Some(result),
                    error: None,
                })
                .await;
        }
        Err(_) => {
            send_error(
                client,
                request_id,
                error_codes::INTERNAL_ERROR,
                "serialization failure",
            )
            .await;
        }
    }
}

async fn send_error(client: &Weak<ClientInner>, request_id: u64, code: i32, message: &str) {
    let Some(inner) = client.upgrade() else {
        return;
    };
    let _ = Client::from_inner(inner)
        .send_response(&JsonRpcResponse {
            jsonrpc: "2.0".to_string(),
            id: request_id,
            result: None,
            error: Some(JsonRpcError {
                code,
                message: message.to_string(),
                data: None,
            }),
        })
        .await;
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn token_debug_is_redacted() {
        let token = GitHubToken::new("do-not-print", 28_800);
        assert!(!format!("{token:?}").contains("do-not-print"));
    }

    #[test]
    fn retiring_session_removes_its_provider() {
        let registry = GitHubTokenRegistry::new();
        let provider = Arc::new(|_args: GitHubTokenProviderArgs| async {
            Ok(GitHubTokenProviderResult::Cancelled)
        });
        let registration_id = registry.register(provider);
        let session_id = crate::SessionId::from("session-1");
        registry.claim(&registration_id, session_id.clone());

        registry.retire_session(&session_id);

        assert!(
            !registry
                .state
                .lock()
                .providers
                .contains_key(&registration_id)
        );
    }
}
