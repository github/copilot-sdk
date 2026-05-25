package e2e

import (
	"context"
	"sync"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

func TestCanvasE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	handler := &testCanvasHandler{}
	canvasDecl := copilot.CanvasDeclaration{
		ID:          "counter",
		DisplayName: "Counter",
		Description: "A simple counter canvas for e2e testing",
		InputSchema: map[string]any{
			"type": "object",
			"properties": map[string]any{
				"startValue": map[string]any{"type": "number"},
			},
		},
		Actions: []rpc.CanvasAction{{
			Name:        "increment",
			Description: copilot.String("Increment the counter"),
			InputSchema: map[string]any{
				"type": "object",
				"properties": map[string]any{
					"amount": map[string]any{"type": "number"},
				},
			},
		}},
	}

	session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
		OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		Canvases:            []copilot.CanvasDeclaration{canvasDecl},
		CanvasHandler:       handler,
	})
	if err != nil {
		t.Fatalf("Failed to create session: %v", err)
	}

	listResult, err := session.RPC.Canvas.List(t.Context())
	if err != nil {
		t.Fatalf("Canvas.List failed: %v", err)
	}
	if len(listResult.Canvases) != 1 {
		t.Fatalf("expected 1 canvas, got %d", len(listResult.Canvases))
	}
	if listResult.Canvases[0].CanvasID != "counter" {
		t.Fatalf("expected canvasId=counter, got %q", listResult.Canvases[0].CanvasID)
	}

	openResult, err := session.RPC.Canvas.Open(t.Context(), &rpc.CanvasOpenRequest{
		CanvasID:   "counter",
		InstanceID: "counter-1",
		Input: map[string]any{
			"startValue": float64(3),
		},
	})
	if err != nil {
		t.Fatalf("Canvas.Open failed: %v", err)
	}
	if openResult.CanvasID != "counter" || openResult.InstanceID != "counter-1" {
		t.Fatalf("unexpected open result: %+v", openResult)
	}
	if openResult.URL == nil || *openResult.URL != "https://example.test/counter/counter-1" {
		t.Fatalf("unexpected open URL: %+v", openResult.URL)
	}
	if calls := handler.OpenCalls(); len(calls) != 1 || calls[0].CanvasID != "counter" || calls[0].InstanceID != "counter-1" {
		t.Fatalf("unexpected open calls: %+v", calls)
	}

	actionResult, err := session.RPC.Canvas.InvokeAction(t.Context(), &rpc.CanvasInvokeActionRequest{
		InstanceID: "counter-1",
		ActionName: "increment",
		Input: map[string]any{
			"amount": float64(2),
		},
	})
	if err != nil {
		t.Fatalf("Canvas.InvokeAction failed: %v", err)
	}
	actionPayload, ok := actionResult.Result.(map[string]any)
	if !ok || actionPayload["count"] != float64(5) {
		t.Fatalf("unexpected action result: %#v", actionResult.Result)
	}
	if calls := handler.ActionCalls(); len(calls) != 1 || calls[0].ActionName != "increment" {
		t.Fatalf("unexpected action calls: %+v", calls)
	}

	closeResult, err := session.RPC.Canvas.Close(t.Context(), &rpc.CanvasCloseRequest{
		InstanceID: "counter-1",
	})
	if err != nil {
		t.Fatalf("Canvas.Close failed: %v", err)
	}
	if closeResult == nil {
		t.Fatal("expected non-nil close result")
	}
	if calls := handler.CloseCalls(); len(calls) != 1 || calls[0].CanvasID != "counter" || calls[0].InstanceID != "counter-1" {
		t.Fatalf("unexpected close calls: %+v", calls)
	}
}

type testCanvasHandler struct {
	copilot.CanvasHandlerDefaults

	mu          sync.Mutex
	openCalls   []canvasOpenCall
	closeCalls  []canvasCloseCall
	actionCalls []canvasActionCall
	counts      map[string]float64
}

type canvasOpenCall struct {
	CanvasID   string
	InstanceID string
	Input      any
}

type canvasCloseCall struct {
	CanvasID   string
	InstanceID string
}

type canvasActionCall struct {
	CanvasID   string
	InstanceID string
	ActionName string
	Input      any
}

func (h *testCanvasHandler) OnOpen(ctx context.Context, req rpc.CanvasProviderOpenRequest) (rpc.CanvasProviderOpenResult, error) {
	h.mu.Lock()
	defer h.mu.Unlock()

	if h.counts == nil {
		h.counts = make(map[string]float64)
	}
	h.openCalls = append(h.openCalls, canvasOpenCall{
		CanvasID:   req.CanvasID,
		InstanceID: req.InstanceID,
		Input:      req.Input,
	})
	h.counts[req.InstanceID] = numberField(req.Input, "startValue")

	return rpc.CanvasProviderOpenResult{
		URL:    copilot.String("https://example.test/counter/" + req.InstanceID),
		Title:  copilot.String("Counter"),
		Status: copilot.String("ready"),
	}, nil
}

func (h *testCanvasHandler) OnClose(ctx context.Context, req rpc.CanvasProviderCloseRequest) error {
	h.mu.Lock()
	defer h.mu.Unlock()

	h.closeCalls = append(h.closeCalls, canvasCloseCall{
		CanvasID:   req.CanvasID,
		InstanceID: req.InstanceID,
	})
	delete(h.counts, req.InstanceID)
	return nil
}

func (h *testCanvasHandler) OnAction(ctx context.Context, req rpc.CanvasProviderInvokeActionRequest) (any, error) {
	h.mu.Lock()
	defer h.mu.Unlock()

	if h.counts == nil {
		h.counts = make(map[string]float64)
	}
	h.actionCalls = append(h.actionCalls, canvasActionCall{
		CanvasID:   req.CanvasID,
		InstanceID: req.InstanceID,
		ActionName: req.ActionName,
		Input:      req.Input,
	})
	h.counts[req.InstanceID] += numberField(req.Input, "amount")
	return map[string]any{"count": h.counts[req.InstanceID]}, nil
}

func (h *testCanvasHandler) OpenCalls() []canvasOpenCall {
	h.mu.Lock()
	defer h.mu.Unlock()
	out := make([]canvasOpenCall, len(h.openCalls))
	copy(out, h.openCalls)
	return out
}

func (h *testCanvasHandler) CloseCalls() []canvasCloseCall {
	h.mu.Lock()
	defer h.mu.Unlock()
	out := make([]canvasCloseCall, len(h.closeCalls))
	copy(out, h.closeCalls)
	return out
}

func (h *testCanvasHandler) ActionCalls() []canvasActionCall {
	h.mu.Lock()
	defer h.mu.Unlock()
	out := make([]canvasActionCall, len(h.actionCalls))
	copy(out, h.actionCalls)
	return out
}

func numberField(value any, key string) float64 {
	m, ok := value.(map[string]any)
	if !ok {
		return 0
	}
	n, ok := m[key].(float64)
	if !ok {
		return 0
	}
	return n
}
