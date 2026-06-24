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
//! [`path`] / [`install_at`].
//!
//! The embedded bytes are part of the consumer's signed binary and therefore
//! trusted *as the source of truth* — but the bytes that land on disk are not.
//! A non-atomic write, a multi-process race, or antivirus quarantining the
//! freshly-written executable can leave a truncated or corrupt image that, if
//! handed back as "good", fails to launch (e.g. Windows `ERROR_BAD_EXE_FORMAT`).
//! Installation therefore: extracts to a unique temp file in the target dir,
//! fsyncs and marks it executable, verifies the staged bytes against the
//! trusted in-memory image, atomically renames it into place, re-verifies the
//! published file, and records an integrity marker. Subsequent runs trust an
//! existing install only after a cheap re-check (size marker + executable-image
//! header); anything that looks truncated or quarantined is re-extracted, and
//! the whole publish is retried before surfacing a clear, actionable error.

// The atomic-publish + verify helpers (and their unit tests) are pure
// std-only logic that doesn't touch the embedded archive, so they compile
// whenever the binary is bundled *or* we're building the test harness —
// the standard `cargo test --no-default-features` job has `has_bundled_cli`
// off but still needs to exercise them.
#[cfg(any(has_bundled_cli, test))]
use std::fs;
#[cfg(all(has_bundled_cli, not(windows)))]
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
/// Idempotent: skips extraction when an already-published binary passes the
/// integrity re-check (size marker + executable-image header), and
/// re-extracts a corrupt or quarantined one.
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

/// Number of times we re-extract + re-publish the binary before giving up.
/// A single transient failure (e.g. antivirus briefly locking or quarantining
/// the freshly-written file) is retried; a persistent one surfaces a clear
/// error rather than handing back a broken path.
#[cfg(has_bundled_cli)]
const MAX_PUBLISH_ATTEMPTS: u32 = 3;

#[cfg(has_bundled_cli)]
fn install(install_dir: &Path, archive: &[u8]) -> Result<PathBuf, EmbeddedCliError> {
    let verbose = std::env::var("COPILOT_CLI_INSTALL_VERBOSE").ok().as_deref() == Some("1");

    fs::create_dir_all(install_dir)
        .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::CreateDir, e))?;

    let final_path = install_dir.join(CLI_BINARY_NAME);
    let marker_path = marker_path(install_dir);

    // Fast path: a previous install left both the binary and the integrity
    // marker we wrote *after* verifying it. Re-validate cheaply (size +
    // executable-image magic) so a binary that was later truncated or
    // quarantined by antivirus is re-extracted instead of trusted blindly.
    if existing_install_is_valid(&final_path, &marker_path) {
        if verbose {
            eprintln!("embedded CLI already installed at {}", final_path.display());
        }
        return Ok(final_path);
    }

    // The bytes extracted from the embedded archive are part of the
    // consumer's trusted, signed binary — so they are the known-good
    // reference we verify the on-disk file against after publishing.
    let start = std::time::Instant::now();
    let bytes = extract_binary(archive, CLI_BINARY_NAME)?;
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
fn existing_install_is_valid(final_path: &Path, marker_path: &Path) -> bool {
    let Ok(meta) = fs::metadata(final_path) else {
        return false;
    };
    if !meta.is_file() || meta.len() == 0 {
        return false;
    }
    match read_marker_len(marker_path) {
        Some(expected) if expected == meta.len() => looks_like_valid_image(final_path),
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
    static COUNTER: AtomicU64 = AtomicU64::new(0);
    let nanos = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .map(|d| d.as_nanos())
        .unwrap_or(0);
    let unique = format!(
        ".copilot-cli.tmp.{}.{}.{}",
        std::process::id(),
        COUNTER.fetch_add(1, Ordering::Relaxed),
        nanos
    );
    let tmp = dir.join(unique);

    // `create_new` guarantees we never clobber a sibling's in-flight temp
    // file (the pid + counter + nanos name already makes that practically
    // impossible).
    let mut file = fs::OpenOptions::new()
        .write(true)
        .create_new(true)
        .open(&tmp)
        .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Io, e))?;

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

