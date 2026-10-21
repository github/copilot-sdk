/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use super::*;

struct LocalHandler;

#[async_trait]
impl CopilotRequestHandler for LocalHandler {
    async fn send_request(
        &self,
        _request: CopilotHttpRequest,
        _ctx: &CopilotRequestContext,
    ) -> Result<CopilotHttpResponse, CopilotRequestError> {
        Ok(CopilotHttpResponse::new(
            200,
            None,
            HeaderMap::new(),
            Box::pin(futures_util::stream::empty()),
        ))
    }

    async fn open_websocket(
        &self,
        _ctx: &CopilotRequestContext,
        _response: CopilotWebSocketResponse,
    ) -> Result<Box<dyn CopilotWebSocketHandler>, CopilotRequestError> {
        Ok(Box::new(LocalWebSocket))
    }
}

struct LocalWebSocket;

#[async_trait]
impl CopilotWebSocketHandler for LocalWebSocket {
    async fn send_request_message(
        &self,
        _message: CopilotWebSocketMessage,
    ) -> Result<(), CopilotRequestError> {
        Ok(())
    }

    async fn close(&self) -> Result<(), CopilotRequestError> {
        Ok(())
    }
}

#[test]
fn local_runtime_keeps_explicit_http_and_websocket_handlers() {
    let _handler: Arc<dyn CopilotRequestHandler> = Arc::new(LocalHandler);
}
