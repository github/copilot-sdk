/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

//! Shared cross-process contract for the taskbar hover-card round-trip.
//!
//! Two separate binaries cooperate to relay a hover-card interaction back into
//! the running CLI:
//!
//! - **`copilot.exe`** (the `cli-native` addon) hosts the named-pipe server and
//!   writes the focus *sidecar* file that carries the host window handle.
//! - **`copilot-taskbar-activator.exe`** is protocol-activated by the shell,
//!   connects to that pipe, and reads that sidecar.
//!
//! Because they are distinct processes (and, historically, distinct crates),
//! the pipe name, the sidecar file name, and the sidecar payload format were
//! duplicated in every producer/consumer site and had to be kept in lock-step
//! by hand. This crate is the **single source of truth** for those contracts so
//! the two ends cannot drift: both crates depend on it and call these functions
//! instead of re-implementing the rules.
//!
//! The logic is pure (`std`-only, no Windows FFI) so it compiles on every
//! platform and stays unit-testable; the Windows-specific I/O (creating the
//! pipe, joining `%TEMP%`, reading/writing the file) stays in the respective
//! callers.

/// Prefix of the per-session named pipe, before the sanitized session id.
pub const PIPE_NAME_PREFIX: &str = r"\\.\pipe\copilot-taskbar-";

/// Prefix of the per-session focus sidecar file name, before the sanitized
/// session id.
pub const SIDECAR_FILE_PREFIX: &str = "copilot-taskbar-";

/// Suffix (extension) of the per-session focus sidecar file name.
pub const SIDECAR_FILE_SUFFIX: &str = ".hwnd";

/// Sanitize a session id into the shared name stem used by both the pipe path
/// and the sidecar file name. Every character outside `[A-Za-z0-9._-]` is
/// replaced with `_`. Session ids are expected to be UUIDs, so in practice this
/// is an identity transform; the rule exists so a non-UUID id can never produce
/// an invalid pipe path or file name.
#[must_use]
pub fn sanitize_session_id(session_id: &str) -> String {
    session_id
        .chars()
        .map(|c| {
            if c.is_ascii_alphanumeric() || matches!(c, '.' | '_' | '-') {
                c
            } else {
                '_'
            }
        })
        .collect()
}

/// Full Windows named-pipe path for a session:
/// `\\.\pipe\copilot-taskbar-<sanitized id>`.
#[must_use]
pub fn pipe_path_for_session(session_id: &str) -> String {
    format!("{PIPE_NAME_PREFIX}{}", sanitize_session_id(session_id))
}

/// Bare file name (no directory) of the per-session focus sidecar:
/// `copilot-taskbar-<sanitized id>.hwnd`. Callers join this onto the temp
/// directory (`std::env::temp_dir()`) so the directory resolution stays in the
/// Windows-specific caller while the name contract lives here.
#[must_use]
pub fn sidecar_file_name(session_id: &str) -> String {
    format!(
        "{SIDECAR_FILE_PREFIX}{}{SIDECAR_FILE_SUFFIX}",
        sanitize_session_id(session_id)
    )
}

/// The parsed contents of a focus sidecar file.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct SidecarPayload {
    /// The host window handle as a raw `isize` (the platform HWND value).
    pub hwnd: isize,
    /// The process id that owned the window at capture time, when recorded.
    /// `None` for legacy sidecars written before owner-PID validation existed.
    pub owner_pid: Option<u32>,
    /// The terminal tab-match title (line 2), when present and non-blank.
    pub match_text: Option<String>,
}

/// Build the sidecar file contents. Line 1 is `"<decimal HWND> <owner PID>"` —
/// the host window handle plus the process id that owns it, captured at write
/// time. An optional line 2 carries the tab-match text (the terminal tab title)
/// for UI Automation tab selection in the activator. The match text is written
/// only when it is non-blank so an empty/branding-only title never produces an
/// over-broad "matches every Copilot tab" marker.
///
/// The owner PID lets the reader reject a *reused* HWND: top-level handles are
/// recycled by the OS, so a stale sidecar could otherwise foreground an
/// unrelated window that inherited the old handle value.
#[must_use]
pub fn format_sidecar(hwnd: isize, owner_pid: u32, match_text: Option<&str>) -> String {
    let header = format!("{hwnd} {owner_pid}");
    match match_text.map(str::trim).filter(|s| !s.is_empty()) {
        Some(text) => format!("{header}\n{text}"),
        None => header,
    }
}

