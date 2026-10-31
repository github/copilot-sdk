// Copyright (c) Microsoft Corporation. All rights reserved.

use std::ffi::OsString;
use std::path::PathBuf;

use super::Directory;

pub(super) fn directory(
    kind: Directory,
    apple: bool,
    env: impl Fn(&str) -> Option<OsString>,
    home: impl FnOnce() -> Option<PathBuf>,
) -> Option<PathBuf> {
    let suffix = match kind {
        Directory::Home => "",
        Directory::Cache if apple => "Library/Caches",
        Directory::Config | Directory::LocalData if apple => "Library/Application Support",
        Directory::Cache => ".cache",
        Directory::Config => ".config",
        Directory::LocalData => ".local/share",
    };
    if !apple {
        let key = match kind {
            Directory::Home => None,
            Directory::Cache => Some("XDG_CACHE_HOME"),
            Directory::Config => Some("XDG_CONFIG_HOME"),
            Directory::LocalData => Some("XDG_DATA_HOME"),
        };
        if let Some(path) = key.and_then(&env).filter(|path| {
            // Unix absolute paths start with '/', even when these decisions
            // are exercised on Windows by the cross-platform policy tests.
            path.as_encoded_bytes().starts_with(b"/")
        }) {
            return Some(path.into());
        }
    }
    // std accepts an empty passwd home; dirs treated it as unavailable.
    let home = home().filter(|home| !home.as_os_str().is_empty())?;
    if suffix.is_empty() {
        Some(home)
    } else {
        Some(home.join(suffix))
    }
}
