/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use std::sync::Mutex;
use std::sync::atomic::AtomicUsize;
use std::time::{Duration, Instant};

use super::*;

static FFI_LIFECYCLE_TEST_LOCK: Mutex<()> = Mutex::new(());
static TEST_ALLOW_CLOSE: AtomicBool = AtomicBool::new(false);
static TEST_CLOSE_CALLS: AtomicUsize = AtomicUsize::new(0);
static TEST_SHUTDOWN_CALLS: AtomicUsize = AtomicUsize::new(0);

#[cfg(copilot_cli_host)]
pub(super) fn library_without_private_admission() -> &'static Library {
    static LIBRARY: OnceLock<Library> = OnceLock::new();
    LIBRARY.get_or_init(|| {
        #[cfg(unix)]
        let library = libloading::os::unix::Library::this().into();
        #[cfg(windows)]
        let library = libloading::os::windows::Library::this().unwrap().into();
        library
    })
}

#[test]
fn explicit_image_does_not_require_a_cli_entrypoint() {
    let directory = tempfile::tempdir().unwrap();
    let entrypoint = directory.path().join("no-cli");
    let image = directory.path().join("missing-runtime.node");
    let error = FfiHost::create(
        Some(&entrypoint),
        None,
        None,
        Vec::new(),
        Vec::new(),
        FfiHostTarget {
            auth_owner: None,
            server_id: Some(42),
            library_path: Some(&image),
            session_binding: None,
        },
    )
    .err()
    .expect("the missing native image must fail");
    assert!(
        error
            .to_string()
            .contains("failed to resolve in-process runtime library"),
        "{error}"
    );
}

unsafe extern "C" fn test_host_shutdown(_server_id: u32) -> bool {
    TEST_SHUTDOWN_CALLS.fetch_add(1, Ordering::SeqCst);
    true
}

unsafe extern "C" fn test_connection_write(
    _connection_id: u32,
    _bytes: *const u8,
    _length: usize,
) -> bool {
    true
}

unsafe extern "C" fn test_connection_close(_connection_id: u32) -> bool {
    TEST_CLOSE_CALLS.fetch_add(1, Ordering::SeqCst);
    TEST_ALLOW_CLOSE.load(Ordering::SeqCst)
}

unsafe extern "C" fn test_set_ui_thread_handler(connection_id: u32, enabled: bool) -> bool {
    connection_id == 21 && enabled
}

unsafe extern "C" fn test_run_ui_action(connection_id: u32, action_id: u32) -> bool {
    connection_id == 21 && action_id == 42
}

#[test]
fn argv_without_entrypoint_contains_only_client_options() {
    let argv: Vec<String> = serde_json::from_slice(&build_argv_json(
        None,
        None,
        &["--log-level".into(), "debug".into()],
    ))
    .unwrap();

    assert_eq!(argv, ["--log-level", "debug"]);
}

#[test]
fn explicit_javascript_entrypoint_uses_node() {
    let argv: Vec<String> = serde_json::from_slice(&build_argv_json(
        Some(Path::new("index.js")),
        Some("microsoft/vscode"),
        &[],
    ))
    .unwrap();

    assert_eq!(
        argv,
        [
            "node",
            "index.js",
            "--embedded-host",
            "--no-auto-update",
            "--cli-login-sync-integration-id=microsoft/vscode",
        ]
    );
}

#[cfg(windows)]
#[test]
fn child_process_path_removes_windows_verbatim_prefix() {
    assert_eq!(
        path_for_child_process(PathBuf::from(r"\\?\D:\a\copilot-sdk\index.js")),
        PathBuf::from(r"D:\a\copilot-sdk\index.js")
    );
    assert_eq!(
        path_for_child_process(PathBuf::from(r"\\?\UNC\server\share\copilot-sdk\index.js")),
        PathBuf::from(r"\\server\share\copilot-sdk\index.js")
    );
}

#[test]
fn environment_is_omitted_when_empty() {
    assert_eq!(build_env_json(&[]), None);
}

#[test]
fn environment_key_casing_preserves_later_overrides_on_windows() {
    let env: serde_json::Value = serde_json::from_slice(
        &build_env_json(&[
            ("COPILOT_HOME".into(), "original-home".into()),
            ("copilot_home".into(), "alias-home".into()),
            ("COPILOT_HOME".into(), "typed-home".into()),
            ("HTTP_PROXY".into(), "http://proxy".into()),
            ("http_proxy".into(), "http://proxy".into()),
        ])
        .unwrap(),
    )
    .unwrap();

    assert_eq!(env["COPILOT_HOME"], "typed-home");
    assert_eq!(env["HTTP_PROXY"], "http://proxy");
    if cfg!(windows) {
        assert!(env.get("copilot_home").is_none());
        assert!(env.get("http_proxy").is_none());
    } else {
        assert_eq!(env["copilot_home"], "alias-home");
        assert_eq!(env["http_proxy"], "http://proxy");
    }
}

#[test]
fn environment_serializes_worker_overrides() {
    let env: serde_json::Value = serde_json::from_slice(
        &build_env_json(&[
            ("COPILOT_HOME".into(), "state".into()),
            ("COPILOT_DISABLE_KEYTAR".into(), "1".into()),
        ])
        .unwrap(),
    )
    .unwrap();

    assert_eq!(env["COPILOT_HOME"], "state");
    assert_eq!(env["COPILOT_DISABLE_KEYTAR"], "1");
    for (name, value) in std::env::vars_os() {
        let (Ok(name), Ok(value)) = (name.into_string(), value.into_string()) else {
            continue;
        };
        let name = if cfg!(windows) {
            name.to_uppercase()
        } else {
            name
        };
        if matches!(name.as_str(), "COPILOT_HOME" | "COPILOT_DISABLE_KEYTAR") {
            continue;
        }
        assert!(
            env.get(&name) == Some(&serde_json::Value::String(value)),
            "startup environment must preserve inherited variable {name}"
        );
    }
}

