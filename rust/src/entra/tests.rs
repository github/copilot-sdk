#![cfg(test)]

use super::EntraTokenProviderResult;

#[test]
fn result_serializes_wire_fields_without_exposing_credentials_in_debug() {
    let result = EntraTokenProviderResult::Token {
        account_id: "host-account".to_owned(),
        access_token: "host-secret".to_owned(),
    };
    assert_eq!(
        serde_json::to_value(&result).unwrap(),
        serde_json::json!({
            "kind": "token",
            "accountId": "host-account",
            "accessToken": "host-secret",
        })
    );
    assert_eq!(format!("{result:?}"), "Token { .. }");
    let identity = EntraTokenProviderResult::Identity {
        account_id: "host-account".to_owned(),
        username: Some("display-name".to_owned()),
    };
    assert_eq!(format!("{identity:?}"), "Identity { .. }");
    assert_eq!(
        serde_json::to_value(EntraTokenProviderResult::Cancelled).unwrap(),
        serde_json::json!({"kind": "cancelled"})
    );
}
