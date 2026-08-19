//! Tests for the build-time and runtime CLI provisioning path.
//!
//! Covers the `COPILOT_CLI_PATH` env override, the build-time-extracted
//! binary used when `bundled-cli` is off, and the embed-mode lazy
//! extraction. Mutating env vars is process-global, so all such tests
//! use `serial_test` to avoid races with each other (and with the e2e
//! tests which also read them).

use std::path::PathBuf;

use github_copilot_sdk::{
    CliProgram, Client, ClientOptions, ErrorKind, HAS_BUNDLED_CLI, install_bundled_cli,
    install_bundled_runtime,
};
#[cfg(all(feature = "bundled-cli", has_bundled_cli))]
use github_copilot_sdk::{SessionConfig, Transport};
use serial_test::serial;

fn unset_env(key: &str) {
    // SAFETY: these tests are serialized with #[serial(copilot_cli_path)]
    // so no other test in this binary mutates COPILOT_CLI_PATH while
    // we hold the lock. POSIX `setenv`/`unsetenv` are generally
    // thread-safe on modern platforms, and we use `current_thread`
    // tokio runtimes to avoid concurrent reads from worker threads.
    // This doesn't satisfy the strict Rust 2024 safety contract
    // (other tests in the binary may read env vars), but the practical
    // race window is negligible.
    unsafe { std::env::remove_var(key) };
}

fn set_env(key: &str, value: &str) {
    // SAFETY: see `unset_env`.
    unsafe { std::env::set_var(key, value) };
}

/// COPILOT_CLI_PATH wins when it points at a real file, regardless of
/// build mode.
#[tokio::test(flavor = "current_thread")]
#[serial(copilot_cli_path)]
async fn env_override_resolves_to_pointed_file() {
    let tmp = tempfile::NamedTempFile::new().expect("create tempfile");
    // resolve.rs only checks `is_file()` for COPILOT_CLI_PATH, so a plain
    // tempfile is sufficient — we don't need it to be executable. The
    // downstream `Client::start` call will fail to exec an empty file,
    // which we tolerate below; we just need to observe that the resolver
    // returned the env-override path rather than `BinaryNotFound`.
    let path = tmp.path().to_path_buf();

    set_env(
        "COPILOT_CLI_PATH",
        path.to_str().expect("utf-8 tempfile path"),
    );
    let opts = ClientOptions::default().with_program(CliProgram::Resolve);

    // `Client::start` reads the env var via resolve.rs. We don't want to
    // actually launch a subprocess against our empty temp file, so go
    // through the public API just far enough to observe the resolution.
    // The easiest observable behavior is that `Client::start` doesn't
    // return `Error::BinaryNotFound` — it'll fail later trying to exec
    // the empty file, which we tolerate.
    let result = Client::start(opts).await;
    unset_env("COPILOT_CLI_PATH");

    match result {
        Ok(_) => {}
        Err(e) => {
            let msg = format!("{e}");
            assert!(
                !msg.contains("not found"),
                "expected COPILOT_CLI_PATH to win; got {msg}"
            );
        }
    }

    // Drop tmp explicitly so the file outlives the assertions above.
    drop(tmp);
    let _ = path;
}

/// A stale (non-existent) COPILOT_CLI_PATH falls through to the next
/// resolution source (embed or dev) rather than failing outright.
#[tokio::test(flavor = "current_thread")]
#[serial(copilot_cli_path)]
async fn stale_env_override_falls_through() {
    set_env("COPILOT_CLI_PATH", "/definitely/does/not/exist/copilot");
    let opts = ClientOptions::default().with_program(CliProgram::Resolve);
    let result = Client::start(opts).await;
    unset_env("COPILOT_CLI_PATH");

    // In a normally-configured build (either `bundled-cli` on or off)
    // the resolver should find a binary via the next source. Failing
    // here would mean fallthrough is broken.
    if let Err(e) = &result {
        assert!(
            !matches!(e.kind(), ErrorKind::BinaryNotFound { .. }),
            "stale COPILOT_CLI_PATH should fall through; got BinaryNotFound: {e}"
        );
    }
}

/// With `bundled-cli` off, `build.rs` extracts the runtime wrapper into the
/// per-user cache and the runtime resolver recomputes its location from
/// `COPILOT_SDK_CLI_VERSION` + the OS-derived binary name. This test
/// mirrors that convention and asserts the file is on disk where the
/// resolver expects to find it.
#[cfg(all(not(feature = "bundled-cli"), has_extracted_cli))]
#[test]
fn extracted_binary_present_at_conventional_path() {
    let version = env!("COPILOT_SDK_CLI_VERSION");
    let binary = if cfg!(windows) {
        "copilot-runtime.exe"
    } else {
        "copilot-runtime"
    };
    let sanitized = sanitize_version_for_test(version);
    let path = dirs::cache_dir()
        .expect("platform cache dir")
        .join("github-copilot-sdk")
        .join("cli")
        .join(sanitized)
        .join(binary);
    assert!(
        path.is_file(),
        "expected build.rs to extract the CLI to {} (`bundled-cli` off)",
        path.display()
    );
}

