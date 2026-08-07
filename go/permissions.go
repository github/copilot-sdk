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
// through [WithDecisionContext] rather than by hand.
//
// Experimental: AttributedPermissionResult is part of an experimental API and
// may change or be removed.
type AttributedPermissionResult struct {
	rpc.PermissionDecision
	// DecisionContext describes how and where the decision was reached. When nil
	// the SDK omits it from the wire, preserving legacy behavior.
	DecisionContext *rpc.PermissionDecisionContext
}

// WithDecisionContext attaches provenance to a permission decision so the
// runtime can attribute auto-approval telemetry to the responding surface.
//
// The returned value satisfies [rpc.PermissionDecision], so a
// [PermissionHandlerFunc] can return it directly. Applying WithDecisionContext
// to an already-attributed result replaces the previous context rather than
// nesting it. If result is a [rpc.PermissionDecisionNoResult] (attributed or
// not), the SDK still suppresses the response.
//
// Experimental: WithDecisionContext is part of an experimental API and may
// change or be removed.
func WithDecisionContext(result rpc.PermissionDecision, decisionContext *rpc.PermissionDecisionContext) *AttributedPermissionResult {
	if attributed, ok := result.(*AttributedPermissionResult); ok {
		result = attributed.PermissionDecision
	}
	return &AttributedPermissionResult{
		PermissionDecision: result,
		DecisionContext:    decisionContext,
	}
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
