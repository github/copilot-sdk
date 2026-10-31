/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use std::ffi::OsStr;
use std::fs;
use std::path::{Path, PathBuf};

use tempfile::tempdir;

use super::{
    CLI_CACHE_DIR, cache_install_dir, extracted_runtime_install_dir, platform_cache_dir,
    runtime_install_dir,
};

#[test]
fn build_and_runtime_shared_path_uses_the_platform_cache_or_temp_fallback() {
    let version = "1.2.3/test";
    let cache = crate::platform_dirs::cache_dir().unwrap_or_else(std::env::temp_dir);
    assert_eq!(platform_cache_dir(), cache);
    assert_eq!(
        extracted_runtime_install_dir(version),
        runtime_install_dir(
            std::env::var_os("COPILOT_CLI_EXTRACT_DIR").as_deref(),
            &cache,
            version
        )
    );
    assert_eq!(
        runtime_install_dir(None, &cache, version),
        cache
            .join("github-copilot-sdk")
            .join("runtime")
            .join("1.2.3_test")
    );
}

#[test]
fn empty_custom_directory_is_not_replaced_with_a_default() {
    assert_eq!(
        runtime_install_dir(Some(OsStr::new("")), Path::new("cache"), "1.2.3"),
        PathBuf::new()
    );
}

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
