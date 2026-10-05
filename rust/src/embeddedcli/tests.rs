/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use super::*;

#[cfg(all(has_bundled_cli, feature = "in-process"))]
#[test]
fn embedded_runtime_archive_contains_runtime_assets_and_excludes_cli() {
    let gz = flate2::read::GzDecoder::new(build_time::RUNTIME_ARCHIVE);
    let mut archive = tar::Archive::new(gz);
    let mut names: Vec<String> = archive
        .entries()
        .expect("archive entries")
        .map(|entry| {
            entry
                .expect("archive entry")
                .path()
                .expect("archive path")
                .to_string_lossy()
                .into_owned()
        })
        .collect();
    names.sort();

    assert!(names.contains(&RUNTIME_LIBRARY_NAME.to_string()));
    assert!(names.contains(&RUNTIME_BINARY_NAME.to_string()));
    assert!(names.contains(&RUNTIME_NODE_NAME.to_string()));
    assert!(names.iter().any(|name| name.starts_with("ripgrep/")));
    assert!(names.iter().any(|name| name.starts_with("definitions/")));
    assert!(!names.contains(&CLI_BINARY_NAME.to_string()));
    assert!(!names.contains(&"app.js".to_string()));
    assert!(!names.contains(&"cli-main.js".to_string()));
}

/// Bytes whose header looks like a valid executable image on the host
/// platform, so `looks_like_valid_image` accepts them. `extra` padding
/// bytes follow the magic so size checks have something to disagree about.
fn fake_image(extra: usize) -> Vec<u8> {
    let mut bytes = Vec::new();
    #[cfg(windows)]
    bytes.extend_from_slice(b"MZ\x90\x00");
    #[cfg(target_os = "macos")]
    bytes.extend_from_slice(&[0xfe, 0xed, 0xfa, 0xcf]);
    #[cfg(all(not(windows), not(target_os = "macos")))]
    bytes.extend_from_slice(b"\x7fELF");
    bytes.extend(std::iter::repeat_n(0xAB, extra));
    bytes
}

#[test]
fn publish_verified_writes_and_records_marker() {
    let dir = tempfile::tempdir().expect("tempdir");
    let final_path = dir.path().join("copilot-bin");
    let marker = marker_path(dir.path());
    let bytes = fake_image(2048);

    publish_verified(dir.path(), &final_path, &marker, &bytes).expect("publish");

    assert!(final_path.is_file(), "binary should be published");
    assert_eq!(fs::read(&final_path).expect("read"), bytes);
    assert_eq!(read_marker_len(&marker), Some(bytes.len() as u64));
    assert!(existing_install_is_valid(
        &final_path,
        &marker,
        bytes.len() as u64
    ));

    // No leftover temp files in the install dir.
    let leftovers: Vec<_> = fs::read_dir(dir.path())
        .expect("read_dir")
        .filter_map(|e| e.ok())
        .filter(|e| e.file_name().to_string_lossy().contains(".tmp."))
        .collect();
    assert!(leftovers.is_empty(), "temp files should be cleaned up");
}

#[test]
fn publish_overwrites_an_existing_binary() {
    let dir = tempfile::tempdir().expect("tempdir");
    let final_path = dir.path().join("copilot-bin");
    let marker = marker_path(dir.path());

    // Pre-existing (stale) binary at the destination.
    fs::write(&final_path, b"old contents").expect("seed");

    let bytes = fake_image(512);
    publish_verified(dir.path(), &final_path, &marker, &bytes).expect("publish");

    assert_eq!(fs::read(&final_path).expect("read"), bytes);
}

