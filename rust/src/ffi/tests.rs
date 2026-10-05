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

#[test]
fn argv_without_entrypoint_contains_only_client_options() {
    let argv: Vec<String> = serde_json::from_slice(&build_argv_json(
        None,
        &["--log-level".into(), "debug".into()],
    ))
    .unwrap();

    assert_eq!(argv, ["--log-level", "debug"]);
}

#[test]
fn explicit_javascript_entrypoint_uses_node() {
    let argv: Vec<String> =
        serde_json::from_slice(&build_argv_json(Some(Path::new("index.js")), &[])).unwrap();

    assert_eq!(
        argv,
        ["node", "index.js", "--embedded-host", "--no-auto-update"]
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
fn environment_serializes_worker_overrides() {
    let env: serde_json::Value = serde_json::from_slice(
        &build_env_json(&[
            ("COPILOT_HOME".into(), "state".into()),
            ("COPILOT_DISABLE_KEYTAR".into(), "1".into()),
        ])
        .unwrap(),
    )
    .unwrap();

    assert_eq!(
        env,
        serde_json::json!({
            "COPILOT_HOME": "state",
            "COPILOT_DISABLE_KEYTAR": "1",
        })
    );
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
        host_shutdown: test_host_shutdown,
        connection_write: test_connection_write,
        connection_close: test_connection_close,
        server_id: AtomicU32::new(11),
        connection_id: AtomicU32::new(21),
        callback_state: AtomicPtr::new(state_ptr),
        closed: AtomicBool::new(false),
        operation_lock: parking_lot::Mutex::new(()),
        library_path: PathBuf::from("test-runtime"),
    };

    shared.close();

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
