//! Auto-generated typed JSON-RPC namespace — do not edit manually.
//!
//! Generated from `api.schema.json` by `scripts/codegen/rust.ts`. The
//! [`ClientRpc`] and [`SessionRpc`] view structs let callers reach every
//! protocol method through a typed namespace tree, so wire method names
//! and request/response shapes live in exactly one place — this file.

#![allow(missing_docs)]
#![allow(clippy::too_many_arguments)]
#![allow(deprecated)]
#![allow(dead_code)]

use super::cli_api_types::{rpc_methods, *};
use crate::session::Session;
use crate::{Client, Error};

/// Typed view over the [`Client`]'s server-level RPC namespace.
#[derive(Clone, Copy)]
pub struct ClientRpc<'a> {
    pub(crate) client: &'a Client,
}

impl<'a> ClientRpc<'a> {
    /// `cafeMetrics.*` sub-namespace.
    pub fn cafe_metrics(&self) -> ClientRpcCafeMetrics<'a> {
        ClientRpcCafeMetrics {
            client: self.client,
        }
    }

    /// `providersSource.*` sub-namespace.
    pub fn providers_source(&self) -> ClientRpcProvidersSource<'a> {
        ClientRpcProvidersSource {
            client: self.client,
        }
    }

    /// `session.*` sub-namespace.
    pub fn session(&self) -> ClientRpcSession<'a> {
        ClientRpcSession {
            client: self.client,
        }
    }
}

/// `cafeMetrics.*` RPCs.
#[derive(Clone, Copy)]
pub struct ClientRpcCafeMetrics<'a> {
    pub(crate) client: &'a Client,
}

impl<'a> ClientRpcCafeMetrics<'a> {
    /// Starts anonymous CAFE metrics for this process. The runtime selects the endpoint from COPILOT_CAFE_METRICS_URL or this connection's original auth owner. Offline and unsupported platforms refuse startup. Release eligibility must resolve before samples are sent. Requires the private trusted CLI host capability.
    ///
    /// Wire method: `cafeMetrics.start`.
    ///
    /// # Parameters
    ///
    /// * `params` - Versions and provider configuration for CAFE.
    ///
    /// # Returns
    ///
    /// CAFE startup result.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub async fn start(
        &self,
        params: CafeMetricsStartRequest,
    ) -> Result<CafeMetricsStartResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        self.client
            .call_typed(rpc_methods::CAFEMETRICS_START, Some(wire_params))
            .await
    }

    /// Resolves release eligibility once from the immutable release lookup shared with auto-update. Requires the private trusted CLI host capability.
    ///
    /// Wire method: `cafeMetrics.resolveAdmission`.
    ///
    /// # Parameters
    ///
    /// * `params` - Immutable release lookup result.
    ///
    /// # Returns
    ///
    /// Admission result.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub async fn resolve_admission(
        &self,
        params: CafeMetricsResolveAdmissionRequest,
    ) -> Result<CafeMetricsResolveAdmissionResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        self.client
            .call_typed(rpc_methods::CAFEMETRICS_RESOLVEADMISSION, Some(wire_params))
            .await
    }

    /// Records one terminal resume result. Does nothing before startup. Requires the private trusted CLI host capability.
    ///
    /// Wire method: `cafeMetrics.recordResumeResult`.
    ///
    /// # Parameters
    ///
    /// * `params` - Terminal result of one resume attempt.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub async fn record_resume_result(
        &self,
        params: CafeMetricsRecordResumeResultRequest,
    ) -> Result<(), Error> {
        let wire_params = serde_json::to_value(params)?;
        let _: serde::de::IgnoredAny = self
            .client
            .call_typed(
                rpc_methods::CAFEMETRICS_RECORDRESUMERESULT,
                Some(wire_params),
            )
            .await?;
        Ok(())
    }

    /// Stops recording and drains within one 1.5-second deadline. flushOnly sends at most one batch without stopping recording. Requires the private trusted CLI host capability.
    ///
    /// Wire method: `cafeMetrics.shutdown`.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub async fn shutdown(&self) -> Result<(), Error> {
        let wire_params = serde_json::json!({});
        let _: serde::de::IgnoredAny = self
            .client
            .call_typed(rpc_methods::CAFEMETRICS_SHUTDOWN, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Stops recording and drains within one 1.5-second deadline. flushOnly sends at most one batch without stopping recording. Requires the private trusted CLI host capability.
    ///
    /// Wire method: `cafeMetrics.shutdown`.
    ///
    /// # Parameters
    ///
    /// * `params` - Preliminary flush or final disposal.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub async fn shutdown_with_params(
        &self,
        params: CafeMetricsShutdownRequest,
    ) -> Result<(), Error> {
        let wire_params = serde_json::to_value(params)?;
        let _: serde::de::IgnoredAny = self
            .client
            .call_typed(rpc_methods::CAFEMETRICS_SHUTDOWN, Some(wire_params))
            .await?;
        Ok(())
    }
}

