//! Typed, same-checkout calls for the CLI host.
//!
//! These accessors grant no authority. Only the native CLI host can provision its connection.
//!
//! Published SDK builds have no privileged startup helper, including with `local-runtime`:
//!
#![cfg_attr(
    not(copilot_cli_host),
    doc = "```compile_fail\nuse github_copilot_sdk::native_cli::start;\n```"
)]

use crate::Client;
pub use crate::generated::cli_api_types::*;

/// Typed internal calls on the CLI's existing connection. This grants no authority.
pub fn rpc(client: &Client) -> crate::generated::cli_rpc::ClientRpc<'_> {
    crate::generated::cli_rpc::ClientRpc { client }
}

/// Observe transport closure without acquiring control of the SDK connection.
///
/// The returned child token cannot cancel the underlying transport.
pub fn connection_closed_token(client: &Client) -> tokio_util::sync::CancellationToken {
    client.inner.rpc.connection_closed_token().child_token()
}
