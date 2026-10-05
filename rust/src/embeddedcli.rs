// Copyright (c) Microsoft Corporation. All rights reserved.

//! Lazy runtime installer for the CLI binary that build.rs embedded in this
//! crate (gated on the `bundled-cli` cargo feature, which is in the default
//! feature set).
//!
//! Builds embed two platform release payloads from GitHub Releases: the full
//! CLI archive and a filtered runtime archive containing the wrapper,
//! `runtime.node`, auxiliary runtime assets, and optionally the in-process
//! runtime library. Extraction to a real on-disk path is deferred until the
//! relevant installer is called.
//!
//! The embedded bytes are part of the consumer's signed binary and therefore
//! trusted *as the source of truth* — but the bytes that land on disk are not.
//! A non-atomic write, a multi-process race, or antivirus quarantining the
//! freshly-written executable can leave a truncated or corrupt image that, if
//! handed back as "good", fails to launch (e.g. Windows `ERROR_BAD_EXE_FORMAT`).
//! Full CLI installation therefore: extracts to a unique temp file in the target dir,
//! fsyncs and marks it executable, verifies the staged bytes against the
//! trusted in-memory image, atomically renames it into place, re-verifies the
//! published file, and records an integrity marker. Subsequent runs trust an
//! existing install only after a cheap re-check (size marker + executable-image
//! header); anything that looks truncated or quarantined is re-extracted, and
//! the whole publish is retried before surfacing a clear, actionable error.
//!
//! Runtime assets are compared and extracted with bounded buffers rather than
//! whole-file allocations. Matching files need only read access and retain
//! their installed permissions, provided the wrapper remains executable by
//! the current process. Changed files are staged beside their targets and
//! published after archive validation, including the gzip trailer.
//! Installation is atomic per file, not across the entire runtime bundle.

// The atomic-publish + verify helpers (and their unit tests) are pure
// std-only logic that doesn't touch the embedded archive, so they compile
// whenever the binary is bundled *or* we're building the test harness —
// the standard `cargo test --no-default-features` job has `has_bundled_cli`
// off but still needs to exercise them.
#[cfg(has_bundled_cli)]
use std::collections::HashSet;
#[cfg(any(has_bundled_cli, test))]
use std::fs;
#[cfg(has_bundled_cli)]
use std::io::Read;
#[cfg(any(has_bundled_cli, test))]
use std::io::Write;
use std::path::{Path, PathBuf};
use std::sync::OnceLock;
#[cfg(any(has_bundled_cli, test))]
use std::sync::atomic::{AtomicU64, Ordering};

#[cfg(has_bundled_cli)]
use tracing::{info, warn};

// When the `bundled-cli` cargo feature is enabled and the target platform is
// supported, build.rs generates `bundled_cli.rs` exposing both selected archives.
// The CLI version is exposed crate-wide via the
// `cargo:rustc-env=COPILOT_SDK_CLI_VERSION` emit (see `build.rs`), and the
// release-scoped cache identity is emitted separately to avoid destination
// collisions. The binary name is OS-derived.
#[cfg(has_bundled_cli)]
mod build_time {
    include!(concat!(env!("OUT_DIR"), "/bundled_cli.rs"));
}

// Keep the actual version for diagnostics and a release-scoped identity for
// cache paths. Legacy releases use the version unchanged for both.
#[cfg(has_bundled_cli)]
const CLI_VERSION: &str = env!("COPILOT_SDK_CLI_VERSION");
#[cfg(has_bundled_cli)]
const CLI_CACHE_ID: &str = env!("COPILOT_SDK_CLI_CACHE_ID");

// OS-derived; matches the release-archive entry name and the on-disk
// filename. No need to bake this — `cfg(windows)` reflects the target
// the runtime is running on, which by definition is the same target
// build.rs targeted.
#[cfg(all(has_bundled_cli, windows))]
const CLI_BINARY_NAME: &str = "copilot.exe";
#[cfg(all(has_bundled_cli, not(windows)))]
const CLI_BINARY_NAME: &str = "copilot";
#[cfg(all(has_bundled_cli, windows))]
const RUNTIME_BINARY_NAME: &str = "copilot-runtime.exe";
#[cfg(all(has_bundled_cli, not(windows)))]
const RUNTIME_BINARY_NAME: &str = "copilot-runtime";
#[cfg(has_bundled_cli)]
const RUNTIME_NODE_NAME: &str = "runtime.node";
#[cfg(has_bundled_cli)]
const RUNTIME_VERSION_MARKER: &str = ".copilot-runtime-version";

#[cfg(feature = "bundled-cli")]
static INSTALLED_PATH: OnceLock<Option<PathBuf>> = OnceLock::new();
#[cfg(feature = "bundled-cli")]
static INSTALLED_RUNTIME_PATH: OnceLock<Option<PathBuf>> = OnceLock::new();

