//! Host-owned storage for the runtime memory feature.
//!
//! <div class="warning">
//!
//! **Experimental.** These types are part of an experimental wire-protocol
//! surface and may change or be removed in future SDK or CLI releases.
//!
//! </div>

use async_trait::async_trait;
use serde::{Deserialize, Serialize};

use crate::Error;
use crate::types::SessionId;

/// A memory the agent asked to store, after validation, secret filtering and
/// approval.
///
/// <div class="warning">
///
/// **Experimental.** This type may change or be removed in a future release.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
#[non_exhaustive]
pub struct StoredMemory {
    /// ID of the session that stored the memory.
    pub session_id: SessionId,
    /// Short topic the memory is about.
    pub subject: String,
    /// The fact to remember.
    pub fact: String,
    /// Sources supporting the fact, such as file paths with line numbers.
    pub citations: String,
    /// Why the agent considers the fact worth remembering.
    pub reason: String,
}

/// Stores memories in the app instead of the built-in memory backends.
///
/// <div class="warning">
///
/// **Experimental.** This trait may change or be removed in a future release.
///
/// </div>
///
/// Register with [`SessionConfig::with_memory_store`](crate::SessionConfig::with_memory_store)
/// or [`ResumeSessionConfig::with_memory_store`](crate::ResumeSessionConfig::with_memory_store);
/// it requires memory enabled through `with_memory`, otherwise session setup
/// fails, and is not supported for cloud sessions. The SDK sends
/// `hasMemoryStore` on `session.create` / `session.resume` and serves callbacks
/// that arrive while either request is pending. The runtime validates the memory, filters secrets and requests approval, then
/// calls [`store`](Self::store). `store_memory` succeeds only after it returns
/// `Ok`; an error fails the tool call with a generic message and nothing is
/// stored elsewhere. While registered, the runtime retrieves no memories and
/// `vote_memory` is unavailable, so supply recall yourself, for example
/// through the system message. The SDK drops the `store` future when the
/// runtime cancels the call, for example after its 60-second timeout, or the
/// connection closes.
#[async_trait]
pub trait MemoryStore: Send + Sync + 'static {
    /// Persist one memory.
    async fn store(&self, memory: StoredMemory) -> Result<(), Error>;
}
