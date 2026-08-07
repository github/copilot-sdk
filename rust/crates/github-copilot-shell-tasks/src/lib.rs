/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

//! Shared Rust integration with the experimental `Windows.UI.Shell.Tasks`
//! ("Forerunner" / taskbar presence) API.
//!
//! This crate is the single home for the pieces that the Copilot desktop app
//! (`github/github-app`) and the Copilot CLI (`github/copilot-agent-runtime`)
//! would otherwise each re-implement. It is deliberately lightweight — no
//! `build.rs`, and its only dependencies are the Windows-only `windows` /
//! `windows-core` crates — so a napi addon or Tauri backend can depend on it
//! without pulling the full `github-copilot-sdk` client. The client crate
//! re-exports it as `github_copilot_sdk::shell_tasks` behind its `shell-tasks`
//! feature.
//!
//! - `contract` — the pure-`std`, FFI-free cross-process contract for the
//!   hover-card round-trip (named-pipe name, focus sidecar file name, and the
//!   sidecar payload format). Compiles and unit-tests on every platform.
//! - `content` — the data-driven `AppTaskContent` builder: a neutral
//!   `ContentSpec` plus a `#[cfg(windows)]` `build_content` that reproduces the
//!   identical WinRT call sequence both products otherwise hand-roll.
//! - `sparse` — the napi-free sparse (external-location) package registrar that
//!   grants an unpackaged exe a Windows package identity (required by the
//!   presence API), including the process-lifetime MTA keepalive. Windows-only
//!   routines; neutral result types compile everywhere.
//! - `bindings` — the vendored WinRT projection of the `AppTaskContract`,
//!   re-exported as `Windows`. Windows-only; see below.
//!
//! The WinRT-touching code (`content`'s builder, `sparse`'s registrar, and the
//! `bindings` projection) is `#[cfg(windows)]` and pulls the `windows` /
//! `windows-core` crates only on Windows (they are declared under
//! `[target.'cfg(windows)'.dependencies]`), so the crate still compiles as a
//! lean, pure-`std` no-op on every other platform.
#![warn(missing_docs)]
#![deny(rustdoc::broken_intra_doc_links)]
#![cfg_attr(test, allow(clippy::unwrap_used))]

pub mod contract;
pub mod content;
pub mod sparse;

// Vendored, machine-generated WinRT projection: it has no doc comments and is
// never hand-edited, so `missing_docs` (a crate-level `warn`) is allowed here to
// keep `bindings.rs` byte-for-byte verbatim (regenerate per the command at the
// top of that file). It is Windows-only because the projected types resolve to
// the `windows` crate's `Windows.Foundation.*` surface.
#[cfg(windows)]
#[allow(missing_docs)]
mod bindings;

/// The projected `Windows.UI.Shell.Tasks` namespace (`AppTaskContent`,
/// `AppTaskInfo`, `AppTaskResultAsset`, `AppTaskManager`, and their enums).
/// Present only on Windows (with the `shell-tasks` feature enabled).
#[cfg(windows)]
pub use bindings::Windows;

/// Re-export of the exact `windows` crate this crate is built against.
///
/// A consumer (e.g. the desktop app's Tauri backend) that adopts this crate as
/// an isolated WinRT "island" must funnel **all** of its `Windows.UI.Shell.Tasks`
/// call-sites — including the non-`Tasks` WinRT types those call-sites touch
/// (`Foundation::Uri`, `ApplicationModel::Package`, `Win32::System::Com`, …) —
/// through *this* re-export rather than declaring its own `windows` dependency.
/// That guarantees every value handed to the projected `Windows.UI.Shell.Tasks`
/// types comes from the same `windows` version the projection was generated
/// against, so a consumer pinned to an older `windows` for the rest of its graph
/// (Tauri/wry) can still drive the presence API without a version conflict.
#[cfg(windows)]
pub use ::windows;

/// Re-export of the exact `windows-core` crate this crate is built against
/// (source of `HSTRING`, `Result`, `Interface`, …). See [`windows`] for why a
/// WinRT-island consumer must route its core types through this re-export.
#[cfg(windows)]
pub use ::windows_core;
