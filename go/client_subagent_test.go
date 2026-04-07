package copilot

import (
	"encoding/json"
	"io"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
)

// newTestClient creates a minimal test client with initialized maps.
func newTestClient() *Client {
	return &Client{
		sessions:          make(map[string]*Session),
		childToParent:     make(map[string]string),
		childToAgent:      make(map[string]string),
		subagentInstances: make(map[string]map[string]*subagentInstance),
	}
}

// newSubagentTestSession creates a minimal test session with tools and agents.
func newSubagentTestSession(id string, tools []Tool, agents []CustomAgentConfig) *Session {
	s := &Session{
		SessionID:    id,
		toolHandlers: make(map[string]ToolHandler),
		customAgents: agents,
	}
	for _, t := range tools {
		if t.Name != "" && t.Handler != nil {
			s.toolHandlers[t.Name] = t.Handler
		}
	}
	return s
}

func strPtr(s string) *string { return &s }

func testToolHandler(inv ToolInvocation) (ToolResult, error) {
	return ToolResult{TextResultForLLM: "ok", ResultType: "success"}, nil
}

// ---------------------------------------------------------------------------
// TestResolveSession
// ---------------------------------------------------------------------------

func TestResolveSession(t *testing.T) {
	t.Run("direct_session_returns_session", func(t *testing.T) {
		c := newTestClient()
		parent := newSubagentTestSession("parent-1", nil, nil)
		c.sessions["parent-1"] = parent

		session, isChild, err := c.resolveSession("parent-1")
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if isChild {
			t.Fatal("expected isChild=false for direct session")
		}
		if session != parent {
			t.Fatal("returned session does not match registered session")
		}
	})

	t.Run("child_session_returns_parent", func(t *testing.T) {
		c := newTestClient()
		parent := newSubagentTestSession("parent-1", nil, nil)
		c.sessions["parent-1"] = parent
		c.childToParent["child-1"] = "parent-1"

		session, isChild, err := c.resolveSession("child-1")
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if !isChild {
			t.Fatal("expected isChild=true for child session")
		}
		if session != parent {
			t.Fatal("returned session should be the parent session")
		}
	})

	t.Run("unknown_session_returns_error", func(t *testing.T) {
		c := newTestClient()

		session, isChild, err := c.resolveSession("nonexistent")
		if err == nil {
			t.Fatal("expected error for unknown session")
		}
		if !strings.Contains(err.Error(), "unknown session") {
			t.Fatalf("error should contain 'unknown session', got: %v", err)
		}
		if isChild {
			t.Fatal("expected isChild=false")
		}
		if session != nil {
			t.Fatal("expected nil session")
		}
	})

	t.Run("child_of_deleted_parent_returns_error", func(t *testing.T) {
		c := newTestClient()
		c.childToParent["child-1"] = "parent-1"
		// parent-1 is NOT registered in c.sessions

		session, isChild, err := c.resolveSession("child-1")
		if err == nil {
			t.Fatal("expected error when parent session is missing")
		}
		if !strings.Contains(err.Error(), "parent session") {
			t.Fatalf("error should contain 'parent session', got: %v", err)
		}
		if isChild {
			t.Fatal("expected isChild=false on error path")
		}
		if session != nil {
			t.Fatal("expected nil session")
		}
	})
}

// ---------------------------------------------------------------------------
// TestChildToolAllowlist
// ---------------------------------------------------------------------------

func TestChildToolAllowlist(t *testing.T) {
	setup := func(tools []string) *Client {
		c := newTestClient()
		agents := []CustomAgentConfig{{Name: "test-agent", Tools: tools}}
		parent := newSubagentTestSession("parent-1", []Tool{
			{Name: "save_output", Handler: testToolHandler},
			{Name: "other_tool", Handler: testToolHandler},
		}, agents)
		c.sessions["parent-1"] = parent
		c.childToParent["child-1"] = "parent-1"
		c.childToAgent["child-1"] = "test-agent"
		return c
	}

	t.Run("nil_tools_allows_all", func(t *testing.T) {
		c := setup(nil) // nil Tools = all allowed
		if !c.isToolAllowedForChild("child-1", "save_output") {
			t.Fatal("nil Tools should allow save_output")
		}
		if !c.isToolAllowedForChild("child-1", "other_tool") {
			t.Fatal("nil Tools should allow other_tool")
		}
		if !c.isToolAllowedForChild("child-1", "any_random_tool") {
			t.Fatal("nil Tools should allow any tool")
		}
	})

	t.Run("explicit_list_allows_listed_tool", func(t *testing.T) {
		c := setup([]string{"save_output"})
		if !c.isToolAllowedForChild("child-1", "save_output") {
			t.Fatal("save_output should be allowed")
		}
	})

	t.Run("explicit_list_blocks_unlisted_tool", func(t *testing.T) {
		c := setup([]string{"save_output"})
		if c.isToolAllowedForChild("child-1", "other_tool") {
			t.Fatal("other_tool should be blocked")
		}
	})

	t.Run("empty_tools_blocks_all", func(t *testing.T) {
		c := setup([]string{}) // empty = block all
		if c.isToolAllowedForChild("child-1", "save_output") {
			t.Fatal("empty Tools should block save_output")
		}
		if c.isToolAllowedForChild("child-1", "other_tool") {
			t.Fatal("empty Tools should block other_tool")
		}
	})
}

