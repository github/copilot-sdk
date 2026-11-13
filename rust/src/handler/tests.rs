/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use super::*;

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
