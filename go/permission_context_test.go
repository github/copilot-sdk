package copilot

import (
	"encoding/json"
	"fmt"
	"io"
	"testing"
	"time"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
)

// runPermissionExchange drives executePermissionAndRespond with the supplied
// handler and captures the raw JSON-RPC request frame the SDK emits (if any).
// The second return value reports whether a request was sent at all, so tests
// can assert that no-result decisions suppress the response entirely.
func runPermissionExchange(t *testing.T, handler PermissionHandlerFunc) (frame []byte, sent bool) {
	t.Helper()

	stdinR, stdinW := io.Pipe()
	stdoutR, stdoutW := io.Pipe()
	t.Cleanup(func() {
		stdinR.Close()
		stdinW.Close()
		stdoutR.Close()
		stdoutW.Close()
	})

	client := jsonrpc2.NewClient(stdinW, stdoutR)
	client.Start()
	t.Cleanup(client.Stop)

	session := &Session{
		SessionID: "session-1",
		client:    client,
		RPC:       rpc.NewSessionRPC(client, "session-1"),
	}

	frameCh := make(chan []byte, 1)
	go func() {
		captured, err := readTestJSONRPCFrame(stdinR)
		if err != nil {
			return
		}
		var request struct {
			ID json.RawMessage `json:"id"`
		}
		_ = json.Unmarshal(captured, &request)
		// Publish the captured frame before unblocking the RPC round trip so a
		// sent response is always observable before executePermissionAndRespond
		// returns.
		frameCh <- captured
		response := map[string]any{
			"jsonrpc": "2.0",
			"id":      json.RawMessage(request.ID),
			"result":  map[string]any{"applied": true},
		}
		data, _ := json.Marshal(response)
		_, _ = fmt.Fprintf(stdoutW, "Content-Length: %d\r\n\r\n%s", len(data), data)
	}()

	done := make(chan struct{})
	go func() {
		session.executePermissionAndRespond("permission-1", nil, handler)
		close(done)
	}()

	select {
	case captured := <-frameCh:
		return captured, true
	case <-done:
		select {
		case captured := <-frameCh:
			return captured, true
		default:
			return nil, false
		}
	case <-time.After(2 * time.Second):
		t.Fatal("timed out waiting for permission response")
		return nil, false
	}
}

// paramsOf extracts the top-level params object from a JSON-RPC request frame.
func paramsOf(t *testing.T, frame []byte) map[string]json.RawMessage {
	t.Helper()
	var request struct {
		Method string                     `json:"method"`
		Params map[string]json.RawMessage `json:"params"`
	}
	if err := json.Unmarshal(frame, &request); err != nil {
		t.Fatalf("failed to unmarshal request frame: %v", err)
	}
	if request.Method != "session.permissions.handlePendingPermissionRequest" {
		t.Fatalf("unexpected method %q", request.Method)
	}
	return request.Params
}

func sampleDecisionContext() *rpc.PermissionDecisionContext {
	return &rpc.PermissionDecisionContext{
		Outcome: PermissionDecisionOutcomeAutoApproved,
		Source:  PermissionDecisionSourceHostPolicy,
		Surface: PermissionDecisionSurfaceSDK,
	}
}

func TestPermissionDecisionContextForwardedAsSiblingOfResult(t *testing.T) {
	frame, sent := runPermissionExchange(t, func(PermissionRequest, PermissionInvocation) (rpc.PermissionDecision, error) {
		return WithDecisionContext(&rpc.PermissionDecisionApproveOnce{}, sampleDecisionContext()), nil
	})
	if !sent {
		t.Fatal("expected a permission response to be sent")
	}

	params := paramsOf(t, frame)

	// decisionContext must be a top-level sibling of result.
	rawContext, ok := params["decisionContext"]
	if !ok {
		t.Fatal("expected decisionContext to be present as a top-level sibling of result")
	}
	var context rpc.PermissionDecisionContext
	if err := json.Unmarshal(rawContext, &context); err != nil {
		t.Fatalf("failed to unmarshal decisionContext: %v", err)
	}
	if context.Outcome != PermissionDecisionOutcomeAutoApproved ||
		context.Source != PermissionDecisionSourceHostPolicy ||
		context.Surface != PermissionDecisionSurfaceSDK {
		t.Fatalf("unexpected decisionContext contents: %#v", context)
	}

	// result must exist and must NOT contain a nested decisionContext.
	rawResult, ok := params["result"]
	if !ok {
		t.Fatal("expected result to be present")
	}
	var result map[string]json.RawMessage
	if err := json.Unmarshal(rawResult, &result); err != nil {
		t.Fatalf("failed to unmarshal result: %v", err)
	}
	if _, nested := result["decisionContext"]; nested {
		t.Fatal("decisionContext must not be nested inside result")
	}
}

func TestPermissionDecisionContextOmittedWithoutAttribution(t *testing.T) {
	frame, sent := runPermissionExchange(t, func(PermissionRequest, PermissionInvocation) (rpc.PermissionDecision, error) {
		return &rpc.PermissionDecisionApproveOnce{}, nil
	})
	if !sent {
		t.Fatal("expected a permission response to be sent")
	}

	params := paramsOf(t, frame)
	if _, ok := params["decisionContext"]; ok {
		t.Fatal("expected decisionContext to be absent when no context is supplied")
	}
	if _, ok := params["result"]; !ok {
		t.Fatal("expected result to be present")
	}
}

func TestWithDecisionContextReplacesRatherThanNests(t *testing.T) {
	first := &rpc.PermissionDecisionContext{
		Outcome: PermissionDecisionOutcomePromptedUser,
		Source:  PermissionDecisionSourceHumanResponse,
		Surface: PermissionDecisionSurfaceTui,
	}
	second := sampleDecisionContext()

	wrapped := WithDecisionContext(WithDecisionContext(&rpc.PermissionDecisionApproveOnce{}, first), second)

	if wrapped.DecisionContext != second {
		t.Fatalf("expected the second context to replace the first, got %#v", wrapped.DecisionContext)
	}
	// The underlying decision must be the plain approve-once, not another wrapper.
	if _, ok := wrapped.PermissionDecision.(*rpc.PermissionDecisionApproveOnce); !ok {
		t.Fatalf("expected unwrapped decision to be *rpc.PermissionDecisionApproveOnce, got %T", wrapped.PermissionDecision)
	}

	frame, sent := runPermissionExchange(t, func(PermissionRequest, PermissionInvocation) (rpc.PermissionDecision, error) {
		return wrapped, nil
	})
	if !sent {
		t.Fatal("expected a permission response to be sent")
	}
	params := paramsOf(t, frame)
	rawContext, ok := params["decisionContext"]
	if !ok {
		t.Fatal("expected decisionContext to be present")
	}
	var context rpc.PermissionDecisionContext
	if err := json.Unmarshal(rawContext, &context); err != nil {
		t.Fatalf("failed to unmarshal decisionContext: %v", err)
	}
	if context.Surface != PermissionDecisionSurfaceSDK {
		t.Fatalf("expected replaced surface %q, got %q", PermissionDecisionSurfaceSDK, context.Surface)
	}
}

func TestAttributedNoResultStillSuppressesResponse(t *testing.T) {
	frame, sent := runPermissionExchange(t, func(PermissionRequest, PermissionInvocation) (rpc.PermissionDecision, error) {
		return WithDecisionContext(&rpc.PermissionDecisionNoResult{}, sampleDecisionContext()), nil
	})
	if sent {
		t.Fatalf("expected no response to be sent for an attributed no-result decision, got frame: %s", frame)
	}
}