/// Returns the path to the installed CLI binary, lazily extracting the
/// embedded archive on first call.
///
/// On first call this extracts the embedded archive to
/// `<platform cache dir>/github-copilot-sdk/cli/<version>/copilot[.exe]`
/// and returns the resulting path. The cache dir comes from
/// [`dirs::cache_dir()`] — `%LOCALAPPDATA%` on Windows,
/// `~/Library/Caches/` on macOS, `$XDG_CACHE_HOME` (or `~/.cache/`) on
/// Linux. Subsequent calls return the cached result. Extraction
/// is skipped when a previously-published binary is still present and
/// passes a cheap integrity re-check (size marker + executable-image
/// header); a truncated, empty, or quarantined binary is re-extracted
/// rather than returned.
///
/// Returns `None` if no CLI was embedded at build time.
#[cfg(feature = "bundled-cli")]
pub(crate) fn path() -> Option<PathBuf> {
    INSTALLED_PATH
        .get_or_init(|| {
            #[cfg(has_bundled_cli)]
            {
                let dir = default_install_dir(CLI_CACHE_ID);
                match install_cli(
                    &dir,
                    build_time::CLI_ARCHIVE,
                    build_time::CLI_BINARY_SIZE,
                ) {
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
/// Idempotent: skips extraction when an already-published binary passes the
/// integrity re-check (size marker + executable-image header), and
/// re-extracts a corrupt or quarantined one.
/// Returns `None` when the SDK was built without a bundled CLI.
#[cfg(feature = "bundled-cli")]
#[allow(dead_code)] // Used by resolve.rs when ClientOptions::bundled_cli_extract_dir is set.
pub(crate) fn install_at(extract_dir: &Path) -> Option<PathBuf> {
    #[cfg(has_bundled_cli)]
    {
        match install_cli(
            extract_dir,
            build_time::CLI_ARCHIVE,
            build_time::CLI_BINARY_SIZE,
        ) {
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

/// Returns the path to the bundled runtime wrapper, extracting the wrapper and
/// adjacent `runtime.node` on first call.
#[cfg(feature = "bundled-cli")]
pub(crate) fn runtime_path() -> Option<PathBuf> {
    INSTALLED_RUNTIME_PATH
        .get_or_init(|| {
            #[cfg(has_bundled_cli)]
            {
                let dir = default_install_dir(CLI_CACHE_ID);
                match install_runtime(&dir, build_time::RUNTIME_ARCHIVE) {
                    Ok(path) => {
                        info!(path = %path.display(), version = CLI_VERSION, "embedded runtime installed");
                        return Some(path);
                    }
                    Err(e) => {
                        warn!(error = %e, "embedded runtime installation failed");
                    }
                }
            }
            None
        })
        .clone()
}

/// Installs the bundled runtime wrapper and adjacent `runtime.node` into a
/// caller-specified directory.
#[cfg(feature = "bundled-cli")]
pub(crate) fn install_runtime_at(extract_dir: &Path) -> Option<PathBuf> {
    #[cfg(has_bundled_cli)]
    {
        let install_dir = match runtime_install_dir(extract_dir, CLI_CACHE_ID) {
            Ok(dir) => dir,
            Err(e) => {
                warn!(error = %e, "embedded runtime install directory selection failed");
                return None;
            }
        };
        match install_runtime(&install_dir, build_time::RUNTIME_ARCHIVE) {
            Ok(path) => {
                info!(path = %path.display(), version = CLI_VERSION, "embedded runtime installed");
                return Some(path);
            }
            Err(e) => {
                warn!(error = %e, "embedded runtime installation failed");
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
fn runtime_install_dir(base_dir: &Path, version: &str) -> Result<PathBuf, EmbeddedCliError> {
    fs::create_dir_all(base_dir)
        .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::CreateDir, e))?;
    let marker = base_dir.join(RUNTIME_VERSION_MARKER);
    match fs::OpenOptions::new()
        .write(true)
        .create_new(true)
        .open(&marker)
    {
        Ok(mut file) => {
            if let Err(error) = file
                .write_all(version.as_bytes())
                .and_then(|()| file.sync_all())
            {
                drop(file);
                let _ = fs::remove_file(&marker);
                return Err(EmbeddedCliError::new(EmbeddedCliErrorKind::Io, error));
            }
            Ok(base_dir.to_path_buf())
        }
        Err(error) if error.kind() == std::io::ErrorKind::AlreadyExists => {
            let installed_version = fs::read_to_string(marker).unwrap_or_default();
            if installed_version == version {
                Ok(base_dir.to_path_buf())
            } else {
                Ok(base_dir.join(version))
            }
        }
        Err(error) => Err(EmbeddedCliError::new(EmbeddedCliErrorKind::Io, error)),
    }
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

/// Number of times we re-extract + re-publish the binary before giving up.
/// A single transient failure (e.g. antivirus briefly locking or quarantining
/// the freshly-written file) is retried; a persistent one surfaces a clear
/// error rather than handing back a broken path.
#[cfg(has_bundled_cli)]
const MAX_PUBLISH_ATTEMPTS: u32 = 3;

// Natural platform shared-library name for the in-process FFI runtime.
#[cfg(all(has_bundled_cli, feature = "in-process", windows))]
const RUNTIME_LIBRARY_NAME: &str = "copilot_runtime.dll";
#[cfg(all(has_bundled_cli, feature = "in-process", target_os = "macos"))]
const RUNTIME_LIBRARY_NAME: &str = "libcopilot_runtime.dylib";
#[cfg(all(
    has_bundled_cli,
    feature = "in-process",
    not(windows),
    not(target_os = "macos")
))]
const RUNTIME_LIBRARY_NAME: &str = "libcopilot_runtime.so";

#[cfg(has_bundled_cli)]
fn install_runtime(install_dir: &Path, archive: &[u8]) -> Result<PathBuf, EmbeddedCliError> {
    fs::create_dir_all(install_dir)
        .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::CreateDir, e))?;
    let root = fs::canonicalize(install_dir)
        .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Io, e))?;
    let mut required = vec![RUNTIME_BINARY_NAME, RUNTIME_NODE_NAME];
    #[cfg(feature = "in-process")]
    required.push(RUNTIME_LIBRARY_NAME);
    let mut seen = HashSet::new();
    let mut changed = HashSet::new();
    let mut pending = Vec::new();
    let mut tar = tar::Archive::new(flate2::read::GzDecoder::new(archive));
    for entry in tar
        .entries()
        .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Archive, e))?
    {
        let mut entry =
            entry.map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Archive, e))?;
        if !entry.header().entry_type().is_file() {
            continue;
        }
        let path = entry
            .path()
            .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Archive, e))?;
        let path = runtime_asset_path(&path)?;
        if !seen.insert(path.as_os_str().to_ascii_lowercase()) {
            return Err(EmbeddedCliError::with_message(
                EmbeddedCliErrorKind::Archive,
                format!("duplicate embedded runtime asset path: {}", path.display()),
            ));
        }
        if !selected_runtime_asset(&path) {
            continue;
        }
        if let Some(index) = required.iter().position(|name| path == Path::new(name)) {
            if entry.size() == 0 {
                return Err(EmbeddedCliError::with_message(
                    EmbeddedCliErrorKind::Verification,
                    format!("embedded runtime artifact is empty: {}", path.display()),
                ));
            }
            required.remove(index);
        }
        let target = root.join(&path);
        let parent = target.parent().ok_or_else(|| {
            EmbeddedCliError::with_message(
                EmbeddedCliErrorKind::Archive,
                format!("embedded runtime asset has no parent: {}", path.display()),
            )
        })?;
        check_runtime_asset_parent(&root, parent, false)?;
        match existing_runtime_file(&target, entry.size())? {
            Some(mut installed) => {
                let matches = runtime_entry_matches(&mut entry, &mut installed)?;
                #[cfg(unix)]
                let matches = matches
                    && (path != Path::new(RUNTIME_BINARY_NAME)
                        || check_runtime_wrapper_execute_access(&target).is_ok());
                if !matches {
                    changed.insert(path);
                }
            }
            None => {
                check_runtime_asset_parent(&root, parent, true)?;
                pending.push(stage_runtime_entry(&mut entry, &target)?);
            }
        }
    }
    // TAR's end marker can precede gzip's CRC and size trailer.
    std::io::copy(&mut tar.into_inner(), &mut std::io::sink())
        .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Archive, e))?;
    if !required.is_empty() {
        return Err(EmbeddedCliErrorKind::BinaryNotFoundInArchive.into());
    }
    // Recover bytes consumed by failed comparisons without retaining their
    // prefixes in memory. Cold installs and valid warm caches need one pass.
    if !changed.is_empty() {
        let mut tar = tar::Archive::new(flate2::read::GzDecoder::new(archive));
        for entry in tar
            .entries()
            .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Archive, e))?
        {
            let mut entry =
                entry.map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Archive, e))?;
            if !entry.header().entry_type().is_file() {
                continue;
            }
            let path = entry
                .path()
                .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Archive, e))?;
            let path = runtime_asset_path(&path)?;
            if changed.contains(&path) {
                pending.push(stage_runtime_entry(&mut entry, &root.join(path))?);
            }
        }
        std::io::copy(&mut tar.into_inner(), &mut std::io::sink())
            .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Archive, e))?;
    }
    for staged in pending {
        publish(&staged.temporary, &staged.target)?;
    }
    #[cfg(unix)]
    check_runtime_wrapper_execute_access(&root.join(RUNTIME_BINARY_NAME))
        .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Verification, e))?;
    Ok(install_dir.join(RUNTIME_BINARY_NAME))
}

