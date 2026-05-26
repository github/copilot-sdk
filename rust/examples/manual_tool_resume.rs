//! Demonstrates manually resolving permission and external tool requests across resumes.

use std::time::Duration;

use github_copilot_sdk::generated::api_types::{
    HandlePendingToolCallRequest, PermissionDecision, PermissionDecisionApproveOnce,
    PermissionDecisionApproveOnceKind, PermissionDecisionRequest,
};
use github_copilot_sdk::generated::session_events::{
    AssistantMessageData, ExternalToolRequestedData, PermissionRequestedData, SessionEventType,
};
use github_copilot_sdk::{
    Client, ClientOptions, EventSubscription, RecvError, ResumeSessionConfig, SessionConfig,
};
use serde_json::json;

const TOOL_NAME: &str = "manual_resume_status";

fn manual_tool() -> github_copilot_sdk::Tool {
    // No handler is registered for this tool, so the SDK leaves execution pending.
    github_copilot_sdk::Tool::new(TOOL_NAME)
        .with_description("Looks up a status value. The SDK consumer supplies the result manually.")
        .with_parameters(json!({
            "type": "object",
            "properties": {
                "id": {
                    "type": "string",
                    "description": "Identifier to look up"
                }
            },
            "required": ["id"]
        }))
}

async fn wait_for_permission(
    events: &mut EventSubscription,
) -> Result<PermissionRequestedData, RecvError> {
    loop {
        let event = events.recv().await?;
        if event.parsed_type() == SessionEventType::PermissionRequested
            && let Some(data) = event.typed_data::<PermissionRequestedData>()
        {
            return Ok(data);
        }
    }
}

async fn wait_for_tool(
    events: &mut EventSubscription,
) -> Result<ExternalToolRequestedData, RecvError> {
    loop {
        let event = events.recv().await?;
        if event.parsed_type() == SessionEventType::ExternalToolRequested
            && let Some(data) = event.typed_data::<ExternalToolRequestedData>()
            && data.tool_name == TOOL_NAME
        {
            return Ok(data);
        }
    }
}

async fn wait_for_assistant(events: &mut EventSubscription) -> Result<String, RecvError> {
    loop {
        let event = events.recv().await?;
        if event.parsed_type() == SessionEventType::AssistantMessage
            && let Some(data) = event.typed_data::<AssistantMessageData>()
        {
            return Ok(data.content);
        }
    }
}

async fn pause() {
    println!("Simulating time passing...\n");
    tokio::time::sleep(Duration::from_secs(1)).await;
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let tool = manual_tool();

    // 1. Create a session with a declaration-only tool, then stop after the permission prompt.
    let client1 = Client::start(ClientOptions::default()).await?;
    let session1 = client1
        .create_session(SessionConfig::default().with_tools([tool.clone()]))
        .await?;
    let session_id = session1.id().clone();

    // Subscribe before sending so the permission event cannot be missed.
    let mut permission_events = session1.subscribe();
    session1
        .send("Use the manual_resume_status tool with id 'alpha', then tell me the status.")
        .await?;

    let permission = wait_for_permission(&mut permission_events).await?;
    client1.force_stop();
    pause().await;

    // 2. Resume pending work and grant permission to invoke the tool.
    let client2 = Client::start(ClientOptions::default()).await?;
    let session2 = client2
        .resume_session(
            ResumeSessionConfig::new(session_id.clone())
                .with_tools([tool.clone()])
                .with_continue_pending_work(true),
        )
        .await?;

    // Subscribe before approving so the external tool request cannot be missed.
    let mut tool_events = session2.subscribe();
    session2
        .rpc()
        .permissions()
        .handle_pending_permission_request(PermissionDecisionRequest {
            request_id: permission.request_id,
            result: PermissionDecision::ApproveOnce(PermissionDecisionApproveOnce {
                kind: PermissionDecisionApproveOnceKind::ApproveOnce,
            }),
        })
        .await?;

    let tool_request = wait_for_tool(&mut tool_events).await?;
    client2.force_stop();
    pause().await;

    // 3. Resume again and manually provide the pending tool result.
    let client3 = Client::start(ClientOptions::default()).await?;
    let session3 = client3
        .resume_session(
            ResumeSessionConfig::new(session_id)
                .with_tools([tool])
                .with_continue_pending_work(true),
        )
        .await?;

    let mut assistant_events = session3.subscribe();
    session3
        .rpc()
        .tools()
        .handle_pending_tool_call(HandlePendingToolCallRequest {
            request_id: tool_request.request_id,
            result: Some(json!("MANUAL_STATUS_READY")),
            error: None,
        })
        .await?;

    let answer = wait_for_assistant(&mut assistant_events).await?;
    println!("{answer}");
    client3.force_stop();
    Ok(())
}
