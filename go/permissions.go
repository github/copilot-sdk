package copilot

import (
	"github.com/github/copilot-sdk/go/rpc"
)

// PermissionHandler provides pre-built OnPermissionRequest implementations.
var PermissionHandler = struct {
	// ApproveAll approves all permission requests.
	ApproveAll PermissionHandlerFunc
}{
	ApproveAll: func(_ PermissionRequest, _ PermissionInvocation) (rpc.PermissionDecision, error) {
		return &rpc.PermissionDecisionApproveOnce{}, nil
	},
}
