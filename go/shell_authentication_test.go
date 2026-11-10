// Copyright (c) Microsoft Corporation. All rights reserved.

package copilot

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"io"
	"log"
	"net"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
)

func shellAuthenticationTestRequest() shellAuthenticationRequest {
	return shellAuthenticationRequest{
		SessionID: "s1",
		RequestID: "secret-1",
		Program:   "ssh",
		Prompt:    "Enter passphrase for key '/k': ",
		InputKind: "secret",
		Command:   "ssh host",
		Cwd:       "/work",
	}
}

func cancelledShellAuthentication() ShellAuthenticationResponse {
	return ShellAuthenticationResponse{Action: ShellAuthenticationActionCancel}
}

// handlerAwaitingShellAuthenticationCancel registers a handler that reports its
// invocation context and then waits until that context ends.
func handlerAwaitingShellAuthenticationCancel(session *Session) <-chan context.Context {
	started := make(chan context.Context, 1)
	session.registerShellAuthenticationHandler(func(_ ShellAuthenticationRequest, invocation ShellAuthenticationInvocation) (ShellAuthenticationResponse, error) {
		started <- invocation.Context
		<-invocation.Context.Done()
		return ShellAuthenticationResponse{Action: ShellAuthenticationActionAccept, Value: "too late"}, nil
	})
	return started
}

func receiveWithin[T any](t *testing.T, values <-chan T, what string) T {
	t.Helper()
	select {
	case value := <-values:
		return value
	case <-time.After(5 * time.Second):
		t.Fatalf("timed out waiting for %s", what)
		var zero T
		return zero
	}
}

func askForShellAuthentication(client *Client, request shellAuthenticationRequest) <-chan ShellAuthenticationResponse {
	prepareShellAuthentication(client, request)
	answers := make(chan ShellAuthenticationResponse, 1)
	go func() {
		response, rpcErr := client.handleShellAuthenticationRequest(request)
		if rpcErr != nil || response == nil {
			answers <- ShellAuthenticationResponse{Action: "unexpected rpc error"}
			return
		}
		answers <- *response
	}()
	return answers
}

// prepareShellAuthentication registers request the way the transport does, before it
// reads the next message.
func prepareShellAuthentication(client *Client, request shellAuthenticationRequest) {
	raw, err := json.Marshal(request)
	if err != nil {
		panic(err)
	}
	client.prepareShellAuthenticationRequest(raw)
}

// requestShellAuthentication runs a shellAuthentication.request the way the transport does:
// its preparation in read order, then its handler.
func requestShellAuthentication(client *Client, request shellAuthenticationRequest) (*ShellAuthenticationResponse, *jsonrpc2.Error) {
	prepareShellAuthentication(client, request)
	return client.handleShellAuthenticationRequest(request)
}

// captureShellAuthenticationLog redirects the standard logger until the test ends.
func captureShellAuthenticationLog(t *testing.T) *ahpTestLog {
	t.Helper()
	output := new(ahpTestLog)
	previous := log.Writer()
	log.SetOutput(output)
	t.Cleanup(func() { log.SetOutput(previous) })
	return output
}

