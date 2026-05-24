package copilot

import (
	"context"
	"encoding/json"
	"errors"
	"testing"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
)

func TestCanvasDeclaration_JSONShape(t *testing.T) {
	desc := "bump"
	decl := CanvasDeclaration{
		ID:          "counter",
		DisplayName: "Counter",
		Description: "Count things",
		Actions: []rpc.CanvasAction{
			{Name: "increment", Description: &desc},
		},
	}

	data, err := json.Marshal(decl)
	if err != nil {
		t.Fatalf("marshal failed: %v", err)
	}

	var decoded map[string]any
	if err := json.Unmarshal(data, &decoded); err != nil {
		t.Fatalf("unmarshal failed: %v", err)
	}

	if decoded["id"] != "counter" {
		t.Fatalf("expected id=counter, got %v", decoded["id"])
	}
	if decoded["displayName"] != "Counter" {
		t.Fatalf("expected displayName=Counter, got %v", decoded["displayName"])
	}
	if decoded["description"] != "Count things" {
		t.Fatalf("expected description, got %v", decoded["description"])
	}
	if _, present := decoded["inputSchema"]; present {
		t.Fatalf("inputSchema should be omitted when nil, got %v", decoded["inputSchema"])
	}
	actions, ok := decoded["actions"].([]any)
	if !ok || len(actions) != 1 {
		t.Fatalf("expected actions array of length 1, got %v", decoded["actions"])
	}
	first, _ := actions[0].(map[string]any)
	if first["name"] != "increment" {
		t.Fatalf("expected first action name=increment, got %v", first["name"])
	}
}

func TestCanvasDeclaration_OmitsEmptyActions(t *testing.T) {
	decl := CanvasDeclaration{ID: "x", DisplayName: "X", Description: "y"}
	data, err := json.Marshal(decl)
	if err != nil {
		t.Fatalf("marshal failed: %v", err)
	}
	var decoded map[string]any
	_ = json.Unmarshal(data, &decoded)
	if _, present := decoded["actions"]; present {
		t.Fatalf("actions should be omitted when nil, got %v", decoded["actions"])
	}
}

func TestCanvasHandlerDefaults_OnAction_ReturnsNoHandler(t *testing.T) {
	d := CanvasHandlerDefaults{}
	_, err := d.OnAction(context.Background(), CanvasActionContext{})
	if err == nil {
		t.Fatalf("expected error from default OnAction")
	}
	cerr, ok := err.(*CanvasError)
	if !ok {
		t.Fatalf("expected *CanvasError, got %T", err)
	}
	if cerr.Code != "canvas_action_no_handler" {
		t.Fatalf("expected code=canvas_action_no_handler, got %q", cerr.Code)
	}
}

func TestCanvasHandlerDefaults_OnClose_ReturnsNil(t *testing.T) {
	d := CanvasHandlerDefaults{}
	if err := d.OnClose(context.Background(), CanvasLifecycleContext{}); err != nil {
		t.Fatalf("expected nil from default OnClose, got %v", err)
	}
}

func TestCanvasError_ErrorString(t *testing.T) {
	e := NewCanvasError("foo_code", "bar message")
	if got := e.Error(); got != "foo_code: bar message" {
		t.Fatalf("unexpected Error() output: %q", got)
	}
}

// recordingCanvasHandler captures calls for assertion.
type recordingCanvasHandler struct {
	CanvasHandlerDefaults
	openCtx    *CanvasOpenContext
	openResult CanvasOpenResponse
	openErr    error
}

func (h *recordingCanvasHandler) OnOpen(ctx context.Context, c CanvasOpenContext) (CanvasOpenResponse, error) {
	h.openCtx = &c
	return h.openResult, h.openErr
}

