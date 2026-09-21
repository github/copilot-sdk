// Copyright (c) Microsoft Corporation. All rights reserved.

package e2e

import (
	"bufio"
	"encoding/json"
	"fmt"
	"io"
	"net"
	"strconv"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
)

func TestScenarioTestingCloudE2E(t *testing.T) {
	t.Run("notifies steerability before first send", func(t *testing.T) {
		var mu sync.Mutex
		var events []copilot.SessionEvent
		messageID := "message-1"
		fixture := newGeneratedRPCFixture(t, t.Context())
		fixture.server.SetRequestHandler("session.remote.notifySteerableChanged", func(_ json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			mu.Lock()
			events = append(events, scenarioEvent("remote-1", &copilot.SessionRemoteSteerableChangedData{
				RemoteSteerable: true,
			}))
			mu.Unlock()
			return json.RawMessage(`{}`), nil
		})
		fixture.server.SetRequestHandler("session.send", func(req json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			var params struct {
				Prompt string `json:"prompt"`
			}
			if err := json.Unmarshal(req, &params); err != nil {
				t.Errorf("unmarshal session.send: %v", err)
			}
			mu.Lock()
			events = append(events, scenarioEvent("message-1", &copilot.UserMessageData{
				Content:            params.Prompt,
				MessageID:          &messageID,
				TransformedContent: &params.Prompt,
			}))
			mu.Unlock()
			return mustJSON(t, map[string]any{"messageId": messageID}), nil
		})
		fixture.server.SetRequestHandler("session.getMessages", func(_ json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			mu.Lock()
			defer mu.Unlock()
			return mustJSON(t, map[string]any{"events": append([]copilot.SessionEvent(nil), events...)}), nil
		})

		session := fixture.session

		if _, err := session.RPC.Remote.NotifySteerableChanged(t.Context(), &rpc.RemoteNotifySteerableChangedRequest{
			RemoteSteerable: true,
		}); err != nil {
			t.Fatalf("NotifySteerableChanged failed: %v", err)
		}
		const prompt = "SCENARIO_STEERABLE_FIRST_SEND"
		if _, err := session.Send(t.Context(), copilot.MessageOptions{Prompt: prompt}); err != nil {
			t.Fatalf("Send failed: %v", err)
		}

		got, err := session.GetEvents(t.Context())
		if err != nil {
			t.Fatalf("GetEvents failed: %v", err)
		}
		if len(got) != 2 {
			t.Fatalf("Expected two persisted events, got %d: %#v", len(got), got)
		}
		remote, ok := got[0].Data.(*copilot.SessionRemoteSteerableChangedData)
		if !ok || !remote.RemoteSteerable {
			t.Fatalf("Expected first event to persist remote steerability, got %#v", got[0].Data)
		}
		message, ok := got[1].Data.(*copilot.UserMessageData)
		if !ok || message.TransformedContent == nil || !strings.Contains(*message.TransformedContent, prompt) {
			t.Fatalf("Expected second event to contain first message, got %#v", got[1].Data)
		}
	})

	t.Run("routes first event for server assigned session id", func(t *testing.T) {
		fixture := newAssignedCloudSessionFixture(t)
		defer fixture.Close()

		client := copilot.NewClient(&copilot.ClientOptions{
			Connection: copilot.URIConnection{URL: fixture.URL()},
		})
		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Start failed: %v", err)
		}
		defer client.Stop()

		firstEvent := make(chan copilot.SessionEvent, 1)
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			Cloud: &copilot.CloudSessionOptions{
				Repository: &copilot.CloudSessionRepository{
					Owner:  "github",
					Name:   "copilot-sdk",
					Branch: "main",
				},
			},
			OnEvent: func(event copilot.SessionEvent) {
				select {
				case firstEvent <- event:
				default:
				}
			},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		defer session.Disconnect()

		if session.SessionID != "server-assigned-cloud-session" {
			t.Fatalf("Expected server-assigned id, got %q", session.SessionID)
		}
		select {
		case event := <-firstEvent:
			start, ok := event.Data.(*copilot.SessionStartData)
			if !ok {
				t.Fatalf("Expected session.start event, got %T", event.Data)
			}
			if start.SessionID != session.SessionID {
				t.Fatalf("Expected event session id %q, got %q", session.SessionID, start.SessionID)
			}
		case <-t.Context().Done():
			t.Fatal("Test context ended before first cloud event was routed")
		}

		create := fixture.CreateRequest()
		if _, ok := create["sessionId"]; ok {
			t.Fatalf("Cloud session.create unexpectedly sent sessionId: %#v", create)
		}
		cloud, ok := create["cloud"].(map[string]any)
		if !ok {
			t.Fatalf("Expected cloud request object, got %#v", create["cloud"])
		}
		repository, ok := cloud["repository"].(map[string]any)
		if !ok || repository["owner"] != "github" || repository["name"] != "copilot-sdk" || repository["branch"] != "main" {
			t.Fatalf("Unexpected cloud repository: %#v", cloud["repository"])
		}
	})

	t.Run("resumes using runtime id returned by cloud connect", func(t *testing.T) {
		var mu sync.Mutex
		var calls []capturedRPCRequest
		fixture := newGeneratedRPCFixture(t, t.Context())
		setHandler := func(method string, result any) {
			fixture.server.SetRequestHandler(method, func(req json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				mu.Lock()
				calls = append(calls, capturedRPCRequest{Method: method, Request: append(json.RawMessage(nil), req...)})
				mu.Unlock()
				return mustJSON(t, result), nil
			})
		}
		setHandler("sessions.connect", remoteSessionConnection("github/copilot-sdk#123"))
		setHandler("session.resume", map[string]any{"sessionId": "runtime-session-id", "workspacePath": "C:\\workspace"})
		client := fixture.client

		connection, err := client.RPC.Sessions.Connect(t.Context(), &rpc.ConnectRemoteSessionParams{
			SessionID: "cloud-control-session",
		})
		if err != nil {
			t.Fatalf("Sessions.Connect failed: %v", err)
		}
		if connection.SessionID != "runtime-session-id" || connection.Metadata.SessionID != "runtime-session-id" {
			t.Fatalf("Unexpected runtime ids: %#v", connection)
		}
		if connection.Metadata.ResourceID == nil || *connection.Metadata.ResourceID != "github/copilot-sdk#123" {
			t.Fatalf("Unexpected resource id: %#v", connection.Metadata.ResourceID)
		}

		resumed, err := client.ResumeSession(t.Context(), connection.SessionID, nil)
		if err != nil {
			t.Fatalf("ResumeSession failed: %v", err)
		}
		defer resumed.Disconnect()
		if resumed.SessionID != connection.SessionID {
			t.Fatalf("Expected resumed id %q, got %q", connection.SessionID, resumed.SessionID)
		}

		mu.Lock()
		defer mu.Unlock()
		assertCapturedSessionID(t, calls, "sessions.connect", "cloud-control-session")
		assertCapturedSessionID(t, calls, "session.resume", "runtime-session-id")
	})

	t.Run("exposes cloud resource mismatch before resume", func(t *testing.T) {
		var mu sync.Mutex
		var methods []string
		fixture := newGeneratedRPCFixture(t, t.Context())
		fixture.server.SetRequestHandler("sessions.connect", func(_ json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			mu.Lock()
			methods = append(methods, "sessions.connect")
			mu.Unlock()
			return mustJSON(t, remoteSessionConnection("github/other-repository#456")), nil
		})
		fixture.server.SetRequestHandler("session.resume", func(_ json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			mu.Lock()
			methods = append(methods, "session.resume")
			mu.Unlock()
			return json.RawMessage(`{"sessionId":"runtime-session-id"}`), nil
		})
		client := fixture.client

		connection, err := client.RPC.Sessions.Connect(t.Context(), &rpc.ConnectRemoteSessionParams{
			SessionID: "cloud-control-session",
		})
		if err != nil {
			t.Fatalf("Sessions.Connect failed: %v", err)
		}
		if connection.Metadata.ResourceID == nil || *connection.Metadata.ResourceID == "github/copilot-sdk#123" {
			t.Fatalf("Expected mismatched resource id, got %#v", connection.Metadata.ResourceID)
		}

		mu.Lock()
		defer mu.Unlock()
		for _, method := range methods {
			if method == "session.resume" {
				t.Fatal("Resource mismatch must be exposed before session.resume")
			}
		}
	})
}