func TestShellAuthenticationReturnsOnlyAWellFormedAnswer(t *testing.T) {
	session := &Session{SessionID: "s1"}
	client := &Client{sessions: map[string]*Session{"s1": session}}
	for _, tt := range []struct {
		name    string
		handler ShellAuthenticationHandler
		want    ShellAuthenticationResponse
		// loggedType is the error type a logged failure names, or empty when
		// nothing is logged.
		loggedType string
	}{
		{
			name: "accept",
			handler: func(ShellAuthenticationRequest, ShellAuthenticationInvocation) (ShellAuthenticationResponse, error) {
				return ShellAuthenticationResponse{Action: ShellAuthenticationActionAccept, Value: "HUNTER2-FAKE-0001"}, nil
			},
			want: ShellAuthenticationResponse{Action: ShellAuthenticationActionAccept, Value: "HUNTER2-FAKE-0001"},
		},
		{
			name: "error",
			handler: func(ShellAuthenticationRequest, ShellAuthenticationInvocation) (ShellAuthenticationResponse, error) {
				return ShellAuthenticationResponse{Action: ShellAuthenticationActionAccept, Value: "unused"}, errors.New("rejected HUNTER2-FAKE-0003")
			},
			want:       cancelledShellAuthentication(),
			loggedType: "*errors.errorString",
		},
		{
			name: "unknown action",
			handler: func(ShellAuthenticationRequest, ShellAuthenticationInvocation) (ShellAuthenticationResponse, error) {
				return ShellAuthenticationResponse{Action: "maybe", Value: "unused"}, nil
			},
			want: cancelledShellAuthentication(),
		},
		{
			name: "cancel",
			handler: func(ShellAuthenticationRequest, ShellAuthenticationInvocation) (ShellAuthenticationResponse, error) {
				return cancelledShellAuthentication(), nil
			},
			want: cancelledShellAuthentication(),
		},
		{
			name: "panic",
			handler: func(ShellAuthenticationRequest, ShellAuthenticationInvocation) (ShellAuthenticationResponse, error) {
				panic("rejected HUNTER2-FAKE-0004")
			},
			want:       cancelledShellAuthentication(),
			loggedType: "copilot.shellAuthenticationHandlerPanic",
		},
	} {
		t.Run(tt.name, func(t *testing.T) {
			output := captureShellAuthenticationLog(t)
			session.registerShellAuthenticationHandler(tt.handler)
			got, rpcErr := requestShellAuthentication(client, shellAuthenticationTestRequest())
			if rpcErr != nil {
				t.Fatalf("unexpected RPC error: %v", rpcErr)
			}
			if *got != tt.want {
				t.Fatalf("got %#v, want %#v", *got, tt.want)
			}
			text := output.text()
			want := ""
			if tt.loggedType != "" {
				want = "shell authentication handler failed: session_id=s1 request_id=secret-1 error_type=" + tt.loggedType
			}
			if (want == "") != (text == "") || !strings.Contains(text, want) || strings.Contains(text, "HUNTER2") {
				t.Fatalf("log %q, want %q", text, want)
			}
		})
	}
}

func TestShellAuthenticationDoesNotLogAHandlerStoppedByItsCancelledPrompt(t *testing.T) {
	output := captureShellAuthenticationLog(t)
	session := &Session{SessionID: "s1"}
	client := &Client{sessions: map[string]*Session{"s1": session}}
	started := make(chan struct{}, 1)
	session.registerShellAuthenticationHandler(func(_ ShellAuthenticationRequest, invocation ShellAuthenticationInvocation) (ShellAuthenticationResponse, error) {
		started <- struct{}{}
		<-invocation.Context.Done()
		return ShellAuthenticationResponse{}, invocation.Context.Err()
	})

	answers := askForShellAuthentication(client, shellAuthenticationTestRequest())
	receiveWithin(t, started, "handler start")
	if _, rpcErr := client.handleShellAuthenticationCancel(shellAuthenticationCancelRequest{SessionID: "s1", RequestID: "secret-1"}); rpcErr != nil {
		t.Fatalf("unexpected RPC error: %v", rpcErr)
	}
	if got := receiveWithin(t, answers, "cancelled answer"); got != cancelledShellAuthentication() {
		t.Fatalf("got %#v, want cancel", got)
	}
	if text := output.text(); text != "" {
		t.Fatalf("unexpected log %q", text)
	}
}

func TestShellAuthenticationSendsAnAcceptedEmptyAnswer(t *testing.T) {
	session := &Session{SessionID: "s1"}
	client := &Client{sessions: map[string]*Session{"s1": session}}
	session.registerShellAuthenticationHandler(func(ShellAuthenticationRequest, ShellAuthenticationInvocation) (ShellAuthenticationResponse, error) {
		return ShellAuthenticationResponse{Action: ShellAuthenticationActionAccept, Value: ""}, nil
	})
	request := shellAuthenticationTestRequest()
	request.InputKind = "text"
	got, rpcErr := requestShellAuthentication(client, request)
	if rpcErr != nil {
		t.Fatalf("unexpected RPC error: %v", rpcErr)
	}
	for _, tt := range []struct {
		response *ShellAuthenticationResponse
		want     string
	}{
		{got, `{"action":"accept","value":""}`},
		{&ShellAuthenticationResponse{Action: ShellAuthenticationActionCancel}, `{"action":"cancel"}`},
	} {
		wire, err := json.Marshal(tt.response)
		if err != nil {
			t.Fatalf("marshal: %v", err)
		}
		if string(wire) != tt.want {
			t.Fatalf("got %s, want %s", wire, tt.want)
		}
	}
}

