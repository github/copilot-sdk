//! Session-scoped skill provider callbacks.
//!
//! <div class="warning">
//!
//! **Experimental.** These types are part of an experimental wire-protocol
//! surface and may change or be removed in future SDK or CLI releases.
//!
//! </div>

use async_trait::async_trait;

use crate::Error;
pub use crate::rpc::SkillProviderDescriptor;

/// Supplies session-scoped skills to the runtime.
///
/// <div class="warning">
///
/// **Experimental.** This trait is part of an experimental wire-protocol
/// surface and may change or be removed in future SDK or CLI releases.
///
/// </div>
///
/// Each inbound `skillProvider.*` request is dispatched on its own spawned
/// task, so implementations must be safe for concurrent calls. When the
/// runtime cancels a call, for example after its time limit or when the
/// session disconnects, the SDK drops the provider future and the runtime
/// ignores any later result. Register a
/// provider with [`SessionConfig::with_skill_provider`](crate::SessionConfig::with_skill_provider)
/// or [`ResumeSessionConfig::with_skill_provider`](crate::ResumeSessionConfig::with_skill_provider).
#[async_trait]
pub trait SkillProvider: Send + Sync + 'static {
    /// Return the catalog of skills this session exposes.
    async fn list_skills(&self) -> std::result::Result<Vec<SkillProviderDescriptor>, Error>;

    /// Return the markdown for one skill name.
    ///
    /// Return `Ok(None)` when the skill name is not found.
    async fn read_skill(&self, name: &str) -> std::result::Result<Option<String>, Error>;
}