#[test]
fn corrupt_or_unmarked_install_is_rejected() {
    let dir = tempfile::tempdir().expect("tempdir");
    let final_path = dir.path().join("copilot-bin");
    let marker = marker_path(dir.path());
    let bytes = fake_image(4096);

    // Missing binary entirely.
    assert!(!existing_install_is_valid(&final_path, &marker, 1));

    // Valid binary but no marker (e.g. installed by an older SDK).
    fs::write(&final_path, &bytes).expect("write binary");
    assert!(
        !existing_install_is_valid(&final_path, &marker, bytes.len() as u64),
        "an install without a marker must not be trusted"
    );

    // Marker present but the binary was later truncated (partial write /
    // antivirus). Marker still records the original full size.
    write_marker(&marker, bytes.len() as u64).expect("marker");
    assert!(existing_install_is_valid(
        &final_path,
        &marker,
        bytes.len() as u64
    ));
    assert!(
        !existing_install_is_valid(&final_path, &marker, bytes.len() as u64 + 1),
        "a marker from the wrapper-as-CLI regression must not validate the full CLI"
    );
    fs::write(&final_path, &bytes[..bytes.len() / 2]).expect("truncate");
    assert!(
        !existing_install_is_valid(&final_path, &marker, bytes.len() as u64),
        "a truncated binary must be detected via the size marker"
    );

    // Zero-length binary (quarantined to empty).
    fs::write(&final_path, b"").expect("empty");
    assert!(!existing_install_is_valid(
        &final_path,
        &marker,
        bytes.len() as u64
    ));
}

#[test]
fn invalid_image_header_is_rejected() {
    let dir = tempfile::tempdir().expect("tempdir");
    let final_path = dir.path().join("copilot-bin");
    let marker = marker_path(dir.path());

    // Right size, has a marker, but the bytes are not a valid image.
    let garbage = vec![0u8; 4096];
    fs::write(&final_path, &garbage).expect("write garbage");
    write_marker(&marker, garbage.len() as u64).expect("marker");

    assert!(
        !existing_install_is_valid(&final_path, &marker, garbage.len() as u64),
        "a non-executable image must be rejected even with a matching marker"
    );
}

#[test]
fn verification_rejects_size_and_content_mismatch() {
    let dir = tempfile::tempdir().expect("tempdir");
    let path = dir.path().join("staged");
    let expected = fake_image(1024);

    // Exact match passes.
    fs::write(&path, &expected).expect("write");
    verify_on_disk_matches(&path, &expected).expect("exact match should verify");

    // Truncated -> size mismatch.
    fs::write(&path, &expected[..100]).expect("truncate");
    assert!(verify_on_disk_matches(&path, &expected).is_err());

    // Same length, different bytes -> content mismatch.
    let mut tampered = expected.clone();
    *tampered.last_mut().expect("non-empty") ^= 0xFF;
    fs::write(&path, &tampered).expect("tamper");
    assert!(verify_on_disk_matches(&path, &expected).is_err());

    // Missing file -> I/O error.
    fs::remove_file(&path).expect("remove");
    assert!(verify_on_disk_matches(&path, &expected).is_err());
}

#[test]
fn temp_files_are_unique_and_synced() {
    let dir = tempfile::tempdir().expect("tempdir");
    let data = fake_image(256);

    let a = write_temp_file(dir.path(), &data).expect("temp a");
    let b = write_temp_file(dir.path(), &data).expect("temp b");

    assert_ne!(a, b, "temp file names must be unique");
    assert_eq!(fs::read(&a).expect("read a"), data);
    assert_eq!(fs::read(&b).expect("read b"), data);

    #[cfg(unix)]
    {
        use std::os::unix::fs::PermissionsExt;
        let mode = fs::metadata(&a).expect("meta").permissions().mode();
        assert_eq!(mode & 0o777, 0o755, "temp binary should be executable");
    }
}

#[cfg(has_bundled_cli)]
#[test]
fn runtime_install_replaces_stale_pair() {
    let dir = tempfile::tempdir().expect("tempdir");
    fs::write(dir.path().join(RUNTIME_NODE_NAME), b"stale runtime").expect("seed runtime");
    fs::write(dir.path().join(RUNTIME_BINARY_NAME), b"stale wrapper").expect("seed wrapper");

    install_runtime(dir.path(), build_time::RUNTIME_ARCHIVE).expect("install runtime");

    assert_eq!(
        fs::read(dir.path().join(RUNTIME_NODE_NAME)).expect("read runtime"),
        extract_binary(build_time::RUNTIME_ARCHIVE, RUNTIME_NODE_NAME).expect("extract runtime")
    );
    assert_eq!(
        fs::read(dir.path().join(RUNTIME_BINARY_NAME)).expect("read wrapper"),
        extract_binary(build_time::RUNTIME_ARCHIVE, RUNTIME_BINARY_NAME).expect("extract wrapper")
    );
}