func TestShellAuthenticationPassesTheRuntimeRequest(t *testing.T) {
	session := &Session{SessionID: "s1"}
	client := &Client{sessions: map[string]*Session{"s1": session}}
	var requests []ShellAuthenticationRequest
	var sessionIDs []string
	session.registerShellAuthenticationHandler(func(request ShellAuthenticationRequest, invocation ShellAuthenticationInvocation) (ShellAuthenticationResponse, error) {
		requests = append(requests, request)
		sessionIDs = append(sessionIDs, invocation.SessionID)
		return cancelledShellAuthentication(), nil
	})
	userName := shellAuthenticationTestRequest()
	userName.RequestID = "secret-2"
	userName.InputKind = "text"
	for _, request := range []shellAuthenticationRequest{shellAuthenticationTestRequest(), userName} {
		if _, rpcErr := requestShellAuthentication(client, request); rpcErr != nil {
			t.Fatalf("unexpected RPC error: %v", rpcErr)
		}
	}

	if len(requests) != 2 || sessionIDs[0] != "s1" || sessionIDs[1] != "s1" {
		t.Fatalf("unexpected handler calls: %#v %#v", requests, sessionIDs)
	}
	got := requests[0]
	if got.RequestID != "secret-1" || got.Program != "ssh" || got.Prompt != "Enter passphrase for key '/k': " ||
		got.InputKind != ShellAuthenticationInputKindSecret || got.Command != "ssh host" || got.Cwd != "/work" {
		t.Fatalf("unexpected secret request: %#v", got)
	}
	if requests[1].InputKind != ShellAuthenticationInputKindText {
		t.Fatalf("unexpected text request: %#v", requests[1])
	}
}

func TestShellAuthenticationCancelsAMalformedRequestWithoutAsking(t *testing.T) {
	session := &Session{SessionID: "s1"}
	client := &Client{sessions: map[string]*Session{"s1": session}}
	session.registerShellAuthenticationHandler(func(ShellAuthenticationRequest, ShellAuthenticationInvocation) (ShellAuthenticationResponse, error) {
		t.Error("handler called for a malformed request")
		return cancelledShellAuthentication(), nil
	})
	unknownKind := shellAuthenticationTestRequest()
	unknownKind.InputKind = "password"
	noRequestID := shellAuthenticationTestRequest()
	noRequestID.RequestID = ""
	unknownSession := shellAuthenticationTestRequest()
	unknownSession.SessionID = "unknown"
	for _, request := range []shellAuthenticationRequest{unknownKind, noRequestID, unknownSession} {
		got, rpcErr := requestShellAuthentication(client, request)
		if rpcErr != nil || *got != cancelledShellAuthentication() {
			t.Fatalf("got %#v, %v for %#v", got, rpcErr, request)
		}
	}
	// A request without the program that asked or its prompt is malformed too.
	for _, raw := range []string{
		`{"sessionId":"s1","requestId":"secret-1","prompt":"Password: ","inputKind":"secret"}`,
		`{"sessionId":"s1","requestId":"secret-1","program":"sudo","inputKind":"secret"}`,
		`{"sessionId":"s1","requestId":"secret-1","program":"sudo","prompt":null,"inputKind":"secret"}`,
	} {
		var request shellAuthenticationRequest
		if err := json.Unmarshal([]byte(raw), &request); err != nil {
			t.Fatal(err)
		}
		client.prepareShellAuthenticationRequest(json.RawMessage(raw))
		got, rpcErr := client.handleShellAuthenticationRequest(request)
		if rpcErr != nil || *got != cancelledShellAuthentication() {
			t.Fatalf("got %#v, %v for %s", got, rpcErr, raw)
		}
	}
	// Without a handler, the session cancels too.
	session.registerShellAuthenticationHandler(nil)
	got, _ := requestShellAuthentication(client, shellAuthenticationTestRequest())
	if *got != cancelledShellAuthentication() {
		t.Fatalf("got %#v without a handler", *got)
	}
}

