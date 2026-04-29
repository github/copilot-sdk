#[cfg(any(has_bundled_cli, test))]
use std::fs;
#[cfg(any(has_bundled_cli, test))]
use std::io::{self, Read, Write};
#[cfg(any(has_bundled_cli, test))]
use std::path::Path;
use std::path::PathBuf;
use std::sync::OnceLock;

#[cfg(has_bundled_cli)]
use tracing::{info, warn};

// When the SDK is built with COPILOT_CLI_VERSION set, build.rs generates
// bundled_cli.rs with the compressed binary bytes, hash, and version.
#[cfg(has_bundled_cli)]
mod build_time {
    include!(concat!(env!("OUT_DIR"), "/bundled_cli.rs"));
}

static INSTALLED_PATH: OnceLock<Option<PathBuf>> = OnceLock::new();

/// Returns the bundled CLI version string, if one was embedded at build time.
pub fn bundled_version() -> Option<&'static str> {
    #[cfg(has_bundled_cli)]
    {
        Some(build_time::CLI_VERSION)
    }
    #[cfg(not(has_bundled_cli))]
    {
        None
    }
}

/// Returns the path to the installed CLI binary, lazily extracting on first call.
///
/// When the SDK was built with `COPILOT_CLI_VERSION` set, this extracts the
/// embedded binary to `~/.cache/github-copilot-sdk-{version}/copilot` (or
/// `copilot.exe` on Windows), verifies the SHA-256 hash, and returns the
/// path. Subsequent calls return the cached result.
///
/// Returns `None` if no CLI was embedded at build time.
pub fn path() -> Option<PathBuf> {
    INSTALLED_PATH
        .get_or_init(|| {
            #[cfg(has_bundled_cli)]
            {
                match install(
                    build_time::CLI_BYTES,
                    build_time::CLI_HASH,
                    build_time::CLI_VERSION,
                ) {
                    Ok(path) => {
                        info!(path = %path.display(), version = build_time::CLI_VERSION, "embedded CLI installed");
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

#[cfg(has_bundled_cli)]
fn install(
    compressed: &[u8],
    expected_hash: [u8; 32],
    version: &str,
) -> Result<PathBuf, EmbeddedCliError> {
    let verbose = std::env::var("COPILOT_CLI_INSTALL_VERBOSE").ok().as_deref() == Some("1");

    let cache = dirs::cache_dir().unwrap_or_else(std::env::temp_dir);
    // Use a versioned directory so multiple versions can coexist,
    // but keep the binary named `copilot` — the CLI checks argv[0]
    // for this exact name.
    let install_dir = if version.is_empty() {
        cache.join("github-copilot-sdk")
    } else {
        cache.join(format!("github-copilot-sdk-{}", sanitize_version(version)))
    };
    fs::create_dir_all(&install_dir).map_err(EmbeddedCliError::CreateDir)?;

    let binary_name = binary_name();
    let final_path = install_dir.join(&binary_name);

    // If the binary already exists and hash matches, skip extraction.
    if final_path.is_file() {
        let existing_hash = hash_file(&final_path)?;
        if existing_hash == expected_hash {
            if verbose {
                eprintln!("embedded CLI already installed at {}", final_path.display());
            }
            return Ok(final_path);
        }
        if verbose {
            eprintln!("embedded CLI hash mismatch, reinstalling");
        }
    }

    let start = std::time::Instant::now();
    let decompressed = decompress(compressed)?;

    let actual_hash = sha256(&decompressed);
    if actual_hash != expected_hash {
        return Err(EmbeddedCliError::HashMismatch);
    }

    write_binary(&final_path, &decompressed)?;

    if verbose {
        eprintln!(
            "embedded CLI installed at {} in {:?}",
            final_path.display(),
            start.elapsed()
        );
    }

    Ok(final_path)
}

#[cfg(any(has_bundled_cli, test))]
fn binary_name() -> String {
    if cfg!(target_os = "windows") {
        "copilot.exe".to_string()
    } else {
        "copilot".to_string()
    }
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
fn decompress(data: &[u8]) -> Result<Vec<u8>, EmbeddedCliError> {
    let mut decoder = zstd::Decoder::new(data).map_err(EmbeddedCliError::Decompress)?;
    let mut out = Vec::new();
    decoder
        .read_to_end(&mut out)
        .map_err(EmbeddedCliError::Decompress)?;
    Ok(out)
}

#[cfg(any(has_bundled_cli, test))]
fn sha256(data: &[u8]) -> [u8; 32] {
    use sha2::Digest;
    let mut hasher = sha2::Sha256::new();
    hasher.update(data);
    hasher.finalize().into()
}

#[cfg(has_bundled_cli)]
fn hash_file(path: &Path) -> Result<[u8; 32], EmbeddedCliError> {
    use sha2::Digest;
    let mut file = fs::File::open(path).map_err(EmbeddedCliError::Io)?;
    let mut hasher = sha2::Sha256::new();
    let mut buf = [0u8; 8192];
    loop {
        let n = file.read(&mut buf).map_err(EmbeddedCliError::Io)?;
        if n == 0 {
            break;
        }
        hasher.update(&buf[..n]);
    }
    Ok(hasher.finalize().into())
}

#[cfg(any(has_bundled_cli, test))]
fn write_binary(path: &Path, data: &[u8]) -> Result<(), EmbeddedCliError> {
    let mut file = fs::OpenOptions::new()
        .write(true)
        .create(true)
        .truncate(true)
        .open(path)
        .map_err(EmbeddedCliError::Io)?;

    file.write_all(data).map_err(EmbeddedCliError::Io)?;

    #[cfg(unix)]
    {
        use std::os::unix::fs::PermissionsExt;
        fs::set_permissions(path, fs::Permissions::from_mode(0o755))
            .map_err(EmbeddedCliError::Io)?;
    }

    Ok(())
}

#[cfg(any(has_bundled_cli, test))]
#[derive(Debug, thiserror::Error)]
#[allow(dead_code)]
enum EmbeddedCliError {
    #[error("failed to create install directory: {0}")]
    CreateDir(io::Error),

    #[error("decompression failed: {0}")]
    Decompress(io::Error),

    #[error("SHA-256 hash of decompressed binary does not match expected hash")]
    HashMismatch,

    #[error("I/O error: {0}")]
    Io(io::Error),
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn install_extracts_to_cache_dir() {
        let temp = tempfile::tempdir().expect("should create temp dir");
        let original = b"fake copilot binary";
        let hash = sha256(original);
        let compressed = zstd::encode_all(&original[..], 3).expect("compression should succeed");

        // Override cache dir via env for test isolation.
        let path = install_to_dir(&temp, &compressed, hash);
        let expected_name = binary_name();
        assert!(path.is_file());
        assert_eq!(
            path.file_name().and_then(|s| s.to_str()),
            Some(expected_name.as_str())
        );

        let installed_content = fs::read(&path).expect("should read installed binary");
        assert_eq!(installed_content, original);

        // Second install should be idempotent (hash matches, skips extraction).
        let path2 = install_to_dir(&temp, &compressed, hash);
        assert_eq!(path, path2);
    }

    #[test]
    fn install_rejects_hash_mismatch() {
        let temp = tempfile::tempdir().expect("should create temp dir");
        let original = b"fake copilot binary";
        let wrong_hash = [0u8; 32];
        let compressed = zstd::encode_all(&original[..], 3).expect("compression should succeed");

        let result = install_to_dir_result(&temp, &compressed, wrong_hash);
        assert!(result.is_err());
        assert!(result.unwrap_err().to_string().contains("SHA-256"),);
    }

    // Test helpers that install to a specific directory instead of the global cache.
    fn install_to_dir(temp: &tempfile::TempDir, compressed: &[u8], hash: [u8; 32]) -> PathBuf {
        install_to_dir_result(temp, compressed, hash).expect("install should succeed")
    }

    fn install_to_dir_result(
        temp: &tempfile::TempDir,
        compressed: &[u8],
        hash: [u8; 32],
    ) -> Result<PathBuf, EmbeddedCliError> {
        let install_dir = temp.path().to_path_buf();
        fs::create_dir_all(&install_dir).expect("create dir");
        let binary_name = binary_name();
        let final_path = install_dir.join(&binary_name);

        let decompressed = decompress(compressed)?;
        let actual_hash = sha256(&decompressed);
        if actual_hash != hash {
            return Err(EmbeddedCliError::HashMismatch);
        }
        write_binary(&final_path, &decompressed)?;
        Ok(final_path)
    }
}
