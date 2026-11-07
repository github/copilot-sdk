// Copyright (c) Microsoft Corporation. All rights reserved.

use std::fs;
use std::io;
use std::os::unix::fs::{DirBuilderExt, MetadataExt, PermissionsExt};
use std::path::{Path, PathBuf};

use super::private_temporary_cache;

unsafe extern "C" {
    fn geteuid() -> u32;
}

pub(super) fn private_temporary_cache_dir(temp_dir: &Path) -> io::Result<PathBuf> {
    fs::DirBuilder::new()
        .recursive(true)
        .mode(0o755)
        .create(temp_dir)?;
    let base = fs::canonicalize(temp_dir)?;
    // SAFETY: geteuid has no arguments or preconditions; supported Unix targets use a u32 uid_t.
    let uid = unsafe { geteuid() };
    // Like explicit cache paths, the OS/application-selected temp base is trusted.
    let root = base.join(format!("copilot-sdk-cache-{uid}"));
    match fs::DirBuilder::new().mode(0o700).create(&root) {
        Ok(()) => {}
        Err(error) if error.kind() == io::ErrorKind::AlreadyExists => {}
        Err(error) => return Err(error),
    }
    let metadata = fs::symlink_metadata(&root)?;
    let mode = metadata.permissions().mode();
    if !private_temporary_cache(metadata.uid(), mode, metadata.is_dir(), uid) {
        return Err(io::Error::new(
            io::ErrorKind::PermissionDenied,
            format!(
                "unsafe temporary cache '{}' (owner UID {}, mode {mode:#o}); \
                 expected an owner-only directory for UID {uid}",
                root.display(),
                metadata.uid()
            ),
        ));
    }
    Ok(root)
}

#[cfg(test)]
#[path = "unix/tests.rs"]
mod tests;