func mustJSON(t *testing.T, value any) json.RawMessage {
	t.Helper()
	result, err := json.Marshal(value)
	if err != nil {
		t.Fatalf("Marshal fixture result failed: %v", err)
	}
	return result
}

func scenarioEvent(id string, data copilot.SessionEventData) copilot.SessionEvent {
	return copilot.SessionEvent{
		Data:      data,
		ID:        id,
		Timestamp: time.Date(2026, 9, 17, 19, 0, 0, 0, time.UTC),
	}
}

func remoteSessionConnection(resourceID string) map[string]any {
	return map[string]any{
		"sessionId": "runtime-session-id",
		"metadata": map[string]any{
			"kind":         "coding-agent",
			"modifiedTime": "2026-09-17T20:00:00Z",
			"name":         "Cloud task",
			"repository": map[string]any{
				"branch": "main",
				"name":   "copilot-sdk",
				"owner":  "github",
			},
			"resourceId": resourceID,
			"sessionId":  "runtime-session-id",
			"startTime":  "2026-09-17T19:00:00Z",
			"state":      "active",
		},
	}
}

func assertCapturedSessionID(t *testing.T, calls []capturedRPCRequest, method, want string) {
	t.Helper()
	for _, call := range calls {
		if call.Method != method {
			continue
		}
		var params map[string]any
		if err := json.Unmarshal(call.Request, &params); err != nil {
			t.Fatalf("Unmarshal %s request failed: %v", method, err)
		}
		if got := params["sessionId"]; got != want {
			t.Fatalf("%s sessionId: got %#v, want %q", method, got, want)
		}
		return
	}
	t.Fatalf("Did not capture %s", method)
}

