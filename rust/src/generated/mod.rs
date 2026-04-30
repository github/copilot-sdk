//! Auto-generated protocol types — do not edit manually.
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
