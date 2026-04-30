//! Session filesystem provider — virtualizable filesystem layer over JSON-RPC.
//!
//! When [`ClientOptions::session_fs`] is set, the SDK tells the CLI to delegate
//! all per-session filesystem operations (`readFile`, `writeFile`, `stat`, ...)
//! to a [`SessionFsProvider`] registered on each session. This lets host
//! applications sandbox sessions, project files into in-memory or remote
//! storage, and apply permission policies before bytes move.
//!
//! # Concurrency
//!
//! Each inbound `sessionFs.*` request is dispatched on its own spawned task,
//! matching Node's behavior. Provider implementations MUST be safe for
//! concurrent invocation across distinct paths. Use internal synchronization
//! (e.g. [`tokio::sync::Mutex`] keyed by path) if your backing store needs
//! ordering.
//!
//! # Errors
//!
//! Provider methods return [`Result<T, FsError>`]. The SDK adapts these into
//! the schema's `{ ..., error: Option<SessionFsError> }` payload, mapping
//! [`FsError::NotFound`] to the wire's `ENOENT` and everything else to
//! `UNKNOWN`. A [`From<std::io::Error>`] conversion is provided so handlers
//! backed by [`tokio::fs`](https://docs.rs/tokio/latest/tokio/fs/index.html)
//! can propagate `io::Error` with `?`.
//!
//! # Example
//!
//! ```no_run
//! use std::sync::Arc;
//! use async_trait::async_trait;
//! use github_copilot_sdk::types::{SessionFsProvider, FsError, FileInfo, DirEntry};
//!
//! struct MyProvider;
//!
//! #[async_trait]
//! impl SessionFsProvider for MyProvider {
//!     async fn read_file(&self, path: &str) -> Result<String, FsError> {
//!         std::fs::read_to_string(path)
//!             .map_err(FsError::from)
//!     }
//! }
//! ```

use async_trait::async_trait;

use crate::generated::api_types::{
    SessionFsError, SessionFsErrorCode, SessionFsReaddirWithTypesEntry,
    SessionFsReaddirWithTypesEntryType, SessionFsSetProviderConventions, SessionFsStatResult,
};

/// Configuration for a custom session filesystem provider.
///
/// When set on [`ClientOptions::session_fs`](crate::ClientOptions::session_fs),
/// the SDK calls `sessionFs.setProvider` during [`Client::start`](crate::Client::start)
/// to tell the CLI to route per-session filesystem operations to the SDK.
#[non_exhaustive]
#[derive(Debug, Clone)]
pub struct SessionFsConfig {
    /// Initial working directory for sessions (the user's project directory).
    pub initial_cwd: String,
    /// Path within each session's SessionFs where the runtime stores
    /// session-scoped files (events, workspace, checkpoints, etc.).
    pub session_state_path: String,
    /// Path conventions used by this filesystem provider.
    pub conventions: SessionFsConventions,
}

impl SessionFsConfig {
    /// Build a new config with the required fields.
    pub fn new(
        initial_cwd: impl Into<String>,
        session_state_path: impl Into<String>,
        conventions: SessionFsConventions,
    ) -> Self {
        Self {
            initial_cwd: initial_cwd.into(),
            session_state_path: session_state_path.into(),
            conventions,
        }
    }
}

/// Path conventions used by a session filesystem provider.
///
/// Hand-authored consumer-facing enum (rather than reusing
/// [`SessionFsSetProviderConventions`]) to avoid exposing the generated
/// catch-all `Unknown` variant on the input side. The SDK rejects unknown
/// conventions at validation time with a typed error.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SessionFsConventions {
    /// POSIX-style paths (`/foo/bar`).
    Posix,
    /// Windows-style paths (`C:\foo\bar`).
    Windows,
}

impl SessionFsConventions {
    pub(crate) fn into_wire(self) -> SessionFsSetProviderConventions {
        match self {
            Self::Posix => SessionFsSetProviderConventions::Posix,
            Self::Windows => SessionFsSetProviderConventions::Windows,
        }
    }
}

/// Error returned by a [`SessionFsProvider`] method.
///
/// The SDK maps this onto the wire schema's [`SessionFsError`]:
/// [`FsError::NotFound`] becomes `ENOENT`, everything else becomes `UNKNOWN`.
#[non_exhaustive]
#[derive(Debug, Clone, thiserror::Error)]
pub enum FsError {
    /// File or directory does not exist.
    #[error("not found: {0}")]
    NotFound(String),