#[cfg(has_bundled_cli)]
#[test]
fn custom_runtime_install_dir_isolated_by_version() {
    let dir = tempfile::tempdir().expect("tempdir");

    assert_eq!(
        runtime_install_dir(dir.path(), "1.0.0").expect("claim directory"),
        dir.path()
    );
    assert_eq!(
        runtime_install_dir(dir.path(), "1.0.0").expect("reuse directory"),
        dir.path()
    );
    assert_eq!(
        runtime_install_dir(dir.path(), "2.0.0").expect("isolate directory"),
        dir.path().join("2.0.0")
    );
}

#[cfg(has_bundled_cli)]
fn runtime_fixture(extra: &[(&str, &[u8], u32)]) -> Vec<u8> {
    let encoder = flate2::write::GzEncoder::new(Vec::new(), flate2::Compression::fast());
    let mut archive = tar::Builder::new(encoder);
    let mut entries = vec![
        (RUNTIME_BINARY_NAME, b"wrapper".as_slice(), 0o755),
        (RUNTIME_NODE_NAME, b"runtime".as_slice(), 0o755),
    ];
    #[cfg(feature = "in-process")]
    entries.push((RUNTIME_LIBRARY_NAME, b"library".as_slice(), 0o644));
    entries.extend_from_slice(extra);
    for (name, bytes, mode) in entries {
        let mut header = tar::Header::new_gnu();
        // Raw names also let the installer see traversal fixtures which
        // Builder::append_data would reject before reaching product code.
        header.as_mut_bytes()[..name.len()].copy_from_slice(name.as_bytes());
        header.set_size(bytes.len() as u64);
        header.set_mode(mode);
        header.set_cksum();
        archive.append(&header, bytes).expect("append fixture");
    }
    archive.into_inner().unwrap().finish().unwrap()
}

#[cfg(has_bundled_cli)]
#[test]
fn warm_runtime_rejects_same_size_corruption_despite_unchanged_metadata() {
    let dir = tempfile::tempdir().unwrap();
    let fixture = runtime_fixture(&[]);
    install_runtime(dir.path(), &fixture).unwrap();
    let runtime = dir.path().join(RUNTIME_NODE_NAME);
    let original = fs::metadata(&runtime).unwrap();
    fs::write(&runtime, b"corrupt").unwrap();
    fs::File::options()
        .write(true)
        .open(&runtime)
        .unwrap()
        .set_times(fs::FileTimes::new().set_modified(original.modified().unwrap()))
        .unwrap();
    assert!(install_runtime(dir.path(), b"invalid archive").is_err());
    assert_eq!(fs::read(&runtime).unwrap(), b"corrupt");
    install_runtime(dir.path(), &fixture).unwrap();
    assert_eq!(fs::read(&runtime).unwrap(), b"runtime");
}

#[cfg(has_bundled_cli)]
fn assert_no_runtime_temps(dir: &Path) {
    for entry in fs::read_dir(dir).unwrap() {
        let entry = entry.unwrap();
        assert!(
            !entry
                .file_name()
                .to_string_lossy()
                .starts_with(".copilot-cli.tmp.")
        );
        if entry.file_type().unwrap().is_dir() {
            assert_no_runtime_temps(&entry.path());
        }
    }
}

#[cfg(has_bundled_cli)]
#[test]
fn runtime_cold_install_and_warm_reuse_preserve_bytes_and_modes() {
    let dir = tempfile::tempdir().unwrap();
    let data = vec![0xAB; 3 * 64 * 1024 + 17];
    let archive = runtime_fixture(&[("nested/asset", &data, 0o640)]);
    let wrapper = install_runtime(dir.path(), &archive).unwrap();
    assert_eq!(wrapper, dir.path().join(RUNTIME_BINARY_NAME));
    let asset = dir.path().join("nested/asset");
    assert_eq!(fs::read(&asset).unwrap(), data);
    let modified = std::time::UNIX_EPOCH + std::time::Duration::from_secs(1234567890);
    fs::File::options()
        .write(true)
        .open(&asset)
        .unwrap()
        .set_times(fs::FileTimes::new().set_modified(modified))
        .unwrap();

    install_runtime(dir.path(), &archive).unwrap();

    assert_eq!(fs::metadata(&asset).unwrap().modified().unwrap(), modified);
    assert_eq!(fs::read(&asset).unwrap(), data);
    #[cfg(unix)]
    {
        use std::os::unix::fs::PermissionsExt;
        assert_eq!(
            fs::metadata(&asset).unwrap().permissions().mode() & 0o777,
            0o640
        );
        assert_eq!(
            fs::metadata(wrapper).unwrap().permissions().mode() & 0o777,
            0o755
        );
    }
    assert_no_runtime_temps(dir.path());
}

