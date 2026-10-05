/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use serde_json::json;

use super::*;
use crate::generated::api_types::{
    CanvasProviderInvokeActionRequest, CanvasProviderOpenRequest, CanvasProviderOpenResult,
};
use crate::types::SessionId;

struct EchoHandler;

#[async_trait]
impl CanvasHandler for EchoHandler {
    async fn on_open(
        &self,
        ctx: CanvasProviderOpenRequest,
    ) -> CanvasResult<CanvasProviderOpenResult> {
        Ok(CanvasProviderOpenResult {
            url: Some(format!("https://example.test/{}", ctx.canvas_id)),
            title: Some("Echo".to_string()),
            status: Some("ready".to_string()),
        })
    }

    async fn on_action(&self, ctx: CanvasProviderInvokeActionRequest) -> CanvasResult<Value> {
        Ok(json!({ "echoed": ctx.action_name, "input": ctx.input }))
    }
}

#[test]
fn declaration_serializes_camel_case_and_skips_none() {
    let decl = CanvasDeclaration {
        id: "counter".to_string(),
        display_name: "Counter".to_string(),
        description: "Count things".to_string(),
        input_schema: None,
        actions: Some(vec![CanvasAction {
            name: "increment".to_string(),
            description: Some("bump".to_string()),
            input_schema: None,
        }]),
    };

    let value = serde_json::to_value(&decl).unwrap();

    assert_eq!(value["id"], "counter");
    assert_eq!(value["displayName"], "Counter");
    assert_eq!(value["description"], "Count things");
    assert_eq!(value["actions"][0]["name"], "increment");
}

#[tokio::test]
async fn handler_on_open_returns_response() {
    let handler = EchoHandler;
    let response = handler
        .on_open(CanvasProviderOpenRequest {
            session_id: SessionId::from("s1"),
            extension_id: "project:echo".to_string(),
            canvas_id: "echo".to_string(),
            instance_id: "echo-1".to_string(),
            input: Some(json!({ "x": 1 })),
            host: None,
            session: None,
        })
        .await
        .unwrap();

    assert_eq!(response.url.as_deref(), Some("https://example.test/echo"));
    assert_eq!(response.title.as_deref(), Some("Echo"));
    assert_eq!(response.status.as_deref(), Some("ready"));
}

#[tokio::test]
async fn handler_on_action_returns_value() {
    let handler = EchoHandler;
    let result = handler
        .on_action(CanvasProviderInvokeActionRequest {
            session_id: SessionId::from("s1"),
            extension_id: "project:echo".to_string(),
            canvas_id: "echo".to_string(),
            instance_id: "inst-1".to_string(),
            action_name: "shout".to_string(),
            input: Some(json!("hi")),
            host: None,
            session: None,
        })
        .await
        .unwrap();

    assert_eq!(result["echoed"], "shout");
    assert_eq!(result["input"], "hi");
}

#[tokio::test]
async fn default_on_action_returns_no_handler_error() {
    struct OpenOnly;
    #[async_trait]
    impl CanvasHandler for OpenOnly {
        async fn on_open(
            &self,
            _ctx: CanvasProviderOpenRequest,
        ) -> CanvasResult<CanvasProviderOpenResult> {
            Ok(CanvasProviderOpenResult {
                url: None,
                title: None,
                status: None,
            })
        }
    }

    let err = OpenOnly
        .on_action(CanvasProviderInvokeActionRequest {
            session_id: SessionId::from("s1"),
            extension_id: "project:open-only".to_string(),
            canvas_id: "x".to_string(),
            instance_id: "x-1".to_string(),
            action_name: "anything".to_string(),
            input: Some(Value::Null),
            host: None,
            session: None,
        })
        .await
        .unwrap_err();

    assert_eq!(err.code, "canvas_action_no_handler");
}
