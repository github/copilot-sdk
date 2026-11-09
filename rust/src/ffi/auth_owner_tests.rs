/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use std::sync::atomic::AtomicUsize;
use std::sync::mpsc as std_mpsc;
use std::time::Duration;

use super::*;

#[derive(Debug, PartialEq)]
enum Event {
    Admission,
    Shutdown,
    OwnerDropped,
}

struct State {
    events: mpsc::UnboundedSender<Event>,
    admission_gate: parking_lot::Mutex<Option<std_mpsc::Receiver<()>>>,
    shutdown_gate: parking_lot::Mutex<Option<std_mpsc::Receiver<()>>>,
    allow_close: AtomicBool,
    allow_open: AtomicBool,
    allow_start: AtomicBool,
    default_starts: AtomicUsize,
    owner_starts: AtomicUsize,
    shutdown_complete: AtomicBool,
}

fn states() -> &'static parking_lot::Mutex<HashMap<u32, Arc<State>>> {
    static STATES: OnceLock<parking_lot::Mutex<HashMap<u32, Arc<State>>>> = OnceLock::new();
    STATES.get_or_init(Default::default)
}

fn state(id: u32) -> Arc<State> {
    Arc::clone(&states().lock()[&id])
}

struct Fixture {
    id: u32,
    state: Arc<State>,
    events: mpsc::UnboundedReceiver<Event>,
}

impl Fixture {
    fn new() -> Self {
        static NEXT_ID: AtomicU32 = AtomicU32::new(1);
        let id = NEXT_ID.fetch_add(1, Ordering::SeqCst);
        let (events, receiver) = mpsc::unbounded_channel();
        let state = Arc::new(State {
            events,
            admission_gate: parking_lot::Mutex::new(None),
            shutdown_gate: parking_lot::Mutex::new(None),
            allow_close: AtomicBool::new(true),
            allow_open: AtomicBool::new(true),
            allow_start: AtomicBool::new(true),
            default_starts: AtomicUsize::new(0),
            owner_starts: AtomicUsize::new(0),
            shutdown_complete: AtomicBool::new(false),
        });
        states().lock().insert(id, Arc::clone(&state));
        Self {
            id,
            state,
            events: receiver,
        }
    }

    fn host(&self, behavior: Option<Behavior>) -> FfiHost {
        FfiHost {
            auth_owner: behavior.map(|behavior| {
                Arc::new(Owner {
                    id: self.id,
                    state: Arc::clone(&self.state),
                    behavior,
                }) as Arc<dyn crate::InProcessAuthOwner>
            }),
            host_start_with_auth_owner: behavior
                .map(|_| start_with_owner as HostStartWithAuthOwnerFn),
            session_binding: None,
            session_connection_open: None,
            library_path: PathBuf::from("controlled-runtime"),
            cli_entrypoint: None,
            integration_id: None,
            environment: Vec::new(),
            args: vec![self.id.to_string()],
            attached_server_id: None,
            host_start: start_default,
            host_shutdown: shutdown,
            connection_open: open,
            connection_write: write,
            connection_close: close,
            connection_set_ui_thread_handler: configure_ui,
            connection_run_ui_action: run_ui,
        }
    }

    async fn expect(&mut self, expected: Event) {
        let actual = tokio::time::timeout(Duration::from_secs(5), self.events.recv())
            .await
            .expect("the controlled native operation did not finish");
        assert_eq!(actual, Some(expected));
    }
}

impl Drop for Fixture {
    fn drop(&mut self) {
        states().lock().remove(&self.id);
    }
}

#[derive(Clone, Copy)]
enum Behavior {
    Accept,
    Reject,
    Panic,
}

struct Owner {
    id: u32,
    state: Arc<State>,
    behavior: Behavior,
}

impl crate::InProcessAuthOwner for Owner {
    fn bind_startup(&self, server: u32, connection: u32) -> crate::Result<()> {
        assert_eq!((server, connection), (self.id, self.id));
        self.state.events.send(Event::Admission).unwrap();
        if let Some(gate) = self.state.admission_gate.lock().take() {
            let _ = gate.recv();
        }
        match self.behavior {
            Behavior::Accept => Ok(()),
            Behavior::Reject => Err(Error::with_message(
                ErrorKind::InvalidConfig,
                "the original owner was revoked",
            )),
            Behavior::Panic => panic!("controlled admission panic"),
        }
    }
}

