//! Custom `SessionFsProvider` backed by an in-memory map.
//!
//! Demonstrates registering a [`SessionFsProvider`] so the CLI delegates all
//! per-session filesystem operations to your code. Useful for sandboxed
//! sessions, projecting files into virtual storage, or applying permission
//! policies before bytes are read or written.
//!
//! ```sh
//! cargo run -p github-copilot-sdk --example session_fs
//! ```

use std::collections::HashMap;
use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::session_fs::{
    DirEntry, DirEntryKind, FileInfo, FsError, FsErrorKind, SessionFsConfig, SessionFsConventions,
    SessionFsProvider,
};
use github_copilot_sdk::types::{MessageOptions, SessionConfig};
use github_copilot_sdk::{Client, ClientOptions};
use parking_lot::Mutex;

#[cfg(test)]
#[path = "session_fs/tests.rs"]
mod tests;

enum Entry {
    File(String),
    Directory,
}

/// Every path maps to a file or a directory. Writing a file or creating a
/// directory records its ancestors as directories, so listing a directory
/// only has to look one level below it.
struct InMemoryProvider {
    entries: Mutex<HashMap<String, Entry>>,
}

impl InMemoryProvider {
    fn new() -> Self {
        let mut entries = HashMap::new();
        add_parents(&mut entries, "/workspace/README.md").expect("seed parents are directories");
        entries.insert(
            "/workspace/README.md".to_string(),
            Entry::File("# Demo project\n\nThis file lives in memory.\n".to_string()),
        );
        Self {
            entries: Mutex::new(entries),
        }
    }
}

fn not_found(path: &str) -> FsError {
    FsError::from(FsErrorKind::NotFound(path.to_string()))
}

fn parent(path: &str) -> Option<&str> {
    let path = path.trim_end_matches('/');
    match path.rfind('/')? {
        0 if path.len() > 1 => Some("/"),
        0 => None,
        i => Some(&path[..i]),
    }
}

fn add_parents(entries: &mut HashMap<String, Entry>, path: &str) -> Result<(), FsError> {
    let mut dir = parent(path);
    while let Some(d) = dir {
        if matches!(entries.get(d), Some(Entry::File(_))) {
            return Err(FsError::with_message(
                FsErrorKind::Other,
                format!("{d} is not a directory"),
            ));
        }
        dir = parent(d);
    }

    let mut dir = parent(path);
    while let Some(d) = dir {
        entries.entry(d.to_string()).or_insert(Entry::Directory);
        dir = parent(d);
    }
    Ok(())
}

fn child_prefix(dir: &str) -> String {
    format!("{}/", dir.trim_end_matches('/'))
}

#[async_trait]
impl SessionFsProvider for InMemoryProvider {
    async fn read_file(&self, path: &str) -> Result<String, FsError> {
        match self.entries.lock().get(path) {
            Some(Entry::File(content)) => Ok(content.clone()),
            _ => Err(not_found(path)),
        }
    }

    async fn write_file(
        &self,
        path: &str,
        content: &str,
        _mode: Option<i64>,
    ) -> Result<(), FsError> {
        let mut entries = self.entries.lock();
        if matches!(entries.get(path), Some(Entry::Directory)) {
            return Err(FsError::with_message(
                FsErrorKind::Other,
                format!("{path} is a directory"),
            ));
        }
        add_parents(&mut entries, path)?;
        entries.insert(path.to_string(), Entry::File(content.to_string()));
        Ok(())
    }

    async fn append_file(
        &self,
        path: &str,
        content: &str,
        _mode: Option<i64>,
    ) -> Result<(), FsError> {
        let mut entries = self.entries.lock();
        add_parents(&mut entries, path)?;
        match entries
            .entry(path.to_string())
            .or_insert_with(|| Entry::File(String::new()))
        {
            Entry::File(existing) => {
                existing.push_str(content);
                Ok(())
            }
            Entry::Directory => Err(FsError::with_message(
                FsErrorKind::Other,
                format!("{path} is a directory"),
            )),
        }
    }

    async fn exists(&self, path: &str) -> Result<bool, FsError> {
        Ok(self.entries.lock().contains_key(path))
    }

