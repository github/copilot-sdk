use github_copilot_sdk::generated::api_types::{
    CommandsHandlePendingCommandRequest, HandlePendingToolCallRequest, PermissionDecision,
    PermissionDecisionApproveForLocation, PermissionDecisionApproveForLocationApproval,
    PermissionDecisionApproveForLocationApprovalCustomTool,
    PermissionDecisionApproveForLocationApprovalCustomToolKind,
    PermissionDecisionApproveForLocationKind, PermissionDecisionApproveForSession,
    PermissionDecisionApproveForSessionApproval,
    PermissionDecisionApproveForSessionApprovalCustomTool,
    PermissionDecisionApproveForSessionApprovalCustomToolKind,
    PermissionDecisionApproveForSessionKind, PermissionDecisionApproveOnce,
    PermissionDecisionApproveOnceKind, PermissionDecisionApprovePermanently,
    PermissionDecisionApprovePermanentlyKind, PermissionDecisionReject,
    PermissionDecisionRejectKind, PermissionDecisionRequest, TasksCancelRequest,
    TasksPromoteToBackgroundRequest, TasksRemoveRequest, TasksStartAgentRequest,
    UIElicitationResponse, UIElicitationResponseAction, UIHandlePendingElicitationRequest,
};

use super::support::with_e2e_context;

#[tokio::test]
async fn should_list_task_state_and_return_false_for_missing_task_operations() {
    with_e2e_context(
        "rpc_tasks_and_handlers",
        "should_list_task_state_and_return_false_for_missing_task_operations",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let tasks = session.rpc().tasks().list().await.expect("list tasks");
                assert!(tasks.tasks.is_empty());
                assert!(
                    !session
                        .rpc()
                        .tasks()
                        .promote_to_background(TasksPromoteToBackgroundRequest {
                            id: "missing-task".to_string(),
                        })
                        .await
                        .expect("promote missing")
                        .promoted
                );
                assert!(
                    !session
                        .rpc()
                        .tasks()
                        .cancel(TasksCancelRequest {
                            id: "missing-task".to_string(),
                        })
                        .await
                        .expect("cancel missing")
                        .cancelled
                );
                assert!(
                    !session
                        .rpc()
                        .tasks()
                        .remove(TasksRemoveRequest {
                            id: "missing-task".to_string(),
                        })
                        .await
                        .expect("remove missing")
                        .removed
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_report_implemented_error_for_missing_task_agent_type() {
    with_e2e_context(
        "rpc_tasks_and_handlers",
        "should_report_implemented_error_for_missing_task_agent_type",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                assert_implemented_error(
                    session
                        .rpc()
                        .tasks()
                        .start_agent(TasksStartAgentRequest {
                            agent_type: "missing-agent-type".to_string(),
                            prompt: "Say hi".to_string(),
                            name: "sdk-test-task".to_string(),
                            description: None,
                            model: None,
                        })
                        .await,
                    "session.tasks.startAgent",
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_report_implemented_error_for_invalid_task_agent_model() {
    with_e2e_context(
        "rpc_tasks_and_handlers",
        "should_report_implemented_error_for_invalid_task_agent_model",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                assert_implemented_error(
                    session
                        .rpc()
                        .tasks()
                        .start_agent(TasksStartAgentRequest {
                            agent_type: "general-purpose".to_string(),
                            prompt: "Say hi".to_string(),
                            name: "sdk-test-task".to_string(),
                            description: Some("SDK task agent validation".to_string()),
                            model: Some("not-a-real-model".to_string()),
                        })
                        .await,
                    "session.tasks.startAgent",
                );
                assert!(
                    session
                        .rpc()
                        .tasks()
                        .list()
                        .await
                        .expect("list tasks after invalid start")
                        .tasks
                        .is_empty()
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_return_expected_results_for_missing_pending_handler_requestids() {
    with_e2e_context(
        "rpc_tasks_and_handlers",
        "should_return_expected_results_for_missing_pending_handler_requestids",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let tool = session
                    .rpc()
                    .tools()
                    .handle_pending_tool_call(HandlePendingToolCallRequest {
                        request_id: "missing-tool-request".into(),
                        result: Some(serde_json::json!("tool result")),
                        error: None,
                    })
                    .await
                    .expect("handle missing tool");
                assert!(!tool.success);

                let command = session
                    .rpc()
                    .commands()
                    .handle_pending_command(CommandsHandlePendingCommandRequest {
                        request_id: "missing-command-request".into(),
                        error: Some("command error".to_string()),
                    })
                    .await
                    .expect("handle missing command");
                assert!(command.success);

                let elicitation = session
                    .rpc()
                    .ui()
                    .handle_pending_elicitation(UIHandlePendingElicitationRequest {
                        request_id: "missing-elicitation-request".into(),
                        result: UIElicitationResponse {
                            action: UIElicitationResponseAction::Cancel,
                            content: Default::default(),
                        },
                    })
                    .await
                    .expect("handle missing elicitation");
                assert!(!elicitation.success);

                for (request_id, result) in [
                    (
                        "missing-permission-request",
                        PermissionDecision::Reject(PermissionDecisionReject {
                            feedback: Some("not approved".to_string()),
                            kind: PermissionDecisionRejectKind::Reject,
                        }),
                    ),
                    (
                        "missing-approve-once-request",
                        PermissionDecision::ApproveOnce(PermissionDecisionApproveOnce {
                            kind: PermissionDecisionApproveOnceKind::ApproveOnce,
                        }),
                    ),
                    (
                        "missing-permanent-permission-request",
                        PermissionDecision::ApprovePermanently(
                            PermissionDecisionApprovePermanently {
                                domain: "example.com".to_string(),
                                kind: PermissionDecisionApprovePermanentlyKind::ApprovePermanently,
                            },
                        ),
                    ),
                    (
                        "missing-session-approval-request",
                        PermissionDecision::ApproveForSession(PermissionDecisionApproveForSession {
                            approval: Some(PermissionDecisionApproveForSessionApproval::CustomTool(
                                PermissionDecisionApproveForSessionApprovalCustomTool {
                                    kind: PermissionDecisionApproveForSessionApprovalCustomToolKind::CustomTool,
                                    tool_name: "missing-tool".to_string(),
                                },
                            )),
                            domain: None,
                            kind: PermissionDecisionApproveForSessionKind::ApproveForSession,
                        }),
                    ),
                    (
                        "missing-location-approval-request",
                        PermissionDecision::ApproveForLocation(PermissionDecisionApproveForLocation {
                            approval: PermissionDecisionApproveForLocationApproval::CustomTool(
                                PermissionDecisionApproveForLocationApprovalCustomTool {
                                    kind: PermissionDecisionApproveForLocationApprovalCustomToolKind::CustomTool,
                                    tool_name: "missing-tool".to_string(),
                                },
                            ),
                            kind: PermissionDecisionApproveForLocationKind::ApproveForLocation,
                            location_key: "missing-location".to_string(),
                        }),
                    ),
                ] {
                    let permission = session
                        .rpc()
                        .permissions()
                        .handle_pending_permission_request(PermissionDecisionRequest {
                            request_id: request_id.into(),
                            result,
                        })
                        .await
                        .expect("handle missing permission");
                    assert!(!permission.success, "{request_id} should not be handled");
                }

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

fn assert_implemented_error<T>(result: Result<T, github_copilot_sdk::Error>, method: &str) {
    let err = match result {
        Ok(_) => panic!("RPC should fail"),
        Err(err) => err,
    };
    let message = err.to_string();
    assert!(
        !message.contains(&format!("Unhandled method {method}")),
        "expected implemented error for {method}, got {message}"
    );
}
