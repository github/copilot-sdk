/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package copilot

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"sync"
	"testing"

	"github.com/coder/websocket"
	"github.com/github/copilot-sdk/go/rpc"
)

func TestInferenceHTTPInvocationMetadata(t *testing.T) {
	for _, id := range []*string{nil, ptr("logical-invocation"), ptr("")} {
		t.Run(stringOrEmpty(id), func(t *testing.T) {
			contexts := make(chan *CopilotRequestContext, 2)
			peer := newResponseProtocolPeerWithTransport(t, roundTripFunc(func(req *http.Request) (*http.Response, error) {
				contexts <- RequestContextFrom(req)
				body, err := io.ReadAll(req.Body)
				if err != nil {
					return nil, err
				}
				if string(body) != "request-body" {
					t.Errorf("request body = %q, want request-body", body)
				}
				return &http.Response{
					StatusCode: http.StatusOK,
					Header:     http.Header{},
					Body:       io.NopCloser(strings.NewReader("")),
				}, nil
			}))
			for _, requestID := range []string{"physical-first", "physical-retry"} {
				beginInferenceRequest(t, peer, requestID, id, "http")
				end := true
				binary := true
				_, err := peer.adapter.HttpRequestChunk(&rpc.LlmInferenceHTTPRequestChunkRequest{
					RequestID: requestID, Data: base64.StdEncoding.EncodeToString([]byte("request-body")),
					Binary: &binary, End: &end,
				})
				if err != nil {
					t.Fatal(err)
				}
				peer.ack(peer.nextRequest())
				peer.ack(peer.nextRequest())
				ctx := <-contexts
				assertInvocationID(t, ctx.AgentInvocationID, id)
				if ctx.RequestID != requestID || ctx.SessionID != "session" || ctx.AgentID != "trajectory" ||
					ctx.ParentAgentID != "parent" || ctx.InteractionType != "conversation-agent" {
					t.Fatalf("unexpected context: %+v", ctx)
				}
				if values := ctx.Headers.Values("X-Multiple"); len(values) != 2 || values[0] != "one" || values[1] != "two" {
					t.Fatalf("headers = %v, want [one two]", ctx.Headers)
				}
			}
		})
	}
}

func TestInferenceWebSocketInvocationMetadata(t *testing.T) {
	socket := &metadataWebSocket{done: make(chan struct{})}
	var connectionCtx *CopilotRequestContext
	peer := newResponseProtocolPeerWithTransport(t, nil)
	peer.adapter.handler.OpenWebSocket = func(ctx *CopilotRequestContext) (CopilotWebSocketHandler, error) {
		connectionCtx = ctx
		return socket, nil
	}
	ids := []*string{ptr("opening-invocation"), ptr("later-invocation"), ptr("later-invocation"), nil, ptr("")}
	// Buffer distinct invocation identities before the start to prove the queue
	// stores per-frame metadata rather than updating a shared connection value.
	for _, id := range ids[:2] {
		_, err := peer.adapter.HttpRequestChunk(&rpc.LlmInferenceHTTPRequestChunkRequest{
			RequestID: "reused-socket", Data: "message", AgentInvocationID: id,
		})
		if err != nil {
			t.Fatal(err)
		}
	}
	beginInferenceRequest(t, peer, "reused-socket", ids[0], "websocket")
	peer.ack(peer.nextRequest())
	for _, id := range ids[2:] {
		_, err := peer.adapter.HttpRequestChunk(&rpc.LlmInferenceHTTPRequestChunkRequest{
			RequestID: "reused-socket", Data: "message", AgentInvocationID: id,
		})
		if err != nil {
			t.Fatal(err)
		}
	}
	end := true
	_, err := peer.adapter.HttpRequestChunk(&rpc.LlmInferenceHTTPRequestChunkRequest{
		RequestID: "reused-socket", End: &end,
	})
	if err != nil {
		t.Fatal(err)
	}
	peer.ack(peer.nextRequest())

	assertInvocationID(t, connectionCtx.AgentInvocationID, ids[0])
	if len(socket.messages) != len(ids) {
		t.Fatalf("received %d messages, want %d", len(socket.messages), len(ids))
	}
	for i, message := range socket.messages {
		assertInvocationID(t, message.AgentInvocationID, ids[i])
		if message.Text() != "message" || message.Binary {
			t.Fatalf("unexpected message: %+v", message)
		}
	}
}

