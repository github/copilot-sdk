package copilot

// PermissionHandler provides pre-built OnPermissionRequest implementations.
var PermissionHandler = struct {
	// ApproveAll approves all permission requests.
	ApproveAll func(PermissionRequest, PermissionInvocation) (PermissionRequestResult, error)
}{
	ApproveAll: func(_ PermissionRequest, _ PermissionInvocation) (PermissionRequestResult, error) {
		return PermissionRequestResult{Kind: "approved"}, nil
	},
}