impl Drop for Owner {
    fn drop(&mut self) {
        let _ = self.state.events.send(Event::OwnerDropped);
    }
}

unsafe fn argv_id(bytes: *const u8, len: usize) -> u32 {
    let argv: Vec<String> =
        serde_json::from_slice(unsafe { std::slice::from_raw_parts(bytes, len) }).unwrap();
    argv[0].parse().unwrap()
}

unsafe extern "C" fn start_default(
    argv: *const u8,
    len: usize,
    _env: *const u8,
    _env_len: usize,
) -> u32 {
    let id = unsafe { argv_id(argv, len) };
    state(id).default_starts.fetch_add(1, Ordering::SeqCst);
    id
}

unsafe extern "C" fn start_with_owner(
    argv: *const u8,
    len: usize,
    _env: *const u8,
    _env_len: usize,
    bind: AuthOwnerCallback,
    data: *mut c_void,
) -> u32 {
    let id = unsafe { argv_id(argv, len) };
    let state = state(id);
    state.owner_starts.fetch_add(1, Ordering::SeqCst);
    if unsafe { bind(data, id, id) } && state.allow_start.load(Ordering::SeqCst) {
        id
    } else {
        0
    }
}

unsafe extern "C" fn shutdown(id: u32) -> bool {
    let state = state(id);
    state.events.send(Event::Shutdown).unwrap();
    if let Some(gate) = state.shutdown_gate.lock().take() {
        let _ = gate.recv();
    }
    state.shutdown_complete.store(true, Ordering::SeqCst);
    true
}

unsafe extern "C" fn open(
    id: u32,
    _callback: OutboundCallback,
    _data: *mut c_void,
    _token: *const u8,
    _token_len: usize,
    _host: *const u8,
    _host_len: usize,
    _agent: *const u8,
    _agent_len: usize,
) -> u32 {
    if state(id).allow_open.load(Ordering::SeqCst) {
        id
    } else {
        0
    }
}

unsafe extern "C" fn close(id: u32) -> bool {
    state(id).allow_close.load(Ordering::SeqCst)
}

unsafe extern "C" fn write(_id: u32, _bytes: *const u8, _len: usize) -> bool {
    true
}

unsafe extern "C" fn configure_ui(_id: u32, _enabled: bool) -> bool {
    true
}

unsafe extern "C" fn run_ui(_id: u32, _action: u32) -> bool {
    true
}

#[tokio::test]
async fn owner_survives_until_native_shutdown_finishes() {
    let mut fixture = Fixture::new();
    let transport = fixture.host(Some(Behavior::Accept)).start().await.unwrap();
    fixture.expect(Event::Admission).await;
    assert!(fixture.events.try_recv().is_err());
    let (release, gate) = std_mpsc::channel();
    *fixture.state.shutdown_gate.lock() = Some(gate);
    let cleanup = tokio::task::spawn_blocking(move || drop(transport));
    fixture.expect(Event::Shutdown).await;
    assert!(fixture.events.try_recv().is_err());
    release.send(()).unwrap();
    fixture.expect(Event::OwnerDropped).await;
    cleanup.await.unwrap();
    assert!(fixture.state.shutdown_complete.load(Ordering::SeqCst));
}

#[tokio::test]
async fn client_stop_releases_owner_even_when_rpc_is_closed() {
    let mut fixture = Fixture::new();
    let directory = tempfile::tempdir().unwrap();
    let transport = fixture.host(Some(Behavior::Accept)).start().await.unwrap();
    let client = crate::Client::from_streams(
        tokio::io::empty(),
        tokio::io::sink(),
        directory.path().to_owned(),
    )
    .unwrap();
    *client.inner.ffi_host.lock() = Some(Arc::clone(&transport.1));
    fixture.expect(Event::Admission).await;
    drop(transport);
    assert!(fixture.events.try_recv().is_err());
    client.inner.rpc.force_close();
    let errors = client
        .stop()
        .await
        .expect_err("the RPC is deliberately closed");
    assert_eq!(errors.0.len(), 1, "{errors:?}");
    assert!(errors.0[0].is_transport_failure(), "{errors:?}");
    fixture.expect(Event::Shutdown).await;
    fixture.expect(Event::OwnerDropped).await;
    assert!(fixture.state.shutdown_complete.load(Ordering::SeqCst));
}

