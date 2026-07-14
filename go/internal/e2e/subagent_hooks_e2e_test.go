package e2e

import (
	"net/http"
	"os"
	"path/filepath"
	"sync"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

type subagentRequestRecord struct {
	agentID         string
	parentAgentID   string
	interactionType string
}

type recordingForwardingTransport struct {
	inner   http.RoundTripper
	mu      sync.Mutex
	records []subagentRequestRecord
}

func newRecordingForwardingTransport() *recordingForwardingTransport {
	inner := http.DefaultTransport.(*http.Transport).Clone()
	inner.DisableCompression = true
	return &recordingForwardingTransport{inner: inner}
}

func (rt *recordingForwardingTransport) RoundTrip(req *http.Request) (*http.Response, error) {
	if isInferenceURL(req.URL.String()) {
		rctx := copilot.RequestContextFrom(req)
		record := subagentRequestRecord{}
		if rctx != nil {
			record.agentID = rctx.AgentID
			record.parentAgentID = rctx.ParentAgentID
			record.interactionType = rctx.InteractionType
		}
		rt.mu.Lock()
		rt.records = append(rt.records, record)
		rt.mu.Unlock()
	}
	return rt.inner.RoundTrip(req)
}

func (rt *recordingForwardingTransport) inferenceRecords() []subagentRequestRecord {
	rt.mu.Lock()
	defer rt.mu.Unlock()
	out := make([]subagentRequestRecord, len(rt.records))
	copy(out, rt.records)
	return out
}

func assertSubagentRequestMetadata(t *testing.T, records []subagentRequestRecord) {
	t.Helper()
	if len(records) == 0 {
		t.Fatal("request handler should observe inference requests")
	}
	for _, r := range records {
		if r.parentAgentID == "" {
			continue
		}
		if r.agentID == "" {
			t.Fatal("sub-agent inference request should carry an agent id")
		}
		if r.interactionType == "" {
			t.Fatal("sub-agent inference request should carry an interaction type")
		}
		if r.parentAgentID == r.agentID {
			t.Fatal("sub-agent inference request should have distinct parent and child agent ids")
		}
		return
	}
	t.Fatal("sub-agent inference request should carry a parent agent id")
}

func TestSubagentHooksE2E(t *testing.T) {
	testharness.SkipIfInProcess(t, "an LLM inference provider is process-global in-process")
	ctx := testharness.NewTestContext(t)
	transport := newRecordingForwardingTransport()
	client := ctx.NewClient(func(o *copilot.ClientOptions) {
		o.Env = append(o.Env, "COPILOT_EXP_COPILOT_CLI_SESSION_BASED_SUBAGENTS=true")
		o.RequestHandler = &copilot.CopilotRequestHandler{Transport: transport}
	})
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should invoke preToolUse and postToolUse hooks for sub-agent tool calls", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		type hookEntry struct {
			kind      string
			toolName  string
			sessionID string
		}
		var hookLog []hookEntry
		var mu sync.Mutex

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Hooks: &copilot.SessionHooks{
				OnPreToolUse: func(input copilot.PreToolUseHookInput, invocation copilot.HookInvocation) (*copilot.PreToolUseHookOutput, error) {
					mu.Lock()
					hookLog = append(hookLog, hookEntry{kind: "pre", toolName: input.ToolName, sessionID: input.SessionID})
					mu.Unlock()
					return &copilot.PreToolUseHookOutput{PermissionDecision: "allow"}, nil
				},
				OnPostToolUse: func(input copilot.PostToolUseHookInput, invocation copilot.HookInvocation) (*copilot.PostToolUseHookOutput, error) {
					mu.Lock()
					hookLog = append(hookLog, hookEntry{kind: "post", toolName: input.ToolName, sessionID: input.SessionID})
					mu.Unlock()
					return nil, nil
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// Create a file for the sub-agent to read
		testFile := filepath.Join(ctx.WorkDir, "subagent-test.txt")
		if err := os.WriteFile(testFile, []byte("Hello from subagent test!"), 0644); err != nil {
			t.Fatalf("Failed to write test file: %v", err)
		}

		_, err = session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Use the task tool to spawn an explore agent that reads the file subagent-test.txt in the current directory and reports its contents. You must use the task tool.",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		mu.Lock()
		defer mu.Unlock()

		// Parent tool hooks fire for "task"
		var taskPre *hookEntry
		for i := range hookLog {
			if hookLog[i].kind == "pre" && hookLog[i].toolName == "task" {
				taskPre = &hookLog[i]
				break
			}
		}
		if taskPre == nil {
			t.Fatal("preToolUse should fire for the parent's 'task' tool call")
			return
		}

		// Sub-agent tool hooks fire for "view"
		var viewPre, viewPost []hookEntry
		for _, h := range hookLog {
			if h.toolName == "view" {
				if h.kind == "pre" {
					viewPre = append(viewPre, h)
				} else {
					viewPost = append(viewPost, h)
				}
			}
		}
		if len(viewPre) == 0 {
			t.Fatal("preToolUse should fire for the sub-agent's 'view' tool call")
		}
		if len(viewPost) == 0 {
			t.Fatal("postToolUse should fire for the sub-agent's 'view' tool call")
		}

		// input.SessionID distinguishes parent from sub-agent
		if viewPre[0].sessionID == taskPre.sessionID {
			t.Error("Sub-agent tool hooks should have a different sessionId than parent tool hooks")
		}
		assertSubagentRequestMetadata(t, transport.inferenceRecords())
	})
}
