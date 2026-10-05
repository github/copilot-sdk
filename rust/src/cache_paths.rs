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
mod tests {
    use std::ffi::OsStr;
    use std::fs;
    use std::path::{Path, PathBuf};

    use tempfile::tempdir;

    use super::{CLI_CACHE_DIR, cache_install_dir, runtime_install_dir};

    #[test]
    fn custom_runtime_directory_is_used_directly() {
        let cache = Path::new("ignored-cache");
        let custom = OsStr::new("custom-runtime");

        assert_eq!(
            runtime_install_dir(Some(custom), cache, "1.2.3"),
            PathBuf::from(custom)
        );
    }

    #[test]
    fn stale_runtime_cleanup_cannot_remove_same_version_bundled_cli() {
        let cache = tempdir().expect("create cache root");
        let version = "1.2.3/test";
        let bundled_cli_dir = cache_install_dir(cache.path(), CLI_CACHE_DIR, version);
        let runtime_dir = runtime_install_dir(None, cache.path(), version);
        let bundled_cli = bundled_cli_dir.join(if cfg!(windows) {
            "copilot.exe"
        } else {
            "copilot"
        });
        let runtime_marker = runtime_dir.join(".hostless-runtime-assets-v1");

        fs::create_dir_all(&bundled_cli_dir).expect("create bundled CLI directory");
        fs::write(&bundled_cli, b"bundled-cli").expect("write bundled CLI");
        fs::create_dir_all(&runtime_dir).expect("create runtime directory");
        fs::write(&runtime_marker, b"stale").expect("write stale runtime marker");

        assert_ne!(runtime_dir, bundled_cli_dir);
        fs::remove_dir_all(&runtime_dir).expect("clear stale runtime directory");

        assert_eq!(
            fs::read(&bundled_cli).expect("bundled CLI survives runtime cleanup"),
            b"bundled-cli"
        );
        assert!(!runtime_marker.exists());
    }
}