// ---------------------------------------------------------------------------
// TestSubagentInstanceTracking
// ---------------------------------------------------------------------------

func TestSubagentInstanceTracking(t *testing.T) {
	makeEvent := func(evType SessionEventType, toolCallID, agentName, childSessionID string) SessionEvent {
		return SessionEvent{
			Type:      evType,
			Timestamp: time.Now(),
			Data: Data{
				ToolCallID:      strPtr(toolCallID),
				AgentName:       strPtr(agentName),
				RemoteSessionID: strPtr(childSessionID),
			},
		}
	}

	t.Run("started_creates_instance", func(t *testing.T) {
		c := newTestClient()
		parent := newSubagentTestSession("parent-1", nil, nil)
		c.sessions["parent-1"] = parent

		event := makeEvent(SessionEventTypeSubagentStarted, "tc-1", "my-agent", "child-session-1")
		c.onSubagentStarted("parent-1", event)

		// Verify subagentInstances
		instances, ok := c.subagentInstances["parent-1"]
		if !ok {
			t.Fatal("expected subagentInstances entry for parent-1")
		}
		inst, ok := instances["tc-1"]
		if !ok {
			t.Fatal("expected instance with toolCallID tc-1")
		}
		if inst.agentName != "my-agent" {
			t.Fatalf("expected agentName 'my-agent', got %q", inst.agentName)
		}
		if inst.childSessionID != "child-session-1" {
			t.Fatalf("expected childSessionID 'child-session-1', got %q", inst.childSessionID)
		}

		// Verify child mappings
		if c.childToParent["child-session-1"] != "parent-1" {
			t.Fatal("childToParent mapping not set")
		}
		if c.childToAgent["child-session-1"] != "my-agent" {
			t.Fatal("childToAgent mapping not set")
		}
	})

	t.Run("completed_removes_instance", func(t *testing.T) {
		c := newTestClient()
		parent := newSubagentTestSession("parent-1", nil, nil)
		c.sessions["parent-1"] = parent

		startEvent := makeEvent(SessionEventTypeSubagentStarted, "tc-1", "my-agent", "child-session-1")
		c.onSubagentStarted("parent-1", startEvent)

		endEvent := makeEvent(SessionEventTypeSubagentCompleted, "tc-1", "my-agent", "child-session-1")
		c.onSubagentEnded("parent-1", endEvent)

		// Instance removed
		if instances, ok := c.subagentInstances["parent-1"]; ok && len(instances) > 0 {
			t.Fatal("expected instance to be removed after completion")
		}

		// Child mappings preserved for in-flight requests
		if c.childToParent["child-session-1"] != "parent-1" {
			t.Fatal("childToParent should be preserved after subagent completion")
		}
		if c.childToAgent["child-session-1"] != "my-agent" {
			t.Fatal("childToAgent should be preserved after subagent completion")
		}
	})

	t.Run("concurrent_same_agent_tracked_independently", func(t *testing.T) {
		c := newTestClient()
		parent := newSubagentTestSession("parent-1", nil, nil)
		c.sessions["parent-1"] = parent

		// Two launches of the same agent with different toolCallIDs
		event1 := makeEvent(SessionEventTypeSubagentStarted, "tc-1", "my-agent", "child-1")
		event2 := makeEvent(SessionEventTypeSubagentStarted, "tc-2", "my-agent", "child-2")
		c.onSubagentStarted("parent-1", event1)
		c.onSubagentStarted("parent-1", event2)

		instances := c.subagentInstances["parent-1"]
		if len(instances) != 2 {
			t.Fatalf("expected 2 instances, got %d", len(instances))
		}

		// Complete one
		endEvent := makeEvent(SessionEventTypeSubagentCompleted, "tc-1", "my-agent", "child-1")
		c.onSubagentEnded("parent-1", endEvent)

		instances = c.subagentInstances["parent-1"]
		if len(instances) != 1 {
			t.Fatalf("expected 1 instance remaining, got %d", len(instances))
		}
		if _, ok := instances["tc-2"]; !ok {
			t.Fatal("tc-2 should still be tracked")
		}
	})
}

