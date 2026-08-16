//! Internal resolution of the GitHub Copilot CLI binary.
//!
//! Resolution order:
//!
//! 1. An explicit path supplied by the application via
//!    [`CliProgram::Path`](crate::CliProgram::Path).
//! 2. The `COPILOT_CLI_PATH` environment variable.
//! 3. The `COPILOT_RUNTIME_PATH` environment variable.
//! 4. The bundled CLI embedded in this crate at build time (when the
//!    `bundled-cli` cargo feature is on, the default).
//! 5. The build-time-extracted CLI in the per-user cache (when
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

pub(crate) struct ResolvedProgram {
    pub(crate) executable: PathBuf,
    pub(crate) residual_cli: Option<PathBuf>,
    pub(crate) is_runtime_wrapper: bool,
}

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
) -> Result<ResolvedProgram, Error> {
    if let Ok(value) = env::var("COPILOT_CLI_PATH") {
        let candidate = PathBuf::from(&value);
        if candidate.is_file() {
            return Ok(ResolvedProgram {
                executable: candidate,
                residual_cli: None,
                is_runtime_wrapper: false,
            });
        }
        warn!(
            path = %candidate.display(),
            "COPILOT_CLI_PATH is set but does not point to a file; falling back"
        );
    }

    if let Ok(value) = env::var("COPILOT_RUNTIME_PATH") {
        let candidate = PathBuf::from(&value);
        validate_runtime_pair(&candidate)?;
        let residual_cli = match env::var("COPILOT_RUNTIME_RESIDUAL_CLI_PATH") {
            Ok(value) => {
                let path = PathBuf::from(value);
                if !path.is_file() {
                    return Err(Error::with_message(
                        ErrorKind::BinaryNotFound {
                            name: cli_binary_name().into(),
                            hint: Some(
                                "COPILOT_RUNTIME_RESIDUAL_CLI_PATH must point to the compatible \
                                 residual CLI entrypoint for the local runtime"
                                    .into(),
                            ),
                        },
                        format!(
                            "COPILOT_RUNTIME_RESIDUAL_CLI_PATH does not point to a file: '{}'",
                            path.display()
                        ),
                    ));
                }
                Some(path)
            }
            Err(_) => candidate
                .parent()
                .map(|dir| dir.join(cli_binary_name()))
                .filter(|path| path.is_file()),
        };
        return Ok(ResolvedProgram {
            executable: candidate,
            residual_cli,
            is_runtime_wrapper: true,
        });
    }

    #[cfg(feature = "bundled-cli")]
    {
        let bundled = match extract_dir {
            Some(dir) => crate::embeddedcli::install_at(dir),
            None => crate::embeddedcli::path(),
        };
        if let Some(path) = bundled {
            let residual_cli = path.parent().map(|dir| dir.join(cli_binary_name()));
            return Ok(ResolvedProgram {
                executable: path,
                residual_cli,
                is_runtime_wrapper: true,
            });
        }
    }

    #[cfg(not(feature = "bundled-cli"))]
    {
        let _ = extract_dir;
        if let Some(program) = extracted_program() {
            return Ok(program);
        }
    }

    Err(ErrorKind::BinaryNotFound {
        name: runtime_binary_name().into(),
        hint: Some(
            "the Copilot CLI is not bundled in this build of github-copilot-sdk and \
             COPILOT_CLI_PATH and COPILOT_RUNTIME_PATH are not set. Either keep the default \
             `bundled-cli` cargo feature enabled, set one of those variables, or supply an explicit path via \
             `CliProgram::Path(...)` on `ClientOptions::program`."
                .into(),
        ),
    }
    .into())
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
fn extracted_program() -> Option<ResolvedProgram> {
    let version = env!("COPILOT_SDK_CLI_VERSION");
    let dir = match env::var_os("COPILOT_CLI_EXTRACT_DIR") {
        Some(custom) => PathBuf::from(custom),
        None => dirs::cache_dir()
            .unwrap_or_else(env::temp_dir)
            .join("github-copilot-sdk")
            .join("cli")
            .join(sanitize_version(version)),
    };

    let path = dir.join(runtime_binary_name());
    let residual_cli = dir.join(cli_binary_name());
    if validate_runtime_pair(&path).is_ok() && residual_cli.is_file() {
        return Some(ResolvedProgram {
            executable: path,
            residual_cli: Some(residual_cli),
            is_runtime_wrapper: true,
        });
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
fn extracted_program() -> Option<ResolvedProgram> {
    None
}

fn validate_runtime_pair(wrapper: &Path) -> Result<(), Error> {
    let wrapper_valid = wrapper
        .metadata()
        .map(|metadata| metadata.is_file() && metadata.len() > 0)
        .unwrap_or(false);
    let runtime_node = wrapper
        .parent()
        .map(|parent| parent.join("runtime.node"))
        .unwrap_or_else(|| PathBuf::from("runtime.node"));
    let runtime_valid = runtime_node
        .metadata()
        .map(|metadata| metadata.is_file() && metadata.len() > 0)
        .unwrap_or(false);
    if wrapper_valid && runtime_valid {
        #[cfg(unix)]
        {
            use std::os::unix::fs::PermissionsExt;

            let metadata = wrapper.metadata().map_err(|e| {
                Error::with_message(
                    ErrorKind::InvalidConfig,
                    format!(
                        "failed to inspect Copilot runtime wrapper permissions at '{}': {e}",
                        wrapper.display()
                    ),
                )
            })?;
            if metadata.permissions().mode() & 0o111 == 0 {
                let mut permissions = metadata.permissions();
                permissions.set_mode(permissions.mode() | 0o111);
                std::fs::set_permissions(wrapper, permissions).map_err(|e| {
                    Error::with_message(
                        ErrorKind::InvalidConfig,
                        format!(
                            "failed to make Copilot runtime wrapper executable at '{}': {e}",
                            wrapper.display()
                        ),
                    )
                })?;
            }
        }
        return Ok(());
    }
    let detail = format!(
        "COPILOT_RUNTIME_PATH must point to a non-empty wrapper with an adjacent non-empty runtime.node; checked '{}' and '{}'",
        wrapper.display(),
        runtime_node.display()
    );
    Err(Error::with_message(
        ErrorKind::BinaryNotFound {
            name: runtime_binary_name().into(),
            hint: Some(detail.clone()),
        },
        detail,
    ))
}

fn cli_binary_name() -> &'static str {
    if cfg!(windows) {
        "copilot.exe"
    } else {
        "copilot"
    }
}

fn runtime_binary_name() -> &'static str {
    if cfg!(windows) {
        "copilot-runtime.exe"
    } else {
        "copilot-runtime"
    }
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

#[cfg(test)]
mod tests {
    use std::fs;

    use tempfile::tempdir;

    use super::validate_runtime_pair;

    #[test]
    fn runtime_override_requires_adjacent_nonempty_runtime_node() {
        let dir = tempdir().expect("temp dir");
        let wrapper = dir.path().join(if cfg!(windows) {
            "copilot-runtime.exe"
        } else {
            "copilot-runtime"
        });
        fs::write(&wrapper, b"wrapper").expect("write wrapper");

        let error = validate_runtime_pair(&wrapper).expect_err("runtime.node is required");
        assert!(error.to_string().contains("runtime.node"));

        fs::write(dir.path().join("runtime.node"), b"runtime").expect("write runtime.node");
        validate_runtime_pair(&wrapper).expect("complete pair is valid");
    }
}