#[cfg(has_bundled_cli)]
#[test]
fn runtime_repairs_same_size_corruption_truncation_and_extra_bytes() {
    let dir = tempfile::tempdir().unwrap();
    let archive = runtime_fixture(&[]);
    let runtime = dir.path().join(RUNTIME_NODE_NAME);
    install_runtime(dir.path(), &archive).unwrap();

    for corrupt in [b"runtimX".as_slice(), b"run", b"", b"runtime plus garbage"] {
        fs::write(&runtime, corrupt).unwrap();
        install_runtime(dir.path(), &archive).unwrap();
        assert_eq!(fs::read(&runtime).unwrap(), b"runtime");
        assert_no_runtime_temps(dir.path());
    }
}

#[cfg(has_bundled_cli)]
#[test]
fn runtime_repairs_corruption_across_comparison_chunks() {
    let dir = tempfile::tempdir().unwrap();
    let bytes = vec![0xAB; 3 * 64 * 1024 + 17];
    let archive = runtime_fixture(&[("nested/asset", &bytes, 0o644)]);
    install_runtime(dir.path(), &archive).unwrap();
    for offset in [0, bytes.len() / 2, bytes.len() - 1] {
        let mut corrupt = bytes.clone();
        corrupt[offset] ^= 1;
        fs::write(dir.path().join("nested/asset"), &corrupt).unwrap();
        install_runtime(dir.path(), &archive).unwrap();
        assert_eq!(fs::read(dir.path().join("nested/asset")).unwrap(), bytes);
        assert_no_runtime_temps(dir.path());
    }
}

#[cfg(all(has_bundled_cli, unix))]
#[test]
fn runtime_reuse_preserves_executable_caller_selected_permissions() {
    use std::os::unix::fs::PermissionsExt;

    let dir = tempfile::tempdir().unwrap();
    let fixture = runtime_fixture(&[]);
    let wrapper = install_runtime(dir.path(), &fixture).unwrap();
    for mode in [0o745, 0o754, 0o700, 0o500] {
        fs::set_permissions(&wrapper, fs::Permissions::from_mode(mode)).unwrap();
        install_runtime(dir.path(), &fixture).unwrap();
        assert_eq!(
            fs::metadata(&wrapper).unwrap().permissions().mode() & 0o777,
            mode
        );
    }
    assert_no_runtime_temps(dir.path());
}

#[cfg(all(has_bundled_cli, unix))]
#[test]
fn runtime_repairs_nonexecutable_wrapper() {
    use std::os::unix::fs::PermissionsExt;

    let dir = tempfile::tempdir().unwrap();
    let archive = runtime_fixture(&[]);
    let wrapper = install_runtime(dir.path(), &archive).unwrap();
    for mode in [0o400, 0o600] {
        fs::set_permissions(&wrapper, fs::Permissions::from_mode(mode)).unwrap();
        install_runtime(dir.path(), &archive).unwrap();
        assert_eq!(
            fs::metadata(&wrapper).unwrap().permissions().mode() & 0o777,
            0o755
        );
        assert_eq!(fs::read(&wrapper).unwrap(), b"wrapper");
        assert_no_runtime_temps(dir.path());
    }
}

#[cfg(all(has_bundled_cli, unix))]
#[test]
fn runtime_rejects_nonexecutable_wrapper_in_readonly_cache() {
    use std::os::unix::fs::PermissionsExt;

    let dir = tempfile::tempdir().unwrap();
    let archive = runtime_fixture(&[]);
    let wrapper = install_runtime(dir.path(), &archive).unwrap();
    fs::set_permissions(&wrapper, fs::Permissions::from_mode(0o400)).unwrap();
    fs::set_permissions(dir.path(), fs::Permissions::from_mode(0o555)).unwrap();
    let write_denied = fs::File::create(dir.path().join("write-probe")).is_err();
    let result = install_runtime(dir.path(), &archive);
    fs::set_permissions(dir.path(), fs::Permissions::from_mode(0o755)).unwrap();
    if write_denied {
        assert!(
            result.is_err(),
            "returned a nonexecutable wrapper: {result:?}"
        );
        assert_eq!(
            fs::metadata(&wrapper).unwrap().permissions().mode() & 0o777,
            0o400
        );
    } else {
        eprintln!("read-only permission enforcement unavailable (e.g. privileged user)");
        fs::remove_file(dir.path().join("write-probe")).unwrap();
        result.unwrap();
        assert_eq!(
            fs::metadata(&wrapper).unwrap().permissions().mode() & 0o777,
            0o755
        );
    }
    assert_no_runtime_temps(dir.path());
}

