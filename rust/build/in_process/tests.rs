// Copyright (c) Microsoft Corporation. All rights reserved.
#![cfg(test)]

use std::fs;
use std::io::{Cursor, Write};
use std::path::{Path, PathBuf};
use std::process::{Command, Output};

use sha2::{Digest, Sha256};
use tempfile::TempDir;

use super::*;

const VERSION: &str = "1.2.3-unstable.20260923";
const HASH: &str = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
const PLATFORMS: &[&str] = &[
    "darwin-arm64",
    "darwin-x64",
    "linux-arm64",
    "linux-x64",
    "linuxmusl-arm64",
    "linuxmusl-x64",
    "win32-arm64",
    "win32-x64",
];

fn release(sdk: bool) -> Release {
    Release::new(
        VERSION.into(),
        sdk.then_some(format!(
            "https://github.com/github/copilot-sdk/releases/download/runtime-{VERSION}"
        ))
        .as_deref(),
    )
    .unwrap()
}

#[test]
fn legacy_and_unstable_snapshots_resolve_every_asset_and_checksums_consistently() {
    for sdk in [false, true] {
        let release = release(sdk);
        let base = if sdk {
            format!("https://github.com/github/copilot-sdk/releases/download/runtime-{VERSION}")
        } else {
            format!("https://github.com/github/copilot-cli/releases/download/v{VERSION}")
        };
        let metadata = if sdk {
            format!("release-url={base}\r\n")
        } else {
            String::new()
        };
        assert_eq!(
            release.asset_url("SHA256SUMS.txt"),
            format!("{base}/SHA256SUMS.txt")
        );
        for target in PLATFORMS {
            let extension = if target.starts_with("win32") {
                "zip"
            } else {
                "tar.gz"
            };
            let cli_asset = format!("copilot-{target}.{extension}");
            let runtime_asset = format!("github-copilot-{VERSION}-{target}.tgz");
            for (key, asset) in [
                (cli_asset.clone(), cli_asset),
                (format!("copilot-{target}"), runtime_asset),
            ] {
                let snapshot = format!(
                    "# fixture\r\n\r\nversion={VERSION}\r\n{metadata}{key}={}\r\n",
                    HASH.to_ascii_uppercase()
                );
                let (parsed, hash) = parse_snapshot(&snapshot, &key).unwrap();
                assert_eq!(parsed, release);
                assert_eq!(hash, HASH);
                assert_eq!(parsed.asset_url(&asset), format!("{base}/{asset}"));
                let expected_key = if sdk {
                    format!("copilot-sdk-runtime-{VERSION}-{asset}")
                } else {
                    format!("v{VERSION}-{asset}")
                };
                assert_eq!(parsed.cache_key(&asset), expected_key);
                assert_eq!(
                    find_sha256_for_asset(&format!("{HASH} *{asset}\r\n"), &asset),
                    HASH
                );
            }
        }
    }
}

#[test]
fn explicit_legacy_url_and_omitted_url_have_the_same_identity() {
    for version in [
        "1.2.3",
        "1.2.3-4",
        "0.0.0-dev",
        "1.2.3-unstable.r123.gabcdef0",
        "1.2.3-4.unstable.r123.gabcdef0",
    ] {
        let implicit = Release::new(version.into(), None).unwrap();
        let explicit = Release::new(
            version.into(),
            Some(&format!(
                "https://github.com/github/copilot-cli/releases/download/v{version}"
            )),
        )
        .unwrap();
        assert_eq!(implicit, explicit);
        assert_eq!(implicit.cache_identity(), version);
    }
    assert_ne!(
        release(false).cache_identity(),
        release(true).cache_identity()
    );
}

