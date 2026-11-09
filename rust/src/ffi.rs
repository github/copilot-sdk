//! In-process FFI transport: hosts the Copilot runtime by loading its native
//! library and speaking JSON-RPC over its C ABI,
//! instead of spawning a CLI child process and communicating over stdio/TCP.
//!
//! The runtime's `host_start` export constructs the Rust server synchronously in
//! this process. JSON-RPC frames are pumped across the ABI: writes go to
//! `connection_write`; inbound frames arrive on a native callback that feeds an
//! async reader. The framing is unchanged — the same LSP `Content-Length:`
//! frames the stdio transport uses.

use std::collections::HashMap;
use std::ffi::c_void;
use std::path::{Path, PathBuf};
use std::pin::Pin;
use std::sync::atomic::{AtomicBool, AtomicPtr, AtomicU32, Ordering};
use std::sync::{Arc, OnceLock};
use std::task::{Context, Poll};

use libloading::Library;
use tokio::io::{AsyncRead, ReadBuf};
use tokio::sync::mpsc;
use tracing::{debug, warn};

use crate::{Error, ErrorKind};

type OutboundCallback = unsafe extern "C" fn(*mut c_void, *const u8, usize);
type HostStartFn = unsafe extern "C" fn(*const u8, usize, *const u8, usize) -> u32;
type AuthOwnerCallback = unsafe extern "C" fn(*mut c_void, u32, u32) -> bool;
type HostStartWithAuthOwnerFn =
    unsafe extern "C" fn(*const u8, usize, *const u8, usize, AuthOwnerCallback, *mut c_void) -> u32;
type HostShutdownFn = unsafe extern "C" fn(u32) -> bool;
#[allow(clippy::type_complexity)]
type ConnectionOpenFn = unsafe extern "C" fn(
    u32,
    OutboundCallback,
    *mut c_void,
    *const u8,
    usize,
    *const u8,
    usize,
    *const u8,
    usize,
) -> u32;
type ConnectionWriteFn = unsafe extern "C" fn(u32, *const u8, usize) -> bool;
type ConnectionCloseFn = unsafe extern "C" fn(u32) -> bool;
type SessionConnectionOpenFn = unsafe extern "C" fn(
    u32,
    *const u8,
    usize,
    *const u8,
    usize,
    OutboundCallback,
    *mut c_void,
) -> u32;
type ConnectionSetUiThreadHandlerFn = unsafe extern "C" fn(u32, bool) -> bool;
type ConnectionRunUiActionFn = unsafe extern "C" fn(u32, u32) -> bool;

/// State handed to the native side as `user_data` so the outbound callback can
/// route inbound frames back to the reader.
struct CallbackState {
    tx: mpsc::UnboundedSender<Vec<u8>>,
    closing: AtomicBool,
}

extern "C" fn on_outbound(user_data: *mut c_void, bytes: *const u8, len: usize) {
    if user_data.is_null() || bytes.is_null() || len == 0 {
        return;
    }
    let state = unsafe { &*(user_data as *const CallbackState) };
    if state.closing.load(Ordering::SeqCst) {
        return;
    }
    let slice = unsafe { std::slice::from_raw_parts(bytes, len) };
    let _ = state.tx.send(slice.to_vec());
}

/// Bound exports and connection lifecycle state, shared between the
/// synchronous frame sender and the owning [`Client`]. The cdylib itself is loaded
/// process-globally and never unloaded (see [`load_library`]), so this holds
/// only the bound fn pointers and connection state.
pub(crate) struct FfiShared {
    auth_owner: parking_lot::Mutex<Option<Arc<dyn crate::InProcessAuthOwner>>>,
    host_shutdown: HostShutdownFn,
    owns_host: bool,
    connection_write: ConnectionWriteFn,
    connection_close: ConnectionCloseFn,
    connection_set_ui_thread_handler: ConnectionSetUiThreadHandlerFn,
    connection_run_ui_action: ConnectionRunUiActionFn,
    server_id: AtomicU32,
    connection_id: AtomicU32,
    callback_state: AtomicPtr<CallbackState>,
    closed: AtomicBool,
    operation_lock: parking_lot::Mutex<()>,
    library_path: PathBuf,
}

// The raw fn pointers and the boxed callback state are safe to move across
// threads: the native side copies buffers synchronously and the callback only
// forwards to a thread-safe channel sender.
unsafe impl Send for FfiShared {}
unsafe impl Sync for FfiShared {}

impl FfiShared {
    pub(crate) fn server_id(&self) -> Option<u32> {
        let server_id = self.server_id.load(Ordering::SeqCst);
        (!self.closed.load(Ordering::SeqCst) && server_id != 0).then_some(server_id)
    }

