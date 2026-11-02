use std::ffi::OsStr;
use std::path::{Path, PathBuf};

const SDK_CACHE_DIR: &str = "github-copilot-sdk";
#[cfg(test)]
const CLI_CACHE_DIR: &str = "cli";
const RUNTIME_CACHE_DIR: &str = "runtime";

pub(crate) fn extracted_runtime_install_dir(version: &str) -> PathBuf {
    runtime_install_dir(
        std::env::var_os("COPILOT_CLI_EXTRACT_DIR").as_deref(),
        &platform_cache_dir(),
        version,
    )
}

fn runtime_install_dir(custom_dir: Option<&OsStr>, cache_root: &Path, version: &str) -> PathBuf {
    match custom_dir {
        Some(custom_dir) => PathBuf::from(custom_dir),
        None => cache_install_dir(cache_root, RUNTIME_CACHE_DIR, version),
    }
}

fn cache_install_dir(cache_root: &Path, namespace: &str, version: &str) -> PathBuf {
    cache_root
        .join(SDK_CACHE_DIR)
        .join(namespace)
        .join(version_component(version))
}

fn platform_cache_dir() -> PathBuf {
    dirs::cache_dir().unwrap_or_else(std::env::temp_dir)
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