#[test]
fn source_checkouts_without_snapshots_resolve_canonical_unstable_releases() {
    let root = fixture_dir();
    let manifest_dir = root.path().join("rust");
    let node_dir = root.path().join("nodejs");
    fs::create_dir_all(&manifest_dir).unwrap();
    fs::create_dir_all(&node_dir).unwrap();
    assert!(!manifest_dir.join("cli-version.txt").exists());
    assert!(!manifest_dir.join("cli-version-in-process.txt").exists());

    for (version, sdk_release) in [
        ("1.2.3", false),
        ("1.2.3-4", false),
        ("0.0.0-dev", false),
        ("1.2.3-unstable.r123.gabcdef0", true),
        ("1.2.3-4.unstable.r123.gabcdef0", true),
        ("0.0.0-0.unstable.r1.g0000000", true),
        ("1.2.3-unstable.20260923", false),
        ("1.2.3.unstable.r123.gabcdef0", false),
        ("1.2.3-unstable.r0.gabcdef0", false),
        ("1.2.3-unstable.r0123.gabcdef0", false),
        ("01.2.3-unstable.r123.gabcdef0", false),
        ("1.2.3-04.unstable.r123.gabcdef0", false),
        ("1.2.3-unstable.r123.gabcdef00", false),
        ("1.2.3-unstable.r123.gABCDEF0", false),
    ] {
        fs::write(
            node_dir.join("package.json"),
            serde_json::json!({ "copilotCliVersion": version }).to_string(),
        )
        .unwrap();
        let (release, hash) = resolve_version_and_optional_hash(&manifest_dir, "copilot-linux-x64");
        assert_eq!(release.version, version);
        assert_eq!(hash, None);
        let base = if sdk_release {
            format!("https://github.com/github/copilot-sdk/releases/download/runtime-{version}")
        } else {
            format!("https://github.com/github/copilot-cli/releases/download/v{version}")
        };
        for asset in [
            "SHA256SUMS.txt".into(),
            "copilot-linux-x64.tar.gz".into(),
            format!("github-copilot-{version}-linux-x64.tgz"),
        ] {
            assert_eq!(release.asset_url(&asset), format!("{base}/{asset}"));
            let key = if sdk_release {
                format!("copilot-sdk-runtime-{version}-{asset}")
            } else {
                format!("v{version}-{asset}")
            };
            assert_eq!(release.cache_key(&asset), key);
        }
    }
}

#[test]
fn source_checkouts_still_prefer_snapshots_and_preserve_legacy_locations() {
    let root = fixture_dir();
    let manifest_dir = root.path().join("rust");
    let node_dir = root.path().join("nodejs");
    fs::create_dir_all(&manifest_dir).unwrap();
    fs::create_dir_all(&node_dir).unwrap();
    fs::write(
        node_dir.join("package.json"),
        r#"{"copilotCliVersion":"9.9.9"}"#,
    )
    .unwrap();
    let version = "1.2.3-4.unstable.r123.gabcdef0";
    for sdk_release in [false, true] {
        let base = if sdk_release {
            format!("https://github.com/github/copilot-sdk/releases/download/runtime-{version}")
        } else {
            format!("https://github.com/github/copilot-cli/releases/download/v{version}")
        };
        let location = if sdk_release {
            format!("release-url={base}\n")
        } else {
            String::new()
        };
        fs::write(
            manifest_dir.join("cli-version-in-process.txt"),
            format!("version={version}\n{location}copilot-linux-x64={HASH}\n"),
        )
        .unwrap();
        let (release, hash) = resolve_version_and_optional_hash(&manifest_dir, "copilot-linux-x64");
        assert_eq!(release.version, version);
        assert_eq!(hash.as_deref(), Some(HASH));
        assert_eq!(
            release.asset_url("SHA256SUMS.txt"),
            format!("{base}/SHA256SUMS.txt")
        );
    }
}

#[test]
fn snapshots_reject_mismatched_versions_untrusted_locations_and_invalid_hashes() {
    for url in [
        "https://github.com/github/copilot-sdk/releases/download/runtime-9.9.9",
        "https://github.com/github/copilot-sdk/releases/latest",
        "https://github.com/github/copilot-agent-runtime/releases/download/runtime-1.2.3",
        "https://token@github.com/github/copilot-sdk/releases/download/runtime-1.2.3",
        "https://github.com/github/copilot-cli/releases/download/v1.2.3/",
        "https://github.com/github/copilot-cli/releases/download/v1.2.3?token=secret",
        "http://github.com/github/copilot-cli/releases/download/v1.2.3",
    ] {
        let snapshot = format!("version=1.2.3\nrelease-url={url}\nasset={HASH}");
        assert!(parse_snapshot(&snapshot, "asset").is_err(), "{url}");
    }
    for snapshot in [
        format!("version=\nasset={HASH}"),
        format!("version=../1.2.3\nasset={HASH}"),
        format!("version=1.2.3\nversion=1.2.4\nasset={HASH}"),
        format!("version=1.2.3\nasset={HASH}\nasset={HASH}"),
        format!("version=1.2.3\nrelease-url=\nrelease-url=\nasset={HASH}"),
        format!("version=1.2.3\nother={HASH}"),
        "version=1.2.3\nasset=bad-hash".into(),
        format!("version=1.2.3\nmalformed line\nasset={HASH}"),
    ] {
        assert!(parse_snapshot(&snapshot, "asset").is_err(), "{snapshot}");
    }
}