    pub(crate) fn configure_ui_thread(&self, enabled: bool) -> Result<(), Error> {
        let connection = self.connection_id.load(Ordering::SeqCst);
        if !self.closed.load(Ordering::SeqCst)
            && unsafe { (self.connection_set_ui_thread_handler)(connection, enabled) }
        {
            Ok(())
        } else {
            Err(Error::with_message(
                ErrorKind::InvalidConfig,
                "failed to configure the in-process UI dispatcher",
            ))
        }
    }

    pub(crate) fn run_ui_action(&self, action_id: u32) -> Result<(), Error> {
        let connection = self.connection_id.load(Ordering::SeqCst);
        // No operation lock across native UI: another thread must still be able
        // to close the connection and request cancellation.
        if !self.closed.load(Ordering::SeqCst)
            && unsafe { (self.connection_run_ui_action)(connection, action_id) }
        {
            Ok(())
        } else {
            Err(Error::with_message(
                ErrorKind::InvalidConfig,
                "native UI action is retired, already executed, or not on the process main thread",
            ))
        }
    }

    pub(crate) fn library_path(&self) -> &Path {
        &self.library_path
    }

    pub(crate) fn owns_host(&self) -> bool {
        self.owns_host
    }

    pub(crate) fn connection_id(&self) -> Option<u32> {
        let id = self.connection_id.load(Ordering::SeqCst);
        (id != 0 && !self.closed.load(Ordering::SeqCst)).then_some(id)
    }

    /// Close the connection, shut down an owned host, and free the callback state.
    /// Idempotent; called from [`Client::stop`], drop, and on startup failure.
    pub(crate) fn close(&self) {
        let (conn, server, state) = {
            let _operation = self.operation_lock.lock();
            if self.closed.swap(true, Ordering::SeqCst) {
                return;
            }
            let state = self
                .callback_state
                .swap(std::ptr::null_mut(), Ordering::SeqCst);
            if !state.is_null() {
                unsafe { &*state }.closing.store(true, Ordering::SeqCst);
            }
            (
                self.connection_id.swap(0, Ordering::SeqCst),
                self.server_id.swap(0, Ordering::SeqCst),
                state as usize,
            )
        };
        // New sends now fail without waiting for native close or host shutdown.
        // The callback state remains alive until native callbacks are quiescent.
        if conn != 0 {
            let quiesced = unsafe { (self.connection_close)(conn) };
            if !quiesced {
                let connection_close = self.connection_close;
                let host_shutdown = self.host_shutdown;
                let owns_host = self.owns_host;
                let auth_owner = Arc::new(self.auth_owner.lock().take());
                let cleanup_auth_owner = Arc::clone(&auth_owner);
                let library_path = self.library_path.clone();
                if let Err(error) = std::thread::Builder::new()
                    .name("copilot-ffi-cleanup".to_owned())
                    .spawn(move || {
                        let _auth_owner = cleanup_auth_owner;
                        while !unsafe { connection_close(conn) } {
                            std::thread::sleep(std::time::Duration::from_millis(100));
                        }
                        release_callback_state(state);
                        if owns_host && server != 0 && !unsafe { host_shutdown(server) } {
                            warn!(
                                library = %library_path.display(),
                                server_id = server,
                                "FFI runtime host shutdown did not recognize server"
                            );
                        }
                        debug!(library = %library_path.display(), "FFI runtime connection closed");
                    })
                {
                    // The host remains live if no cleanup worker can start.
                    std::mem::forget(auth_owner);
                    warn!(
                        error = %error,
                        library = %self.library_path.display(),
                        "failed to start deferred FFI cleanup thread; callback state retained"
                    );
                }
                return;
            }
        }
        release_callback_state(state);
        if self.owns_host && server != 0 && !unsafe { (self.host_shutdown)(server) } {
            warn!(
                library = %self.library_path.display(),
                server_id = server,
                "FFI runtime host shutdown did not recognize server"
            );
        }
        self.auth_owner.lock().take();
        debug!(library = %self.library_path.display(), "FFI runtime connection closed");
    }

    pub(crate) fn write_frame(&self, frame: &[u8]) -> std::io::Result<()> {
        let _operation = self.operation_lock.lock();
        let conn = self.connection_id.load(Ordering::SeqCst);
        if !self.closed.load(Ordering::SeqCst)
            && conn != 0
            && unsafe { (self.connection_write)(conn, frame.as_ptr(), frame.len()) }
        {
            Ok(())
        } else {
            Err(std::io::Error::new(
                std::io::ErrorKind::BrokenPipe,
                "failed to write a frame to the in-process runtime connection",
            ))
        }
    }
}

pub(crate) struct HostSessionConstruction {
    pub(crate) token: u32,
    connection_id: u32,
    release: unsafe extern "C" fn(u32, u32) -> bool,
}

impl Drop for HostSessionConstruction {
    fn drop(&mut self) {
        // SAFETY: the library cache retains this function for the process lifetime.
        unsafe { (self.release)(self.connection_id, self.token) };
    }
}

