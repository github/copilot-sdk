// Canvas declarations, provider callbacks, and host-side canvas RPC types.
//
// This file mirrors rust/src/canvas.rs. The SDK does not maintain a per-canvas
// registry; multiplexing across declared canvases is the CanvasHandler
// implementor's responsibility (typically by switching on CanvasOpenContext.CanvasID).

package copilot

import (
	"context"

	"github.com/github/copilot-sdk/go/rpc"
)

// CanvasDeclaration is the declarative metadata for a single canvas, sent over
// the wire on `session.create` / `session.resume`.
type CanvasDeclaration struct {
	// ID is the canvas identifier, unique within the declaring connection.
	ID string `json:"id"`
	// DisplayName is the human-readable name shown in host UI and canvas pickers.
	DisplayName string `json:"displayName"`
	// Description is a short, single-sentence description shown to the agent in canvas catalogs.
	Description string `json:"description"`
	// InputSchema is the JSON Schema for the `input` payload accepted by `canvas.open`.
	InputSchema map[string]any `json:"inputSchema,omitempty"`
	// Actions are the agent-callable actions this canvas exposes.
	Actions []rpc.CanvasAction `json:"actions,omitempty"`
}

// CanvasOpenResponse is the response returned from CanvasHandler.OnOpen.
type CanvasOpenResponse struct {
	// URL the host should render. Optional for canvases with no visual surface.
	URL *string `json:"url,omitempty"`
	// Title is the provider-supplied title shown in host chrome.
	Title *string `json:"title,omitempty"`
	// Status is the provider-supplied status text shown in host chrome.
	Status *string `json:"status,omitempty"`
}

// CanvasHostContext carries host capability hints passed to canvas provider callbacks.
type CanvasHostContext struct {
	// Capabilities describes host feature support relevant to canvases.
	Capabilities CanvasHostCapabilities `json:"capabilities"`
}

// CanvasHostCapabilities describes host capability details passed to canvas provider callbacks.
type CanvasHostCapabilities struct {
	// Canvases indicates whether the host supports canvas rendering.
	Canvases bool `json:"canvases"`
}

// CanvasOpenContext is the context handed to CanvasHandler.OnOpen.
type CanvasOpenContext struct {
	// SessionID is the session that requested the canvas.
	SessionID string
	// ExtensionID is the owning provider identifier.
	ExtensionID string
	// CanvasID is the canvas id from the declaring CanvasDeclaration.
	CanvasID string
	// InstanceID is the stable instance id supplied by the runtime.
	InstanceID string
	// Input is the validated input payload.
	Input any
	// Host carries host capabilities supplied by the runtime.
	Host *CanvasHostContext
}

// CanvasActionContext is the context handed to CanvasHandler.OnAction.
type CanvasActionContext struct {
	// SessionID is the session that invoked the action.
	SessionID string
	// ExtensionID is the owning provider identifier.
	ExtensionID string
	// CanvasID is the canvas id targeted by the action.
	CanvasID string
	// InstanceID is the instance id targeted by the action.
	InstanceID string
	// ActionName is the action name from CanvasAction.Name.
	ActionName string
	// Input is the validated input payload.
	Input any
	// Host carries host capabilities supplied by the runtime.
	Host *CanvasHostContext
}

// CanvasLifecycleContext is the context handed to a canvas's close lifecycle hook.
type CanvasLifecycleContext struct {
	// SessionID is the session owning the canvas instance.
	SessionID string
	// ExtensionID is the owning provider identifier.
	ExtensionID string
	// CanvasID is the canvas id from the declaring CanvasDeclaration.
	CanvasID string
	// InstanceID is the instance id this lifecycle event applies to.
	InstanceID string
	// Host carries host capabilities supplied by the runtime.
	Host *CanvasHostContext
}

// CanvasError is a structured error returned from canvas handlers.
//
// Wire envelope:
//
//	{ "code": "<code>", "message": "<message>" }
type CanvasError struct {
	// Code is the machine-readable error code.
	Code string `json:"code"`
	// Message is the human-readable message.
	Message string `json:"message"`
}

// Error implements the error interface.
func (e *CanvasError) Error() string {
	return e.Code + ": " + e.Message
}

// NewCanvasError constructs a new error envelope with the given code and message.
func NewCanvasError(code, message string) *CanvasError {
	return &CanvasError{Code: code, Message: message}
}

