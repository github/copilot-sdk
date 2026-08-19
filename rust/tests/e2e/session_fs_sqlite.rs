use std::collections::HashMap;
use std::sync::{Arc, Mutex};

use async_trait::async_trait;
use github_copilot_sdk::session_fs::{FsError, FsErrorKind};
use github_copilot_sdk::{
    DirEntry, DirEntryKind, FileInfo, SessionConfig, SessionFsCapabilities, SessionFsConfig,
    SessionFsConventions, SessionFsProvider, SessionFsSqliteProvider, SessionFsSqliteQueryResult,
    SessionFsSqliteQueryType, SessionFsSqliteTransactionError, SessionFsSqliteTransactionStatement,
};
use rusqlite::Connection;

static E2E: super::support::SharedE2eGroup =
    super::support::SharedE2eGroup::new("session_fs_sqlite", sqlite_client_options, 2);

#[derive(Debug)]
struct SqliteCall {
    session_id: String,
    query_type: String,
    query: String,
}

struct InMemorySqliteProvider {
    session_id: String,
    files: Mutex<HashMap<String, String>>,
    dirs: Mutex<std::collections::HashSet<String>>,
    db: Mutex<Option<Connection>>,
    sqlite_calls: Arc<Mutex<Vec<SqliteCall>>>,
}

