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
fn null_id_error_decodes_as_an_uncorrelated_error() {
    let json = r#"{"jsonrpc":"2.0","id":null,"error":{"code":-32700,"message":"recursion limit exceeded at line 1 column 513"}}"#;
    match serde_json::from_str::<JsonRpcMessage>(json).unwrap() {
        JsonRpcMessage::UncorrelatedError(error) => {
            assert_eq!(error.code, -32700);
            assert_eq!(
                error.message,
                "recursion limit exceeded at line 1 column 513"
            );
        }
        other => panic!("expected UncorrelatedError, got {other:?}"),
    }
    // A numeric id is still a response, and a null id must carry an error.
    let json = r#"{"jsonrpc":"2.0","id":3,"error":{"code":-32700,"message":"m"}}"#;
    assert!(matches!(
        serde_json::from_str::<JsonRpcMessage>(json).unwrap(),
        JsonRpcMessage::Response(response) if response.id == 3
    ));
    let json = r#"{"jsonrpc":"2.0","id":null,"result":{}}"#;
    assert!(serde_json::from_str::<JsonRpcMessage>(json).is_err());
}

#[test]
fn an_error_code_wider_than_i32_is_kept() {
    let json = r#"{"jsonrpc":"2.0","id":3,"error":{"code":4294967296,"message":"wide"}}"#;
    let JsonRpcMessage::Response(response) = serde_json::from_str::<JsonRpcMessage>(json).unwrap()
    else {
        panic!("expected a response");
    };
    let error = response.error.unwrap();
    assert_eq!(error.code, 4_294_967_296);
    // The public code is an `i32`: a wider one saturates, and the message keeps it.
    let error = Error::from_rpc(error.code, error.message, None);
    assert_eq!(error.rpc_code(), Some(i32::MAX));
    assert!(
        error
            .message()
            .is_some_and(|message| message.contains("4294967296")),
        "{error}"
    );
}

