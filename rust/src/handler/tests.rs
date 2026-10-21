/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use super::*;

#[tokio::test]
async fn approve_all_handler_returns_approved() {
    let result = ApproveAllHandler
        .handle(
            SessionId::from("s1"),
            RequestId::new("1"),
            PermissionRequestData::default(),
        )
        .await;
    assert!(matches!(
        result,
        PermissionResult::Decision {
            decision: PermissionDecision::ApproveOnce(_),
            ..
        }
    ));
}

#[tokio::test]
async fn approve_all_handler_fails_when_managed_settings_enabled() {
    let result = ApproveAllHandler
        .handle(
            SessionId::from("s1"),
            RequestId::new("1"),
            PermissionRequestData {
                managed_settings_enabled: true,
                ..Default::default()
            },
        )
        .await;
    assert!(matches!(
        result,
        PermissionResult::Decision {
            decision: PermissionDecision::UserNotAvailable(_),
            ..
        }
    ));
}

#[tokio::test]
async fn approve_all_handler_leaves_managed_approval_pending() {
    let result = ApproveAllHandler
        .handle(
            SessionId::from("s1"),
            RequestId::new("1"),
            PermissionRequestData {
                managed_approval_required: Some(true),
                ..Default::default()
            },
        )
        .await;
    assert!(matches!(result, PermissionResult::NoResult));
}

#[tokio::test]
async fn deny_all_handler_returns_denied() {
    let result = DenyAllHandler
        .handle(
            SessionId::from("s1"),
            RequestId::new("1"),
            PermissionRequestData::default(),
        )
        .await;
    assert!(matches!(
        result,
        PermissionResult::Decision {
            decision: PermissionDecision::Reject(_),
            ..
        }
    ));
}

#[test]
fn mcp_auth_result_token_converts_to_wire_response() {
    let wire = McpAuthResult::Token {
        access_token: "host-token".to_string(),
        token_type: Some("Bearer".to_string()),
        expires_in: Some(3600),
    }
    .into_wire();

    match wire {
        McpOauthPendingRequestResponse::Token(token) => {
            assert_eq!(token.access_token, "host-token");
            assert_eq!(token.token_type.as_deref(), Some("Bearer"));
            assert_eq!(token.expires_in, Some(3600));
        }
        McpOauthPendingRequestResponse::Cancelled(_) => panic!("expected token response"),
        McpOauthPendingRequestResponse::RuntimeRefresh(_) => panic!("expected token response"),
    }
}

#[test]
fn mcp_auth_runtime_refresh_is_not_a_token_or_cancellation() {
    let wire =
        serde_json::to_value(McpAuthResponse::RuntimeRefresh.into_wire_for_request(true)).unwrap();
    assert_eq!(wire, serde_json::json!({"kind":"runtimeRefresh"}));
}

#[test]
fn mcp_auth_runtime_refresh_requires_advertised_capability() {
    let unsupported =
        serde_json::to_value(McpAuthResponse::RuntimeRefresh.into_wire_for_request(false)).unwrap();
    assert_eq!(unsupported, serde_json::json!({"kind":"cancelled"}));
    let supported =
        serde_json::to_value(McpAuthResponse::RuntimeRefresh.into_wire_for_request(true)).unwrap();
    assert_eq!(supported, serde_json::json!({"kind":"runtimeRefresh"}));
}

#[test]
fn old_oauth_requests_do_not_imply_runtime_refresh_support() {
    let data: crate::session_events::McpOauthRequiredData =
        serde_json::from_value(serde_json::json!({
            "requestId": "old-request",
            "serverName": "remote",
            "serverUrl": "https://example.test/mcp",
            "reason": "refresh",
        }))
        .unwrap();
    assert!(!data.runtime_refresh_supported.unwrap_or(false));
}

#[test]
fn mcp_auth_response_capability_truth_table() {
    for supported in [None, Some(false), Some(true)] {
        for response in ["runtimeRefresh", "token", "cancelled"] {
            let result = match response {
                "runtimeRefresh" => McpAuthResponse::RuntimeRefresh,
                "token" => McpAuthResponse::Legacy(McpAuthResult::Token {
                    access_token: "host-token".to_owned(),
                    token_type: None,
                    expires_in: None,
                }),
                _ => McpAuthResponse::Legacy(McpAuthResult::Cancelled),
            };
            let expected = match response {
                "token" => serde_json::json!({"kind":"token","accessToken":"host-token"}),
                "runtimeRefresh" if supported == Some(true) => {
                    serde_json::json!({"kind":"runtimeRefresh"})
                }
                _ => serde_json::json!({"kind":"cancelled"}),
            };
            assert_eq!(
                serde_json::to_value(result.into_wire_for_request(supported.unwrap_or(false)))
                    .unwrap(),
                expected,
                "capability={supported:?}, response={response}",
            );
        }
    }
}

#[test]
fn legacy_mcp_request_literals_and_exhaustive_results_remain_source_compatible() {
    let request = McpAuthRequest {
        request_id: RequestId::new("legacy"),
        server_name: "remote".to_owned(),
        server_url: "https://example.test/mcp".to_owned(),
        reason: McpOauthRequestReason::Refresh,
        www_authenticate_params: None,
        resource_metadata: None,
        static_client_config: None,
    };
    let McpAuthRequest {
        request_id,
        server_name,
        server_url,
        reason,
        www_authenticate_params,
        resource_metadata,
        static_client_config,
    } = request;
    assert_eq!(&*request_id, "legacy");
    assert_eq!(server_name, "remote");
    assert_eq!(server_url, "https://example.test/mcp");
    assert!(matches!(reason, McpOauthRequestReason::Refresh));
    assert!(www_authenticate_params.is_none());
    assert!(resource_metadata.is_none());
    assert!(static_client_config.is_none());
    let legacy = McpAuthResult::Cancelled;
    match legacy {
        McpAuthResult::Token { .. } => panic!("expected legacy cancellation"),
        McpAuthResult::Cancelled => {}
    }
}

#[tokio::test]
async fn legacy_mcp_handler_default_extension_preserves_token_response() {
    struct LegacyHandler;

    #[async_trait]
    impl McpAuthHandler for LegacyHandler {
        async fn handle(
            &self,
            _session: SessionId,
            _id: RequestId,
            request: McpAuthRequest,
        ) -> McpAuthResult {
            assert_eq!(request.server_name, "remote");
            McpAuthResult::Token {
                access_token: "host-token".to_owned(),
                token_type: None,
                expires_in: None,
            }
        }
    }
    for supported in [false, true] {
        let response = LegacyHandler
            .handle_with_runtime_refresh(
                SessionId::from("legacy-session"),
                RequestId::new("legacy"),
                McpAuthRequest {
                    request_id: RequestId::new("legacy"),
                    server_name: "remote".to_owned(),
                    server_url: "https://example.test/mcp".to_owned(),
                    reason: McpOauthRequestReason::Refresh,
                    www_authenticate_params: None,
                    resource_metadata: None,
                    static_client_config: None,
                },
                supported,
            )
            .await;
        let wire = serde_json::to_value(response.into_wire_for_request(supported)).unwrap();
        assert_eq!(
            wire,
            serde_json::json!({"kind":"token","accessToken":"host-token"})
        );
    }
}
