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
    }
}
