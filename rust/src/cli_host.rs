//! Typed, same-checkout calls for the CLI host.
//!
//! These accessors grant no authority. Only the native CLI host can provision its connection.
//!
//! The SDK has no privileged startup helper, including with `local-runtime` enabled:
//!
//! ```compile_fail
//! use github_copilot_sdk::cli_host::start;
//! ```

use crate::Client;
pub use crate::generated::cli_api_types::*;

/// Typed internal calls on the CLI's existing connection. This grants no authority.
pub fn rpc(client: &Client) -> crate::generated::cli_rpc::ClientRpc<'_> {
    crate::generated::cli_rpc::ClientRpc { client }
}
