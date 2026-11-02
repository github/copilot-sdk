//! Reverse request dispatch for host-owned session creation.

use super::*;

pub(super) async fn handle_session_creation_request(
    client: &Client,
    session_id: &SessionId,
    handler: &Arc<parking_lot::RwLock<Option<Arc<dyn crate::SessionCreationHandler>>>>,
    request: &crate::JsonRpcRequest,
) {
    let Some(pending) = client.inner.rpc.cancellable_requests.claim(request.id) else {
        return;
    };
    let handler = handler.read().clone();
    let params = request.params.clone().unwrap_or_default();
    let params = serde_json::from_value::<crate::rpc::SessionManagementCreateHostRequest>(params);
    let result = match (handler, params) {
        (Some(handler), Ok(params))
            if params.parent_session_id == session_id.as_str()
                && params.session_id == *session_id =>
        {
            let closed = client.inner.rpc.connection_closed_token();
            tokio::select! {
                biased;
                _ = closed.cancelled() => return,
                _ = pending.cancellation().cancelled() => {
                    let _ = send_error_response(client, request.id, error_codes::REQUEST_CANCELLED, "Session creation cancelled").await;
                    return;
                }
                result = handler.create_session(params) => result.and_then(|value| serde_json::to_value(value).map_err(Into::into)),
            }
        }
        _ => {
            let _ = send_error_response(
                client,
                request.id,
                error_codes::INVALID_PARAMS,
                "Session creation handler is unavailable",
            )
            .await;
            return;
        }
    };
    match result {
        Ok(result) => {
            let _ = client
                .send_response(&JsonRpcResponse {
                    jsonrpc: "2.0".to_owned(),
                    id: request.id,
                    result: Some(result),
                    error: None,
                })
                .await;
        }
        Err(_) => {
            let _ = send_error_response(
                client,
                request.id,
                error_codes::INTERNAL_ERROR,
                "Session creation callback failed",
            )
            .await;
        }
    }
}