// ---------------------------------------------------------------------------
// TestRequestHandlerResolution
// ---------------------------------------------------------------------------

func TestRequestHandlerResolution(t *testing.T) {
	t.Run("tool_call_resolves_child_session", func(t *testing.T) {
		c := newTestClient()
		agents := []CustomAgentConfig{{Name: "test-agent", Tools: nil}} // nil = all tools
		parent := newSubagentTestSession("parent-1", []Tool{
			{Name: "my_tool", Handler: testToolHandler},
		}, agents)
		c.sessions["parent-1"] = parent
		c.childToParent["child-1"] = "parent-1"
		c.childToAgent["child-1"] = "test-agent"

		resp, rpcErr := c.handleToolCallRequestV2(toolCallRequestV2{
			SessionID:  "child-1",
			ToolCallID: "tc-1",
			ToolName:   "my_tool",
			Arguments:  map[string]any{},
		})
		if rpcErr != nil {
			t.Fatalf("unexpected RPC error: %v", rpcErr.Message)
		}
		if resp.Result.ResultType != "success" {
			t.Fatalf("expected success result, got %q", resp.Result.ResultType)
		}
		if resp.Result.TextResultForLLM != "ok" {
			t.Fatalf("expected 'ok', got %q", resp.Result.TextResultForLLM)
		}
	})

	t.Run("permission_request_resolves_child_session", func(t *testing.T) {
		c := newTestClient()
		parent := newSubagentTestSession("parent-1", nil, nil)
		parent.permissionHandler = func(req PermissionRequest, inv PermissionInvocation) (PermissionRequestResult, error) {
			return PermissionRequestResult{Kind: "approved"}, nil
		}
		c.sessions["parent-1"] = parent
		c.childToParent["child-1"] = "parent-1"
		c.childToAgent["child-1"] = "test-agent"

		resp, rpcErr := c.handlePermissionRequestV2(permissionRequestV2{
			SessionID: "child-1",
			Request:   PermissionRequest{Kind: "file_write"},
		})
		if rpcErr != nil {
			t.Fatalf("unexpected RPC error: %v", rpcErr.Message)
		}
		if resp.Result.Kind != "approved" {
			t.Fatalf("expected 'approved', got %q", resp.Result.Kind)
		}
	})

	t.Run("user_input_resolves_child_session", func(t *testing.T) {
		c := newTestClient()
		parent := newSubagentTestSession("parent-1", nil, nil)
		parent.userInputHandler = func(req UserInputRequest, inv UserInputInvocation) (UserInputResponse, error) {
			return UserInputResponse{Answer: "test-answer"}, nil
		}
		c.sessions["parent-1"] = parent
		c.childToParent["child-1"] = "parent-1"
		c.childToAgent["child-1"] = "test-agent"

		resp, rpcErr := c.handleUserInputRequest(userInputRequest{
			SessionID: "child-1",
			Question:  "What is your name?",
		})
		if rpcErr != nil {
			t.Fatalf("unexpected RPC error: %v", rpcErr.Message)
		}
		if resp.Answer != "test-answer" {
			t.Fatalf("expected 'test-answer', got %q", resp.Answer)
		}
	})

	t.Run("hooks_invoke_resolves_child_session", func(t *testing.T) {
		c := newTestClient()
		parent := newSubagentTestSession("parent-1", nil, nil)
		parent.hooks = &SessionHooks{
			OnPreToolUse: func(input PreToolUseHookInput, inv HookInvocation) (*PreToolUseHookOutput, error) {
				return &PreToolUseHookOutput{PermissionDecision: "allow"}, nil
			},
		}
		c.sessions["parent-1"] = parent
		c.childToParent["child-1"] = "parent-1"
		c.childToAgent["child-1"] = "test-agent"

		hookInput, _ := json.Marshal(PreToolUseHookInput{
			Timestamp: time.Now().Unix(),
			Cwd:       "/tmp",
			ToolName:  "some_tool",
		})

		result, rpcErr := c.handleHooksInvoke(hooksInvokeRequest{
			SessionID: "child-1",
			Type:      "preToolUse",
			Input:     json.RawMessage(hookInput),
		})
		if rpcErr != nil {
			t.Fatalf("unexpected RPC error: %v", rpcErr.Message)
		}
		if result == nil {
			t.Fatal("expected non-nil result")
		}
		if result["output"] == nil {
			t.Fatal("expected output in result")
		}
	})

	t.Run("tool_call_child_denied_tool_returns_unsupported", func(t *testing.T) {
		c := newTestClient()
		agents := []CustomAgentConfig{{Name: "test-agent", Tools: []string{"allowed_tool"}}}
		parent := newSubagentTestSession("parent-1", []Tool{
			{Name: "allowed_tool", Handler: testToolHandler},
			{Name: "denied_tool", Handler: testToolHandler},
		}, agents)
		c.sessions["parent-1"] = parent
		c.childToParent["child-1"] = "parent-1"
		c.childToAgent["child-1"] = "test-agent"

		resp, rpcErr := c.handleToolCallRequestV2(toolCallRequestV2{
			SessionID:  "child-1",
			ToolCallID: "tc-1",
			ToolName:   "denied_tool",
			Arguments:  map[string]any{},
		})
		// Should NOT return an RPC error — returns an unsupported tool result instead
		if rpcErr != nil {
			t.Fatalf("should not return RPC error for denied tool, got: %v", rpcErr.Message)
		}
		if resp.Result.ResultType != "failure" {
			t.Fatalf("expected failure result, got %q", resp.Result.ResultType)
		}
		if !strings.Contains(resp.Result.TextResultForLLM, "not supported") {
			t.Fatalf("expected 'not supported' message, got %q", resp.Result.TextResultForLLM)
		}
	})
}