pub(crate) fn prepare_host_session_construction(
    library_path: &Path,
    connection_id: u32,
    payload: &str,
) -> Result<HostSessionConstruction, Error> {
    let library = load_library(library_path)?;
    let prepare = *bind::<unsafe extern "C" fn(u32, *const u8, usize) -> u32>(
        library,
        b"copilot_runtime_prepare_host_session_construction\0",
        library_path,
    )?;
    let release = *bind::<unsafe extern "C" fn(u32, u32) -> bool>(
        library,
        b"copilot_runtime_release_host_session_construction\0",
        library_path,
    )?;
    // SAFETY: the runtime copies this initialized UTF-8 slice before the call returns.
    let token = unsafe { prepare(connection_id, payload.as_ptr(), payload.len()) };
    if token == 0 {
        return Err(Error::with_message(
            ErrorKind::InvalidConfig,
            "the in-process connection cannot prepare host session construction",
        ));
    }
    Ok(HostSessionConstruction {
        token,
        connection_id,
        release,
    })
}

fn release_callback_state(state: usize) {
    if state == 0 {
        return;
    }
    let state = state as *mut CallbackState;
    drop(unsafe { Box::from_raw(state) });
}

type HostResultCallback = unsafe extern "C" fn(*mut c_void, *const u8, usize);
type HostPrepareFn =
    unsafe extern "C" fn(u32, *const u8, usize, HostResultCallback, *mut c_void) -> bool;
type HostInitializeFn = unsafe extern "C" fn(u32, bool, HostResultCallback, *mut c_void) -> bool;
type HostReleaseFn = unsafe extern "C" fn(u32) -> bool;
type HostBeginPermissionUpdateFn = unsafe extern "C" fn(u32) -> u32;

/// A pending permission update on one original in-process host registration.
#[doc(hidden)]
pub struct InProcessPermissionUpdate {
    ticket: u32,
    finish: HostReleaseFn,
}

impl Drop for InProcessPermissionUpdate {
    fn drop(&mut self) {
        unsafe { (self.finish)(self.ticket) };
    }
}

/// Callback authority for a native resource that the SDK already owns.
#[doc(hidden)]
pub struct InProcessSessionHost {
    lease: AtomicU32,
    channel: u32,
    token: String,
    generation: String,
    initialize: HostInitializeFn,
    release: HostReleaseFn,
    begin_permission_update: HostBeginPermissionUpdateFn,
    finish_permission_update: HostReleaseFn,
}

fn host_result(bytes: &[u8]) -> Result<serde_json::Value, Error> {
    let envelope: serde_json::Value = serde_json::from_slice(bytes)
        .map_err(|error| Error::with_message(ErrorKind::InvalidConfig, error.to_string()))?;
    if envelope["ok"] != true {
        return Err(Error::with_message(
            ErrorKind::InvalidConfig,
            envelope["error"]["message"]
                .as_str()
                .unwrap_or("Invalid SDK host result")
                .to_owned(),
        ));
    }
    Ok(envelope["result"].clone())
}

unsafe extern "C" fn host_prepare_result(data: *mut c_void, bytes: *const u8, len: usize) {
    let result = unsafe { &mut *data.cast::<Option<Result<serde_json::Value, Error>>>() };
    *result = Some(host_result(unsafe {
        std::slice::from_raw_parts(bytes, len)
    }));
}

unsafe extern "C" fn host_initialize_result(data: *mut c_void, bytes: *const u8, len: usize) {
    let sender = unsafe {
        Box::from_raw(data.cast::<tokio::sync::oneshot::Sender<Result<serde_json::Value, Error>>>())
    };
    let _ = sender.send(host_result(unsafe {
        std::slice::from_raw_parts(bytes, len)
    }));
}

