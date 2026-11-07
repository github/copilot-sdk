/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use std::ffi::OsStr;
use std::fs;
use std::io;
use std::path::{Path, PathBuf};

use tempfile::tempdir;

#[cfg(not(unix))]
use super::fallback_cache_root;
use super::{CLI_CACHE_DIR, cache_install_dir, private_temporary_cache, runtime_install_dir};

#[cfg(not(unix))]
#[test]
fn non_unix_fallback_preserves_system_temp_dir() {
    assert_eq!(
        fallback_cache_root().expect("system temporary directory"),
        std::env::temp_dir()
    );
}

#[test]
fn build_and_runtime_share_versioned_cache_paths() {
    let version = "1.2.3/test";
    let cache = Path::new("cache");
    assert_eq!(
        runtime_install_dir(None, || Ok(cache.into()), version).expect("available cache"),
        cache
            .join("github-copilot-sdk")
            .join("runtime")
            .join("1.2.3_test")
    );
}

#[test]
fn unsafe_default_cache_error_is_propagated() {
    let error = runtime_install_dir(
        None,
        || {
            Err(io::Error::new(
                io::ErrorKind::PermissionDenied,
                "unsafe cache",
            ))
        },
        "1.2.3",
    )
    .expect_err("unsafe default must not produce an executable path");
    assert_eq!(error.kind(), io::ErrorKind::PermissionDenied);
    assert_eq!(error.to_string(), "unsafe cache");
}

#[test]
fn temporary_cache_must_be_an_owner_only_directory() {
    for (owner, mode, directory, expected) in [
        (1001, 0o700, true, true),
        (1001, 0o755, true, false),
        (1001, 0o770, true, false),
        (1002, 0o700, true, false),
        (0, 0o700, true, false),
        (1001, 0o700, false, false),
    ] {
        assert_eq!(
            private_temporary_cache(owner, mode, directory, 1001),
            expected,
            "owner={owner} mode={mode:#o} directory={directory}"
        );
    }
}

#[test]
fn empty_custom_directory_is_not_replaced_with_a_default() {
    assert_eq!(
        runtime_install_dir(
            Some(OsStr::new("")),
            || panic!("must not discover cache"),
            "1.2.3"
        )
        .expect("explicit empty path is preserved"),
        PathBuf::new()
    );
}

#[test]
fn custom_runtime_directory_is_used_directly() {
    let custom = OsStr::new("custom-runtime");

    assert_eq!(
        runtime_install_dir(Some(custom), || panic!("must not discover cache"), "1.2.3")
            .expect("explicit path needs no user cache"),
        PathBuf::from(custom)
    );
}

#[test]
fn stale_runtime_cleanup_cannot_remove_same_version_bundled_cli() {
    let cache = tempdir().expect("create cache root");
    let version = "1.2.3/test";
    let bundled_cli_dir = cache_install_dir(cache.path(), CLI_CACHE_DIR, version);
    let runtime_dir =
        runtime_install_dir(None, || Ok(cache.path().into()), version).expect("available cache");
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
