//! Inbound `sessionFs.*` JSON-RPC request dispatch helpers.
//!
//! Internal — public-facing trait lives in `crate::session_fs`. Each helper
//! deserializes the typed request, calls the [`SessionFsProvider`] method,
//! and serializes the schema response with `FsError` mapped onto the wire's
//! `SessionFsError` variant.

use std::sync::Arc;

use base64::Engine as _;
use serde::Serialize;
use serde_json::Value;
use tracing::warn;

use crate::generated::api_types::{
    SessionFsAppendFileRequest, SessionFsError, SessionFsErrorCode, SessionFsExistsRequest,
    SessionFsExistsResult, SessionFsMkdirRequest, SessionFsReadFileBytesRequest,
    SessionFsReadFileBytesResult, SessionFsReadFileRequest, SessionFsReadFileResult,
    SessionFsReaddirRequest, SessionFsReaddirResult, SessionFsReaddirWithTypesRequest,
    SessionFsReaddirWithTypesResult, SessionFsRenameRequest, SessionFsRmRequest,
    SessionFsSqliteExistsParams, SessionFsSqliteExistsResult, SessionFsSqliteQueryRequest,
    SessionFsSqliteQueryResult as GeneratedSqliteQueryResult,
    SessionFsSqliteTransactionError as GeneratedSqliteTransactionError,
    SessionFsSqliteTransactionErrorClass, SessionFsSqliteTransactionRequest,
    SessionFsSqliteTransactionResult as GeneratedSqliteTransactionResult, SessionFsStatRequest,
    SessionFsStatResult, SessionFsWriteFileBytesRequest, SessionFsWriteFileRequest,
};
use crate::session_fs::SessionFsProvider;
use crate::{Client, JsonRpcRequest, JsonRpcResponse, error_codes};

const MAX_BINARY_BYTES: usize = (64 * 1024 * 1024 - 1024) / 4 * 3;

/// Helper: serialize a typed result, send the response.
async fn respond<T: Serialize>(client: &Client, request_id: u64, result: T) {
    let value = match serde_json::to_value(&result) {
        Ok(v) => v,
        Err(e) => {
            warn!(error = %e, "failed to serialize sessionFs response");
            send_error(client, request_id, "serialization failure").await;
            return;
        }
    };
    let _ = client
        .send_response(&JsonRpcResponse {
            jsonrpc: "2.0".to_string(),
            id: request_id,
            result: Some(value),
            error: None,
        })
        .await;
}

async fn send_error(client: &Client, request_id: u64, message: &str) {
    let _ = client
        .send_response(&JsonRpcResponse {
            jsonrpc: "2.0".to_string(),
            id: request_id,
            result: None,
            error: Some(crate::JsonRpcError {
                code: error_codes::INTERNAL_ERROR,
                message: message.to_string(),
                data: None,
            }),
        })
        .await;
}

fn parse_params<T: serde::de::DeserializeOwned>(request: &JsonRpcRequest) -> Option<T> {
    request
        .params
        .as_ref()
        .and_then(|p| serde_json::from_value(p.clone()).ok())
}

pub(crate) async fn read_file(
    client: &Client,
    provider: &Arc<dyn SessionFsProvider>,
    request: JsonRpcRequest,
) {
    let params: SessionFsReadFileRequest = match parse_params(&request) {
        Some(p) => p,
        None => {
            send_error(client, request.id, "invalid sessionFs.readFile params").await;
            return;
        }
    };
    let id = request.id;
    let result = match provider.read_file(&params.path).await {
        Ok(content) => SessionFsReadFileResult {
            content,
            error: None,
        },
        Err(e) => SessionFsReadFileResult {
            content: String::new(),
            error: Some(e.into_wire()),
        },
    };
    respond(client, id, result).await;
}

