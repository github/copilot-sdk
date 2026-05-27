//! Tests for the build-time and runtime CLI provisioning path.
//!
//! Covers the `COPILOT_CLI_PATH` env override, the dev-mode (no
//! `bundled-cli`) build-time extracted binary, and the embed-mode lazy
//! extraction. Mutating `COPILOT_CLI_PATH` is process-global, so all such
//! tests use `serial_test` to avoid races with each other (and with the
//! e2e tests which also read it).

use std::path::PathBuf;

use github_copilot_sdk::{CliProgram, Client, ClientOptions, Error};
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
    // SAFETY: these tests are serialized with #[serial(copilot_cli_path)]
    // so no other test in this binary mutates COPILOT_CLI_PATH while
    // we hold the lock. POSIX `setenv`/`unsetenv` are generally
    // thread-safe on modern platforms, and we use `current_thread`
    // tokio runtimes to avoid concurrent reads from worker threads.
    // This doesn't satisfy the strict Rust 2024 safety contract
    // (other tests in the binary may read env vars), but the practical
    // race window is negligible.
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

    // In a normally-configured build (either bundled-cli or dev mode) the
    // resolver should find a binary via the next source. Failing here
    // would mean fallthrough is broken.
    if let Err(e) = &result {
        assert!(
            !matches!(e, Error::BinaryNotFound { .. }),
            "stale COPILOT_CLI_PATH should fall through; got BinaryNotFound: {e}"
        );
    }
}

/// In dev mode (no `bundled-cli` feature) build.rs writes the extracted
/// binary into the per-user cache and emits its path as
/// `COPILOT_CLI_DEV_PATH`. The runtime resolver returns that path.
#[cfg(has_dev_cli)]
#[test]
fn dev_mode_extracted_binary_exists() {
    let path = PathBuf::from(env!("COPILOT_CLI_DEV_PATH"));
    assert!(
        path.is_file(),
        "expected build.rs to extract the CLI to {} (dev mode)",
        path.display()
    );

    // Confirm the cache layout matches what runtime resolution expects.
    let mut found = false;
    let comps: Vec<_> = path.components().collect();
    for window in comps.windows(2) {
        if let (std::path::Component::Normal(a), std::path::Component::Normal(b)) =
            (&window[0], &window[1])
            && a.to_str() == Some("github-copilot-sdk")
            && b.to_str() == Some("cli")
        {
            found = true;
            break;
        }
    }
    assert!(
        found,
        "dev path {} does not contain the expected `github-copilot-sdk/cli/` segments",
        path.display()
    );
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