// ---------------------------------------------------------------------------
// TestCleanup
// ---------------------------------------------------------------------------

func TestCleanup(t *testing.T) {
	t.Run("stop_clears_all_maps", func(t *testing.T) {
		c := newTestClient()
		parent := newSubagentTestSession("parent-1", nil, nil)
		c.sessions["parent-1"] = parent
		c.childToParent["child-1"] = "parent-1"
		c.childToAgent["child-1"] = "test-agent"
		c.subagentInstances["parent-1"] = map[string]*subagentInstance{
			"tc-1": {agentName: "test-agent", toolCallID: "tc-1"},
		}

		// Simulate cleanup (Stop() does RPC + map clearing; we test removeChildMappingsForParentLocked + manual clear)
		c.sessionsMux.Lock()
		c.removeChildMappingsForParentLocked("parent-1")
		delete(c.sessions, "parent-1")
		c.sessionsMux.Unlock()

		if len(c.childToParent) != 0 {
			t.Fatal("childToParent should be empty")
		}
		if len(c.childToAgent) != 0 {
			t.Fatal("childToAgent should be empty")
		}
		if len(c.subagentInstances) != 0 {
			t.Fatal("subagentInstances should be empty")
		}
		if len(c.sessions) != 0 {
			t.Fatal("sessions should be empty")
		}
	})

	t.Run("delete_session_clears_only_target_children", func(t *testing.T) {
		c := newTestClient()
		parentA := newSubagentTestSession("parent-A", nil, nil)
		parentB := newSubagentTestSession("parent-B", nil, nil)
		c.sessions["parent-A"] = parentA
		c.sessions["parent-B"] = parentB
		c.childToParent["child-A1"] = "parent-A"
		c.childToParent["child-A2"] = "parent-A"
		c.childToParent["child-B1"] = "parent-B"
		c.childToAgent["child-A1"] = "agent-a"
		c.childToAgent["child-A2"] = "agent-a"
		c.childToAgent["child-B1"] = "agent-b"
		c.subagentInstances["parent-A"] = map[string]*subagentInstance{
			"tc-a1": {agentName: "agent-a"},
		}
		c.subagentInstances["parent-B"] = map[string]*subagentInstance{
			"tc-b1": {agentName: "agent-b"},
		}

		c.sessionsMux.Lock()
		c.removeChildMappingsForParentLocked("parent-A")
		c.sessionsMux.Unlock()

		// parent-A children removed
		if _, ok := c.childToParent["child-A1"]; ok {
			t.Fatal("child-A1 should be removed")
		}
		if _, ok := c.childToParent["child-A2"]; ok {
			t.Fatal("child-A2 should be removed")
		}
		if _, ok := c.subagentInstances["parent-A"]; ok {
			t.Fatal("parent-A subagentInstances should be removed")
		}

		// parent-B children intact
		if c.childToParent["child-B1"] != "parent-B" {
			t.Fatal("child-B1 mapping should still exist")
		}
		if c.childToAgent["child-B1"] != "agent-b" {
			t.Fatal("child-B1 agent mapping should still exist")
		}
		if _, ok := c.subagentInstances["parent-B"]; !ok {
			t.Fatal("parent-B subagentInstances should still exist")
		}
	})

	t.Run("destroy_session_clears_children_via_callback", func(t *testing.T) {
		c := newTestClient()
		parent := newSubagentTestSession("parent-1", nil, nil)
		c.sessions["parent-1"] = parent
		c.childToParent["child-1"] = "parent-1"
		c.childToAgent["child-1"] = "test-agent"
		c.subagentInstances["parent-1"] = map[string]*subagentInstance{
			"tc-1": {agentName: "test-agent"},
		}

		// Set up onDestroy callback (mirrors Client's real onDestroy which only clears child mappings)
		parent.onDestroy = func() {
			c.sessionsMux.Lock()
			defer c.sessionsMux.Unlock()
			c.removeChildMappingsForParentLocked("parent-1")
		}

		// Call onDestroy
		parent.onDestroy()

		if len(c.childToParent) != 0 {
			t.Fatal("childToParent should be cleared by onDestroy")
		}
		if len(c.childToAgent) != 0 {
			t.Fatal("childToAgent should be cleared by onDestroy")
		}
		if len(c.subagentInstances) != 0 {
			t.Fatal("subagentInstances should be cleared by onDestroy")
		}
		// Session itself is NOT removed by onDestroy (that's Destroy()'s job via RPC)
		if _, ok := c.sessions["parent-1"]; !ok {
			t.Fatal("session should still exist after onDestroy (only child mappings cleared)")
		}
	})
}

