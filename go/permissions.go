package copilot

// PermissionHandler provides pre-built OnPermissionRequest implementations.
var PermissionHandler = struct {
	// ApproveAll approves all permission requests.
	ApproveAll PermissionHandlerFunc
}{
	ApproveAll: func(_ PermissionRequest, _ PermissionInvocation) (PermissionRequestResult, error) {
		return PermissionRequestResult{Kind: "approved"}, nil
	},
}