#[cfg(all(has_bundled_cli, unix))]
fn check_runtime_wrapper_execute_access(path: &Path) -> std::io::Result<()> {
    use std::ffi::{CString, c_char, c_int};
    use std::os::unix::ffi::OsStrExt;

    // std has no effective-credentials access check. Bind the POSIX libc API,
    // not a raw syscall: libc handles Linux kernel differences (glibc/musl).
    // These <fcntl.h>/<unistd.h> constants are shared by each OS's supported
    // x86_64 and aarch64 targets; macOS uses different AT_* values than Linux.
    #[cfg(target_os = "linux")]
    const AT_FDCWD: c_int = -100;
    #[cfg(target_os = "macos")]
    const AT_FDCWD: c_int = -2;
    #[cfg(target_os = "linux")]
    const AT_EACCESS: c_int = 0x200;
    #[cfg(target_os = "macos")]
    const AT_EACCESS: c_int = 0x10;
    const X_OK: c_int = 1;
    unsafe extern "C" {
        fn faccessat(dirfd: c_int, path: *const c_char, mode: c_int, flags: c_int) -> c_int;
    }

    let path = CString::new(path.as_os_str().as_bytes())
        .map_err(|e| std::io::Error::new(std::io::ErrorKind::InvalidInput, e))?;
    // SAFETY: path is NUL-terminated and lives through the call. faccessat
    // only reads it and uses the platform's C integer ABI and flag values.
    if unsafe { faccessat(AT_FDCWD, path.as_ptr(), X_OK, AT_EACCESS) } == 0 {
        Ok(())
    } else {
        Err(std::io::Error::last_os_error())
    }
}

