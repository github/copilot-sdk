//! Auto-generated protocol types — **not part of the public API**.
//!
//! This module is crate-private. Its layout, item visibility, and
//! naming may change at any time without notice.
//!
//! Public callers reach the generated types through the stable
//! re-export modules at the crate root:
//!
//! - [`crate::session_events`] for session event payload types
//! - [`crate::rpc`] for JSON-RPC request/response types and typed
//!   namespace builders
//!
//! Generated from the Copilot protocol JSON Schemas by `scripts/codegen/rust.ts`.
#![allow(missing_docs)]
#![allow(rustdoc::bare_urls)]

pub mod api_types;
pub mod rpc;
pub mod session_events;

// Re-export session event types at the module root — no conflicts with
// hand-written types. API types are kept namespaced under `api_types::`
// because some names (Tool, ModelCapabilities, etc.) overlap with the
// hand-written SDK API types in `types.rs`.
pub use session_events::*;
