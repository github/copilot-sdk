//! In-process FFI transport: hosts the Copilot runtime by loading its native
//! library and speaking JSON-RPC over its C ABI,
//! instead of spawning a CLI child process and communicating over stdio/TCP.
//!
//! The runtime's `host_start` export spawns the residual TypeScript worker
//! itself — the packaged single-file CLI (`copilot --embedded-host`) or, for
//! dev, `node dist-cli/index.js --embedded-host`. JSON-RPC frames are pumped
//! across the ABI: writes go to `connection_write`; inbound frames arrive on a
//! native callback that feeds an async reader. The framing is unchanged — the
//! same LSP `Content-Length:` frames the stdio transport uses.

use std::collections::HashMap;
use std::ffi::c_void;
use std::path::{Path, PathBuf};
use std::pin::Pin;
use std::sync::atomic::{AtomicBool, AtomicPtr, AtomicU32, AtomicUsize, Ordering};
use std::sync::{Arc, OnceLock};
use std::task::{Context, Poll};

use libloading::Library;
use tokio::io::{AsyncRead, AsyncWrite, ReadBuf};
use tokio::sync::mpsc;
use tracing::debug;

use crate::{Error, ErrorKind};

type OutboundCallback = unsafe extern "C" fn(*mut c_void, *const u8, usize);
type HostStartFn = unsafe extern "C" fn(*const u8, usize, *const u8, usize) -> u32;
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

/// State handed to the native side as `user_data` so the outbound callback can
/// route inbound frames back to the reader.
struct CallbackState {
    tx: mpsc::UnboundedSender<Vec<u8>>,
    active_callbacks: AtomicUsize,
    closing: AtomicBool,
}

extern "C" fn on_outbound(user_data: *mut c_void, bytes: *const u8, len: usize) {
    if user_data.is_null() || bytes.is_null() || len == 0 {
        return;
    }
    let state = unsafe { &*(user_data as *const CallbackState) };
    state.active_callbacks.fetch_add(1, Ordering::SeqCst);
    if state.closing.load(Ordering::SeqCst) {
        state.active_callbacks.fetch_sub(1, Ordering::SeqCst);
        return;
    }
    let slice = unsafe { std::slice::from_raw_parts(bytes, len) };
    let _ = state.tx.send(slice.to_vec());
    state.active_callbacks.fetch_sub(1, Ordering::SeqCst);
}

/// Bound exports and connection lifecycle state, shared between the
/// [`FfiWriter`] and the owning [`Client`]. The cdylib itself is loaded
/// process-globally and never unloaded (see [`load_library`]), so this holds
/// only the bound fn pointers and connection state.
pub(crate) struct FfiShared {
    host_shutdown: HostShutdownFn,
    connection_write: ConnectionWriteFn,
    connection_close: ConnectionCloseFn,
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
    /// Close the connection, shut the host down, and free the callback state.
    /// Idempotent; called from [`Client::stop`], drop, and on startup failure.
    pub(crate) fn close(&self) {
        let _operation = self.operation_lock.lock();
        if self.closed.swap(true, Ordering::SeqCst) {
            return;
        }
        let state = self.callback_state.load(Ordering::SeqCst);
        if !state.is_null() {
            unsafe { &*state }.closing.store(true, Ordering::SeqCst);
        }
        let conn = self.connection_id.swap(0, Ordering::SeqCst);
        if conn != 0 {
            unsafe { (self.connection_close)(conn) };
        }
        let server = self.server_id.swap(0, Ordering::SeqCst);
        if server != 0 {
            unsafe { (self.host_shutdown)(server) };
        }
        // Free the callback state only after the connection is closed and the
        // host is shut down, so native can no longer invoke the callback.
        let state = self
            .callback_state
            .swap(std::ptr::null_mut(), Ordering::SeqCst);
        if !state.is_null() {
            while unsafe { &*state }.active_callbacks.load(Ordering::SeqCst) != 0 {
                std::thread::yield_now();
            }
            drop(unsafe { Box::from_raw(state) });
        }
        debug!(library = %self.library_path.display(), "FFI runtime connection closed");
    }