#[cfg(has_bundled_cli)]
fn selected_runtime_asset(path: &Path) -> bool {
    if path == Path::new(CLI_BINARY_NAME) {
        return false;
    }
    if matches!(
        path.file_name().and_then(|name| name.to_str()),
        Some("copilot_runtime.dll" | "libcopilot_runtime.dylib" | "libcopilot_runtime.so")
    ) {
        #[cfg(feature = "in-process")]
        return path == Path::new(RUNTIME_LIBRARY_NAME);
        #[cfg(not(feature = "in-process"))]
        return false;
    }
    true
}

#[cfg(has_bundled_cli)]
fn runtime_asset_path(path: &Path) -> Result<PathBuf, EmbeddedCliError> {
    let invalid = || {
        EmbeddedCliError::with_message(
            EmbeddedCliErrorKind::Archive,
            format!(
                "non-portable embedded runtime asset path: {}",
                path.display()
            ),
        )
    };
    // Bundled release assets use ASCII names. Apply the same conservative
    // naming rules on every filesystem, without probing a read-only cache.
    // Reject Unicode, DOS device/short names and trailing-dot/space aliases
    // rather than approximating platform-specific Unicode normalization.
    let name = path
        .to_str()
        .filter(|name| name.is_ascii())
        .ok_or_else(invalid)?;
    if name.starts_with(['/', '\\']) {
        return Err(invalid());
    }
    let mut normalized = PathBuf::new();
    for component in name.split(['/', '\\']) {
        if component.is_empty() || component == "." {
            continue;
        }
        if component == ".."
            || component.ends_with(['.', ' '])
            || component
                .bytes()
                .any(|byte| byte.is_ascii_control() || b"<>:\"|?*~".contains(&byte))
        {
            return Err(invalid());
        }
        let stem = component
            .split('.')
            .next()
            .expect("nonempty component")
            .trim_end_matches(' ')
            .to_ascii_uppercase();
        if matches!(stem.as_str(), "CON" | "PRN" | "AUX" | "NUL")
            || (stem.len() == 4
                && (stem.starts_with("COM") || stem.starts_with("LPT"))
                && matches!(stem.as_bytes()[3], b'1'..=b'9'))
        {
            return Err(invalid());
        }
        normalized.push(component);
    }
    if normalized.as_os_str().is_empty() {
        return Err(invalid());
    }
    Ok(normalized)
}

#[cfg(has_bundled_cli)]
fn check_runtime_asset_parent(
    root: &Path,
    parent: &Path,
    create: bool,
) -> Result<(), EmbeddedCliError> {
    let mut current = root.to_path_buf();
    for component in parent
        .strip_prefix(root)
        .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Archive, e))?
        .components()
    {
        current.push(component);
        if create {
            match fs::create_dir(&current) {
                Ok(()) => {}
                Err(e) if e.kind() == std::io::ErrorKind::AlreadyExists => {}
                Err(e) => return Err(EmbeddedCliError::new(EmbeddedCliErrorKind::CreateDir, e)),
            }
        }
        let metadata = match fs::symlink_metadata(&current) {
            Ok(metadata) => metadata,
            Err(e) if !create && e.kind() == std::io::ErrorKind::NotFound => return Ok(()),
            Err(e) => return Err(EmbeddedCliError::new(EmbeddedCliErrorKind::Io, e)),
        };
        if !metadata.is_dir() {
            return Err(EmbeddedCliError::with_message(
                EmbeddedCliErrorKind::Verification,
                format!(
                    "runtime asset parent is not a directory: {}",
                    current.display()
                ),
            ));
        }
    }
    Ok(())
}