pub(crate) fn prepare_session_host(
    path: &Path,
    connection_id: u32,
    payload: &str,
) -> Result<InProcessSessionHost, Error> {
    let library = load_library(path)?;
    let prepare = *bind::<HostPrepareFn>(library, b"copilot_runtime_session_host_prepare\0", path)?;
    let initialize =
        *bind::<HostInitializeFn>(library, b"copilot_runtime_session_host_initialize\0", path)?;
    let release = *bind::<HostReleaseFn>(library, b"copilot_runtime_session_host_release\0", path)?;
    let begin_permission_update = *bind::<HostBeginPermissionUpdateFn>(
        library,
        b"copilot_runtime_session_host_begin_permission_update\0",
        path,
    )?;
    let finish_permission_update = *bind::<HostReleaseFn>(
        library,
        b"copilot_runtime_session_host_finish_permission_update\0",
        path,
    )?;
    let mut result: Option<Result<serde_json::Value, Error>> = None;
    // The prepare callback runs before this C ABI call returns.
    let accepted = unsafe {
        prepare(
            connection_id,
            payload.as_ptr(),
            payload.len(),
            host_prepare_result,
            (&mut result as *mut Option<Result<serde_json::Value, Error>>).cast(),
        )
    };
    if !accepted {
        return Err(Error::with_message(
            ErrorKind::InvalidConfig,
            "The SDK host preparation was rejected",
        ));
    }
    let result = result.ok_or_else(|| {
        Error::with_message(ErrorKind::InvalidConfig, "The SDK host returned no result")
    })??;
    let lease = result["lease"]
        .as_u64()
        .and_then(|value| u32::try_from(value).ok())
        .ok_or_else(|| {
            Error::with_message(ErrorKind::InvalidConfig, "The SDK host returned no lease")
        })?;
    Ok(InProcessSessionHost {
        lease: AtomicU32::new(lease),
        channel: result["hostChannel"]
            .as_u64()
            .and_then(|value| u32::try_from(value).ok())
            .ok_or_else(|| {
                Error::with_message(ErrorKind::InvalidConfig, "The SDK host returned no channel")
            })?,
        token: result["registrationToken"]
            .as_str()
            .ok_or_else(|| {
                Error::with_message(ErrorKind::InvalidConfig, "The SDK host returned no token")
            })?
            .to_owned(),
        generation: result["generation"]
            .as_str()
            .ok_or_else(|| {
                Error::with_message(
                    ErrorKind::InvalidConfig,
                    "The SDK host returned no generation",
                )
            })?
            .to_owned(),
        initialize,
        release,
        begin_permission_update,
        finish_permission_update,
    })
}

impl InProcessSessionHost {
    /// Register pending setup before scheduling the SDK setter.
    ///
    /// The ticket settles on drop, including cancellation and SDK errors.
    /// This call takes only short in-memory locks, never native session locks.
    pub fn begin_permission_update(&self) -> Result<InProcessPermissionUpdate, Error> {
        let ticket = unsafe { (self.begin_permission_update)(self.lease.load(Ordering::SeqCst)) };
        if ticket == 0 {
            return Err(Error::with_message(
                ErrorKind::InvalidConfig,
                "The SDK host permission update was rejected",
            ));
        }
        Ok(InProcessPermissionUpdate {
            ticket,
            finish: self.finish_permission_update,
        })
    }

    /// The original callback channel.
    pub fn host_channel(&self) -> u32 {
        self.channel
    }

    /// The callback registration capability.
    pub fn registration_token(&self) -> &str {
        &self.token
    }

    /// The native resource generation.
    pub fn generation(&self) -> &str {
        &self.generation
    }

    /// Finish the original facade's callback and model setup.
    pub async fn initialize(&self, exit_plan_mode: bool) -> Result<(), Error> {
        let (sender, receiver) =
            tokio::sync::oneshot::channel::<Result<serde_json::Value, Error>>();
        let data = Box::into_raw(Box::new(sender));
        // An accepted request owns the sender until its single terminal callback.
        let accepted = unsafe {
            (self.initialize)(
                self.lease.load(Ordering::SeqCst),
                exit_plan_mode,
                host_initialize_result,
                data.cast(),
            )
        };
        if !accepted {
            drop(unsafe { Box::from_raw(data) });
            return Err(Error::with_message(
                ErrorKind::InvalidConfig,
                "The SDK host initialization was rejected",
            ));
        }
        receiver
            .await
            .map_err(|error| Error::with_message(ErrorKind::InvalidConfig, error.to_string()))??;
        Ok(())
    }

    /// Release only this facade's callbacks, never its native session.
    pub fn release(&self) {
        let lease = self.lease.swap(0, Ordering::SeqCst);
        if lease != 0 {
            unsafe { (self.release)(lease) };
        }
    }
}

impl Drop for InProcessSessionHost {
    fn drop(&mut self) {
        self.release();
    }
}
impl Drop for FfiShared {
    fn drop(&mut self) {
        self.close();
    }
}

/// Read side of the FFI transport, fed by the native outbound callback via an
/// unbounded channel. Implements [`AsyncRead`] for the JSON-RPC read loop.
pub(crate) struct FfiReader {
    rx: mpsc::UnboundedReceiver<Vec<u8>>,
    leftover: Vec<u8>,
    pos: usize,
}

impl AsyncRead for FfiReader {
    fn poll_read(
        mut self: Pin<&mut Self>,
        cx: &mut Context<'_>,
        buf: &mut ReadBuf<'_>,
    ) -> Poll<std::io::Result<()>> {
        if self.pos >= self.leftover.len() {
            match self.rx.poll_recv(cx) {
                Poll::Ready(Some(chunk)) => {
                    self.leftover = chunk;
                    self.pos = 0;
                }
                Poll::Ready(None) => return Poll::Ready(Ok(())),
                Poll::Pending => return Poll::Pending,
            }
        }
        let available = self.leftover.len() - self.pos;
        let n = available.min(buf.remaining());
        let start = self.pos;
        buf.put_slice(&self.leftover[start..start + n]);
        self.pos += n;
        Poll::Ready(Ok(()))
    }
}

