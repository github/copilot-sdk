package e2e

import (
	"strings"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

// Mirrors dotnet/test/RpcTasksAndHandlersTests.cs (snapshot category "rpc_tasks_and_handlers").
func TestRpcTasksAndHandlersE2E(t *testing.T) {
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

		promote, err := session.RPC.Tasks.PromoteToBackground(t.Context(), &rpc.TasksPromoteToBackgroundRequest{ID: "missing-task"})
		if err != nil {
			t.Fatalf("PromoteToBackground failed: %v", err)
		}
		if promote.Promoted {
			t.Error("Expected Promoted=false for missing task")
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

		feedback := "not approved"
		permission, err := session.RPC.Permissions.HandlePendingPermissionRequest(t.Context(), &rpc.PermissionDecisionRequest{
			RequestID: "missing-permission-request",
			Result:    &rpc.PermissionDecisionReject{Feedback: &feedback},
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
	})
}
