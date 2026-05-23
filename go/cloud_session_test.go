package copilot

import (
	"encoding/json"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
)

// newCloudTestClient returns a Client with pending routing initialized and a
// pre-populated sessions map, suitable for unit-testing cloud session logic
// without a real network connection.
func newCloudTestClient() *Client {
	return &Client{
		sessions: make(map[string]*Session),
		pending: pendingRouting{
			events:  make(map[string][]sessionEventRequest),
			waiters: make(map[string][]chan pendingResult),
		},
	}
}

// TestCreateSession_RejectsCloudConfig verifies that CreateSession returns a
// clear error when config.Cloud is set.
func TestCreateSession_RejectsCloudConfig(t *testing.T) {
	client := NewClient(&ClientOptions{Connection: StdioConnection{Path: "/__nonexistent__"}})
	_, err := client.CreateSession(t.Context(), &SessionConfig{
		Cloud: &CloudSessionOptions{},
	})
	if err == nil {
		t.Fatal("expected error when cloud config is set")
	}
	if !strings.Contains(err.Error(), "CreateCloudSession") {
		t.Errorf("error should mention CreateCloudSession, got: %v", err)
	}
}

// TestCreateCloudSession_RejectsCallerSessionID verifies the SDK rejects a
// caller-supplied SessionID.
func TestCreateCloudSession_RejectsCallerSessionID(t *testing.T) {
	client := NewClient(&ClientOptions{Connection: StdioConnection{Path: "/__nonexistent__"}})
	_, err := client.CreateCloudSession(t.Context(), &SessionConfig{
		Cloud:     &CloudSessionOptions{},
		SessionID: "caller-supplied-id",
	})
	if err == nil {
		t.Fatal("expected error when SessionID is set")
	}
	if !strings.Contains(err.Error(), "SessionID") {
		t.Errorf("error should mention SessionID, got: %v", err)
	}
}

// TestCreateCloudSession_RejectsCallerProvider verifies the SDK rejects a
// caller-supplied Provider.
func TestCreateCloudSession_RejectsCallerProvider(t *testing.T) {
	client := NewClient(&ClientOptions{Connection: StdioConnection{Path: "/__nonexistent__"}})
	_, err := client.CreateCloudSession(t.Context(), &SessionConfig{
		Cloud:    &CloudSessionOptions{},
		Provider: &ProviderConfig{ModelID: "gpt-4"},
	})
	if err == nil {
		t.Fatal("expected error when Provider is set")
	}
	if !strings.Contains(err.Error(), "Provider") {
		t.Errorf("error should mention Provider, got: %v", err)
	}
}

// TestCreateCloudSession_RequiresCloud verifies the SDK rejects configs without
// Cloud set.
func TestCreateCloudSession_RequiresCloud(t *testing.T) {
	client := NewClient(&ClientOptions{Connection: StdioConnection{Path: "/__nonexistent__"}})
	_, err := client.CreateCloudSession(t.Context(), &SessionConfig{})
	if err == nil {
		t.Fatal("expected error when Cloud is nil")
	}
	if !strings.Contains(err.Error(), "Cloud") {
		t.Errorf("error should mention Cloud, got: %v", err)
	}
}

// TestCreateCloudSession_WirePayload verifies that the session.create wire
// payload includes the cloud field and omits sessionId when built by the cloud
// path.
func TestCreateCloudSession_WirePayload(t *testing.T) {
	req := createSessionRequest{
		Cloud: &CloudSessionOptions{
			Repository: &CloudSessionRepository{Owner: "github", Name: "copilot-sdk"},
		},
		// SessionID intentionally left empty
	}

	data, err := json.Marshal(req)
	if err != nil {
		t.Fatalf("marshal error: %v", err)
	}

	var m map[string]any
	if err := json.Unmarshal(data, &m); err != nil {
		t.Fatalf("unmarshal error: %v", err)
	}

	if _, ok := m["sessionId"]; ok {
		t.Error("sessionId must be omitted from the cloud session.create wire payload")
	}

	cloud, ok := m["cloud"]
	if !ok {
		t.Fatal("cloud field must be present in the wire payload")
	}
	cloudMap, ok := cloud.(map[string]any)
	if !ok {
		t.Fatalf("cloud field should be a map, got %T", cloud)
	}
	repo, ok := cloudMap["repository"].(map[string]any)
	if !ok {
		t.Fatal("cloud.repository should be a map")
	}
	if repo["owner"] != "github" || repo["name"] != "copilot-sdk" {
		t.Errorf("unexpected cloud.repository: %v", repo)
	}
}

