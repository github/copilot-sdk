package copilot

import (
	"github.com/github/copilot-sdk/go/rpc"
)

// PermissionHandler provides pre-built OnPermissionRequest implementations.
var PermissionHandler = struct {
	// ApproveAll approves ordinary permission requests. Requests that require
	// managed approval remain pending for an explicit human decision.
	ApproveAll PermissionHandlerFunc
}{
	ApproveAll: func(request PermissionRequest, _ PermissionInvocation) (rpc.PermissionDecision, error) {
		if request.RequiresManagedApproval() {
			return &rpc.PermissionDecisionNoResult{}, nil
		}
		return &rpc.PermissionDecisionApproveOnce{}, nil
	},
}