#[tokio::test]
async fn deferred_close_retains_owner_until_shutdown() {
    let mut fixture = Fixture::new();
    fixture.state.allow_close.store(false, Ordering::SeqCst);
    let transport = fixture.host(Some(Behavior::Accept)).start().await.unwrap();
    fixture.expect(Event::Admission).await;
    drop(transport);
    assert!(fixture.events.try_recv().is_err());
    fixture.state.allow_close.store(true, Ordering::SeqCst);
    fixture.expect(Event::Shutdown).await;
    fixture.expect(Event::OwnerDropped).await;
    assert!(fixture.state.shutdown_complete.load(Ordering::SeqCst));
}

#[tokio::test]
async fn cancelled_start_cleans_up_after_synchronous_admission_returns() {
    let mut fixture = Fixture::new();
    let (release, gate) = std_mpsc::channel();
    *fixture.state.admission_gate.lock() = Some(gate);
    let startup = tokio::spawn(fixture.host(Some(Behavior::Accept)).start());
    fixture.expect(Event::Admission).await;
    startup.abort();
    assert!(startup.await.err().unwrap().is_cancelled());
    assert!(fixture.events.try_recv().is_err());
    release.send(()).unwrap();
    fixture.expect(Event::Shutdown).await;
    fixture.expect(Event::OwnerDropped).await;
    assert!(fixture.state.shutdown_complete.load(Ordering::SeqCst));
}

#[tokio::test]
async fn revoked_owner_and_panics_fail_closed_without_default_startup() {
    for (behavior, message) in [
        (Behavior::Reject, "the original owner was revoked"),
        (Behavior::Panic, "SDK startup auth binder panicked"),
    ] {
        let mut fixture = Fixture::new();
        let error = fixture.host(Some(behavior)).start().await.err().unwrap();
        assert!(error.to_string().contains(message), "{error}");
        fixture.expect(Event::Admission).await;
        fixture.expect(Event::OwnerDropped).await;
        assert_eq!(fixture.state.default_starts.load(Ordering::SeqCst), 0);
        assert!(!fixture.state.shutdown_complete.load(Ordering::SeqCst));
    }
}

#[tokio::test]
async fn failed_host_start_releases_owner_without_opening_connection() {
    let mut fixture = Fixture::new();
    fixture.state.allow_start.store(false, Ordering::SeqCst);
    let error = fixture
        .host(Some(Behavior::Accept))
        .start()
        .await
        .err()
        .unwrap();
    assert!(
        error
            .to_string()
            .contains("copilot_runtime_host_start failed"),
        "{error}"
    );
    fixture.expect(Event::Admission).await;
    fixture.expect(Event::OwnerDropped).await;
    assert!(!fixture.state.shutdown_complete.load(Ordering::SeqCst));
}

#[tokio::test]
async fn failed_connection_open_shuts_down_before_owner_release() {
    let mut fixture = Fixture::new();
    fixture.state.allow_open.store(false, Ordering::SeqCst);
    let error = fixture
        .host(Some(Behavior::Accept))
        .start()
        .await
        .err()
        .unwrap();
    assert!(
        error
            .to_string()
            .contains("copilot_runtime_connection_open failed"),
        "{error}"
    );
    fixture.expect(Event::Admission).await;
    fixture.expect(Event::Shutdown).await;
    fixture.expect(Event::OwnerDropped).await;
    assert!(fixture.state.shutdown_complete.load(Ordering::SeqCst));
}