#[cfg(all(not(feature = "bundled-cli"), has_extracted_cli))]
fn sanitize_version_for_test(version: &str) -> String {
    version
        .chars()
        .map(|c| match c {
            'a'..='z' | 'A'..='Z' | '0'..='9' | '.' | '-' | '_' => c,
            _ => '_',
        })
        .collect()
}

/// With `bundled-cli` off, the resolver locates the build-time-extracted
/// binary without any runtime configuration. Observed via
/// `Client::start`: any outcome other than `BinaryNotFound` means the
/// resolver succeeded.
#[cfg(all(not(feature = "bundled-cli"), has_extracted_cli))]
#[tokio::test(flavor = "current_thread")]
#[serial(copilot_cli_path)]
async fn unbundled_resolver_finds_extracted_binary() {
    unset_env("COPILOT_CLI_PATH");
    unset_env("COPILOT_CLI_EXTRACT_DIR");

    let opts = ClientOptions::default().with_program(CliProgram::Resolve);
    let result = Client::start(opts).await;
    if let Err(e) = result {
        assert!(
            !matches!(e.kind(), ErrorKind::BinaryNotFound { .. }),
            "resolver returned BinaryNotFound with `bundled-cli` off: {e}"
        );
    }
}

/// With `bundled-cli` off, `COPILOT_CLI_EXTRACT_DIR` set at runtime
/// redirects the resolver to look directly under the named directory
/// (no per-version subdir, matching the build-time write semantics).
#[cfg(all(not(feature = "bundled-cli"), has_extracted_cli))]
#[tokio::test(flavor = "current_thread")]
#[serial(copilot_cli_path)]
async fn extract_dir_runtime_override_is_honored() {
    let tmp = tempfile::tempdir().expect("create tempdir");
    let binary = if cfg!(windows) {
        "copilot-runtime.exe"
    } else {
        "copilot-runtime"
    };
    let fake = tmp.path().join(binary);
    std::fs::write(&fake, b"runtime").expect("write fake binary");
    std::fs::write(tmp.path().join("runtime.node"), b"runtime").expect("write runtime.node");

    unset_env("COPILOT_CLI_PATH");
    set_env(
        "COPILOT_CLI_EXTRACT_DIR",
        tmp.path().to_str().expect("utf-8 tempdir path"),
    );

    let opts = ClientOptions::default().with_program(CliProgram::Resolve);
    let result = Client::start(opts).await;

    unset_env("COPILOT_CLI_EXTRACT_DIR");

    if let Err(e) = result {
        assert!(
            !matches!(e.kind(), ErrorKind::BinaryNotFound { .. }),
            "EXTRACT_DIR-redirected resolver returned BinaryNotFound: {e}"
        );
    }

    drop(tmp);
    let _ = fake;
}

/// Build-time version pins, when present, must match the selected bundling
/// implementation's checksum format.
/// When absent, build.rs falls through to `../nodejs/package-lock.json` —
/// both are accepted, this test only checks the pin file's format if it's
/// there.
#[test]
fn pin_file_when_present_is_well_formed() {
    let manifest_dir = env!("CARGO_MANIFEST_DIR");
    let (filename, value_prefix) = if cfg!(feature = "bundled-in-process") {
        ("cli-version-in-process.txt", Some("sha512-"))
    } else {
        ("cli-version.txt", None)
    };
    let pin = PathBuf::from(manifest_dir).join(filename);
    if !pin.is_file() {
        // Contributor build path — no assertion needed.
        return;
    }
    let contents = std::fs::read_to_string(&pin).expect("read CLI version snapshot");
    let mut saw_version = false;
    let mut package_count = 0;
    for raw in contents.lines() {
        let line = raw.trim();
        if line.is_empty() || line.starts_with('#') {
            continue;
        }
        let (key, value) = line
            .split_once('=')
            .unwrap_or_else(|| panic!("malformed line: {raw:?}"));
        assert!(!value.trim().is_empty(), "empty value for key {key:?}");
        if key.trim() == "version" {
            saw_version = true;
        } else {
            if let Some(prefix) = value_prefix {
                assert!(
                    value.trim().starts_with(prefix),
                    "invalid npm integrity for key {key:?}"
                );
            } else {
                assert_eq!(
                    value.trim().len(),
                    64,
                    "invalid SHA-256 hash for key {key:?}"
                );
                assert!(
                    value.trim().bytes().all(|byte| byte.is_ascii_hexdigit()),
                    "invalid SHA-256 hash for key {key:?}"
                );
            }
            package_count += 1;
        }
    }
    assert!(saw_version, "{filename} missing `version=` line");
    assert_eq!(package_count, 6);
}