func TestClient_HandleCanvasOpen_DispatchesToHandler(t *testing.T) {
	title := "Echo"
	url := "https://example.test/echo"
	handler := &recordingCanvasHandler{
		openResult: CanvasOpenResponse{URL: &url, Title: &title},
	}

	session := &Session{SessionID: "s1"}
	session.registerCanvasHandler(handler)

	c := &Client{sessions: map[string]*Session{"s1": session}}

	params := canvasProviderRequestParams{
		SessionID:   "s1",
		ExtensionID: "project:echo",
		CanvasID:    "echo",
		InstanceID:  "echo-1",
		Input:       map[string]any{"x": float64(1)},
	}
	resp, rpcErr := c.handleCanvasOpen(params)
	if rpcErr != nil {
		t.Fatalf("unexpected rpc error: %+v", rpcErr)
	}
	if handler.openCtx == nil {
		t.Fatalf("handler.OnOpen was not called")
	}
	if handler.openCtx.CanvasID != "echo" || handler.openCtx.InstanceID != "echo-1" {
		t.Fatalf("unexpected ctx: %+v", handler.openCtx)
	}
	if resp.URL == nil || *resp.URL != url {
		t.Fatalf("response URL not propagated: %+v", resp)
	}
}

func TestClient_HandleCanvasOpen_NoHandler_ReturnsUnsetError(t *testing.T) {
	session := &Session{SessionID: "s1"}
	c := &Client{sessions: map[string]*Session{"s1": session}}

	_, rpcErr := c.handleCanvasOpen(canvasProviderRequestParams{SessionID: "s1"})
	if rpcErr == nil {
		t.Fatalf("expected error when no canvas handler installed")
	}
	if rpcErr.Code != -32603 {
		t.Fatalf("expected internal-error code, got %d", rpcErr.Code)
	}
	var data map[string]string
	if err := json.Unmarshal(rpcErr.Data, &data); err != nil {
		t.Fatalf("invalid error data: %v", err)
	}
	if data["code"] != "canvas_handler_unset" {
		t.Fatalf("expected code=canvas_handler_unset, got %q", data["code"])
	}
}

func TestClient_HandleCanvasOpen_HandlerCanvasError_Wired(t *testing.T) {
	handler := &recordingCanvasHandler{
		openErr: NewCanvasError("permission_denied", "nope"),
	}
	session := &Session{SessionID: "s1"}
	session.registerCanvasHandler(handler)
	c := &Client{sessions: map[string]*Session{"s1": session}}

	_, rpcErr := c.handleCanvasOpen(canvasProviderRequestParams{SessionID: "s1"})
	if rpcErr == nil {
		t.Fatalf("expected error")
	}
	var data map[string]string
	_ = json.Unmarshal(rpcErr.Data, &data)
	if data["code"] != "permission_denied" {
		t.Fatalf("expected propagated code, got %q", data["code"])
	}
}

func TestClient_HandleCanvasOpen_HandlerGenericError_WrappedAsCanvasHandlerError(t *testing.T) {
	handler := &recordingCanvasHandler{openErr: errors.New("boom")}
	session := &Session{SessionID: "s1"}
	session.registerCanvasHandler(handler)
	c := &Client{sessions: map[string]*Session{"s1": session}}

	_, rpcErr := c.handleCanvasOpen(canvasProviderRequestParams{SessionID: "s1"})
	if rpcErr == nil {
		t.Fatalf("expected error")
	}
	var data map[string]string
	_ = json.Unmarshal(rpcErr.Data, &data)
	if data["code"] != "canvas_handler_error" {
		t.Fatalf("expected code=canvas_handler_error, got %q", data["code"])
	}
	if data["message"] != "boom" {
		t.Fatalf("expected message=boom, got %q", data["message"])
	}
}

