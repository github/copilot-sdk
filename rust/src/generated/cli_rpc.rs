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
        let _value = self
            .client
            .call(rpc_methods::CAFEMETRICS_START, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
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
        let _value = self
            .client
            .call(rpc_methods::CAFEMETRICS_RESOLVEADMISSION, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
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
        let _value = self
            .client
            .call(
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
        let _value = self
            .client
            .call(rpc_methods::CAFEMETRICS_SHUTDOWN, Some(wire_params))
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
        let _value = self
            .client
            .call(rpc_methods::CAFEMETRICS_SHUTDOWN, Some(wire_params))
            .await?;
        Ok(())
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
        let _value = self
            .client
            .call(rpc_methods::SESSION_CAFEMETRICS_ATTACH, Some(wire_params))
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
        let _value = self
            .client
            .call(rpc_methods::SESSION_CAFEMETRICS_DETACH, Some(wire_params))
            .await?;
        Ok(())
    }
}

/// Typed view over a [`Session`]'s RPC namespace.
#[derive(Clone, Copy)]
pub(crate) struct SessionRpc<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpc<'a> {}