#[cfg(has_bundled_cli)]
struct StagedRuntimeFile {
    temporary: PathBuf,
    target: PathBuf,
}

#[cfg(has_bundled_cli)]
impl Drop for StagedRuntimeFile {
    fn drop(&mut self) {
        if let Err(error) = fs::remove_file(&self.temporary)
            && error.kind() != std::io::ErrorKind::NotFound
        {
            warn!(path = %self.temporary.display(), %error, "failed to remove staged runtime asset");
        }
    }
}

#[cfg(has_bundled_cli)]
fn existing_runtime_file(target: &Path, size: u64) -> Result<Option<fs::File>, EmbeddedCliError> {
    let metadata = match fs::symlink_metadata(target) {
        Ok(metadata) if metadata.is_file() => Some(metadata),
        Ok(_) => {
            return Err(EmbeddedCliError::with_message(
                EmbeddedCliErrorKind::Verification,
                format!("runtime asset is not a regular file: {}", target.display()),
            ));
        }
        Err(e) if e.kind() == std::io::ErrorKind::NotFound => None,
        Err(e) => return Err(EmbeddedCliError::new(EmbeddedCliErrorKind::Io, e)),
    };
    let matches = metadata
        .as_ref()
        .is_some_and(|metadata| metadata.len() == size);
    if matches {
        match fs::File::open(target) {
            Ok(file) => return Ok(Some(file)),
            Err(e) => {
                tracing::debug!(path = %target.display(), error = %e,
                    "existing runtime asset cannot be read; repairing");
            }
        }
    }
    Ok(None)
}

#[cfg(has_bundled_cli)]
fn runtime_entry_matches<R: Read>(
    entry: &mut tar::Entry<'_, R>,
    installed: &mut fs::File,
) -> Result<bool, EmbeddedCliError> {
    let mut buffer = [0u8; 64 * 1024];
    let mut on_disk = [0u8; 64 * 1024];
    let mut remaining = entry.size();
    while remaining > 0 {
        let length = remaining.min(buffer.len() as u64) as usize;
        entry
            .read_exact(&mut buffer[..length])
            .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Archive, e))?;
        remaining -= length as u64;
        if let Err(error) = installed.read_exact(&mut on_disk[..length]) {
            tracing::debug!(%error, "existing runtime asset cannot be read; repairing");
            return Ok(false);
        }
        if on_disk[..length] != buffer[..length] {
            return Ok(false);
        }
    }
    match installed.read(&mut on_disk[..1]) {
        Ok(read) => Ok(read == 0),
        Err(error) => {
            tracing::debug!(%error, "existing runtime asset cannot be read; repairing");
            Ok(false)
        }
    }
}

#[cfg(has_bundled_cli)]
fn stage_runtime_entry<R: Read>(
    entry: &mut tar::Entry<'_, R>,
    target: &Path,
) -> Result<StagedRuntimeFile, EmbeddedCliError> {
    let parent = target.parent().expect("runtime asset has a checked parent");
    let (temporary, mut file) = create_temp_file(parent)?;
    let staged = StagedRuntimeFile {
        temporary,
        target: target.to_path_buf(),
    };
    let result = write_runtime_entry(entry, &mut file);
    // Close handles before cleanup or replacement, including on Windows.
    drop(file);
    result?;
    Ok(staged)
}

#[cfg(has_bundled_cli)]
fn write_runtime_entry<R: Read>(
    entry: &mut tar::Entry<'_, R>,
    file: &mut fs::File,
) -> Result<(), EmbeddedCliError> {
    let size = entry.size();
    let written = std::io::copy(&mut (&mut *entry).take(size), &mut *file)
        .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Io, e))?;
    if written != size {
        return Err(EmbeddedCliError::with_message(
            EmbeddedCliErrorKind::Verification,
            format!("runtime entry size mismatch: read {written} bytes, expected {size}"),
        ));
    }
    #[cfg(unix)]
    {
        use std::os::unix::fs::PermissionsExt;
        let mode = entry
            .header()
            .mode()
            .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Archive, e))?
            & 0o777;
        file.set_permissions(fs::Permissions::from_mode(mode))
            .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Io, e))?;
    }
    file.sync_all()
        .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Io, e))
}