// Ensure the JSON-RPC inbound parsing wires through RequestHandlerFor correctly.
func TestClient_HandleCanvasOpen_RawJSONRoundTrip(t *testing.T) {
	handler := &recordingCanvasHandler{
		openResult: CanvasOpenResponse{Status: strPtr("ready")},
	}
	session := &Session{SessionID: "s1"}
	session.registerCanvasHandler(handler)
	c := &Client{sessions: map[string]*Session{"s1": session}}

	rpcHandler := jsonrpc2.RequestHandlerFor(c.handleCanvasOpen)
	raw := []byte(`{"sessionId":"s1","extensionId":"ext","canvasId":"echo","instanceId":"i1","input":{"k":"v"},"host":{"capabilities":{"canvases":true}}}`)
	out, rpcErr := rpcHandler(raw)
	if rpcErr != nil {
		t.Fatalf("unexpected rpc error: %v", rpcErr)
	}
	if handler.openCtx == nil {
		t.Fatalf("handler not invoked")
	}
	if handler.openCtx.Host == nil || !handler.openCtx.Host.Capabilities.Canvases {
		t.Fatalf("host capabilities not parsed: %+v", handler.openCtx.Host)
	}
	var decoded map[string]any
	if err := json.Unmarshal(out, &decoded); err != nil {
		t.Fatalf("bad output JSON: %v", err)
	}
	if decoded["status"] != "ready" {
		t.Fatalf("expected status=ready, got %v", decoded["status"])
	}
}

func TestResumeSessionResponse_OpenCanvasesParse(t *testing.T) {
	raw := []byte(`{
		"sessionId": "s1",
		"workspacePath": "/tmp/ws",
		"openCanvases": [
			{
				"availability": "ready",
				"canvasId": "echo",
				"extensionId": "project:echo",
				"instanceId": "echo-1",
				"reopen": false
			}
		]
	}`)

	var resp resumeSessionResponse
	if err := json.Unmarshal(raw, &resp); err != nil {
		t.Fatalf("unmarshal failed: %v", err)
	}
	if len(resp.OpenCanvases) != 1 {
		t.Fatalf("expected 1 open canvas, got %d", len(resp.OpenCanvases))
	}
	if resp.OpenCanvases[0].CanvasID != "echo" {
		t.Fatalf("unexpected canvasId: %q", resp.OpenCanvases[0].CanvasID)
	}

	session := &Session{SessionID: "s1"}
	session.setOpenCanvases(resp.OpenCanvases)
	got := session.OpenCanvases()
	if len(got) != 1 || got[0].InstanceID != "echo-1" {
		t.Fatalf("OpenCanvases did not surface snapshot: %+v", got)
	}
}

func TestResumeSessionRequest_OpenCanvasesWireShape(t *testing.T) {
	req := resumeSessionRequest{
		SessionID: "s1",
		OpenCanvases: []rpc.OpenCanvasInstance{
			{
				Availability: "ready",
				CanvasID:     "echo",
				ExtensionID:  "project:echo",
				InstanceID:   "echo-1",
				Reopen:       false,
			},
		},
	}

	data, err := json.Marshal(req)
	if err != nil {
		t.Fatalf("marshal failed: %v", err)
	}

	var decoded map[string]any
	if err := json.Unmarshal(data, &decoded); err != nil {
		t.Fatalf("unmarshal failed: %v", err)
	}

	raw, ok := decoded["openCanvases"].([]any)
	if !ok || len(raw) != 1 {
		t.Fatalf("expected openCanvases array of length 1, got %v", decoded["openCanvases"])
	}
	first, _ := raw[0].(map[string]any)
	if first["canvasId"] != "echo" {
		t.Fatalf("expected canvasId=echo, got %v", first["canvasId"])
	}
	if first["instanceId"] != "echo-1" {
		t.Fatalf("expected instanceId=echo-1, got %v", first["instanceId"])
	}

	// Omitted when nil
	empty := resumeSessionRequest{SessionID: "s1"}
	emptyData, err := json.Marshal(empty)
	if err != nil {
		t.Fatalf("marshal empty failed: %v", err)
	}
	var emptyDecoded map[string]any
	if err := json.Unmarshal(emptyData, &emptyDecoded); err != nil {
		t.Fatalf("unmarshal empty failed: %v", err)
	}
	if _, present := emptyDecoded["openCanvases"]; present {
		t.Fatalf("openCanvases should be omitted when nil")
	}
}

func strPtr(s string) *string { return &s }