#[cfg(has_bundled_cli)]
#[test]
fn runtime_archive_errors_do_not_publish_and_clean_up_staging() {
    let dir = tempfile::tempdir().unwrap();
    let valid = runtime_fixture(&[("nested/asset", b"asset", 0o644)]);
    let mut invalid_crc = valid.clone();
    let crc = invalid_crc.len() - 8;
    invalid_crc[crc] ^= 0xFF;
    let mut invalid_length = valid.clone();
    let length = invalid_length.len() - 4;
    invalid_length[length] ^= 0xFF;
    let mut truncated_trailer = valid.clone();
    truncated_trailer.truncate(valid.len() - 1);
    let mut truncated_body = valid.clone();
    truncated_body.truncate(valid.len() / 2);
    for archive in [
        invalid_crc,
        invalid_length,
        truncated_trailer,
        truncated_body,
    ] {
        let runtime = dir.path().join(RUNTIME_NODE_NAME);
        fs::write(&runtime, b"previous complete runtime").unwrap();
        assert!(install_runtime(dir.path(), &archive).is_err());
        assert_eq!(fs::read(runtime).unwrap(), b"previous complete runtime");
        assert!(!dir.path().join(RUNTIME_BINARY_NAME).exists());
        assert!(!dir.path().join("nested/asset").exists());
        assert_no_runtime_temps(dir.path());
    }
}

#[cfg(has_bundled_cli)]
#[test]
fn runtime_short_entry_is_rejected_without_publishing() {
    let dir = tempfile::tempdir().unwrap();
    let mut header = tar::Header::new_gnu();
    header.set_path(RUNTIME_NODE_NAME).unwrap();
    header.set_size(128 * 1024);
    header.set_mode(0o755);
    header.set_cksum();
    let mut encoder = flate2::write::GzEncoder::new(Vec::new(), flate2::Compression::fast());
    encoder.write_all(header.as_bytes()).unwrap();
    encoder.write_all(b"short").unwrap();
    let archive = encoder.finish().unwrap();

    assert!(install_runtime(dir.path(), &archive).is_err());
    assert!(!dir.path().join(RUNTIME_NODE_NAME).exists());
    assert_no_runtime_temps(dir.path());
}

#[cfg(has_bundled_cli)]
#[test]
fn runtime_missing_required_entry_is_rejected_before_publish() {
    let dir = tempfile::tempdir().unwrap();
    let encoder = flate2::write::GzEncoder::new(Vec::new(), flate2::Compression::fast());
    let mut archive = tar::Builder::new(encoder);
    let mut header = tar::Header::new_gnu();
    header.set_size(7);
    header.set_mode(0o755);
    header.set_cksum();
    archive
        .append_data(&mut header, RUNTIME_BINARY_NAME, b"wrapper".as_slice())
        .unwrap();
    let archive = archive.into_inner().unwrap().finish().unwrap();

    assert!(install_runtime(dir.path(), &archive).is_err());
    assert!(!dir.path().join(RUNTIME_BINARY_NAME).exists());
    assert_no_runtime_temps(dir.path());
}

#[cfg(has_bundled_cli)]
#[test]
fn runtime_rejects_traversal_and_cleans_preceding_entries() {
    let dir = tempfile::tempdir().unwrap();
    let install_dir = dir.path().join("install");
    let archive = runtime_fixture(&[("../escaped", b"bad", 0o644)]);
    assert!(install_runtime(&install_dir, &archive).is_err());
    assert!(!dir.path().join("escaped").exists());
    assert!(!install_dir.join(RUNTIME_BINARY_NAME).exists());
    assert_no_runtime_temps(&install_dir);
}