type capturedRPCRequest struct {
	Method  string
	Request json.RawMessage
}

type assignedCloudSessionFixture struct {
	t             *testing.T
	listener      net.Listener
	done          chan struct{}
	mu            sync.Mutex
	createRequest map[string]any
}

func newAssignedCloudSessionFixture(t *testing.T) *assignedCloudSessionFixture {
	t.Helper()
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatalf("Listen failed: %v", err)
	}
	f := &assignedCloudSessionFixture{
		t:        t,
		listener: listener,
		done:     make(chan struct{}),
	}
	go f.serve()
	return f
}

func (f *assignedCloudSessionFixture) URL() string {
	return "http://" + f.listener.Addr().String()
}

func (f *assignedCloudSessionFixture) CreateRequest() map[string]any {
	f.mu.Lock()
	defer f.mu.Unlock()
	result := make(map[string]any, len(f.createRequest))
	for key, value := range f.createRequest {
		result[key] = value
	}
	return result
}

func (f *assignedCloudSessionFixture) Close() {
	_ = f.listener.Close()
	<-f.done
}

func (f *assignedCloudSessionFixture) serve() {
	defer close(f.done)
	conn, err := f.listener.Accept()
	if err != nil {
		return
	}
	defer conn.Close()

	reader := bufio.NewReader(conn)
	for {
		body, err := readRPCFrame(reader)
		if err != nil {
			if err != io.EOF {
				f.t.Errorf("Read fake cloud RPC frame: %v", err)
			}
			return
		}
		var request struct {
			ID     json.RawMessage `json:"id"`
			Method string          `json:"method"`
			Params json.RawMessage `json:"params"`
		}
		if err := json.Unmarshal(body, &request); err != nil {
			f.t.Errorf("Unmarshal fake cloud request: %v", err)
			return
		}
		switch request.Method {
		case "connect":
			if err := writeRPCFrame(conn, map[string]any{
				"jsonrpc": "2.0",
				"id":      request.ID,
				"result": map[string]any{
					"ok":              true,
					"protocolVersion": 4,
					"version":         "fake",
				},
			}); err != nil {
				f.t.Errorf("Write connect response: %v", err)
				return
			}
		case "session.create":
			var params map[string]any
			if err := json.Unmarshal(request.Params, &params); err != nil {
				f.t.Errorf("Unmarshal session.create params: %v", err)
				return
			}
			f.mu.Lock()
			f.createRequest = params
			f.mu.Unlock()
			if err := writeRPCFrame(conn, map[string]any{
				"jsonrpc": "2.0",
				"id":      request.ID,
				"result": map[string]any{
					"sessionId":     "server-assigned-cloud-session",
					"workspacePath": "C:\\cloud-workspace",
				},
			}); err != nil {
				f.t.Errorf("Write session.create response: %v", err)
				return
			}
			event := scenarioEvent("start-1", &copilot.SessionStartData{
				CopilotVersion: "fake",
				Producer:       "scenario-test",
				SessionID:      "server-assigned-cloud-session",
				StartTime:      time.Date(2026, 9, 17, 19, 0, 0, 0, time.UTC),
				Version:        1,
			})
			if err := writeRPCFrame(conn, map[string]any{
				"jsonrpc": "2.0",
				"method":  "session.event",
				"params": map[string]any{
					"sessionId": "server-assigned-cloud-session",
					"event":     event,
				},
			}); err != nil {
				f.t.Errorf("Write first session event: %v", err)
				return
			}
		case "session.options.update", "session.detach":
			if err := writeRPCFrame(conn, map[string]any{
				"jsonrpc": "2.0",
				"id":      request.ID,
				"result":  map[string]any{},
			}); err != nil {
				f.t.Errorf("Write %s response: %v", request.Method, err)
				return
			}
		default:
			if len(request.ID) == 0 {
				continue
			}
			if err := writeRPCFrame(conn, map[string]any{
				"jsonrpc": "2.0",
				"id":      request.ID,
				"error": map[string]any{
					"code":    -32601,
					"message": "unexpected method " + request.Method,
				},
			}); err != nil {
				f.t.Errorf("Write error response: %v", err)
				return
			}
		}
	}
}

func readRPCFrame(reader *bufio.Reader) ([]byte, error) {
	contentLength := -1
	for {
		line, err := reader.ReadString('\n')
		if err != nil {
			return nil, err
		}
		line = strings.TrimSpace(line)
		if line == "" {
			break
		}
		name, value, ok := strings.Cut(line, ":")
		if !ok {
			return nil, fmt.Errorf("invalid RPC header %q", line)
		}
		if name == "Content-Length" {
			contentLength, err = strconv.Atoi(strings.TrimSpace(value))
			if err != nil {
				return nil, fmt.Errorf("parse content length: %w", err)
			}
		}
	}
	if contentLength < 0 {
		return nil, fmt.Errorf("missing Content-Length")
	}
	body := make([]byte, contentLength)
	_, err := io.ReadFull(reader, body)
	return body, err
}

func writeRPCFrame(writer io.Writer, message any) error {
	body, err := json.Marshal(message)
	if err != nil {
		return err
	}
	if _, err := fmt.Fprintf(writer, "Content-Length: %d\r\n\r\n", len(body)); err != nil {
		return err
	}
	_, err = writer.Write(body)
	return err
}
