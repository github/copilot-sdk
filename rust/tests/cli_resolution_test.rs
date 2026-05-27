//! Tests for the build-time and runtime CLI provisioning path.
//!
//! Covers the `COPILOT_CLI_PATH` env override, the dev-mode (no
//! `bundled-cli`) build-time extracted binary, and the embed-mode lazy
//! extraction. Mutating `COPILOT_CLI_PATH` is process-global, so all such
//! tests use `serial_test` to avoid races with each other (and with the
//! e2e tests which also read it).

use std::path::PathBuf;

use github_copilot_sdk::{CliProgram, Client, ClientOptions};
use serial_test::serial;

fn unset_env(key: &str) {
    // SAFETY: tests are serialized with #[serial], so no other thread can
    // observe the env mid-write. This is the standard pattern for testing
    // env-driven behavior in this crate.
    unsafe { std::env::remove_var(key) };
}

fn set_env(key: &str, value: &str) {
    unsafe { std::env::set_var(key, value) };
}

/// COPILOT_CLI_PATH wins when it points at a real file, regardless of
/// build mode.
#[tokio::test]
#[serial(copilot_cli_path)]
async fn env_override_resolves_to_pointed_file() {
    let tmp = tempfile::NamedTempFile::new().expect("create tempfile");
    // Make the temp file executable on POSIX so resolve doesn't reject it;
    // resolve only checks `is_file()`, but downstream `Client::start`
    // wants to exec the binary. We only need the resolver to return the
    // path here, so a `--version`-like wrapper isn't required.
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
#[tokio::test]
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
        let msg = format!("{e}");
        assert!(
            !msg.contains("BinaryNotFound") && !msg.contains("not bundled"),
            "stale COPILOT_CLI_PATH should fall through to bundled/dev CLI; got {msg}"
        );
    }
}

/// In dev mode (no `bundled-cli` feature) build.rs writes the extracted
/// binary into the per-user cache and emits its path as
/// `COPILOT_CLI_DEV_PATH`. The runtime resolver returns that path.
#[cfg(not(feature = "bundled-cli"))]
#[test]
fn dev_mode_extracted_binary_exists() {
    let path = PathBuf::from(env!("COPILOT_CLI_DEV_PATH"));
    assert!(
        path.is_file(),
        "expected build.rs to extract the CLI to {} (dev mode)",
        path.display()
    );

    // Confirm the cache layout matches what runtime resolution expects.
    let display = path.display().to_string();
    assert!(
        display.contains("github-copilot-sdk") && display.contains("/cli/"),
        "dev path {} does not match expected `github-copilot-sdk/cli/<version>/` layout",
        display
    );
}

/// Build-time version pin: `cli-version.txt` (when present) must be a
/// single-line non-empty exact version. When absent, build.rs falls
/// through to `../nodejs/package-lock.json` — both are accepted, this
/// test only checks the pin file's format if it's there.
#[test]
fn pin_file_when_present_is_well_formed() {
    let manifest_dir = env!("CARGO_MANIFEST_DIR");
    let pin = PathBuf::from(manifest_dir).join("cli-version.txt");
    if !pin.is_file() {
        // Mono-repo build path — no assertion needed.
        return;
    }
    let contents = std::fs::read_to_string(&pin).expect("read cli-version.txt");
    let trimmed = contents.trim();
    assert!(!trimmed.is_empty(), "cli-version.txt is empty");
    assert!(
        !trimmed.contains(char::is_whitespace),
        "cli-version.txt should be a single version line, got {trimmed:?}"
    );
}
