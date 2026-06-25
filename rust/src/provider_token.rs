/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

//! BYOK bearer-token provider callbacks.
//!
//! <div class="warning">
//!
//! **Experimental.** These types are part of an experimental wire-protocol
//! surface and may change or be removed in future SDK or CLI releases.
//!
//! </div>

use std::future::Future;

use async_trait::async_trait;

/// Arguments passed to a BYOK bearer-token provider callback.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol
/// surface and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ProviderTokenArgs {
    /// Name of the BYOK provider needing a token.
    ///
    /// This is `"default"` for the singular whole-session provider, otherwise
    /// the named provider's `name`.
    pub provider_name: String,

    /// Id of the session that triggered this token request.
    ///
    /// A client-level shared callback registered for many sessions can use this
    /// to resolve the owning session and scope token acquisition or caching per
    /// session.
    pub session_id: String,
}

/// Error returned by a [`BearerTokenProvider`].
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol
/// surface and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct BearerTokenError {
    message: String,
}

impl BearerTokenError {
    /// Construct a bearer-token error with a human-readable message.
    pub fn message(message: impl Into<String>) -> Self {
        Self {
            message: message.into(),
        }
    }

    /// Return the human-readable error message.
    pub fn as_str(&self) -> &str {
        &self.message
    }
}

impl std::fmt::Display for BearerTokenError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(&self.message)
    }
}

impl std::error::Error for BearerTokenError {}

impl From<String> for BearerTokenError {
    fn from(message: String) -> Self {
        Self::message(message)
    }
}

impl From<&str> for BearerTokenError {
    fn from(message: &str) -> Self {
        Self::message(message)
    }
}

/// Provider-side callback used to acquire bearer tokens for BYOK providers.
///
/// <div class="warning">
///
/// **Experimental.** This trait is part of an experimental wire-protocol
/// surface and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[async_trait]
pub trait BearerTokenProvider: Send + Sync {
    /// Acquire a bearer token without the `Bearer ` prefix.
    async fn get_token(&self, args: ProviderTokenArgs) -> Result<String, BearerTokenError>;
}

#[async_trait]
impl<F, Fut> BearerTokenProvider for F
where
    F: Fn(ProviderTokenArgs) -> Fut + Send + Sync,
    Fut: Future<Output = Result<String, BearerTokenError>> + Send,
{
    async fn get_token(&self, args: ProviderTokenArgs) -> Result<String, BearerTokenError> {
        (self)(args).await
    }
}