#[test]
fn a_peer_message_is_repeated_without_its_quoted_values() {
    let message = r#"invalid type: string "ghp_secret\"token", expected u64 at line 1 column 40"#;
    assert_eq!(
        sanitized_peer_message(message),
        "invalid type: string \"…\", expected u64 at line 1 column 40"
    );
    assert_eq!(
        sanitized_peer_message("unknown variant `ghp_secret`, expected one of `a`"),
        "unknown variant `…`, expected one of `…`"
    );
    let long = "x".repeat(500);
    assert_eq!(
        sanitized_peer_message(&long).chars().count(),
        PEER_MESSAGE_MAX_CHARS + 1
    );
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
                JsonRpcMessage::UncorrelatedError(error) => {
                    panic!("no envelope here has a null id: {error:?}")
                }
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
        r#"{"jsonrpc":null,"id":null,"error":{"code":-32700,"message":"m"}}"#,
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

/// The frame the published framing wrote: the header, then `to_vec`'s body.
fn published_frame<T: serde::Serialize>(message: &T) -> Vec<u8> {
    let body = serde_json::to_vec(message).unwrap();
    let mut frame = format!("Content-Length: {}\r\n\r\n", body.len()).into_bytes();
    frame.extend_from_slice(&body);
    frame
}

#[test]
fn encode_frame_writes_the_published_frame() {
    let message = serde_json::json!({
        "jsonrpc": "2.0",
        "id": 7,
        "result": {"content": "a \"quoted\" line\n"}
    });
    assert_eq!(
        encode_frame(&message).unwrap().as_bytes(),
        published_frame(&message)
    );
    // The length gains a digit at 10, 100, 1000 and 10000 bytes. Each body is a
    // JSON string: its characters and two quotes.
    for body_len in [9, 10, 99, 100, 999, 1000, 9999, 10000] {
        let message = "x".repeat(body_len - 2);
        let frame = encode_frame(&message).unwrap();
        assert!(
            frame
                .as_bytes()
                .starts_with(format!("Content-Length: {body_len}\r\n\r\n").as_bytes())
        );
        assert_eq!(frame.as_bytes(), published_frame(&message));
    }
}

#[test]
fn encode_frame_gives_back_large_spare_capacity() {
    // A 64 KiB string is written in one piece, and its closing quote then
    // doubles the buffer to about twice the frame's size.
    let message = "x".repeat(64 * 1024);
    let frame = encode_frame(&message).unwrap();
    assert_eq!(frame.as_bytes(), published_frame(&message));
    assert!(
        frame.buf.capacity() <= frame.buf.len() + 4096,
        "a {}-byte frame keeps {} bytes",
        frame.buf.len(),
        frame.buf.capacity()
    );
}

#[test]
fn encode_frame_writes_the_header_into_the_room_before_the_body() {
    // A 100-byte body: a JSON string of 98 characters.
    let message = "x".repeat(98);
    let frame = encode_frame(&message).unwrap();
    let header: &[u8] = b"Content-Length: 100\r\n\r\n";
    assert_eq!(frame.start, FRAME_PREFIX - header.len());
    assert_eq!(&frame.buf[frame.start..FRAME_PREFIX], header);
    assert_eq!(
        &frame.buf[FRAME_PREFIX..],
        serde_json::to_vec(&message).unwrap().as_slice()
    );
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

/// `Value::Null` inside `levels` nested arrays.
fn nested(levels: usize) -> Value {
    let mut value = Value::Null;
    for _ in 0..levels {
        value = Value::Array(vec![value]);
    }
    value
}

/// `PEER_MAX_NESTING` is what the CLI's parser, `serde_json::from_slice` with
/// its default recursion limit, reads: a response that deep decodes, one level
/// deeper does not, and the guard agrees on both, for a result and for an
/// error's data. A serde_json change to the limit fails this test.
#[test]
fn peer_max_nesting_is_what_serde_json_reads() {
    fn decodes(response: &JsonRpcResponse) -> bool {
        serde_json::from_slice::<Value>(&serde_json::to_vec(response).unwrap()).is_ok()
    }
    let with_result = |levels| JsonRpcResponse {
        jsonrpc: "2.0".into(),
        id: 1,
        result: Some(nested(levels)),
        error: None,
    };
    let with_data = |levels| JsonRpcResponse {
        jsonrpc: "2.0".into(),
        id: 1,
        result: None,
        error: Some(JsonRpcError {
            code: -1,
            message: "m".into(),
            data: Some(nested(levels)),
        }),
    };
    // The response object is one level, and an error object another.
    for (case, at_limit, too_deep) in [
        (
            "result",
            with_result(PEER_MAX_NESTING - 1),
            with_result(PEER_MAX_NESTING),
        ),
        (
            "error data",
            with_data(PEER_MAX_NESTING - 2),
            with_data(PEER_MAX_NESTING - 1),
        ),
    ] {
        assert!(
            decodes(&at_limit),
            "{case}: serde_json reads {PEER_MAX_NESTING} levels"
        );
        assert!(
            !decodes(&too_deep),
            "{case}: serde_json refuses one level more"
        );
        assert!(
            !at_limit.nests_too_deep_for_peer(),
            "{case}: the guard passes the readable response"
        );
        assert!(
            too_deep.nests_too_deep_for_peer(),
            "{case}: the guard stops the unreadable one"
        );
    }
}

/// A request is held to the same limit as a response: params that make the
/// frame `PEER_MAX_NESTING` deep decode and pass the guard, and one level
/// deeper does neither, so the SDK fails that request instead of sending a
/// frame the CLI cannot identify.
#[test]
fn a_request_passes_the_guard_exactly_when_serde_json_reads_it() {
    fn decodes(request: &JsonRpcRequest) -> bool {
        serde_json::from_slice::<Value>(&serde_json::to_vec(request).unwrap()).is_ok()
    }
    // The request object is one level.
    let at_limit = JsonRpcRequest::new(1, "m", Some(nested(PEER_MAX_NESTING - 1)));
    let too_deep = JsonRpcRequest::new(1, "m", Some(nested(PEER_MAX_NESTING)));
    assert!(
        decodes(&at_limit),
        "serde_json reads a request {PEER_MAX_NESTING} levels deep"
    );
    assert!(
        !decodes(&too_deep),
        "serde_json refuses a request one level deeper"
    );
    assert!(
        !at_limit.nests_too_deep_for_peer(),
        "the request guard passes the readable request"
    );
    assert!(
        too_deep.nests_too_deep_for_peer(),
        "the request guard stops the unreadable request"
    );
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
