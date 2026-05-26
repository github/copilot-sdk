//! Inbound `canvas.*` JSON-RPC request dispatch helpers.
//!
//! Internal — public-facing trait lives in `crate::canvas`. Each helper
//! deserializes the generated wire request, calls the user-facing
//! [`CanvasHandler`] method, and serializes the result back onto JSON-RPC.

use std::sync::Arc;

use serde::Serialize;
use serde_json::Value;
use tracing::warn;

use crate::canvas::{CanvasError, CanvasHandler};
use crate::generated::api_types::{
    CanvasProviderCloseRequest, CanvasProviderInvokeActionRequest, CanvasProviderOpenRequest,
    rpc_methods,
};
use crate::{Client, JsonRpcRequest, JsonRpcResponse, error_codes};

async fn respond<T: Serialize>(client: &Client, request_id: u64, result: T) {
    let value = match serde_json::to_value(&result) {
        Ok(value) => value,
        Err(error) => {
            warn!(error = %error, "failed to serialize canvas response");
            send_error(
                client,
                request_id,
                error_codes::INTERNAL_ERROR,
                "serialization failure",
                None,
            )
            .await;
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

async fn send_error(
    client: &Client,
    request_id: u64,
    code: i32,
    message: &str,
    data: Option<Value>,
) {
    let _ = client
        .send_response(&JsonRpcResponse {
            jsonrpc: "2.0".to_string(),
            id: request_id,
            result: None,
            error: Some(crate::JsonRpcError {
                code,
                message: message.to_string(),
                data,
            }),
        })
        .await;
}

async fn send_canvas_error(client: &Client, request_id: u64, error: CanvasError) {
    let message = error.message.clone();
    let data = Some(serde_json::json!({
        "code": error.code,
        "message": message,
    }));
    send_error(
        client,
        request_id,
        error_codes::INTERNAL_ERROR,
        &error.message,
        data,
    )
    .await;
}

async fn parse_params<T: serde::de::DeserializeOwned>(
    client: &Client,
    request: &JsonRpcRequest,
) -> Option<T> {
    let params = request
        .params
        .as_ref()
        .cloned()
        .unwrap_or(Value::Object(serde_json::Map::new()));
    match serde_json::from_value(params) {
        Ok(params) => Some(params),
        Err(error) => {
            send_error(
                client,
                request.id,
                error_codes::INVALID_PARAMS,
                &format!("invalid params: {error}"),
                None,
            )
            .await;
            None
        }
    }
}

fn canvas_handler_or_err(
    handler: Option<&Arc<dyn CanvasHandler>>,
) -> Result<Arc<dyn CanvasHandler>, CanvasError> {
    handler.cloned().ok_or_else(|| {
        CanvasError::new(
            "canvas_handler_unset",
            "No CanvasHandler installed on this session; call SessionConfig::with_canvas_handler before creating the session.",
        )
    })
}

async fn open(client: &Client, handler: &Arc<dyn CanvasHandler>, request: JsonRpcRequest) {
    let Some(params) = parse_params::<CanvasProviderOpenRequest>(client, &request).await else {
        return;
    };

    match handler.on_open(params).await {
        Ok(result) => respond(client, request.id, result).await,
        Err(error) => send_canvas_error(client, request.id, error).await,
    }
}

async fn close(client: &Client, handler: &Arc<dyn CanvasHandler>, request: JsonRpcRequest) {
    let Some(params) = parse_params::<CanvasProviderCloseRequest>(client, &request).await else {
        return;
    };

    match handler.on_close(params).await {
        Ok(()) => respond(client, request.id, Value::Null).await,
        Err(error) => send_canvas_error(client, request.id, error).await,
    }
}

async fn invoke_action(client: &Client, handler: &Arc<dyn CanvasHandler>, request: JsonRpcRequest) {
    let Some(params) = parse_params::<CanvasProviderInvokeActionRequest>(client, &request).await
    else {
        return;
    };

    match handler.on_action(params).await {
        Ok(result) => respond(client, request.id, result).await,
        Err(error) => send_canvas_error(client, request.id, error).await,
    }
}

/// Dispatch a `canvas.*` request to the appropriate handler. Returns `true`
/// if the request was a canvas method, `false` otherwise.
pub(crate) async fn dispatch(
    client: &Client,
    handler: Option<&Arc<dyn CanvasHandler>>,
    request: JsonRpcRequest,
) -> bool {
    let method = request.method.as_str();
    if !method.starts_with("canvas.") {
        return false;
    }

    let handler = match canvas_handler_or_err(handler) {
        Ok(handler) => handler,
        Err(error) => {
            send_canvas_error(client, request.id, error).await;
            return true;
        }
    };

    match method {
        rpc_methods::CANVAS_OPEN => open(client, &handler, request).await,
        rpc_methods::CANVAS_CLOSE => close(client, &handler, request).await,
        rpc_methods::CANVAS_INVOKEACTION => invoke_action(client, &handler, request).await,
        _ => {
            warn!(method = %method, "unknown canvas.* method");
            send_error(
                client,
                request.id,
                error_codes::METHOD_NOT_FOUND,
                &format!("unknown method: {method}"),
                None,
            )
            .await;
        }
    }

    true
}
