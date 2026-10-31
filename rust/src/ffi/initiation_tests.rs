/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use std::future::Future;
use std::sync::mpsc as blocking;
use std::time::Duration;

use parking_lot::Mutex;
use serde_json::{Value, json};

use super::*;
use crate::jsonrpc::MessageWriter;
use crate::{Client, ClientMode};

static NEXT_ID: AtomicU32 = AtomicU32::new(1000);
static PEERS: OnceLock<Mutex<HashMap<u32, Arc<Native>>>> = OnceLock::new();

struct Native {
    frames: Mutex<Vec<Value>>,
    callback: usize,
    fail: AtomicBool,
    reply: AtomicBool,
    close_gate: Mutex<Option<(blocking::Sender<()>, blocking::Receiver<()>)>>,
    shutdown_gate: Mutex<Option<(blocking::Sender<()>, blocking::Receiver<()>)>>,
}

fn peers() -> &'static Mutex<HashMap<u32, Arc<Native>>> {
    PEERS.get_or_init(Mutex::default)
}

fn wait_at(gate: &Mutex<Option<(blocking::Sender<()>, blocking::Receiver<()>)>>) {
    let wait = gate.lock().take();
    if let Some((entered, release)) = wait {
        let _ = entered.send(());
        let _ = release.recv();
    }
}

unsafe extern "C" fn write(connection: u32, bytes: *const u8, length: usize) -> bool {
    let Some(native) = peers().lock().get(&connection).cloned() else {
        return false;
    };
    let frame = unsafe { std::slice::from_raw_parts(bytes, length) };
    let Some(separator) = frame.windows(4).position(|bytes| bytes == b"\r\n\r\n") else {
        return false;
    };
    let Ok(value) = serde_json::from_slice::<Value>(&frame[separator + 4..]) else {
        return false;
    };
    native.frames.lock().push(value.clone());
    if native.fail.load(Ordering::SeqCst) {
        return false;
    }
    if native.reply.load(Ordering::SeqCst)
        && let Some(id) = value["id"].as_u64()
    {
        let response = json!({
            "jsonrpc": "2.0",
            "id": id,
            "result": {"method": value["method"]},
        });
        let Ok(body) = serde_json::to_vec(&response) else {
            return false;
        };
        let mut frame = format!("Content-Length: {}\r\n\r\n", body.len()).into_bytes();
        frame.extend(body);
        on_outbound(native.callback as *mut c_void, frame.as_ptr(), frame.len());
    }
    true
}

unsafe extern "C" fn close(connection: u32) -> bool {
    let native = peers().lock().get(&connection).cloned();
    if let Some(native) = native {
        wait_at(&native.close_gate);
    }
    true
}

unsafe extern "C" fn shutdown(server: u32) -> bool {
    let native = peers().lock().get(&server).cloned();
    if let Some(native) = native {
        wait_at(&native.shutdown_gate);
    }
    true
}

unsafe extern "C" fn configure_ui(_connection: u32, _enabled: bool) -> bool {
    true
}

unsafe extern "C" fn run_ui(_connection: u32, _action: u32) -> bool {
    true
}

struct Peer {
    client: Option<Client>,
    shared: Arc<FfiShared>,
    native: Arc<Native>,
    id: u32,
}

impl Peer {
    fn new() -> Self {
        let id = NEXT_ID.fetch_add(1, Ordering::SeqCst);
        let (tx, rx) = mpsc::unbounded_channel();
        let callback = Box::into_raw(Box::new(CallbackState {
            tx,
            closing: AtomicBool::new(false),
        }));
        let native = Arc::new(Native {
            frames: Mutex::new(Vec::new()),
            callback: callback as usize,
            fail: AtomicBool::new(false),
            reply: AtomicBool::new(true),
            close_gate: Mutex::new(None),
            shutdown_gate: Mutex::new(None),
        });
        peers().lock().insert(id, native.clone());
        let shared = Arc::new(FfiShared {
            host_shutdown: shutdown,
            owns_host: true,
            connection_write: write,
            connection_close: close,
            connection_set_ui_thread_handler: configure_ui,
            connection_run_ui_action: run_ui,
            server_id: AtomicU32::new(id),
            connection_id: AtomicU32::new(id),
            callback_state: AtomicPtr::new(callback),
            closed: AtomicBool::new(false),
            operation_lock: Mutex::new(()),
            library_path: PathBuf::from("test-ingress-runtime"),
        });
        let sender = shared.clone();
        let client = Client::from_transport(
            FfiReader {
                rx,
                leftover: Vec::new(),
                pos: 0,
            },
            MessageWriter::Synchronous(Arc::new(move |frame| sender.write_frame(frame))),
            None,
            None,
            PathBuf::from("."),
            None,
            None,
            false,
            false,
            false,
            false,
            None,
            None,
            None,
            ClientMode::default(),
            None,
            None,
            false,
        )
        .unwrap();
        *client.inner.ffi_host.lock() = Some(shared.clone());
        Self {
            client: Some(client),
            shared,
            native,
            id,
        }
    }