func TestShellAuthenticationRuntimeCancelClosesAPendingPrompt(t *testing.T) {
	session := &Session{SessionID: "s1"}
	client := &Client{sessions: map[string]*Session{"s1": session}}
	started := handlerAwaitingShellAuthenticationCancel(session)
	answers := askForShellAuthentication(client, shellAuthenticationTestRequest())
	invocationContext := receiveWithin(t, started, "the handler")

	if _, rpcErr := client.handleShellAuthenticationCancel(shellAuthenticationCancelRequest{SessionID: "s1", RequestID: "secret-1"}); rpcErr != nil {
		t.Fatalf("unexpected RPC error: %v", rpcErr)
	}
	if got := receiveWithin(t, answers, "the answer"); got != cancelledShellAuthentication() {
		t.Fatalf("got %#v after the runtime cancelled", got)
	}
	if invocationContext.Err() == nil {
		t.Fatal("the handler's context was not cancelled")
	}
	client.pendingShellAuthenticationRequestsMux.Lock()
	defer client.pendingShellAuthenticationRequestsMux.Unlock()
	if len(client.pendingShellAuthenticationRequests) != 0 {
		t.Fatalf("pending prompts left: %d", len(client.pendingShellAuthenticationRequests))
	}
}

func TestShellAuthenticationCancelsADuplicateRequestWithoutClosingTheOpenOne(t *testing.T) {
	session := &Session{SessionID: "s1"}
	client := &Client{sessions: map[string]*Session{"s1": session}}
	started := handlerAwaitingShellAuthenticationCancel(session)
	answers := askForShellAuthentication(client, shellAuthenticationTestRequest())
	invocationContext := receiveWithin(t, started, "the handler")

	got, rpcErr := requestShellAuthentication(client, shellAuthenticationTestRequest())
	if rpcErr != nil || *got != cancelledShellAuthentication() {
		t.Fatalf("got %#v, %v for the duplicate", got, rpcErr)
	}
	select {
	case <-started:
		t.Fatal("the handler was asked again for the duplicate")
	default:
	}
	if invocationContext.Err() != nil {
		t.Fatal("the duplicate closed the open prompt")
	}

	if _, rpcErr := client.handleShellAuthenticationCancel(shellAuthenticationCancelRequest{SessionID: "s1", RequestID: "secret-1"}); rpcErr != nil {
		t.Fatalf("unexpected RPC error: %v", rpcErr)
	}
	if got := receiveWithin(t, answers, "the answer"); got != cancelledShellAuthentication() {
		t.Fatalf("got %#v after the runtime cancelled", got)
	}
}

func TestShellAuthenticationCancelsAHandlerThatNeverReturns(t *testing.T) {
	session := &Session{SessionID: "s1"}
	client := &Client{sessions: map[string]*Session{"s1": session}}
	release := make(chan struct{})
	t.Cleanup(func() { close(release) })
	started := make(chan struct{}, 1)
	session.registerShellAuthenticationHandler(func(ShellAuthenticationRequest, ShellAuthenticationInvocation) (ShellAuthenticationResponse, error) {
		started <- struct{}{}
		<-release
		return ShellAuthenticationResponse{Action: ShellAuthenticationActionAccept, Value: "too late"}, nil
	})
	answers := askForShellAuthentication(client, shellAuthenticationTestRequest())
	receiveWithin(t, started, "the handler")

	client.handleShellAuthenticationCancel(shellAuthenticationCancelRequest{SessionID: "s1", RequestID: "secret-1"})
	if got := receiveWithin(t, answers, "the answer"); got != cancelledShellAuthentication() {
		t.Fatalf("got %#v for a handler that never returned", got)
	}
}