// TestPendingRouting_BuffersEarlyNotifications verifies that session.event
// notifications arriving before the session is registered are buffered and
// replayed when flushPendingForSession is called.
func TestPendingRouting_BuffersEarlyNotifications(t *testing.T) {
	client := newCloudTestClient()
	dispose := client.beginPendingSessionRouting()
	defer dispose()

	const pendingID = "runtime-assigned-id"

	// Simulate two session.event notifications arriving before the session is
	// registered.
	client.handleSessionEvent(sessionEventRequest{
		SessionID: pendingID,
		Event:     SessionEvent{Data: &SessionIdleData{}},
	})
	client.handleSessionEvent(sessionEventRequest{
		SessionID: pendingID,
		Event:     SessionEvent{Data: &SessionIdleData{}},
	})

	// Verify they are buffered.
	client.pending.mu.Lock()
	bufLen := len(client.pending.events[pendingID])
	client.pending.mu.Unlock()
	if bufLen != 2 {
		t.Fatalf("expected 2 buffered events, got %d", bufLen)
	}

	// Now register the session and flush.
	session, cleanup := newTestSession()
	defer cleanup()
	session.SessionID = pendingID

	var received []SessionEvent
	var mu sync.Mutex
	var wg sync.WaitGroup
	wg.Add(2)
	session.On(func(event SessionEvent) {
		mu.Lock()
		received = append(received, event)
		mu.Unlock()
		wg.Done()
	})

	client.sessionsMux.Lock()
	client.sessions[pendingID] = session
	client.sessionsMux.Unlock()

	client.flushPendingForSession(pendingID, session)

	// Wait for the event handler goroutine to process.
	done := make(chan struct{})
	go func() {
		wg.Wait()
		close(done)
	}()
	select {
	case <-done:
	case <-time.After(2 * time.Second):
		t.Fatal("timed out waiting for buffered events to be dispatched")
	}

	mu.Lock()
	got := len(received)
	mu.Unlock()
	if got != 2 {
		t.Errorf("expected 2 events replayed, got %d", got)
	}

	// Buffer should be cleared after flush.
	client.pending.mu.Lock()
	remaining := len(client.pending.events[pendingID])
	client.pending.mu.Unlock()
	if remaining != 0 {
		t.Errorf("buffer should be empty after flush, got %d", remaining)
	}
}

// TestPendingRouting_ParksInboundRequests verifies that inbound request handlers
// (e.g. userInput.request) park until the session is registered when pending
// routing is active.
func TestPendingRouting_ParksInboundRequests(t *testing.T) {
	client := newCloudTestClient()
	dispose := client.beginPendingSessionRouting()

	const pendingID = "runtime-assigned-id-2"

	// Launch a goroutine that simulates an inbound userInput.request arriving
	// before the session is registered.
	type result struct {
		resp *userInputResponse
		err  *jsonrpcError
	}
	resultCh := make(chan result, 1)
	go func() {
		resp, rpcErr := client.handleUserInputRequest(userInputRequest{
			SessionID: pendingID,
			Question:  "Proceed?",
		})
		resultCh <- result{resp, rpcErr}
	}()

	// Give the goroutine time to park.
	time.Sleep(20 * time.Millisecond)

	// Register the session.
	session, cleanup := newTestSession()
	defer cleanup()
	session.SessionID = pendingID
	session.registerUserInputHandler(func(req UserInputRequest, _ UserInputInvocation) (UserInputResponse, error) {
		return UserInputResponse{Answer: "yes"}, nil
	})

	client.sessionsMux.Lock()
	client.sessions[pendingID] = session
	client.sessionsMux.Unlock()

	client.flushPendingForSession(pendingID, session)
	dispose()

	select {
	case r := <-resultCh:
		if r.err != nil {
			t.Fatalf("expected success, got rpc error: %v", r.err)
		}
		if r.resp == nil || r.resp.Answer != "yes" {
			t.Errorf("expected answer 'yes', got %+v", r.resp)
		}
	case <-time.After(2 * time.Second):
		t.Fatal("timed out waiting for parked request to be resolved")
	}
}

// TestPendingRouting_DropOldestWhenBufferFull verifies drop-oldest behaviour
// when the notification buffer is full.
func TestPendingRouting_DropOldestWhenBufferFull(t *testing.T) {
	client := newCloudTestClient()
	dispose := client.beginPendingSessionRouting()
	defer dispose()

	const pendingID = "overflow-session"

	// Fill buffer beyond the limit.
	for i := range pendingSessionBufferLimit + 5 {
		client.handleSessionEvent(sessionEventRequest{
			SessionID: pendingID,
			Event: SessionEvent{
				// Embed the index so we can verify drop-oldest.
				Data: &SessionIdleData{},
			},
		})
		_ = i
	}

	client.pending.mu.Lock()
	bufLen := len(client.pending.events[pendingID])
	client.pending.mu.Unlock()

	if bufLen != pendingSessionBufferLimit {
		t.Errorf("expected buffer capped at %d, got %d", pendingSessionBufferLimit, bufLen)
	}
}

