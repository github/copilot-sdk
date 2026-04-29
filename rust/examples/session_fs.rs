//! Custom `SessionFsProvider` backed by an in-memory map.
//!
//! Demonstrates registering a [`SessionFsProvider`] so the CLI delegates all
//! per-session filesystem operations to your code. Useful for sandboxed
//! sessions, projecting files into virtual storage, or applying permission
//! policies before bytes are read or written.
//!
//! See `docs/adr/0001-session-fs-provider.md` for the design rationale,
//! including why this SDK uses direct `Arc<dyn SessionFsProvider>`
//! registration rather than the factory-closure pattern used by the Node,
//! Python, and Go SDKs.
//!
//! ```sh
//! cargo run -p github-copilot-sdk --example session_fs
//! ```

use std::collections::HashMap;
use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::session_fs::{
    DirEntry, DirEntryKind, FileInfo, FsError, SessionFsConfig, SessionFsConventions,
    SessionFsProvider,
};
use github_copilot_sdk::types::{MessageOptions, SessionConfig};
use github_copilot_sdk::{Client, ClientOptions};
use parking_lot::Mutex;

struct InMemoryProvider {
    files: Mutex<HashMap<String, String>>,
}

impl InMemoryProvider {
    fn new() -> Self {
        let mut seed = HashMap::new();
        seed.insert(
            "/workspace/README.md".to_string(),
            "# Demo project\n\nThis file lives in memory.\n".to_string(),
        );
        Self {
            files: Mutex::new(seed),
        }
    }
}

#[async_trait]
impl SessionFsProvider for InMemoryProvider {
    async fn read_file(&self, path: &str) -> Result<String, FsError> {
        self.files
            .lock()
            .get(path)
            .cloned()
            .ok_or_else(|| FsError::NotFound(path.to_string()))
    }

    async fn write_file(
        &self,
        path: &str,
        content: &str,
        _mode: Option<i64>,
    ) -> Result<(), FsError> {
        self.files
            .lock()
            .insert(path.to_string(), content.to_string());
        Ok(())
    }

    async fn exists(&self, path: &str) -> Result<bool, FsError> {
        Ok(self.files.lock().contains_key(path))
    }

    async fn stat(&self, path: &str) -> Result<FileInfo, FsError> {
        let files = self.files.lock();
        let content = files
            .get(path)
            .ok_or_else(|| FsError::NotFound(path.to_string()))?;
        Ok(FileInfo::new(
            true,
            false,
            content.len() as i64,
            "2025-01-01T00:00:00Z",
            "2025-01-01T00:00:00Z",
        ))
    }

    async fn readdir_with_types(&self, path: &str) -> Result<Vec<DirEntry>, FsError> {
        let prefix = if path.ends_with('/') {
            path.to_string()
        } else {
            format!("{path}/")
        };
        let names: Vec<DirEntry> = self
            .files
            .lock()
            .keys()
            .filter_map(|k| k.strip_prefix(&prefix))
            .filter(|rest| !rest.is_empty())
            .map(|rest| {
                let name = rest.split('/').next().unwrap_or(rest);
                DirEntry::new(name, DirEntryKind::File)
            })
            .collect();
        Ok(names)
    }

    async fn rm(&self, path: &str, _recursive: bool, force: bool) -> Result<(), FsError> {
        if self.files.lock().remove(path).is_none() && !force {
            return Err(FsError::NotFound(path.to_string()));
        }
        Ok(())
    }
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let provider: Arc<dyn SessionFsProvider> = Arc::new(InMemoryProvider::new());

    let options = {
        let mut opts = ClientOptions::default();
        opts.session_fs = Some(SessionFsConfig::new(
            "/workspace",
            "/workspace/.copilot",
            SessionFsConventions::Posix,
        ));
        opts
    };

    let client = Client::start(options).await?;
    let session = client
        .create_session(
            SessionConfig::default()
                .with_handler(Arc::new(ApproveAllHandler))
                .with_session_fs_provider(provider),
        )
        .await?;

    let response = session
        .send(MessageOptions::new("Summarize README.md."))
        .await?;
    println!("Assistant: {response}");

    Ok(())
}
