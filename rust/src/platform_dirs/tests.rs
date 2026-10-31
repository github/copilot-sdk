// Copyright (c) Microsoft Corporation. All rights reserved.

#![cfg(test)]

use std::cell::Cell;
use std::ffi::OsString;
use std::path::PathBuf;

use super::{Directory, unix};

const BASE_DIRS: [(Directory, &str, &str, &str); 3] = [
    (
        Directory::Cache,
        "XDG_CACHE_HOME",
        ".cache",
        "Library/Caches",
    ),
    (
        Directory::Config,
        "XDG_CONFIG_HOME",
        ".config",
        "Library/Application Support",
    ),
    (
        Directory::LocalData,
        "XDG_DATA_HOME",
        ".local/share",
        "Library/Application Support",
    ),
];

#[test]
fn xdg_overrides_are_absolute_and_do_not_need_a_home_lookup() {
    for (kind, key, _, _) in BASE_DIRS {
        for value in ["/cache", "/", "//server/cache", "/cache/../redirected"] {
            let actual = unix::directory(
                kind,
                false,
                |name| {
                    assert_eq!(name, key);
                    Some(value.into())
                },
                || panic!("absolute XDG paths must not query home"),
            );
            assert_eq!(actual, Some(PathBuf::from(value)));
        }
    }
}

#[test]
fn missing_empty_and_relative_xdg_values_fall_back_to_home() {
    for (kind, key, suffix, _) in BASE_DIRS {
        for value in [
            None,
            Some(""),
            Some("relative/cache"),
            Some("~/cache"),
            Some("C:\\cache"),
        ] {
            let actual = unix::directory(
                kind,
                false,
                |name| {
                    assert_eq!(name, key);
                    value.map(OsString::from)
                },
                || Some("home".into()),
            );
            assert_eq!(actual, Some(PathBuf::from("home").join(suffix)));
        }
    }
}

#[test]
fn home_lookup_accepts_nonempty_values_verbatim_and_ignores_xdg() {
    for apple in [false, true] {
        for home in ["relative", "/", "/home/user", " "] {
            assert_eq!(
                unix::directory(
                    Directory::Home,
                    apple,
                    |_| panic!("home must not query XDG variables"),
                    || Some(home.into()),
                ),
                Some(PathBuf::from(home))
            );
        }
    }
}

#[test]
fn empty_home_lookup_does_not_produce_relative_base_directories() {
    for apple in [false, true] {
        for kind in [
            Directory::Home,
            Directory::Cache,
            Directory::Config,
            Directory::LocalData,
        ] {
            assert_eq!(
                unix::directory(kind, apple, |_| None, || Some(PathBuf::new())),
                None
            );
        }
    }
}

#[test]
fn unavailable_home_propagates_failure() {
    for apple in [false, true] {
        let calls = Cell::new(0);
        assert_eq!(
            unix::directory(
                Directory::Home,
                apple,
                |_| panic!("home must not query XDG variables"),
                || {
                    calls.set(calls.get() + 1);
                    None
                },
            ),
            None
        );
        assert_eq!(calls.get(), 1);
    }
}

#[test]
fn base_directories_use_home_or_remain_unavailable() {
    for apple in [false, true] {
        for (kind, _, linux_suffix, apple_suffix) in BASE_DIRS {
            assert_eq!(unix::directory(kind, apple, |_| None, || None), None);
            assert_eq!(
                unix::directory(kind, apple, |_| None, || Some("home".into())),
                Some(PathBuf::from("home").join(if apple { apple_suffix } else { linux_suffix }))
            );
        }
    }
}

#[test]
fn macos_ignores_all_xdg_overrides_and_uses_application_support_for_config() {
    for (kind, _, _, suffix) in BASE_DIRS {
        assert_eq!(
            unix::directory(
                kind,
                true,
                |_| panic!("macOS must not query XDG variables"),
                || Some("home".into()),
            ),
            Some(PathBuf::from("home").join(suffix))
        );
    }
}

