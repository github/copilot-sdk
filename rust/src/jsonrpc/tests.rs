/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use super::*;

#[test]
fn listener_negotiation_logs_do_not_echo_remote_errors() {
    let message = "request rejected: token=listener-secret-sentinel";
    for method in ["host.getConfiguration", "host.ready"] {
        assert_eq!(
            remote_error_log_message(method, message),
            "listener negotiation request rejected"
        );
    }
    assert_eq!(remote_error_log_message("ping", message), message);
}

#[test]
fn deserialize_notification() {
    let json = r#"{"jsonrpc":"2.0","method":"session.event","params":{"id":"e1"}}"#;
    let msg: JsonRpcMessage = serde_json::from_str(json).unwrap();
    assert!(matches!(msg, JsonRpcMessage::Notification(n) if n.method == "session.event"));
}

#[test]
fn deserialize_request() {
    let json =
        r#"{"jsonrpc":"2.0","id":5,"method":"permission.request","params":{"kind":"shell"}}"#;
    let msg: JsonRpcMessage = serde_json::from_str(json).unwrap();
    assert!(
        matches!(msg, JsonRpcMessage::Request(r) if r.id == 5 && r.method == "permission.request")
    );
}

#[test]
fn deserialize_response_with_result() {
    let json = r#"{"jsonrpc":"2.0","id":3,"result":{"ok":true}}"#;
    let msg: JsonRpcMessage = serde_json::from_str(json).unwrap();
    assert!(matches!(msg, JsonRpcMessage::Response(r) if r.id == 3 && !r.is_error()));
}

#[test]
fn deserialize_error_response() {
    let json = r#"{"jsonrpc":"2.0","id":7,"error":{"code":-32600,"message":"Invalid Request","data":{"nested":[1,{"reason":"invalid"}]}}}"#;
    let msg: JsonRpcMessage = serde_json::from_str(json).unwrap();
    match msg {
        JsonRpcMessage::Response(r) => {
            assert!(r.is_error());
            let err = r.error.unwrap();
            assert_eq!(err.code, -32600);
            assert_eq!(err.message, "Invalid Request");
            assert_eq!(
                err.data,
                Some(serde_json::json!({"nested": [1, {"reason": "invalid"}]}))
            );
        }
        other => panic!("expected Response, got {other:?}"),
    }
}

#[test]
fn deserialize_rejects_non_object() {
    let result = serde_json::from_str::<JsonRpcMessage>(r#""not an object""#);
    assert!(result.is_err());
}

#[test]
fn deserialize_preserves_optional_payloads() {
    for payload in [
        None,
        Some(Value::Null),
        Some(serde_json::json!(false)),
        Some(serde_json::json!(42)),
        Some(serde_json::json!("text")),
        Some(serde_json::json!([{"nested": [1, null, true]}])),
        Some(serde_json::json!({"rows": [{"content": "result"}]})),
    ] {
        for mut envelope in [
            serde_json::json!({"jsonrpc": "2.0", "method": "notify"}),
            serde_json::json!({"jsonrpc": "2.0", "id": 1, "method": "request"}),
            serde_json::json!({"jsonrpc": "2.0", "id": 1}),
        ] {
            let (payload_key, ignored_key) = if envelope.get("method").is_some() {
                ("params", "result")
            } else {
                ("result", "params")
            };
            envelope[ignored_key] = serde_json::json!({"ignored": "opposite payload"});
            if let Some(payload) = &payload {
                envelope[payload_key] = payload.clone();
            }
            let actual = match serde_json::from_value::<JsonRpcMessage>(envelope).unwrap() {
                JsonRpcMessage::Request(request) => request.params,
                JsonRpcMessage::Response(response) => response.result,
                JsonRpcMessage::Notification(notification) => notification.params,
            };
            assert_eq!(actual, payload.clone().filter(|value| !value.is_null()));
        }
    }
}

#[test]
fn deserialize_rejects_invalid_metadata() {
    for json in [
        r#"{"jsonrpc":null,"method":"notify","params":{"nested":[1]}}"#,
        r#"{"jsonrpc":"2.0","method":42,"params":{"nested":[1]}}"#,
        r#"{"jsonrpc":"2.0","id":null,"result":{}}"#,
        r#"{"jsonrpc":"2.0","id":"1","result":{}}"#,
        r#"{"jsonrpc":"2.0","id":-1,"result":{}}"#,
        r#"{"jsonrpc":"2.0","id":1,"method":null,"params":{}}"#,
        r#"{"jsonrpc":"2.0","id":1,"result":{},"error":{"code":"bad","message":"error"}}"#,
    ] {
        assert!(
            serde_json::from_str::<JsonRpcMessage>(json).is_err(),
            "{json}"
        );
    }
}

#[test]
fn request_new_sets_version() {
    let req = JsonRpcRequest::new(42, "test.method", None);
    assert_eq!(req.jsonrpc, "2.0");
    assert_eq!(req.id, 42);
    assert_eq!(req.method, "test.method");
    assert!(req.params.is_none());
}

#[test]
fn request_serializes_camel_case() {
    let req = JsonRpcRequest::new(1, "ping", Some(serde_json::json!({})));
    let json = serde_json::to_string(&req).unwrap();
    assert!(json.contains(r#""jsonrpc":"2.0""#));
    assert!(json.contains(r#""id":1"#));
    assert!(json.contains(r#""method":"ping""#));
}

#[test]
fn notification_without_params_omits_field() {
    let n = JsonRpcNotification {
        jsonrpc: "2.0".into(),
        method: "ping".into(),
        params: None,
    };
    let json = serde_json::to_string(&n).unwrap();
    assert!(!json.contains("params"));
}

#[test]
fn response_without_error_omits_field() {
    let r = JsonRpcResponse {
        jsonrpc: "2.0".into(),
        id: 1,
        result: Some(serde_json::json!(true)),
        error: None,
    };
    let json = serde_json::to_string(&r).unwrap();
    assert!(!json.contains("error"));
}
