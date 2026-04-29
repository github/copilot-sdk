//! Inbound `sessionFs.*` JSON-RPC request dispatch helpers.
//!
//! Internal — public-facing trait lives in `crate::session_fs`. Each helper
//! deserializes the typed request, calls the [`SessionFsProvider`] method,
//! and serializes the schema response with `FsError` mapped onto the wire's
//! `SessionFsError` variant.

use std::sync::Arc;

use serde::Serialize;
use serde_json::Value;
use tracing::warn;

use crate::generated::api_types::{
    SessionFsAppendFileRequest, SessionFsExistsRequest, SessionFsExistsResult,
    SessionFsMkdirRequest, SessionFsReadFileRequest, SessionFsReadFileResult,
    SessionFsReaddirRequest, SessionFsReaddirResult, SessionFsReaddirWithTypesRequest,
    SessionFsReaddirWithTypesResult, SessionFsRenameRequest, SessionFsRmRequest,
    SessionFsStatRequest, SessionFsStatResult, SessionFsWriteFileRequest,
};
use crate::session_fs::{FsError, SessionFsProvider};
use crate::{Client, JsonRpcRequest, JsonRpcResponse, error_codes};

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
        Err(e) => respond(client, id, e.into_wire()).await,
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
    // Match Node's `createSessionFsAdapter`: errors collapse to `exists: false`.
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
        "sessionFs.writeFile" => write_file(client, &provider, request).await,
        "sessionFs.appendFile" => append_file(client, &provider, request).await,
        "sessionFs.exists" => exists(client, &provider, request).await,
        "sessionFs.stat" => stat(client, &provider, request).await,
        "sessionFs.mkdir" => mkdir(client, &provider, request).await,
        "sessionFs.readdir" => readdir(client, &provider, request).await,
        "sessionFs.readdirWithTypes" => readdir_with_types(client, &provider, request).await,
        "sessionFs.rm" => rm(client, &provider, request).await,
        "sessionFs.rename" => rename(client, &provider, request).await,
        _ => {
            warn!(method = %method, "unknown sessionFs.* method");
            send_error(client, request.id, "unknown sessionFs method").await;
        }
    }
    true
}

// FsError is used through `into_wire()` calls above.
#[allow(dead_code)]
fn _ensure_fs_error_used(_e: FsError) {}
