//! GitHub telemetry forwarding callback surface.
//!
//! The runtime forwards per-session GitHub (hydro) telemetry to opted-in host
//! connections via the `gitHubTelemetry.event` JSON-RPC notification. The
//! payload types (`GitHubTelemetryNotification`, `GitHubTelemetryEvent`,
//! `GitHubTelemetryClientInfo`) are generated from the protocol schema and
//! re-exported here so consumers can register a callback against them via
//! [`ClientOptions::on_github_telemetry`](crate::ClientOptions::on_github_telemetry).
//!
//! Experimental: this surface is part of the GitHub telemetry forwarding
//! feature and may change or be removed without notice.

use std::sync::Arc;

#[doc(hidden)]
pub use crate::generated::api_types::{
    GitHubTelemetryClientInfo, GitHubTelemetryEvent, GitHubTelemetryNotification,
};

/// Callback invoked for each `gitHubTelemetry.event` notification forwarded by
/// the runtime to a connection that opted into telemetry forwarding.
///
/// Set via
/// [`ClientOptions::on_github_telemetry`](crate::ClientOptions::on_github_telemetry).
/// Registering a callback auto-enables telemetry forwarding on every session
/// created or resumed by the client.
#[doc(hidden)]
pub type GitHubTelemetryCallback = Arc<dyn Fn(GitHubTelemetryNotification) + Send + Sync>;