// ---------------------------------------------------------------------------
// TestSessionIsolation
// ---------------------------------------------------------------------------

func TestSessionIsolation(t *testing.T) {
	t.Run("child_cannot_reach_other_parent", func(t *testing.T) {
		c := newTestClient()
		parentA := newSubagentTestSession("parent-A", nil, nil)
		parentB := newSubagentTestSession("parent-B", nil, nil)
		c.sessions["parent-A"] = parentA
		c.sessions["parent-B"] = parentB
		c.childToParent["child-A"] = "parent-A"

		session, isChild, err := c.resolveSession("child-A")
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if !isChild {
			t.Fatal("expected isChild=true")
		}
		if session != parentA {
			t.Fatal("child-A should resolve to parent-A, not parent-B")
		}
		if session == parentB {
			t.Fatal("child-A must not resolve to parent-B")
		}
	})

	t.Run("child_session_id_immutable_mapping", func(t *testing.T) {
		c := newTestClient()
		parentA := newSubagentTestSession("parent-A", nil, nil)
		c.sessions["parent-A"] = parentA
		c.childToParent["child-1"] = "parent-A"

		// Resolve multiple times — always gets parent-A
		for i := 0; i < 5; i++ {
			session, isChild, err := c.resolveSession("child-1")
			if err != nil {
				t.Fatalf("iteration %d: unexpected error: %v", i, err)
			}
			if !isChild {
				t.Fatalf("iteration %d: expected isChild=true", i)
			}
			if session != parentA {
				t.Fatalf("iteration %d: mapping should consistently resolve to parent-A", i)
			}
		}
	})
}

// ---------------------------------------------------------------------------
// TestConcurrency
// ---------------------------------------------------------------------------

func TestConcurrency(t *testing.T) {
	t.Run("concurrent_resolve_session_safe", func(t *testing.T) {
		c := newTestClient()
		parent := newSubagentTestSession("parent", nil, nil)
		c.sessions["parent"] = parent
		c.childToParent["child-1"] = "parent"
		c.childToAgent["child-1"] = "agent"

		var wg sync.WaitGroup
		for i := 0; i < 100; i++ {
			wg.Add(1)
			go func() {
				defer wg.Done()
				c.resolveSession("parent")
				c.resolveSession("child-1")
				c.resolveSession("nonexistent")
			}()
		}
		wg.Wait()
	})

	t.Run("concurrent_subagent_events_safe", func(t *testing.T) {
		c := newTestClient()
		parent := newSubagentTestSession("parent", nil, nil)
		c.sessions["parent"] = parent

		var wg sync.WaitGroup
		for i := 0; i < 100; i++ {
			wg.Add(1)
			go func(idx int) {
				defer wg.Done()
				tcID := "tc-" + strings.Repeat("x", idx%10)
				childID := "child-" + strings.Repeat("x", idx%10)
				event := SessionEvent{
					Type:      SessionEventTypeSubagentStarted,
					Timestamp: time.Now(),
					Data: Data{
						ToolCallID:      strPtr(tcID),
						AgentName:       strPtr("agent"),
						RemoteSessionID: strPtr(childID),
					},
				}
				c.handleSubagentEvent("parent", event)

				// Also try resolving concurrently
				c.resolveSession(childID)
				c.isToolAllowedForChild(childID, "some_tool")

				endEvent := SessionEvent{
					Type:      SessionEventTypeSubagentCompleted,
					Timestamp: time.Now(),
					Data: Data{
						ToolCallID: strPtr(tcID),
					},
				}
				c.handleSubagentEvent("parent", endEvent)
			}(i)
		}
		wg.Wait()
	})
}

