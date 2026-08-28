//! Session-scoped GitHub token provider callbacks.

use std::collections::HashMap;
use std::future::Future;
use std::sync::{Arc, OnceLock, Weak};

use async_trait::async_trait;
use parking_lot::Mutex;
use serde_json::Value;

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

#[derive(Default)]
struct RegistryState {
    providers: HashMap<String, Arc<dyn GitHubTokenProvider>>,
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
        self.state
            .lock()
            .providers
            .insert(registration_id.clone(), provider);
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

    pub(crate) async fn dispatch(&self, request: JsonRpcRequest) {
        let Some(inner) = self.client.get().and_then(Weak::upgrade) else {
            return;
        };
        let client = Client::from_inner(inner);
        let _reverse_rpc = client.trace_reverse_request_scheduled(request.id);
        let params = request
            .params
            .clone()
            .unwrap_or(Value::Object(serde_json::Map::new()));
        let params: GitHubTokenAcquireRequest = match serde_json::from_value(params) {
            Ok(params) => params,
            Err(error) => {
                send_error(
                    &client,
                    request.id,
                    error_codes::INVALID_PARAMS,
                    &format!("invalid params: {error}"),
                )
                .await;
                return;
            }
        };
        let provider = self
            .state
            .lock()
            .providers
            .get(&params.registration_id)
            .cloned();
        let Some(provider) = provider else {
            send_error(
                &client,
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
                    &client,
                    request.id,
                    error_codes::INVALID_PARAMS,
                    "unknown GitHub token acquisition reason",
                )
                .await;
                return;
            }
        };

        match provider
            .get_token(GitHubTokenProviderArgs {
                host: params.host,
                session_id: params.session_id,
                reason,
            })
            .await
        {
            Ok(GitHubTokenProviderResult::Token(token)) => {
                respond(
                    &client,
                    request.id,
                    GitHubTokenAcquireResult::Token(token.into_wire()),
                )
                .await;
            }
            Ok(GitHubTokenProviderResult::Cancelled) => {
                respond(
                    &client,
                    request.id,
                    GitHubTokenAcquireResult::Cancelled(GitHubTokenAcquireResultCancelled {
                        kind: Default::default(),
                    }),
                )
                .await;
            }
            Err(error) => {
                send_error(
                    &client,
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

async fn respond(client: &Client, request_id: u64, result: GitHubTokenAcquireResult) {
    match serde_json::to_value(result) {
        Ok(result) => {
            let _ = client
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

async fn send_error(client: &Client, request_id: u64, code: i32, message: &str) {
    let _ = client
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