    /// Any other filesystem error (permission denied, I/O error, etc.).
    ///
    /// The wire mapping always uses `UNKNOWN` as the code; the message is
    /// preserved for diagnostics.
    #[error("{0}")]
    Other(String),
}

impl FsError {
    pub(crate) fn into_wire(self) -> SessionFsError {
        match self {
            Self::NotFound(message) => SessionFsError {
                code: SessionFsErrorCode::ENOENT,
                message: Some(message),
            },
            Self::Other(message) => SessionFsError {
                code: SessionFsErrorCode::UNKNOWN,
                message: Some(message),
            },
        }
    }
}

impl From<std::io::Error> for FsError {
    fn from(err: std::io::Error) -> Self {
        match err.kind() {
            std::io::ErrorKind::NotFound => Self::NotFound(err.to_string()),
            _ => Self::Other(err.to_string()),
        }
    }
}

/// File or directory metadata returned by [`SessionFsProvider::stat`].
///
/// The SDK adapts this into the wire's [`SessionFsStatResult`].
#[non_exhaustive]
#[derive(Debug, Clone)]
pub struct FileInfo {
    /// Whether the path is a regular file.
    pub is_file: bool,
    /// Whether the path is a directory.
    pub is_directory: bool,
    /// File size in bytes.
    pub size: i64,
    /// ISO 8601 timestamp of last modification.
    pub mtime: String,
    /// ISO 8601 timestamp of creation.
    pub birthtime: String,
}

impl FileInfo {
    /// Build a metadata record. The mtime/birthtime arguments are caller-
    /// supplied ISO 8601 strings — the SDK does not format timestamps for
    /// you.
    pub fn new(
        is_file: bool,
        is_directory: bool,
        size: i64,
        mtime: impl Into<String>,
        birthtime: impl Into<String>,
    ) -> Self {
        Self {
            is_file,
            is_directory,
            size,
            mtime: mtime.into(),
            birthtime: birthtime.into(),
        }
    }

    pub(crate) fn into_wire(self) -> SessionFsStatResult {
        SessionFsStatResult {
            is_file: self.is_file,
            is_directory: self.is_directory,
            size: self.size,
            mtime: self.mtime,
            birthtime: self.birthtime,
            error: None,
        }
    }
}

/// Kind of entry returned by [`SessionFsProvider::readdir_with_types`].
///
/// The wire schema's `Unknown` forward-compat variant is intentionally absent
/// from this consumer-facing enum — providers must classify each entry as
/// either a file or a directory.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum DirEntryKind {
    /// Regular file.
    File,
    /// Directory.
    Directory,
}

impl DirEntryKind {
    fn into_wire(self) -> SessionFsReaddirWithTypesEntryType {
        match self {
            Self::File => SessionFsReaddirWithTypesEntryType::File,
            Self::Directory => SessionFsReaddirWithTypesEntryType::Directory,
        }
    }
}

/// Single entry in a directory listing returned by
/// [`SessionFsProvider::readdir_with_types`].
#[non_exhaustive]
#[derive(Debug, Clone)]
pub struct DirEntry {
    /// Entry name (basename, not full path).
    pub name: String,
    /// Whether the entry is a file or a directory.
    pub kind: DirEntryKind,
}

impl DirEntry {
    /// Build a new directory entry.
    pub fn new(name: impl Into<String>, kind: DirEntryKind) -> Self {
        Self {
            name: name.into(),
            kind,
        }
    }

    pub(crate) fn into_wire(self) -> SessionFsReaddirWithTypesEntry {
        SessionFsReaddirWithTypesEntry {
            name: self.name,
            r#type: self.kind.into_wire(),
        }
    }
}

