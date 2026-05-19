use std::collections::HashMap;
use std::sync::{Arc, Mutex};

use async_trait::async_trait;
use github_copilot_sdk::{
    Client, DirEntry, DirEntryKind, FileInfo, FsError, SessionConfig, SessionFsCapabilities,
    SessionFsConfig, SessionFsConventions, SessionFsProvider, SessionFsSqliteQueryResult,
    SessionFsSqliteQueryType,
};

use super::support::{assistant_message_content, with_e2e_context};

#[derive(Debug)]
struct SqliteCall {
    session_id: String,
    query_type: String,
    query: String,
}

/// In-memory SessionFsProvider with stub SQLite handler.
///
/// Returns canned responses based on query type rather than executing real SQL,
/// since the CAPI replay snapshots contain pre-recorded tool results.
struct InMemorySqliteProvider {
    files: Mutex<HashMap<String, String>>,
    dirs: Mutex<std::collections::HashSet<String>>,
    had_query: Mutex<bool>,
    sqlite_calls: Arc<Mutex<Vec<SqliteCall>>>,
}

impl InMemorySqliteProvider {
    fn new(_session_id: &str, calls: Arc<Mutex<Vec<SqliteCall>>>) -> Self {
        let mut dirs = std::collections::HashSet::new();
        dirs.insert("/".to_string());
        Self {
            files: Mutex::new(HashMap::new()),
            dirs: Mutex::new(dirs),
            had_query: Mutex::new(false),
            sqlite_calls: calls,
        }
    }

    fn ensure_parent(dirs: &mut std::collections::HashSet<String>, path: &str) {
        let parts: Vec<&str> = path.trim_end_matches('/').split('/').collect();
        for i in 1..parts.len() {
            let parent = parts[..i].join("/");
            if parent.is_empty() {
                dirs.insert("/".to_string());
            } else {
                dirs.insert(parent);
            }
        }
    }
}

#[async_trait]
impl SessionFsProvider for InMemorySqliteProvider {
    async fn read_file(&self, path: &str) -> Result<String, FsError> {
        let files = self.files.lock().unwrap();
        files
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
        let mut files = self.files.lock().unwrap();
        let mut dirs = self.dirs.lock().unwrap();
        Self::ensure_parent(&mut dirs, path);
        files.insert(path.to_string(), content.to_string());
        Ok(())
    }

    async fn append_file(
        &self,
        path: &str,
        content: &str,
        _mode: Option<i64>,
    ) -> Result<(), FsError> {
        let mut files = self.files.lock().unwrap();
        let mut dirs = self.dirs.lock().unwrap();
        Self::ensure_parent(&mut dirs, path);
        let entry = files.entry(path.to_string()).or_default();
        entry.push_str(content);
        Ok(())
    }

    async fn exists(&self, path: &str) -> Result<bool, FsError> {
        let files = self.files.lock().unwrap();
        let dirs = self.dirs.lock().unwrap();
        Ok(files.contains_key(path) || dirs.contains(path))
    }

    async fn stat(&self, path: &str) -> Result<FileInfo, FsError> {
        let files = self.files.lock().unwrap();
        let dirs = self.dirs.lock().unwrap();
        let now = "1970-01-01T00:00:00Z";
        if dirs.contains(path) {
            Ok(FileInfo::new(false, true, 0, now, now))
        } else if let Some(content) = files.get(path) {
            Ok(FileInfo::new(true, false, content.len() as i64, now, now))
        } else {
            Err(FsError::NotFound(path.to_string()))
        }
    }

    async fn mkdir(&self, path: &str, recursive: bool, _mode: Option<i64>) -> Result<(), FsError> {
        let mut dirs = self.dirs.lock().unwrap();
        if recursive {
            let parts: Vec<&str> = path.trim_end_matches('/').split('/').collect();
            for i in 1..=parts.len() {
                let p = parts[..i].join("/");
                if p.is_empty() {
                    dirs.insert("/".to_string());
                } else {
                    dirs.insert(p);
                }
            }
        } else {
            dirs.insert(path.to_string());
        }
        Ok(())
    }

    async fn readdir(&self, path: &str) -> Result<Vec<String>, FsError> {
        let files = self.files.lock().unwrap();
        let dirs = self.dirs.lock().unwrap();
        let prefix = format!("{}/", path.trim_end_matches('/'));
        let mut names = std::collections::BTreeSet::new();
        for p in files.keys().chain(dirs.iter()) {
            if let Some(name) = p
                .strip_prefix(&prefix)
                .and_then(|rest| rest.split('/').next())
                .filter(|n| !n.is_empty())
            {
                names.insert(name.to_string());
            }
        }
        Ok(names.into_iter().collect())
    }

