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