#[cfg(all(has_bundled_cli, unix))]
#[test]
fn runtime_rejects_symlink_parents_and_targets() {
    use std::os::unix::fs::symlink;

    let dir = tempfile::tempdir().unwrap();
    let outside = tempfile::tempdir().unwrap();
    fs::write(outside.path().join("asset"), b"outside").unwrap();
    symlink(outside.path(), dir.path().join("nested")).unwrap();
    let archive = runtime_fixture(&[("nested/asset", b"new", 0o644)]);

    assert!(install_runtime(dir.path(), &archive).is_err());
    assert_eq!(fs::read(outside.path().join("asset")).unwrap(), b"outside");
    assert_no_runtime_temps(dir.path());
    fs::remove_file(dir.path().join("nested")).unwrap();
    symlink(
        outside.path().join("asset"),
        dir.path().join(RUNTIME_NODE_NAME),
    )
    .unwrap();
    assert!(install_runtime(dir.path(), &archive).is_err());
    assert_eq!(fs::read(outside.path().join("asset")).unwrap(), b"outside");
    assert_no_runtime_temps(dir.path());
}

#[cfg(has_bundled_cli)]
#[test]
fn concurrent_runtime_installers_publish_complete_files() {
    let dir = tempfile::tempdir().unwrap();
    let data = vec![0xAB; 256 * 1024 + 1];
    let archive = runtime_fixture(&[("nested/asset", &data, 0o644)]);
    let barrier = std::sync::Barrier::new(6);
    std::thread::scope(|scope| {
        for _ in 0..6 {
            scope.spawn(|| {
                barrier.wait();
                install_runtime(dir.path(), &archive).unwrap();
            });
        }
    });
    assert_eq!(fs::read(dir.path().join("nested/asset")).unwrap(), data);
    assert_eq!(
        fs::read(dir.path().join(RUNTIME_NODE_NAME)).unwrap(),
        b"runtime"
    );
    assert_no_runtime_temps(dir.path());
}

#[cfg(has_bundled_cli)]
#[test]
fn runtime_duplicate_destinations_fail_on_cold_and_warm_installs() {
    for name in ["asset", "./asset"] {
        let dir = tempfile::tempdir().unwrap();
        let archive = runtime_fixture(&[("asset", b"first", 0o644), (name, b"last", 0o644)]);
        assert!(install_runtime(dir.path(), &archive).is_err());
        assert!(!dir.path().join("asset").exists());
        assert_no_runtime_temps(dir.path());

        fs::write(dir.path().join("asset"), b"last").unwrap();
        assert!(install_runtime(dir.path(), &archive).is_err());
        assert_eq!(fs::read(dir.path().join("asset")).unwrap(), b"last");
        assert_no_runtime_temps(dir.path());
    }
    let dir = tempfile::tempdir().unwrap();
    let archive = runtime_fixture(&[(RUNTIME_NODE_NAME, b"", 0o755)]);
    assert!(install_runtime(dir.path(), &archive).is_err());
    assert!(!dir.path().join(RUNTIME_NODE_NAME).exists());
    assert_no_runtime_temps(dir.path());
    install_runtime(dir.path(), &runtime_fixture(&[])).unwrap();
    assert!(install_runtime(dir.path(), &archive).is_err());
    assert_eq!(
        fs::read(dir.path().join(RUNTIME_NODE_NAME)).unwrap(),
        b"runtime"
    );
    assert_no_runtime_temps(dir.path());
}

#[cfg(has_bundled_cli)]
#[test]
fn runtime_case_aliases_cannot_overwrite_required_artifacts() {
    let names = [
        RUNTIME_NODE_NAME,
        RUNTIME_BINARY_NAME,
        #[cfg(feature = "in-process")]
        RUNTIME_LIBRARY_NAME,
    ];
    for name in names {
        let alias = name.to_ascii_uppercase();
        let archive = runtime_fixture(&[(&alias, b"", 0o755)]);
        let dir = tempfile::tempdir().unwrap();
        let result = install_runtime(dir.path(), &archive);
        assert!(result.is_err(), "accepted alias {alias}: {result:?}");
        assert!(!dir.path().join(name).exists());
        assert_no_runtime_temps(dir.path());

        install_runtime(dir.path(), &runtime_fixture(&[])).unwrap();
        let original = fs::read(dir.path().join(name)).unwrap();
        assert!(install_runtime(dir.path(), &archive).is_err());
        assert_eq!(fs::read(dir.path().join(name)).unwrap(), original);
        fs::write(dir.path().join(name), b"corrupt").unwrap();
        assert!(install_runtime(dir.path(), &archive).is_err());
        assert_eq!(fs::read(dir.path().join(name)).unwrap(), b"corrupt");
        assert_no_runtime_temps(dir.path());
    }
}