#[test]
fn checksum_lookup_rejects_missing_duplicate_and_invalid_entries() {
    for sums in [
        format!("{HASH} asset-other\n"),
        format!("{HASH} asset\n{HASH} *asset\n"),
        "invalid asset\n".into(),
    ] {
        assert!(std::panic::catch_unwind(|| find_sha256_for_asset(&sums, "asset")).is_err());
    }
}

fn fixture_dir() -> TempDir {
    let root = Path::new(env!("CARGO_MANIFEST_DIR"))
        .join("target")
        .join("acquisition-tests");
    fs::create_dir_all(&root).unwrap();
    tempfile::Builder::new()
        .prefix("fixture-")
        .tempdir_in(root)
        .unwrap()
}

#[test]
fn cache_hits_are_verified_and_corruption_is_evicted() {
    let fixture = fixture_dir();
    let bytes = b"verified release archive";
    let hash = format!("{:x}", Sha256::digest(bytes));
    let path = fixture.path().join(release(true).cache_key("asset"));
    assert_eq!(read_verified_cache(&path, &hash), None);
    fs::write(&path, bytes).unwrap();
    assert_eq!(
        read_verified_cache(&path, &hash).as_deref(),
        Some(bytes.as_slice())
    );
    fs::write(&path, b"corrupt release archive").unwrap();
    assert_eq!(read_verified_cache(&path, &hash), None);
    assert!(!path.exists());
}

fn tar_archive(files: &[(&str, &[u8])]) -> Vec<u8> {
    let encoder = flate2::write::GzEncoder::new(Vec::new(), flate2::Compression::default());
    let mut archive = tar::Builder::new(encoder);
    for (name, bytes) in files {
        append_archive_file(&mut archive, name, bytes, 0o755);
    }
    archive.into_inner().unwrap().finish().unwrap()
}

struct Fixture {
    dir: TempDir,
    platform: Platform,
    release: Release,
}

impl Fixture {
    fn new(sdk: bool, windows: bool) -> Self {
        let fixture = Self {
            dir: fixture_dir(),
            platform: Platform {
                package_name: if windows {
                    "copilot-win32-x64"
                } else {
                    "copilot-linux-x64"
                },
                binary_name: if windows { "copilot.exe" } else { "copilot" },
            },
            release: release(sdk),
        };
        for name in ["out", "cache", "extracted"] {
            fs::create_dir_all(fixture.path(name)).unwrap();
        }
        let cli = if windows {
            let mut archive = zip::ZipWriter::new(Cursor::new(Vec::new()));
            archive
                .start_file("copilot.exe", zip::write::SimpleFileOptions::default())
                .unwrap();
            archive.write_all(b"MZ fixture CLI").unwrap();
            archive.finish().unwrap().into_inner()
        } else {
            tar_archive(&[("copilot", b"fixture CLI")])
        };
        let target = fixture
            .platform
            .package_name
            .strip_prefix("copilot-")
            .unwrap();
        let runtime = tar_archive(&[
            (
                &format!("package/{}", fixture.platform.runtime_wrapper_name()),
                b"fixture wrapper",
            ),
            (
                &format!("package/prebuilds/{target}/runtime.node"),
                b"fixture native library",
            ),
            ("package/tls/roots.pem", b"fixture support file"),
        ]);
        for (snapshot, key, asset, bytes) in [
            (
                "cli-version.txt",
                fixture.platform.cli_asset_name(),
                fixture.platform.cli_asset_name(),
                cli,
            ),
            (
                "cli-version-in-process.txt",
                fixture.platform.package_name.into(),
                format!("github-copilot-{VERSION}-{target}.tgz"),
                runtime,
            ),
        ] {
            let location = if sdk {
                format!(
                    "release-url=https://github.com/github/copilot-sdk/releases/download/runtime-{VERSION}\n"
                )
            } else {
                String::new()
            };
            fs::write(
                fixture.path(snapshot),
                format!(
                    "version={VERSION}\n{location}{key}={:x}\n",
                    Sha256::digest(&bytes)
                ),
            )
            .unwrap();
            fs::write(
                fixture
                    .path("cache")
                    .join(fixture.release.cache_key(&asset)),
                bytes,
            )
            .unwrap();
        }
        fixture
    }