/// Prepared FFI host. The cdylib is loaded process-globally and never unloaded
/// (see [`load_library`]).
pub(crate) struct FfiHost {
    auth_owner: Option<Arc<dyn crate::InProcessAuthOwner>>,
    host_start_with_auth_owner: Option<HostStartWithAuthOwnerFn>,
    session_binding: Option<crate::InProcessSessionBinding>,
    session_connection_open: Option<SessionConnectionOpenFn>,
    library_path: PathBuf,
    cli_entrypoint: Option<PathBuf>,
    integration_id: Option<String>,
    environment: Vec<(String, String)>,
    args: Vec<String>,
    attached_server_id: Option<u32>,
    host_start: HostStartFn,
    host_shutdown: HostShutdownFn,
    connection_open: ConnectionOpenFn,
    connection_write: ConnectionWriteFn,
    connection_close: ConnectionCloseFn,
    connection_set_ui_thread_handler: ConnectionSetUiThreadHandlerFn,
    connection_run_ui_action: ConnectionRunUiActionFn,
}

pub(crate) struct FfiHostTarget<'a> {
    pub auth_owner: Option<Arc<dyn crate::InProcessAuthOwner>>,
    pub server_id: Option<u32>,
    pub library_path: Option<&'a Path>,
    pub session_binding: Option<crate::InProcessSessionBinding>,
}

// SAFETY: as for `FfiShared` — the bound exports are plain fn pointers, safe to
// move to the blocking thread that starts the host.
unsafe impl Send for FfiHost {}

impl FfiHost {
    /// Load the selected cdylib, defaulting to a sibling of `runtime_entrypoint`.
    pub(crate) fn create(
        runtime_entrypoint: Option<&Path>,
        cli_entrypoint: Option<&Path>,
        integration_id: Option<&str>,
        environment: Vec<(String, String)>,
        args: Vec<String>,
        target: FfiHostTarget<'_>,
    ) -> Result<Self, Error> {
        let FfiHostTarget {
            auth_owner,
            server_id: attached_server_id,
            library_path: runtime_library_path,
            session_binding,
        } = target;
        if session_binding.is_some() && attached_server_id.is_none() {
            return Err(Error::with_message(
                ErrorKind::InvalidConfig,
                "a borrowed session requires an attached in-process server",
            ));
        }
        let cli_entrypoint = cli_entrypoint
            .map(std::fs::canonicalize)
            .transpose()
            .map_err(|e| {
                Error::with_message(
                    ErrorKind::InvalidConfig,
                    format!("failed to resolve explicit in-process CLI entrypoint: {e}"),
                )
            })?
            .map(path_for_child_process);
        let selected_library = match (attached_server_id, runtime_library_path) {
            (_, Some(path)) => path.to_path_buf(),
            (Some(_), None) => {
                return Err(Error::with_message(
                    ErrorKind::InvalidConfig,
                    "an attached server requires the runtime image already loaded by its host",
                ));
            }
            (None, None) => {
                let runtime_entrypoint = runtime_entrypoint.ok_or_else(|| {
                    Error::with_message(
                        ErrorKind::InvalidConfig,
                        "in-process transport requires a runtime image or CLI entrypoint",
                    )
                })?;
                let runtime_entrypoint =
                    std::fs::canonicalize(runtime_entrypoint).map_err(|e| {
                        Error::with_message(
                            ErrorKind::InvalidConfig,
                            format!(
                                "failed to resolve in-process runtime entrypoint '{}': {e}",
                                runtime_entrypoint.display()
                            ),
                        )
                    })?;
                resolve_library_path(&runtime_entrypoint)?
            }
        };
        let library_path = std::fs::canonicalize(selected_library).map_err(|e| {
            Error::with_message(
                ErrorKind::InvalidConfig,
                format!("failed to resolve in-process runtime library: {e}"),
            )
        })?;
        let lib = load_library(&library_path)?;
        let host_start_with_auth_owner = bind_optional::<HostStartWithAuthOwnerFn>(
            lib,
            b"copilot_runtime_host_start_with_auth_owner\0",
            &library_path,
            auth_owner.is_some(),
        )?;
        let session_connection_open = bind_optional::<SessionConnectionOpenFn>(
            lib,
            b"copilot_runtime_session_connection_open\0",
            &library_path,
            session_binding.is_some(),
        )?;

        let host_start = *bind::<HostStartFn>(lib, b"copilot_runtime_host_start\0", &library_path)?;
        let host_shutdown =
            *bind::<HostShutdownFn>(lib, b"copilot_runtime_host_shutdown\0", &library_path)?;
        let connection_open =
            *bind::<ConnectionOpenFn>(lib, b"copilot_runtime_connection_open\0", &library_path)?;
        let connection_write =
            *bind::<ConnectionWriteFn>(lib, b"copilot_runtime_connection_write\0", &library_path)?;
        let connection_close =
            *bind::<ConnectionCloseFn>(lib, b"copilot_runtime_connection_close\0", &library_path)?;
        let connection_set_ui_thread_handler = *bind::<ConnectionSetUiThreadHandlerFn>(
            lib,
            b"copilot_runtime_connection_set_ui_thread_handler\0",
            &library_path,
        )?;
        let connection_run_ui_action = *bind::<ConnectionRunUiActionFn>(
            lib,
            b"copilot_runtime_connection_run_ui_action\0",
            &library_path,
        )?;

        Ok(Self {
            auth_owner,
            host_start_with_auth_owner,
            session_binding,
            session_connection_open,
            library_path,
            cli_entrypoint,
            integration_id: integration_id.map(str::to_owned),
            environment,
            args,
            attached_server_id,
            host_start,
            host_shutdown,
            connection_open,
            connection_write,
            connection_close,
            connection_set_ui_thread_handler,
            connection_run_ui_action,
        })
    }