pub(crate) async fn read_file_bytes(
    client: &Client,
    provider: &Arc<dyn SessionFsProvider>,
    request: JsonRpcRequest,
) {
    let params: SessionFsReadFileBytesRequest = match parse_params(&request) {
        Some(params) => params,
        None => {
            send_error(client, request.id, "invalid sessionFs.readFileBytes params").await;
            return;
        }
    };
    let result = match provider.binary() {
        Some(binary) => binary.read_file_bytes(&params.path).await,
        None => Err(crate::session_fs::FsError::with_message(
            crate::session_fs::FsErrorKind::Other,
            "binary reads are not supported",
        )),
    };
    respond(client, request.id, binary_read_response(result)).await;
}

fn binary_read_response(
    result: Result<Vec<u8>, crate::session_fs::FsError>,
) -> SessionFsReadFileBytesResult {
    match result {
        Ok(bytes) if bytes.len() > MAX_BINARY_BYTES => SessionFsReadFileBytesResult {
            content: String::new(),
            error: Some(
                crate::session_fs::FsError::with_message(
                    crate::session_fs::FsErrorKind::Other,
                    "sessionFs.readFileBytes content exceeds the binary read limit",
                )
                .into_wire(),
            ),
        },
        Ok(bytes) => SessionFsReadFileBytesResult {
            content: base64::engine::general_purpose::STANDARD.encode(bytes),
            error: None,
        },
        Err(error) => SessionFsReadFileBytesResult {
            content: String::new(),
            error: Some(error.into_wire()),
        },
    }
}

pub(crate) async fn write_file(
    client: &Client,
    provider: &Arc<dyn SessionFsProvider>,
    request: JsonRpcRequest,
) {
    let params: SessionFsWriteFileRequest = match parse_params(&request) {
        Some(p) => p,
        None => {
            send_error(client, request.id, "invalid sessionFs.writeFile params").await;
            return;
        }
    };
    let id = request.id;
    match provider
        .write_file(&params.path, &params.content, params.mode)
        .await
    {
        Ok(()) => respond(client, id, Value::Null).await,
        Err(e) => respond(client, id, e.into_write_wire()).await,
    }
}

pub(crate) async fn write_file_bytes(
    client: &Client,
    provider: &Arc<dyn SessionFsProvider>,
    request: JsonRpcRequest,
) {
    let params: SessionFsWriteFileBytesRequest = match parse_params(&request) {
        Some(params) => params,
        None => {
            send_error(
                client,
                request.id,
                "invalid sessionFs.writeFileBytes params",
            )
            .await;
            return;
        }
    };
    let result = if params.content.len() > MAX_BINARY_BYTES.div_ceil(3) * 4 {
        Err(crate::session_fs::FsError::with_message(
            crate::session_fs::FsErrorKind::Other,
            "sessionFs.writeFileBytes content exceeds the binary write limit",
        ))
    } else {
        match base64::engine::general_purpose::STANDARD.decode(&params.content) {
            Ok(content) => match provider.binary() {
                Some(binary) => {
                    binary
                        .write_file_bytes(&params.path, &content, params.mode)
                        .await
                }
                None => Err(crate::session_fs::FsError::with_message(
                    crate::session_fs::FsErrorKind::Other,
                    "binary writes are not supported",
                )),
            },
            Err(error) => Err(crate::session_fs::FsError::with_message(
                crate::session_fs::FsErrorKind::Other,
                format!("invalid sessionFs.writeFileBytes base64 content: {error}"),
            )),
        }
    };
    match result {
        Ok(()) => respond(client, request.id, Value::Null).await,
        Err(error) => respond(client, request.id, error.into_wire()).await,
    }
}

pub(crate) async fn append_file(
    client: &Client,
    provider: &Arc<dyn SessionFsProvider>,
    request: JsonRpcRequest,
) {
    let params: SessionFsAppendFileRequest = match parse_params(&request) {
        Some(p) => p,
        None => {
            send_error(client, request.id, "invalid sessionFs.appendFile params").await;
            return;
        }
    };
    let id = request.id;
    match provider
        .append_file(&params.path, &params.content, params.mode)
        .await
    {
        Ok(()) => respond(client, id, Value::Null).await,
        Err(e) => respond(client, id, e.into_wire()).await,
    }
}

