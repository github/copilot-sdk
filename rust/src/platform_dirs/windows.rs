// Copyright (c) Microsoft Corporation. All rights reserved.

use std::ffi::OsString;
use std::os::windows::ffi::OsStringExt;
use std::path::PathBuf;

use windows_sys::Win32::System::Com::CoTaskMemFree;
use windows_sys::Win32::UI::Shell::{
    FOLDERID_LocalAppData, FOLDERID_Profile, FOLDERID_RoamingAppData, SHGetKnownFolderPath,
};

use super::Directory;

pub(super) fn directory(kind: Directory) -> Option<PathBuf> {
    let folder = match kind {
        Directory::Home => FOLDERID_Profile,
        Directory::Cache | Directory::LocalData => FOLDERID_LocalAppData,
        Directory::Config => FOLDERID_RoamingAppData,
    };
    // SAFETY: SHGetKnownFolderPath initializes an owned, NUL-terminated UTF-16
    // buffer; CoTaskMemFree accepts that allocation, including on failure/null.
    unsafe {
        known_folder_with(
            |path| SHGetKnownFolderPath(&folder, 0, std::ptr::null_mut(), path),
            |path| CoTaskMemFree(path.cast()),
        )
    }
}

// SAFETY: lookup must supply a readable NUL-terminated buffer on success.
// free must release any buffer lookup returns, including on failure.
unsafe fn known_folder_with(
    lookup: impl FnOnce(&mut *mut u16) -> i32,
    free: impl FnOnce(*mut u16),
) -> Option<PathBuf> {
    let mut path = std::ptr::null_mut();
    let status = lookup(&mut path);
    let result = if status == 0 && !path.is_null() {
        // SAFETY: the caller guarantees the buffer's lifetime and terminator.
        let value = unsafe {
            let len = (0..).take_while(|&i| *path.add(i) != 0).count();
            OsString::from_wide(std::slice::from_raw_parts(path, len))
        };
        Some(PathBuf::from(value))
    } else {
        None
    };
    // No early returns: Windows can allocate even when the lookup fails.
    free(path);
    result
}

#[cfg(test)]
mod tests;