/// Atomically move the staged temp file onto `final_path`.
///
/// `rename` replaces the target atomically on POSIX, but on Windows it fails
/// when the target already exists — so on that error we remove the stale file
/// and retry. The remove-then-rename is the only non-atomic window, and it's
/// guarded upstream: callers re-verify the published file and, on a lost race,
/// accept a peer's identical install instead of erroring.
#[cfg(any(has_bundled_cli, test))]
fn publish(tmp: &Path, final_path: &Path) -> Result<(), EmbeddedCliError> {
    match fs::rename(tmp, final_path) {
        Ok(()) => Ok(()),
        Err(_) if final_path.exists() => {
            let _ = fs::remove_file(final_path);
            fs::rename(tmp, final_path)
                .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Publish, e))
        }
        Err(e) => Err(EmbeddedCliError::new(EmbeddedCliErrorKind::Publish, e)),
    }
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

#[cfg(all(has_bundled_cli, windows))]
fn extract_binary(archive: &[u8], binary_name: &str) -> Result<Vec<u8>, EmbeddedCliError> {
    let cursor = std::io::Cursor::new(archive);
    let mut zip = zip::ZipArchive::new(cursor)
        .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Zip, e))?;
    for i in 0..zip.len() {
        let mut entry = zip
            .by_index(i)
            .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Zip, e))?;
        let name = entry.name().to_string();
        if name == binary_name || name.ends_with(&format!("/{binary_name}")) {
            let mut bytes = Vec::with_capacity(entry.size() as usize);
            std::io::copy(&mut entry, &mut bytes)
                .map_err(|e| EmbeddedCliError::new(EmbeddedCliErrorKind::Io, e))?;
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
    #[cfg(not(windows))]
    Archive,
    #[cfg(windows)]
    Zip,
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
            #[cfg(not(windows))]
            EmbeddedCliErrorKind::Archive => f.write_str("failed to read archive entry"),
            #[cfg(windows)]
            EmbeddedCliErrorKind::Zip => f.write_str("failed to read zip archive"),
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
mod tests {
    use super::*;

    /// Bytes whose header looks like a valid executable image on the host
    /// platform, so `looks_like_valid_image` accepts them. `extra` padding
    /// bytes follow the magic so size checks have something to disagree about.
    fn fake_image(extra: usize) -> Vec<u8> {
        let mut bytes = Vec::new();
        #[cfg(windows)]
        bytes.extend_from_slice(b"MZ\x90\x00");
        #[cfg(target_os = "macos")]
        bytes.extend_from_slice(&[0xfe, 0xed, 0xfa, 0xcf]);
        #[cfg(all(not(windows), not(target_os = "macos")))]
        bytes.extend_from_slice(b"\x7fELF");
        bytes.extend(std::iter::repeat_n(0xAB, extra));
        bytes
    }

    #[test]
    fn publish_verified_writes_and_records_marker() {
        let dir = tempfile::tempdir().expect("tempdir");
        let final_path = dir.path().join("copilot-bin");
        let marker = marker_path(dir.path());
        let bytes = fake_image(2048);

        publish_verified(dir.path(), &final_path, &marker, &bytes).expect("publish");

        assert!(final_path.is_file(), "binary should be published");
        assert_eq!(fs::read(&final_path).expect("read"), bytes);
        assert_eq!(read_marker_len(&marker), Some(bytes.len() as u64));
        assert!(existing_install_is_valid(&final_path, &marker));

        // No leftover temp files in the install dir.
        let leftovers: Vec<_> = fs::read_dir(dir.path())
            .expect("read_dir")
            .filter_map(|e| e.ok())
            .filter(|e| e.file_name().to_string_lossy().contains(".tmp."))
            .collect();
        assert!(leftovers.is_empty(), "temp files should be cleaned up");
    }

    #[test]
    fn publish_overwrites_an_existing_binary() {
        let dir = tempfile::tempdir().expect("tempdir");
        let final_path = dir.path().join("copilot-bin");
        let marker = marker_path(dir.path());

        // Pre-existing (stale) binary at the destination.
        fs::write(&final_path, b"old contents").expect("seed");

        let bytes = fake_image(512);
        publish_verified(dir.path(), &final_path, &marker, &bytes).expect("publish");

        assert_eq!(fs::read(&final_path).expect("read"), bytes);
    }

    #[test]
    fn corrupt_or_unmarked_install_is_rejected() {
        let dir = tempfile::tempdir().expect("tempdir");
        let final_path = dir.path().join("copilot-bin");
        let marker = marker_path(dir.path());
        let bytes = fake_image(4096);

        // Missing binary entirely.
        assert!(!existing_install_is_valid(&final_path, &marker));

        // Valid binary but no marker (e.g. installed by an older SDK).
        fs::write(&final_path, &bytes).expect("write binary");
        assert!(
            !existing_install_is_valid(&final_path, &marker),
            "an install without a marker must not be trusted"
        );

        // Marker present but the binary was later truncated (partial write /
        // antivirus). Marker still records the original full size.
        write_marker(&marker, bytes.len() as u64).expect("marker");
        assert!(existing_install_is_valid(&final_path, &marker));
        fs::write(&final_path, &bytes[..bytes.len() / 2]).expect("truncate");
        assert!(
            !existing_install_is_valid(&final_path, &marker),
            "a truncated binary must be detected via the size marker"
        );

        // Zero-length binary (quarantined to empty).
        fs::write(&final_path, b"").expect("empty");
        assert!(!existing_install_is_valid(&final_path, &marker));
    }

    #[test]
    fn invalid_image_header_is_rejected() {
        let dir = tempfile::tempdir().expect("tempdir");
        let final_path = dir.path().join("copilot-bin");
        let marker = marker_path(dir.path());

        // Right size, has a marker, but the bytes are not a valid image.
        let garbage = vec![0u8; 4096];
        fs::write(&final_path, &garbage).expect("write garbage");
        write_marker(&marker, garbage.len() as u64).expect("marker");

        assert!(
            !existing_install_is_valid(&final_path, &marker),
            "a non-executable image must be rejected even with a matching marker"
        );
    }

    #[test]
    fn verification_rejects_size_and_content_mismatch() {
        let dir = tempfile::tempdir().expect("tempdir");
        let path = dir.path().join("staged");
        let expected = fake_image(1024);

        // Exact match passes.
        fs::write(&path, &expected).expect("write");
        verify_on_disk_matches(&path, &expected).expect("exact match should verify");

        // Truncated -> size mismatch.
        fs::write(&path, &expected[..100]).expect("truncate");
        assert!(verify_on_disk_matches(&path, &expected).is_err());

        // Same length, different bytes -> content mismatch.
        let mut tampered = expected.clone();
        *tampered.last_mut().expect("non-empty") ^= 0xFF;
        fs::write(&path, &tampered).expect("tamper");
        assert!(verify_on_disk_matches(&path, &expected).is_err());

        // Missing file -> I/O error.
        fs::remove_file(&path).expect("remove");
        assert!(verify_on_disk_matches(&path, &expected).is_err());
    }

    #[test]
    fn temp_files_are_unique_and_synced() {
        let dir = tempfile::tempdir().expect("tempdir");
        let data = fake_image(256);

        let a = write_temp_file(dir.path(), &data).expect("temp a");
        let b = write_temp_file(dir.path(), &data).expect("temp b");

        assert_ne!(a, b, "temp file names must be unique");
        assert_eq!(fs::read(&a).expect("read a"), data);
        assert_eq!(fs::read(&b).expect("read b"), data);

        #[cfg(unix)]
        {
            use std::os::unix::fs::PermissionsExt;
            let mode = fs::metadata(&a).expect("meta").permissions().mode();
            assert_eq!(mode & 0o777, 0o755, "temp binary should be executable");
        }
    }
}
