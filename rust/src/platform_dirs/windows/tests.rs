// Copyright (c) Microsoft Corporation. All rights reserved.

#![cfg(test)]

use super::*;

#[test]
fn known_folders_preserve_redirected_unicode_and_unpaired_utf16_paths() {
    for mut wide in [
        "Z:\\redirected\\\u{7528}\u{6237}"
            .encode_utf16()
            .collect::<Vec<_>>(),
        "\\\\server\\share\\profile".encode_utf16().collect(),
        format!("C:\\{}", "a".repeat(600)).encode_utf16().collect(),
        vec![b'C' as u16, b':' as u16, b'\\' as u16, 0xd800],
        vec![],
    ] {
        let expected = PathBuf::from(OsString::from_wide(&wide));
        wide.push(0);
        let output = wide.as_mut_ptr();
        let mut freed = false;
        // SAFETY: wide owns the terminated buffer for the duration of decoding.
        let actual = unsafe {
            known_folder_with(
                |path| {
                    *path = output;
                    0
                },
                |path| {
                    assert_eq!(path, output);
                    freed = true;
                },
            )
        };
        assert_eq!(actual, Some(expected));
        assert!(freed);
    }
}

#[test]
fn failed_and_null_known_folder_results_always_release_the_output() {
    for status in [0, 1, -1] {
        for allocated in [false, true] {
            if status == 0 && allocated {
                continue;
            }
            // An unterminated buffer on failure must never be read.
            let mut wide = [0xffff];
            let output = if allocated {
                wide.as_mut_ptr()
            } else {
                std::ptr::null_mut()
            };
            let mut frees = 0;
            // SAFETY: only the failure branches receive a non-null buffer.
            let actual = unsafe {
                known_folder_with(
                    |path| {
                        *path = output;
                        status
                    },
                    |path| {
                        assert_eq!(path, output);
                        frees += 1;
                    },
                )
            };
            assert_eq!(actual, None);
            assert_eq!(frees, 1);
        }
    }
}

#[test]
fn public_windows_lookups_match_the_os_known_folders() {
    const CHILD: &str = "COPILOT_PLATFORM_DIRS_KNOWN_FOLDER_TEST_CHILD";
    if std::env::var_os(CHILD).is_none() {
        use std::os::windows::process::CommandExt;

        const CREATE_NO_WINDOW: u32 = 0x0800_0000;
        let current = std::thread::current();
        let output = std::process::Command::new(std::env::current_exe().unwrap())
            .current_dir(std::env::temp_dir())
            .creation_flags(CREATE_NO_WINDOW)
            .args(["--exact", current.name().expect("test name"), "--nocapture"])
            .env(CHILD, "1")
            .env("LOCALAPPDATA", "Z:\\not-the-local-known-folder")
            .env("APPDATA", "Z:\\not-the-roaming-known-folder")
            .env("USERPROFILE", "Z:\\not-the-profile-known-folder")
            .env("HOME", "Z:\\not-the-home-folder")
            .output()
            .expect("run known-folder lookup with spoofed environment");
        assert!(
            output.status.success() && String::from_utf8_lossy(&output.stdout).contains("1 passed"),
            "{}{}",
            String::from_utf8_lossy(&output.stdout),
            String::from_utf8_lossy(&output.stderr)
        );
        return;
    }

    for (lookup, folder) in [
        (
            super::super::home_dir as fn() -> Option<PathBuf>,
            FOLDERID_Profile,
        ),
        (super::super::cache_dir, FOLDERID_LocalAppData),
        (super::super::config_dir, FOLDERID_RoamingAppData),
        (super::super::data_local_dir, FOLDERID_LocalAppData),
    ] {
        // SAFETY: the Windows API supplies its owned terminated buffer.
        let expected = unsafe {
            known_folder_with(
                |path| SHGetKnownFolderPath(&folder, 0, std::ptr::null_mut(), path),
                |path| CoTaskMemFree(path.cast()),
            )
        };
        assert_eq!(lookup(), expected);
    }
}

#[test]
fn unknown_os_known_folder_returns_none() {
    let folder = windows_sys::core::GUID::from_u128(0);
    // SAFETY: the real API and its allocator are paired, even on failure.
    let actual = unsafe {
        known_folder_with(
            |path| SHGetKnownFolderPath(&folder, 0, std::ptr::null_mut(), path),
            |path| CoTaskMemFree(path.cast()),
        )
    };
    assert_eq!(actual, None);
}
