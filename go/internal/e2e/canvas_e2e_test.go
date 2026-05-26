package e2e

import (
	"context"
	"encoding/json"
	"fmt"
	"reflect"
	"sync"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
)

func TestCanvasE2E(t *testing.T) {
	t.Run("dispatches_canvas_open", func(t *testing.T) {
		ctx := newCanvasTestContext(t)
		client := ctx.NewClient()
		t.Cleanup(func() { client.ForceStop() })

		handler := &recordingCanvasE2EHandler{}
		session := createCanvasSession(t, client, ctx, handler)

		result, err := session.RPC.Canvas.Open(t.Context(), &rpc.CanvasOpenRequest{
			CanvasID:   "counter",
			InstanceID: "counter-1",
			Input:      json.RawMessage(`{"seed":7}`),
		})
		if err != nil {
			t.Fatalf("Canvas.Open failed: %v", err)
		}

		opens := handler.openCallsSnapshot()
		if len(opens) != 1 {
			t.Fatalf("expected 1 OnOpen call, got %d", len(opens))
		}
		assertOpenCall(t, opens[0], "counter", "counter-1", `{"seed":7}`)
		assertOpenCanvasInstance(t, result, "counter", "counter-1", "https://example.test/counter-1")
	})

	t.Run("dispatches_canvas_action_invoke", func(t *testing.T) {
		ctx := newCanvasTestContext(t)
		client := ctx.NewClient()
		t.Cleanup(func() { client.ForceStop() })

		handler := &recordingCanvasE2EHandler{}
		session := createCanvasSession(t, client, ctx, handler)
		openCanvas(t, session, "counter-2", nil)

		result, err := session.RPC.Canvas.InvokeAction(t.Context(), &rpc.CanvasInvokeActionRequest{
			InstanceID: "counter-2",
			ActionName: "increment",
			Input:      json.RawMessage(`{"amount":3}`),
		})
		if err != nil {
			t.Fatalf("Canvas.InvokeAction failed: %v", err)
		}

		actions := handler.actionCallsSnapshot()
		if len(actions) != 1 {
			t.Fatalf("expected 1 OnAction call, got %d", len(actions))
		}
		if actions[0].CanvasID != "counter" {
			t.Errorf("expected canvasId counter, got %q", actions[0].CanvasID)
		}
		if actions[0].InstanceID != "counter-2" {
			t.Errorf("expected instanceId counter-2, got %q", actions[0].InstanceID)
		}
		if actions[0].ActionName != "increment" {
			t.Errorf("expected actionName increment, got %q", actions[0].ActionName)
		}
		assertJSONValue(t, actions[0].Input, `{"amount":3}`)
		assertJSONValue(t, result.Result, `{"ok":true,"actionName":"increment","input":{"amount":3}}`)
	})

	t.Run("dispatches_canvas_close", func(t *testing.T) {
		ctx := newCanvasTestContext(t)
		client := ctx.NewClient()
		t.Cleanup(func() { client.ForceStop() })

		handler := &recordingCanvasE2EHandler{}
		session := createCanvasSession(t, client, ctx, handler)
		openCanvas(t, session, "counter-3", nil)

		if _, err := session.RPC.Canvas.Close(t.Context(), &rpc.CanvasCloseRequest{InstanceID: "counter-3"}); err != nil {
			t.Fatalf("Canvas.Close failed: %v", err)
		}

		time.Sleep(50 * time.Millisecond)
		closes := handler.closeCallsSnapshot()
		if len(closes) != 1 {
			t.Fatalf("expected 1 OnClose call, got %d", len(closes))
		}
		if closes[0].CanvasID != "counter" {
			t.Errorf("expected canvasId counter, got %q", closes[0].CanvasID)
		}
		if closes[0].InstanceID != "counter-3" {
			t.Errorf("expected instanceId counter-3, got %q", closes[0].InstanceID)
		}
	})

	t.Run("returns_canvas_action_no_handler", func(t *testing.T) {
		ctx := newCanvasTestContext(t)
		client := ctx.NewClient()
		t.Cleanup(func() { client.ForceStop() })

		session := createCanvasSession(t, client, ctx, &openOnlyCanvasE2EHandler{})
		openCanvas(t, session, "counter-4", nil)

		_, err := session.RPC.Canvas.InvokeAction(t.Context(), &rpc.CanvasInvokeActionRequest{
			InstanceID: "counter-4",
			ActionName: "increment",
			Input:      json.RawMessage(`{}`),
		})
		if err == nil {
			t.Fatalf("expected Canvas.InvokeAction to fail")
		}
		assertJSONRPCErrorCode(t, err, "canvas_action_no_handler")
	})

	t.Run("seeds_open_canvases_on_resume", func(t *testing.T) {
		ctx := newCanvasTestContext(t)
		client := ctx.NewClient()
		t.Cleanup(func() { client.ForceStop() })

		sessionA := createCanvasSession(t, client, ctx, &recordingCanvasE2EHandler{})
		openCanvas(t, sessionA, "counter-resume", json.RawMessage(`{"initial":true}`))

		resumed, err := client.ResumeSession(t.Context(), sessionA.SessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest:   copilot.PermissionHandler.ApproveAll,
			WorkingDirectory:      ctx.WorkDir,
			Canvases:              counterCanvasDeclarations(),
			CanvasHandler:         &recordingCanvasE2EHandler{},
			RequestCanvasRenderer: copilot.Bool(true),
			ExtensionInfo:         counterExtensionInfo(),
		})
		if err != nil {
			t.Fatalf("ResumeSession failed: %v", err)
		}
		t.Cleanup(func() { _ = resumed.Disconnect() })

		seeded := resumed.OpenCanvases()
		if len(seeded) == 0 {
			t.Fatalf("expected resumed OpenCanvases to contain entries")
		}
		for _, canvas := range seeded {
			if canvas.InstanceID == "counter-resume" {
				if canvas.CanvasID != "counter" {
					t.Fatalf("expected resumed canvasId counter, got %q", canvas.CanvasID)
				}
				return
			}
		}
		t.Fatalf("expected resumed OpenCanvases to include counter-resume, got %+v", seeded)
	})
}

