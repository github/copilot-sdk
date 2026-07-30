package copilot_test

import (
	"encoding/json"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/rpc"
)

func TestPermissionEventExposesManagedApprovalRequired(t *testing.T) {
	var data copilot.PermissionRequestedData
	err := json.Unmarshal([]byte(`{
		"permissionRequest": {
			"kind": "read",
			"intention": "Read managed content",
			"path": "/workspace/file.txt",
			"managedApprovalRequired": true
		},
		"requestId": "permission-1"
	}`), &data)
	if err != nil {
		t.Fatal(err)
	}

	if !data.PermissionRequest.RequiresManagedApproval() {
		t.Fatal("expected managed approval to be required")
	}
}

func TestApproveAllReturnsErrorWhenManagedSettingsEnabled(t *testing.T) {
	decision, err := copilot.PermissionHandler.ApproveAll(
		&copilot.PermissionRequestRead{},
		copilot.PermissionInvocation{SessionID: "session-1", ManagedSettingsEnabled: true},
	)
	if err == nil {
		t.Fatal("expected an error")
	}
	if decision != nil {
		t.Fatalf("expected no decision, got %T", decision)
	}
}

func TestApproveAllApprovesOrdinaryRequest(t *testing.T) {
	decision, err := copilot.PermissionHandler.ApproveAll(
		&copilot.PermissionRequestRead{},
		copilot.PermissionInvocation{SessionID: "session-1"},
	)
	if err != nil {
		t.Fatal(err)
	}
	if _, ok := decision.(*rpc.PermissionDecisionApproveOnce); !ok {
		t.Fatalf("expected PermissionDecisionApproveOnce, got %T", decision)
	}
}