/// With `bundled-cli` on AND a supported target, `install_bundled_cli`
/// returns a real on-disk path and is idempotent across calls.
#[cfg(all(feature = "bundled-cli", has_bundled_cli))]
#[test]
fn install_bundled_cli_returns_extracted_path() {
    const { assert!(HAS_BUNDLED_CLI) };

    let first = install_bundled_cli().expect("bundled CLI should install");
    assert!(
        first.is_file(),
        "install_bundled_cli returned a path that is not a file: {}",
        first.display()
    );
    assert_eq!(
        first.file_name().and_then(|name| name.to_str()),
        Some(if cfg!(windows) {
            "copilot.exe"
        } else {
            "copilot"
        })
    );

    let second = install_bundled_cli().expect("second call should also succeed");
    assert_eq!(
        first, second,
        "install_bundled_cli must be idempotent across calls"
    );

    #[cfg(feature = "bundled-in-process")]
    {
        let runtime_name = if cfg!(windows) {
            "copilot_runtime.dll"
        } else if cfg!(target_os = "macos") {
            "libcopilot_runtime.dylib"
        } else {
            "libcopilot_runtime.so"
        };
        let runtime = first
            .parent()
            .expect("install directory")
            .join(runtime_name);
        assert!(
            runtime.is_file(),
            "bundled runtime library was not installed: {}",
            runtime.display()
        );
    }
}

/// With `bundled-cli` off (or the target unsupported), the public API
/// reports no bundled CLI and does not fall back to the
/// build-time-extracted dev-cache path that `CliProgram::Resolve` uses.
#[cfg(not(all(feature = "bundled-cli", has_bundled_cli)))]
#[test]
fn install_bundled_cli_is_none_without_embed() {
    const { assert!(!HAS_BUNDLED_CLI) };
    assert!(
        install_bundled_cli().is_none(),
        "install_bundled_cli must not fall back to the dev-cache path"
    );
}

#[cfg(all(feature = "bundled-cli", has_bundled_cli))]
#[test]
fn install_bundled_runtime_returns_wrapper_bundle() {
    let first = install_bundled_runtime().expect("bundled runtime should install");
    assert_eq!(
        first.file_name().and_then(|name| name.to_str()),
        Some(if cfg!(windows) {
            "copilot-runtime.exe"
        } else {
            "copilot-runtime"
        })
    );
    let runtime_node = first
        .parent()
        .expect("install directory")
        .join("runtime.node");
    assert!(
        runtime_node.is_file(),
        "runtime.node was not installed: {}",
        runtime_node.display()
    );
    let cli = first
        .parent()
        .expect("install directory")
        .join(if cfg!(windows) {
            "copilot.exe"
        } else {
            "copilot"
        });
    assert!(
        cli.is_file(),
        "compatible CLI host was not installed: {}",
        cli.display()
    );

    let second = install_bundled_runtime().expect("second call should also succeed");
    assert_eq!(first, second);
}

#[cfg(all(feature = "bundled-cli", has_bundled_cli))]
#[tokio::test(flavor = "current_thread")]
#[serial(copilot_cli_path)]
async fn bundled_runtime_clean_extract_starts_with_sibling_cli() {
    let temp = tempfile::tempdir().expect("create tempdir");
    let extract_dir = temp.path().join("runtime");
    let empty_path = temp.path().join("empty-path");
    let working_dir = temp.path().join("work");
    std::fs::create_dir(&empty_path).expect("create empty PATH directory");
    std::fs::create_dir(&working_dir).expect("create working directory");
    assert!(!extract_dir.exists());

    let options = ClientOptions::new()
        .with_bundled_cli_extract_dir(&extract_dir)
        .with_cwd(&working_dir)
        .with_env([("PATH", empty_path.as_os_str())])
        .with_env_remove([
            "COPILOT_RUNTIME_HOST_COMMAND",
            "COPILOT_CLI_PATH",
            "COPILOT_RUNTIME_PROVIDER_LIB",
        ])
        .with_transport(Transport::Stdio)
        .with_use_logged_in_user(false);
    let client = Client::start(options)
        .await
        .expect("start bundled runtime from clean extraction");
    let response = client
        .ping(Some("sibling CLI fallback"))
        .await
        .expect("ping bundled runtime");
    assert_eq!(response.message, "pong: sibling CLI fallback");

    let session = client
        .create_session(SessionConfig::default())
        .await
        .expect("create session");
    session.disconnect().await.expect("disconnect session");
    client.stop().await.expect("stop bundled runtime");

    assert!(extract_dir.join("runtime.node").is_file());
    assert!(
        extract_dir
            .join(if cfg!(windows) {
                "copilot-runtime.exe"
            } else {
                "copilot-runtime"
            })
            .is_file()
    );
    assert!(
        extract_dir
            .join(if cfg!(windows) {
                "copilot.exe"
            } else {
                "copilot"
            })
            .is_file()
    );
}

#[cfg(not(all(feature = "bundled-cli", has_bundled_cli)))]
#[test]
fn install_bundled_runtime_is_none_without_embed() {
    assert!(
        install_bundled_runtime().is_none(),
        "install_bundled_runtime must not fall back to the dev-cache path"
    );
}