impl InMemorySqliteProvider {
    fn new(session_id: &str, calls: Arc<Mutex<Vec<SqliteCall>>>) -> Self {
        let mut dirs = std::collections::HashSet::new();
        dirs.insert("/".to_string());
        Self {
            session_id: session_id.to_string(),
            files: Mutex::new(HashMap::new()),
            dirs: Mutex::new(dirs),
            db: Mutex::new(None),
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

    fn get_or_create_db(db: &mut Option<Connection>) -> Result<&mut Connection, FsError> {
        if db.is_none() {
            let conn =
                Connection::open_in_memory().map_err(|e| FsError::new(FsErrorKind::Other, e))?;
            conn.execute_batch("PRAGMA busy_timeout = 5000;")
                .map_err(|e| FsError::new(FsErrorKind::Other, e))?;
            *db = Some(conn);
        }
        Ok(db.as_mut().unwrap())
    }
}

#[async_trait]
impl SessionFsProvider for InMemorySqliteProvider {
    async fn read_file(&self, path: &str) -> Result<String, FsError> {
        let files = self.files.lock().unwrap();
        files
            .get(path)
            .cloned()
            .ok_or_else(|| FsError::from(FsErrorKind::NotFound(path.to_string())))
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
            Err(FsError::from(FsErrorKind::NotFound(path.to_string())))
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

    fn sqlite(&self) -> Option<&dyn SessionFsSqliteProvider> {
        Some(self)
    }
}

#[async_trait]
impl SessionFsSqliteProvider for InMemorySqliteProvider {
    async fn sqlite_query(
        &self,
        query_type: SessionFsSqliteQueryType,
        query: &str,
        _params: Option<&HashMap<String, serde_json::Value>>,
    ) -> Result<Option<SessionFsSqliteQueryResult>, FsError> {
        let mut db_guard = self.db.lock().unwrap();
        let db = Self::get_or_create_db(&mut db_guard)?;
        Ok(Some(Self::run_statement(
            db,
            query_type,
            query,
            &self.session_id,
            &self.sqlite_calls,
        )?))
    }

    async fn sqlite_transaction(
        &self,
        statements: &[SessionFsSqliteTransactionStatement],
    ) -> Result<Vec<SessionFsSqliteQueryResult>, SessionFsSqliteTransactionError> {
        let mut db_guard = self.db.lock().unwrap();
        let db = Self::get_or_create_db(&mut db_guard)?;
        db.execute_batch("BEGIN IMMEDIATE")
            .map_err(|e| Self::classify_sqlite_error(&e))?;
        let mut results = Vec::with_capacity(statements.len());
        for statement in statements {
            match Self::run_statement(
                db,
                statement.query_type.clone(),
                &statement.query,
                &self.session_id,
                &self.sqlite_calls,
            ) {
                Ok(result) => results.push(result),
                Err(e) => {
                    let _ = db.execute_batch("ROLLBACK");
                    return Err(Self::classify_error_message(e.to_string()));
                }
            }
        }
        db.execute_batch("COMMIT")
            .map_err(|e| SessionFsSqliteTransactionError::post_commit_ambiguous(e.to_string()))?;
        Ok(results)
    }

    async fn sqlite_exists(&self) -> Result<bool, FsError> {
        Ok(self.db.lock().unwrap().is_some())
    }
}

impl InMemorySqliteProvider {
    fn classify_sqlite_error(error: &rusqlite::Error) -> SessionFsSqliteTransactionError {
        Self::classify_error_message(error.to_string())
    }

    fn classify_error_message(message: String) -> SessionFsSqliteTransactionError {
        if message.contains("locked") || message.contains("busy") {
            SessionFsSqliteTransactionError::busy_or_locked(message)
        } else {
            SessionFsSqliteTransactionError::fatal(message)
        }
    }

    fn run_statement(
        db: &Connection,
        query_type: SessionFsSqliteQueryType,
        query: &str,
        session_id: &str,
        sqlite_calls: &Arc<Mutex<Vec<SqliteCall>>>,
    ) -> Result<SessionFsSqliteQueryResult, FsError> {
        let qt_str = match query_type {
            SessionFsSqliteQueryType::Exec => "exec",
            SessionFsSqliteQueryType::Query => "query",
            SessionFsSqliteQueryType::Run => "run",
            SessionFsSqliteQueryType::Unknown => "unknown",
        };
        sqlite_calls.lock().unwrap().push(SqliteCall {
            session_id: session_id.to_string(),
            query_type: qt_str.to_string(),
            query: query.to_string(),
        });

        let trimmed = query.trim();
        if trimmed.is_empty() {
            return Ok(SessionFsSqliteQueryResult::default());
        }

        match query_type {
            SessionFsSqliteQueryType::Exec => {
                db.execute_batch(trimmed)
                    .map_err(|e| FsError::new(FsErrorKind::Other, e))?;
                Ok(SessionFsSqliteQueryResult::default())
            }
            SessionFsSqliteQueryType::Query => {
                let mut stmt = db
                    .prepare(trimmed)
                    .map_err(|e| FsError::new(FsErrorKind::Other, e))?;
                let col_count = stmt.column_count();
                let columns: Vec<String> = (0..col_count)
                    .map(|i| stmt.column_name(i).unwrap().to_string())
                    .collect();
                let mut rows = vec![];
                let mut query_rows = stmt
                    .query([])
                    .map_err(|e| FsError::new(FsErrorKind::Other, e))?;
                while let Some(row) = query_rows
                    .next()
                    .map_err(|e| FsError::new(FsErrorKind::Other, e))?
                {
                    let mut map = HashMap::new();
                    for (i, col) in columns.iter().enumerate() {
                        let val: rusqlite::types::Value = row
                            .get(i)
                            .map_err(|e| FsError::new(FsErrorKind::Other, e))?;
                        let json_val = match val {
                            rusqlite::types::Value::Null => serde_json::Value::Null,
                            rusqlite::types::Value::Integer(n) => {
                                serde_json::Value::Number(n.into())
                            }
                            rusqlite::types::Value::Real(f) => serde_json::Value::Number(
                                serde_json::Number::from_f64(f).unwrap_or(0.into()),
                            ),
                            rusqlite::types::Value::Text(s) => serde_json::Value::String(s),
                            rusqlite::types::Value::Blob(b) => {
                                serde_json::Value::String(String::from_utf8_lossy(&b).into_owned())
                            }
                        };
                        map.insert(col.clone(), json_val);
                    }
                    rows.push(map);
                }
                Ok(SessionFsSqliteQueryResult {
                    columns,
                    rows,
                    rows_affected: 0,
                    last_insert_rowid: None,
                })
            }
            SessionFsSqliteQueryType::Run => {
                let affected = db
                    .execute(trimmed, [])
                    .map_err(|e| FsError::new(FsErrorKind::Other, e))?;
                let last_id = db.last_insert_rowid();
                Ok(SessionFsSqliteQueryResult {
                    columns: vec![],
                    rows: vec![],
                    rows_affected: affected as i64,
                    last_insert_rowid: Some(last_id),
                })
            }
            _ => Ok(SessionFsSqliteQueryResult::default()),
        }
    }
}

fn session_state_path_sqlite() -> String {
    if cfg!(windows) {
        "/session-state".to_string()
    } else {
        std::env::temp_dir()
            .join("copilot-rust-sessionfs-sqlite-state")
            .join("session-state")
            .to_string_lossy()
            .replace('\\', "/")
    }
}

fn sqlite_session_fs_config() -> SessionFsConfig {
    SessionFsConfig::new(
        "/",
        session_state_path_sqlite(),
        SessionFsConventions::Posix,
    )
    .with_capabilities(SessionFsCapabilities::new().with_sqlite(true))
}

fn sqlite_client_options(
    context: &super::support::E2eContext,
) -> github_copilot_sdk::ClientOptions {
    context
        .client_options()
        .with_session_fs(sqlite_session_fs_config())
}

fn sqlite_session_config(
    ctx: &super::support::E2eContext,
    provider: Arc<InMemorySqliteProvider>,
) -> SessionConfig {
    ctx.approve_all_session_config()
        .with_session_fs_provider(provider)
}

// TODO(cli-1.0.81-2): CLI 1.0.81-2 hangs servicing the nested SQL tool request, so this test
// times out. Re-enable once the runtime reentrancy fix ships.
#[ignore = "blocked on CLI 1.0.81-2 nested tool-request servicing regression"]
#[tokio::test]
async fn should_route_sql_queries_through_the_sessionfs_sqlite_handler() {
    super::support::with_shared_e2e_context(
        &E2E,
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
                let client = ctx.start_client().await;
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
                         Then insert a row with id \"a1\" and name \"Widget\".",
                    )
                    .await
                    .expect("send")
                    .expect("assistant message");
                let _ = answer;

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
                        session_calls.iter().any(|c| c.query_type == "exec"),
                        "expected exec queryType"
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

// TODO(cli-1.0.81-2): CLI 1.0.81-2 hangs servicing the nested SQL tool request, so this test
// times out. Re-enable once the runtime reentrancy fix ships.
#[ignore = "blocked on CLI 1.0.81-2 nested tool-request servicing regression"]
#[tokio::test]
async fn should_allow_subagents_to_use_sql_tool_via_inherited_sessionfs() {
    super::support::with_shared_e2e_context(
        &E2E,
        "session_fs_sqlite",
        "should_allow_subagents_to_use_sql_tool_via_inherited_sessionfs",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let session_id = "00000000-0000-4000-8000-000000000202";
                let sqlite_calls = Arc::new(Mutex::new(Vec::new()));
                let provider = Arc::new(InMemorySqliteProvider::new(session_id, sqlite_calls.clone()));
                let provider_ref = provider.clone();
                let client = ctx.start_client().await;
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