func TestShellAuthenticationCancelReachesTheSessionThatTookItAfterAResume(t *testing.T) {
	session := &Session{SessionID: "s1"}
	client := &Client{sessions: map[string]*Session{"s1": session}}
	started := handlerAwaitingShellAuthenticationCancel(session)
	answers := askForShellAuthentication(client, shellAuthenticationTestRequest())
	receiveWithin(t, started, "the handler")
	// A resume registers a new session object while the prompt is open.
	client.sessionsMux.Lock()
	client.sessions["s1"] = &Session{SessionID: "s1"}
	client.sessionsMux.Unlock()

	client.handleShellAuthenticationCancel(shellAuthenticationCancelRequest{SessionID: "s1", RequestID: "secret-1"})
	if got := receiveWithin(t, answers, "the answer"); got != cancelledShellAuthentication() {
		t.Fatalf("got %#v after the runtime cancelled", got)
	}
}

// signalingReader closes seen once the client has read marker.
type signalingReader struct {
	io.ReadCloser
	marker []byte
	seen   chan struct{}
	read   []byte
	once   sync.Once
}

func (r *signalingReader) Read(p []byte) (int, error) {
	n, err := r.ReadCloser.Read(p)
	r.read = append(r.read, p[:n]...)
	if bytes.Contains(r.read, r.marker) {
		r.once.Do(func() { close(r.seen) })
	}
	return n, err
}

func TestShellAuthenticationCancelOrCloseThatFollowsItsRequestIsNeverMissed(t *testing.T) {
	for _, tt := range []struct {
		name   string
		follow func(t *testing.T, client *Client, server *jsonrpc2.Client)
	}{
		{"cancel", func(t *testing.T, _ *Client, server *jsonrpc2.Client) {
			if _, err := server.Request(t.Context(), "shellAuthentication.cancel", shellAuthenticationCancelRequest{SessionID: "s1", RequestID: "secret-1"}); err != nil {
				t.Errorf("cancel failed: %v", err)
			}
		}},
		{"connection close", func(t *testing.T, client *Client, server *jsonrpc2.Client) {
			// A round trip after the request means the client has finished
			// reading it, as it has by the time the connection closes.
			if _, err := server.Request(t.Context(), "shellAuthentication.cancel", shellAuthenticationCancelRequest{SessionID: "s1", RequestID: "unrelated"}); err != nil {
				t.Errorf("round trip failed: %v", err)
			}
			// What handleConnectionClose does for open prompts; the rest of it
			// would wait for the sessions lock this test holds.
			client.cancelPendingShellAuthenticationRequests()
		}},
	} {
		t.Run(tt.name, func(t *testing.T) {
			clientConn, serverConn := net.Pipe()
			t.Cleanup(func() {
				clientConn.Close()
				serverConn.Close()
			})
			reader := &signalingReader{ReadCloser: clientConn, marker: []byte(`"shellAuthentication.request"`), seen: make(chan struct{})}
			rpcClient := jsonrpc2.NewClient(clientConn, reader)
			server := jsonrpc2.NewClient(serverConn, serverConn)
			session := newSession("s1", rpcClient, "", false)
			client := &Client{
				client:   rpcClient,
				RPC:      rpc.NewServerRPC(rpcClient),
				sessions: map[string]*Session{"s1": session},
			}
			client.setupNotificationHandler()
			rpcClient.Start()
			server.Start()
			t.Cleanup(server.Stop)
			t.Cleanup(rpcClient.Stop)
			asked := make(chan struct{}, 1)
			release := make(chan struct{})
			t.Cleanup(func() { close(release) })
			session.registerShellAuthenticationHandler(func(_ ShellAuthenticationRequest, invocation ShellAuthenticationInvocation) (ShellAuthenticationResponse, error) {
				asked <- struct{}{}
				select {
				case <-invocation.Context.Done():
				case <-release:
				}
				return ShellAuthenticationResponse{Action: ShellAuthenticationActionAccept, Value: "too late"}, nil
			})

			// Hold the request's goroutine before it looks up its session, so what
			// follows the request on the wire runs first.
			client.sessionsMux.Lock()
			type answer struct {
				raw json.RawMessage
				err error
			}
			answers := make(chan answer, 1)
			go func() {
				raw, err := server.Request(t.Context(), "shellAuthentication.request", shellAuthenticationTestRequest())
				answers <- answer{raw, err}
			}()
			receiveWithin(t, reader.seen, "the request to be read")
			tt.follow(t, client, server)
			client.sessionsMux.Unlock()

			result := receiveWithin(t, answers, "the answer")
			var got ShellAuthenticationResponse
			if result.err != nil || json.Unmarshal(result.raw, &got) != nil || got != cancelledShellAuthentication() {
				t.Fatalf("got %s, %v; want a cancel", result.raw, result.err)
			}
			select {
			case <-asked:
				t.Fatal("the handler was asked for a prompt that was already closed")
			default:
			}
		})
	}
}