    /// Start the native runtime and open the FFI JSON-RPC connection.
    ///
    /// Cancellation leaves the blocking task to finish. Its unclaimed transport
    /// closes on drop, retaining the auth owner through native cleanup.
    pub(crate) async fn start(self) -> Result<(FfiReader, Arc<FfiShared>), Error> {
        tokio::task::spawn_blocking(move || self.start_blocking())
            .await
            .map_err(|e| {
                Error::with_message(
                    ErrorKind::InvalidConfig,
                    format!("in-process runtime startup task failed: {e}"),
                )
            })?
    }

    fn start_blocking(self) -> Result<(FfiReader, Arc<FfiShared>), Error> {
        let argv = build_argv_json(
            self.cli_entrypoint.as_deref(),
            self.integration_id.as_deref(),
            &self.args,
        );
        let env = build_env_json(&self.environment);

        let (env_ptr, env_len) = match &env {
            Some(bytes) => (bytes.as_ptr(), bytes.len()),
            None => (std::ptr::null(), 0),
        };

        let owns_host = self.attached_server_id.is_none();
        let server_id = match self.attached_server_id {
            Some(server_id) => server_id,
            None => {
                if let (Some(owner), Some(start)) =
                    (self.auth_owner.as_ref(), self.host_start_with_auth_owner)
                {
                    struct Admission<'a> {
                        owner: &'a dyn crate::InProcessAuthOwner,
                        error: parking_lot::Mutex<Option<Error>>,
                    }
                    unsafe extern "C" fn bind(
                        state: *mut c_void,
                        server: u32,
                        connection: u32,
                    ) -> bool {
                        // SAFETY: startup retains this borrowed state until the call returns.
                        let admission = unsafe { &*state.cast::<Admission<'_>>() };
                        let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
                            admission.owner.bind_startup(server, connection)
                        }));
                        match result {
                            Ok(Ok(())) => true,
                            Ok(Err(error)) => {
                                *admission.error.lock() = Some(error);
                                false
                            }
                            Err(_) => {
                                *admission.error.lock() = Some(Error::with_message(
                                    ErrorKind::InvalidConfig,
                                    "SDK startup auth binder panicked",
                                ));
                                false
                            }
                        }
                    }
                    let mut admission = Admission {
                        owner: owner.as_ref(),
                        error: parking_lot::Mutex::new(None),
                    };
                    // SAFETY: byte spans and callback state remain live throughout startup.
                    let server = unsafe {
                        start(
                            argv.as_ptr(),
                            argv.len(),
                            env_ptr,
                            env_len,
                            bind,
                            (&mut admission as *mut Admission<'_>).cast(),
                        )
                    };
                    if let Some(error) = admission.error.into_inner() {
                        return Err(error);
                    }
                    server
                } else {
                    unsafe { (self.host_start)(argv.as_ptr(), argv.len(), env_ptr, env_len) }
                }
            }
        };

        if server_id == 0 {
            return Err(Error::with_message(
                ErrorKind::InvalidConfig,
                format!(
                    "copilot_runtime_host_start failed (library '{}')",
                    self.library_path.display()
                ),
            ));
        }

        let (tx, rx) = mpsc::unbounded_channel::<Vec<u8>>();
        let state_ptr = Box::into_raw(Box::new(CallbackState {
            tx,
            closing: AtomicBool::new(false),
        }));
        let connection_id = if let Some(binding) = self.session_binding.as_ref() {
            let open = self.session_connection_open.ok_or_else(|| {
                Error::with_message(
                    ErrorKind::InvalidConfig,
                    "session binding export is missing",
                )
            })?;
            unsafe {
                open(
                    binding.parent_connection_id,
                    binding.native_session_id.as_ptr(),
                    binding.native_session_id.len(),
                    binding.registration_token.as_ptr(),
                    binding.registration_token.len(),
                    on_outbound,
                    state_ptr as *mut c_void,
                )
            }
        } else {
            unsafe {
                (self.connection_open)(
                    server_id,
                    on_outbound,
                    state_ptr as *mut c_void,
                    std::ptr::null(),
                    0,
                    std::ptr::null(),
                    0,
                    std::ptr::null(),
                    0,
                )
            }
        };
        if connection_id == 0 {
            drop(unsafe { Box::from_raw(state_ptr) });
            if owns_host {
                unsafe { (self.host_shutdown)(server_id) };
            }
            return Err(Error::with_message(
                ErrorKind::InvalidConfig,
                "copilot_runtime_connection_open failed",
            ));
        }

        let shared = Arc::new(FfiShared {
            auth_owner: parking_lot::Mutex::new(self.auth_owner),
            host_shutdown: self.host_shutdown,
            owns_host,
            connection_write: self.connection_write,
            connection_close: self.connection_close,
            connection_set_ui_thread_handler: self.connection_set_ui_thread_handler,
            connection_run_ui_action: self.connection_run_ui_action,
            server_id: AtomicU32::new(server_id),
            connection_id: AtomicU32::new(connection_id),
            callback_state: AtomicPtr::new(state_ptr),
            closed: AtomicBool::new(false),
            operation_lock: parking_lot::Mutex::new(()),
            library_path: self.library_path.clone(),
        });

        debug!(
            library = %self.library_path.display(),
            server_id, connection_id, "FFI runtime host started"
        );

        let reader = FfiReader {
            rx,
            leftover: Vec::new(),
            pos: 0,
        };
        Ok((reader, shared))
    }
}

