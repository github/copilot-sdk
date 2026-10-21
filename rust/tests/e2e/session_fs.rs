// Copyright (c) Microsoft Corporation. All rights reserved.

use std::path::{Path, PathBuf};
use std::sync::{Arc, Mutex};

use async_trait::async_trait;
use base64::Engine;
use github_copilot_sdk::session_fs::{FsError, SessionFsBinaryProvider};
use github_copilot_sdk::{
    Client, DirEntry, DirEntryKind, FileInfo, SessionFsCapabilities, SessionFsConfig,
    SessionFsConventions, SessionFsProvider,
};

struct BinaryFileProvider {
    root: PathBuf,
    read_paths: Mutex<Vec<String>>,
}

impl BinaryFileProvider {
    fn path(&self, path: &str) -> PathBuf {
        self.root.join(path.trim_start_matches('/'))
    }
}

#[async_trait]
impl SessionFsBinaryProvider for BinaryFileProvider {
    async fn read_file_bytes(&self, path: &str) -> Result<Vec<u8>, FsError> {
        self.read_paths.lock().unwrap().push(path.to_string());
        tokio::fs::read(self.path(path))
            .await
            .map_err(FsError::from)
    }

    async fn write_file_bytes(
        &self,
        path: &str,
        content: &[u8],
        _mode: Option<i64>,
    ) -> Result<(), FsError> {
        let target = self.path(path);
        tokio::fs::create_dir_all(target.parent().unwrap())
            .await
            .map_err(FsError::from)?;
        tokio::fs::write(target, content)
            .await
            .map_err(FsError::from)
    }
}

#[async_trait]
impl SessionFsProvider for BinaryFileProvider {
    fn binary(&self) -> Option<&dyn SessionFsBinaryProvider> {
        Some(self)
    }

    async fn read_file(&self, path: &str) -> Result<String, FsError> {
        tokio::fs::read_to_string(self.path(path))
            .await
            .map_err(FsError::from)
    }

    async fn write_file(
        &self,
        path: &str,
        content: &str,
        _mode: Option<i64>,
    ) -> Result<(), FsError> {
        let target = self.path(path);
        tokio::fs::create_dir_all(target.parent().unwrap())
            .await
            .map_err(FsError::from)?;
        tokio::fs::write(target, content)
            .await
            .map_err(FsError::from)
    }

    async fn append_file(
        &self,
        path: &str,
        content: &str,
        _mode: Option<i64>,
    ) -> Result<(), FsError> {
        use tokio::io::AsyncWriteExt;

        let target = self.path(path);
        tokio::fs::create_dir_all(target.parent().unwrap())
            .await
            .map_err(FsError::from)?;
        let mut file = tokio::fs::OpenOptions::new()
            .create(true)
            .append(true)
            .open(target)
            .await
            .map_err(FsError::from)?;
        file.write_all(content.as_bytes())
            .await
            .map_err(FsError::from)
    }

    async fn exists(&self, path: &str) -> Result<bool, FsError> {
        tokio::fs::try_exists(self.path(path))
            .await
            .map_err(FsError::from)
    }

    async fn stat(&self, path: &str) -> Result<FileInfo, FsError> {
        let metadata = tokio::fs::metadata(self.path(path))
            .await
            .map_err(FsError::from)?;
        Ok(FileInfo::new(
            metadata.is_file(),
            metadata.is_dir(),
            metadata.len() as i64,
            "1970-01-01T00:00:00Z",
            "1970-01-01T00:00:00Z",
        ))
    }

    async fn mkdir(&self, path: &str, _recursive: bool, _mode: Option<i64>) -> Result<(), FsError> {
        tokio::fs::create_dir_all(self.path(path))
            .await
            .map_err(FsError::from)
    }

    async fn readdir(&self, path: &str) -> Result<Vec<String>, FsError> {
        let mut dir = tokio::fs::read_dir(self.path(path))
            .await
            .map_err(FsError::from)?;
        let mut names = Vec::new();
        while let Some(entry) = dir.next_entry().await.map_err(FsError::from)? {
            names.push(entry.file_name().to_string_lossy().into_owned());
        }
        Ok(names)
    }

    async fn readdir_with_types(&self, path: &str) -> Result<Vec<DirEntry>, FsError> {
        let mut dir = tokio::fs::read_dir(self.path(path))
            .await
            .map_err(FsError::from)?;
        let mut entries = Vec::new();
        while let Some(entry) = dir.next_entry().await.map_err(FsError::from)? {
            let kind = if entry.file_type().await.map_err(FsError::from)?.is_dir() {
                DirEntryKind::Directory
            } else {
                DirEntryKind::File
            };
            entries.push(DirEntry::new(entry.file_name().to_string_lossy(), kind));
        }
        Ok(entries)
    }

