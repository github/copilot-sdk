//! Host-owned creation for the experimental session management API.

use async_trait::async_trait;

use crate::Error;
pub use crate::rpc::{
    SessionManagementCloseRequest, SessionManagementCloseResult, SessionManagementConfigureRequest,
    SessionManagementCreateHostRequest, SessionManagementCreateRequest,
    SessionManagementCreateResult, SessionManagementPolicy, SessionManagementScope,
};

/// Creates and supervises a separate session with the requested launch configuration.
///
/// Register with [`Session::set_session_creation_handler`](crate::session::Session::set_session_creation_handler).
/// The runtime sends the initial task only after validating the returned session.
/// The host must retain the new session, route approvals, preserve the permission
/// ceiling in the request, and clean up its sessions on shutdown.
/// Create it with the exact `new_session_id` allocated by the runtime.
///
/// Calls may run concurrently. Cancellation or connection closure drops the
/// callback future; implementations must handle cancellation without leaking
/// an unowned session.
#[async_trait]
pub trait SessionCreationHandler: Send + Sync + 'static {
    /// Materialize the requested session, returning its live runtime ID.
    async fn create_session(
        &self,
        request: SessionManagementCreateHostRequest,
    ) -> Result<SessionManagementCreateResult, Error>;
}