type recordingCanvasE2EHandler struct {
	copilot.CanvasHandlerDefaults

	mu          sync.Mutex
	openCalls   []copilot.CanvasOpenContext
	closeCalls  []copilot.CanvasLifecycleContext
	actionCalls []copilot.CanvasActionContext
}

func (h *recordingCanvasE2EHandler) OnOpen(ctx context.Context, c copilot.CanvasOpenContext) (copilot.CanvasOpenResponse, error) {
	h.mu.Lock()
	h.openCalls = append(h.openCalls, c)
	h.mu.Unlock()
	url := fmt.Sprintf("https://example.test/%s", c.InstanceID)
	return copilot.CanvasOpenResponse{URL: &url}, nil
}

func (h *recordingCanvasE2EHandler) OnClose(ctx context.Context, c copilot.CanvasLifecycleContext) error {
	h.mu.Lock()
	h.closeCalls = append(h.closeCalls, c)
	h.mu.Unlock()
	return nil
}

func (h *recordingCanvasE2EHandler) OnAction(ctx context.Context, c copilot.CanvasActionContext) (any, error) {
	h.mu.Lock()
	h.actionCalls = append(h.actionCalls, c)
	h.mu.Unlock()
	return map[string]any{"ok": true, "actionName": c.ActionName, "input": c.Input}, nil
}

func (h *recordingCanvasE2EHandler) openCallsSnapshot() []copilot.CanvasOpenContext {
	h.mu.Lock()
	defer h.mu.Unlock()
	return append([]copilot.CanvasOpenContext(nil), h.openCalls...)
}

func (h *recordingCanvasE2EHandler) closeCallsSnapshot() []copilot.CanvasLifecycleContext {
	h.mu.Lock()
	defer h.mu.Unlock()
	return append([]copilot.CanvasLifecycleContext(nil), h.closeCalls...)
}

func (h *recordingCanvasE2EHandler) actionCallsSnapshot() []copilot.CanvasActionContext {
	h.mu.Lock()
	defer h.mu.Unlock()
	return append([]copilot.CanvasActionContext(nil), h.actionCalls...)
}

type openOnlyCanvasE2EHandler struct {
	copilot.CanvasHandlerDefaults
}

func (h *openOnlyCanvasE2EHandler) OnOpen(ctx context.Context, c copilot.CanvasOpenContext) (copilot.CanvasOpenResponse, error) {
	url := fmt.Sprintf("https://example.test/%s", c.InstanceID)
	return copilot.CanvasOpenResponse{URL: &url}, nil
}

func newCanvasTestContext(t *testing.T) *testharness.TestContext {
	t.Helper()
	ctx := testharness.NewTestContext(t)
	ctx.ConfigureForTest(t)
	return ctx
}