// newTestSessionWithRPC creates a test session with a buffered eventCh and a
// stub RPC layer whose underlying process is already "exited". Any RPC call
// (e.g. denyToolCallBroadcast) returns immediately with an error instead of
// blocking, which is safe because callers ignore the error.
func newTestSessionWithRPC(id string, agents []CustomAgentConfig) *Session {
	s := &Session{
		SessionID:    id,
		toolHandlers: make(map[string]ToolHandler),
		customAgents: agents,
		eventCh:      make(chan SessionEvent, 10),
	}
	pr, pw := io.Pipe()
	rpcClient := jsonrpc2.NewClient(pw, pr)
	done := make(chan struct{})
	close(done)
	rpcClient.SetProcessDone(done, nil)
	s.RPC = rpc.NewSessionRpc(rpcClient, id)
	return s
}

// ---------------------------------------------------------------------------
// TestV3BroadcastAllowlistEnforcement
// ---------------------------------------------------------------------------

func TestV3BroadcastAllowlistEnforcement(t *testing.T) {
	t.Run("denied tool for child session does not dispatch", func(t *testing.T) {
		c := newTestClient()
		agents := []CustomAgentConfig{{Name: "test-agent", Tools: []string{"allowed_tool"}}}
		parent := newTestSessionWithRPC("parent-1", agents)
		c.sessions["parent-1"] = parent

		// Register child via onSubagentStarted
		startEvent := SessionEvent{
			Type:      SessionEventTypeSubagentStarted,
			Timestamp: time.Now(),
			Data: Data{
				ToolCallID:      strPtr("tc-1"),
				AgentName:       strPtr("test-agent"),
				RemoteSessionID: strPtr("child-1"),
			},
		}
		c.onSubagentStarted("parent-1", startEvent)

		// Invoke handleSessionEvent with a denied tool
		c.handleSessionEvent(sessionEventRequest{
			SessionID: "parent-1",
			Event: SessionEvent{
				Type: SessionEventTypeExternalToolRequested,
				Data: Data{
					SessionID: strPtr("child-1"),
					ToolName:  strPtr("denied_tool"),
					RequestID: strPtr("req-1"),
				},
			},
		})

		// eventCh should be empty — dispatchEvent was never called
		select {
		case ev := <-parent.eventCh:
			t.Fatalf("expected no dispatched event, got %v", ev.Type)
		default:
		}
	})

	t.Run("allowed tool for child session dispatches normally", func(t *testing.T) {
		c := newTestClient()
		agents := []CustomAgentConfig{{Name: "test-agent", Tools: []string{"allowed_tool"}}}
		// No tool handlers registered — broadcast goroutine finds no handler and returns.
		parent := newTestSessionWithRPC("parent-1", agents)
		c.sessions["parent-1"] = parent

		startEvent := SessionEvent{
			Type:      SessionEventTypeSubagentStarted,
			Timestamp: time.Now(),
			Data: Data{
				ToolCallID:      strPtr("tc-1"),
				AgentName:       strPtr("test-agent"),
				RemoteSessionID: strPtr("child-1"),
			},
		}
		c.onSubagentStarted("parent-1", startEvent)

		c.handleSessionEvent(sessionEventRequest{
			SessionID: "parent-1",
			Event: SessionEvent{
				Type: SessionEventTypeExternalToolRequested,
				Data: Data{
					SessionID: strPtr("child-1"),
					ToolName:  strPtr("allowed_tool"),
					RequestID: strPtr("req-2"),
				},
			},
		})

		select {
		case ev := <-parent.eventCh:
			if ev.Type != SessionEventTypeExternalToolRequested {
				t.Fatalf("expected external_tool.requested, got %v", ev.Type)
			}
		case <-time.After(time.Second):
			t.Fatal("expected event to be dispatched but eventCh was empty")
		}
	})

	t.Run("parent session tool event always dispatches", func(t *testing.T) {
		c := newTestClient()
		agents := []CustomAgentConfig{{Name: "test-agent", Tools: []string{"allowed_tool"}}}
		parent := newTestSessionWithRPC("parent-1", agents)
		c.sessions["parent-1"] = parent

		// Register a child so the child-to-parent map is populated, but use the
		// parent's session ID in the event so the request is NOT treated as a child.
		startEvent := SessionEvent{
			Type:      SessionEventTypeSubagentStarted,
			Timestamp: time.Now(),
			Data: Data{
				ToolCallID:      strPtr("tc-1"),
				AgentName:       strPtr("test-agent"),
				RemoteSessionID: strPtr("child-1"),
			},
		}
		c.onSubagentStarted("parent-1", startEvent)

		// Event with SessionID = parent (not a child) — should always dispatch.
		c.handleSessionEvent(sessionEventRequest{
			SessionID: "parent-1",
			Event: SessionEvent{
				Type: SessionEventTypeExternalToolRequested,
				Data: Data{
					SessionID: strPtr("parent-1"),
					ToolName:  strPtr("denied_tool"),
					RequestID: strPtr("req-3"),
				},
			},
		})

		select {
		case ev := <-parent.eventCh:
			if ev.Type != SessionEventTypeExternalToolRequested {
				t.Fatalf("expected external_tool.requested, got %v", ev.Type)
			}
		case <-time.After(time.Second):
			t.Fatal("expected event to be dispatched for parent session but eventCh was empty")
		}
	})
}