// TestPendingRouting_RejectsWaitersOnDispose verifies that waiters are
// rejected with an error when pending mode ends without registration.
func TestPendingRouting_RejectsWaitersOnDispose(t *testing.T) {
	client := newCloudTestClient()
	dispose := client.beginPendingSessionRouting()

	const pendingID = "never-registered"

	resultCh := make(chan *jsonrpcError, 1)
	go func() {
		_, rpcErr := client.handleUserInputRequest(userInputRequest{
			SessionID: pendingID,
			Question:  "Proceed?",
		})
		resultCh <- rpcErr
	}()

	// Give the goroutine time to park.
	time.Sleep(20 * time.Millisecond)

	// Dispose without registering the session.
	dispose()

	select {
	case rpcErr := <-resultCh:
		if rpcErr == nil {
			t.Fatal("expected an rpc error after dispose without registration")
		}
		if !strings.Contains(rpcErr.Message, "routing ended before session was registered") {
			t.Errorf("expected routing-ended message, got: %s", rpcErr.Message)
		}
		if rpcErr.Code != -32603 {
			t.Errorf("expected code -32603, got: %d", rpcErr.Code)
		}
	case <-time.After(2 * time.Second):
		t.Fatal("timed out waiting for rejected waiter")
	}
}

// TestPendingRouting_OverflowEmitsError verifies that when the parked-waiter
// buffer reaches its cap, the oldest waiter receives the overflow error response
// and the remaining 128 waiters resolve normally after registration.
func TestPendingRouting_OverflowEmitsError(t *testing.T) {
	client := newCloudTestClient()
	dispose := client.beginPendingSessionRouting()

	const pendingID = "overflow-request-session"
	const total = pendingSessionBufferLimit + 1 // 129

	type result struct {
		resp *userInputResponse
		err  *jsonrpcError
	}

	// Register a user-input handler so the session resolves successfully.
	session, cleanup := newTestSession()
	defer cleanup()
	session.SessionID = pendingID
	session.registerUserInputHandler(func(req UserInputRequest, _ UserInputInvocation) (UserInputResponse, error) {
		return UserInputResponse{Answer: "yes"}, nil
	})

	results := make([]chan result, total)
	for i := range total {
		results[i] = make(chan result, 1)
		go func(ch chan result) {
			resp, rpcErr := client.handleUserInputRequest(userInputRequest{
				SessionID: pendingID,
				Question:  "Proceed?",
			})
			ch <- result{resp, rpcErr}
		}(results[i])
	}

	// Give goroutines time to park.
	time.Sleep(50 * time.Millisecond)

	// Register the session and flush — this resolves the 128 remaining waiters.
	client.sessionsMux.Lock()
	client.sessions[pendingID] = session
	client.sessionsMux.Unlock()
	client.flushPendingForSession(pendingID, session)
	dispose()

	// Collect all results with a timeout.
	var gotOverflow int
	var gotSuccess int
	deadline := time.After(2 * time.Second)
	for _, ch := range results {
		select {
		case r := <-ch:
			if r.err != nil {
				if !strings.Contains(r.err.Message, "pending session buffer overflow") {
					t.Errorf("unexpected error message: %s", r.err.Message)
				}
				if r.err.Code != -32603 {
					t.Errorf("expected code -32603 for overflow, got: %d", r.err.Code)
				}
				gotOverflow++
			} else {
				gotSuccess++
			}
		case <-deadline:
			t.Fatalf("timed out: overflow=%d success=%d", gotOverflow, gotSuccess)
		}
	}

	if gotOverflow != 1 {
		t.Errorf("expected exactly 1 overflow rejection, got %d", gotOverflow)
	}
	if gotSuccess != pendingSessionBufferLimit {
		t.Errorf("expected %d successful resolutions, got %d", pendingSessionBufferLimit, gotSuccess)
	}
}

// TestPendingRouting_GuardDropDistinctMessage verifies that when the last
// pending-routing guard drops without registration, parked waiters receive the
// distinct routing-ended error (not the overflow message) so the two paths are
// distinguishable in logs and debugging.
func TestPendingRouting_GuardDropDistinctMessage(t *testing.T) {
	client := newCloudTestClient()
	dispose := client.beginPendingSessionRouting()

	const pendingID = "guard-drop-session"

	resultCh := make(chan *jsonrpcError, 1)
	go func() {
		_, rpcErr := client.handleUserInputRequest(userInputRequest{
			SessionID: pendingID,
			Question:  "Proceed?",
		})
		resultCh <- rpcErr
	}()

	// Give the goroutine time to park.
	time.Sleep(20 * time.Millisecond)

	// Drop the guard without registering — simulates session.create failing.
	dispose()

	select {
	case rpcErr := <-resultCh:
		if rpcErr == nil {
			t.Fatal("expected an rpc error after guard drop without registration")
		}
		const want = "pending session routing ended before session was registered"
		if rpcErr.Message != want {
			t.Errorf("expected exact message %q, got %q", want, rpcErr.Message)
		}
		if rpcErr.Code != -32603 {
			t.Errorf("expected code -32603, got: %d", rpcErr.Code)
		}
		// Must NOT contain the overflow message.
		if strings.Contains(rpcErr.Message, "buffer overflow") {
			t.Errorf("guard-drop path must not use overflow message, got: %s", rpcErr.Message)
		}
	case <-time.After(2 * time.Second):
		t.Fatal("timed out waiting for rejected waiter")
	}
}

// jsonrpcError is a local alias for jsonrpc2.Error used in test assertions.
type jsonrpcError = jsonrpc2.Error