    async fn stat(&self, path: &str) -> Result<FileInfo, FsError> {
        let (is_file, size) = match self.entries.lock().get(path) {
            Some(Entry::File(content)) => (true, content.len() as i64),
            Some(Entry::Directory) => (false, 0),
            None => return Err(not_found(path)),
        };
        Ok(FileInfo::new(
            is_file,
            !is_file,
            size,
            "2025-01-01T00:00:00Z",
            "2025-01-01T00:00:00Z",
        ))
    }

    async fn mkdir(&self, path: &str, recursive: bool, _mode: Option<i64>) -> Result<(), FsError> {
        let mut entries = self.entries.lock();
        if matches!(entries.get(path), Some(Entry::File(_))) {
            return Err(FsError::with_message(
                FsErrorKind::Other,
                format!("{path} is not a directory"),
            ));
        }
        if !recursive && parent(path).is_some_and(|p| !entries.contains_key(p)) {
            return Err(not_found(path));
        }
        add_parents(&mut entries, path)?;
        entries.entry(path.to_string()).or_insert(Entry::Directory);
        Ok(())
    }

    async fn readdir(&self, path: &str) -> Result<Vec<String>, FsError> {
        let entries = self.readdir_with_types(path).await?;
        Ok(entries.into_iter().map(|entry| entry.name).collect())
    }

    async fn readdir_with_types(&self, path: &str) -> Result<Vec<DirEntry>, FsError> {
        let entries = self.entries.lock();
        if !matches!(entries.get(path), Some(Entry::Directory)) {
            return Err(not_found(path));
        }
        let prefix = child_prefix(path);
        Ok(entries
            .iter()
            .filter_map(|(key, entry)| {
                let name = key.strip_prefix(&prefix)?;
                if name.is_empty() || name.contains('/') {
                    return None;
                }
                let kind = match entry {
                    Entry::File(_) => DirEntryKind::File,
                    Entry::Directory => DirEntryKind::Directory,
                };
                Some(DirEntry::new(name, kind))
            })
            .collect())
    }

    async fn rm(&self, path: &str, recursive: bool, force: bool) -> Result<(), FsError> {
        let mut entries = self.entries.lock();
        if !entries.contains_key(path) {
            return if force { Ok(()) } else { Err(not_found(path)) };
        }
        let prefix = child_prefix(path);
        let has_children = entries.keys().any(|key| key.starts_with(&prefix));
        if has_children && !recursive {
            return Err(FsError::with_message(
                FsErrorKind::Other,
                format!("{path} is not empty"),
            ));
        }
        entries.remove(path);
        entries.retain(|key, _| !key.starts_with(&prefix));
        Ok(())
    }

    async fn rename(&self, src: &str, dest: &str) -> Result<(), FsError> {
        let mut entries = self.entries.lock();
        let source = entries.get(src).ok_or_else(|| not_found(src))?;
        if src == dest {
            return Ok(());
        }
        let prefix = child_prefix(src);
        if matches!(source, Entry::Directory) && dest.starts_with(&prefix) {
            return Err(FsError::with_message(
                FsErrorKind::Other,
                format!("cannot move {src} into its own subtree"),
            ));
        }
        match (source, entries.get(dest)) {
            (Entry::File(_), Some(Entry::Directory)) | (Entry::Directory, Some(Entry::File(_))) => {
                return Err(FsError::with_message(
                    FsErrorKind::Other,
                    format!("{src} and {dest} have different types"),
                ));
            }
            (Entry::Directory, Some(Entry::Directory))
                if entries
                    .keys()
                    .any(|key| key.starts_with(&child_prefix(dest))) =>
            {
                return Err(FsError::with_message(
                    FsErrorKind::Other,
                    format!("{dest} is not empty"),
                ));
            }
            _ => {}
        }
        add_parents(&mut entries, dest)?;
        let entry = entries.remove(src).ok_or_else(|| not_found(src))?;
        let children: Vec<String> = entries
            .keys()
            .filter(|key| key.starts_with(&prefix))
            .cloned()
            .collect();
        for key in children {
            if let Some(child) = entries.remove(&key) {
                let moved = format!("{}{}", child_prefix(dest), &key[prefix.len()..]);
                entries.insert(moved, child);
            }
        }
        entries.insert(dest.to_string(), entry);
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
                .with_permission_handler(Arc::new(ApproveAllHandler))
                .with_session_fs_provider(provider),
        )
        .await?;

    let response = session
        .send(MessageOptions::new("Summarize README.md."))
        .await?;
    println!("Assistant: {response}");

    Ok(())
}
