package copilot

import (
	"encoding/json"
	"testing"

	"github.com/github/copilot-sdk/go/rpc"
)

func TestProgrammaticPermissionOwnershipReachesBothHandlers(t *testing.T) {
	for _, withPrompt := range []bool{false, true} {
		for _, programID := range []any{nil, "", "program-parent"} {
			data := map[string]any{
				"requestId": "permission-1",
				"permissionRequest": map[string]any{
					"kind": "shell", "fullCommandText": "echo example", "intention": "Test command",
					"managedApprovalRequired": true,
				},
			}
			if programID != nil {
				data["programToolCallId"] = programID
			}
			wire, err := json.Marshal(map[string]any{"type": "permission.requested", "data": data})
			if err != nil {
				t.Fatal(err)
			}
			var event SessionEvent
			if err := json.Unmarshal(wire, &event); err != nil {
				t.Fatal(err)
			}
			session := &Session{SessionID: "permission-session"}
			var captured PermissionRequest
			capture := func(request PermissionRequest, invocation PermissionInvocation) (rpc.PermissionDecision, error) {
				captured = request
				if invocation.SessionID != session.SessionID {
					t.Fatalf("unexpected invocation: %#v", invocation)
				}
				if got := invocation.ProgramToolCallID; programID == nil {
					if got != nil {
						t.Fatalf("ordinary callback has program ownership: %#v", invocation)
					}
				} else if got == nil || *got != programID {
					t.Fatalf("callback lost program ownership %q: %#v", programID, invocation)
				}
				return PermissionHandler.ApproveAll(request, invocation)
			}
			if withPrompt {
				session.registerPermissionHandlerWithPrompt(func(request PermissionRequest, invocation PermissionHandlerInvocation) (rpc.PermissionDecision, error) {
					return capture(request, invocation.PermissionInvocation)
				})
			} else {
				session.registerPermissionHandler(capture)
			}
			session.handleBroadcastEvent(event, nil)
			if captured == nil || !captured.RequiresManagedApproval() {
				t.Fatalf("permission callback lost the managed request: %#v", captured)
			}
			if _, ok := captured.(*PermissionRequestShell); !ok {
				t.Fatalf("lost typed permission request: %#v", captured)
			}
		}
	}
}
