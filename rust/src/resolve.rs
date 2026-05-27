//! Internal resolution of the GitHub Copilot CLI binary.
//!
//! Resolution order (matches the .NET and TypeScript SDKs):
//!
//! 1. An explicit path supplied by the application via
//!    [`CliProgram::Path`](crate::CliProgram::Path).
//! 2. The `COPILOT_CLI_PATH` environment variable.
//! 3. The bundled CLI embedded in this crate at build time (gated on the
//!    default `bundled-cli` cargo feature).
//! 4. The build-time-extracted CLI in the per-user cache (when
//!    `bundled-cli` is disabled, i.e. dev builds).
//!
//! There is no PATH scanning and no walking of standard install locations.
//! If none of the above resolves to a real file,
//! [`Client::start`](crate::Client::start) returns
//! [`Error::BinaryNotFound`](crate::Error::BinaryNotFound).

use std::env;
use std::path::{Path, PathBuf};

use tracing::warn;

use crate::Error;

/// Resolve the CLI binary, optionally overriding the directory the bundled
/// CLI is extracted to. Called by `Client::start` to thread
/// `ClientOptions::bundled_cli_extract_dir` through to
/// `embeddedcli::install_at`. `extract_dir` only affects embed mode — in
/// dev mode the binary path is baked in at build time and `extract_dir`
/// is ignored (there's no archive to re-extract).
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
        if let Some(path) = dev_cli_path() {
            return Ok(path);
        }
    }

    Err(Error::BinaryNotFound {
        name: "copilot",
        hint: "the Copilot CLI is not bundled in this build of github-copilot-sdk and \
               COPILOT_CLI_PATH is not set. Either keep the default `bundled-cli` cargo \
               feature enabled, set COPILOT_CLI_PATH, or supply an explicit path via \
               `CliProgram::Path(...)` on `ClientOptions::program`.",
    })
}

/// Path to the CLI extracted into the per-user cache by `build.rs` when
/// `bundled-cli` is disabled. Returns `None` if the cached file is missing
/// (e.g. the user deleted the cache after building).
#[cfg(not(feature = "bundled-cli"))]
fn dev_cli_path() -> Option<PathBuf> {
    // `has_dev_cli` is emitted by build.rs only when it successfully extracted
    // the CLI for a supported target. On unsupported targets (where
    // target_platform() returns None) the cfg is absent.
    #[cfg(has_dev_cli)]
    {
        let path = PathBuf::from(env!("COPILOT_CLI_DEV_PATH"));
        if path.is_file() {
            return Some(path);
        }
        warn!(
            path = %path.display(),
            "build-time-extracted CLI is missing from cache; rebuild the crate to re-extract"
        );
    }
    None
}
