//! Lazy runtime installer for the CLI binary that build.rs embedded in this
//! crate (gated on the `bundled-cli` cargo feature, which is in the default
//! feature set).
//!
//! build.rs downloads the platform's `copilot-{platform}.{tar.gz,zip}`
//! archive from GitHub Releases, SHA-256 verifies it against the version
//! pinned in `cli-version.txt` (or `../nodejs/package-lock.json` when
//! building inside the github/copilot-sdk repo itself), and embeds the
//! **raw archive bytes**
//! into the consumer's compiled artifact via `include_bytes!()`. Extraction
//! to a real on-disk path is deferred until the first call to
//! [`path`] / [`install_at`] — at which point the bytes are part of the
//! consumer's signed binary and trusted, so no further hashing is done.

#[cfg(has_bundled_cli)]
use std::fs;
#[cfg(all(has_bundled_cli, not(windows)))]
use std::io::Read;
#[cfg(has_bundled_cli)]
use std::io::Write;
use std::path::{Path, PathBuf};
use std::sync::OnceLock;

#[cfg(has_bundled_cli)]
use tracing::{info, warn};

// When the `bundled-cli` cargo feature is enabled and the target platform is
// supported, build.rs generates `bundled_cli.rs` exposing the raw archive
// bytes. The CLI version is exposed crate-wide via the
// `cargo:rustc-env=COPILOT_SDK_CLI_VERSION` emit (see `build.rs`), and the
// binary name is OS-derived — so no other generated constants are needed.
#[cfg(has_bundled_cli)]
mod build_time {
    include!(concat!(env!("OUT_DIR"), "/bundled_cli.rs"));
}

// Pinned at build time and consumed by both install paths (path/install_at).
// Sourced from the unconditional `COPILOT_SDK_CLI_VERSION` env emit in
// build.rs — the single source of truth for "what version did build.rs
// target", shared with the runtime resolver used when `bundled-cli` is off.
#[cfg(has_bundled_cli)]
const CLI_VERSION: &str = env!("COPILOT_SDK_CLI_VERSION");

// OS-derived; matches the release-archive entry name and the on-disk
// filename. No need to bake this — `cfg(windows)` reflects the target
// the runtime is running on, which by definition is the same target
// build.rs targeted.
#[cfg(all(has_bundled_cli, windows))]
const CLI_BINARY_NAME: &str = "copilot.exe";
#[cfg(all(has_bundled_cli, not(windows)))]
const CLI_BINARY_NAME: &str = "copilot";

#[cfg(feature = "bundled-cli")]
static INSTALLED_PATH: OnceLock<Option<PathBuf>> = OnceLock::new();

/// Returns the path to the installed CLI binary, lazily extracting the
/// embedded archive on first call.
///
/// On first call this extracts the embedded archive to
/// `<platform cache dir>/github-copilot-sdk/cli/<version>/copilot[.exe]`
/// and returns the resulting path. The cache dir comes from
/// [`dirs::cache_dir()`] — `%LOCALAPPDATA%` on Windows,
/// `~/Library/Caches/` on macOS, `$XDG_CACHE_HOME` (or `~/.cache/`) on
/// Linux. Subsequent calls return the cached result. The extraction
/// is skipped when the target file already exists — the per-version
/// install directory and the assumption that the consumer's binary is
/// trusted mean no further hashing is needed.
///
/// Returns `None` if no CLI was embedded at build time.
#[cfg(feature = "bundled-cli")]
pub(crate) fn path() -> Option<PathBuf> {
    INSTALLED_PATH
        .get_or_init(|| {
            #[cfg(has_bundled_cli)]
            {
                let dir = default_install_dir(CLI_VERSION);
                match install(&dir, build_time::CLI_ARCHIVE) {
                    Ok(path) => {
                        info!(path = %path.display(), version = CLI_VERSION, "embedded CLI installed");
                        return Some(path);
                    }
                    Err(e) => {
                        warn!(error = %e, "embedded CLI installation failed");
                    }
                }
            }
            None
        })
        .clone()
}