    fn client(&self) -> &Client {
        self.client.as_ref().unwrap()
    }
}

impl Drop for Peer {
    fn drop(&mut self) {
        self.client.take();
        self.shared.close();
        peers().lock().remove(&self.id);
    }
}

fn completion(
    future: impl Future<Output = crate::Result<Value>> + Send + 'static,
) -> impl Future<Output = crate::Result<Value>> + Send + 'static {
    future
}

#[tokio::test]
async fn public_begin_call_reaches_c_abi_before_return_and_regular_call_stays_lazy() {
    let peer = Peer::new();
    let lazy = peer.client().call("lazy", None);
    assert!(peer.native.frames.lock().is_empty());
    drop(lazy);

    let method = String::from("first");
    let first = completion(peer.client().begin_call(&method, None).unwrap());
    drop(method);
    let second = completion(peer.client().begin_call("second", None).unwrap());
    let methods: Vec<_> = peer
        .native
        .frames
        .lock()
        .iter()
        .map(|frame| frame["method"].clone())
        .collect();
    assert_eq!(methods, [json!("first"), json!("second")]);
    assert_eq!(first.await.unwrap(), json!({"method": "first"}));
    assert_eq!(second.await.unwrap(), json!({"method": "second"}));
}

#[tokio::test]
async fn public_begin_call_reports_native_write_error_immediately() {
    let peer = Peer::new();
    peer.native.fail.store(true, Ordering::SeqCst);
    let error = match peer.client().begin_call("failed", None) {
        Ok(_) => panic!("native rejection must not return a completion future"),
        Err(error) => error,
    };
    assert_eq!(error.kind(), &crate::ErrorKind::Io);
    assert_eq!(peer.native.frames.lock().len(), 1);
}

#[tokio::test]
async fn completion_does_not_keep_client_or_connection_alive() {
    for method in ["pending", "session.sendAndWait"] {
        let mut peer = Peer::new();
        peer.native.reply.store(false, Ordering::SeqCst);
        let client = peer.client.take().unwrap();
        let weak = Arc::downgrade(&client.inner);
        let pending = completion(client.begin_call(method, None).unwrap());
        drop(client);
        assert!(weak.upgrade().is_none());
        assert!(peer.shared.closed.load(Ordering::SeqCst));
        assert!(
            tokio::time::timeout(Duration::from_secs(5), pending)
                .await
                .unwrap()
                .is_err()
        );
    }
}

struct Release(Option<blocking::Sender<()>>);

impl Drop for Release {
    fn drop(&mut self) {
        if let Some(sender) = self.0.take() {
            let _ = sender.send(());
        }
    }
}

fn assert_submission_does_not_wait_for_native_close(shutdown_phase: bool) {
    let peer = Peer::new();
    let (entered, observed) = blocking::channel();
    let (release, wait) = blocking::channel();
    let gate = if shutdown_phase {
        &peer.native.shutdown_gate
    } else {
        &peer.native.close_gate
    };
    *gate.lock() = Some((entered, wait));
    let release = Release(Some(release));
    let shared = peer.shared.clone();
    let closing = std::thread::spawn(move || shared.close());
    let entered = observed.recv_timeout(Duration::from_secs(5));
    if entered.is_err() {
        drop(release);
        closing.join().unwrap();
        panic!("native close did not reach its controlled gate: {entered:?}");
    }
    let client = peer.client().clone();
    let (finished, result) = blocking::channel();
    let submitting = std::thread::spawn(move || {
        let failed = client.begin_call("during-close", None).is_err();
        let _ = finished.send(failed);
    });
    let before_release = result.recv_timeout(Duration::from_secs(5));
    drop(release);
    closing.join().unwrap();
    submitting.join().unwrap();
    assert_eq!(
        before_release,
        Ok(true),
        "submission waited for native close or shutdown"
    );
    assert!(peer.native.frames.lock().is_empty());
}

#[tokio::test]
async fn synchronous_submission_does_not_wait_for_connection_close() {
    assert_submission_does_not_wait_for_native_close(false);
}

#[tokio::test]
async fn synchronous_submission_does_not_wait_for_host_shutdown() {
    assert_submission_does_not_wait_for_native_close(true);
}