/// Implementor-supplied filesystem backing for a session.
///
/// Each method takes a path using the conventions declared in
/// [`SessionFsConfig::conventions`] and returns the operation's result. The
/// SDK adapts every `Result<_, FsError>` into the JSON-RPC response shape
/// expected by the GitHub Copilot CLI.
///
/// # Concurrency
///
/// Implementations MUST be `Send + Sync` and safe for concurrent invocation
/// across distinct paths. The SDK dispatches each inbound `sessionFs.*`
/// request on its own spawned task. Use internal synchronization (e.g.
/// [`tokio::sync::Mutex`] keyed by path) if your backing store requires
/// ordering.
///
/// # Forward compatibility
///
/// Methods on this trait have default implementations that return
/// `Err(FsError::Other("operation not supported".into()))`. When the CLI
/// schema grows new `sessionFs.*` methods, the SDK adds them to this trait
/// with default impls so existing implementations continue to compile.
/// Override only the methods relevant to your backing store.
#[async_trait]
pub trait SessionFsProvider: Send + Sync + 'static {
    /// Read the full contents of a file as UTF-8.
    async fn read_file(&self, path: &str) -> Result<String, FsError> {
        let _ = path;
        Err(FsError::Other("read_file not supported".to_string()))
    }

    /// Write content to a file, creating parent directories if needed.
    async fn write_file(
        &self,
        path: &str,
        content: &str,
        mode: Option<i64>,
    ) -> Result<(), FsError> {
        let _ = (path, content, mode);
        Err(FsError::Other("write_file not supported".to_string()))
    }

    /// Append content to a file, creating parent directories if needed.
    async fn append_file(
        &self,
        path: &str,
        content: &str,
        mode: Option<i64>,
    ) -> Result<(), FsError> {
        let _ = (path, content, mode);
        Err(FsError::Other("append_file not supported".to_string()))
    }

    /// Check whether a path exists.
    ///
    /// Returns `Ok(false)` for non-existent paths, not [`FsError::NotFound`].
    async fn exists(&self, path: &str) -> Result<bool, FsError> {
        let _ = path;
        Err(FsError::Other("exists not supported".to_string()))
    }

    /// Get metadata about a file or directory.
    async fn stat(&self, path: &str) -> Result<FileInfo, FsError> {
        let _ = path;
        Err(FsError::Other("stat not supported".to_string()))
    }

    /// Create a directory. When `recursive`, missing parents are also created.
    async fn mkdir(&self, path: &str, recursive: bool, mode: Option<i64>) -> Result<(), FsError> {
        let _ = (path, recursive, mode);
        Err(FsError::Other("mkdir not supported".to_string()))
    }

    /// List entry names in a directory.
    async fn readdir(&self, path: &str) -> Result<Vec<String>, FsError> {
        let _ = path;
        Err(FsError::Other("readdir not supported".to_string()))
    }

    /// List directory entries with type information.
    async fn readdir_with_types(&self, path: &str) -> Result<Vec<DirEntry>, FsError> {
        let _ = path;
        Err(FsError::Other(
            "readdir_with_types not supported".to_string(),
        ))
    }

    /// Remove a file or directory. When `force`, missing paths are not an
    /// error. When `recursive`, directory contents are removed as well.
    async fn rm(&self, path: &str, recursive: bool, force: bool) -> Result<(), FsError> {
        let _ = (path, recursive, force);
        Err(FsError::Other("rm not supported".to_string()))
    }

    /// Rename or move a file or directory.
    async fn rename(&self, src: &str, dest: &str) -> Result<(), FsError> {
        let _ = (src, dest);
        Err(FsError::Other("rename not supported".to_string()))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn fs_error_maps_io_not_found_to_enoent() {
        let io_err = std::io::Error::new(std::io::ErrorKind::NotFound, "missing.txt");
        let fs_err: FsError = io_err.into();
        assert!(matches!(fs_err, FsError::NotFound(_)));
        let wire = fs_err.into_wire();
        assert_eq!(wire.code, SessionFsErrorCode::ENOENT);
    }

    #[test]
    fn fs_error_maps_other_io_to_unknown() {
        let io_err = std::io::Error::other("disk full");
        let fs_err: FsError = io_err.into();
        assert!(matches!(fs_err, FsError::Other(_)));
        let wire = fs_err.into_wire();
        assert_eq!(wire.code, SessionFsErrorCode::UNKNOWN);
        assert!(wire.message.unwrap().contains("disk full"));
    }

    #[test]
    fn conventions_maps_to_wire() {
        assert_eq!(
            SessionFsConventions::Posix.into_wire(),
            SessionFsSetProviderConventions::Posix
        );
        assert_eq!(
            SessionFsConventions::Windows.into_wire(),
            SessionFsSetProviderConventions::Windows
        );
    }

    struct DefaultProvider;
    #[async_trait]
    impl SessionFsProvider for DefaultProvider {}

    #[tokio::test]
    async fn default_impls_return_unsupported() {
        let p = DefaultProvider;
        let err = p.read_file("/x").await.unwrap_err();
        assert!(matches!(err, FsError::Other(ref m) if m.contains("not supported")));
    }
}