    async fn rm(&self, path: &str, recursive: bool, force: bool) -> Result<(), FsError> {
        let target = self.path(path);
        let result = if recursive {
            tokio::fs::remove_dir_all(target).await
        } else {
            tokio::fs::remove_file(target).await
        };
        match result {
            Err(err) if force && err.kind() == std::io::ErrorKind::NotFound => Ok(()),
            result => result.map_err(FsError::from),
        }
    }

    async fn rename(&self, src: &str, dest: &str) -> Result<(), FsError> {
        let target = self.path(dest);
        tokio::fs::create_dir_all(target.parent().unwrap())
            .await
            .map_err(FsError::from)?;
        tokio::fs::rename(self.path(src), target)
            .await
            .map_err(FsError::from)
    }
}

#[tokio::test]
async fn should_route_file_operations_through_the_session_fs_provider() {
    super::support::with_e2e_context(
        "session_fs",
        "should_route_file_operations_through_the_session_fs_provider",
        |ctx| {
            Box::pin(async move {
                let provider = Arc::new(BinaryFileProvider {
                    root: ctx.work_dir().join("text-provider"),
                    read_paths: Mutex::new(Vec::new()),
                });
                let session_state_path = if cfg!(windows) {
                    "/session-state".to_string()
                } else {
                    ctx.work_dir()
                        .join("session-state")
                        .to_string_lossy()
                        .into_owned()
                };
                let client = Client::start(
                    ctx.client_options().with_session_fs(
                        SessionFsConfig::new(
                            "/",
                            session_state_path.clone(),
                            SessionFsConventions::Posix,
                        )
                        .with_capabilities(SessionFsCapabilities::new().with_binary(true)),
                    ),
                )
                .await
                .expect("start session client");
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_session_fs_provider(provider.clone()),
                    )
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait("What is 100 + 200?")
                    .await
                    .expect("send arithmetic request")
                    .expect("final assistant message");
                assert!(
                    answer.data["content"]
                        .as_str()
                        .is_some_and(|content| content.contains("300")),
                    "unexpected assistant response: {:?}",
                    answer.data
                );
                session.disconnect().await.expect("disconnect session");
                let events_path = provider.path(&format!("{session_state_path}/events.jsonl"));
                let events = tokio::fs::read_to_string(&events_path)
                    .await
                    .unwrap_or_else(|err| {
                        panic!("read provider events {}: {err}", events_path.display())
                    });
                assert!(
                    events.contains("300"),
                    "provider events did not contain the response"
                );
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_view_an_image_that_exists_only_in_the_binary_session_fs_provider() {
    super::support::with_e2e_context(
        "session_fs",
        "should_view_an_image_that_exists_only_in_the_binary_session_fs_provider",
        |ctx| {
            Box::pin(async move {
                let image_path = "/sdk-provider-image.png";
                let image_bytes = base64::engine::general_purpose::STANDARD
                    .decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFBQIAX8jx0gAAAABJRU5ErkJggg==")
                    .unwrap();
                let provider = Arc::new(BinaryFileProvider {
                    root: ctx.work_dir().join("binary-provider"),
                    read_paths: Mutex::new(Vec::new()),
                });
                let session_state_path = if cfg!(windows) {
                    "/session-state".to_string()
                } else {
                    ctx.work_dir()
                        .join("session-state")
                        .to_string_lossy()
                        .into_owned()
                };
                let client = Client::start(
                    ctx.client_options().with_session_fs(
                        SessionFsConfig::new("/", session_state_path, SessionFsConventions::Posix)
                            .with_capabilities(SessionFsCapabilities::new().with_binary(true)),
                    ),
                )
                .await
                .expect("start binary session client");
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_session_fs_provider(provider.clone()),
                    )
                    .await
                    .expect("create binary session");
                let stored_path = provider.path(image_path);
                tokio::fs::create_dir_all(stored_path.parent().unwrap())
                    .await
                    .unwrap();
                tokio::fs::write(stored_path, &image_bytes).await.unwrap();
                assert!(!Path::new(image_path).exists());

                let answer = session
                    .send_and_wait(
                        "Use the view tool to view /sdk-provider-image.png, then reply with exactly SDK_PROVIDER_IMAGE_DONE.",
                    )
                    .await
                    .expect("send image view request")
                    .expect("final assistant message");
                assert!(
                    answer.data["content"]
                        .as_str()
                        .is_some_and(|content| content.contains("SDK_PROVIDER_IMAGE_DONE")),
                    "unexpected assistant response: {:?}",
                    answer.data
                );
                assert!(
                    provider.read_paths.lock().unwrap().iter().any(|p| p == image_path),
                    "view did not request provider image bytes"
                );
                let events = session.get_events().await.expect("get session events");
                assert!(
                    events.iter().any(|event| {
                        event.event_type == "session.binary_asset"
                            && event.data["mimeType"] == "image/png"
                            && event.data["data"]
                                == base64::engine::general_purpose::STANDARD.encode(&image_bytes)
                    }),
                    "missing image asset with exact provider bytes"
                );
                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}