func TestInferenceWebSocketTransformationReceivesInvocationMetadata(t *testing.T) {
	id := "later-invocation"
	var received *string
	forwarder := NewCopilotWebSocketForwarder("ws://unused.test", nil)
	forwarder.OnSendRequestMessage = func(message CopilotWebSocketMessage) *CopilotWebSocketMessage {
		received = message.AgentInvocationID
		return nil
	}
	if err := forwarder.SendRequestMessage(context.Background(), CopilotWebSocketMessage{
		Data: []byte("message"), AgentInvocationID: &id,
	}); err != nil {
		t.Fatal(err)
	}
	assertInvocationID(t, received, &id)
}

func beginInferenceRequest(t *testing.T, peer *responseProtocolPeer, requestID string, id *string, transport string) {
	t.Helper()
	params := map[string]any{
		"requestId": requestID, "method": "GET", "url": "http://unused.test",
		"sessionId": "session", "agentId": "trajectory", "parentAgentId": "parent",
		"interactionType": "conversation-agent", "transport": transport,
		"headers": map[string][]string{"X-Multiple": {"one", "two"}},
	}
	if transport == "http" {
		params["method"] = "POST"
	}
	if id != nil {
		params["agentInvocationId"] = *id
	}
	peer.send(responseProtocolMessage{
		JSONRPC: "2.0", ID: json.RawMessage("42"), Method: "llmInference.httpRequestStart",
		Params: mustMarshal(t, params),
	})
}

func assertInvocationID(t *testing.T, got, want *string) {
	t.Helper()
	if (got == nil) != (want == nil) || got != nil && *got != *want {
		t.Fatalf("invocation ID = %v (%q), want %v (%q)", got, stringOrEmpty(got), want, stringOrEmpty(want))
	}
}

type metadataWebSocket struct {
	messages []CopilotWebSocketMessage
	done     chan struct{}
	once     sync.Once
}

func (s *metadataWebSocket) Open(context.Context, WebSocketResponseWriter) error { return nil }
func (s *metadataWebSocket) SendRequestMessage(_ context.Context, message CopilotWebSocketMessage) error {
	s.messages = append(s.messages, message)
	return nil
}
func (s *metadataWebSocket) Done() <-chan struct{} { return s.done }
func (s *metadataWebSocket) Err() error            { return nil }
func (s *metadataWebSocket) Close() error {
	s.once.Do(func() { close(s.done) })
	return nil
}

func TestInferenceHTTPTransportPanicFailsRequest(t *testing.T) {
	peer := newResponseProtocolPeerWithTransport(t, roundTripFunc(func(*http.Request) (*http.Response, error) {
		panic("transport panic")
	}))
	peer.beginRequest()

	assertResponseStartStatus(t, peer, http.StatusBadGateway)
	assertTerminalErrorMessage(t, peer, "CopilotRequestHandler panicked: transport panic")
}

func TestInferenceHTTPResponseBodyPanicFailsRequest(t *testing.T) {
	peer := newResponseProtocolPeer(t, panickingResponseBody{})
	peer.beginRequest()

	assertResponseStartStatus(t, peer, http.StatusOK)
	assertTerminalErrorMessage(t, peer, "HTTP response body read panicked: body read panic")
}

