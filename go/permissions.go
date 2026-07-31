package copilot

import (
	"github.com/github/copilot-sdk/go/rpc"
)

// PermissionHandler provides pre-built OnPermissionRequest implementations.
var PermissionHandler = struct {
	// ApproveAll approves permission requests unless managed approval is required.
	ApproveAll PermissionHandlerFunc
}{
	ApproveAll: func(request PermissionRequest, invocation PermissionInvocation) (rpc.PermissionDecision, error) {
		if request.RequiresManagedApproval() {
			return &rpc.PermissionDecisionNoResult{}, nil
		}
		return &rpc.PermissionDecisionApproveOnce{}, nil
	},
}
