//! Tests for the build-time and runtime CLI provisioning path.
//!
//! Covers the `COPILOT_CLI_PATH` env override, the build-time-extracted
//! binary used when `bundled-cli` is off, and the embed-mode lazy
//! extraction. Mutating env vars is process-global, so all such tests
//! use `serial_test` to avoid races with each other (and with the e2e
//! tests which also read them).

use std::path::PathBuf;

use github_copilot_sdk::{CliProgram, Client, ClientOptions, ErrorKind};
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

/// With `bundled-cli` off, `build.rs` extracts the binary into the
/// per-user cache and the runtime resolver recomputes its location from
/// `COPILOT_SDK_CLI_VERSION` + the OS-derived binary name. This test
/// mirrors that convention and asserts the file is on disk where the
/// resolver expects to find it.
#[cfg(all(not(feature = "bundled-cli"), has_extracted_cli))]
#[test]
fn extracted_binary_present_at_conventional_path() {
    let version = env!("COPILOT_SDK_CLI_VERSION");
    let binary = if cfg!(windows) {
        "copilot.exe"
    } else {
        "copilot"
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
/// We place a fake `copilot[.exe]` there and assert the resolver picks
/// it up — failing here means the build-time / runtime convention has
/// drifted.
#[cfg(all(not(feature = "bundled-cli"), has_extracted_cli))]
#[tokio::test(flavor = "current_thread")]
#[serial(copilot_cli_path)]
async fn extract_dir_runtime_override_is_honored() {
    let tmp = tempfile::tempdir().expect("create tempdir");
    let binary = if cfg!(windows) {
        "copilot.exe"
    } else {
        "copilot"
    };
    let fake = tmp.path().join(binary);
    std::fs::write(&fake, b"").expect("write fake binary");

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

/// Build-time version pin: `cli-version.txt` (when present) must be a
/// combined snapshot — a `version=X.Y.Z` line plus per-asset hash lines.
/// When absent, build.rs falls through to `../nodejs/package-lock.json` —
/// both are accepted, this test only checks the pin file's format if it's
/// there.
#[test]
fn pin_file_when_present_is_well_formed() {
    let manifest_dir = env!("CARGO_MANIFEST_DIR");
    let pin = PathBuf::from(manifest_dir).join("cli-version.txt");
    if !pin.is_file() {
        // Contributor build path — no assertion needed.
        return;
    }
    let contents = std::fs::read_to_string(&pin).expect("read cli-version.txt");
    let mut saw_version = false;
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
        }
    }
    assert!(saw_version, "cli-version.txt missing `version=` line");
}
