package copilot

// PermissionHandler provides pre-built OnPermissionRequest implementations.
var PermissionHandler = struct {
	// ApproveAll approves all permission requests.
	ApproveAll PermissionHandlerFunc
	// NoResult leaves the pending permission request unanswered.
	NoResult PermissionHandlerFunc
}{
	ApproveAll: func(_ PermissionRequest, _ PermissionInvocation) (PermissionRequestResult, error) {
		return PermissionRequestResult{Kind: PermissionRequestResultKindApproved}, nil
	},
	NoResult: func(_ PermissionRequest, _ PermissionInvocation) (PermissionRequestResult, error) {
		return PermissionRequestResult{Kind: PermissionRequestResultKindNoResult}, nil
	},
}