func createCanvasSession(t *testing.T, client *copilot.Client, ctx *testharness.TestContext, handler copilot.CanvasHandler) *copilot.Session {
	t.Helper()
	session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
		OnPermissionRequest:   copilot.PermissionHandler.ApproveAll,
		WorkingDirectory:      ctx.WorkDir,
		Canvases:              counterCanvasDeclarations(),
		CanvasHandler:         handler,
		RequestCanvasRenderer: copilot.Bool(true),
		ExtensionInfo:         counterExtensionInfo(),
	})
	if err != nil {
		t.Fatalf("CreateSession failed: %v", err)
	}
	t.Cleanup(func() { _ = session.Disconnect() })
	return session
}

func counterCanvasDeclarations() []copilot.CanvasDeclaration {
	description := "Increment the counter"
	return []copilot.CanvasDeclaration{
		{
			ID:          "counter",
			DisplayName: "Counter",
			Description: "A test counter canvas",
			Actions: []rpc.CanvasAction{
				{Name: "increment", Description: &description},
			},
		},
	}
}

func counterExtensionInfo() *copilot.ExtensionInfo {
	return &copilot.ExtensionInfo{Source: "github-app", Name: "counter-provider"}
}

func openCanvas(t *testing.T, session *copilot.Session, instanceID string, input any) *rpc.OpenCanvasInstance {
	t.Helper()
	result, err := session.RPC.Canvas.Open(t.Context(), &rpc.CanvasOpenRequest{
		CanvasID:   "counter",
		InstanceID: instanceID,
		Input:      input,
	})
	if err != nil {
		t.Fatalf("Canvas.Open failed: %v", err)
	}
	return result
}

func assertOpenCall(t *testing.T, got copilot.CanvasOpenContext, canvasID, instanceID, input string) {
	t.Helper()
	if got.CanvasID != canvasID {
		t.Errorf("expected canvasId %q, got %q", canvasID, got.CanvasID)
	}
	if got.InstanceID != instanceID {
		t.Errorf("expected instanceId %q, got %q", instanceID, got.InstanceID)
	}
	assertJSONValue(t, got.Input, input)
}

func assertOpenCanvasInstance(t *testing.T, got *rpc.OpenCanvasInstance, canvasID, instanceID, url string) {
	t.Helper()
	if got == nil {
		t.Fatalf("expected non-nil OpenCanvasInstance")
	}
	if got.CanvasID != canvasID {
		t.Errorf("expected canvasId %q, got %q", canvasID, got.CanvasID)
	}
	if got.InstanceID != instanceID {
		t.Errorf("expected instanceId %q, got %q", instanceID, got.InstanceID)
	}
	if got.URL == nil || *got.URL != url {
		t.Errorf("expected url %q, got %v", url, got.URL)
	}
	if got.Availability != rpc.CanvasInstanceAvailabilityReady {
		t.Errorf("expected availability ready, got %q", got.Availability)
	}
}

func assertJSONValue(t *testing.T, got any, wantJSON string) {
	t.Helper()
	var want any
	if err := json.Unmarshal([]byte(wantJSON), &want); err != nil {
		t.Fatalf("failed to unmarshal expected JSON: %v", err)
	}
	gotJSON, err := json.Marshal(got)
	if err != nil {
		t.Fatalf("failed to marshal actual value: %v", err)
	}
	var normalizedGot any
	if err := json.Unmarshal(gotJSON, &normalizedGot); err != nil {
		t.Fatalf("failed to normalize actual JSON %s: %v", gotJSON, err)
	}
	if !reflect.DeepEqual(normalizedGot, want) {
		t.Fatalf("JSON mismatch: got %s, want %s", gotJSON, wantJSON)
	}
}

func assertJSONRPCErrorCode(t *testing.T, err error, wantCode string) {
	t.Helper()
	rpcErr, ok := err.(*jsonrpc2.Error)
	if !ok {
		t.Fatalf("expected *jsonrpc2.Error, got %T: %v", err, err)
	}
	var data struct {
		Code string `json:"code"`
	}
	if unmarshalErr := json.Unmarshal(rpcErr.Data, &data); unmarshalErr != nil {
		t.Fatalf("failed to unmarshal JSON-RPC error data %s: %v", rpcErr.Data, unmarshalErr)
	}
	if data.Code != wantCode {
		t.Fatalf("expected error code %q, got %q (error: %v)", wantCode, data.Code, err)
	}
}
