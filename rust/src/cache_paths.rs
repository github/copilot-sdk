use std::ffi::OsStr;
use std::io;
use std::path::{Path, PathBuf};

const SDK_CACHE_DIR: &str = "github-copilot-sdk";
#[cfg(test)]
const CLI_CACHE_DIR: &str = "cli";
const RUNTIME_CACHE_DIR: &str = "runtime";

#[cfg(unix)]
#[path = "cache_paths/unix.rs"]
mod unix;

// Shared by build.rs and the extracted-runtime resolver; unused by bundled builds and unit tests.
#[cfg_attr(any(test, feature = "bundled-cli"), allow(dead_code))]
pub(crate) fn extracted_runtime_install_dir(version: &str) -> io::Result<PathBuf> {
    runtime_install_dir(
        std::env::var_os("COPILOT_CLI_EXTRACT_DIR").as_deref(),
        platform_cache_root,
        version,
    )
}

fn runtime_install_dir(
    custom_dir: Option<&OsStr>,
    cache_dir: impl FnOnce() -> io::Result<PathBuf>,
    version: &str,
) -> io::Result<PathBuf> {
    match custom_dir {
        Some(custom_dir) => Ok(PathBuf::from(custom_dir)),
        None => Ok(cache_install_dir(&cache_dir()?, RUNTIME_CACHE_DIR, version)),
    }
}

pub(crate) fn cache_install_dir(cache_root: &Path, namespace: &str, version: &str) -> PathBuf {
    cache_root
        .join(SDK_CACHE_DIR)
        .join(namespace)
        .join(version_component(version))
}

pub(crate) fn platform_cache_root() -> io::Result<PathBuf> {
    if let Some(cache) = crate::platform_dirs::cache_dir() {
        return Ok(cache);
    }
    fallback_cache_root().map_err(|error| {
        io::Error::new(
            error.kind(),
            format!(
                "user cache unavailable; configure the platform user cache or an explicit \
                 extraction directory (`bundled_cli_extract_dir` for bundled artifacts, \
                 `COPILOT_CLI_EXTRACT_DIR` for non-bundled artifacts): {error}"
            ),
        )
    })
}

fn fallback_cache_root() -> io::Result<PathBuf> {
    #[cfg(unix)]
    {
        unix::private_temporary_cache_dir(&std::env::temp_dir())
    }
    #[cfg(not(unix))]
    {
        Ok(std::env::temp_dir())
    }
}

#[cfg(any(unix, test))]
fn private_temporary_cache(owner: u32, mode: u32, is_directory: bool, uid: u32) -> bool {
    is_directory && owner == uid && mode & 0o077 == 0
}

fn version_component(version: &str) -> String {
    version
        .chars()
        .map(|c| match c {
            'a'..='z' | 'A'..='Z' | '0'..='9' | '.' | '-' | '_' => c,
            _ => '_',
        })
        .collect()
}

#[cfg(test)]
#[path = "cache_paths/tests.rs"]
mod tests;