/// Install the embedded CLI binary into the given directory instead of the
/// default `<platform cache dir>/github-copilot-sdk/cli/<version>/` location
/// (see [`path`] for the per-platform mapping).
///
/// Idempotent: skips extraction if the target binary already exists.
/// Returns `None` when the SDK was built without a bundled CLI.
#[cfg(feature = "bundled-cli")]
#[allow(dead_code)] // Used by resolve.rs when ClientOptions::bundled_cli_extract_dir is set.
pub(crate) fn install_at(extract_dir: &Path) -> Option<PathBuf> {
    #[cfg(has_bundled_cli)]
    {
        match install(extract_dir, build_time::CLI_ARCHIVE) {
            Ok(path) => {
                info!(path = %path.display(), version = CLI_VERSION, "embedded CLI installed");
                return Some(path);
            }
            Err(e) => {
                warn!(error = %e, "embedded CLI installation failed");
            }
        }
    }
    #[cfg(not(has_bundled_cli))]
    {
        let _ = extract_dir;
    }
    None
}

#[cfg(has_bundled_cli)]
fn default_install_dir(version: &str) -> PathBuf {
    let cache = dirs::cache_dir().unwrap_or_else(std::env::temp_dir);
    let root = cache.join("github-copilot-sdk").join("cli");
    if version.is_empty() {
        root.join("unversioned")
    } else {
        root.join(sanitize_version(version))
    }
}

#[cfg(has_bundled_cli)]
fn install(install_dir: &Path, archive: &[u8]) -> Result<PathBuf, EmbeddedCliError> {
    let verbose = std::env::var("COPILOT_CLI_INSTALL_VERBOSE").ok().as_deref() == Some("1");

    fs::create_dir_all(install_dir).map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::CreateDir, e))?;

    let final_path = install_dir.join(CLI_BINARY_NAME);

    // Per-version install dir means a present file at this path is the
    // binary we want — no need to hash-verify the bytes are unchanged.
    if final_path.is_file() {
        if verbose {
            eprintln!("embedded CLI already installed at {}", final_path.display());
        }
        return Ok(final_path);
    }

    let start = std::time::Instant::now();
    let bytes = extract_binary(archive, CLI_BINARY_NAME)?;
    write_binary(&final_path, &bytes)?;

    if verbose {
        eprintln!(
            "embedded CLI extracted to {} in {:?}",
            final_path.display(),
            start.elapsed()
        );
    }

    Ok(final_path)
}

#[cfg(all(has_bundled_cli, not(windows)))]
fn extract_binary(archive: &[u8], binary_name: &str) -> Result<Vec<u8>, EmbeddedCliError> {
    let gz = flate2::read::GzDecoder::new(archive);
    let mut tar = tar::Archive::new(gz);
    for entry in tar.entries().map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Archive, e))? {
        let mut entry = entry.map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Archive, e))?;
        let path = entry.path().map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Archive, e))?;
        let name = path.to_string_lossy();
        if name == binary_name || name.ends_with(&format!("/{binary_name}")) {
            let mut bytes = Vec::with_capacity(entry.size() as usize);
            entry
                .read_to_end(&mut bytes)
                .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Archive, e))?;
            return Ok(bytes);
        }
    }
    Err(EmbeddedCliErrorKind::BinaryNotFoundInArchive.into())
}

#[cfg(all(has_bundled_cli, windows))]
fn extract_binary(archive: &[u8], binary_name: &str) -> Result<Vec<u8>, EmbeddedCliError> {
    let cursor = std::io::Cursor::new(archive);
    let mut zip = zip::ZipArchive::new(cursor).map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Zip, e))?;
    for i in 0..zip.len() {
        let mut entry = zip.by_index(i).map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Zip, e))?;
        let name = entry.name().to_string();
        if name == binary_name || name.ends_with(&format!("/{binary_name}")) {
            let mut bytes = Vec::with_capacity(entry.size() as usize);
            std::io::copy(&mut entry, &mut bytes).map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Io, e))?;
            return Ok(bytes);
        }
    }
    Err(EmbeddedCliErrorKind::BinaryNotFoundInArchive.into())
}