#[test]
fn non_ascii_paths_are_preserved_without_expansion_or_normalization() {
    let home = OsString::from("/home/\u{7528}\u{6237}/../user");
    assert_eq!(
        unix::directory(
            Directory::Home,
            false,
            |_| panic!("home must not query XDG variables"),
            || Some(PathBuf::from(&home)),
        ),
        Some(home.into())
    );
}

#[cfg(unix)]
#[test]
fn non_unicode_home_and_xdg_paths_are_preserved() {
    use std::os::unix::ffi::OsStringExt;

    let home = OsString::from_vec(b"/home/\xff".to_vec());
    let xdg = OsString::from_vec(b"/cache/\xfe".to_vec());
    for (kind, key, suffix, _) in BASE_DIRS {
        assert_eq!(
            unix::directory(
                kind,
                false,
                |name| (name == key).then(|| xdg.clone()),
                || panic!("absolute non-Unicode XDG override must be accepted"),
            ),
            Some(PathBuf::from(&xdg))
        );
        assert_eq!(
            unix::directory(kind, false, |_| None, || Some(PathBuf::from(&home)),),
            Some(PathBuf::from(&home).join(suffix))
        );
    }
}

#[cfg(unix)]
#[test]
fn public_unix_lookups_match_the_platform_policy() {
    for (kind, lookup) in [
        (Directory::Home, super::home_dir as fn() -> Option<PathBuf>),
        (Directory::Cache, super::cache_dir),
        (Directory::Config, super::config_dir),
        (Directory::LocalData, super::data_local_dir),
    ] {
        assert_eq!(
            lookup(),
            unix::directory(
                kind,
                cfg!(any(target_os = "macos", target_os = "ios")),
                |key| std::env::var_os(key),
                std::env::home_dir,
            )
        );
    }
}

#[cfg(unix)]
#[test]
fn actual_home_lookup_preserves_environment_and_passwd_fallback() {
    use std::os::unix::ffi::OsStringExt;

    const CHILD: &str = "COPILOT_PLATFORM_DIRS_PASSWD_TEST_CHILD";
    if std::env::var_os(CHILD).is_some() {
        let home = std::env::var_os("HOME")
            .filter(|home| !home.is_empty())
            .map(PathBuf::from)
            .or_else(|| std::env::home_dir().filter(|home| !home.as_os_str().is_empty()));
        assert_eq!(super::home_dir(), home);
        for ((_, _, linux_suffix, apple_suffix), lookup) in BASE_DIRS.into_iter().zip([
            super::cache_dir as fn() -> Option<PathBuf>,
            super::config_dir,
            super::data_local_dir,
        ]) {
            assert_eq!(
                lookup(),
                home.as_ref().map(|home| home.join(
                    if cfg!(any(target_os = "macos", target_os = "ios")) {
                        apple_suffix
                    } else {
                        linux_suffix
                    }
                ))
            );
        }
        return;
    }
    let current = std::thread::current();
    let test = current.name().expect("test name");
    for home in [
        None,
        Some(OsString::new()),
        Some("relative-home".into()),
        Some("/".into()),
        Some(" ".into()),
        Some(OsString::from_vec(b"/home/\xff".to_vec())),
    ] {
        let mut child = std::process::Command::new(std::env::current_exe().unwrap());
        child
            .current_dir(std::env::temp_dir())
            .args(["--exact", test, "--nocapture"])
            .env(CHILD, "1")
            .env_remove("HOME")
            .env_remove("XDG_CACHE_HOME")
            .env_remove("XDG_CONFIG_HOME")
            .env_remove("XDG_DATA_HOME");
        if let Some(home) = &home {
            child.env("HOME", home);
        }
        let output = child.output().expect("run isolated HOME lookup");
        assert!(
            output.status.success() && String::from_utf8_lossy(&output.stdout).contains("1 passed"),
            "HOME={home:?}: {}{}",
            String::from_utf8_lossy(&output.stdout),
            String::from_utf8_lossy(&output.stderr)
        );
    }
}