    async fn readdir_with_types(&self, path: &str) -> Result<Vec<DirEntry>, FsError> {
        let files = self.files.lock().unwrap();
        let dirs = self.dirs.lock().unwrap();
        let prefix = format!("{}/", path.trim_end_matches('/'));
        let mut entries: HashMap<String, DirEntryKind> = HashMap::new();
        for d in dirs.iter() {
            if let Some(name) = d
                .strip_prefix(&prefix)
                .and_then(|rest| rest.split('/').next())
                .filter(|n| !n.is_empty())
            {
                entries.insert(name.to_string(), DirEntryKind::Directory);
            }
        }
        for f in files.keys() {
            if let Some(name) = f
                .strip_prefix(&prefix)
                .and_then(|rest| rest.split('/').next())
                .filter(|n| !n.is_empty())
            {
                entries
                    .entry(name.to_string())
                    .or_insert(DirEntryKind::File);
            }
        }
        let mut result: Vec<DirEntry> = entries
            .into_iter()
            .map(|(name, kind)| DirEntry::new(name, kind))
            .collect();
        result.sort_by(|a, b| a.name.cmp(&b.name));
        Ok(result)
    }

    async fn rm(&self, path: &str, _recursive: bool, _force: bool) -> Result<(), FsError> {
        let mut files = self.files.lock().unwrap();
        let mut dirs = self.dirs.lock().unwrap();
        files.remove(path);
        dirs.remove(path);
        Ok(())
    }

    async fn rename(&self, src: &str, dest: &str) -> Result<(), FsError> {
        let mut files = self.files.lock().unwrap();
        let mut dirs = self.dirs.lock().unwrap();
        if let Some(content) = files.remove(src) {
            Self::ensure_parent(&mut dirs, dest);
            files.insert(dest.to_string(), content);
        }
        Ok(())
    }

    async fn sqlite_query(
        &self,
        session_id: &str,
        query: &str,
        query_type: SessionFsSqliteQueryType,
        _params: Option<&HashMap<String, serde_json::Value>>,
    ) -> Result<SessionFsSqliteQueryResult, FsError> {
        let qt_str = match query_type {
            SessionFsSqliteQueryType::Exec => "exec",
            SessionFsSqliteQueryType::Query => "query",
            SessionFsSqliteQueryType::Run => "run",
            SessionFsSqliteQueryType::Unknown => "unknown",
        };
        self.sqlite_calls.lock().unwrap().push(SqliteCall {
            session_id: session_id.to_string(),
            query_type: qt_str.to_string(),
            query: query.to_string(),
        });
        *self.had_query.lock().unwrap() = true;

        // Return canned results based on query type. The CLI formats tool results from the
        // SessionFsSqliteQueryResult, and the CAPI replay snapshots contain the expected formatted
        // output. These stubs produce results that match the snapshot expectations.
        let upper = query.trim().to_uppercase();
        match query_type {
            SessionFsSqliteQueryType::Exec => Ok(SessionFsSqliteQueryResult {
                columns: vec![],
                rows: vec![],
                rows_affected: 0,
                last_insert_rowid: None,
                error: None,
            }),
            SessionFsSqliteQueryType::Run => Ok(SessionFsSqliteQueryResult {
                columns: vec![],
                rows: vec![],
                rows_affected: 1,
                last_insert_rowid: Some(1.0),
                error: None,
            }),
            SessionFsSqliteQueryType::Query => {
                if upper.contains("SELECT") {
                    Ok(SessionFsSqliteQueryResult {
                        columns: vec!["id".to_string(), "name".to_string()],
                        rows: vec![{
                            let mut m = HashMap::new();
                            m.insert(
                                "id".to_string(),
                                serde_json::Value::String("a1".to_string()),
                            );
                            m.insert(
                                "name".to_string(),
                                serde_json::Value::String("Widget".to_string()),
                            );
                            m
                        }],
                        rows_affected: 0,
                        last_insert_rowid: None,
                        error: None,
                    })
                } else {
                    Ok(SessionFsSqliteQueryResult {
                        columns: vec![],
                        rows: vec![],
                        rows_affected: 0,
                        last_insert_rowid: None,
                        error: None,
                    })
                }
            }
            _ => Ok(SessionFsSqliteQueryResult {
                columns: vec![],
                rows: vec![],
                rows_affected: 0,
                last_insert_rowid: None,
                error: None,
            }),
        }
    }

    async fn sqlite_exists(&self, _session_id: &str) -> Result<bool, FsError> {
        Ok(*self.had_query.lock().unwrap())
    }
}

fn session_state_path_sqlite() -> String {
    "/session-state".to_string()
}

fn sqlite_session_fs_config() -> SessionFsConfig {
    SessionFsConfig::new(
        "/",
        session_state_path_sqlite(),
        SessionFsConventions::Posix,
    )
    .with_capabilities(SessionFsCapabilities::new().with_sqlite(true))
}

async fn start_sqlite_client(ctx: &super::support::E2eContext) -> Client {
    Client::start(
        ctx.client_options()
            .with_session_fs(sqlite_session_fs_config()),
    )
    .await
    .expect("start sqlite client")
}

fn sqlite_session_config(
    ctx: &super::support::E2eContext,
    provider: Arc<InMemorySqliteProvider>,
) -> SessionConfig {
    ctx.approve_all_session_config()
        .with_session_fs_provider(provider)
}

