// Copyright (c) Microsoft Corporation. All rights reserved.

//! The four user-directory lookups shared by SDK provisioning and native hosts.
//!
//! No directories are created. Unavailable OS lookups
//! return `None`; choosing a temporary-directory fallback belongs to the caller.
//! The SDK and its build script include this same source privately so published
//! SDK packages need neither another published crate nor runtime-crate dependencies.

use std::path::PathBuf;

#[cfg(any(unix, test))]
mod unix;
#[cfg(windows)]
mod windows;

#[derive(Clone, Copy)]
enum Directory {
    Home,
    Cache,
    Config,
    LocalData,
}

/// The user's home directory.
///
/// Unix uses nonempty `HOME`, then the current user's passwd entry. Windows
/// uses `FOLDERID_Profile`, not environment variables.
pub fn home_dir() -> Option<PathBuf> {
    directory(Directory::Home)
}

/// The user's cache directory.
///
/// Linux uses absolute `XDG_CACHE_HOME`, otherwise `HOME/.cache`. macOS uses
/// `HOME/Library/Caches`. Windows uses the LocalAppData known folder.
pub fn cache_dir() -> Option<PathBuf> {
    directory(Directory::Cache)
}

/// The user's configuration directory.
///
/// Linux uses absolute `XDG_CONFIG_HOME`, otherwise `HOME/.config`. macOS uses
/// `HOME/Library/Application Support`. Windows uses the RoamingAppData known folder.
pub fn config_dir() -> Option<PathBuf> {
    directory(Directory::Config)
}

/// The user's machine-local application data directory.
///
/// Linux uses absolute `XDG_DATA_HOME`, otherwise `HOME/.local/share`. macOS uses
/// `HOME/Library/Application Support`. Windows uses the LocalAppData known folder.
pub fn data_local_dir() -> Option<PathBuf> {
    directory(Directory::LocalData)
}

fn directory(kind: Directory) -> Option<PathBuf> {
    #[cfg(windows)]
    {
        windows::directory(kind)
    }
    #[cfg(unix)]
    {
        unix::directory(
            kind,
            cfg!(any(target_os = "macos", target_os = "ios")),
            |key| std::env::var_os(key),
            std::env::home_dir,
        )
    }
    #[cfg(not(any(windows, unix)))]
    {
        let _ = kind;
        None
    }
}

#[cfg(test)]
mod tests;
