//! Internal resolution of the GitHub Copilot CLI binary.
//!
//! Resolution order:
//!
//! 1. An explicit path supplied by the application via
//!    [`CliProgram::Path`](crate::CliProgram::Path).
//! 2. The `COPILOT_CLI_PATH` environment variable.
//! 3. The bundled CLI embedded in this crate at build time (when the
//!    `bundled-cli` cargo feature is on, the default).
//! 4. The build-time-extracted CLI in the per-user cache (when
//!    `bundled-cli` is off).
//!
//! There is no PATH scanning and no walking of standard install locations.
//! If none of the above resolves to a real file,
//! [`Client::start`](crate::Client::start) returns
//! an [`ErrorKind::BinaryNotFound`](crate::ErrorKind::BinaryNotFound) error.

use std::env;
use std::path::{Path, PathBuf};

use tracing::warn;

use crate::{Error, ErrorKind};

/// Resolve the CLI binary, optionally overriding the directory the bundled
/// CLI is extracted to. Called by `Client::start` to thread
/// `ClientOptions::bundled_cli_extract_dir` through to
/// `embeddedcli::install_at`. `extract_dir` only applies when the
/// `bundled-cli` feature is on — with it off the binary lives at a
/// build-time-known conventional location and `extract_dir` is ignored
/// (there's no archive to re-extract; pointing the lookup elsewhere
/// would be exactly equivalent to setting `CliProgram::Path`). Set
/// `COPILOT_CLI_EXTRACT_DIR` at build time to relocate that extraction;
/// the same env var is honored at runtime to find binaries written
/// under it.
pub(crate) fn copilot_binary_with_extract_dir(
    extract_dir: Option<&Path>,
) -> Result<PathBuf, Error> {
    if let Ok(value) = env::var("COPILOT_CLI_PATH") {
        let candidate = PathBuf::from(&value);
        if candidate.is_file() {
            return Ok(candidate);
        }
        warn!(
            path = %candidate.display(),
            "COPILOT_CLI_PATH is set but does not point to a file; falling back"
        );
    }

    #[cfg(feature = "bundled-cli")]
    {
        let bundled = match extract_dir {
            Some(dir) => crate::embeddedcli::install_at(dir),
            None => crate::embeddedcli::path(),
        };
        if let Some(path) = bundled {
            return Ok(path);
        }
    }

    #[cfg(not(feature = "bundled-cli"))]
    {
        let _ = extract_dir;
        if let Some(path) = extracted_cli_path() {
            return Ok(path);
        }
    }

    Err(ErrorKind::BinaryNotFound {
        name: "copilot".into(),
        hint: Some(
            "the Copilot CLI is not bundled in this build of github-copilot-sdk and \
             COPILOT_CLI_PATH is not set. Either keep the default `bundled-cli` cargo \
             feature enabled, set COPILOT_CLI_PATH, or supply an explicit path via \
             `CliProgram::Path(...)` on `ClientOptions::program`."
                .into(),
        ),
    }.into())
}

/// Path to the CLI extracted into the per-user cache by `build.rs` when
/// `bundled-cli` is disabled. Returns `None` if the cached file is missing
/// (e.g. the user deleted the cache after building, or built with
/// `COPILOT_SKIP_CLI_DOWNLOAD`).
///
/// The path is recomputed from the build-time-baked
/// `COPILOT_SDK_CLI_VERSION`, the OS-derived binary name, and the
/// optional `COPILOT_CLI_EXTRACT_DIR` env var. This must match
/// `build.rs::extracted_install_dir` exactly — both sides implement the
/// same convention. We deliberately don't bake the resolved path into
/// the crate at build time: an absolute path leaks the build machine's
/// `$HOME` / `$LOCALAPPDATA` into the artifact, breaks sccache across
/// machines, and prevents copying `target/` between hosts.
#[cfg(all(not(feature = "bundled-cli"), has_extracted_cli))]
fn extracted_cli_path() -> Option<PathBuf> {
    let version = env!("COPILOT_SDK_CLI_VERSION");
    let binary = if cfg!(windows) {
        "copilot.exe"
    } else {
        "copilot"
    };

    let dir = match env::var_os("COPILOT_CLI_EXTRACT_DIR") {
        Some(custom) => PathBuf::from(custom),
        None => dirs::cache_dir()
            .unwrap_or_else(env::temp_dir)
            .join("github-copilot-sdk")
            .join("cli")
            .join(sanitize_version(version)),
    };

    let path = dir.join(binary);
    if path.is_file() {
        return Some(path);
    }
    warn!(
        path = %path.display(),
        "expected build-time-extracted CLI is missing; rebuild the crate or set COPILOT_CLI_PATH"
    );
    None
}

/// `has_extracted_cli` is absent when the target is unsupported or the
/// build opted out via `COPILOT_SKIP_CLI_DOWNLOAD`. In both cases there's
/// no binary to look up, so the resolver returns `None` immediately.
#[cfg(all(not(feature = "bundled-cli"), not(has_extracted_cli)))]
fn extracted_cli_path() -> Option<PathBuf> {
    None
}

/// Replace characters outside `[a-zA-Z0-9._-]` with `_`. Kept in sync
/// with `build.rs::sanitize_version` and `embeddedcli::sanitize_version`
/// so all three resolve to the same cache directory for any given
/// version.
#[cfg(all(not(feature = "bundled-cli"), has_extracted_cli))]
fn sanitize_version(version: &str) -> String {
    version
        .chars()
        .map(|c| match c {
            'a'..='z' | 'A'..='Z' | '0'..='9' | '.' | '-' | '_' => c,
            _ => '_',
        })
        .collect()
}
