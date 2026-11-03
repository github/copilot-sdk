/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use super::*;
use crate::generated::api_types::{
    CanvasProviderInvokeActionRequest, CanvasProviderOpenRequest, CanvasProviderOpenResult,
};
use crate::types::SessionId;

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