#[cfg(has_bundled_cli)]
fn install_cli(
    install_dir: &Path,
    archive: &[u8],
    expected_binary_size: u64,
) -> Result<PathBuf, EmbeddedCliError> {
    let verbose = std::env::var("COPILOT_CLI_INSTALL_VERBOSE").ok().as_deref() == Some("1");

    fs::create_dir_all(install_dir)
        .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::CreateDir, e))?;

    let final_path = install_dir.join(CLI_BINARY_NAME);
    let marker_path = marker_path(install_dir);

    // Fast path: a previous install left both the binary and the integrity
    // marker we wrote *after* verifying it. Re-validate cheaply (size +
    // executable-image magic) so a binary that was later truncated or
    // quarantined by antivirus is re-extracted instead of trusted blindly.
    if existing_install_is_valid(&final_path, &marker_path, expected_binary_size) {
        if verbose {
            eprintln!("embedded CLI already installed at {}", final_path.display());
        }
        return Ok(final_path);
    }

    // The bytes extracted from the embedded archive are part of the
    // consumer's trusted, signed binary — so they are the known-good
    // reference we verify the on-disk file against after publishing.
    let start = std::time::Instant::now();
    let bytes = extract_cli_binary(archive)?;
    if bytes.is_empty() {
        return Err(EmbeddedCliError::with_message(
            EmbeddedCliErrorKind::Verification,
            "extracted CLI binary is empty",
        ));
    }

    let mut last_err: Option<EmbeddedCliError> = None;
    for attempt in 1..=MAX_PUBLISH_ATTEMPTS {
        match publish_verified(install_dir, &final_path, &marker_path, &bytes) {
            Ok(()) => {
                if verbose {
                    eprintln!(
                        "embedded CLI extracted to {} in {:?}",
                        final_path.display(),
                        start.elapsed()
                    );
                }
                return Ok(final_path);
            }
            Err(e) => {
                // Another process may have raced us and published the same
                // good binary; if what's on disk matches our trusted bytes,
                // accept its install rather than fighting over it.
                if verify_on_disk_matches(&final_path, &bytes).is_ok() {
                    let _ = write_marker(&marker_path, bytes.len() as u64);
                    return Ok(final_path);
                }
                warn!(attempt, error = %e, "embedded CLI publish attempt failed; retrying");
                last_err = Some(e);
            }
        }
    }

    Err(EmbeddedCliError::with_source(
        EmbeddedCliErrorKind::Blocked,
        last_err,
    ))
}

/// Path of the integrity marker written next to the installed binary. Its
/// presence (and recorded size) is proof a previous run published a verified
/// binary, letting the fast path skip re-extraction without trusting a bare
/// `is_file()` check.
#[cfg(any(has_bundled_cli, test))]
fn marker_path(install_dir: &Path) -> PathBuf {
    install_dir.join(".copilot-cli.ok")
}

/// Cheap, allocation-light validity check for an already-installed binary:
/// the file exists and is non-empty, an integrity marker recording its
/// expected size is present and matches, and the first bytes look like a
/// valid executable image for this platform. Catches the realistic failure
/// modes (zero-length / truncated / quarantined-to-garbage) without re-reading
/// the whole file.
#[cfg(any(has_bundled_cli, test))]
fn existing_install_is_valid(
    final_path: &Path,
    marker_path: &Path,
    expected_binary_size: u64,
) -> bool {
    let Ok(meta) = fs::metadata(final_path) else {
        return false;
    };
    if !meta.is_file() || meta.len() == 0 {
        return false;
    }
    match read_marker_len(marker_path) {
        Some(expected) if expected == expected_binary_size && expected == meta.len() => {
            looks_like_valid_image(final_path)
        }
        _ => false,
    }
}

/// Extract → stage in a unique temp file in the *same* directory → verify the
/// staged bytes → atomically rename into place → re-verify the published file
/// → write the integrity marker. Every step that can leave a partial file
/// cleans up after itself, so a failure never leaves a half-written binary at
/// the final path.
#[cfg(any(has_bundled_cli, test))]
fn publish_verified(
    install_dir: &Path,
    final_path: &Path,
    marker_path: &Path,
    bytes: &[u8],
) -> Result<(), EmbeddedCliError> {
    let tmp = write_temp_file(install_dir, bytes)?;

    // Verify the staged copy before it ever becomes the live binary, so a
    // short write or in-flight antivirus tampering is caught here.
    if let Err(e) = verify_on_disk_matches(&tmp, bytes) {
        let _ = fs::remove_file(&tmp);
        return Err(e);
    }

    if let Err(e) = publish(&tmp, final_path) {
        let _ = fs::remove_file(&tmp);
        return Err(e);
    }

    // Re-verify after the rename: catches the window where antivirus
    // quarantines or rewrites the file between staging and publishing.
    verify_on_disk_matches(final_path, bytes)?;

    write_marker(marker_path, bytes.len() as u64)?;
    Ok(())
}