#[cfg(any(unix, windows))]
#[test]
fn environment_preserves_non_unicode_explicit_settings() {
    const CHILD: &str = "COPILOT_FFI_ENVIRONMENT_TEST_KEY";
    if let Ok(key) = std::env::var(CHILD) {
        let environment: serde_json::Value = serde_json::from_slice(
            &build_env_json(&[("COPILOT_HOME".into(), "typed-home".into())]).unwrap(),
        )
        .unwrap();
        let normalized = if cfg!(windows) {
            key.to_uppercase()
        } else {
            key.clone()
        };
        assert_eq!(environment[&normalized], "\u{FFFD}");
        assert_eq!(environment["COPILOT_HOME"], "typed-home");
        assert!(environment.get("UNRELATED_NON_UNICODE").is_none());
        if !cfg!(windows) && key == "https_proxy" {
            assert_eq!(environment["HTTPS_PROXY"], "http://fallback.invalid:8080");
        }
        let overridden: serde_json::Value =
            serde_json::from_slice(&build_env_json(&[(key, "typed-setting".into())]).unwrap())
                .unwrap();
        assert_eq!(overridden[&normalized], "typed-setting");
        return;
    }

    #[cfg(unix)]
    let non_unicode = {
        use std::os::unix::ffi::OsStringExt;
        std::ffi::OsString::from_vec(vec![0xFF])
    };
    #[cfg(windows)]
    let non_unicode = {
        use std::os::windows::ffi::OsStringExt;
        std::ffi::OsString::from_wide(&[0xD800])
    };
    let current = std::thread::current();
    for key in [
        "http_proxy",
        "HTTP_PROXY",
        "https_proxy",
        "HTTPS_PROXY",
        "COPILOT_PROXY_KERBEROS_SPN",
        "TMPDIR",
    ] {
        let output = std::process::Command::new(
            std::env::current_exe().expect("resolve current test executable"),
        )
        .args(["--exact", current.name().expect("test name"), "--nocapture"])
        .env_clear()
        .env(CHILD, key)
        .env("HTTPS_PROXY", "http://fallback.invalid:8080")
        .env("UNRELATED_NON_UNICODE", &non_unicode)
        .env(key, &non_unicode)
        .output()
        .expect("run isolated environment snapshot");
        assert!(
            output.status.success() && String::from_utf8_lossy(&output.stdout).contains("1 passed"),
            "{key}: {}{}",
            String::from_utf8_lossy(&output.stdout),
            String::from_utf8_lossy(&output.stderr)
        );
    }
}

#[test]
fn callback_state_is_retained_until_connection_close_succeeds() {
    let _guard = FFI_LIFECYCLE_TEST_LOCK.lock().unwrap();
    TEST_ALLOW_CLOSE.store(false, Ordering::SeqCst);
    TEST_CLOSE_CALLS.store(0, Ordering::SeqCst);
    TEST_SHUTDOWN_CALLS.store(0, Ordering::SeqCst);

    let (tx, mut rx) = mpsc::unbounded_channel();
    let state_ptr = Box::into_raw(Box::new(CallbackState {
        tx,
        closing: AtomicBool::new(false),
    }));
    let shared = FfiShared {
        #[cfg(copilot_cli_host)]
        library: library_without_private_admission(),
        auth_owner: parking_lot::Mutex::new(None),
        host_shutdown: test_host_shutdown,
        owns_host: true,
        connection_write: test_connection_write,
        connection_close: test_connection_close,
        connection_set_ui_thread_handler: test_set_ui_thread_handler,
        connection_run_ui_action: test_run_ui_action,
        server_id: AtomicU32::new(11),
        connection_id: AtomicU32::new(21),
        callback_state: AtomicPtr::new(state_ptr),
        closed: AtomicBool::new(false),
        operation_lock: parking_lot::Mutex::new(()),
        library_path: PathBuf::from("test-runtime"),
    };

    assert_eq!(shared.server_id(), Some(11));
    shared.close();
    assert_eq!(shared.server_id(), None);

    assert!(TEST_CLOSE_CALLS.load(Ordering::SeqCst) >= 1);
    assert_eq!(TEST_SHUTDOWN_CALLS.load(Ordering::SeqCst), 0);
    assert_eq!(shared.connection_id.load(Ordering::SeqCst), 0);
    assert!(shared.callback_state.load(Ordering::SeqCst).is_null());

    assert!(matches!(
        rx.try_recv(),
        Err(mpsc::error::TryRecvError::Empty)
    ));

    TEST_ALLOW_CLOSE.store(true, Ordering::SeqCst);
    let deadline = Instant::now() + Duration::from_secs(5);
    while Instant::now() < deadline && TEST_SHUTDOWN_CALLS.load(Ordering::SeqCst) == 0 {
        std::thread::sleep(Duration::from_millis(10));
    }

    assert!(TEST_CLOSE_CALLS.load(Ordering::SeqCst) >= 2);
    assert_eq!(TEST_SHUTDOWN_CALLS.load(Ordering::SeqCst), 1);
    assert!(matches!(
        rx.try_recv(),
        Err(mpsc::error::TryRecvError::Disconnected)
    ));

    let close_calls_after_cleanup = TEST_CLOSE_CALLS.load(Ordering::SeqCst);
    shared.close();
    assert_eq!(
        TEST_CLOSE_CALLS.load(Ordering::SeqCst),
        close_calls_after_cleanup
    );
    assert_eq!(TEST_SHUTDOWN_CALLS.load(Ordering::SeqCst), 1);
}