    fn path(&self, path: &str) -> PathBuf {
        self.dir.path().join(path)
    }

    fn command(&self, bundled: bool, in_process: bool) -> Command {
        let mut command = Command::new(std::env::current_exe().unwrap());
        command
            .args([
                "--exact",
                "implementation::tests::run_build_script",
                "--nocapture",
            ])
            .env("COPILOT_ACQUISITION_TEST_CHILD", "1")
            .env("CARGO_MANIFEST_DIR", self.dir.path())
            .env("OUT_DIR", self.path("out"))
            .env("BUNDLED_CLI_CACHE_DIR", self.path("cache"))
            .env("COPILOT_CLI_EXTRACT_DIR", self.path("extracted"))
            .env(
                "CARGO_CFG_TARGET_OS",
                if self.platform.package_name.contains("win32") {
                    "windows"
                } else {
                    "linux"
                },
            )
            .env("CARGO_CFG_TARGET_ARCH", "x86_64")
            .env("CARGO_CFG_TARGET_ENV", "")
            .env("CARGO_FEATURE_RUNTIME", "1")
            .env_remove("COPILOT_SKIP_CLI_DOWNLOAD")
            .env_remove("DOCS_RS")
            .env_remove("CARGO_FEATURE_LOCAL_RUNTIME")
            .env_remove("CARGO_FEATURE_BUNDLED_CLI")
            .env_remove("CARGO_FEATURE_IN_PROCESS");
        if bundled {
            command.env("CARGO_FEATURE_BUNDLED_CLI", "1");
        }
        if in_process {
            command.env("CARGO_FEATURE_IN_PROCESS", "1");
        }
        #[cfg(windows)]
        {
            use std::os::windows::process::CommandExt;
            const CREATE_NO_WINDOW: u32 = 0x0800_0000;
            command.creation_flags(CREATE_NO_WINDOW);
        }
        command
    }
}

#[track_caller]
fn succeeded(output: Output) -> String {
    assert!(
        output.status.success(),
        "stdout:\n{}\nstderr:\n{}",
        String::from_utf8_lossy(&output.stdout),
        String::from_utf8_lossy(&output.stderr)
    );
    let stdout = String::from_utf8(output.stdout).unwrap();
    assert!(!stdout.contains("Downloading "), "{stdout}");
    stdout
}

#[test]
fn run_build_script() {
    if std::env::var_os("COPILOT_ACQUISITION_TEST_CHILD").is_some() {
        super::main();
    }
}

#[test]
fn seeded_builds_support_bundled_in_process_and_extracted_modes_for_both_locations() {
    for sdk in [false, true] {
        for windows in [false, true] {
            for bundled in [false, true] {
                for in_process in [false, true] {
                    let fixture = Fixture::new(sdk, windows);
                    let stdout = succeeded(fixture.command(bundled, in_process).output().unwrap());
                    assert!(stdout.contains(&format!(
                        "cargo:rustc-env=COPILOT_SDK_CLI_CACHE_ID={}",
                        fixture.release.cache_identity()
                    )));
                    if bundled {
                        assert!(stdout.contains("cargo:rustc-cfg=has_bundled_cli"));
                        assert!(fixture.path("out/copilot_cli.archive").is_file());
                        let runtime =
                            fs::read(fixture.path("out/copilot_runtime.archive")).unwrap();
                        assert!(archive_contains_tar_entry(
                            &runtime,
                            fixture.platform.runtime_wrapper_name()
                        ));
                        assert!(archive_contains_tar_entry(&runtime, "roots.pem"));
                        assert_eq!(
                            archive_contains_tar_entry(
                                &runtime,
                                fixture.platform.runtime_library_name()
                            ),
                            in_process
                        );
                    } else {
                        assert!(stdout.contains("cargo:rustc-cfg=has_extracted_cli"));
                        assert!(
                            fixture
                                .path("extracted")
                                .join(fixture.platform.runtime_wrapper_name())
                                .is_file()
                        );
                        assert!(fixture.path("extracted/tls/roots.pem").is_file());
                        assert_eq!(
                            fixture
                                .path("extracted")
                                .join(fixture.platform.runtime_library_name())
                                .is_file(),
                            in_process
                        );
                        // A valid extracted cache needs neither archive nor a network lookup.
                        fs::remove_dir_all(fixture.path("cache")).unwrap();
                        succeeded(fixture.command(bundled, in_process).output().unwrap());
                    }
                }
            }
        }
    }
}