/// Write `contents` to a uniquely-named temp file in `dir` (same filesystem as
/// the final path so the later rename is atomic), flushing and fsync-ing the
/// bytes to disk and marking it executable on unix before returning its path.
#[cfg(any(has_bundled_cli, test))]
fn write_temp_file(dir: &Path, contents: &[u8]) -> Result<PathBuf, EmbeddedCliError> {
    let (tmp, mut file) = create_temp_file(dir)?;

    if let Err(e) = file
        .write_all(contents)
        .and_then(|()| file.flush())
        .and_then(|()| file.sync_all())
    {
        drop(file);
        let _ = fs::remove_file(&tmp);
        return Err(EmbeddedCliError::new(EmbeddedCliErrorKind::Io, e));
    }

    #[cfg(unix)]
    {
        use std::os::unix::fs::PermissionsExt;
        if let Err(e) = fs::set_permissions(&tmp, fs::Permissions::from_mode(0o755)) {
            drop(file);
            let _ = fs::remove_file(&tmp);
            return Err(EmbeddedCliError::new(EmbeddedCliErrorKind::Io, e));
        }
    }

    drop(file);
    Ok(tmp)
}

#[cfg(any(has_bundled_cli, test))]
fn create_temp_file(dir: &Path) -> Result<(PathBuf, fs::File), EmbeddedCliError> {
    static COUNTER: AtomicU64 = AtomicU64::new(0);
    let nanos = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .map(|d| d.as_nanos())
        .unwrap_or(0);
    let tmp = dir.join(format!(
        ".copilot-cli.tmp.{}.{}.{}",
        std::process::id(),
        COUNTER.fetch_add(1, Ordering::Relaxed),
        nanos
    ));
    let file = fs::OpenOptions::new()
        .write(true)
        .create_new(true)
        .open(&tmp)
        .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Io, e))?;
    Ok((tmp, file))
}

/// Atomically move the staged temp file onto `final_path`.
///
/// Rust uses rename on POSIX and MoveFileExW with MOVEFILE_REPLACE_EXISTING on
/// Windows. Never unlink the destination on failure: readers must retain the
/// previous complete file if replacement is blocked.
#[cfg(any(has_bundled_cli, test))]
fn publish(tmp: &Path, final_path: &Path) -> Result<(), EmbeddedCliError> {
    fs::rename(tmp, final_path).map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Publish, e))
}

/// Read the file at `path` and confirm it byte-for-byte matches the trusted
/// `expected` image. Size is checked first so the common corruption case
/// (truncation) produces a precise error.
#[cfg(any(has_bundled_cli, test))]
fn verify_on_disk_matches(path: &Path, expected: &[u8]) -> Result<(), EmbeddedCliError> {
    let actual = fs::read(path).map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Io, e))?;
    if actual.len() != expected.len() {
        return Err(EmbeddedCliError::with_message(
            EmbeddedCliErrorKind::Verification,
            format!(
                "size mismatch: on-disk {} bytes, expected {} bytes",
                actual.len(),
                expected.len()
            ),
        ));
    }
    if actual != expected {
        return Err(EmbeddedCliError::with_message(
            EmbeddedCliErrorKind::Verification,
            "on-disk binary differs from the embedded image",
        ));
    }
    Ok(())
}

/// Best-effort check that the first bytes of `path` are a valid executable
/// image header for the current platform (PE on Windows, Mach-O on macOS,
/// ELF elsewhere). Returns `false` on any I/O error or unrecognized header.
#[cfg(any(has_bundled_cli, test))]
fn looks_like_valid_image(path: &Path) -> bool {
    use std::io::Read as _;
    let mut buf = [0u8; 4];
    let Ok(mut file) = fs::File::open(path) else {
        return false;
    };
    let Ok(read) = file.read(&mut buf) else {
        return false;
    };
    let head = &buf[..read];

    #[cfg(windows)]
    {
        head.starts_with(b"MZ")
    }
    #[cfg(target_os = "macos")]
    {
        matches!(
            head,
            [0xfe, 0xed, 0xfa, 0xce] // Mach-O 32-bit
                | [0xfe, 0xed, 0xfa, 0xcf] // Mach-O 64-bit
                | [0xce, 0xfa, 0xed, 0xfe] // byte-swapped 32-bit
                | [0xcf, 0xfa, 0xed, 0xfe] // byte-swapped 64-bit
                | [0xca, 0xfe, 0xba, 0xbe] // universal (fat)
                | [0xbe, 0xba, 0xfe, 0xca] // byte-swapped universal
        )
    }
    #[cfg(all(not(windows), not(target_os = "macos")))]
    {
        head.starts_with(b"\x7fELF")
    }
}

/// Write the integrity marker recording the published binary's size. Best
/// effort: a torn write just means the next run can't parse it and re-extracts.
#[cfg(any(has_bundled_cli, test))]
fn write_marker(marker_path: &Path, size: u64) -> Result<(), EmbeddedCliError> {
    fs::write(marker_path, size.to_string())
        .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Io, e))
}

/// Parse the size recorded in the integrity marker, or `None` if it's missing
/// or unparsable.
#[cfg(any(has_bundled_cli, test))]
fn read_marker_len(marker_path: &Path) -> Option<u64> {
    fs::read_to_string(marker_path)
        .ok()?
        .trim()
        .parse::<u64>()
        .ok()
}