func TestInferenceWebSocketSendRequestMessagePanicFailsRequest(t *testing.T) {
	socket := &panickingSendWebSocket{done: make(chan struct{})}
	peer := newResponseProtocolPeerWithTransport(t, nil)
	peer.adapter.handler.OpenWebSocket = func(*CopilotRequestContext) (CopilotWebSocketHandler, error) {
		return socket, nil
	}
	beginInferenceRequest(t, peer, "panicking-socket", nil, "websocket")
	assertResponseStartStatus(t, peer, http.StatusSwitchingProtocols)

	if _, err := peer.adapter.HttpRequestChunk(&rpc.LlmInferenceHTTPRequestChunkRequest{
		RequestID: "panicking-socket", Data: "message",
	}); err != nil {
		t.Fatal(err)
	}
	assertTerminalErrorMessage(t, peer, "CopilotWebSocketHandler.SendRequestMessage panicked: send panic")
}

func TestInferenceWebSocketForwarderResponseHookPanicFailsRequest(t *testing.T) {
	upstream := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		conn, err := websocket.Accept(w, r, nil)
		if err != nil {
			return
		}
		defer conn.CloseNow()
		if err := conn.Write(r.Context(), websocket.MessageText, []byte("upstream message")); err != nil {
			return
		}
		for {
			if _, _, err := conn.Read(r.Context()); err != nil {
				return
			}
		}
	}))
	t.Cleanup(upstream.Close)

	peer := newResponseProtocolPeerWithTransport(t, nil)
	peer.adapter.handler.OpenWebSocket = func(*CopilotRequestContext) (CopilotWebSocketHandler, error) {
		forwarder := NewCopilotWebSocketForwarder("ws"+strings.TrimPrefix(upstream.URL, "http"), nil)
		forwarder.OnSendResponseMessage = func(CopilotWebSocketMessage) *CopilotWebSocketMessage {
			panic("response hook panic")
		}
		return forwarder, nil
	}
	beginInferenceRequest(t, peer, "forwarded-socket", nil, "websocket")
	assertResponseStartStatus(t, peer, http.StatusSwitchingProtocols)
	assertTerminalErrorMessage(t, peer, "CopilotWebSocketForwarder panicked while forwarding a response message: response hook panic")
}

func assertResponseStartStatus(t *testing.T, peer *responseProtocolPeer, want int) {
	t.Helper()
	head := peer.nextRequest()
	if head.Method != "llmInference.httpResponseStart" {
		t.Fatalf("first response method = %q, want httpResponseStart", head.Method)
	}
	var params map[string]any
	if err := json.Unmarshal(head.Params, &params); err != nil {
		t.Fatal(err)
	}
	if params["status"] != float64(want) {
		t.Fatalf("response status = %v, want %d", params["status"], want)
	}
	peer.ack(head)
}

func assertTerminalErrorMessage(t *testing.T, peer *responseProtocolPeer, want string) {
	t.Helper()
	terminal := peer.nextRequest()
	params := responseChunkParams(t, terminal)
	errorValue, ok := params["error"].(map[string]any)
	if params["end"] != true || !ok || errorValue["message"] != want {
		t.Fatalf("terminal chunk = %s, want error %q", terminal.Params, want)
	}
	peer.ack(terminal)
}

type panickingResponseBody struct{}

func (panickingResponseBody) Read([]byte) (int, error) { panic("body read panic") }
func (panickingResponseBody) Close() error             { return nil }

type panickingSendWebSocket struct {
	done chan struct{}
	once sync.Once
}

func (s *panickingSendWebSocket) Open(context.Context, WebSocketResponseWriter) error { return nil }
func (s *panickingSendWebSocket) SendRequestMessage(context.Context, CopilotWebSocketMessage) error {
	panic("send panic")
}
func (s *panickingSendWebSocket) Done() <-chan struct{} { return s.done }
func (s *panickingSendWebSocket) Err() error            { return nil }
func (s *panickingSendWebSocket) Close() error {
	s.once.Do(func() { close(s.done) })
	return nil
}