pub(crate) async fn exists(
    client: &Client,
    provider: &Arc<dyn SessionFsProvider>,
    request: JsonRpcRequest,
) {
    let params: SessionFsExistsRequest = match parse_params(&request) {
        Some(p) => p,
        None => {
            send_error(client, request.id, "invalid sessionFs.exists params").await;
            return;
        }
    };
    let id = request.id;
    let exists_value = provider.exists(&params.path).await.unwrap_or(false);
    respond(
        client,
        id,
        SessionFsExistsResult {
            exists: exists_value,
        },
    )
    .await;
}

pub(crate) async fn stat(
    client: &Client,
    provider: &Arc<dyn SessionFsProvider>,
    request: JsonRpcRequest,
) {
    let params: SessionFsStatRequest = match parse_params(&request) {
        Some(p) => p,
        None => {
            send_error(client, request.id, "invalid sessionFs.stat params").await;
            return;
        }
    };
    let id = request.id;
    let result = match provider.stat(&params.path).await {
        Ok(info) => info.into_wire(),
        Err(e) => SessionFsStatResult {
            is_file: false,
            is_directory: false,
            size: 0,
            mtime: String::new(),
            birthtime: String::new(),
            error: Some(e.into_wire()),
        },
    };
    respond(client, id, result).await;
}

pub(crate) async fn mkdir(
    client: &Client,
    provider: &Arc<dyn SessionFsProvider>,
    request: JsonRpcRequest,
) {
    let params: SessionFsMkdirRequest = match parse_params(&request) {
        Some(p) => p,
        None => {
            send_error(client, request.id, "invalid sessionFs.mkdir params").await;
            return;
        }
    };
    let id = request.id;
    let recursive = params.recursive.unwrap_or(false);
    match provider.mkdir(&params.path, recursive, params.mode).await {
        Ok(()) => respond(client, id, Value::Null).await,
        Err(e) => respond(client, id, e.into_wire()).await,
    }
}

pub(crate) async fn readdir(
    client: &Client,
    provider: &Arc<dyn SessionFsProvider>,
    request: JsonRpcRequest,
) {
    let params: SessionFsReaddirRequest = match parse_params(&request) {
        Some(p) => p,
        None => {
            send_error(client, request.id, "invalid sessionFs.readdir params").await;
            return;
        }
    };
    let id = request.id;
    let result = match provider.readdir(&params.path).await {
        Ok(entries) => SessionFsReaddirResult {
            entries,
            error: None,
        },
        Err(e) => SessionFsReaddirResult {
            entries: Vec::new(),
            error: Some(e.into_wire()),
        },
    };
    respond(client, id, result).await;
}

pub(crate) async fn readdir_with_types(
    client: &Client,
    provider: &Arc<dyn SessionFsProvider>,
    request: JsonRpcRequest,
) {
    let params: SessionFsReaddirWithTypesRequest = match parse_params(&request) {
        Some(p) => p,
        None => {
            send_error(
                client,
                request.id,
                "invalid sessionFs.readdirWithTypes params",
            )
            .await;
            return;
        }
    };
    let id = request.id;
    let result = match provider.readdir_with_types(&params.path).await {
        Ok(entries) => SessionFsReaddirWithTypesResult {
            entries: entries.into_iter().map(|e| e.into_wire()).collect(),
            error: None,
        },
        Err(e) => SessionFsReaddirWithTypesResult {
            entries: Vec::new(),
            error: Some(e.into_wire()),
        },
    };
    respond(client, id, result).await;
}