/// `providersSource.*` RPCs.
#[derive(Clone, Copy)]
pub struct ClientRpcProvidersSource<'a> {
    pub(crate) client: &'a Client,
}

impl<'a> ClientRpcProvidersSource<'a> {
    /// Forwards a saved-model addition over the original authenticated local attachment to its owning server. Requires private CLI-host admission and fails if that attachment is no longer active; never saves to the controller as a fallback.
    ///
    /// Wire method: `providersSource.addModel`.
    ///
    /// # Parameters
    ///
    /// * `params` - Saves through the exact authenticated local attachment that discovered a model.
    ///
    /// # Returns
    ///
    /// Result of adding a model to the saved provider configuration.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub async fn add_model(
        &self,
        params: AttachedProviderConfigurationAddModelRequest,
    ) -> Result<SavedProviderConfigurationAddModelResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        self.client
            .call_typed(rpc_methods::PROVIDERSSOURCE_ADDMODEL, Some(wire_params))
            .await
    }
}

/// `session.*` RPCs.
#[derive(Clone, Copy)]
pub struct ClientRpcSession<'a> {
    pub(crate) client: &'a Client,
}

impl<'a> ClientRpcSession<'a> {
    /// `session.cafeMetrics.*` sub-namespace.
    pub fn cafe_metrics(&self) -> ClientRpcSessionCafeMetrics<'a> {
        ClientRpcSessionCafeMetrics {
            client: self.client,
        }
    }

    /// `session.canvasSource.*` sub-namespace.
    pub fn canvas_source(&self) -> ClientRpcSessionCanvasSource<'a> {
        ClientRpcSessionCanvasSource {
            client: self.client,
        }
    }

    /// `session.mcp.*` sub-namespace.
    pub fn mcp(&self) -> ClientRpcSessionMcp<'a> {
        ClientRpcSessionMcp {
            client: self.client,
        }
    }
}

/// `session.cafeMetrics.*` RPCs.
#[derive(Clone, Copy)]
pub struct ClientRpcSessionCafeMetrics<'a> {
    pub(crate) client: &'a Client,
}

impl<'a> ClientRpcSessionCafeMetrics<'a> {
    /// Records live model-call events, including subagents, and root compaction events. Replaces any earlier recorder. Requires the private trusted CLI host capability.
    ///
    /// Wire method: `session.cafeMetrics.attach`.
    ///
    /// # Parameters
    ///
    /// * `params` - Identifies the target session.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub async fn attach(&self, params: SessionCafeMetricsAttachParams) -> Result<(), Error> {
        let wire_params = serde_json::to_value(params)?;
        let _: serde::de::IgnoredAny = self
            .client
            .call_typed(rpc_methods::SESSION_CAFEMETRICS_ATTACH, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Stops recording this session. Does nothing if it has no recorder. Requires the private trusted CLI host capability.
    ///
    /// Wire method: `session.cafeMetrics.detach`.
    ///
    /// # Parameters
    ///
    /// * `params` - Identifies the target session.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub async fn detach(&self, params: SessionCafeMetricsDetachParams) -> Result<(), Error> {
        let wire_params = serde_json::to_value(params)?;
        let _: serde::de::IgnoredAny = self
            .client
            .call_typed(rpc_methods::SESSION_CAFEMETRICS_DETACH, Some(wire_params))
            .await?;
        Ok(())
    }
}