#[tokio::test]
async fn default_start_does_not_need_auth_owner_export() {
    let mut fixture = Fixture::new();
    let transport = fixture.host(None).start().await.unwrap();
    assert_eq!(fixture.state.default_starts.load(Ordering::SeqCst), 1);
    assert_eq!(fixture.state.owner_starts.load(Ordering::SeqCst), 0);
    drop(transport);
    fixture.expect(Event::Shutdown).await;
    assert!(fixture.events.try_recv().is_err());
}

#[tokio::test]
async fn simultaneous_hosts_keep_independent_auth_owners() {
    let mut first = Fixture::new();
    let mut second = Fixture::new();
    let (first_host, second_host) = tokio::join!(
        first.host(Some(Behavior::Accept)).start(),
        second.host(Some(Behavior::Accept)).start(),
    );
    let first_host = first_host.unwrap();
    let second_host = second_host.unwrap();
    first.expect(Event::Admission).await;
    second.expect(Event::Admission).await;
    assert_ne!(first_host.1.server_id(), second_host.1.server_id());
    drop(first_host);
    first.expect(Event::Shutdown).await;
    first.expect(Event::OwnerDropped).await;
    assert_eq!(second_host.1.server_id(), Some(second.id));
    assert!(second.events.try_recv().is_err());
    drop(second_host);
    second.expect(Event::Shutdown).await;
    second.expect(Event::OwnerDropped).await;
}

#[test]
fn missing_auth_owner_export_is_required_only_for_owner_startup() {
    #[cfg(unix)]
    let library: Library = libloading::os::unix::Library::this().into();
    #[cfg(windows)]
    let library: Library = libloading::os::windows::Library::this().unwrap().into();
    let symbol = b"copilot_runtime_host_start_with_auth_owner\0";
    let path = Path::new("runtime-without-auth-owner-export");
    assert!(
        bind_optional::<HostStartWithAuthOwnerFn>(&library, symbol, path, false)
            .unwrap()
            .is_none()
    );
    let error =
        bind_optional::<HostStartWithAuthOwnerFn>(&library, symbol, path, true).unwrap_err();
    assert!(
        error
            .to_string()
            .contains("is missing an expected export (copilot_runtime_host_start_with_auth_owner)"),
        "{error}"
    );
}

#[tokio::test]
async fn auth_owner_rejects_a_conflicting_client_token() {
    let mut options = crate::ClientOptions::new().with_transport(crate::Transport::InProcess);
    options.in_process_auth_owner = Some(Arc::new(|_, _| Ok(())));
    options.github_token = Some("ghs_independent_client_token".to_owned());
    let error = crate::Client::start(options).await.unwrap_err();
    assert!(
        error
            .to_string()
            .contains("a live auth owner cannot be combined with github_token"),
        "{error}"
    );
}

#[tokio::test]
async fn auth_owner_requires_unattached_in_process_transport() {
    use crate::{Client, ClientOptions, Transport};

    for transport in [
        Transport::Default,
        Transport::Stdio,
        Transport::Tcp {
            port: 0,
            connection_token: None,
        },
        Transport::External {
            host: "127.0.0.1".into(),
            port: 0,
            connection_token: None,
        },
    ] {
        let mut options = ClientOptions::new()
            .with_transport(transport)
            .with_env([("COPILOT_SDK_DEFAULT_CONNECTION", "stdio")]);
        options.in_process_auth_owner = Some(Arc::new(|_, _| Ok(())));
        let error = Client::start(options).await.unwrap_err();
        assert!(
            error
                .to_string()
                .contains("a live auth owner requires SDK-owned in-process startup"),
            "{error}"
        );
    }

    for borrowed_session in [false, true] {
        let mut options = ClientOptions::new().with_transport(Transport::InProcess);
        options.in_process_auth_owner = Some(Arc::new(|_, _| Ok(())));
        if borrowed_session {
            options.in_process_session_binding = Some(crate::InProcessSessionBinding {
                parent_connection_id: 1,
                native_session_id: "borrowed".into(),
                registration_token: "registration".into(),
            });
        } else {
            options.in_process_server_id = Some(1);
        }
        let error = Client::start(options).await.unwrap_err();
        assert!(
            error
                .to_string()
                .contains("a live auth owner requires SDK-owned in-process startup"),
            "{error}"
        );
    }
}