func TestShellAuthenticationConnectionCloseAndRetirementCancelOpenPrompts(t *testing.T) {
	rpcClient, server, _ := newRuntimeShutdownRpcPair(t)
	t.Cleanup(server.Stop)
	session := newSession("s1", rpcClient, "", false)
	client := &Client{
		client:           rpcClient,
		RPC:              rpc.NewServerRPC(rpcClient),
		sessions:         map[string]*Session{"s1": session},
		isExternalServer: true,
	}
	started := handlerAwaitingShellAuthenticationCancel(session)
	answers := askForShellAuthentication(client, shellAuthenticationTestRequest())
	receiveWithin(t, started, "the handler")
	client.handleConnectionClose()
	if got := receiveWithin(t, answers, "the answer"); got != cancelledShellAuthentication() {
		t.Fatalf("got %#v after the connection closed", got)
	}

	// A retired session cancels the prompts it is answering.
	retired := newSession("s2", rpcClient, "", false)
	started = handlerAwaitingShellAuthenticationCancel(retired)
	answered := make(chan ShellAuthenticationResponse, 1)
	go func() {
		answered <- retired.handleShellAuthenticationRequest(context.Background(), ShellAuthenticationRequest{RequestID: "secret-2", InputKind: ShellAuthenticationInputKindSecret})
	}()
	receiveWithin(t, started, "the handler")
	retired.stopEventProcessing()
	if got := receiveWithin(t, answered, "the answer"); got != cancelledShellAuthentication() {
		t.Fatalf("got %#v after the session retired", got)
	}
}

func TestClient_RequestsShellAuthenticationOnlyWithAHandler(t *testing.T) {
	rpcClient, server, _ := newRuntimeShutdownRpcPair(t)
	t.Cleanup(server.Stop)
	client := &Client{
		client:   rpcClient,
		RPC:      rpc.NewServerRPC(rpcClient),
		sessions: make(map[string]*Session),
	}
	sent := make(chan json.RawMessage, 4)
	for _, method := range []string{"session.create", "session.resume"} {
		server.SetRequestHandler(method, func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			sent <- append(json.RawMessage(nil), params...)
			sessionID := sessionIDFromParams(t, params)
			return []byte(`{"sessionId":"` + sessionID + `","workspacePath":"/workspace"}`), nil
		})
	}
	handler := func(ShellAuthenticationRequest, ShellAuthenticationInvocation) (ShellAuthenticationResponse, error) {
		return cancelledShellAuthentication(), nil
	}

	if _, err := client.CreateSession(t.Context(), &SessionConfig{SessionID: "secret-create", OnShellAuthenticationRequest: handler}); err != nil {
		t.Fatalf("CreateSession failed: %v", err)
	}
	if _, err := client.ResumeSession(t.Context(), "secret-resume", &ResumeSessionConfig{OnShellAuthenticationRequest: handler}); err != nil {
		t.Fatalf("ResumeSession failed: %v", err)
	}
	if _, err := client.CreateSession(t.Context(), &SessionConfig{SessionID: "plain-create"}); err != nil {
		t.Fatalf("CreateSession without a handler failed: %v", err)
	}
	if _, err := client.ResumeSession(t.Context(), "plain-resume", nil); err != nil {
		t.Fatalf("ResumeSession without a handler failed: %v", err)
	}

	for _, want := range []any{true, true, nil, nil} {
		var params map[string]any
		if err := json.Unmarshal(receiveWithin(t, sent, "a session request"), &params); err != nil {
			t.Fatal(err)
		}
		if got := params["requestShellAuthentication"]; got != want {
			t.Fatalf("requestShellAuthentication = %v, want %v", got, want)
		}
	}
}
