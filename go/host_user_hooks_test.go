package copilot

import (
	"encoding/json"
	"errors"
	"fmt"
	"strings"
	"testing"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
)

func TestHostUserHooksDoesNotProbePing(t *testing.T) {
	rpcClient, server, _ := newRuntimeShutdownRpcPair(t)
	t.Cleanup(server.Stop)
	client := &Client{client: rpcClient}
	pingCalls := 0
	server.SetRequestHandler("connect", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		return nil, &jsonrpc2.Error{Code: -32601, Message: "Unhandled method connect"}
	})
	server.SetRequestHandler("ping", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		pingCalls++
		return []byte(`{"protocolVersion":4}`), nil
	})
	var rpcErr *jsonrpc2.Error
	if err := client.verifyProtocolVersion(t.Context()); !errors.As(err, &rpcErr) || rpcErr.Code != -32601 {
		t.Fatalf("expected connect method-not-found error, got %v", err)
	}
	if pingCalls != 0 {
		t.Fatalf("unexpected legacy ping probe: %d", pingCalls)
	}
}

func TestHostUserHooksRejectsProtocol3(t *testing.T) {
	rpcClient, server, _ := newRuntimeShutdownRpcPair(t)
	t.Cleanup(server.Stop)
	client := &Client{client: rpcClient, internalRPC: rpc.NewInternalServerRPC(rpcClient)}
	calls := 0
	server.SetRequestHandler("connect", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		calls++
		return []byte(`{"ok":true,"protocolVersion":3,"version":"test"}`), nil
	})
	if err := client.verifyProtocolVersion(t.Context()); err == nil || !strings.Contains(err.Error(), "SDK protocol version mismatch") {
		t.Fatalf("expected protocol 3 rejection, got %v", err)
	}
	if calls != 1 {
		t.Fatalf("expected exactly one handshake, got %d", calls)
	}
}

func TestHostUserHooksMatrix(t *testing.T) {
	f, tr := false, true
	for _, mode := range []ClientMode{ModeEmpty, ModeCopilotCli} {
		for _, supplied := range []*bool{nil, &f, &tr} {
			label := "unset"
			if supplied != nil {
				label = fmt.Sprint(*supplied)
			}
			for _, operation := range []string{"create", "resume"} {
				t.Run(fmt.Sprintf("%s/%s/%s", mode, operation, label), func(t *testing.T) {
					client, requests, cleanup := newInMemoryClientWithOptions(t, &ClientOptions{
						Mode: mode, Connection: URIConnection{URL: "http://localhost:1234"},
					})
					defer cleanup()
					called := false
					hooks := &SessionHooks{OnSessionStart: func(input SessionStartHookInput, invocation HookInvocation) (*SessionStartHookOutput, error) {
						called = true
						return nil, nil
					}}
					var session *Session
					var err error
					if operation == "create" {
						config := &SessionConfig{AvailableTools: []string{}, EnableHostUserHooks: supplied, Hooks: hooks}
						session, err = client.CreateSession(t.Context(), config)
						if config.EnableHostUserHooks != supplied {
							t.Fatal("mutated host hook config")
						}
					} else {
						config := &ResumeSessionConfig{AvailableTools: []string{}, EnableHostUserHooks: supplied, Hooks: hooks}
						session, err = client.ResumeSession(t.Context(), "existing-enabled-session", config)
						if config.EnableHostUserHooks != supplied {
							t.Fatal("mutated host hook config")
						}
					}
					if err != nil {
						t.Fatal(err)
					}
					request, ok := findRequest(requests.snapshot(), "session."+operation)
					expected := mode != ModeEmpty
					if supplied != nil {
						expected = *supplied
					}
					if !ok || request.Params["enableHostUserHooks"] != expected {
						t.Fatalf("expected enableHostUserHooks=%v, got %+v", expected, request.Params)
					}
					if request.Params["hooks"] != true || session.getHooks() != hooks {
						t.Fatal("SDK callbacks must remain registered independently")
					}
					_, err = session.getHooks().OnSessionStart(SessionStartHookInput{}, HookInvocation{})
					if err != nil || !called {
						t.Fatal("SDK callback did not run")
					}
				})
			}
		}
	}
}

func TestHostUserHooksConfigReuse(t *testing.T) {
	create := &SessionConfig{AvailableTools: []string{}}
	resume := &ResumeSessionConfig{AvailableTools: []string{}}
	for _, mode := range []ClientMode{ModeEmpty, ModeCopilotCli} {
		client, requests, cleanup := newInMemoryClientWithOptions(t, &ClientOptions{
			Mode: mode, Connection: URIConnection{URL: "http://localhost:1234"},
		})
		defer cleanup()
		session, err := client.CreateSession(t.Context(), create)
		if err != nil {
			t.Fatal(err)
		}
		_, err = client.ResumeSession(t.Context(), session.SessionID, resume)
		if err != nil {
			t.Fatal(err)
		}
		for _, method := range []string{"session.create", "session.resume"} {
			request, ok := findRequest(requests.snapshot(), method)
			if !ok || request.Params["enableHostUserHooks"] != (mode != ModeEmpty) {
				t.Fatalf("%s used stale config: %+v", method, request.Params)
			}
		}
		if create.EnableHostUserHooks != nil || resume.EnableHostUserHooks != nil {
			t.Fatal("default persisted in caller config")
		}
	}
}

func TestHostUserHooksResumeCurrentDefaults(t *testing.T) {
	for _, mode := range []ClientMode{ModeEmpty, ModeCopilotCli} {
		client, requests, cleanup := newInMemoryClientWithOptions(t, &ClientOptions{
			Mode: mode, Connection: URIConnection{URL: "http://localhost:1234"},
		})
		defer cleanup()
		enabled := true
		session, err := client.CreateSession(t.Context(), &SessionConfig{
			AvailableTools: []string{}, EnableHostUserHooks: &enabled,
		})
		if err != nil {
			t.Fatal(err)
		}
		var config *ResumeSessionConfig
		if mode == ModeEmpty {
			config = &ResumeSessionConfig{AvailableTools: []string{}}
		}
		_, err = client.ResumeSession(t.Context(), session.SessionID, config)
		if err != nil {
			t.Fatal(err)
		}
		request, ok := findRequest(requests.snapshot(), "session.resume")
		if !ok || request.Params["enableHostUserHooks"] != (mode != ModeEmpty) {
			t.Fatalf("resume inherited previous value: %+v", request.Params)
		}
	}
}