// ---------------------------------------------------------------------------
// TestToolAllowlist_EmptyToolsList
// ---------------------------------------------------------------------------

func TestToolAllowlist_EmptyToolsList(t *testing.T) {
	t.Run("agent with empty Tools denies all tools", func(t *testing.T) {
		c := newTestClient()
		agents := []CustomAgentConfig{{Name: "test-agent", Tools: []string{}}}
		parent := newSubagentTestSession("parent-1", nil, agents)
		c.sessions["parent-1"] = parent
		c.childToParent["child-1"] = "parent-1"
		c.childToAgent["child-1"] = "test-agent"

		for _, tool := range []string{"allowed_tool", "any_tool", "save_output", ""} {
			if c.isToolAllowedForChild("child-1", tool) {
				t.Fatalf("empty Tools list should deny %q", tool)
			}
		}
	})
}

// ---------------------------------------------------------------------------
// TestEnrichAgentToolDefinitions
// ---------------------------------------------------------------------------

func TestEnrichAgentToolDefinitions(t *testing.T) {
	t.Run("populates definitions for matching tools", func(t *testing.T) {
		sessionTools := []Tool{
			{Name: "tool_a", Description: "desc_a", Parameters: map[string]any{"type": "object"}, Handler: testToolHandler},
			{Name: "tool_b", Description: "desc_b", Parameters: map[string]any{"type": "string"}, Handler: testToolHandler},
		}
		agents := []CustomAgentConfig{
			{Name: "agent1", Tools: []string{"tool_a"}},
		}

		enrichAgentToolDefinitions(agents, sessionTools)

		if len(agents[0].ToolDefinitions) != 1 {
			t.Fatalf("expected 1 tool definition, got %d", len(agents[0].ToolDefinitions))
		}
		def := agents[0].ToolDefinitions[0]
		if def.Name != "tool_a" {
			t.Errorf("expected Name=tool_a, got %s", def.Name)
		}
		if def.Description != "desc_a" {
			t.Errorf("expected Description=desc_a, got %s", def.Description)
		}
		if def.Parameters["type"] != "object" {
			t.Errorf("expected Parameters[type]=object, got %v", def.Parameters["type"])
		}
		if def.Handler != nil {
			t.Error("Handler should not be copied into ToolDefinitions")
		}

		// Verify Handler is excluded from wire format via json:"-"
		data, err := json.Marshal(def)
		if err != nil {
			t.Fatalf("json.Marshal failed: %v", err)
		}
		if strings.Contains(string(data), "handler") {
			t.Errorf("wire format should not contain handler field, got: %s", data)
		}
	})

	t.Run("skips agents with nil tools", func(t *testing.T) {
		sessionTools := []Tool{
			{Name: "tool_a", Description: "desc_a", Handler: testToolHandler},
		}
		agents := []CustomAgentConfig{
			{Name: "agent1", Tools: nil},
		}

		enrichAgentToolDefinitions(agents, sessionTools)

		if agents[0].ToolDefinitions != nil {
			t.Errorf("expected nil ToolDefinitions for nil Tools, got %v", agents[0].ToolDefinitions)
		}
	})

	t.Run("skips agents with empty tools list", func(t *testing.T) {
		sessionTools := []Tool{
			{Name: "tool_a", Description: "desc_a", Handler: testToolHandler},
		}
		agents := []CustomAgentConfig{
			{Name: "agent1", Tools: []string{}},
		}

		enrichAgentToolDefinitions(agents, sessionTools)

		if agents[0].ToolDefinitions != nil {
			t.Errorf("expected nil ToolDefinitions for empty Tools, got %v", agents[0].ToolDefinitions)
		}
	})

	t.Run("handles missing tool names gracefully", func(t *testing.T) {
		sessionTools := []Tool{
			{Name: "tool_a", Description: "desc_a", Handler: testToolHandler},
		}
		agents := []CustomAgentConfig{
			{Name: "agent1", Tools: []string{"nonexistent_tool"}},
		}

		enrichAgentToolDefinitions(agents, sessionTools)

		if agents[0].ToolDefinitions != nil {
			t.Errorf("expected nil ToolDefinitions when no tools match, got %v", agents[0].ToolDefinitions)
		}
	})

	t.Run("handles multiple agents independently", func(t *testing.T) {
		sessionTools := []Tool{
			{Name: "tool_a", Description: "desc_a", Parameters: map[string]any{"a": 1}, Handler: testToolHandler},
			{Name: "tool_b", Description: "desc_b", Parameters: map[string]any{"b": 2}, Handler: testToolHandler},
			{Name: "tool_c", Description: "desc_c", Parameters: map[string]any{"c": 3}, Handler: testToolHandler},
		}
		agents := []CustomAgentConfig{
			{Name: "agent1", Tools: []string{"tool_a", "tool_b"}},
			{Name: "agent2", Tools: []string{"tool_c"}},
			{Name: "agent3", Tools: nil},
		}

		enrichAgentToolDefinitions(agents, sessionTools)

		// agent1: should have tool_a and tool_b
		if len(agents[0].ToolDefinitions) != 2 {
			t.Fatalf("agent1: expected 2 tool definitions, got %d", len(agents[0].ToolDefinitions))
		}
		if agents[0].ToolDefinitions[0].Name != "tool_a" {
			t.Errorf("agent1: expected first def=tool_a, got %s", agents[0].ToolDefinitions[0].Name)
		}
		if agents[0].ToolDefinitions[1].Name != "tool_b" {
			t.Errorf("agent1: expected second def=tool_b, got %s", agents[0].ToolDefinitions[1].Name)
		}

		// agent2: should have tool_c
		if len(agents[1].ToolDefinitions) != 1 {
			t.Fatalf("agent2: expected 1 tool definition, got %d", len(agents[1].ToolDefinitions))
		}
		if agents[1].ToolDefinitions[0].Name != "tool_c" {
			t.Errorf("agent2: expected def=tool_c, got %s", agents[1].ToolDefinitions[0].Name)
		}

		// agent3: nil Tools → should remain nil
		if agents[2].ToolDefinitions != nil {
			t.Errorf("agent3: expected nil ToolDefinitions for nil Tools, got %v", agents[2].ToolDefinitions)
		}
	})

	t.Run("does not mutate caller config", func(t *testing.T) {
		sessionTools := []Tool{
			{Name: "tool_a", Description: "desc_a", Parameters: map[string]any{"type": "object"}, Handler: testToolHandler},
		}
		originalAgents := []CustomAgentConfig{
			{Name: "agent1", Tools: []string{"tool_a"}},
		}

		// Simulate the copy-before-enrich pattern used in CreateSession
		copied := make([]CustomAgentConfig, len(originalAgents))
		copy(copied, originalAgents)
		enrichAgentToolDefinitions(copied, sessionTools)

		// The copy should have definitions
		if len(copied[0].ToolDefinitions) != 1 {
			t.Fatalf("copied agent should have 1 tool definition, got %d", len(copied[0].ToolDefinitions))
		}

		// The original should remain untouched
		if originalAgents[0].ToolDefinitions != nil {
			t.Errorf("original agent ToolDefinitions should still be nil, got %v", originalAgents[0].ToolDefinitions)
		}
	})
}