pub(crate) async fn rm(
    client: &Client,
    provider: &Arc<dyn SessionFsProvider>,
    request: JsonRpcRequest,
) {
    let params: SessionFsRmRequest = match parse_params(&request) {
        Some(p) => p,
        None => {
            send_error(client, request.id, "invalid sessionFs.rm params").await;
            return;
        }
    };
    let id = request.id;
    let recursive = params.recursive.unwrap_or(false);
    let force = params.force.unwrap_or(false);
    match provider.rm(&params.path, recursive, force).await {
        Ok(()) => respond(client, id, Value::Null).await,
        Err(e) => respond(client, id, e.into_wire()).await,
    }
}

pub(crate) async fn rename(
    client: &Client,
    provider: &Arc<dyn SessionFsProvider>,
    request: JsonRpcRequest,
) {
    let params: SessionFsRenameRequest = match parse_params(&request) {
        Some(p) => p,
        None => {
            send_error(client, request.id, "invalid sessionFs.rename params").await;
            return;
        }
    };
    let id = request.id;
    match provider.rename(&params.src, &params.dest).await {
        Ok(()) => respond(client, id, Value::Null).await,
        Err(e) => respond(client, id, e.into_wire()).await,
    }
}

pub(crate) async fn sqlite_query(
    client: &Client,
    provider: &Arc<dyn SessionFsProvider>,
    request: JsonRpcRequest,
) {
    let params: SessionFsSqliteQueryRequest = match parse_params(&request) {
        Some(p) => p,
        None => {
            send_error(client, request.id, "invalid sessionFs.sqliteQuery params").await;
            return;
        }
    };
    let id = request.id;
    let sqlite = match provider.sqlite() {
        Some(s) => s,
        None => {
            // SQLite not supported — return a result-level error, not a
            // transport error, so the CLI can surface it gracefully.
            respond(
                client,
                id,
                GeneratedSqliteQueryResult {
                    columns: Vec::new(),
                    error: Some(SessionFsError {
                        code: SessionFsErrorCode::UNKNOWN,
                        message: Some(
                            "SQLite is not supported by this SessionFs provider".to_string(),
                        ),
                        write_changed: None,
                    }),
                    last_insert_rowid: None,
                    rows: Vec::new(),
                    rows_affected: 0,
                },
            )
            .await;
            return;
        }
    };
    let sqlite_params = params.params.as_ref().filter(|p| !p.is_empty());
    let result = match sqlite
        .sqlite_query(params.query_type, &params.query, sqlite_params)
        .await
    {
        Ok(Some(result)) => GeneratedSqliteQueryResult {
            columns: result.columns,
            rows: result.rows,
            rows_affected: result.rows_affected,
            last_insert_rowid: result.last_insert_rowid,
            error: None,
        },
        Ok(None) => GeneratedSqliteQueryResult {
            columns: Vec::new(),
            rows: Vec::new(),
            rows_affected: 0,
            last_insert_rowid: None,
            error: None,
        },
        Err(e) => GeneratedSqliteQueryResult {
            columns: Vec::new(),
            error: Some(e.into_wire()),
            last_insert_rowid: None,
            rows: Vec::new(),
            rows_affected: 0,
        },
    };
    respond(client, id, result).await;
}