#[cfg(has_bundled_cli)]
#[test]
fn runtime_mixed_separator_and_case_aliases_are_rejected() {
    let dir = tempfile::tempdir().unwrap();
    let archive = runtime_fixture(&[
        ("nested/asset", b"first", 0o644),
        (r".\NESTED\ASSET", b"last", 0o644),
    ]);
    assert!(install_runtime(dir.path(), &archive).is_err());
    assert!(!dir.path().join("nested/asset").exists());
    assert_no_runtime_temps(dir.path());

    install_runtime(
        dir.path(),
        &runtime_fixture(&[("nested/asset", b"last", 0o644)]),
    )
    .unwrap();
    assert!(install_runtime(dir.path(), &archive).is_err());
    assert_eq!(fs::read(dir.path().join("nested/asset")).unwrap(), b"last");
    assert_no_runtime_temps(dir.path());
}

#[cfg(has_bundled_cli)]
#[test]
fn runtime_nonportable_alias_paths_are_rejected_before_publish() {
    for name in [
        "runtime.node.",
        "runtime.node ",
        "runtime.node:stream",
        "RUNTIM~1.NOD",
        "NUL",
        "con.txt",
        "aux .txt",
        "COM1",
        "LPT9.txt",
        "n\u{00e9}sted/asset",
        r"..\escaped",
        r"C:\escaped",
        r"\\server\share\asset",
    ] {
        let dir = tempfile::tempdir().unwrap();
        let archive = runtime_fixture(&[(name, b"bad", 0o644)]);
        assert!(
            install_runtime(dir.path(), &archive).is_err(),
            "accepted {name}"
        );
        assert!(!dir.path().join(RUNTIME_NODE_NAME).exists());
        assert_no_runtime_temps(dir.path());
    }
}

#[cfg(all(has_bundled_cli, feature = "in-process"))]
#[test]
fn runtime_library_aliases_are_rejected_before_repair() {
    let alias = format!("./{RUNTIME_LIBRARY_NAME}");
    for duplicate in [b"another".as_slice(), b""] {
        let dir = tempfile::tempdir().unwrap();
        let archive = runtime_fixture(&[(&alias, duplicate, 0o644)]);
        assert!(install_runtime(dir.path(), &archive).is_err());
        assert!(!dir.path().join(RUNTIME_LIBRARY_NAME).exists());
        assert_no_runtime_temps(dir.path());

        install_runtime(dir.path(), &runtime_fixture(&[])).unwrap();
        let library = dir.path().join(RUNTIME_LIBRARY_NAME);
        assert!(install_runtime(dir.path(), &archive).is_err());
        assert_eq!(fs::read(&library).unwrap(), b"library");
        fs::write(&library, b"corrupt").unwrap();
        assert!(install_runtime(dir.path(), &archive).is_err());
        assert_eq!(fs::read(&library).unwrap(), b"corrupt");
        assert_no_runtime_temps(dir.path());
    }
}

#[cfg(all(has_bundled_cli, unix))]
#[test]
fn warm_runtime_install_needs_no_writable_cache() {
    assert_read_only_runtime_cache(0o555, false);
}

#[cfg(all(has_bundled_cli, unix))]
#[test]
fn warm_runtime_reuses_owner_only_immutable_cache() {
    assert_read_only_runtime_cache(0o500, false);
}

#[cfg(all(has_bundled_cli, unix))]
#[test]
fn warm_runtime_reuses_nonexecutable_native_library() {
    assert_read_only_runtime_cache(0o555, true);
    assert_read_only_runtime_cache(0o500, true);
}