/// `session.canvasSource.*` RPCs.
#[derive(Clone, Copy)]
pub struct ClientRpcSessionCanvasSource<'a> {
    pub(crate) client: &'a Client,
}

impl<'a> ClientRpcSessionCanvasSource<'a> {
    /// Closes a canvas through its original session source. Requires the privately admitted in-process CLI host.
    ///
    /// Wire method: `session.canvasSource.close`.
    ///
    /// # Parameters
    ///
    /// * `params` - Closes a canvas through its original owned or attached session namespace.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub async fn close(&self, params: CanvasSourceCloseRequest) -> Result<(), Error> {
        let wire_params = serde_json::to_value(params)?;
        let _: serde::de::IgnoredAny = self
            .client
            .call_typed(rpc_methods::SESSION_CANVASSOURCE_CLOSE, Some(wire_params))
            .await?;
        Ok(())
    }
}

/// `session.mcp.*` RPCs.
#[derive(Clone, Copy)]
pub struct ClientRpcSessionMcp<'a> {
    pub(crate) client: &'a Client,
}

impl<'a> ClientRpcSessionMcp<'a> {
    /// `mcp.oauth.*` sub-namespace.
    pub fn oauth(&self) -> ClientRpcSessionMcpOauth<'a> {
        ClientRpcSessionMcpOauth {
            client: self.client,
        }
    }
}

/// `mcp.oauth.*` RPCs.
#[derive(Clone, Copy)]
pub struct ClientRpcSessionMcpOauth<'a> {
    pub(crate) client: &'a Client,
}