#[tokio::test]
async fn should_route_sql_queries_through_the_sessionfs_sqlite_handler() {
    with_e2e_context(
        "session_fs_sqlite",
        "should_route_sql_queries_through_the_sessionfs_sqlite_handler",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let session_id = "00000000-0000-4000-8000-000000000201";
                let sqlite_calls = Arc::new(Mutex::new(Vec::new()));
                let provider = Arc::new(InMemorySqliteProvider::new(
                    session_id,
                    sqlite_calls.clone(),
                ));
                let client = start_sqlite_client(ctx).await;
                let session = client
                    .create_session(
                        sqlite_session_config(ctx, provider).with_session_id(session_id),
                    )
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait(
                        "Use the sql tool to create a table called \"items\" with columns \
                         id (TEXT PRIMARY KEY) and name (TEXT). \
                         Then insert a row with id \"a1\" and name \"Widget\". \
                         Then select all rows from items and tell me what you find.",
                    )
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(
                    assistant_message_content(&answer).contains("Widget"),
                    "expected 'Widget' in response"
                );

                {
                    let calls = sqlite_calls.lock().unwrap();
                    let session_calls: Vec<&SqliteCall> = calls
                        .iter()
                        .filter(|c| c.session_id == session_id)
                        .collect();
                    assert!(!session_calls.is_empty(), "expected sqlite calls");
                    assert!(
                        session_calls
                            .iter()
                            .any(|c| c.query.to_uppercase().contains("CREATE TABLE")),
                        "expected CREATE TABLE"
                    );
                    assert!(
                        session_calls
                            .iter()
                            .any(|c| c.query.to_uppercase().contains("INSERT")),
                        "expected INSERT"
                    );
                    assert!(
                        session_calls
                            .iter()
                            .any(|c| c.query.to_uppercase().contains("SELECT")),
                        "expected SELECT"
                    );
                    assert!(
                        session_calls.iter().any(|c| c.query_type == "exec"),
                        "expected exec queryType"
                    );
                    assert!(
                        session_calls.iter().any(|c| c.query_type == "query"),
                        "expected query queryType"
                    );
                    assert!(
                        session_calls.iter().any(|c| c.query_type == "run"),
                        "expected run queryType"
                    );
                }

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_allow_subagents_to_use_sql_tool_via_inherited_sessionfs() {
    with_e2e_context(
        "session_fs_sqlite",
        "should_allow_subagents_to_use_sql_tool_via_inherited_sessionfs",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let session_id = "00000000-0000-4000-8000-000000000202";
                let sqlite_calls = Arc::new(Mutex::new(Vec::new()));
                let provider = Arc::new(InMemorySqliteProvider::new(session_id, sqlite_calls.clone()));
                let provider_ref = provider.clone();
                let client = start_sqlite_client(ctx).await;
                let session = client
                    .create_session(
                        sqlite_session_config(ctx, provider).with_session_id(session_id),
                    )
                    .await
                    .expect("create session");

                session
                    .send_and_wait(
                        "Use the task tool to ask a task agent to do the following: \
                         Use the sql tool to run this query: INSERT INTO todos \
                         (id, title, status) VALUES ('subagent-test', 'Created by subagent', 'done')",
                    )
                    .await
                    .expect("send");

                session.disconnect().await.expect("disconnect session");

                {
                    let calls = sqlite_calls.lock().unwrap();
                    let session_calls: Vec<&SqliteCall> =
                        calls.iter().filter(|c| c.session_id == session_id).collect();
                    let insert_calls: Vec<&&SqliteCall> = session_calls
                        .iter()
                        .filter(|c| c.query.to_uppercase().contains("INSERT"))
                        .collect();
                    assert!(!insert_calls.is_empty(), "expected INSERT calls from subagent");
                }

                // Read events.jsonl from in-memory FS
                let events_path = format!("{}/events.jsonl", session_state_path_sqlite());
                let content = provider_ref
                    .read_file(&events_path)
                    .await
                    .expect("read events.jsonl");
                let lines: Vec<&str> = content.lines().filter(|l| !l.is_empty()).collect();
                let sql_tool_events: Vec<serde_json::Value> = lines
                    .iter()
                    .filter_map(|line| serde_json::from_str::<serde_json::Value>(line).ok())
                    .filter(|e| {
                        e.get("type").and_then(|t| t.as_str()) == Some("tool.execution_start")
                            && e.get("data")
                                .and_then(|d| d.get("toolName"))
                                .and_then(|t| t.as_str())
                                == Some("sql")
                    })
                    .collect();
                assert!(
                    !sql_tool_events.is_empty(),
                    "expected sql tool events in events.jsonl"
                );
                for e in &sql_tool_events {
                    assert!(
                        e.get("agentId").is_some()
                            && e.get("agentId") != Some(&serde_json::Value::Null)
                            && e.get("agentId").and_then(|v| v.as_str()) != Some(""),
                        "expected agentId on sql tool event"
                    );
                }

                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}