#[cfg(all(has_bundled_cli, unix))]
fn assert_read_only_runtime_cache(permission_mask: u32, readonly_native_library: bool) {
    use std::os::unix::fs::PermissionsExt;

    let dir = tempfile::tempdir().unwrap();
    let archive = runtime_fixture(&[("nested/asset", b"asset", 0o644)]);
    install_runtime(dir.path(), &archive).unwrap();
    let files = [
        RUNTIME_BINARY_NAME,
        RUNTIME_NODE_NAME,
        "nested/asset",
        #[cfg(feature = "in-process")]
        RUNTIME_LIBRARY_NAME,
    ];
    let mut read_only_files = Vec::new();
    for name in files {
        let path = dir.path().join(name);
        let mut mode = fs::metadata(&path).unwrap().permissions().mode() & permission_mask;
        if readonly_native_library && name != RUNTIME_BINARY_NAME {
            mode &= !0o111;
        }
        fs::set_permissions(&path, fs::Permissions::from_mode(mode)).unwrap();
        read_only_files.push((path, mode));
    }
    fs::set_permissions(
        dir.path().join("nested"),
        fs::Permissions::from_mode(permission_mask),
    )
    .unwrap();
    fs::set_permissions(dir.path(), fs::Permissions::from_mode(permission_mask)).unwrap();
    let write_denied = fs::File::create(dir.path().join("write-probe")).is_err();
    let result = install_runtime(dir.path(), &archive);
    fs::set_permissions(dir.path(), fs::Permissions::from_mode(0o755)).unwrap();
    fs::set_permissions(dir.path().join("nested"), fs::Permissions::from_mode(0o755)).unwrap();
    result.unwrap();
    if !write_denied {
        eprintln!("read-only permission enforcement unavailable (e.g. privileged user)");
        fs::remove_file(dir.path().join("write-probe")).unwrap();
    }
    for (path, mode) in read_only_files {
        assert_eq!(
            fs::metadata(path).unwrap().permissions().mode() & 0o777,
            mode
        );
    }
    assert_eq!(fs::read(dir.path().join("nested/asset")).unwrap(), b"asset");
    assert_no_runtime_temps(dir.path());
}

#[cfg(all(has_bundled_cli, unix))]
#[test]
fn runtime_repairs_unreadable_but_replaceable_file() {
    use std::os::unix::fs::PermissionsExt;

    let dir = tempfile::tempdir().unwrap();
    let archive = runtime_fixture(&[("asset", b"trusted", 0o000)]);
    let asset = dir.path().join("asset");
    fs::write(&asset, b"corrupt").unwrap();
    fs::set_permissions(&asset, fs::Permissions::from_mode(0o000)).unwrap();
    if fs::File::open(&asset).is_ok() {
        eprintln!("unreadable permission enforcement unavailable (e.g. privileged user)");
    }
    let result = install_runtime(dir.path(), &archive);
    let mode = fs::metadata(&asset).unwrap().permissions().mode() & 0o777;
    fs::set_permissions(&asset, fs::Permissions::from_mode(0o600)).unwrap();
    result.unwrap();
    assert_eq!(mode, 0o000);
    assert_eq!(fs::read(asset).unwrap(), b"trusted");
    assert_no_runtime_temps(dir.path());
}

#[cfg(has_bundled_cli)]
#[test]
fn runtime_replacement_preserves_open_reader_contents() {
    let dir = tempfile::tempdir().unwrap();
    let asset = dir.path().join("asset");
    let original = runtime_fixture(&[("asset", b"old complete file", 0o644)]);
    install_runtime(dir.path(), &original).unwrap();
    let mut reader = fs::File::open(&asset).unwrap();
    let replacement = runtime_fixture(&[("asset", b"new complete file", 0o644)]);
    install_runtime(dir.path(), &replacement).unwrap();
    let mut bytes = Vec::new();
    reader.read_to_end(&mut bytes).unwrap();
    assert_eq!(bytes, b"old complete file");
    assert_eq!(fs::read(&asset).unwrap(), b"new complete file");
    assert_no_runtime_temps(dir.path());
}

#[test]
fn failed_publish_does_not_remove_previous_file() {
    let dir = tempfile::tempdir().unwrap();
    let target = dir.path().join("installed");
    fs::write(&target, b"previous complete file").unwrap();
    assert!(publish(&dir.path().join("missing-temporary"), &target).is_err());
    assert_eq!(fs::read(target).unwrap(), b"previous complete file");
}