impl<'a> ClientRpcSessionMcpOauth<'a> {
    /// Reserves a host-driven OAuth flow identity before authentication or cancellation.
    ///
    /// Wire method: `session.mcp.oauth.prepareFlow`.
    ///
    /// # Parameters
    ///
    /// * `params` - Correlation identity reserved before an effectful authentication request.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub async fn prepare_flow(
        &self,
        params: SessionMcpOauthPrepareFlowParams,
    ) -> Result<(), Error> {
        let wire_params = serde_json::to_value(params)?;
        let _: serde::de::IgnoredAny = self
            .client
            .call_typed(
                rpc_methods::SESSION_MCP_OAUTH_PREPAREFLOW,
                Some(wire_params),
            )
            .await?;
        Ok(())
    }

    /// Cancels an exact host-driven OAuth flow on its original session requester.
    ///
    /// Wire method: `session.mcp.oauth.cancelFlow`.
    ///
    /// # Parameters
    ///
    /// * `params` - Correlation identity reserved before an effectful authentication request.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub async fn cancel_flow(&self, params: SessionMcpOauthCancelFlowParams) -> Result<(), Error> {
        let wire_params = serde_json::to_value(params)?;
        let _: serde::de::IgnoredAny = self
            .client
            .call_typed(rpc_methods::SESSION_MCP_OAUTH_CANCELFLOW, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Authenticates an arbitrary MCP URL through the original session credential owner, with correlated host progress and authorization callbacks.
    ///
    /// Wire method: `session.mcp.oauth.authenticate`.
    ///
    /// # Parameters
    ///
    /// * `params` - Full host-driven authentication for an arbitrary, possibly unsaved, MCP URL.
    ///
    /// # Returns
    ///
    /// Token projection for the existing host-delegated provider.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub async fn authenticate(
        &self,
        params: SessionMcpOauthAuthenticateParams,
    ) -> Result<McpOauthAuthenticateResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        self.client
            .call_typed(
                rpc_methods::SESSION_MCP_OAUTH_AUTHENTICATE,
                Some(wire_params),
            )
            .await
    }

    /// Checks whether the selected MCP credential owner has a valid access token.
    ///
    /// Wire method: `session.mcp.oauth.hasValidTokens`.
    ///
    /// # Parameters
    ///
    /// * `params` - A narrow token-validity query against the chosen credential owner.
    ///
    /// # Returns
    ///
    /// Whether the credential operation succeeded.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub async fn has_valid_tokens(
        &self,
        params: SessionMcpOauthHasValidTokensParams,
    ) -> Result<SessionMcpOauthHasValidTokensResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        self.client
            .call_typed(
                rpc_methods::SESSION_MCP_OAUTH_HASVALIDTOKENS,
                Some(wire_params),
            )
            .await
    }

    /// Attempts silent MCP token refresh without opening a browser.
    ///
    /// Wire method: `session.mcp.oauth.tryRefreshTokens`.
    ///
    /// # Parameters
    ///
    /// * `params` - A narrow token-validity query against the chosen credential owner.
    ///
    /// # Returns
    ///
    /// Whether the credential operation succeeded.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub async fn try_refresh_tokens(
        &self,
        params: SessionMcpOauthTryRefreshTokensParams,
    ) -> Result<SessionMcpOauthTryRefreshTokensResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        self.client
            .call_typed(
                rpc_methods::SESSION_MCP_OAUTH_TRYREFRESHTOKENS,
                Some(wire_params),
            )
            .await
    }

    /// Stores a confidential MCP client's static secret in its selected credential owner.
    ///
    /// Wire method: `session.mcp.oauth.saveStaticClientSecret`.
    ///
    /// # Parameters
    ///
    /// * `params` - Persist a confidential client's static secret without exposing token-store CRUD.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub async fn save_static_client_secret(
        &self,
        params: SessionMcpOauthSaveStaticClientSecretParams,
    ) -> Result<(), Error> {
        let wire_params = serde_json::to_value(params)?;
        let _: serde::de::IgnoredAny = self
            .client
            .call_typed(
                rpc_methods::SESSION_MCP_OAUTH_SAVESTATICCLIENTSECRET,
                Some(wire_params),
            )
            .await?;
        Ok(())
    }

    /// Releases an explicit host-owned MCP OAuth store on its original session requester.
    ///
    /// Wire method: `session.mcp.oauth.disposeStore`.
    ///
    /// # Parameters
    ///
    /// * `params` - Dispose an explicit host store without altering the session authorizer's owner.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub async fn dispose_store(
        &self,
        params: SessionMcpOauthDisposeStoreParams,
    ) -> Result<(), Error> {
        let wire_params = serde_json::to_value(params)?;
        let _: serde::de::IgnoredAny = self
            .client
            .call_typed(
                rpc_methods::SESSION_MCP_OAUTH_DISPOSESTORE,
                Some(wire_params),
            )
            .await?;
        Ok(())
    }

    /// Completes a tokenless initialize handshake for an unsaved remote MCP URL, preserving session egress and the refusing authentication challenge.
    ///
    /// Wire method: `session.mcp.oauth.testConnection`.
    ///
    /// # Parameters
    ///
    /// * `params` - Tokenless initialize handshake for a possibly unsaved remote MCP server.
    ///
    /// # Returns
    ///
    /// Outcome of the tokenless handshake, with the original challenge when available.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub async fn test_connection(
        &self,
        params: SessionMcpOauthTestConnectionParams,
    ) -> Result<McpOauthTestConnectionResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        self.client
            .call_typed(
                rpc_methods::SESSION_MCP_OAUTH_TESTCONNECTION,
                Some(wire_params),
            )
            .await
    }

    /// Derives an MCP credential key through the selected session account and store owner.
    ///
    /// Wire method: `session.mcp.oauth.storeKey`.
    ///
    /// # Parameters
    ///
    /// * `params` - Derive a credential key through the selected account and store owner.
    ///
    /// # Returns
    ///
    /// The selected owner's OAuth credential key.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub async fn store_key(
        &self,
        params: SessionMcpOauthStoreKeyParams,
    ) -> Result<SessionMcpOauthStoreKeyResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        self.client
            .call_typed(rpc_methods::SESSION_MCP_OAUTH_STOREKEY, Some(wire_params))
            .await
    }
}

/// Typed view over a [`Session`]'s RPC namespace.
#[derive(Clone, Copy)]
pub(crate) struct SessionRpc<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpc<'a> {}