    fn write_frame(&self, frame: &[u8]) -> bool {
        let _operation = self.operation_lock.lock();
        if self.closed.load(Ordering::SeqCst) {
            return false;
        }
        let conn = self.connection_id.load(Ordering::SeqCst);
        if conn == 0 {
            return false;
        }
        unsafe { (self.connection_write)(conn, frame.as_ptr(), frame.len()) }
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

/// Write side of the FFI transport. Each frame is forwarded synchronously to
/// the native `connection_write` export (native copies before returning).
pub(crate) struct FfiWriter {
    shared: Arc<FfiShared>,
}

impl AsyncWrite for FfiWriter {
    fn poll_write(
        self: Pin<&mut Self>,
        _cx: &mut Context<'_>,
        buf: &[u8],
    ) -> Poll<std::io::Result<usize>> {
        if self.shared.write_frame(buf) {
            Poll::Ready(Ok(buf.len()))
        } else {
            Poll::Ready(Err(std::io::Error::new(
                std::io::ErrorKind::BrokenPipe,
                "failed to write a frame to the in-process runtime connection",
            )))
        }
    }

    fn poll_flush(self: Pin<&mut Self>, _cx: &mut Context<'_>) -> Poll<std::io::Result<()>> {
        Poll::Ready(Ok(()))
    }

    fn poll_shutdown(self: Pin<&mut Self>, _cx: &mut Context<'_>) -> Poll<std::io::Result<()>> {
        Poll::Ready(Ok(()))
    }
}

/// Prepared FFI host: the bound cdylib exports plus the spawn arguments needed
/// to start the runtime worker. The cdylib is loaded process-globally and never
/// unloaded (see [`load_library`]).
pub(crate) struct FfiHost {
    library_path: PathBuf,
    entrypoint: PathBuf,
    environment: Vec<(String, String)>,
    args: Vec<String>,
    host_start: HostStartFn,
    host_shutdown: HostShutdownFn,
    connection_open: ConnectionOpenFn,
    connection_write: ConnectionWriteFn,
    connection_close: ConnectionCloseFn,
}

// SAFETY: as for `FfiShared` — the bound exports are plain fn pointers, safe to
// move to the blocking thread that starts the host.
unsafe impl Send for FfiHost {}

impl FfiHost {
    /// Load the cdylib next to `entrypoint` and bind its exports.
    ///
    /// `entrypoint` is the packaged single-file CLI binary or, for dev, a
    /// `.js` file launched via `node`. The native library is resolved relative
    /// to the entrypoint directory, supporting both packaged and development
    /// layouts.
    pub(crate) fn create(
        entrypoint: &Path,
        environment: Vec<(String, String)>,
        args: Vec<String>,
    ) -> Result<Self, Error> {
        let entrypoint = std::fs::canonicalize(entrypoint)
            .map(path_for_child_process)
            .map_err(|e| {
                Error::with_message(
                    ErrorKind::InvalidConfig,
                    format!(
                        "failed to resolve in-process CLI entrypoint '{}': {e}",
                        entrypoint.display()
                    ),
                )
            })?;
        let library_path =
            std::fs::canonicalize(resolve_library_path(&entrypoint)?).map_err(|e| {
                Error::with_message(
                    ErrorKind::InvalidConfig,
                    format!("failed to resolve in-process runtime library: {e}"),
                )
            })?;
        let lib = load_library(&library_path)?;

        let host_start = *bind::<HostStartFn>(lib, b"copilot_runtime_host_start\0", &library_path)?;
        let host_shutdown =
            *bind::<HostShutdownFn>(lib, b"copilot_runtime_host_shutdown\0", &library_path)?;
        let connection_open =
            *bind::<ConnectionOpenFn>(lib, b"copilot_runtime_connection_open\0", &library_path)?;
        let connection_write =
            *bind::<ConnectionWriteFn>(lib, b"copilot_runtime_connection_write\0", &library_path)?;
        let connection_close =
            *bind::<ConnectionCloseFn>(lib, b"copilot_runtime_connection_close\0", &library_path)?;

        Ok(Self {
            library_path,
            entrypoint,
            environment,
            args,
            host_start,
            host_shutdown,
            connection_open,
            connection_write,
            connection_close,
        })
    }

    /// Start the runtime worker and open the FFI JSON-RPC connection.
    ///
    /// `host_start` blocks until the worker connects back and signals
    /// readiness (up to ~30s), and must not run on an async executor thread, so
    /// the blocking handshake is offloaded to [`tokio::task::spawn_blocking`].
    pub(crate) async fn start(self) -> Result<(FfiReader, FfiWriter, Arc<FfiShared>), Error> {
        tokio::task::spawn_blocking(move || self.start_blocking())
            .await
            .map_err(|e| {
                Error::with_message(
                    ErrorKind::InvalidConfig,
                    format!("in-process runtime startup task failed: {e}"),
                )
            })?
    }

    fn start_blocking(self) -> Result<(FfiReader, FfiWriter, Arc<FfiShared>), Error> {
        let argv = build_argv_json(&self.entrypoint, &self.args);
        let env = build_env_json(&self.environment);

        let (env_ptr, env_len) = match &env {
            Some(bytes) => (bytes.as_ptr(), bytes.len()),
            None => (std::ptr::null(), 0),
        };

        let server_id = unsafe { (self.host_start)(argv.as_ptr(), argv.len(), env_ptr, env_len) };

        if server_id == 0 {
            return Err(Error::with_message(
                ErrorKind::InvalidConfig,
                format!(
                    "copilot_runtime_host_start failed (library '{}', entrypoint '{}')",
                    self.library_path.display(),
                    self.entrypoint.display()
                ),
            ));
        }

        let (tx, rx) = mpsc::unbounded_channel::<Vec<u8>>();
        let state_ptr = Box::into_raw(Box::new(CallbackState {
            tx,
            active_callbacks: AtomicUsize::new(0),
            closing: AtomicBool::new(false),
        }));
        let connection_id = unsafe {
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
        };
        if connection_id == 0 {
            drop(unsafe { Box::from_raw(state_ptr) });
            unsafe { (self.host_shutdown)(server_id) };
            return Err(Error::with_message(
                ErrorKind::InvalidConfig,
                "copilot_runtime_connection_open failed",
            ));
        }

        let shared = Arc::new(FfiShared {
            host_shutdown: self.host_shutdown,
            connection_write: self.connection_write,
            connection_close: self.connection_close,
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
        let writer = FfiWriter {
            shared: Arc::clone(&shared),
        };
        Ok((reader, writer, shared))
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
                 `bundled-in-process` feature or set COPILOT_CLI_PATH to a compatible CLI package.",
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

fn build_argv_json(entrypoint: &Path, extra_args: &[String]) -> Vec<u8> {
    // A `.js` entrypoint (dev / dist-cli) is launched via node; the packaged
    // single-file CLI binary embeds its own Node and is invoked directly.
    let entrypoint_str = entrypoint.to_string_lossy().into_owned();
    let is_js = entrypoint
        .extension()
        .and_then(|ext| ext.to_str())
        .is_some_and(|ext| ext.eq_ignore_ascii_case("js"));
    let mut argv: Vec<String> = if is_js {
        vec![
            "node".to_string(),
            entrypoint_str,
            "--embedded-host".to_string(),
            "--no-auto-update".to_string(),
        ]
    } else {
        vec![
            entrypoint_str,
            "--embedded-host".to_string(),
            "--no-auto-update".to_string(),
        ]
    };
    argv.extend_from_slice(extra_args);
    serde_json::to_vec(&argv).expect("argv serializes")
}

fn build_env_json(environment: &[(String, String)]) -> Option<Vec<u8>> {
    if environment.is_empty() {
        return None;
    }
    let map: serde_json::Map<String, serde_json::Value> = environment
        .iter()
        .map(|(k, v)| (k.clone(), serde_json::Value::String(v.clone())))
        .collect();
    Some(serde_json::to_vec(&map).expect("env serializes"))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn argv_pins_worker_and_appends_client_options() {
        let argv: Vec<String> = serde_json::from_slice(&build_argv_json(
            Path::new("copilot"),
            &["--log-level".into(), "debug".into()],
        ))
        .unwrap();

        assert_eq!(
            argv,
            [
                "copilot",
                "--embedded-host",
                "--no-auto-update",
                "--log-level",
                "debug"
            ]
        );
    }

    #[test]
    fn javascript_entrypoint_uses_node() {
        let argv: Vec<String> =
            serde_json::from_slice(&build_argv_json(Path::new("index.js"), &[])).unwrap();

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
}