#[cfg(has_bundled_cli)]
fn sanitize_version(version: &str) -> String {
    version
        .chars()
        .map(|c| match c {
            'a'..='z' | 'A'..='Z' | '0'..='9' | '.' | '-' | '_' => c,
            _ => '_',
        })
        .collect()
}

#[cfg(has_bundled_cli)]
fn write_binary(path: &Path, data: &[u8]) -> Result<(), EmbeddedCliError> {
    let mut file = fs::OpenOptions::new()
        .write(true)
        .create(true)
        .truncate(true)
        .open(path)
        .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Io, e))?;

    file.write_all(data).map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Io, e))?;

    #[cfg(unix)]
    {
        use std::os::unix::fs::PermissionsExt;
        fs::set_permissions(path, fs::Permissions::from_mode(0o755))
            .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Io, e))?;
    }

    Ok(())
}

#[cfg(has_bundled_cli)]
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
#[allow(dead_code)]
enum EmbeddedCliErrorKind {
    CreateDir,
    #[cfg(not(windows))]
    Archive,
    #[cfg(windows)]
    Zip,
    BinaryNotFoundInArchive,
    Io,
}

#[cfg(has_bundled_cli)]
impl std::fmt::Display for EmbeddedCliErrorKind {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            EmbeddedCliErrorKind::CreateDir => f.write_str("failed to create install directory"),
            #[cfg(not(windows))]
            EmbeddedCliErrorKind::Archive => f.write_str("failed to read archive entry"),
            #[cfg(windows)]
            EmbeddedCliErrorKind::Zip => f.write_str("failed to read zip archive"),
            EmbeddedCliErrorKind::BinaryNotFoundInArchive => {
                f.write_str("CLI binary not found in embedded archive")
            }
            EmbeddedCliErrorKind::Io => f.write_str("I/O error"),
        }
    }
}

#[cfg(has_bundled_cli)]
#[allow(dead_code)]
struct EmbeddedCliError {
    repr: crate::errors::Repr<EmbeddedCliErrorKind>,
}

#[cfg(has_bundled_cli)]
impl EmbeddedCliError {
    fn new<E>(kind: EmbeddedCliErrorKind, error: E) -> Self
    where
        E: Into<Box<dyn std::error::Error + Send + Sync>>,
    {
        Self {
            repr: crate::errors::Repr::Custom(crate::errors::Custom {
                kind,
                error: error.into(),
            }),
        }
    }

}

#[cfg(has_bundled_cli)]
impl From<EmbeddedCliErrorKind> for EmbeddedCliError {
    fn from(kind: EmbeddedCliErrorKind) -> Self {
        Self {
            repr: crate::errors::Repr::Simple(kind),
        }
    }
}

#[cfg(has_bundled_cli)]
impl std::fmt::Display for EmbeddedCliError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match &self.repr {
            crate::errors::Repr::Simple(kind) => write!(f, "{kind}"),
            crate::errors::Repr::SimpleMessage(_, msg) => write!(f, "{msg}"),
            crate::errors::Repr::Custom(crate::errors::Custom { kind, error }) => {
                write!(f, "{kind}: {error}")
            }
        }
    }
}

#[cfg(has_bundled_cli)]
impl std::fmt::Debug for EmbeddedCliError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "EmbeddedCliError({self})")
    }
}

#[cfg(has_bundled_cli)]
impl std::error::Error for EmbeddedCliError {
    fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
        match &self.repr {
            crate::errors::Repr::Custom(crate::errors::Custom { error, .. }) => Some(&**error),
            _ => None,
        }
    }
}