fn bind<'lib, T>(
    lib: &'lib Library,
    symbol: &[u8],
    library_path: &Path,
) -> Result<libloading::Symbol<'lib, T>, Error> {
    match unsafe { lib.get::<T>(symbol) } {
        Ok(export) => Ok(export),
        Err(e) => Err(Error::with_message(
            ErrorKind::InvalidConfig,
            format!(
                "in-process runtime library '{}' is missing an expected export ({}): {e}",
                library_path.display(),
                String::from_utf8_lossy(symbol.strip_suffix(b"\0").unwrap_or(symbol))
            ),
        )),
    }
}

fn bind_optional<T: Copy>(
    lib: &Library,
    symbol: &[u8],
    library_path: &Path,
    enabled: bool,
) -> Result<Option<T>, Error> {
    if enabled {
        Ok(Some(*bind::<T>(lib, symbol, library_path)?))
    } else {
        Ok(None)
    }
}

/// Loads the runtime cdylib once per process and never unloads it, returning a
/// `'static` reference. Subsequent loads of the same path reuse the first
/// handle.
///
/// The library stays mapped because native worker threads can outlive an
/// individual connection teardown.
fn load_library(library_path: &Path) -> Result<&'static Library, Error> {
    static LIBRARIES: OnceLock<parking_lot::Mutex<HashMap<PathBuf, &'static Library>>> =
        OnceLock::new();
    let cache = LIBRARIES.get_or_init(|| parking_lot::Mutex::new(HashMap::new()));

    let mut guard = cache.lock();
    if let Some(lib) = guard.get(library_path) {
        return Ok(*lib);
    }

    let lib = unsafe { Library::new(library_path) }.map_err(|e| {
        Error::with_message(
            ErrorKind::InvalidConfig,
            format!(
                "failed to load in-process runtime library '{}': {e}",
                library_path.display()
            ),
        )
    })?;
    // Leak the library so it is never unloaded for the process lifetime.
    let leaked: &'static Library = Box::leak(Box::new(lib));
    guard.insert(library_path.to_path_buf(), leaked);
    Ok(leaked)
}

/// The natural platform shared-library file name for the runtime cdylib — the
/// `.node` file renamed to what the Rust cdylib would be called on this OS.
fn natural_library_name() -> &'static str {
    if cfg!(windows) {
        "copilot_runtime.dll"
    } else if cfg!(target_os = "macos") {
        "libcopilot_runtime.dylib"
    } else {
        "libcopilot_runtime.so"
    }
}

/// The package prebuild folder name for the current host.
pub(crate) fn prebuilds_folder() -> Option<String> {
    let platform = if cfg!(target_os = "windows") {
        "win32"
    } else if cfg!(target_os = "macos") {
        "darwin"
    } else if cfg!(all(target_os = "linux", target_env = "musl")) {
        "linuxmusl"
    } else if cfg!(target_os = "linux") {
        "linux"
    } else {
        return None;
    };
    let arch = if cfg!(target_arch = "x86_64") {
        "x64"
    } else if cfg!(target_arch = "aarch64") {
        "arm64"
    } else {
        return None;
    };
    Some(format!("{platform}-{arch}"))
}

