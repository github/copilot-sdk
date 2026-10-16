/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use std::sync::{Mutex, mpsc as std_mpsc};
use std::time::Duration;

use super::*;
use crate::Client;

// The native function pointer cannot capture the per-test shutdown gate.
static SHUTDOWN_GATE: Mutex<Option<(std_mpsc::SyncSender<()>, std_mpsc::Receiver<()>)>> =
    Mutex::new(None);

unsafe extern "C" fn gated_host_shutdown(_server_id: u32) -> bool {
    let gate = SHUTDOWN_GATE.lock().unwrap();
    let (started, release) = gate.as_ref().unwrap();
    started.send(()).unwrap();
    release.recv().unwrap();
    true
}

unsafe extern "C" fn connection_close(_connection_id: u32) -> bool {
    true
}

unsafe extern "C" fn connection_write(
    _connection_id: u32,
    _bytes: *const u8,
    _length: usize,
) -> bool {
    true
}

#[test]
fn async_stop_keeps_current_thread_executor_responsive_during_native_shutdown() {
    let (started_tx, started_rx) = std_mpsc::sync_channel(1);
    let (release_tx, release_rx) = std_mpsc::channel();
    let (progress_tx, progress_rx) = std_mpsc::channel();
    let (tick_tx, tick_rx) = tokio::sync::oneshot::channel();
    *SHUTDOWN_GATE.lock().unwrap() = Some((started_tx, release_rx));
    let worker = std::thread::spawn(move || {
        tokio::runtime::Builder::new_current_thread()
            .enable_all()
            .build()
            .unwrap()
            .block_on(async {
                let directory = tempfile::tempdir().unwrap();
                let client = Client::from_streams(
                    tokio::io::empty(),
                    tokio::io::sink(),
                    directory.path().to_path_buf(),
                )
                .unwrap();
                *client.inner.ffi_host.lock() = Some(Arc::new(FfiShared {
                    host_shutdown: gated_host_shutdown,
                    connection_write,
                    connection_close,
                    server_id: AtomicU32::new(11),
                    connection_id: AtomicU32::new(0),
                    callback_state: AtomicPtr::new(std::ptr::null_mut()),
                    closed: AtomicBool::new(false),
                    operation_lock: parking_lot::Mutex::new(()),
                    library_path: PathBuf::from("test-runtime"),
                }));
                client.inner.rpc.force_close();
                let stop = tokio::spawn(async move { client.stop().await });
                tick_rx.await.unwrap();
                progress_tx.send(stop.is_finished()).unwrap();
                stop.await
                    .unwrap()
                    .expect_err("the RPC connection is closed")
            })
    });

    let started = started_rx.recv_timeout(Duration::from_secs(5));
    tick_tx.send(()).unwrap();
    let progress = progress_rx.recv_timeout(Duration::from_secs(5));
    release_tx.send(()).unwrap();
    let errors = worker.join().unwrap();
    *SHUTDOWN_GATE.lock().unwrap() = None;

    assert_eq!(started, Ok(()), "native shutdown did not start");
    assert_eq!(
        progress,
        Ok(false),
        "native shutdown blocked the executor or stop completed before cleanup"
    );
    assert_eq!(errors.0.len(), 1, "only the closed RPC should fail");
}