pub(crate) async fn sqlite_transaction(
    client: &Client,
    provider: &Arc<dyn SessionFsProvider>,
    request: JsonRpcRequest,
) {
    let params: SessionFsSqliteTransactionRequest = match parse_params(&request) {
        Some(p) => p,
        None => {
            send_error(
                client,
                request.id,
                "invalid sessionFs.sqliteTransaction params",
            )
            .await;
            return;
        }
    };
    let id = request.id;
    let sqlite = match provider.sqlite() {
        Some(s) => s,
        None => {
            // SQLite not supported — return a result-level error, not a
            // transport error, so the CLI can surface it gracefully.
            respond(
                client,
                id,
                GeneratedSqliteTransactionResult {
                    results: Vec::new(),
                    error: Some(GeneratedSqliteTransactionError {
                        error_class: SessionFsSqliteTransactionErrorClass::Fatal,
                        message: "SQLite is not supported by this SessionFs provider".to_string(),
                    }),
                },
            )
            .await;
            return;
        }
    };
    let result = match sqlite.sqlite_transaction(&params.statements).await {
        Ok(results) => GeneratedSqliteTransactionResult {
            results: results
                .into_iter()
                .map(|result| GeneratedSqliteQueryResult {
                    columns: result.columns,
                    rows: result.rows,
                    rows_affected: result.rows_affected,
                    last_insert_rowid: result.last_insert_rowid,
                    error: None,
                })
                .collect(),
            error: None,
        },
        Err(e) => GeneratedSqliteTransactionResult {
            results: Vec::new(),
            error: Some(GeneratedSqliteTransactionError {
                error_class: e.error_class,
                message: e.message,
            }),
        },
    };
    respond(client, id, result).await;
}

pub(crate) async fn sqlite_exists(
    client: &Client,
    provider: &Arc<dyn SessionFsProvider>,
    request: JsonRpcRequest,
) {
    let _params: SessionFsSqliteExistsParams = match parse_params(&request) {
        Some(p) => p,
        None => {
            send_error(client, request.id, "invalid sessionFs.sqliteExists params").await;
            return;
        }
    };
    let id = request.id;
    let result = match provider.sqlite() {
        Some(sqlite) => match sqlite.sqlite_exists().await {
            Ok(exists) => SessionFsSqliteExistsResult { exists },
            Err(_) => SessionFsSqliteExistsResult { exists: false },
        },
        None => SessionFsSqliteExistsResult { exists: false },
    };
    respond(client, id, result).await;
}

/// Dispatch a `sessionFs.*` request to the appropriate handler. Returns
/// `true` if the request was a session-fs method (whether or not a provider
/// was registered), `false` otherwise (caller should continue matching).
pub(crate) async fn dispatch(
    client: &Client,
    provider: Option<&Arc<dyn SessionFsProvider>>,
    request: JsonRpcRequest,
) -> bool {
    let method = request.method.as_str();
    if !method.starts_with("sessionFs.") {
        return false;
    }
    let provider = match provider {
        Some(p) => p.clone(),
        None => {
            warn!(method = %method, "sessionFs request without registered provider");
            send_error(
                client,
                request.id,
                "no sessionFs provider registered for this session",
            )
            .await;
            return true;
        }
    };
    match method {
        "sessionFs.readFile" => read_file(client, &provider, request).await,
        "sessionFs.readFileBytes" => read_file_bytes(client, &provider, request).await,
        "sessionFs.writeFileBytes" => write_file_bytes(client, &provider, request).await,
        "sessionFs.writeFile" => write_file(client, &provider, request).await,
        "sessionFs.appendFile" => append_file(client, &provider, request).await,
        "sessionFs.exists" => exists(client, &provider, request).await,
        "sessionFs.stat" => stat(client, &provider, request).await,
        "sessionFs.mkdir" => mkdir(client, &provider, request).await,
        "sessionFs.readdir" => readdir(client, &provider, request).await,
        "sessionFs.readdirWithTypes" => readdir_with_types(client, &provider, request).await,
        "sessionFs.rm" => rm(client, &provider, request).await,
        "sessionFs.rename" => rename(client, &provider, request).await,
        "sessionFs.sqliteQuery" => sqlite_query(client, &provider, request).await,
        "sessionFs.sqliteTransaction" => sqlite_transaction(client, &provider, request).await,
        "sessionFs.sqliteExists" => sqlite_exists(client, &provider, request).await,
        _ => {
            warn!(method = %method, "unknown sessionFs.* method");
            send_error(client, request.id, "unknown sessionFs method").await;
        }
    }
    true
}

#[cfg(test)]
mod tests;
