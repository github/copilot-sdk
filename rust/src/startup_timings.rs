//! Per-phase timing breakdown for [`Client::start`](crate::Client::start).
//!
//! `Client::start` performs several sequential phases between "spawn the CLI"
//! and "client is ready to create sessions": resolving (and possibly
//! extracting) the CLI binary, spawning the subprocess, waiting for the TCP
//! port announcement, the `connect` protocol handshake, and the optional
//! `sessionFs.setProvider` / `llmInference.setProvider` registration RPCs.
//!
//! Each phase is already measured internally with an [`Instant`] and logged at
//! `debug`. [`StartupTimings`] aggregates those durations into a single value
//! so a host can attribute total startup latency ("time to first token"
//! groundwork) to a specific phase — e.g. separating "process exec cost" from
//! "handshake/negotiation cost" — instead of reconstructing it from scattered
//! log lines.
//!
//! Retrieve it after start via
//! [`Client::startup_timings`](crate::Client::startup_timings).
//!
//! [`Instant`]: std::time::Instant

use std::time::Duration;

/// Millisecond breakdown of the phases of [`Client::start`](crate::Client::start).
///
/// Every field is `Option<u64>` because a phase is only timed when it actually
/// runs: `program_resolve_ms` is `None` when the caller supplies an explicit
/// CLI path (no resolution/extraction), `port_wait_ms` is `Some` only for the
/// TCP transport, and `session_fs_ms` / `llm_handler_ms` are `Some` only when
/// the corresponding option is configured. `process_spawn_ms` is `None` for
/// transports that do not spawn a subprocess (external server, in-process
/// FFI runtime).
///
/// Durations are whole milliseconds, matching the existing `elapsed_ms`
/// tracing fields.
#[derive(Debug, Clone, Default, PartialEq, Eq)]
#[non_exhaustive]
pub struct StartupTimings {
    /// Time spent in `resolve::copilot_binary_with_extract_dir` locating (and,
    /// for a bundled CLI, extracting) the copilot binary. `None` when the
    /// caller passes an explicit [`CliProgram::Path`](crate::CliProgram::Path).
    pub program_resolve_ms: Option<u64>,
    /// Time spent spawning the CLI subprocess (`command.spawn()`). `None` for
    /// the external-server and in-process transports, which do not spawn a
    /// child.
    pub process_spawn_ms: Option<u64>,
    /// Time spent waiting for the TCP server to announce its listening port on
    /// stdout. `Some` only for the TCP transport.
    pub port_wait_ms: Option<u64>,
    /// Time spent on the `connect` protocol handshake in
    /// [`Client::verify_protocol_version`](crate::Client::verify_protocol_version),
    /// including the fallback to the legacy `ping` RPC.
    pub handshake_ms: Option<u64>,
    /// Time spent registering the filesystem provider via
    /// `sessionFs.setProvider`. `Some` only when
    /// [`ClientOptions::session_fs`](crate::ClientOptions::session_fs) is set.
    pub session_fs_ms: Option<u64>,
    /// Time spent registering the LLM inference provider via
    /// `llmInference.setProvider`. `Some` only when
    /// [`ClientOptions::request_handler`](crate::ClientOptions::request_handler)
    /// is set.
    pub llm_handler_ms: Option<u64>,
    /// Total wall-clock time for [`Client::start`](crate::Client::start), from
    /// entry to the client being ready. Always present.
    pub total_ms: Option<u64>,
}

impl StartupTimings {
    /// Whole milliseconds of `duration`, saturating at [`u64::MAX`].
    pub(crate) fn millis(duration: Duration) -> u64 {
        u64::try_from(duration.as_millis()).unwrap_or(u64::MAX)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn millis_truncates_to_whole_milliseconds() {
        assert_eq!(StartupTimings::millis(Duration::from_micros(1_999)), 1);
        assert_eq!(StartupTimings::millis(Duration::from_millis(250)), 250);
        assert_eq!(StartupTimings::millis(Duration::ZERO), 0);
    }

    #[test]
    fn default_leaves_every_phase_unset() {
        let timings = StartupTimings::default();
        assert_eq!(timings, StartupTimings::default());
        assert!(timings.program_resolve_ms.is_none());
        assert!(timings.process_spawn_ms.is_none());
        assert!(timings.port_wait_ms.is_none());
        assert!(timings.handshake_ms.is_none());
        assert!(timings.session_fs_ms.is_none());
        assert!(timings.llm_handler_ms.is_none());
        assert!(timings.total_ms.is_none());
    }
}
