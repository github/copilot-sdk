/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

//! Inbound `providerToken.*` JSON-RPC request dispatch helpers.

use std::collections::HashMap;
use std::sync::Arc;

use serde::Serialize;
use serde_json::Value;
use tracing::warn;

use crate::generated::api_types::{
    ProviderTokenAcquireRequest, ProviderTokenAcquireResult, rpc_methods,
};
use crate::provider_token::{BearerTokenError, BearerTokenProvider, ProviderTokenArgs};
use crate::{Client, JsonRpcRequest, JsonRpcResponse, error_codes};

async fn respond<T: Serialize>(client: &Client, request_id: u64, result: T) {
    let value = match serde_json::to_value(&result) {
        Ok(value) => value,
        Err(error) => {
            warn!(error = %error, "failed to serialize provider token response");
            send_error(
                client,
                request_id,
                error_codes::INTERNAL_ERROR,
                "serialization failure",
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

async fn send_error(client: &Client, request_id: u64, code: i32, message: &str) {
    let _ = client
        .send_response(&JsonRpcResponse {
            jsonrpc: "2.0".to_string(),
            id: request_id,
            result: None,
            error: Some(crate::JsonRpcError {
                code,
                message: message.to_string(),
                data: None,
            }),
        })
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
            )
            .await;
            None
        }
    }
}

fn token_provider_or_err(
    providers: &HashMap<String, Arc<dyn BearerTokenProvider>>,
    provider_name: &str,
) -> Result<Arc<dyn BearerTokenProvider>, BearerTokenError> {
    providers.get(provider_name).cloned().ok_or_else(|| {
        BearerTokenError::message(format!(
            "No bearer-token provider installed for BYOK provider {provider_name:?}"
        ))
    })
}

async fn get_token(
    client: &Client,
    providers: &HashMap<String, Arc<dyn BearerTokenProvider>>,
    request: JsonRpcRequest,
) {
    let Some(params) = parse_params::<ProviderTokenAcquireRequest>(client, &request).await else {
        return;
    };

    let token_provider = match token_provider_or_err(providers, &params.provider_name) {
        Ok(provider) => provider,
        Err(error) => {
            send_error(
                client,
                request.id,
                error_codes::INTERNAL_ERROR,
                &error.to_string(),
            )
            .await;
            return;
        }
    };

    match token_provider
        .get_token(ProviderTokenArgs {
            provider_name: params.provider_name,
            session_id: params.session_id.into_inner(),
        })
        .await
    {
        Ok(token) => respond(client, request.id, ProviderTokenAcquireResult { token }).await,
        Err(error) => {
            send_error(
                client,
                request.id,
                error_codes::INTERNAL_ERROR,
                &format!("Bearer-token provider failed: {error}"),
            )
            .await;
        }
    }
}

pub(crate) async fn dispatch(
    client: &Client,
    providers: &HashMap<String, Arc<dyn BearerTokenProvider>>,
    request: JsonRpcRequest,
) {
    let method = request.method.as_str();
    match method {
        rpc_methods::PROVIDERTOKEN_GETTOKEN => get_token(client, providers, request).await,
        _ => {
            warn!(method = %method, "unknown providerToken.* method");
            send_error(
                client,
                request.id,
                error_codes::METHOD_NOT_FOUND,
                &format!("unknown method: {method}"),
            )
            .await;
        }
    }
}