/// Parse focus sidecar file contents produced by [`format_sidecar`]. Line 1 is
/// `"<decimal HWND> <owner PID>"` (the owner PID is optional for
/// forward/backward compatibility); an optional line 2 carries the tab-match
/// title. Returns `None` when the HWND is absent, unparsable, or zero — the
/// same "no usable window" signal the reader treats as a missing sidecar.
#[must_use]
pub fn parse_sidecar(contents: &str) -> Option<SidecarPayload> {
    let mut lines = contents.lines();
    let mut header = lines.next()?.split_whitespace();
    let hwnd: isize = header.next()?.trim().parse().ok()?;
    if hwnd == 0 {
        return None;
    }
    let owner_pid: Option<u32> = header.next().and_then(|p| p.parse().ok());
    let match_text = lines
        .next()
        .map(str::trim)
        .filter(|s| !s.is_empty())
        .map(str::to_string);
    Some(SidecarPayload {
        hwnd,
        owner_pid,
        match_text,
    })
}

/// Versioned contract test vectors for [`sanitize_session_id`], shared so every
/// producer/consumer asserts the identical rule. Each tuple is
/// `(input, expected sanitized stem)`.
pub const SANITIZE_VECTORS: &[(&str, &str)] = &[
    ("abc-123", "abc-123"),
    (
        "550e8400-e29b-41d4-a716-446655440000",
        "550e8400-e29b-41d4-a716-446655440000",
    ),
    ("a/b:c d", "a_b_c_d"),
    ("with.dot_and-dash", "with.dot_and-dash"),
    ("slash\\back", "slash_back"),
    ("unicode\u{00e9}", "unicode_"),
];

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn sanitize_matches_contract_vectors() {
        for (input, expected) in SANITIZE_VECTORS {
            assert_eq!(sanitize_session_id(input), *expected, "input {input:?}");
        }
    }

    #[test]
    fn pipe_path_uses_prefix_and_sanitized_id() {
        assert_eq!(
            pipe_path_for_session("abc-123"),
            r"\\.\pipe\copilot-taskbar-abc-123"
        );
        assert_eq!(
            pipe_path_for_session("a/b:c d"),
            r"\\.\pipe\copilot-taskbar-a_b_c_d"
        );
    }

    #[test]
    fn sidecar_file_name_uses_prefix_suffix_and_sanitized_id() {
        assert_eq!(sidecar_file_name("abc-123"), "copilot-taskbar-abc-123.hwnd");
        assert_eq!(sidecar_file_name("a/b:c d"), "copilot-taskbar-a_b_c_d.hwnd");
    }

    #[test]
    fn format_sidecar_writes_hwnd_and_pid_without_match_text() {
        assert_eq!(format_sidecar(12345, 6789, None), "12345 6789");
    }

    #[test]
    fn format_sidecar_appends_non_blank_match_text() {
        assert_eq!(
            format_sidecar(12345, 6789, Some("Copilot - tab")),
            "12345 6789\nCopilot - tab"
        );
    }

    #[test]
    fn format_sidecar_omits_blank_match_text() {
        assert_eq!(format_sidecar(1, 2, Some("   ")), "1 2");
    }

    #[test]
    fn format_sidecar_trims_match_text() {
        assert_eq!(format_sidecar(1, 2, Some("  tab  ")), "1 2\ntab");
    }

    #[test]
    fn round_trip_format_then_parse() {
        let text = format_sidecar(98765, 4321, Some("My Tab"));
        assert_eq!(
            parse_sidecar(&text),
            Some(SidecarPayload {
                hwnd: 98765,
                owner_pid: Some(4321),
                match_text: Some("My Tab".to_string()),
            })
        );
    }

    #[test]
    fn parse_rejects_zero_and_missing_hwnd() {
        assert_eq!(parse_sidecar("0 1234"), None);
        assert_eq!(parse_sidecar(""), None);
        assert_eq!(parse_sidecar("notanumber"), None);
    }

    #[test]
    fn parse_accepts_legacy_hwnd_only_sidecar() {
        assert_eq!(
            parse_sidecar("55555"),
            Some(SidecarPayload {
                hwnd: 55555,
                owner_pid: None,
                match_text: None,
            })
        );
    }

    #[test]
    fn parse_ignores_blank_match_text_line() {
        assert_eq!(
            parse_sidecar("42 7\n   "),
            Some(SidecarPayload {
                hwnd: 42,
                owner_pid: Some(7),
                match_text: None,
            })
        );
    }
}
