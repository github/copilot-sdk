//! Internal resolution of the GitHub Copilot CLI binary.
//!
//! Resolution order (matches the .NET and TypeScript SDKs):
//!
//! 1. An explicit path supplied by the application via
//!    [`CliProgram::Path`](crate::CliProgram::Path).
//! 2. The `COPILOT_CLI_PATH` environment variable.
//! 3. The bundled CLI embedded in this crate at build time (gated on the
//!    default `bundled-cli` cargo feature).
//!
//! There is no PATH scanning and no walking of standard install locations.
//! If you've opted out of bundling (via `default-features = false`) and
//! neither `CliProgram::Path` nor `COPILOT_CLI_PATH` is set,
//! [`Client::start`](crate::Client::start) returns
//! [`Error::BinaryNotFound`](crate::Error::BinaryNotFound).

use std::env;
use std::path::{Path, PathBuf};

use tracing::warn;

use crate::Error;

/// Resolve the CLI binary, optionally overriding the directory the bundled
/// CLI is extracted to. Called by `Client::start` to thread
/// `ClientOptions::bundled_cli_extract_dir` through to
/// `embeddedcli::install_at`.
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
            "COPILOT_CLI_PATH is set but does not point to a file; falling back to bundled CLI"
        );
    }

    let bundled = match extract_dir {
        Some(dir) => crate::embeddedcli::install_at(dir),
        None => crate::embeddedcli::path(),
    };
    if let Some(path) = bundled {
        return Ok(path);
    }

    Err(Error::BinaryNotFound {
        name: "copilot",
        hint: "the Copilot CLI is not bundled in this build of github-copilot-sdk and \
               COPILOT_CLI_PATH is not set. Either keep the default `bundled-cli` cargo \
               feature enabled, set COPILOT_CLI_PATH, or supply an explicit path via \
               `CliProgram::Path(...)` on `ClientOptions::program`.",
    })
}