#[cfg(all(has_bundled_cli, not(windows)))]
fn extract_cli_binary(archive: &[u8]) -> Result<Vec<u8>, EmbeddedCliError> {
    extract_binary(archive, CLI_BINARY_NAME)
}

#[cfg(all(has_bundled_cli, windows))]
fn extract_cli_binary(archive: &[u8]) -> Result<Vec<u8>, EmbeddedCliError> {
    let reader = std::io::Cursor::new(archive);
    let mut zip = zip::ZipArchive::new(reader)
        .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Archive, e))?;
    for index in 0..zip.len() {
        let mut entry = zip
            .by_index(index)
            .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Archive, e))?;
        if entry.name() == CLI_BINARY_NAME || entry.name().ends_with(&format!("/{CLI_BINARY_NAME}"))
        {
            let mut bytes = Vec::with_capacity(entry.size() as usize);
            entry
                .read_to_end(&mut bytes)
                .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Archive, e))?;
            return Ok(bytes);
        }
    }
    Err(EmbeddedCliErrorKind::BinaryNotFoundInArchive.into())
}

#[cfg(all(has_bundled_cli, any(not(windows), test)))]
fn extract_binary(archive: &[u8], binary_name: &str) -> Result<Vec<u8>, EmbeddedCliError> {
    let gz = flate2::read::GzDecoder::new(archive);
    let mut tar = tar::Archive::new(gz);
    for entry in tar
        .entries()
        .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Archive, e))?
    {
        let mut entry =
            entry.map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Archive, e))?;
        let path = entry
            .path()
            .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Archive, e))?;
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

#[cfg(any(has_bundled_cli, test))]
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
#[allow(dead_code)]
enum EmbeddedCliErrorKind {
    CreateDir,
    Archive,
    BinaryNotFoundInArchive,
    Io,
    /// Atomically renaming the staged temp file onto the final path failed.
    Publish,
    /// The published (or staged) file didn't match the trusted embedded image.
    Verification,
    /// Extraction kept producing a corrupt/missing binary across all retries —
    /// most likely antivirus interference.
    Blocked,
}

#[cfg(any(has_bundled_cli, test))]
impl std::fmt::Display for EmbeddedCliErrorKind {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            EmbeddedCliErrorKind::CreateDir => f.write_str("failed to create install directory"),
            EmbeddedCliErrorKind::Archive => f.write_str("failed to read archive entry"),
            EmbeddedCliErrorKind::BinaryNotFoundInArchive => {
                f.write_str("CLI binary not found in embedded archive")
            }
            EmbeddedCliErrorKind::Io => f.write_str("I/O error"),
            EmbeddedCliErrorKind::Publish => {
                f.write_str("failed to publish the extracted CLI binary")
            }
            EmbeddedCliErrorKind::Verification => {
                f.write_str("extracted CLI binary failed integrity verification")
            }
            EmbeddedCliErrorKind::Blocked => f.write_str(
                "bundled CLI appears blocked or corrupt after multiple attempts \
                 (possibly quarantined by antivirus)",
            ),
        }
    }
}

#[cfg(any(has_bundled_cli, test))]
#[allow(dead_code)]
struct EmbeddedCliError {
    repr: crate::errors::Repr<EmbeddedCliErrorKind>,
}

#[cfg(any(has_bundled_cli, test))]
#[allow(dead_code)]
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

    fn with_message(
        kind: EmbeddedCliErrorKind,
        message: impl Into<std::borrow::Cow<'static, str>>,
    ) -> Self {
        Self {
            repr: crate::errors::Repr::SimpleMessage(kind, message.into()),
        }
    }

    /// Build an error from `kind`, attaching the last failure as the source
    /// when one is available so the actionable message still carries context.
    fn with_source(kind: EmbeddedCliErrorKind, source: Option<EmbeddedCliError>) -> Self {
        match source {
            Some(source) => Self::new(kind, Box::new(source)),
            None => Self {
                repr: crate::errors::Repr::Simple(kind),
            },
        }
    }
}

#[cfg(any(has_bundled_cli, test))]
impl From<EmbeddedCliErrorKind> for EmbeddedCliError {
    fn from(kind: EmbeddedCliErrorKind) -> Self {
        Self {
            repr: crate::errors::Repr::Simple(kind),
        }
    }
}

#[cfg(any(has_bundled_cli, test))]
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

#[cfg(any(has_bundled_cli, test))]
impl std::fmt::Debug for EmbeddedCliError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "EmbeddedCliError({self})")
    }
}

#[cfg(any(has_bundled_cli, test))]
impl std::error::Error for EmbeddedCliError {
    fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
        match &self.repr {
            crate::errors::Repr::Custom(crate::errors::Custom { error, .. }) => Some(&**error),
            _ => None,
        }
    }
}

#[cfg(test)]
mod tests;
