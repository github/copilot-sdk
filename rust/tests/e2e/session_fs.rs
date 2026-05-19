use std::path::{Path, PathBuf};
use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::generated::api_types::PlanUpdateRequest;
use github_copilot_sdk::{
    Client, DirEntry, DirEntryKind, FileInfo, FsError, SessionConfig, SessionFsConfig,
    SessionFsConventions, SessionFsProvider,
};

use super::support::{assistant_message_content, wait_for_condition, with_e2e_context};

#[tokio::test]
async fn should_route_file_operations_through_the_session_fs_provider() {
    with_e2e_context(
        "session_fs",
        "should_route_file_operations_through_the_session_fs_provider",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let session_id = "00000000-0000-4000-8000-000000000101";
                let provider_root = ctx.work_dir().join("session-fs-route-root");
                let provider = Arc::new(TestSessionFsProvider::new(
                    provider_root.clone(),
                    session_id,
                ));
                let client = start_session_fs_client(ctx, provider.clone()).await;
                let session = client
                    .create_session(session_config(ctx, provider).with_session_id(session_id))
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait("What is 100 + 200?")
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(assistant_message_content(&answer).contains("300"));
                let events_path = provider_root
                    .join(session.id().as_ref())
                    .join(provider_relative_path(&session_state_path()))
                    .join("events.jsonl");
                wait_for_file_containing(&events_path, "300").await;
                let content = std::fs::read_to_string(events_path).expect("read events");
                assert!(content.contains("300"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_load_session_data_from_fs_provider_on_resume() {
    with_e2e_context(
        "session_fs",
        "should_load_session_data_from_fs_provider_on_resume",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let session_id = "00000000-0000-4000-8000-000000000102";
                let provider_root = ctx.work_dir().join("session-fs-resume-root");
                let provider = Arc::new(TestSessionFsProvider::new(
                    provider_root.clone(),
                    session_id,
                ));
                let client = start_session_fs_client(ctx, provider.clone()).await;
                let session1 = client
                    .create_session(
                        session_config(ctx, provider.clone()).with_session_id(session_id),
                    )
                    .await
                    .expect("create session");
                let session_id = session1.id().clone();
                let first = session1
                    .send_and_wait("What is 50 + 50?")
                    .await
                    .expect("send first")
                    .expect("first answer");
                assert!(assistant_message_content(&first).contains("100"));
                session1
                    .disconnect()
                    .await
                    .expect("disconnect first session");

                let session2 = client
                    .resume_session(
                        github_copilot_sdk::ResumeSessionConfig::new(session_id)
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                            .with_handler(Arc::new(github_copilot_sdk::handler::ApproveAllHandler))
                            .with_session_fs_provider(provider),
                    )
                    .await
                    .expect("resume session");
                let second = session2
                    .send_and_wait("What is that times 3?")
                    .await
                    .expect("send second")
                    .expect("second answer");
                assert!(assistant_message_content(&second).contains("300"));

                session2
                    .disconnect()
                    .await
                    .expect("disconnect resumed session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_map_all_sessionfs_handler_operations() {
    let root = PathBuf::from("target").join("session-fs-handler-ops");
    if root.exists() {
        std::fs::remove_dir_all(&root).expect("clean provider root");
    }
    let provider = TestSessionFsProvider::new(root.clone(), "handler-session");

    provider
        .mkdir("/workspace/nested", true, None)
        .await
        .expect("mkdir");
    provider
        .write_file("/workspace/nested/file.txt", "hello", None)
        .await
        .expect("write");
    provider
        .append_file("/workspace/nested/file.txt", " world", None)
        .await
        .expect("append");
    assert!(
        provider
            .exists("/workspace/nested/file.txt")
            .await
            .expect("exists")
    );
    let stat = provider
        .stat("/workspace/nested/file.txt")
        .await
        .expect("stat");
    assert!(stat.is_file);
    assert!(!stat.is_directory);
    assert_eq!(stat.size, "hello world".len() as i64);
    assert_eq!(
        provider
            .read_file("/workspace/nested/file.txt")
            .await
            .expect("read"),
        "hello world"
    );
    assert!(
        provider
            .readdir("/workspace/nested")
            .await
            .expect("readdir")
            .iter()
            .any(|entry| entry == "file.txt")
    );
    assert!(
        provider
            .readdir_with_types("/workspace/nested")
            .await
            .expect("readdir types")
            .iter()
            .any(|entry| entry.name == "file.txt" && entry.kind == DirEntryKind::File)
    );
    provider
        .rename(
            "/workspace/nested/file.txt",
            "/workspace/nested/renamed.txt",
        )
        .await
        .expect("rename");
    assert!(
        !provider
            .exists("/workspace/nested/file.txt")
            .await
            .expect("old path missing")
    );
    assert_eq!(
        provider
            .read_file("/workspace/nested/renamed.txt")
            .await
            .expect("read renamed"),
        "hello world"
    );
    provider
        .rm("/workspace/nested/renamed.txt", false, false)
        .await
        .expect("remove");
    assert!(
        !provider
            .exists("/workspace/nested/renamed.txt")
            .await
            .expect("removed missing")
    );
    provider
        .rm("/workspace/nested/missing.txt", false, true)
        .await
        .expect("forced remove");
    assert!(matches!(
        provider.stat("/workspace/nested/missing.txt").await,
        Err(FsError::NotFound(_))
    ));
    let _ = std::fs::remove_dir_all(root);
}

#[tokio::test]
async fn should_reject_setprovider_when_sessions_already_exist() {
    let config = session_fs_config();

    assert_eq!(config.initial_cwd, "/");
    assert_eq!(config.session_state_path, session_state_path());
}

#[tokio::test]
async fn sessionfsprovider_converts_exceptions_to_rpc_errors() {
    let provider = ThrowingSessionFsProvider {
        error: FsError::NotFound("missing".to_string()),
    };
    assert!(matches!(
        provider.read_file("missing.txt").await,
        Err(FsError::NotFound(message)) if message.contains("missing")
    ));
    assert!(
        !provider
            .exists("missing.txt")
            .await
            .expect("exists maps errors to false")
    );
    assert!(matches!(
        provider.write_file("missing.txt", "content", None).await,
        Err(FsError::NotFound(message)) if message.contains("missing")
    ));

    let unknown = ThrowingSessionFsProvider {
        error: FsError::Other("bad path".to_string()),
    };
    assert!(matches!(
        unknown.write_file("bad.txt", "content", None).await,
        Err(FsError::Other(message)) if message.contains("bad path")
    ));
}

#[tokio::test]
async fn should_persist_plan_md_via_sessionfs() {
    with_e2e_context(
        "session_fs",
        "should_persist_plan_md_via_sessionfs",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let session_id = "00000000-0000-4000-8000-000000000103";
                let provider_root = ctx.work_dir().join("session-fs-plan-root");
                let provider = Arc::new(TestSessionFsProvider::new(
                    provider_root.clone(),
                    session_id,
                ));
                let client = start_session_fs_client(ctx, provider.clone()).await;
                let session = client
                    .create_session(session_config(ctx, provider).with_session_id(session_id))
                    .await
                    .expect("create session");

                session.send_and_wait("What is 2 + 3?").await.expect("send");
                session
                    .rpc()
                    .plan()
                    .update(PlanUpdateRequest {
                        content: "# Test Plan\n\nThis is a test.".to_string(),
                    })
                    .await
                    .expect("update plan");
                let plan_path = provider_root
                    .join(session.id().as_ref())
                    .join(provider_relative_path(&session_state_path()))
                    .join("plan.md");
                wait_for_file_containing(&plan_path, "This is a test.").await;
                assert!(
                    std::fs::read_to_string(plan_path)
                        .expect("read plan")
                        .contains("This is a test.")
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_map_large_output_handling_into_sessionfs() {
    let root = PathBuf::from("target").join("session-fs-large-output");
    if root.exists() {
        std::fs::remove_dir_all(&root).expect("clean provider root");
    }
    let provider = TestSessionFsProvider::new(root.clone(), "large-output-session");
    let content = "x".repeat(100_000);

    provider
        .write_file("/session-state/temp/large.txt", &content, None)
        .await
        .expect("write large content");

    assert_eq!(
        provider
            .read_file("/session-state/temp/large.txt")
            .await
            .expect("read large content"),
        content
    );
    let _ = std::fs::remove_dir_all(root);
}

#[tokio::test]
async fn should_succeed_with_compaction_while_using_sessionfs() {
    with_e2e_context(
        "session_fs",
        "should_succeed_with_compaction_while_using_sessionfs",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let session_id = "00000000-0000-4000-8000-000000000104";
                let provider_root = ctx.work_dir().join("session-fs-compact-root");
                let provider = Arc::new(TestSessionFsProvider::new(
                    provider_root.clone(),
                    session_id,
                ));
                let client = start_session_fs_client(ctx, provider.clone()).await;
                let session = client
                    .create_session(session_config(ctx, provider).with_session_id(session_id))
                    .await
                    .expect("create session");

                session.send_and_wait("What is 2+2?").await.expect("send");
                let result = session
                    .rpc()
                    .history()
                    .compact()
                    .await
                    .expect("compact history");
                assert!(result.success);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_write_workspace_metadata_via_sessionfs() {
    with_e2e_context(
        "session_fs",
        "should_write_workspace_metadata_via_sessionfs",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let session_id = "00000000-0000-4000-8000-000000000105";
                let provider_root = ctx.work_dir().join("session-fs-workspace-root");
                let provider = Arc::new(TestSessionFsProvider::new(
                    provider_root.clone(),
                    session_id,
                ));
                let client = start_session_fs_client(ctx, provider.clone()).await;
                let session = client
                    .create_session(session_config(ctx, provider).with_session_id(session_id))
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait("What is 7 * 8?")
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(assistant_message_content(&answer).contains("56"));
                let workspace_path = provider_root
                    .join(session.id().as_ref())
                    .join(provider_relative_path(&session_state_path()))
                    .join("workspace.yaml");
                wait_for_file_containing(&workspace_path, session.id().as_ref()).await;

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

async fn start_session_fs_client(
    ctx: &super::support::E2eContext,
    _provider: Arc<TestSessionFsProvider>,
) -> Client {
    Client::start(ctx.client_options().with_session_fs(session_fs_config()))
        .await
        .expect("start sessionfs client")
}

fn session_config(
    ctx: &super::support::E2eContext,
    provider: Arc<TestSessionFsProvider>,
) -> SessionConfig {
    ctx.approve_all_session_config()
        .with_session_fs_provider(provider)
}

fn session_fs_config() -> SessionFsConfig {
    SessionFsConfig::new("/", session_state_path(), SessionFsConventions::Posix)
}

fn session_state_path() -> String {
    if cfg!(windows) {
        "/session-state".to_string()
    } else {
        std::env::temp_dir()
            .join("copilot-rust-sessionfs-state")
            .join("session-state")
            .to_string_lossy()
            .replace('\\', "/")
    }
}

fn provider_relative_path(path: &str) -> PathBuf {
    PathBuf::from(path.trim_start_matches(['/', '\\']))
}

async fn wait_for_file_containing(path: &Path, needle: &str) {
    wait_for_condition("session fs file content", || async {
        std::fs::read_to_string(path)
            .map(|content| content.contains(needle))
            .unwrap_or(false)
    })
    .await;
}

struct TestSessionFsProvider {
    root: PathBuf,
    session_id: String,
}

impl TestSessionFsProvider {
    fn new(root: PathBuf, session_id: impl Into<String>) -> Self {
        std::fs::create_dir_all(&root).expect("create provider root");
        Self {
            root,
            session_id: session_id.into(),
        }
    }

    fn resolve(&self, path: &str) -> Result<PathBuf, FsError> {
        let root = std::fs::canonicalize(&self.root).map_err(FsError::from)?;
        let mut full = root.clone();
        if self.session_id.is_empty()
            || self.session_id == "."
            || self.session_id == ".."
            || self.session_id.contains('/')
            || self.session_id.contains('\\')
            || self.session_id.contains(':')
        {
            return Err(FsError::Other(format!(
                "invalid sessionfs session id: {}",
                self.session_id
            )));
        }
        full.push(&self.session_id);
        for segment in path
            .trim_start_matches(['/', '\\'])
            .split(['/', '\\'])
            .filter(|segment| !segment.is_empty())
        {
            if segment == "." || segment == ".." || segment.contains(':') {
                return Err(FsError::Other(format!("invalid sessionfs path: {path}")));
            }
            full.push(segment);
        }
        Ok(full)
    }
}

#[async_trait]
impl SessionFsProvider for TestSessionFsProvider {
    async fn read_file(&self, path: &str) -> Result<String, FsError> {
        std::fs::read_to_string(self.resolve(path)?).map_err(FsError::from)
    }

    async fn write_file(
        &self,
        path: &str,
        content: &str,
        _mode: Option<i64>,
    ) -> Result<(), FsError> {
        let path = self.resolve(path)?;
        if let Some(parent) = path.parent() {
            std::fs::create_dir_all(parent).map_err(FsError::from)?;
        }
        std::fs::write(path, content).map_err(FsError::from)
    }

    async fn append_file(
        &self,
        path: &str,
        content: &str,
        _mode: Option<i64>,
    ) -> Result<(), FsError> {
        let path = self.resolve(path)?;
        if let Some(parent) = path.parent() {
            std::fs::create_dir_all(parent).map_err(FsError::from)?;
        }
        use std::io::Write;
        let mut file = std::fs::OpenOptions::new()
            .create(true)
            .append(true)
            .open(path)
            .map_err(FsError::from)?;
        file.write_all(content.as_bytes()).map_err(FsError::from)
    }

    async fn exists(&self, path: &str) -> Result<bool, FsError> {
        Ok(self.resolve(path)?.exists())
    }

    async fn stat(&self, path: &str) -> Result<FileInfo, FsError> {
        let path = self.resolve(path)?;
        let metadata = std::fs::metadata(path).map_err(FsError::from)?;
        Ok(FileInfo::new(
            metadata.is_file(),
            metadata.is_dir(),
            metadata.len() as i64,
            "1970-01-01T00:00:00Z",
            "1970-01-01T00:00:00Z",
        ))
    }

    async fn mkdir(&self, path: &str, _recursive: bool, _mode: Option<i64>) -> Result<(), FsError> {
        std::fs::create_dir_all(self.resolve(path)?).map_err(FsError::from)
    }

    async fn readdir(&self, path: &str) -> Result<Vec<String>, FsError> {
        let mut entries = std::fs::read_dir(self.resolve(path)?)
            .map_err(FsError::from)?
            .map(|entry| {
                entry
                    .map_err(FsError::from)
                    .map(|entry| entry.file_name().to_string_lossy().into_owned())
            })
            .collect::<Result<Vec<_>, _>>()?;
        entries.sort();
        Ok(entries)
    }

    async fn readdir_with_types(&self, path: &str) -> Result<Vec<DirEntry>, FsError> {
        let mut entries = std::fs::read_dir(self.resolve(path)?)
            .map_err(FsError::from)?
            .map(|entry| {
                let entry = entry.map_err(FsError::from)?;
                let kind = if entry.file_type().map_err(FsError::from)?.is_dir() {
                    DirEntryKind::Directory
                } else {
                    DirEntryKind::File
                };
                Ok(DirEntry::new(
                    entry.file_name().to_string_lossy().into_owned(),
                    kind,
                ))
            })
            .collect::<Result<Vec<_>, FsError>>()?;
        entries.sort_by(|left, right| left.name.cmp(&right.name));
        Ok(entries)
    }

    async fn rm(&self, path: &str, recursive: bool, force: bool) -> Result<(), FsError> {
        let path = self.resolve(path)?;
        if path.is_file() {
            return std::fs::remove_file(path).map_err(FsError::from);
        }
        if path.is_dir() {
            if recursive {
                return std::fs::remove_dir_all(path).map_err(FsError::from);
            }
            return std::fs::remove_dir(path).map_err(FsError::from);
        }
        if force {
            Ok(())
        } else {
            Err(FsError::NotFound(format!("not found: {}", path.display())))
        }
    }

    async fn rename(&self, src: &str, dest: &str) -> Result<(), FsError> {
        let src = self.resolve(src)?;
        let dest = self.resolve(dest)?;
        if let Some(parent) = dest.parent() {
            std::fs::create_dir_all(parent).map_err(FsError::from)?;
        }
        std::fs::rename(src, dest).map_err(FsError::from)
    }
}

#[derive(Clone)]
struct ThrowingSessionFsProvider {
    error: FsError,
}

#[async_trait]
impl SessionFsProvider for ThrowingSessionFsProvider {
    async fn read_file(&self, _path: &str) -> Result<String, FsError> {
        Err(self.error.clone())
    }

    async fn write_file(
        &self,
        _path: &str,
        _content: &str,
        _mode: Option<i64>,
    ) -> Result<(), FsError> {
        Err(self.error.clone())
    }

    async fn exists(&self, _path: &str) -> Result<bool, FsError> {
        Ok(false)
    }
}