#[test]
fn stale_extracted_markers_reinstall_verified_runtime_assets() {
    let fixture = Fixture::new(true, false);
    succeeded(fixture.command(false, true).output().unwrap());
    let wrapper = fixture
        .path("extracted")
        .join(fixture.platform.runtime_wrapper_name());
    fs::write(&wrapper, b"stale wrapper").unwrap();
    fs::write(
        fixture.path("extracted/.hostless-runtime-assets-v1"),
        format!("{VERSION}\n{HASH}\n"),
    )
    .unwrap();
    fs::write(fixture.path("extracted/stale-file"), b"old payload").unwrap();
    succeeded(fixture.command(false, true).output().unwrap());
    assert_eq!(fs::read(wrapper).unwrap(), b"fixture wrapper");
    assert!(!fixture.path("extracted/stale-file").exists());
}

#[test]
fn builds_reject_inconsistent_snapshot_versions_and_locations_before_acquisition() {
    for change_version in [false, true] {
        let fixture = Fixture::new(true, false);
        let path = fixture.path("cli-version.txt");
        let contents = fs::read_to_string(&path).unwrap();
        let changed = if change_version {
            contents.replace(VERSION, "9.9.9")
        } else {
            contents.replace(
                &format!("release-url=https://github.com/github/copilot-sdk/releases/download/runtime-{VERSION}\n"),
                ""
            )
        };
        fs::write(path, changed).unwrap();
        let output = fixture.command(true, false).output().unwrap();
        assert!(!output.status.success());
        assert!(String::from_utf8_lossy(&output.stderr).contains("same version and release URL"));
        assert!(!String::from_utf8_lossy(&output.stdout).contains("Downloading "));
    }
}

#[test]
fn external_stream_only_builds_need_no_runtime_artifacts() {
    let fixture = Fixture::new(true, false);
    fs::remove_file(fixture.path("cli-version.txt")).unwrap();
    fs::remove_file(fixture.path("cli-version-in-process.txt")).unwrap();
    fs::remove_dir_all(fixture.path("cache")).unwrap();
    let mut command = fixture.command(false, false);
    command.env_remove("CARGO_FEATURE_RUNTIME");
    let stdout = succeeded(command.output().unwrap());
    assert!(!stdout.contains("cargo:rustc-env=COPILOT_SDK_CLI_VERSION"));
    assert!(!stdout.contains("cargo:rustc-cfg=has_"));
    assert!(!fixture.path("extracted/copilot-runtime").exists());
}

#[test]
fn skip_modes_need_no_snapshots_and_local_runtime_preserves_bundled_precedence() {
    for skip in [
        "DOCS_RS",
        "COPILOT_SKIP_CLI_DOWNLOAD",
        "CARGO_FEATURE_LOCAL_RUNTIME",
    ] {
        let fixture = Fixture::new(true, false);
        fs::remove_file(fixture.path("cli-version.txt")).unwrap();
        fs::remove_file(fixture.path("cli-version-in-process.txt")).unwrap();
        fs::remove_dir_all(fixture.path("cache")).unwrap();
        let mut command = fixture.command(false, true);
        command.env(skip, "1");
        let stdout = succeeded(command.output().unwrap());
        assert!(!stdout.contains("cargo:rustc-cfg=has_"));
    }
    let fixture = Fixture::new(true, false);
    let mut command = fixture.command(true, true);
    command.env("CARGO_FEATURE_LOCAL_RUNTIME", "1");
    assert!(succeeded(command.output().unwrap()).contains("cargo:rustc-cfg=has_bundled_cli"));
}
