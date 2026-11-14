package copilot

import (
	"errors"

	"github.com/github/copilot-sdk/go/rpc"
)

// AttributedPermissionResult pairs a permission decision with the context
// describing how it was reached, so the runtime can attribute auto-approval
// telemetry to the responding surface.
//
// The embedded [rpc.PermissionDecision] carries the actual decision, while
// DecisionContext is informational only and never changes permission behavior.
// It satisfies [rpc.PermissionDecision] itself, so a [PermissionHandlerFunc]
// can return it wherever a plain decision is expected. Prefer constructing it
// through [NewAttributedPermissionResult] rather than by hand.
//
// Experimental: AttributedPermissionResult is part of an experimental API and
// may change or be removed.
type AttributedPermissionResult struct {
	rpc.PermissionDecision
	// DecisionContext describes how and where the decision was reached. When nil
	// the SDK omits it from the wire, preserving legacy behavior.
	DecisionContext *rpc.PermissionDecisionContext
}

// NewAttributedPermissionResult pairs a permission decision with the context
// describing how it was reached, so the runtime can attribute auto-approval
// telemetry to the responding surface.
//
// The returned value satisfies [rpc.PermissionDecision], so a
// [PermissionHandlerFunc] can return it directly. Passing an already-attributed
// result replaces the previous context rather than nesting it. If result is a
// [rpc.PermissionDecisionNoResult] (attributed or not), the SDK still
// suppresses the response.
//
// Experimental: NewAttributedPermissionResult is part of an experimental API
// and may change or be removed.
func NewAttributedPermissionResult(result rpc.PermissionDecision, decisionContext *rpc.PermissionDecisionContext) *AttributedPermissionResult {
	decision, _ := splitAttribution(result)
	return &AttributedPermissionResult{
		PermissionDecision: decision,
		DecisionContext:    decisionContext,
	}
}

// splitAttribution separates an optionally attributed result into the bare
// decision and its context, returning a nil context when there is none.
//
// Both the pointer and value forms are matched: embedding an interface promotes
// its methods to the value type too, so an AttributedPermissionResult passed by
// value also satisfies [rpc.PermissionDecision] and must not slip through
// unwrapped.
func splitAttribution(result rpc.PermissionDecision) (rpc.PermissionDecision, *rpc.PermissionDecisionContext) {
	switch attributed := result.(type) {
	case *AttributedPermissionResult:
		return attributed.PermissionDecision, attributed.DecisionContext
	case AttributedPermissionResult:
		return attributed.PermissionDecision, attributed.DecisionContext
	}
	return result, nil
}

// PermissionHandler provides pre-built OnPermissionRequest implementations.
var PermissionHandler = struct {
	// ApproveAll approves permission requests when managed settings are disabled.
	ApproveAll PermissionHandlerFunc
}{
	ApproveAll: func(request PermissionRequest, invocation PermissionInvocation) (rpc.PermissionDecision, error) {
		if invocation.ManagedSettingsEnabled {
			return nil, errors.New("approveAll cannot be used when managed settings are enabled")
		}
		if request.RequiresManagedApproval() {
			return &rpc.PermissionDecisionNoResult{}, nil
		}
		return &rpc.PermissionDecisionApproveOnce{}, nil
	},
}
