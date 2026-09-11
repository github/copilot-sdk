//! JSON-RPC request/response types and typed namespace builders.
//!
//! All types are auto-generated from the Copilot CLI protocol schemas.
//! This module is the stable public access point — the underlying
//! crate-private modules where the types are defined are an
//! implementation detail whose layout may change.
//!
//! Use the [`crate::Client::rpc`] and [`crate::session::Session::rpc`] helper
//! methods to obtain a typed view over the protocol surface.

pub use crate::generated::api_types::*;
pub use crate::generated::rpc::*;

/// Optional exact-name query for active local messageable sessions.
#[derive(Debug, Clone, Default, serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ListMessageableSessionsRequest {
    /// Optional exact session name query. Matching semantics are owned by the local host.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
}

/// Sanitized active local session available for exact-ID messaging selection.
#[derive(Debug, Clone, Default, serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MessageableSession {
    /// Stable session ID to provide to `session.sendSessionMessage`.
    pub session_id: crate::SessionId,
    /// Current session name when available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Current session summary when available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary: Option<String>,
}

/// Sanitized active local sessions available for exact-ID messaging selection.
#[derive(Debug, Clone, Default, serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ListMessageableSessionsResult {
    /// Messageable sessions in deterministic session-ID order.
    pub sessions: Vec<MessageableSession>,
}

/// Actual recipient delivery class for an admitted cross-session message.
#[derive(Debug, Clone, Default, PartialEq, Eq, serde::Serialize, serde::Deserialize)]
pub enum SessionMessageDelivery {
    /// The recipient was idle and the message started a turn.
    #[serde(rename = "idle")]
    Idle,
    /// The message entered the active turn's safe steering boundary.
    #[serde(rename = "steering")]
    Steering,
    /// The message was admitted to the recipient queue.
    #[serde(rename = "queued")]
    Queued,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Parameters for one authenticated exact-target cross-session message.
#[derive(Debug, Clone, Default, serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SendSessionMessageRequest {
    /// Exact active local recipient session ID.
    pub target_session_id: String,
    /// Natural-language message content.
    pub content: String,
    /// Requested delivery mode. The host applies its existing default when omitted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub delivery: Option<SendMode>,
}

/// Recipient admission result for an authenticated cross-session message.
#[derive(Debug, Clone, Default, serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SendSessionMessageResult {
    /// Unique identifier assigned to the admitted message.
    pub message_id: String,
    /// Actual recipient delivery class at admission.
    pub delivery: SessionMessageDelivery,
    /// Sanitized recipient display name for presentation only.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub target_display_name: Option<String>,
}

impl SendRequest {
    /// Set the message provenance without changing other request options.
    ///
    /// When this is not called, the source field is omitted by default.
    pub fn with_source(mut self, source: crate::MessageSource) -> Self {
        self.source = Some(source.to_string());
        self
    }
}
