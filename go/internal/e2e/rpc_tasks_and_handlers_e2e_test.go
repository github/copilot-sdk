package e2e

import (
	"strings"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

// Mirrors dotnet/test/RpcTasksAndHandlersTests.cs (snapshot category "rpc_tasks_and_handlers").
func TestRPCTasksAndHandlersE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should list task state and return false for missing task operations", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		tasks, err := session.RPC.Tasks.List(t.Context())
		if err != nil {
			t.Fatalf("Tasks.List failed: %v", err)
		}
		if tasks.Tasks == nil {
			t.Error("Expected non-nil Tasks list")
		}
		if len(tasks.Tasks) != 0 {
			t.Errorf("Expected empty Tasks list, got %d tasks", len(tasks.Tasks))
		}

		if _, err := session.RPC.Tasks.Refresh(t.Context()); err != nil {
			t.Fatalf("Tasks.Refresh failed: %v", err)
		}
		if _, err := session.RPC.Tasks.WaitForPending(t.Context()); err != nil {
			t.Fatalf("Tasks.WaitForPending failed: %v", err)
		}

		progress, err := session.RPC.Tasks.GetProgress(t.Context(), &rpc.TasksGetProgressRequest{ID: "missing-task"})
		if err != nil {
			t.Fatalf("Tasks.GetProgress failed: %v", err)
		}
		if progress.Progress != nil {
			t.Errorf("Expected nil Progress for missing task, got %+v", progress.Progress)
		}

		current, err := session.RPC.Tasks.GetCurrentPromotable(t.Context())
		if err != nil {
			t.Fatalf("Tasks.GetCurrentPromotable failed: %v", err)
		}
		if current.Task != nil {
			t.Errorf("Expected nil current promotable task, got %+v", current.Task)
		}

		promote, err := session.RPC.Tasks.PromoteToBackground(t.Context(), &rpc.TasksPromoteToBackgroundRequest{ID: "missing-task"})
		if err != nil {
			t.Fatalf("PromoteToBackground failed: %v", err)
		}
		if promote.Promoted {
			t.Error("Expected Promoted=false for missing task")
		}

		promoteCurrent, err := session.RPC.Tasks.PromoteCurrentToBackground(t.Context())
		if err != nil {
			t.Fatalf("Tasks.PromoteCurrentToBackground failed: %v", err)
		}
		if promoteCurrent.Task != nil {
			t.Errorf("Expected nil task from PromoteCurrentToBackground, got %+v", promoteCurrent.Task)
		}

		cancel, err := session.RPC.Tasks.Cancel(t.Context(), &rpc.TasksCancelRequest{ID: "missing-task"})
		if err != nil {
			t.Fatalf("Cancel failed: %v", err)
		}
		if cancel.Cancelled {
			t.Error("Expected Cancelled=false for missing task")
		}

		remove, err := session.RPC.Tasks.Remove(t.Context(), &rpc.TasksRemoveRequest{ID: "missing-task"})
		if err != nil {
			t.Fatalf("Remove failed: %v", err)
		}
		if remove.Removed {
			t.Error("Expected Removed=false for missing task")
		}

		sendMessage, err := session.RPC.Tasks.SendMessage(t.Context(), &rpc.TasksSendMessageRequest{
			ID:      "missing-task",
			Message: "hello from the Go SDK E2E test",
		})
		if err != nil {
			t.Fatalf("Tasks.SendMessage failed: %v", err)
		}
		if sendMessage.Sent {
			t.Error("Expected Sent=false for missing task")
		}
		if sendMessage.Error == nil || strings.TrimSpace(*sendMessage.Error) == "" {
			t.Errorf("Expected missing task SendMessage to return an error message, got %+v", sendMessage)
		}
	})

	t.Run("should report implemented error for missing task agent type", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		_, err = session.RPC.Tasks.StartAgent(t.Context(), &rpc.TasksStartAgentRequest{
			AgentType: "missing-agent-type",
			Prompt:    "Say hi",
			Name:      "sdk-test-task",
		})
		if err == nil {
			t.Fatal("Expected an error for missing agent type")
		}
		if strings.Contains(strings.ToLower(err.Error()), "unhandled method session.tasks.startagent") {
			t.Errorf("Expected an implemented error, but the method appears unhandled: %v", err)
		}
	})

	t.Run("should report implemented error for invalid task agent model", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		description := "SDK task agent validation"
		model := "not-a-real-model"
		_, err = session.RPC.Tasks.StartAgent(t.Context(), &rpc.TasksStartAgentRequest{
			AgentType:   "general-purpose",
			Prompt:      "Say hi",
			Name:        "sdk-test-task",
			Description: &description,
			Model:       &model,
		})
		if err == nil {
			t.Fatal("Expected an error for invalid agent model")
		}
		if strings.Contains(strings.ToLower(err.Error()), "unhandled method session.tasks.startagent") {
			t.Errorf("Expected an implemented error, but the method appears unhandled: %v", err)
		}

		tasks, err := session.RPC.Tasks.List(t.Context())
		if err != nil {
			t.Fatalf("Tasks.List failed: %v", err)
		}
		if len(tasks.Tasks) != 0 {
			t.Fatalf("Expected no task to be created for invalid model, got %+v", tasks.Tasks)
		}
	})

	t.Run("should return expected results for missing pending handler request ids", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		tool, err := session.RPC.Tools.HandlePendingToolCall(t.Context(), &rpc.HandlePendingToolCallRequest{
			RequestID: "missing-tool-request",
			Result:    rpc.ExternalToolStringResult("tool result"),
		})
		if err != nil {
			t.Fatalf("Tools.HandlePendingToolCall failed: %v", err)
		}
		if tool.Success {
			t.Error("Expected Success=false for missing tool request id")
		}

		commandErr := "command error"
		command, err := session.RPC.Commands.HandlePendingCommand(t.Context(), &rpc.CommandsHandlePendingCommandRequest{
			RequestID: "missing-command-request",
			Error:     &commandErr,
		})
		if err != nil {
			t.Fatalf("Commands.HandlePendingCommand failed: %v", err)
		}
		// Per dotnet RpcTasksAndHandlersTests, missing command requests return Success=true.
		if !command.Success {
			t.Error("Expected Success=true for missing command request id")
		}

		elicitation, err := session.RPC.UI.HandlePendingElicitation(t.Context(), &rpc.UIHandlePendingElicitationRequest{
			RequestID: "missing-elicitation-request",
			Result:    rpc.UIElicitationResponse{Action: rpc.UIElicitationResponseActionCancel},
		})
		if err != nil {
			t.Fatalf("UI.HandlePendingElicitation failed: %v", err)
		}
		if elicitation.Success {
			t.Error("Expected Success=false for missing elicitation request id")
		}

		userInput, err := session.RPC.UI.HandlePendingUserInput(t.Context(), &rpc.UIHandlePendingUserInputRequest{
			RequestID: "missing-user-input-request",
			Response:  rpc.UIUserInputResponse{Answer: "typed answer", WasFreeform: true},
		})
		if err != nil {
			t.Fatalf("UI.HandlePendingUserInput failed: %v", err)
		}
		if userInput.Success {
			t.Error("Expected Success=false for missing user input request id")
		}

		sampling, err := session.RPC.UI.HandlePendingSampling(t.Context(), &rpc.UIHandlePendingSamplingRequest{
			RequestID: "missing-sampling-request",
			Response:  &rpc.UIHandlePendingSamplingResponse{},
		})
		if err != nil {
			t.Fatalf("UI.HandlePendingSampling failed: %v", err)
		}
		if sampling.Success {
			t.Error("Expected Success=false for missing sampling request id")
		}

		autoModeSwitch, err := session.RPC.UI.HandlePendingAutoModeSwitch(t.Context(), &rpc.UIHandlePendingAutoModeSwitchRequest{
			RequestID: "missing-auto-mode-switch-request",
			Response:  rpc.UIAutoModeSwitchResponseNo,
		})
		if err != nil {
			t.Fatalf("UI.HandlePendingAutoModeSwitch failed: %v", err)
		}
		if autoModeSwitch.Success {
			t.Error("Expected Success=false for missing auto mode switch request id")
		}

		feedback := "No pending plan approval"
		selectedAction := rpc.UIExitPlanModeActionExitOnly
		exitPlanMode, err := session.RPC.UI.HandlePendingExitPlanMode(t.Context(), &rpc.UIHandlePendingExitPlanModeRequest{
			RequestID: "missing-exit-plan-mode-request",
			Response: rpc.UIExitPlanModeResponse{
				Approved:       false,
				Feedback:       &feedback,
				SelectedAction: &selectedAction,
			},
		})
		if err != nil {
			t.Fatalf("UI.HandlePendingExitPlanMode failed: %v", err)
		}
		if exitPlanMode.Success {
			t.Error("Expected Success=false for missing exit plan mode request id")
		}

		permissionFeedback := "not approved"
		permission, err := session.RPC.Permissions.HandlePendingPermissionRequest(t.Context(), &rpc.PermissionDecisionRequest{
			RequestID: "missing-permission-request",
			Result:    &rpc.PermissionDecisionReject{Feedback: &permissionFeedback},
		})
		if err != nil {
			t.Fatalf("Permissions.HandlePendingPermissionRequest (reject) failed: %v", err)
		}
		if permission.Success {
			t.Error("Expected Success=false for missing permission request id")
		}

		domain := "example.com"
		permanent, err := session.RPC.Permissions.HandlePendingPermissionRequest(t.Context(), &rpc.PermissionDecisionRequest{
			RequestID: "missing-permanent-permission-request",
			Result:    &rpc.PermissionDecisionApprovePermanently{Domain: domain},
		})
		if err != nil {
			t.Fatalf("Permissions.HandlePendingPermissionRequest (approve-permanently) failed: %v", err)
		}
		if permanent.Success {
			t.Error("Expected Success=false for missing permanent permission request id")
		}

		sessionApproval, err := session.RPC.Permissions.HandlePendingPermissionRequest(t.Context(), &rpc.PermissionDecisionRequest{
			RequestID: "missing-session-approval-request",
			Result: &rpc.PermissionDecisionApproveForSession{
				Approval: &rpc.PermissionDecisionApproveForSessionApprovalCustomTool{ToolName: "missing-tool"},
			},
		})
		if err != nil {
			t.Fatalf("Permissions.HandlePendingPermissionRequest (approve-for-session) failed: %v", err)
		}
		if sessionApproval.Success {
			t.Error("Expected Success=false for missing session approval request id")
		}

		locationApproval, err := session.RPC.Permissions.HandlePendingPermissionRequest(t.Context(), &rpc.PermissionDecisionRequest{
			RequestID: "missing-location-approval-request",
			Result: &rpc.PermissionDecisionApproveForLocation{
				Approval:    &rpc.PermissionDecisionApproveForLocationApprovalCustomTool{ToolName: "missing-tool"},
				LocationKey: "missing-location",
			},
		})
		if err != nil {
			t.Fatalf("Permissions.HandlePendingPermissionRequest (approve-for-location) failed: %v", err)
		}
		if locationApproval.Success {
			t.Error("Expected Success=false for missing location approval request id")
		}
	})

	t.Run("should round trip rpc elicitation through config handler", func(t *testing.T) {
		handlerContext := make(chan copilot.ElicitationContext, 1)
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			OnElicitationRequest: func(ctx copilot.ElicitationContext) (copilot.ElicitationResult, error) {
				handlerContext <- ctx
				return copilot.ElicitationResult{
					Action: copilot.ElicitationActionAccept,
					Content: map[string]any{
						"answer":    "from handler",
						"confirmed": true,
					},
				}, nil
			},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		response, err := session.RPC.UI.Elicitation(t.Context(), &rpc.UIElicitationRequest{
			Message: "Need details",
			RequestedSchema: rpc.UIElicitationSchema{
				Type: rpc.UIElicitationSchemaTypeObject,
				Properties: map[string]rpc.UIElicitationSchemaProperty{
					"answer":    &rpc.UIElicitationSchemaPropertyString{},
					"confirmed": &rpc.UIElicitationSchemaPropertyBoolean{},
				},
				Required: []string{"answer"},
			},
		})
		if err != nil {
			t.Fatalf("UI.Elicitation failed: %v", err)
		}

		var ctx copilot.ElicitationContext
		select {
		case ctx = <-handlerContext:
		case <-time.After(30 * time.Second):
			t.Fatal("Timed out waiting for elicitation handler")
		}
		if ctx.SessionID != session.SessionID || ctx.Message != "Need details" {
			t.Fatalf("Unexpected elicitation context: %+v", ctx)
		}
		if ctx.RequestedSchema == nil || ctx.RequestedSchema.Properties == nil {
			t.Fatalf("Expected requested schema to include properties, got %+v", ctx.RequestedSchema)
		}
		if response.Action != rpc.UIElicitationResponseActionAccept {
			t.Fatalf("Expected accept response, got %+v", response)
		}
		if got, ok := response.Content["answer"].(rpc.UIElicitationStringValue); !ok || string(got) != "from handler" {
			t.Fatalf("Expected answer content from handler, got %+v", response.Content["answer"])
		}
		if got, ok := response.Content["confirmed"].(rpc.UIElicitationBooleanValue); !ok || !bool(got) {
			t.Fatalf("Expected confirmed content true, got %+v", response.Content["confirmed"])
		}
	})

	t.Run("should register and unregister direct auto mode switch handler", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		missing, err := session.RPC.UI.UnregisterDirectAutoModeSwitchHandler(t.Context(), &rpc.UIUnregisterDirectAutoModeSwitchHandlerRequest{
			Handle: "missing-direct-auto-mode-handle",
		})
		if err != nil {
			t.Fatalf("UI.UnregisterDirectAutoModeSwitchHandler(missing) failed: %v", err)
		}
		if missing.Unregistered {
			t.Fatal("Expected missing direct handler unregister to return false")
		}

		registration, err := session.RPC.UI.RegisterDirectAutoModeSwitchHandler(t.Context())
		if err != nil {
			t.Fatalf("UI.RegisterDirectAutoModeSwitchHandler failed: %v", err)
		}
		if strings.TrimSpace(registration.Handle) == "" {
			t.Fatal("Expected non-empty direct auto mode switch handler handle")
		}

		unregister, err := session.RPC.UI.UnregisterDirectAutoModeSwitchHandler(t.Context(), &rpc.UIUnregisterDirectAutoModeSwitchHandlerRequest{
			Handle: registration.Handle,
		})
		if err != nil {
			t.Fatalf("UI.UnregisterDirectAutoModeSwitchHandler failed: %v", err)
		}
		if !unregister.Unregistered {
			t.Fatal("Expected registered direct handler to unregister")
		}

		unregisterAgain, err := session.RPC.UI.UnregisterDirectAutoModeSwitchHandler(t.Context(), &rpc.UIUnregisterDirectAutoModeSwitchHandlerRequest{
			Handle: registration.Handle,
		})
		if err != nil {
			t.Fatalf("UI.UnregisterDirectAutoModeSwitchHandler second call failed: %v", err)
		}
		if unregisterAgain.Unregistered {
			t.Fatal("Expected second direct handler unregister to return false")
		}
	})
}
