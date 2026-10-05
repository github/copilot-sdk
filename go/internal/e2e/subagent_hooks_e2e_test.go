package e2e

import (
	"bytes"
	"encoding/json"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/google/uuid"
)

const childContext = "Subagent start hook verified: read the requested file."
const stopResponsePrefix = "Subagent stop hook verified: "

type subagentRequestRecord struct {
	agentID         string
	parentAgentID   string
	interactionType string
	childPrompt     string
}

type recordingForwardingTransport struct {
	inner                 http.RoundTripper
	mu                    sync.Mutex
	records               []subagentRequestRecord
	modifiedParentRequest chan struct{}
}

func newRecordingForwardingTransport() *recordingForwardingTransport {
	inner := http.DefaultTransport.(*http.Transport).Clone()
	inner.DisableCompression = true
	return &recordingForwardingTransport{inner: inner, modifiedParentRequest: make(chan struct{}, 1)}
}

func (rt *recordingForwardingTransport) RoundTrip(req *http.Request) (*http.Response, error) {
	if isInferenceURL(req.URL.String()) {
		body, err := io.ReadAll(req.Body)
		if err != nil {
			return nil, err
		}
		req.Body = io.NopCloser(bytes.NewReader(body))
		rctx := copilot.RequestContextFrom(req)
		record := subagentRequestRecord{}
		if rctx != nil {
			record.agentID = rctx.AgentID
			record.parentAgentID = rctx.ParentAgentID
			record.interactionType = rctx.InteractionType
		}
		if record.parentAgentID != "" {
			var payload struct {
				Messages []struct {
					Role    string          `json:"role"`
					Content json.RawMessage `json:"content"`
				} `json:"messages"`
			}
			if err := json.Unmarshal(body, &payload); err != nil {
				return nil, err
			}
			for _, message := range payload.Messages {
				if message.Role == "user" && len(message.Content) > 0 {
					var prompts []string
					switch message.Content[0] {
					case '"':
						var prompt string
						if err := json.Unmarshal(message.Content, &prompt); err != nil {
							return nil, err
						}
						prompts = append(prompts, prompt)
					case '[':
						var parts []struct {
							Text string `json:"text"`
						}
						if err := json.Unmarshal(message.Content, &parts); err != nil {
							return nil, err
						}
						for _, part := range parts {
							prompts = append(prompts, part.Text)
						}
					}
					for _, prompt := range prompts {
						if strings.Contains(prompt, childContext+"\n\n") {
							record.childPrompt = prompt
						}
					}
				}
			}
		} else if bytes.Contains(body, []byte(stopResponsePrefix)) {
			select {
			case rt.modifiedParentRequest <- struct{}{}:
			default:
			}
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

	t.Run("should apply subagent lifecycle hook outputs", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		type hookEntry struct {
			kind      string
			toolName  string
			sessionID string
		}
		var hookLog []hookEntry
		var mu sync.Mutex
		const waitingText = "I've launched an explore agent to read subagent-test.txt. Waiting for it to complete..."
		const finalText = "The explore agent successfully read the file. The contents of **subagent-test.txt** are:\n\n```\nHello from subagent test!\n```"
		parentSessionID := uuid.NewString()
		parentWaiting := make(chan struct{})
		releaseView := sync.OnceFunc(func() { close(parentWaiting) })
		defer releaseView()
		var startInputs []copilot.SubagentStartHookInput
		var stopInputs []copilot.SubagentStopHookInput
		var startInvocations []copilot.HookInvocation
		var stopInvocations []copilot.HookInvocation
		stopObserved := make(chan struct{}, 1)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			SessionID:           parentSessionID,
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
					// A fast child can inject its result before the fixture's waiting reply is requested.
					if input.ToolName == "view" && input.SessionID != parentSessionID {
						select {
						case <-parentWaiting:
						case <-t.Context().Done():
							return nil, t.Context().Err()
						}
					}
					return nil, nil
				},
				OnSubagentStart: func(input copilot.SubagentStartHookInput, invocation copilot.HookInvocation) (*copilot.SubagentStartHookOutput, error) {
					mu.Lock()
					startInputs = append(startInputs, input)
					startInvocations = append(startInvocations, invocation)
					mu.Unlock()
					return &copilot.SubagentStartHookOutput{AdditionalContext: childContext}, nil
				},
				OnSubagentStop: func(input copilot.SubagentStopHookInput, invocation copilot.HookInvocation) (*copilot.SubagentStopHookOutput, error) {
					mu.Lock()
					stopInputs = append(stopInputs, input)
					stopInvocations = append(stopInvocations, invocation)
					mu.Unlock()
					select {
					case stopObserved <- struct{}{}:
					default:
					}
					return &copilot.SubagentStopHookOutput{ModifiedResponse: copilot.String(stopResponsePrefix + input.Response)}, nil
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

		unsubscribe := session.On(func(event copilot.SessionEvent) {
			if message, ok := event.Data.(*copilot.AssistantMessageData); ok && (event.AgentID == nil || *event.AgentID == "") && message.Content == waitingText {
				releaseView()
			}
		})
		defer unsubscribe()
		response, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Use the task tool to spawn an explore agent that reads the file subagent-test.txt in the current directory and reports its contents. You must use the task tool.",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}
		if response == nil {
			t.Fatal("Missing parent final response")
		}
		message, ok := response.Data.(*copilot.AssistantMessageData)
		if !ok || (response.AgentID != nil && *response.AgentID != "") || message.Content != finalText {
			t.Fatalf("Unexpected parent final response: %+v", response)
		}
		events, err := session.GetEvents(t.Context())
		if err != nil {
			t.Fatalf("Failed to read history: %v", err)
		}
		var replies []string
		for _, event := range events {
			if message, ok := event.Data.(*copilot.AssistantMessageData); ok && (event.AgentID == nil || *event.AgentID == "") && (message.Content == waitingText || message.Content == finalText) {
				replies = append(replies, message.Content)
			}
		}
		if len(replies) != 2 || replies[0] != waitingText || replies[1] != finalText {
			t.Fatalf("Expected durable waiting reply followed by final reply, got %q", replies)
		}

		select {
		case <-stopObserved:
		case <-time.After(120 * time.Second):
			t.Fatal("Timed out waiting for the subagentStop hook")
		}
		select {
		case <-transport.modifiedParentRequest:
		case <-time.After(120 * time.Second):
			t.Fatal("Timed out waiting for the parent inference request with the rewritten response")
		}
		mu.Lock()
		defer mu.Unlock()
		if len(startInputs) != 1 || len(stopInputs) != 1 {
			t.Fatalf("Expected one subagentStart and one subagentStop invocation, got starts=%+v stops=%+v", startInputs, stopInputs)
		}
		start, stop := startInputs[0], stopInputs[0]
		if startInvocations[0].SessionID != session.SessionID || stopInvocations[0].SessionID != session.SessionID ||
			start.SessionID != session.SessionID || stop.SessionID != session.SessionID {
			t.Errorf("Expected parent session ID %q in hook inputs and invocations, got start=%+v stop=%+v", session.SessionID, start, stop)
		}
		if !start.Timestamp.After(time.UnixMilli(0)) || !stop.Timestamp.After(time.UnixMilli(0)) ||
			stop.Timestamp.Before(start.Timestamp) {
			t.Errorf("Expected ordered timestamps, got start=%v stop=%v", start.Timestamp, stop.Timestamp)
		}
		if start.WorkingDirectory != ctx.WorkDir || stop.WorkingDirectory != ctx.WorkDir {
			t.Errorf("Expected working directory %q, got start=%q stop=%q", ctx.WorkDir, start.WorkingDirectory, stop.WorkingDirectory)
		}
		if stop.TranscriptPath != start.TranscriptPath || (start.TranscriptPath != "" && !filepath.IsAbs(start.TranscriptPath)) {
			t.Errorf("Expected matching transcript paths (absolute when available), got start=%q stop=%q", start.TranscriptPath, stop.TranscriptPath)
		}
		if start.AgentName != "explore" || stop.AgentName != start.AgentName || stop.AgentType != "explore" {
			t.Errorf("Expected explore agent name and type, got start=%+v stop=%+v", start, stop)
		}
		if start.AgentDisplayName != "" {
			t.Errorf("Expected no optional display name on this task-created agent, got %q", start.AgentDisplayName)
		}
		if stop.AgentDisplayName != start.AgentDisplayName {
			t.Errorf("Expected stop display name %q, got %q", start.AgentDisplayName, stop.AgentDisplayName)
		}
		if start.AgentDescription != "" {
			t.Errorf("Expected no optional description on this task-created agent, got %q", start.AgentDescription)
		}
		if stop.AgentDescription != start.AgentDescription {
			t.Errorf("Expected stop description %q, got %q", start.AgentDescription, stop.AgentDescription)
		}
		if stop.AgentID == "" {
			t.Error("Expected a runtime-generated sub-agent ID")
		}
		if stop.StopReason != "end_turn" || !strings.Contains(stop.Response, "Hello from subagent test!") {
			t.Errorf("Expected completed turn and file contents in stop input, got %+v", stop)
		}

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
		requests := transport.inferenceRecords()
		assertSubagentRequestMetadata(t, requests)
		startContextObserved := false
		for _, request := range requests {
			if strings.Contains(request.childPrompt, childContext+"\n\nRead the file \"subagent-test.txt\"") {
				startContextObserved = true
			}
		}
		if !startContextObserved {
			t.Errorf("Start hook context did not reach a child inference prompt: %+v", requests)
		}
	})
}