fn resolve_library_path(entrypoint: &Path) -> Result<PathBuf, Error> {
    let dir = entrypoint.parent().ok_or_else(|| {
        Error::with_message(
            ErrorKind::InvalidConfig,
            format!(
                "could not determine directory for CLI entrypoint '{}'",
                entrypoint.display()
            ),
        )
    })?;

    // Bundled/flat layout: natural shared-library name next to the CLI.
    let flat = dir.join(natural_library_name());
    if flat.is_file() {
        return Ok(flat);
    }

    let adjacent = dir.join("runtime.node");
    if adjacent.is_file() {
        return Ok(adjacent);
    }

    // Development package layout.
    let prebuilds =
        prebuilds_folder().map(|folder| dir.join("prebuilds").join(folder).join("runtime.node"));
    if let Some(prebuilds_path) = &prebuilds
        && prebuilds_path.is_file()
    {
        return Ok(prebuilds_path.clone());
    }

    Err(Error::with_message(
        ErrorKind::BinaryNotFound {
            name: natural_library_name().into(),
            hint: Some(format!(
                "native runtime library not found next to '{}'. Enable the \
                 `in-process` feature or set COPILOT_CLI_PATH to a compatible CLI package.",
                entrypoint.display()
            )),
        },
        "native runtime library not found",
    ))
}

#[cfg(windows)]
fn path_for_child_process(path: PathBuf) -> PathBuf {
    use std::ffi::OsString;
    use std::os::windows::ffi::{OsStrExt, OsStringExt};

    const VERBATIM_PREFIX: &[u16] = &[b'\\' as u16, b'\\' as u16, b'?' as u16, b'\\' as u16];
    const UNC_PREFIX: &[u16] = &[b'U' as u16, b'N' as u16, b'C' as u16, b'\\' as u16];

    let encoded: Vec<u16> = path.as_os_str().encode_wide().collect();
    let Some(stripped) = encoded.strip_prefix(VERBATIM_PREFIX) else {
        return path;
    };
    let normalized = if let Some(unc_path) = stripped.strip_prefix(UNC_PREFIX) {
        let mut result = vec![b'\\' as u16, b'\\' as u16];
        result.extend_from_slice(unc_path);
        result
    } else {
        stripped.to_vec()
    };
    PathBuf::from(OsString::from_wide(&normalized))
}

#[cfg(not(windows))]
fn path_for_child_process(path: PathBuf) -> PathBuf {
    path
}

fn build_argv_json(
    entrypoint: Option<&Path>,
    integration_id: Option<&str>,
    extra_args: &[String],
) -> Vec<u8> {
    let mut argv = Vec::new();
    if let Some(entrypoint) = entrypoint {
        let entrypoint_str = entrypoint.to_string_lossy().into_owned();
        if entrypoint
            .extension()
            .and_then(|ext| ext.to_str())
            .is_some_and(|ext| ext.eq_ignore_ascii_case("js"))
        {
            argv.push("node".to_string());
        }
        argv.extend([
            entrypoint_str,
            "--embedded-host".to_string(),
            "--no-auto-update".to_string(),
        ]);
    }
    if let Some(integration_id) = integration_id {
        argv.push(format!("--cli-login-sync-integration-id={integration_id}"));
    }
    argv.extend_from_slice(extra_args);
    serde_json::to_vec(&argv).expect("argv serializes")
}

fn build_env_json(environment: &[(String, String)]) -> Option<Vec<u8>> {
    if environment.is_empty() {
        return None;
    }
    let map: serde_json::Map<String, serde_json::Value> = std::env::vars_os()
        .filter_map(|(key, value)| {
            let key = key.into_string().ok()?;
            let value = match value.into_string() {
                Ok(value) => value,
                Err(_) => {
                    let name = if cfg!(windows) {
                        key.to_uppercase()
                    } else {
                        key.clone()
                    };
                    if !matches!(
                        name.as_str(),
                        "http_proxy"
                            | "HTTP_PROXY"
                            | "https_proxy"
                            | "HTTPS_PROXY"
                            | "COPILOT_PROXY_KERBEROS_SPN"
                            | "TMPDIR"
                    ) {
                        return None;
                    }
                    // Match native startup capture without falling back to another setting.
                    "\u{FFFD}".into()
                }
            };
            Some((key, value))
        })
        .chain(environment.iter().cloned())
        .map(|(k, v)| {
            let key = if cfg!(windows) { k.to_uppercase() } else { k };
            (key, serde_json::Value::String(v))
        })
        .collect();
    Some(serde_json::to_vec(&map).expect("env serializes"))
}

#[cfg(test)]
mod tests;

#[cfg(test)]
mod initiation_tests;

#[cfg(test)]
mod shutdown_tests;

#[cfg(test)]
mod auth_owner_tests;