// CanvasErrorNoHandler is the default error returned when a custom action has no handler.
func CanvasErrorNoHandler() *CanvasError {
	return NewCanvasError(
		"canvas_action_no_handler",
		"No handler implemented for this canvas action",
	)
}

// CanvasHandler is the provider-side canvas lifecycle handler.
//
// A session installs a single CanvasHandler (via SessionConfig.CanvasHandler).
// The handler receives every inbound `canvas.open` / `canvas.close` /
// `canvas.action.invoke` JSON-RPC request the runtime issues for this session
// and decides — typically by inspecting CanvasOpenContext.CanvasID — which
// application-side canvas should handle the call.
//
// The SDK does not maintain a per-canvas registry; multiplexing across declared
// canvases is the implementor's responsibility.
//
// Embed CanvasHandlerDefaults to inherit no-op defaults for OnClose and a
// "no handler" error for OnAction.
type CanvasHandler interface {
	OnOpen(ctx context.Context, c CanvasOpenContext) (CanvasOpenResponse, error)
	OnClose(ctx context.Context, c CanvasLifecycleContext) error
	OnAction(ctx context.Context, c CanvasActionContext) (any, error)
}

// CanvasHandlerDefaults supplies default OnClose / OnAction implementations
// that consumers can inherit by embedding it in their CanvasHandler.
//
// Example:
//
//	type myHandler struct {
//	    copilot.CanvasHandlerDefaults
//	}
//	func (h *myHandler) OnOpen(ctx context.Context, c copilot.CanvasOpenContext) (copilot.CanvasOpenResponse, error) { ... }
type CanvasHandlerDefaults struct{}

// OnClose returns nil by default.
func (CanvasHandlerDefaults) OnClose(ctx context.Context, c CanvasLifecycleContext) error {
	return nil
}

// OnAction returns CanvasErrorNoHandler() by default.
func (CanvasHandlerDefaults) OnAction(ctx context.Context, c CanvasActionContext) (any, error) {
	return nil, CanvasErrorNoHandler()
}

// canvasProviderRequestParams is the wire shape of the common fields sent by
// direct `canvas.*` provider callbacks (canvas.open / canvas.close).
type canvasProviderRequestParams struct {
	SessionID   string             `json:"sessionId"`
	ExtensionID string             `json:"extensionId"`
	CanvasID    string             `json:"canvasId"`
	InstanceID  string             `json:"instanceId"`
	Input       any                `json:"input,omitempty"`
	Host        *CanvasHostContext `json:"host,omitempty"`
}

func (p *canvasProviderRequestParams) toOpenContext() CanvasOpenContext {
	return CanvasOpenContext{
		SessionID:   p.SessionID,
		ExtensionID: p.ExtensionID,
		CanvasID:    p.CanvasID,
		InstanceID:  p.InstanceID,
		Input:       p.Input,
		Host:        p.Host,
	}
}

func (p *canvasProviderRequestParams) toLifecycleContext() CanvasLifecycleContext {
	return CanvasLifecycleContext{
		SessionID:   p.SessionID,
		ExtensionID: p.ExtensionID,
		CanvasID:    p.CanvasID,
		InstanceID:  p.InstanceID,
		Host:        p.Host,
	}
}

// canvasInvokeParams is the wire shape for `canvas.action.invoke`.
type canvasInvokeParams struct {
	SessionID   string             `json:"sessionId"`
	ExtensionID string             `json:"extensionId"`
	CanvasID    string             `json:"canvasId"`
	InstanceID  string             `json:"instanceId"`
	ActionName  string             `json:"actionName"`
	Input       any                `json:"input,omitempty"`
	Host        *CanvasHostContext `json:"host,omitempty"`
}

func (p *canvasInvokeParams) toActionContext() CanvasActionContext {
	return CanvasActionContext{
		SessionID:   p.SessionID,
		ExtensionID: p.ExtensionID,
		CanvasID:    p.CanvasID,
		InstanceID:  p.InstanceID,
		ActionName:  p.ActionName,
		Input:       p.Input,
		Host:        p.Host,
	}
}

// ExtensionInfo carries stable extension identity for session participants
// that provide canvases.
type ExtensionInfo struct {
	// Source is the extension namespace/source, e.g. "github-app".
	Source string `json:"source"`
	// Name is the stable provider name within the source namespace.
	Name string `json:"name"`
}
