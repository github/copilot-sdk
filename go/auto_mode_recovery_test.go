// Copyright (c) Microsoft Corporation. All rights reserved.

package copilot

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net"
	"reflect"
	"runtime"
	"strings"
	"sync"
	"sync/atomic"
	"testing"
	"time"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
)

func autoModeRecoveryClient(t *testing.T, caps *SessionCapabilitiesWithAutoModeRecovery) (*Client, *jsonrpc2.Client, *requestRecorder) {
	t.Helper()
	client, server, requests, _ := autoModeRecoveryClientWithPeer(t, caps)
	return client, server, requests
}

func autoModeRecoveryClientWithPeer(t *testing.T, caps *SessionCapabilitiesWithAutoModeRecovery) (*Client, *jsonrpc2.Client, *requestRecorder, net.Conn) {
	t.Helper()
	transport, server, _, peer := newRuntimeShutdownRpcPairWithPeer(t)
	client := NewClient(nil)
	client.client = transport
	client.RPC = rpc.NewServerRPC(transport)
	client.state = stateConnected
	client.setupNotificationHandler()
	requests := &requestRecorder{}
	for _, method := range []string{"session.create", "session.resume", "session.detach", "session.delete", "session.options.update", "session.skills.reload"} {
		server.SetRequestHandler(method, func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			var input map[string]any
			if err := json.Unmarshal(params, &input); err != nil {
				return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
			}
			requests.append(recordedRequest{Method: method, Params: input})
			output := map[string]any{"success": true}
			switch method {
			case "session.create", "session.resume":
				output = map[string]any{"sessionId": input["sessionId"], "capabilities": caps}
			}
			data, err := json.Marshal(output)
			if err != nil {
				return nil, &jsonrpc2.Error{Code: -32603, Message: err.Error()}
			}
			return data, nil
		})
	}
	t.Cleanup(server.Stop)
	t.Cleanup(func() {
		if err := client.Stop(); err != nil {
			t.Errorf("client cleanup: %v", err)
		}
	})
	return client, server, requests, peer
}

func openAutoModeRecoverySession(ctx context.Context, client *Client, resume, enabled bool, handler AutoModeSwitchRequestHandler) (*Session, error) {
	if enabled {
		options := recoveryOptionsForLegacyTestHandler(handler)
		if resume {
			return client.ResumeSessionWithAutoModeRecovery(ctx, "session-1", &ResumeSessionConfig{
				OnEvent: func(SessionEvent) {},
			}, options)
		}
		return client.CreateSessionWithAutoModeRecovery(ctx, &SessionConfig{
			OnEvent: func(SessionEvent) {},
		}, options)
	}
	if resume {
		return client.ResumeSession(ctx, "session-1", &ResumeSessionConfig{
			OnAutoModeSwitchRequest: handler,
			OnEvent:                 func(SessionEvent) {},
		})
	}
	return client.CreateSession(ctx, &SessionConfig{
		OnAutoModeSwitchRequest: handler,
		OnEvent:                 func(SessionEvent) {},
	})
}

func openAutoModeRecoverySessionWithRecovery(ctx context.Context, client *Client, resume, enabled bool, handler AutoModeSwitchRequestHandlerWithAutoModeRecovery) (*Session, error) {
	if !enabled {
		var legacy AutoModeSwitchRequestHandler
		if handler != nil {
			legacy = func(request AutoModeSwitchRequest, invocation AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
				return handler(AutoModeSwitchRequestWithAutoModeRecovery{AutoModeSwitchRequest: request}, invocation)
			}
		}
		return openAutoModeRecoverySession(ctx, client, resume, false, legacy)
	}
	options := &AutoModeRecoveryOptions{OnAutoModeSwitchRequest: handler}
	if resume {
		return client.ResumeSessionWithAutoModeRecovery(ctx, "session-1", &ResumeSessionConfig{
			OnEvent: func(SessionEvent) {},
		}, options)
	}
	return client.CreateSessionWithAutoModeRecovery(ctx, &SessionConfig{
		OnEvent: func(SessionEvent) {},
	}, options)
}

func recoveryOptionsForLegacyTestHandler(handler AutoModeSwitchRequestHandler) *AutoModeRecoveryOptions {
	options := &AutoModeRecoveryOptions{}
	if handler != nil {
		options.OnAutoModeSwitchRequest = func(request AutoModeSwitchRequestWithAutoModeRecovery, invocation AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
			return handler(request.AutoModeSwitchRequest, invocation)
		}
	}
	return options
}

func createAutoModeRecoverySession(ctx context.Context, client *Client, config *SessionConfig) (*Session, error) {
	return client.CreateSessionWithAutoModeRecovery(ctx, config, recoveryOptionsForLegacyTestHandler(config.OnAutoModeSwitchRequest))
}

func resumeAutoModeRecoverySession(ctx context.Context, client *Client, sessionID string, config *ResumeSessionConfig) (*Session, error) {
	return client.ResumeSessionWithAutoModeRecovery(ctx, sessionID, config, recoveryOptionsForLegacyTestHandler(config.OnAutoModeSwitchRequest))
}

func mockSessionPrefixLookup(t *testing.T, server *jsonrpc2.Client, canonicalID string) {
	t.Helper()
	server.SetRequestHandler("sessions.findByPrefix", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var input rpc.SessionsFindByPrefixRequest
		if err := json.Unmarshal(params, &input); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
		}
		if !strings.HasPrefix(strings.ToLower(canonicalID), strings.ToLower(input.Prefix)) {
			t.Errorf("prefix %q does not match canonical fixture %q", input.Prefix, canonicalID)
		}
		result, err := json.Marshal(map[string]string{"sessionId": canonicalID})
		if err != nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: err.Error()}
		}
		return result, nil
	})
}

func TestAutoModeRecoveryPostCreateFailureRollsBackAssignedID(t *testing.T) {
	for _, failureMethod := range []string{"session.eventLog.registerInterest", "session.options.update"} {
		for _, cloud := range []bool{false, true} {
			for _, cleanupFailure := range []bool{false, true} {
				t.Run(fmt.Sprintf("%s/cloud-%t/cleanup-failure-%t", failureMethod, cloud, cleanupFailure), func(t *testing.T) {
					client, server, requests := autoModeRecoveryClient(t, nil)
					server.SetRequestHandler("session.create", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
						var input map[string]any
						if err := json.Unmarshal(params, &input); err != nil {
							return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
						}
						id, _ := input["sessionId"].(string)
						if id == "" {
							id = "cloud-setup-failed"
						}
						response, err := json.Marshal(map[string]any{"sessionId": id, "capabilities": map[string]any{"ui": map[string]any{"autoModeRecovery": true}}})
						if err != nil {
							t.Fatal(err)
						}
						return response, nil
					})
					server.SetRequestHandler("session.eventLog.registerInterest", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
						return []byte(`{}`), nil
					})
					server.SetRequestHandler(failureMethod, func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
						return nil, &jsonrpc2.Error{Code: -32603, Message: "post-create setup failed"}
					})
					server.SetRequestHandler("session.delete", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
						var input map[string]any
						if err := json.Unmarshal(params, &input); err != nil {
							return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
						}
						requests.append(recordedRequest{Method: "session.delete", Params: input})
						if cleanupFailure {
							return []byte(`{"success":false,"error":"cloud rollback rejected"}`), nil
						}
						return []byte(`{"success":true}`), nil
					})
					skip := true
					config := &SessionConfig{
						SessionID: "caller-owned", SkipCustomInstructions: &skip,
						OnAutoModeSwitchRequest: func(AutoModeSwitchRequest, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
							return AutoModeSwitchResponseNo, nil
						},
						OnMCPAuthRequest: func(MCPAuthRequest, MCPAuthInvocation) (*MCPAuthResult, error) { return nil, nil },
					}
					if cloud {
						config.SessionID = ""
						config.Cloud = &CloudSessionOptions{}
					}
					_, err := createAutoModeRecoverySession(t.Context(), client, config)
					if err == nil || !strings.Contains(err.Error(), "post-create setup failed") {
						t.Fatalf("setup error = %v", err)
					}
					deleted := 0
					for _, request := range requests.snapshot() {
						if request.Method == "session.delete" {
							deleted++
							if request.Params["sessionId"] != "cloud-setup-failed" {
								t.Fatalf("deleted caller-owned session: %+v", request)
							}
						}
					}
					want := 0
					if cloud {
						want = 1
					}
					if deleted != want {
						t.Fatalf("deletions = %d, want %d", deleted, want)
					}
					if cloud && cleanupFailure {
						if !strings.Contains(err.Error(), "cloud rollback rejected") {
							t.Fatalf("lost cleanup error: %v", err)
						}
						if _, err := client.ResumeSession(t.Context(), "cloud-setup-failed", nil); err == nil || !strings.Contains(err.Error(), "stop and restart") {
							t.Fatalf("failed rollback allowed reuse: %v", err)
						}
					}
				})
			}
		}
	}
}

func TestAutoModeRecoveryCloudSetupFencesAssignedID(t *testing.T) {
	client, server, requests := autoModeRecoveryClient(t, nil)
	entered, release := make(chan struct{}), make(chan struct{})
	releaseOnce := sync.OnceFunc(func() { close(release) })
	defer releaseOnce()
	server.SetRequestHandler("session.create", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		return []byte(`{"sessionId":"cloud-session","capabilities":{"ui":{"autoModeRecovery":true}}}`), nil
	})
	server.SetRequestHandler("session.eventLog.registerInterest", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		close(entered)
		<-release
		return nil, &jsonrpc2.Error{Code: -32603, Message: "MCP registration failed"}
	})
	ctx, cancel := context.WithTimeout(t.Context(), 5*time.Second)
	defer cancel()
	created := make(chan error, 1)
	go func() {
		_, err := client.CreateSessionWithAutoModeRecovery(ctx, &SessionConfig{
			Cloud: &CloudSessionOptions{},
			OnMCPAuthRequest: func(MCPAuthRequest, MCPAuthInvocation) (*MCPAuthResult, error) {
				return nil, nil
			},
		}, &AutoModeRecoveryOptions{
			OnAutoModeSwitchRequest: func(AutoModeSwitchRequestWithAutoModeRecovery, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
				return AutoModeSwitchResponseNo, nil
			},
		})
		created <- err
	}()
	select {
	case <-entered:
	case <-ctx.Done():
		t.Fatal("cloud creation did not reach MCP setup")
	}
	client.sessionsMux.Lock()
	owner := client.sessions["cloud-session"]
	client.sessionsMux.Unlock()
	resumed := make(chan error, 1)
	go func() {
		_, err := resumeAutoModeRecoverySession(ctx, client, "cloud-session", &ResumeSessionConfig{
			OnAutoModeSwitchRequest: func(AutoModeSwitchRequest, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
				return AutoModeSwitchResponseNo, nil
			},
		})
		resumed <- err
	}()
	for {
		client.sessionOperationsMux.Lock()
		operation := client.sessionOperations["cloud-session"]
		waiting := operation != nil && operation.users >= 2
		client.sessionOperationsMux.Unlock()
		earlyResume := false
		for _, request := range requests.snapshot() {
			earlyResume = earlyResume || request.Method == "session.resume"
		}
		if earlyResume {
			t.Fatal("resume overtook server-assigned cloud initialization")
		}
		if waiting {
			break
		}
		select {
		case <-ctx.Done():
			t.Fatal("resume did not reach admission")
		default:
			runtime.Gosched()
		}
	}
	client.sessionsMux.Lock()
	current := client.sessions["cloud-session"]
	client.sessionsMux.Unlock()
	if current != owner {
		t.Fatal("resume replaced the provisional cloud owner")
	}
	releaseOnce()
	if err := <-created; err == nil || !strings.Contains(err.Error(), "MCP registration failed") {
		t.Fatalf("cloud create error = %v", err)
	}
	if err := <-resumed; err == nil || !strings.Contains(err.Error(), "does not support") {
		t.Fatalf("recovery resume error = %v", err)
	}
	detaches, deletions := 0, 0
	for _, request := range requests.snapshot() {
		if request.Method == "session.detach" {
			detaches++
		}
		if request.Method == "session.delete" {
			deletions++
		}
	}
	if detaches != 2 || deletions != 1 {
		t.Fatalf("detaches = %d, deletions = %d, want cloud rollback and rejected resume cleanup", detaches, deletions)
	}
}

func TestAutoModeRecoveryPrefixLookupUsesConfiguredStorage(t *testing.T) {
	client, server, requests := autoModeRecoveryClient(t, &SessionCapabilitiesWithAutoModeRecovery{UI: &UICapabilitiesWithAutoModeRecovery{AutoModeRecovery: true}})
	canonical := "abcdef12-abcd-4123-8123-123456789abc"
	server.SetRequestHandler("sessions.findByPrefix", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var lookup rpc.SessionsFindByPrefixOptions
		if err := json.Unmarshal(params, &lookup); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
		}
		if lookup.ConfigDir == nil || *lookup.ConfigDir != "configured-home" {
			t.Errorf("lookup did not use configured storage: %s", params)
		}
		return json.RawMessage(`{"sessionId":"` + canonical + `"}`), nil
	})
	session, err := resumeAutoModeRecoverySession(t.Context(), client, "abcdef12", &ResumeSessionConfig{
		ConfigDirectory: "configured-home",
		OnAutoModeSwitchRequest: func(AutoModeSwitchRequest, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
			return AutoModeSwitchResponseNo, nil
		},
	})
	if err != nil {
		t.Fatal(err)
	}
	if session.SessionID != canonical {
		t.Fatalf("expected canonical ID %q, got %q", canonical, session.SessionID)
	}
	for _, request := range requests.snapshot() {
		if request.Method == "session.resume" && request.Params["configDir"] != "configured-home" {
			t.Fatalf("resume did not use configured storage: %+v", request.Params)
		}
	}
}

func TestAutoModeRecoverySessionIDsOverlapOnlyForUUIDAliases(t *testing.T) {
	canonical := "abcdef12-abcd-4123-8123-123456789abc"
	for _, test := range []struct {
		left, right string
		overlap     bool
	}{
		{"session-1", "session-1", true},
		{"session-1", "session-123", false},
		{"session-a", "SESSION-A", false},
		{"abcdef12", "abcdef12-custom", false},
		{"abcdef", canonical, false},
		{"abcdef12", canonical, true},
		{"ABCDEF12-ABCD", canonical, true},
		{strings.ToUpper(canonical), canonical, true},
	} {
		if actual := sessionIDsOverlap(test.left, test.right); actual != test.overlap {
			t.Errorf("overlap(%q, %q) = %v, want %v", test.left, test.right, actual, test.overlap)
		}
		if actual := sessionIDsOverlap(test.right, test.left); actual != test.overlap {
			t.Errorf("overlap(%q, %q) = %v, want %v", test.right, test.left, actual, test.overlap)
		}
	}
}

func TestAutoModeRecoveryResidentLookupPreservesCustomIDs(t *testing.T) {
	client := NewClient(nil)
	client.sessions["session-123"] = &Session{SessionID: "session-123"}
	if actual := client.residentSessionID("session-1"); actual != "session-1" {
		t.Fatalf("custom ID resolved to %q", actual)
	}
}

func TestAutoModeRecoveryEnrollment(t *testing.T) {
	for _, resume := range []bool{false, true} {
		name := "create"
		if resume {
			name = "resume"
		}
		t.Run(name, func(t *testing.T) {
			t.Run("requires-handler", func(t *testing.T) {
				client := NewClient(nil)
				_, err := openAutoModeRecoverySession(context.Background(), client, resume, true, nil)
				if err == nil || !strings.Contains(err.Error(), "requires OnAutoModeSwitchRequest") {
					t.Fatalf("expected configuration rejection, got %v", err)
				}
				if client.client != nil {
					t.Fatal("invalid enrollment started a transport")
				}
			})
			for _, caps := range []*SessionCapabilitiesWithAutoModeRecovery{nil, {}, {UI: &UICapabilitiesWithAutoModeRecovery{}}} {
				t.Run("unsupported", func(t *testing.T) {
					client, _, requests := autoModeRecoveryClient(t, caps)
					_, err := openAutoModeRecoverySessionWithRecovery(context.Background(), client, resume, true,
						func(AutoModeSwitchRequestWithAutoModeRecovery, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
							return AutoModeSwitchResponseNo, nil
						})
					if err == nil || !strings.Contains(err.Error(), "does not support AutoModeRecovery") {
						t.Fatalf("expected unsupported enrollment, got %v", err)
					}
					assertRequestMethod(t, requests.snapshot(), "session.detach")
					if len(client.sessions) != 0 {
						t.Fatal("unsupported session remains registered")
					}
				})
			}
			for _, enabled := range []bool{false, true} {
				t.Run("callback", func(t *testing.T) {
					caps := &SessionCapabilitiesWithAutoModeRecovery{UI: &UICapabilitiesWithAutoModeRecovery{AutoModeRecovery: enabled}}
					client, server, requests := autoModeRecoveryClient(t, caps)
					received := make(chan AutoModeSwitchRequestWithAutoModeRecovery, 1)
					session, err := openAutoModeRecoverySessionWithRecovery(context.Background(), client, resume, enabled,
						func(request AutoModeSwitchRequestWithAutoModeRecovery, invocation AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
							if invocation.SessionID == "" {
								t.Error("missing session identity")
							}
							received <- request
							return AutoModeSwitchResponseYes, nil
						})
					if err != nil {
						t.Fatal(err)
					}
					params := requests.snapshot()[0].Params
					if params["requestAutoModeSwitch"] != true || (params["requestAutoModeRecovery"] == true) != enabled {
						t.Fatalf("wrong enrollment: %+v", params)
					}
					if session.CapabilitiesWithAutoModeRecovery().UI.AutoModeRecovery != enabled {
						t.Fatalf("capability not surfaced: %+v", session.CapabilitiesWithAutoModeRecovery())
					}
					offer := AutoModeSwitchRequestWithAutoModeRecovery{
						AutoModeSwitchRequest: AutoModeSwitchRequest{ErrorCode: ptr("model_overloaded"), RetryAfterSeconds: ptr(3.5)},
					}
					if enabled {
						offer.AutoModeRecovery = &AutoModeRecoveryContext{
							RequestID: "offer-1", OriginalModel: "explicit-model", AllowAlways: false,
							InteractionID: ptr("interaction-1"), OriginatingMessageID: ptr("user-1"),
						}
					}
					ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
					defer cancel()
					response, err := server.Request(ctx, "autoModeSwitch.request", autoModeSwitchRequest{
						SessionID: session.SessionID, ErrorCode: offer.ErrorCode,
						RetryAfterSeconds: offer.RetryAfterSeconds, AutoModeRecovery: offer.AutoModeRecovery,
					})
					if err != nil {
						t.Fatal(err)
					}
					var result autoModeSwitchResponse
					if err := json.Unmarshal(response, &result); err != nil {
						t.Fatal(err)
					}
					if result.Response != AutoModeSwitchResponseYes || !reflect.DeepEqual(<-received, offer) {
						t.Fatalf("callback context or response lost: %s", response)
					}
				})
			}
		})
	}
}

func TestAutoModeRecoveryEventSubscriptionsPreserveLegacyPayloads(t *testing.T) {
	client, _, _, peer := autoModeRecoveryClientWithPeer(t, &SessionCapabilitiesWithAutoModeRecovery{
		UI: &UICapabilitiesWithAutoModeRecovery{AutoModeRecovery: true},
	})
	legacy := make(chan SessionEvent, 1)
	aware := make(chan SessionEvent, 1)
	session, err := client.CreateSessionWithAutoModeRecovery(t.Context(), &SessionConfig{
		OnEvent: func(event SessionEvent) {
			if event.ID == "recovery-capability-change" {
				legacy <- event
			}
		},
	}, &AutoModeRecoveryOptions{
		OnAutoModeSwitchRequest: func(AutoModeSwitchRequestWithAutoModeRecovery, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
			return AutoModeSwitchResponseNo, nil
		},
		OnEvent: func(event SessionEvent) {
			if event.ID == "recovery-capability-change" {
				aware <- event
			}
		},
	})
	if err != nil {
		t.Fatal(err)
	}
	ctx, cancel := context.WithTimeout(t.Context(), 5*time.Second)
	defer cancel()
	notification, err := json.Marshal(map[string]any{
		"jsonrpc": "2.0",
		"method":  "session.event",
		"params": map[string]any{
			"sessionId": session.SessionID,
			"event": map[string]any{
				"id":        "recovery-capability-change",
				"timestamp": "2026-11-05T00:00:00Z",
				"type":      "capabilities.changed",
				"data": map[string]any{"ui": map[string]any{
					"autoModeRecovery": false, "elicitation": true, "mcpApps": true,
				}},
			},
		},
	})
	if err != nil {
		t.Fatal(err)
	}
	deadline, _ := ctx.Deadline()
	if err := peer.SetWriteDeadline(deadline); err != nil {
		t.Fatal(err)
	}
	if _, err := fmt.Fprintf(peer, "Content-Length: %d\r\n\r\n%s", len(notification), notification); err != nil {
		t.Fatal(err)
	}
	if err := peer.SetWriteDeadline(time.Time{}); err != nil {
		t.Fatal(err)
	}
	select {
	case event := <-legacy:
		data, ok := event.Data.(*CapabilitiesChangedData)
		if !ok || data.UI == nil || data.UI.Elicitation == nil || !*data.UI.Elicitation ||
			data.UI.MCPApps == nil || !*data.UI.MCPApps {
			t.Fatalf("legacy subscriber received %T: %#v", event.Data, event.Data)
		}
	case <-ctx.Done():
		t.Fatal("legacy subscriber did not receive the capability change")
	}
	select {
	case event := <-aware:
		data, ok := event.Data.(*CapabilitiesChangedDataWithAutoModeRecovery)
		if !ok || data.UI == nil || data.UI.AutoModeRecovery == nil || *data.UI.AutoModeRecovery {
			t.Fatalf("recovery subscriber received %T: %#v", event.Data, event.Data)
		}
	case <-ctx.Done():
		t.Fatal("recovery subscriber did not receive the capability change")
	}
	caps := session.CapabilitiesWithAutoModeRecovery()
	if caps.UI == nil || caps.UI.AutoModeRecovery || !caps.UI.Elicitation || !caps.UI.MCPApps {
		t.Fatalf("capability delta was not applied: %#v", caps)
	}
}

func TestAutoModeRecoveryOfferDoesNotReachLegacyHandler(t *testing.T) {
	client, server, _ := autoModeRecoveryClient(t, nil)
	var called atomic.Bool
	session, err := client.CreateSession(t.Context(), &SessionConfig{
		OnAutoModeSwitchRequest: func(AutoModeSwitchRequest, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
			called.Store(true)
			return AutoModeSwitchResponseYes, nil
		},
	})
	if err != nil {
		t.Fatal(err)
	}
	_, err = server.Request(t.Context(), "autoModeSwitch.request", autoModeSwitchRequest{
		SessionID: session.SessionID,
		AutoModeRecovery: &AutoModeRecoveryContext{
			RequestID: "unexpected-recovery", OriginalModel: "explicit-model",
		},
	})
	if err == nil || !strings.Contains(err.Error(), "no recovery-aware") || called.Load() {
		t.Fatalf("unexpected offer reached the legacy handler: called=%v, error=%v", called.Load(), err)
	}
}

func TestAutoModeRecoveryComposesWithMCPAuthRuntimeRefresh(t *testing.T) {
	for _, resume := range []bool{false, true} {
		t.Run(fmt.Sprintf("resume-%t", resume), func(t *testing.T) {
			client, server, _, peer := autoModeRecoveryClientWithPeer(t, &SessionCapabilitiesWithAutoModeRecovery{
				UI: &UICapabilitiesWithAutoModeRecovery{AutoModeRecovery: true},
			})
			server.SetRequestHandler("session.eventLog.registerInterest", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				return []byte(`{}`), nil
			})
			handled := make(chan map[string]any, 1)
			server.SetRequestHandler("session.mcp.oauth.handlePendingRequest", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				var request map[string]any
				if err := json.Unmarshal(params, &request); err != nil {
					return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
				}
				handled <- request
				return []byte(`{}`), nil
			})
			options := &AutoModeRecoveryOptions{
				OnAutoModeSwitchRequest: func(AutoModeSwitchRequestWithAutoModeRecovery, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
					return AutoModeSwitchResponseNo, nil
				},
				MCPAuthRuntimeRefreshHandler: func(request MCPAuthRequest, invocation MCPAuthInvocation, supported bool) (*MCPAuthResult, error) {
					if request.RequestID != "refresh-request" || invocation.SessionID == "" || !supported {
						t.Errorf("refresh callback lost its context: %#v, %#v, %v", request, invocation, supported)
					}
					return MCPAuthResultRuntimeRefresh(), nil
				},
			}
			var session *Session
			var err error
			if resume {
				session, err = client.ResumeSessionWithAutoModeRecovery(t.Context(), "session-1", nil, options)
			} else {
				session, err = client.CreateSessionWithAutoModeRecovery(t.Context(), nil, options)
			}
			if err != nil {
				t.Fatal(err)
			}
			notification, err := json.Marshal(map[string]any{
				"jsonrpc": "2.0", "method": "session.event",
				"params": map[string]any{
					"sessionId": session.SessionID,
					"event": map[string]any{
						"id": "refresh-event", "timestamp": "2026-11-05T00:00:00Z",
						"type": "mcp.oauth_required",
						"data": map[string]any{
							"requestId": "refresh-request", "serverName": "test-mcp",
							"serverUrl": "https://mcp.example.test", "reason": "refresh",
							"runtimeRefreshSupported": true,
						},
					},
				},
			})
			if err != nil {
				t.Fatal(err)
			}
			ctx, cancel := context.WithTimeout(t.Context(), 5*time.Second)
			defer cancel()
			deadline, _ := ctx.Deadline()
			if err := peer.SetWriteDeadline(deadline); err != nil {
				t.Fatal(err)
			}
			if _, err := fmt.Fprintf(peer, "Content-Length: %d\r\n\r\n%s", len(notification), notification); err != nil {
				t.Fatal(err)
			}
			if err := peer.SetWriteDeadline(time.Time{}); err != nil {
				t.Fatal(err)
			}
			select {
			case request := <-handled:
				result, ok := request["result"].(map[string]any)
				if !ok || result["kind"] != "runtimeRefresh" || request["requestId"] != "refresh-request" {
					t.Fatalf("refresh handoff = %#v", request)
				}
			case <-ctx.Done():
				t.Fatal("MCP refresh handoff did not reach the runtime")
			}
		})
	}
}

func TestAutoModeRecoveryRejectedCloudCreateDeletesServerAssignedSession(t *testing.T) {
	for _, cleanupFailure := range []bool{false, true} {
		t.Run(fmt.Sprintf("cleanup-failure-%t", cleanupFailure), func(t *testing.T) {
			client, server, requests := autoModeRecoveryClient(t, nil)
			server.SetRequestHandler("session.create", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				var input map[string]any
				if err := json.Unmarshal(params, &input); err != nil {
					return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
				}
				if _, exists := input["sessionId"]; exists {
					t.Error("cloud create sent a client-assigned session ID")
				}
				requests.append(recordedRequest{Method: "session.create", Params: input})
				return []byte(`{"sessionId":"cloud-recovery-rejected"}`), nil
			})
			server.SetRequestHandler("session.delete", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				var input map[string]any
				if err := json.Unmarshal(params, &input); err != nil {
					return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
				}
				requests.append(recordedRequest{Method: "session.delete", Params: input})
				if cleanupFailure {
					return []byte(`{"success":false,"error":"cloud rollback rejected"}`), nil
				}
				return []byte(`{"success":true}`), nil
			})
			_, err := createAutoModeRecoverySession(t.Context(), client, &SessionConfig{
				Cloud: &CloudSessionOptions{
					Repository: &CloudSessionRepository{Owner: "github", Name: "copilot-sdk", Branch: "main"},
				},
				OnAutoModeSwitchRequest: func(AutoModeSwitchRequest, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
					return AutoModeSwitchResponseNo, nil
				},
			})
			if err == nil || !strings.Contains(err.Error(), "does not support AutoModeRecovery") {
				t.Fatalf("expected unsupported enrollment, got %v", err)
			}
			snapshot := requests.snapshot()
			if len(snapshot) != 3 || snapshot[1].Method != "session.detach" || snapshot[2].Method != "session.delete" {
				t.Fatalf("expected cloud create, detach, delete; got %+v", snapshot)
			}
			if snapshot[2].Params["sessionId"] != "cloud-recovery-rejected" {
				t.Fatalf("deleted wrong cloud session: %+v", snapshot[2])
			}
			if cleanupFailure && !strings.Contains(err.Error(), "cloud rollback rejected") {
				t.Fatalf("missing cloud rollback error: %v", err)
			}
			if cleanupFailure {
				if _, err := client.ResumeSession(t.Context(), "cloud-recovery-rejected", &ResumeSessionConfig{}); err == nil || !strings.Contains(err.Error(), "stop and restart") {
					t.Fatalf("same-ID resume bypassed failed cloud rollback: %v", err)
				}
				if _, err := client.CreateSession(t.Context(), &SessionConfig{SessionID: "cloud-recovery-rejected"}); err == nil || !strings.Contains(err.Error(), "stop and restart") {
					t.Fatalf("same-ID create bypassed failed cloud rollback: %v", err)
				}
			}
			client.sessionsMux.Lock()
			activeSessions := len(client.sessions)
			client.sessionsMux.Unlock()
			if activeSessions != 0 {
				t.Fatalf("rejected create left %d active sessions", activeSessions)
			}
			if cleanupFailure {
				freshClient, _, _ := autoModeRecoveryClient(t, nil)
				client.transportMux.Lock()
				oldTransport := client.client
				client.client = freshClient.client
				client.transportMux.Unlock()
				defer func() {
					client.transportMux.Lock()
					client.client = oldTransport
					client.transportMux.Unlock()
				}()
				replacement, err := client.ResumeSession(t.Context(), "cloud-recovery-rejected", &ResumeSessionConfig{})
				if err != nil {
					t.Fatalf("old cleanup fence leaked into new connection: %v", err)
				}
				if err := replacement.Disconnect(); err != nil {
					t.Fatalf("replacement cleanup: %v", err)
				}
				client.sessionsMux.Lock()
				delete(client.sessions, replacement.SessionID)
				client.sessionsMux.Unlock()
			}
		})
	}
}

func TestAutoModeRecoveryCancelledCloudCreateKeepsReuseBehindDeletion(t *testing.T) {
	client, server, _ := autoModeRecoveryClient(t, nil)
	deleteStarted := make(chan struct{})
	releaseDelete := make(chan struct{})
	release := sync.OnceFunc(func() { close(releaseDelete) })
	defer release()
	var deleteFinished atomic.Bool
	server.SetRequestHandler("session.create", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var input map[string]any
		if err := json.Unmarshal(params, &input); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
		}
		if sessionID, ok := input["sessionId"]; ok {
			output, err := json.Marshal(map[string]any{"sessionId": sessionID})
			if err != nil {
				return nil, &jsonrpc2.Error{Code: -32603, Message: err.Error()}
			}
			return output, nil
		}
		return []byte(`{"sessionId":"cloud-recovery-rejected"}`), nil
	})
	server.SetRequestHandler("session.delete", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		close(deleteStarted)
		<-releaseDelete
		deleteFinished.Store(true)
		return []byte(`{"success":true}`), nil
	})
	server.SetRequestHandler("session.resume", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		if !deleteFinished.Load() {
			t.Error("same-ID resume overtook cloud rollback")
		}
		return []byte(`{"sessionId":"cloud-recovery-rejected"}`), nil
	})
	ctx, cancel := context.WithCancel(t.Context())
	defer cancel()
	created := make(chan error, 1)
	go func() {
		_, err := createAutoModeRecoverySession(ctx, client, &SessionConfig{
			Cloud: &CloudSessionOptions{
				Repository: &CloudSessionRepository{Owner: "github", Name: "copilot-sdk", Branch: "main"},
			},
			OnAutoModeSwitchRequest: func(AutoModeSwitchRequest, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
				return AutoModeSwitchResponseNo, nil
			},
		})
		created <- err
	}()
	deadline, stop := context.WithTimeout(t.Context(), 5*time.Second)
	defer stop()
	select {
	case <-deleteStarted:
	case <-deadline.Done():
		t.Fatal("cloud rollback did not reach deletion")
	}
	cancel()
	select {
	case err := <-created:
		if !errors.Is(err, context.Canceled) {
			t.Fatalf("expected caller cancellation, got %v", err)
		}
	case <-deadline.Done():
		t.Fatal("cloud create did not stop waiting after cancellation")
	}
	reused := make(chan error, 1)
	go func() {
		_, err := client.ResumeSession(deadline, "cloud-recovery-rejected", &ResumeSessionConfig{})
		reused <- err
	}()
	if _, err := client.CreateSession(deadline, &SessionConfig{SessionID: "unrelated"}); err != nil {
		t.Fatalf("cloud rollback blocked unrelated ID: %v", err)
	}
	release()
	select {
	case err := <-reused:
		if err != nil {
			t.Fatalf("same-ID resume after rollback: %v", err)
		}
	case <-deadline.Done():
		t.Fatal("same-ID resume did not finish after cloud rollback")
	}
}

func TestAutoModeRecoveryUnsupportedEnrollmentDoesNotWaitForDetach(t *testing.T) {
	for _, resume := range []bool{false, true} {
		operation := "create"
		if resume {
			operation = "resume"
		}
		for _, cancelRequest := range []bool{true, false} {
			boundary := "cleanup-timeout"
			if cancelRequest {
				boundary = "caller-cancellation"
			}
			t.Run(operation+"/"+boundary, func(t *testing.T) {
				t.Parallel()
				client, server, _ := autoModeRecoveryClient(t, nil)
				detachStarted := make(chan string, 1)
				releaseDetach := make(chan struct{})
				server.SetRequestHandler("session.detach", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
					var request sessionDetachRequest
					if err := json.Unmarshal(params, &request); err != nil {
						return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
					}
					detachStarted <- request.SessionID
					// Keep the peer connected but do not acknowledge cleanup.
					<-releaseDetach
					return []byte(`{"success":true}`), nil
				})
				ctx, cancel := context.WithCancel(t.Context())
				defer cancel()
				rejected := make(chan error, 1)
				finished := make(chan struct{})
				go func() {
					defer close(finished)
					_, err := openAutoModeRecoverySession(ctx, client, resume, true,
						func(AutoModeSwitchRequest, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
							return AutoModeSwitchResponseNo, nil
						})
					rejected <- err
				}()
				var releaseOnce sync.Once
				release := func() { releaseOnce.Do(func() { close(releaseDetach) }) }
				t.Cleanup(func() {
					release()
					select {
					case <-finished:
					case <-time.After(5 * time.Second):
						t.Error("enrollment did not finish after releasing the detach response")
					}
				})
				var sessionID string
				select {
				case sessionID = <-detachStarted:
				case <-time.After(5 * time.Second):
					t.Fatal("unsupported enrollment did not attempt detach")
				}
				client.sessionsMux.Lock()
				session := client.sessions[sessionID]
				client.sessionsMux.Unlock()
				if session == nil {
					t.Fatal("missing session before detach completed")
				}
				wait := 15 * time.Second
				expected := context.DeadlineExceeded
				if cancelRequest {
					cancel()
					wait = 5 * time.Second
					expected = context.Canceled
				}
				select {
				case err := <-rejected:
					if err == nil || !strings.Contains(err.Error(), "does not support AutoModeRecovery") || !errors.Is(err, expected) {
						t.Fatalf("expected unsupported enrollment and %v, got %v", expected, err)
					}
				case <-time.After(wait):
					t.Fatal("unsupported enrollment is still waiting for an unresponsive detach")
				}
				client.sessionsMux.Lock()
				remaining := len(client.sessions)
				client.sessionsMux.Unlock()
				if cancelRequest {
					if remaining != 1 {
						t.Fatalf("pending cleanup lost its callback route: %d sessions", remaining)
					}
					release()
					if err := client.waitForAutoModeRecoveryCleanup(t.Context(), sessionID); err != nil {
						t.Fatal(err)
					}
					client.sessionsMux.Lock()
					remaining = len(client.sessions)
					client.sessionsMux.Unlock()
				}
				if remaining != 0 {
					t.Fatalf("unsupported enrollment left %d registered sessions", remaining)
				}
				select {
				case <-session.eventDone:
				default:
					t.Fatal("unsupported enrollment did not retire its event consumer")
				}
				session.autoModeSwitchMu.Lock()
				retainedHandler := session.autoModeSwitchHandler != nil || session.autoModeSwitchRecoveryHandler != nil
				session.autoModeSwitchMu.Unlock()
				if retainedHandler {
					t.Fatal("unsupported enrollment retained its consent handler")
				}
			})
		}
	}
}

func TestAutoModeRecoveryCapabilityChanges(t *testing.T) {
	session, cleanup := newTestSession()
	defer cleanup()
	session.setCapabilities(&SessionCapabilitiesWithAutoModeRecovery{UI: &UICapabilitiesWithAutoModeRecovery{
		UICapabilities: UICapabilities{Elicitation: true}, AutoModeRecovery: true,
	}})
	session.dispatchEvent(SessionEvent{Data: &CapabilitiesChangedData{
		UI: &CapabilitiesChangedUI{Elicitation: ptr(false)},
	}})
	if caps := session.CapabilitiesWithAutoModeRecovery(); !caps.UI.AutoModeRecovery || caps.UI.Elicitation {
		t.Fatalf("partial update dropped recovery: %+v", caps.UI)
	}

	session.dispatchEvent(SessionEvent{Data: &CapabilitiesChangedDataWithAutoModeRecovery{
		UI: &CapabilitiesChangedUIWithAutoModeRecovery{AutoModeRecovery: ptr(false)},
	}})
	if session.CapabilitiesWithAutoModeRecovery().UI.AutoModeRecovery {
		t.Fatal("revocation was lost")
	}
}

func TestAutoModeRecoveryRejectedResumePreservesResidentSession(t *testing.T) {
	client, server, requests := autoModeRecoveryClient(t, nil)
	resident, err := openAutoModeRecoverySession(t.Context(), client, false, false,
		func(AutoModeSwitchRequest, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
			return AutoModeSwitchResponseYesAlways, nil
		})
	if err != nil {
		t.Fatal(err)
	}
	server.SetRequestHandler("session.resume", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		return nil, &jsonrpc2.Error{Code: -32000, Message: "resume rejected"}
	})
	_, err = resumeAutoModeRecoverySession(t.Context(), client, resident.SessionID, &ResumeSessionConfig{
		OnAutoModeSwitchRequest: func(AutoModeSwitchRequest, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
			return AutoModeSwitchResponseNo, nil
		},
	})
	if err == nil || !strings.Contains(err.Error(), "resume rejected") {
		t.Fatalf("expected resume RPC rejection, got %v", err)
	}
	for _, request := range requests.snapshot() {
		if request.Method == "session.detach" {
			t.Fatal("rejected resume detached the resident session")
		}
	}
	client.sessionsMux.Lock()
	current := client.sessions[resident.SessionID]
	client.sessionsMux.Unlock()
	if current != resident {
		t.Fatal("rejected resume did not restore the resident callback owner")
	}
	ctx, cancel := context.WithTimeout(t.Context(), 5*time.Second)
	defer cancel()
	reply, err := server.Request(ctx, "autoModeSwitch.request", autoModeSwitchRequest{
		SessionID: resident.SessionID, ErrorCode: ptr("user_weekly_rate_limited"),
	})
	if err != nil {
		t.Fatal(err)
	}
	var response autoModeSwitchResponse
	if err := json.Unmarshal(reply, &response); err != nil {
		t.Fatal(err)
	}
	if response.Response != AutoModeSwitchResponseYesAlways {
		t.Fatalf("resident handler lost after rejection: %s", reply)
	}
}

func TestAutoModeRecoveryAbbreviatedResumeOwnership(t *testing.T) {
	for _, resident := range []bool{false, true} {
		for _, rpcRejected := range []bool{false, true} {
			t.Run(fmt.Sprintf("resident=%t/rpc-rejected=%t", resident, rpcRejected), func(t *testing.T) {
				client, server, requests := autoModeRecoveryClient(t, nil)
				const canonicalID = "abcdef12-3456-4789-abcd-0123456789ab"
				mockSessionPrefixLookup(t, server, canonicalID)
				var previous *Session
				var err error
				if resident {
					previous, err = client.CreateSession(t.Context(), &SessionConfig{
						SessionID: canonicalID,
						OnAutoModeSwitchRequest: func(AutoModeSwitchRequest, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
							return AutoModeSwitchResponseYesAlways, nil
						},
					})
					if err != nil {
						t.Fatal(err)
					}
					t.Cleanup(previous.stopEventProcessing)
				}
				server.SetRequestHandler("session.resume", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
					if rpcRejected {
						return nil, &jsonrpc2.Error{Code: -32000, Message: "resume rejected"}
					}
					return []byte(`{"sessionId":"abcdef12-3456-4789-abcd-0123456789ab"}`), nil
				})
				callbacks := make(chan string, 1)
				server.SetRequestHandler("session.detach", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
					var input map[string]any
					if err := json.Unmarshal(params, &input); err != nil {
						return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
					}
					requests.append(recordedRequest{Method: "session.detach", Params: input})
					reply, err := server.Request(t.Context(), "autoModeSwitch.request", autoModeSwitchRequest{
						SessionID: canonicalID, ErrorCode: ptr("user_weekly_rate_limited"),
					})
					if err != nil {
						t.Error(err)
					} else if !strings.Contains(string(reply), `"yes"`) {
						t.Errorf("canonical cleanup callback was lost: %s", reply)
					}
					return []byte(`{"success":true}`), nil
				})
				t.Cleanup(func() {
					server.SetRequestHandler("session.detach", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
						return []byte(`{"success":true}`), nil
					})
				})
				_, err = resumeAutoModeRecoverySession(t.Context(), client, "abcdef12", &ResumeSessionConfig{
					OnAutoModeSwitchRequest: func(_ AutoModeSwitchRequest, invocation AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
						callbacks <- invocation.SessionID
						return AutoModeSwitchResponseYes, nil
					},
				})
				expectedError := "does not support AutoModeRecovery"
				if rpcRejected {
					expectedError = "resume rejected"
				}
				if err == nil || !strings.Contains(err.Error(), expectedError) {
					t.Fatalf("expected %q, got %v", expectedError, err)
				}
				detaches := 0
				for _, request := range requests.snapshot() {
					if request.Method == "session.delete" {
						t.Fatal("resume deleted a persisted session")
					}
					if request.Method == "session.detach" {
						detaches++
						if request.Params["sessionId"] != canonicalID {
							t.Errorf("detached abbreviated ID: %+v", request.Params)
						}
					}
				}
				expectedDetaches := 0
				if !resident && !rpcRejected {
					expectedDetaches = 1
				}
				if detaches != expectedDetaches {
					t.Fatalf("expected %d detaches, got %d", expectedDetaches, detaches)
				}
				client.sessionsMux.Lock()
				current := client.sessions[canonicalID]
				alias := client.sessions["abcdef12"]
				client.sessionsMux.Unlock()
				if alias != nil {
					t.Fatal("abbreviated callback owner remains registered")
				}
				if rpcRejected {
					if current != previous {
						t.Fatal("RPC rejection did not restore the canonical owner")
					}
				} else if resident {
					if current == nil || current == previous {
						t.Fatal("accepted canonical callback owner was not retained")
					}
					reply, err := server.Request(t.Context(), "autoModeSwitch.request", autoModeSwitchRequest{
						SessionID: canonicalID, ErrorCode: ptr("user_weekly_rate_limited"),
					})
					if err != nil || !strings.Contains(string(reply), `"yes"`) {
						t.Fatalf("accepted canonical callback was lost: %s, %v", reply, err)
					}
				} else if current != nil {
					t.Fatal("new rejected attachment remains registered")
				}
				if !rpcRejected {
					select {
					case invocationID := <-callbacks:
						if invocationID != canonicalID {
							t.Fatalf("callback invocation used abbreviated identity: %s", invocationID)
						}
					default:
						t.Fatal("canonical callback was not invoked")
					}
				}
			})
		}
	}
}

func TestResumeUUIDPrefixCallbackOwnership(t *testing.T) {
	const canonicalID = "abcdef12-3456-4789-abcd-0123456789ab"
	for _, resident := range []bool{false, true} {
		for _, outcome := range []string{"accepted", "lookup-rejected", "resume-rejected", "ambiguous"} {
			t.Run(fmt.Sprintf("resident=%t/%s", resident, outcome), func(t *testing.T) {
				client, server, requests := autoModeRecoveryClient(t, &SessionCapabilitiesWithAutoModeRecovery{UI: &UICapabilitiesWithAutoModeRecovery{AutoModeRecovery: true}})
				ctx, cancel := context.WithTimeout(t.Context(), 5*time.Second)
				defer cancel()
				var previous *Session
				if resident {
					var err error
					previous, err = client.CreateSession(ctx, &SessionConfig{
						SessionID: canonicalID,
						OnAutoModeSwitchRequest: func(AutoModeSwitchRequest, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
							return AutoModeSwitchResponseYesAlways, nil
						},
					})
					if err != nil {
						t.Fatal(err)
					}
					t.Cleanup(previous.stopEventProcessing)
				}
				checkCallback := func(expected string) {
					t.Helper()
					reply, err := server.Request(ctx, "autoModeSwitch.request", autoModeSwitchRequest{
						SessionID: canonicalID, ErrorCode: ptr("user_weekly_rate_limited"),
					})
					if expected == "" {
						if err == nil || !strings.Contains(err.Error(), "unknown session") {
							t.Errorf("expected no canonical callback owner, got %s, %v", reply, err)
						}
					} else if err != nil || !strings.Contains(string(reply), `"response":"`+expected+`"`) {
						t.Errorf("expected canonical callback %q, got %s, %v", expected, reply, err)
					}
				}
				residentResponse := ""
				if resident {
					residentResponse = "yes_always"
				}
				var lookups, resumes atomic.Int32
				server.SetRequestHandler("sessions.findByPrefix", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
					lookups.Add(1)
					var input rpc.SessionsFindByPrefixRequest
					if err := json.Unmarshal(params, &input); err != nil {
						return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
					}
					if input.Prefix != "ABCDEF12-3456" {
						t.Errorf("unexpected prefix: %q", input.Prefix)
					}
					checkCallback(residentResponse)
					if outcome == "lookup-rejected" {
						return nil, &jsonrpc2.Error{Code: -32601, Message: "lookup rejected"}
					}
					if outcome == "ambiguous" {
						// Another persisted session shares this prefix but is not in the local map.
						return []byte(`{}`), nil
					}
					return []byte(`{"sessionId":"abcdef12-3456-4789-abcd-0123456789ab"}`), nil
				})
				server.SetRequestHandler("session.resume", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
					resumes.Add(1)
					var input resumeSessionRequest
					if err := json.Unmarshal(params, &input); err != nil {
						return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
					}
					if outcome == "ambiguous" {
						checkCallback(residentResponse)
						return nil, &jsonrpc2.Error{Code: -32000, Message: "ambiguous prefix"}
					}
					checkCallback("yes")
					if input.SessionID != canonicalID {
						t.Errorf("resume did not pin the resolved full ID: %q", input.SessionID)
					}
					if outcome == "resume-rejected" {
						return nil, &jsonrpc2.Error{Code: -32000, Message: "resume rejected"}
					}
					return []byte(`{"sessionId":"abcdef12-3456-4789-abcd-0123456789ab","capabilities":{"ui":{"autoModeRecovery":true}}}`), nil
				})
				session, err := resumeAutoModeRecoverySession(ctx, client, "ABCDEF12-3456", &ResumeSessionConfig{
					OnAutoModeSwitchRequest: func(_ AutoModeSwitchRequest, invocation AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
						if invocation.SessionID != canonicalID {
							t.Errorf("callback used alias identity: %q", invocation.SessionID)
						}
						return AutoModeSwitchResponseYes, nil
					},
				})
				if session != nil {
					t.Cleanup(session.stopEventProcessing)
				}
				expectedError := map[string]string{
					"lookup-rejected": "lookup rejected", "resume-rejected": "resume rejected",
					"ambiguous": "no unique session",
				}[outcome]
				if outcome == "accepted" {
					if err != nil {
						t.Fatal(err)
					}
					if session.SessionID != canonicalID {
						t.Errorf("returned alias identity: %q", session.SessionID)
					}
					checkCallback("yes")
				} else {
					if err == nil || !strings.Contains(err.Error(), expectedError) {
						t.Fatalf("expected %q, got %v", expectedError, err)
					}
					checkCallback(residentResponse)
				}
				expectedResumes := int32(1)
				if outcome == "lookup-rejected" || outcome == "ambiguous" {
					expectedResumes = 0
				}
				if lookups.Load() != 1 || resumes.Load() != expectedResumes {
					t.Errorf("lookups=%d resumes=%d, expected 1/%d",
						lookups.Load(), resumes.Load(), expectedResumes)
				}
				client.sessionsMux.Lock()
				current, alias := client.sessions[canonicalID], client.sessions["ABCDEF12-3456"]
				client.sessionsMux.Unlock()
				if alias != nil || (outcome != "accepted" && current != previous) {
					t.Error("failed resume did not restore the exact resident owner")
				}
				for _, request := range requests.snapshot() {
					if request.Method == "session.detach" || request.Method == "session.delete" {
						t.Errorf("failed prefix resolution detached or deleted a persisted session: %+v", request)
					}
				}
			})
		}
	}
}

func TestResumeSessionPrefixLookupGuard(t *testing.T) {
	const canonicalID = "abcdef12-3456-4789-abcd-0123456789ab"
	for _, test := range []struct {
		sessionID string
		lookup    bool
	}{
		{"abcdef1", true},
		{"ABCDEF12", true},
		{"abcdef12-3456", true},
		{canonicalID[:35], true},
		{"abcdef", false},
		{"abc-def1", false},
		{"abcdefg", false},
		{"abcdeｆ1", false},
		{"abcdef1é", false},
		{"deadbeef_task1", false},
		{"session-123", false},
		{canonicalID, false},
		{strings.ToUpper(canonicalID), false},
		{canonicalID + "0", false},
	} {
		t.Run(test.sessionID, func(t *testing.T) {
			client, server, _ := autoModeRecoveryClient(t, &SessionCapabilitiesWithAutoModeRecovery{UI: &UICapabilitiesWithAutoModeRecovery{AutoModeRecovery: true}})
			var lookups atomic.Int32
			server.SetRequestHandler("sessions.findByPrefix", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				lookups.Add(1)
				var input rpc.SessionsFindByPrefixRequest
				if err := json.Unmarshal(params, &input); err != nil {
					return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
				}
				if input.Prefix != test.sessionID {
					t.Errorf("lookup changed the requested prefix: %q", input.Prefix)
				}
				return []byte(`{"sessionId":"abcdef12-3456-4789-abcd-0123456789ab"}`), nil
			})
			expectedID, expectedLookups := test.sessionID, int32(0)
			if test.lookup {
				expectedID, expectedLookups = canonicalID, 1
			}
			server.SetRequestHandler("session.resume", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				var input resumeSessionRequest
				if err := json.Unmarshal(params, &input); err != nil {
					return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
				}
				if input.SessionID != expectedID {
					t.Errorf("resume identity: expected %q, got %q", expectedID, input.SessionID)
				}
				result, err := json.Marshal(map[string]any{
					"sessionId":    input.SessionID,
					"capabilities": map[string]any{"ui": map[string]bool{"autoModeRecovery": true}},
				})
				if err != nil {
					return nil, &jsonrpc2.Error{Code: -32603, Message: err.Error()}
				}
				return result, nil
			})
			session, err := resumeAutoModeRecoverySession(t.Context(), client, test.sessionID, &ResumeSessionConfig{
				OnAutoModeSwitchRequest: func(AutoModeSwitchRequest, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
					return AutoModeSwitchResponseNo, nil
				},
			})
			if err != nil {
				t.Fatal(err)
			}
			t.Cleanup(session.stopEventProcessing)
			if session.SessionID != expectedID || lookups.Load() != expectedLookups {
				t.Errorf("session ID=%q, lookups=%d; expected ID=%q, lookups=%d",
					session.SessionID, lookups.Load(), expectedID, expectedLookups)
			}
		})
	}
}

func TestExactHexResumeSkipsHostLookup(t *testing.T) {
	for _, test := range []struct{ provider, resident bool }{{false, false}, {true, false}, {true, true}} {
		t.Run(fmt.Sprintf("provider=%t/resident=%t", test.provider, test.resident), func(t *testing.T) {
			provider := test.provider
			client, server, requests := autoModeRecoveryClient(t, &SessionCapabilitiesWithAutoModeRecovery{UI: &UICapabilitiesWithAutoModeRecovery{AutoModeRecovery: true}})
			var previous *Session
			if test.resident {
				var err error
				previous, err = client.CreateSession(t.Context(), &SessionConfig{
					SessionID: "abcdef0",
					OnAutoModeSwitchRequest: func(AutoModeSwitchRequest, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
						return AutoModeSwitchResponseYesAlways, nil
					},
				})
				if err != nil {
					t.Fatal(err)
				}
				t.Cleanup(previous.stopEventProcessing)
			}
			server.SetRequestHandler("sessions.findByPrefix", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				t.Error("unexpected host lookup")
				return nil, &jsonrpc2.Error{Code: -32601, Message: "legacy lookup"}
			})
			config := &ResumeSessionConfig{ConfigDirectory: "configured-home"}
			if provider {
				client.options.SessionFS = &SessionFSConfig{
					InitialWorkingDirectory: "/workspace", SessionStatePath: "/sessions",
					Conventions: rpc.SessionFSSetProviderConventionsPosix,
				}
				config.OnAutoModeSwitchRequest = func(AutoModeSwitchRequest, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
					return AutoModeSwitchResponseYes, nil
				}
				config.CreateSessionFSProvider = func(session *Session) SessionFSProvider {
					if session.SessionID != "abcdef0" {
						t.Errorf("provider ID: %q", session.SessionID)
					}
					client.sessionsMux.Lock()
					current := client.sessions["abcdef0"]
					client.sessionsMux.Unlock()
					if current != previous {
						t.Error("provider setup replaced the resident callback owner before RPC")
					}
					return reentrySessionFSProvider{}
				}
			}
			var session *Session
			var err error
			if provider {
				session, err = resumeAutoModeRecoverySession(t.Context(), client, "abcdef0", config)
			} else {
				session, err = client.ResumeSession(t.Context(), "abcdef0", config)
			}
			if err != nil {
				t.Fatal(err)
			}
			t.Cleanup(session.stopEventProcessing)
			if session.SessionID != "abcdef0" {
				t.Errorf("resume ID: %q", session.SessionID)
			}
			for _, request := range requests.snapshot() {
				if request.Method == "sessions.findByPrefix" {
					t.Fatal("unexpected host lookup")
				}
				if request.Method == "session.resume" && (request.Params["sessionId"] != "abcdef0" || request.Params["configDir"] != "configured-home") {
					t.Errorf("resume changed exact ID/configured storage: %+v", request.Params)
				}
			}
		})
	}
}

func TestResumePrefixLookupCancellationReleasesCanonicalAdmission(t *testing.T) {
	client, server, _ := autoModeRecoveryClient(t, nil)
	const canonicalID = "abcdef12-3456-4789-abcd-0123456789ab"
	ctx, cancel := context.WithTimeout(t.Context(), 5*time.Second)
	defer cancel()
	entered, releaseLookup := make(chan struct{}), make(chan struct{})
	release := sync.OnceFunc(func() { close(releaseLookup) })
	defer release()
	server.SetRequestHandler("sessions.findByPrefix", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		close(entered)
		<-releaseLookup
		return []byte(`{"sessionId":"abcdef12-3456-4789-abcd-0123456789ab"}`), nil
	})
	var resumeCalls atomic.Int32
	server.SetRequestHandler("session.resume", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		resumeCalls.Add(1)
		return nil, &jsonrpc2.Error{Code: -32000, Message: "cancelled lookup must not resume"}
	})
	lookupCtx, cancelLookup := context.WithCancel(ctx)
	defer cancelLookup()
	resumed := make(chan error, 1)
	go func() {
		_, err := resumeAutoModeRecoverySession(lookupCtx, client, "abcdef12", &ResumeSessionConfig{
			OnAutoModeSwitchRequest: func(AutoModeSwitchRequest, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
				return AutoModeSwitchResponseNo, nil
			},
		})
		resumed <- err
	}()
	select {
	case <-entered:
	case <-ctx.Done():
		t.Fatal("prefix lookup did not reach the runtime")
	}
	created := make(chan *Session, 1)
	createErrors := make(chan error, 1)
	go func() {
		session, err := client.CreateSession(ctx, &SessionConfig{SessionID: canonicalID})
		created <- session
		createErrors <- err
	}()
	for {
		client.sessionOperationsMux.Lock()
		queued := false
		for _, operation := range client.sessionOperations {
			queued = queued || operation.users == 2
		}
		client.sessionOperationsMux.Unlock()
		if queued {
			break
		}
		select {
		case <-created:
			t.Fatal("canonical initialization overtook prefix lookup")
		case <-ctx.Done():
			t.Fatal("canonical initialization did not reach admission")
		default:
			runtime.Gosched()
		}
	}
	cancelLookup()
	select {
	case err := <-resumed:
		if !errors.Is(err, context.Canceled) {
			t.Fatalf("expected cancelled lookup, got %v", err)
		}
	case <-ctx.Done():
		t.Fatal("cancelled prefix lookup did not settle")
	}
	select {
	case session := <-created:
		if err := <-createErrors; err != nil {
			t.Fatal(err)
		}
		t.Cleanup(session.stopEventProcessing)
		client.sessionsMux.Lock()
		current, alias := client.sessions[canonicalID], client.sessions["abcdef12"]
		client.sessionsMux.Unlock()
		if current != session || alias != nil || resumeCalls.Load() != 0 {
			t.Fatal("cancelled prefix lookup published callbacks or resumed a session")
		}
	case <-ctx.Done():
		t.Fatal("cancelled prefix lookup retained canonical admission")
	}
}

func TestAutoModeRecoveryAbbreviatedResumeFencesCanonicalInitialization(t *testing.T) {
	client, server, _ := autoModeRecoveryClient(t, &SessionCapabilitiesWithAutoModeRecovery{UI: &UICapabilitiesWithAutoModeRecovery{AutoModeRecovery: true}})
	const canonicalID = "abcdef12-3456-4789-abcd-0123456789ab"
	mockSessionPrefixLookup(t, server, canonicalID)
	patchEntered := make(chan struct{})
	releasePatch := make(chan struct{})
	release := sync.OnceFunc(func() { close(releasePatch) })
	defer release()
	server.SetRequestHandler("session.resume", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		return []byte(`{"sessionId":"abcdef12-3456-4789-abcd-0123456789ab","capabilities":{"ui":{"autoModeRecovery":true}}}`), nil
	})
	server.SetRequestHandler("session.options.update", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var input map[string]any
		if err := json.Unmarshal(params, &input); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
		}
		if input["sessionId"] != canonicalID {
			t.Errorf("options patch used abbreviated identity: %+v", input)
		}
		close(patchEntered)
		<-releasePatch
		return []byte(`{"success":true}`), nil
	})
	ctx, cancel := context.WithTimeout(t.Context(), 5*time.Second)
	defer cancel()
	resumed := make(chan error, 1)
	go func() {
		session, err := resumeAutoModeRecoverySession(ctx, client, "abcdef12", &ResumeSessionConfig{
			SkipCustomInstructions: Bool(true),
			OnAutoModeSwitchRequest: func(AutoModeSwitchRequest, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
				return AutoModeSwitchResponseYes, nil
			},
		})
		if session != nil {
			t.Cleanup(session.stopEventProcessing)
		}
		resumed <- err
	}()
	select {
	case <-patchEntered:
	case <-ctx.Done():
		t.Fatal("abbreviated resume did not reach options patch")
	}
	created := make(chan struct{}, 1)
	server.SetRequestHandler("session.create", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		created <- struct{}{}
		return []byte(`{"sessionId":"abcdef12-3456-4789-abcd-0123456789ab","capabilities":{"ui":{"autoModeRecovery":true}}}`), nil
	})
	reused := make(chan error, 1)
	go func() {
		_, err := client.CreateSession(ctx, &SessionConfig{SessionID: canonicalID})
		reused <- err
	}()
	for {
		client.sessionOperationsMux.Lock()
		queued := false
		for _, operation := range client.sessionOperations {
			queued = queued || operation.users == 2
		}
		client.sessionOperationsMux.Unlock()
		if queued {
			break
		}
		select {
		case <-created:
			t.Fatal("canonical create overtook abbreviated resume setup")
		case <-ctx.Done():
			t.Fatal("canonical create never reached admission")
		default:
			runtime.Gosched()
		}
	}
	release()
	if err := <-resumed; err != nil {
		t.Fatal(err)
	}
	if err := <-reused; err != nil {
		t.Fatal(err)
	}
}

func TestAutoModeRecoveryInFlightAliasCannotBeDetached(t *testing.T) {
	for _, test := range []struct {
		recoveryFirst bool
		aliasID       string
	}{
		{false, "abcdef12"}, {true, "abcdef12"},
		{false, "ABCDEF12"}, {true, "ABCDEF12"},
	} {
		t.Run(fmt.Sprintf("recovery-first=%t/alias=%s", test.recoveryFirst, test.aliasID), func(t *testing.T) {
			recoveryFirst := test.recoveryFirst
			client, server, _ := autoModeRecoveryClient(t, nil)
			const canonicalID = "abcdef12-3456-4789-abcd-0123456789ab"
			mockSessionPrefixLookup(t, server, canonicalID)
			ctx, cancel := context.WithTimeout(t.Context(), 5*time.Second)
			defer cancel()
			ordinaryEntered := make(chan struct{})
			recoveryEntered := make(chan struct{})
			releaseFirst := make(chan struct{})
			release := sync.OnceFunc(func() { close(releaseFirst) })
			defer release()
			var attached atomic.Bool
			server.SetRequestHandler("session.resume", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				var input struct {
					SessionID               string `json:"sessionId"`
					RequestAutoModeRecovery bool   `json:"requestAutoModeRecovery"`
				}
				if err := json.Unmarshal(params, &input); err != nil {
					return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
				}
				attached.Store(true)
				recovery := input.RequestAutoModeRecovery
				expectedID := test.aliasID
				if recovery {
					expectedID = canonicalID
				}
				if input.SessionID != expectedID {
					t.Errorf("resume identity: expected %q, got %q", expectedID, input.SessionID)
				}
				if recovery {
					close(recoveryEntered)
				} else {
					close(ordinaryEntered)
				}
				if recovery == recoveryFirst {
					<-releaseFirst
				}
				return []byte(`{"sessionId":"abcdef12-3456-4789-abcd-0123456789ab"}`), nil
			})
			server.SetRequestHandler("session.detach", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				attached.Store(false)
				return []byte(`{"success":true}`), nil
			})
			ordinaryResult := make(chan *Session, 1)
			ordinaryError := make(chan error, 1)
			startOrdinary := func() {
				go func() {
					session, err := client.ResumeSession(ctx, test.aliasID, &ResumeSessionConfig{})
					ordinaryResult <- session
					ordinaryError <- err
				}()
			}
			recoveryResult := make(chan error, 1)
			startRecovery := func() {
				go func() {
					_, err := resumeAutoModeRecoverySession(ctx, client, canonicalID, &ResumeSessionConfig{
						OnAutoModeSwitchRequest: func(AutoModeSwitchRequest, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
							return AutoModeSwitchResponseNo, nil
						},
					})
					recoveryResult <- err
				}()
			}
			firstEntered, secondEntered := ordinaryEntered, recoveryEntered
			if recoveryFirst {
				firstEntered, secondEntered = recoveryEntered, ordinaryEntered
				startRecovery()
			} else {
				startOrdinary()
			}
			select {
			case <-firstEntered:
			case <-ctx.Done():
				t.Fatal("first resume did not reach the runtime")
			}
			if recoveryFirst {
				startOrdinary()
			} else {
				startRecovery()
			}
			secondAccepted := false
		waitForSecond:
			for {
				client.sessionOperationsMux.Lock()
				queued := false
				for _, operation := range client.sessionOperations {
					queued = queued || operation.users == 2
				}
				client.sessionOperationsMux.Unlock()
				select {
				case <-secondEntered:
					secondAccepted = true
					break waitForSecond
				case <-ctx.Done():
					t.Fatal("second resume was neither queued nor accepted")
				default:
					if queued {
						break waitForSecond
					}
					runtime.Gosched()
				}
			}
			var recoveryErr error
			if !recoveryFirst && secondAccepted {
				recoveryErr = <-recoveryResult
			}
			release()
			session := <-ordinaryResult
			if err := <-ordinaryError; err != nil {
				t.Fatalf("ordinary alias resume failed: %v", err)
			}
			t.Cleanup(session.stopEventProcessing)
			if recoveryFirst || !secondAccepted {
				recoveryErr = <-recoveryResult
			}
			if recoveryErr == nil || !strings.Contains(recoveryErr.Error(), "does not support AutoModeRecovery") {
				t.Fatalf("expected unsupported recovery acknowledgment, got %v", recoveryErr)
			}
			if !attached.Load() {
				t.Fatal("ordinary alias resume returned an attachment detached by recovery cleanup")
			}
		})
	}
}

func TestAutoModeRecoveryPendingConsentAllowsIndependentSessionOperation(t *testing.T) {
	for _, action := range []string{"resume", "delete"} {
		t.Run(action, func(t *testing.T) {
			client, server, _ := autoModeRecoveryClient(t, &SessionCapabilitiesWithAutoModeRecovery{UI: &UICapabilitiesWithAutoModeRecovery{AutoModeRecovery: true}})
			entered := make(chan struct{})
			release := make(chan struct{})
			unblock := sync.OnceFunc(func() { close(release) })
			defer unblock()
			session, err := createAutoModeRecoverySession(t.Context(), client, &SessionConfig{
				SessionID: "pending-consent",
				OnAutoModeSwitchRequest: func(AutoModeSwitchRequest, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
					close(entered)
					<-release
					return AutoModeSwitchResponseNo, nil
				},
			})
			if err != nil {
				t.Fatal(err)
			}
			t.Cleanup(session.stopEventProcessing)
			callbackDone := make(chan error, 1)
			go func() {
				_, err := server.Request(t.Context(), "autoModeSwitch.request", autoModeSwitchRequest{SessionID: session.SessionID})
				callbackDone <- err
			}()
			<-entered
			if action == "resume" {
				replacement, err := client.ResumeSession(t.Context(), session.SessionID, &ResumeSessionConfig{})
				if err != nil {
					t.Fatalf("independent resume could not invalidate pending consent: %v", err)
				}
				t.Cleanup(replacement.stopEventProcessing)
			} else if err := client.DeleteSession(t.Context(), session.SessionID); err != nil {
				t.Fatalf("independent deletion could not invalidate pending consent: %v", err)
			}
			unblock()
			if err := <-callbackDone; err != nil {
				t.Fatal(err)
			}
		})
	}
}

func TestAutoModeRecoverySuccessfulResumeKeepsAcceptedOwnerWithoutAcknowledgment(t *testing.T) {
	for _, test := range []struct {
		name string
		caps *SessionCapabilities
	}{
		{"missing-capabilities", nil},
		{"missing-ui", &SessionCapabilities{}},
		{"false-acknowledgment", &SessionCapabilities{UI: &UICapabilities{}}},
	} {
		t.Run(test.name, func(t *testing.T) {
			client, server, requests := autoModeRecoveryClient(t, nil)
			resident, err := client.CreateSession(t.Context(), &SessionConfig{
				SessionID: "session-1",
				OnAutoModeSwitchRequest: func(AutoModeSwitchRequest, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
					return AutoModeSwitchResponseYesAlways, nil
				},
				GitHubTokenProvider: func(GitHubTokenProviderArgs) (*GitHubTokenProviderResult, error) {
					return GitHubTokenResult(&GitHubToken{AccessToken: "resident-token"}), nil
				},
			})
			if err != nil {
				t.Fatal(err)
			}
			t.Cleanup(resident.stopEventProcessing)
			server.SetRequestHandler("session.resume", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				var input map[string]any
				if err := json.Unmarshal(params, &input); err != nil {
					return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
				}
				requests.append(recordedRequest{Method: "session.resume", Params: input})
				data, err := json.Marshal(map[string]any{
					"sessionId": input["sessionId"], "capabilities": test.caps, "workspacePath": "resumed-workspace",
				})
				if err != nil {
					return nil, &jsonrpc2.Error{Code: -32603, Message: err.Error()}
				}
				return data, nil
			})
			_, err = resumeAutoModeRecoverySession(t.Context(), client, resident.SessionID, &ResumeSessionConfig{
				OnAutoModeSwitchRequest: func(AutoModeSwitchRequest, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
					return AutoModeSwitchResponseNo, nil
				},
				GitHubTokenProvider: func(GitHubTokenProviderArgs) (*GitHubTokenProviderResult, error) {
					return GitHubTokenResult(&GitHubToken{AccessToken: "resumed-token"}), nil
				},
			})
			if err == nil || !strings.Contains(err.Error(), "does not support AutoModeRecovery") {
				t.Fatalf("expected unsupported enrollment, got %v", err)
			}
			var residentRegistration, resumedRegistration string
			for _, request := range requests.snapshot() {
				switch request.Method {
				case "session.detach":
					t.Fatal("missing acknowledgment detached the accepted resident attachment")
				case "session.create":
					residentRegistration, _ = request.Params["gitHubTokenProviderRegistrationId"].(string)
				case "session.resume":
					resumedRegistration, _ = request.Params["gitHubTokenProviderRegistrationId"].(string)
				}
			}
			client.sessionsMux.Lock()
			current := client.sessions[resident.SessionID]
			client.sessionsMux.Unlock()
			if current == nil || current == resident {
				t.Fatal("successful resume restored the old callback owner")
			}
			if current.WorkspacePath() != "resumed-workspace" {
				t.Fatalf("accepted resume workspace was lost: %q", current.WorkspacePath())
			}
			reply, err := server.Request(t.Context(), "autoModeSwitch.request", autoModeSwitchRequest{
				SessionID: resident.SessionID, ErrorCode: ptr("user_weekly_rate_limited"),
			})
			if err != nil {
				t.Fatal(err)
			}
			var response autoModeSwitchResponse
			if err := json.Unmarshal(reply, &response); err != nil {
				t.Fatal(err)
			}
			if response.Response != AutoModeSwitchResponseNo {
				t.Fatalf("accepted resume callback was lost: %s", reply)
			}
			if residentRegistration == "" || resumedRegistration == "" || residentRegistration == resumedRegistration {
				t.Fatalf("expected distinct provider registrations: %q, %q", residentRegistration, resumedRegistration)
			}
			token, err := server.Request(t.Context(), "gitHubToken.getToken", &rpc.GitHubTokenAcquireRequest{
				RegistrationID: resumedRegistration, Host: "github.example.com", SessionID: &resident.SessionID,
				Reason: rpc.GitHubTokenAcquireReasonRefresh,
			})
			if err != nil {
				t.Fatal(err)
			}
			var acquired struct {
				AccessToken string `json:"accessToken"`
			}
			if err := json.Unmarshal(token, &acquired); err != nil {
				t.Fatal(err)
			}
			if acquired.AccessToken != "resumed-token" {
				t.Fatalf("accepted provider was lost: %s", token)
			}
			if _, err := server.Request(t.Context(), "gitHubToken.getToken", &rpc.GitHubTokenAcquireRequest{
				RegistrationID: residentRegistration, Host: "github.example.com", Reason: rpc.GitHubTokenAcquireReasonRefresh,
			}); err == nil {
				t.Fatal("replaced provider registration was not released")
			}
			if err := current.Disconnect(); err != nil {
				t.Fatal(err)
			}
			if _, err := server.Request(t.Context(), "gitHubToken.getToken", &rpc.GitHubTokenAcquireRequest{
				RegistrationID: resumedRegistration, Host: "github.example.com", Reason: rpc.GitHubTokenAcquireReasonRefresh,
			}); err == nil {
				t.Fatal("accepted provider registration survived disconnect")
			}
		})
	}
}

func TestAutoModeRecoveryCancelledEnrollmentKeepsCallbacksUntilCleanupSettles(t *testing.T) {
	for _, test := range []struct{ resume, cleanupFailure bool }{
		{false, false}, {true, false}, {false, true}, {true, true},
	} {
		t.Run(fmt.Sprintf("resume=%t/cleanup-failure=%t", test.resume, test.cleanupFailure), func(t *testing.T) {
			client, server, _ := autoModeRecoveryClient(t, nil)
			started := make(chan string, 1)
			release := make(chan struct{})
			var releaseOnce sync.Once
			releaseDetach := func() { releaseOnce.Do(func() { close(release) }) }
			server.SetRequestHandler("session.detach", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				var request sessionDetachRequest
				if err := json.Unmarshal(params, &request); err != nil {
					return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
				}
				started <- request.SessionID
				<-release
				if test.cleanupFailure {
					return []byte(`{"success":false,"error":"detach rejected"}`), nil
				}
				return []byte(`{"success":true}`), nil
			})
			ctx, cancel := context.WithCancel(t.Context())
			defer cancel()
			result := make(chan error, 1)
			go func() {
				handler := func(AutoModeSwitchRequest, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
					return AutoModeSwitchResponseYesAlways, nil
				}
				hooks := &SessionHooks{OnSessionEnd: func(_ SessionEndHookInput, invocation HookInvocation) (*SessionEndHookOutput, error) {
					return &SessionEndHookOutput{SessionSummary: "cleanup for " + invocation.SessionID}, nil
				}}
				var err error
				if test.resume {
					_, err = resumeAutoModeRecoverySession(ctx, client, "session-1", &ResumeSessionConfig{
						OnAutoModeSwitchRequest: handler, Hooks: hooks,
					})
				} else {
					_, err = createAutoModeRecoverySession(ctx, client, &SessionConfig{
						OnAutoModeSwitchRequest: handler, Hooks: hooks,
					})
				}
				result <- err
			}()
			defer releaseDetach()
			var sessionID string
			select {
			case sessionID = <-started:
			case <-time.After(5 * time.Second):
				t.Fatal("detach did not start")
			}
			cancel()
			select {
			case err := <-result:
				if !errors.Is(err, context.Canceled) {
					t.Fatalf("expected caller cancellation, got %v", err)
				}
			case <-time.After(5 * time.Second):
				t.Fatal("caller cancellation did not return")
			}
			waitCtx, stopWaiting := context.WithTimeout(t.Context(), 5*time.Second)
			defer stopWaiting()
			response, err := server.Request(waitCtx, "autoModeSwitch.request", autoModeSwitchRequest{SessionID: sessionID})
			if err != nil {
				t.Fatalf("callback during owned cleanup failed: %v", err)
			}
			var reply autoModeSwitchResponse
			if err := json.Unmarshal(response, &reply); err != nil || reply.Response != AutoModeSwitchResponseYesAlways {
				t.Fatalf("cleanup lost original callback: %s (%v)", response, err)
			}
			hookRequest := map[string]any{
				"sessionId": sessionID, "hookType": "sessionEnd",
				"input": map[string]any{"sessionId": sessionID, "reason": "error", "timestamp": 1730000000000, "cwd": "workdir"},
			}
			hookResponse, err := server.Request(waitCtx, "hooks.invoke", hookRequest)
			if err != nil || !strings.Contains(string(hookResponse), "cleanup for "+sessionID) {
				t.Fatalf("teardown hook lost its original callback: %s (%v)", hookResponse, err)
			}
			releaseDetach()
			if err := client.waitForAutoModeRecoveryCleanup(waitCtx, sessionID); (err != nil) != test.cleanupFailure {
				t.Fatalf("cleanup failure=%t, got %v", test.cleanupFailure, err)
			}
			if _, err := server.Request(waitCtx, "autoModeSwitch.request", autoModeSwitchRequest{SessionID: sessionID}); err == nil || !strings.Contains(err.Error(), "unknown session") {
				t.Fatalf("settled cleanup retained its callback route: %v", err)
			}
			if _, err := server.Request(waitCtx, "hooks.invoke", hookRequest); err == nil || !strings.Contains(err.Error(), "unknown session") {
				t.Fatalf("settled cleanup retained its teardown callback route: %v", err)
			}
			if test.cleanupFailure {
				if _, err := client.ResumeSession(waitCtx, sessionID, &ResumeSessionConfig{}); err == nil {
					t.Fatal("uncertain cleanup released the same-ID fence")
				}
			}
		})
	}
}

func TestAutoModeRecoveryCancelledEnrollmentStillAttemptsDetach(t *testing.T) {
	client, server, _ := autoModeRecoveryClient(t, nil)
	session, err := openAutoModeRecoverySession(t.Context(), client, false, false,
		func(AutoModeSwitchRequest, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
			return AutoModeSwitchResponseNo, nil
		})
	if err != nil {
		t.Fatal(err)
	}
	detached := make(chan struct{}, 1)
	server.SetRequestHandler("session.detach", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		detached <- struct{}{}
		return []byte(`{"success":true}`), nil
	})
	ctx, cancel := context.WithCancel(t.Context())
	cancel()
	err = client.requireAutoModeRecovery(ctx, session, true, false)
	if !errors.Is(err, context.Canceled) || !strings.Contains(err.Error(), "does not support AutoModeRecovery") {
		t.Fatalf("caller cancellation or unsupported enrollment was lost: %v", err)
	}
	select {
	case <-detached:
	case <-time.After(5 * time.Second):
		t.Fatal("caller cancellation prevented the cleanup detach from being sent")
	}
	if err := client.waitForAutoModeRecoveryCleanup(t.Context(), session.SessionID); err != nil {
		t.Fatal(err)
	}
	select {
	case <-session.eventDone:
	default:
		t.Fatal("settled cleanup did not retire the local session")
	}
}

type autoRecoveryAdmissionContext struct {
	context.Context
	checked chan struct{}
	release <-chan struct{}
	once    sync.Once
}

func (c *autoRecoveryAdmissionContext) Done() <-chan struct{} {
	c.once.Do(func() {
		close(c.checked)
		<-c.release
	})
	return c.Context.Done()
}

func TestAutoModeRecoveryInitializationKeepsProvisionalOwner(t *testing.T) {
	client, server, _ := autoModeRecoveryClient(t, nil)
	created := make(chan struct{})
	releaseResponse := make(chan struct{})
	releaseRetry := make(chan struct{})
	var responseOnce, retryOnce sync.Once
	releaseFirst := func() { responseOnce.Do(func() { close(releaseResponse) }) }
	releaseSecond := func() { retryOnce.Do(func() { close(releaseRetry) }) }
	defer releaseFirst()
	defer releaseSecond()
	detached := make(chan struct{}, 3)
	var creates atomic.Int32
	server.SetRequestHandler("session.create", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		if creates.Add(1) == 1 {
			close(created)
			<-releaseResponse
		}
		return []byte(`{"sessionId":"session-1"}`), nil
	})
	server.SetRequestHandler("session.detach", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		detached <- struct{}{}
		return []byte(`{"success":true}`), nil
	})
	ctx, cancel := context.WithTimeout(t.Context(), 5*time.Second)
	defer cancel()
	wait := func(signal <-chan struct{}, phase string) {
		t.Helper()
		select {
		case <-signal:
		case <-ctx.Done():
			t.Fatalf("%s: %v", phase, ctx.Err())
		}
	}
	first := make(chan error, 1)
	go func() {
		_, err := createAutoModeRecoverySession(ctx, client, &SessionConfig{
			SessionID: "session-1",
			OnAutoModeSwitchRequest: func(AutoModeSwitchRequest, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
				return AutoModeSwitchResponseNo, nil
			},
		})
		first <- err
	}()
	wait(created, "first create did not reach the runtime")
	client.sessionsMux.Lock()
	owner := client.sessions["session-1"]
	client.sessionsMux.Unlock()
	retryCtx, cancelRetry := context.WithCancel(ctx)
	defer cancelRetry()
	admission := &autoRecoveryAdmissionContext{
		Context: retryCtx, checked: make(chan struct{}), release: releaseRetry,
	}
	retry := make(chan error, 1)
	go func() {
		_, err := client.CreateSession(admission, &SessionConfig{SessionID: "session-1"})
		retry <- err
	}()
	wait(admission.checked, "second create did not reach admission")
	client.sessionsMux.Lock()
	current := client.sessions["session-1"]
	client.sessionsMux.Unlock()
	releaseSecond()
	releaseFirst()
	firstError := <-first
	retryError := <-retry
	if current != owner {
		t.Fatal("concurrent create replaced the provisional owner before its enrollment result")
	}
	if retryError != nil {
		t.Fatalf("concurrent create did not wait for the first attachment's cleanup: %v", retryError)
	}
	if firstError == nil || !strings.Contains(firstError.Error(), "does not support AutoModeRecovery") {
		t.Fatalf("expected unsupported first enrollment, got %v", firstError)
	}
	select {
	case <-detached:
	default:
		t.Fatal("failed same-ID create suppressed required cleanup of the first attachment")
	}
}

func TestAutoModeRecoveryCleanupFencesPublicDeletion(t *testing.T) {
	client, server, _ := autoModeRecoveryClient(t, nil)
	var deletes atomic.Int32
	server.SetRequestHandler("session.delete", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		deletes.Add(1)
		return []byte(`{"success":true}`), nil
	})
	finished := make(chan struct{})
	client.autoModeRecoveryCleanups = map[*jsonrpc2.Client]map[string]<-chan struct{}{
		client.client: {"session-1": finished},
	}
	endCallback := client.beginSessionCallback("session-1")
	err := client.DeleteSession(t.Context(), "session-1")
	endCallback()
	if err == nil || !strings.Contains(err.Error(), "active session callback") {
		t.Fatalf("deletion must reject callback re-entry during cleanup, got %v", err)
	}
	close(finished)
	err = client.DeleteSession(t.Context(), "session-1")
	if err == nil || !strings.Contains(err.Error(), "cleanup failed") {
		t.Fatalf("deletion must preserve failed cleanup fencing, got %v", err)
	}
	if deletes.Load() != 0 {
		t.Fatalf("fenced deletion sent %d RPCs", deletes.Load())
	}
	delete(client.autoModeRecoveryCleanups[client.client], "session-1")
	if err := client.DeleteSession(t.Context(), "session-1"); err != nil {
		t.Fatalf("deletion after completed cleanup: %v", err)
	}
	if deletes.Load() != 1 {
		t.Fatalf("unfenced deletion sent %d RPCs", deletes.Load())
	}
}

func TestAutoModeRecoveryCleanupFencesSameIDReuse(t *testing.T) {
	for _, resume := range []bool{false, true} {
		for _, retryResume := range []bool{false, true} {
			t.Run(fmt.Sprintf("resume=%t/retry-resume=%t", resume, retryResume), func(t *testing.T) {
				client, server, _ := autoModeRecoveryClient(t, nil)
				detachStarted := make(chan string, 1)
				releaseDetach := make(chan struct{})
				var firstDetach sync.Once
				server.SetRequestHandler("session.detach", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
					var request sessionDetachRequest
					if err := json.Unmarshal(params, &request); err != nil {
						return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
					}

					firstDetach.Do(func() {
						detachStarted <- request.SessionID
						<-releaseDetach
					})
					return []byte(`{"success":true}`), nil
				})
				ctx, cancel := context.WithCancel(t.Context())
				rejected := make(chan error, 1)
				go func() {
					_, err := openAutoModeRecoverySession(ctx, client, resume, true,
						func(AutoModeSwitchRequest, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
							return AutoModeSwitchResponseNo, nil
						})
					rejected <- err
				}()
				var releaseOnce sync.Once
				release := func() { releaseOnce.Do(func() { close(releaseDetach) }) }
				t.Cleanup(release)
				defer cancel()
				var sessionID string
				select {
				case sessionID = <-detachStarted:
				case <-time.After(5 * time.Second):
					t.Fatal("enrollment did not reach detach")
				}
				client.sessionsMux.Lock()
				rejectedOwner := client.sessions[sessionID]
				client.sessionsMux.Unlock()
				cancel()
				if err := <-rejected; !errors.Is(err, context.Canceled) {
					t.Fatalf("expected canceled enrollment, got %v", err)
				}
				reuse := func(ctx context.Context) (*Session, error) {
					if retryResume {
						return client.ResumeSession(ctx, sessionID, &ResumeSessionConfig{})
					}
					return client.CreateSession(ctx, &SessionConfig{SessionID: sessionID})
				}
				retryCtx, cancelRetry := context.WithCancel(t.Context())
				defer cancelRetry()
				admission := &autoRecoveryAdmissionContext{
					Context: retryCtx, checked: make(chan struct{}), release: releaseDetach,
				}
				retried := make(chan error, 1)
				go func() {
					_, err := reuse(admission)
					retried <- err
				}()
				select {
				case <-admission.checked:
				case <-time.After(5 * time.Second):
					t.Fatal("replacement did not reach its cancellation boundary")
				}
				client.sessionsMux.Lock()
				premature := client.sessions[sessionID]
				client.sessionsMux.Unlock()
				cancelRetry()
				release()
				if err := <-retried; !errors.Is(err, context.Canceled) {
					t.Fatalf("same-ID cleanup wait lost cancellation: %v", err)
				}
				if premature != rejectedOwner {
					t.Fatal("same-ID reuse published a replacement before the rejected attachment finished detaching")
				}
				replacement, err := reuse(t.Context())
				if err != nil {
					t.Fatalf("same-ID reuse failed after cleanup: %v", err)
				}
				if err := replacement.Disconnect(); err != nil {
					t.Fatal(err)
				}
			})
		}
	}
}

type reentrySessionFSProvider struct {
	SessionFSProvider
	write func() error
}

func TestRejectedRecoveryCleanupHookRejectsSameIDResume(t *testing.T) {
	client, server, _ := autoModeRecoveryClient(t, nil)
	nested := make(chan error, 1)
	server.SetRequestHandler("session.detach", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var request sessionDetachRequest
		if err := json.Unmarshal(params, &request); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
		}
		_, err := server.Request(t.Context(), "hooks.invoke", map[string]any{
			"sessionId": request.SessionID, "hookType": "sessionEnd",
			"input": map[string]any{"sessionId": request.SessionID, "timestamp": 1730000000000, "cwd": "workdir", "reason": "error"},
		})
		if err != nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: err.Error()}
		}
		return []byte(`{"success":true}`), nil
	})
	_, err := createAutoModeRecoverySession(t.Context(), client, &SessionConfig{
		SessionID: "cleanup-hook",
		OnAutoModeSwitchRequest: func(AutoModeSwitchRequest, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
			return AutoModeSwitchResponseNo, nil
		},
		Hooks: &SessionHooks{OnSessionEnd: func(SessionEndHookInput, HookInvocation) (*SessionEndHookOutput, error) {
			ctx, cancel := context.WithTimeout(t.Context(), time.Second)
			defer cancel()
			_, err := client.ResumeSession(ctx, "cleanup-hook", &ResumeSessionConfig{})
			nested <- err
			return nil, nil
		}},
	})
	if err == nil || !strings.Contains(err.Error(), "does not support AutoModeRecovery") {
		t.Fatalf("expected rejected enrollment, got %v", err)
	}
	if err := <-nested; err == nil || !strings.Contains(err.Error(), "active session callback") {
		t.Fatalf("expected prompt session callback rejection, got %v", err)
	}
	server.SetRequestHandler("session.detach", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		return []byte(`{"success":true}`), nil
	})
	replacement, err := client.ResumeSession(t.Context(), "cleanup-hook", &ResumeSessionConfig{})
	if err != nil {
		t.Fatalf("callback scope survived cleanup settlement: %v", err)
	}
	replacement.stopEventProcessing()
}

func (p reentrySessionFSProvider) WriteFile(string, string, *int) error {
	return p.write()
}

func TestSessionFSCallbackRejectsSameIDInitialization(t *testing.T) {
	for _, action := range []string{"create", "resume", "delete"} {
		t.Run(action, func(t *testing.T) {
			client, server, _ := autoModeRecoveryClient(t, nil)
			client.options.SessionFS = &SessionFSConfig{
				InitialWorkingDirectory: "/workspace",
				SessionStatePath:        "/sessions",
				Conventions:             rpc.SessionFSSetProviderConventionsPosix,
			}
			nested := make(chan error, 1)
			config := &SessionConfig{SessionID: "reentry"}
			config.CreateSessionFSProvider = func(*Session) SessionFSProvider {
				return reentrySessionFSProvider{write: func() error {
					ctx, cancel := context.WithTimeout(t.Context(), time.Second)
					defer cancel()
					var err error
					switch action {
					case "resume":
						_, err = client.ResumeSession(ctx, "reentry", &ResumeSessionConfig{
							CreateSessionFSProvider: config.CreateSessionFSProvider,
						})
					case "create":
						_, err = client.CreateSession(ctx, config)
					case "delete":
						err = client.DeleteSession(ctx, "reentry")
					}
					nested <- err
					return nil
				}}
			}
			server.SetRequestHandler("session.create", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				_, err := server.Request(t.Context(), "sessionFs.writeFile", map[string]any{
					"sessionId": "reentry", "path": "/state", "content": "{}",
				})
				if err != nil {
					return nil, &jsonrpc2.Error{Code: -32603, Message: err.Error()}
				}
				return []byte(`{"sessionId":"reentry"}`), nil
			})
			session, err := client.CreateSession(t.Context(), config)
			if err != nil {
				t.Fatal(err)
			}
			defer session.stopEventProcessing()
			if err := <-nested; err == nil || !strings.Contains(err.Error(), "active session callback") {
				t.Fatalf("expected prompt callback admission error, got %v", err)
			}
		})
	}
}

func TestSessionFSCallbackAdmissionIsScopedAndCounted(t *testing.T) {
	client, server, _ := autoModeRecoveryClient(t, nil)
	client.options.SessionFS = &SessionFSConfig{
		InitialWorkingDirectory: "/workspace",
		SessionStatePath:        "/sessions",
		Conventions:             rpc.SessionFSSetProviderConventionsPosix,
	}
	entered := make(chan struct{}, 2)
	release := make(chan struct{})
	defer close(release)
	factory := func(*Session) SessionFSProvider {
		return reentrySessionFSProvider{write: func() error {
			entered <- struct{}{}
			<-release
			return nil
		}}
	}
	session, err := client.CreateSession(t.Context(), &SessionConfig{
		SessionID: "active", CreateSessionFSProvider: factory,
	})
	if err != nil {
		t.Fatal(err)
	}
	defer session.stopEventProcessing()
	results := make(chan error, 2)
	for range 2 {
		go func() {
			_, err := server.Request(t.Context(), "sessionFs.writeFile", map[string]any{
				"sessionId": "active", "path": "/state", "content": "{}",
			})
			results <- err
		}()
	}
	<-entered
	<-entered
	initializing := make(chan struct{})
	releaseInitialization := make(chan struct{})
	unblockInitialization := sync.OnceFunc(func() { close(releaseInitialization) })
	defer unblockInitialization()
	server.SetRequestHandler("session.create", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var input struct {
			SessionID string `json:"sessionId"`
		}
		if err := json.Unmarshal(params, &input); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
		}
		if input.SessionID == "active" {
			close(initializing)
			<-releaseInitialization
		}
		response, _ := json.Marshal(map[string]string{"sessionId": input.SessionID})
		return response, nil
	})
	initialized := make(chan error, 1)
	go func() {
		replacement, err := client.CreateSession(t.Context(), &SessionConfig{
			SessionID: "active", CreateSessionFSProvider: factory,
		})
		if replacement != nil {
			t.Cleanup(replacement.stopEventProcessing)
		}
		initialized <- err
	}()
	<-initializing
	for remaining := 2; remaining > 0; remaining-- {
		for _, resume := range []bool{false, true} {
			var err error
			if resume {
				_, err = client.ResumeSession(t.Context(), "active", &ResumeSessionConfig{CreateSessionFSProvider: factory})
			} else {
				_, err = client.CreateSession(t.Context(), &SessionConfig{SessionID: "active", CreateSessionFSProvider: factory})
			}
			if err == nil || !strings.Contains(err.Error(), "active session callback") {
				t.Fatalf("%d callbacks active: expected independent same-ID rejection, got %v", remaining, err)
			}
		}
		other, err := client.CreateSession(t.Context(), &SessionConfig{
			SessionID: "other", CreateSessionFSProvider: factory,
		})
		if err != nil {
			t.Fatalf("unrelated initialization failed: %v", err)
		}
		other.stopEventProcessing()
		release <- struct{}{}
		if err := <-results; err != nil {
			t.Fatal(err)
		}
	}
	unblockInitialization()
	if err := <-initialized; err != nil {
		t.Fatal(err)
	}
	resumed, err := client.ResumeSession(t.Context(), "active", &ResumeSessionConfig{CreateSessionFSProvider: factory})
	if err != nil {
		t.Fatalf("initialization after callbacks failed: %v", err)
	}
	defer resumed.stopEventProcessing()
}

func TestCanonicalSessionFSCallbackRejectsWaitOnItsAliasPermit(t *testing.T) {
	client, server, _ := autoModeRecoveryClient(t, nil)
	client.options.SessionFS = &SessionFSConfig{
		InitialWorkingDirectory: "/workspace",
		SessionStatePath:        "/sessions",
		Conventions:             rpc.SessionFSSetProviderConventionsPosix,
	}
	const canonicalID = "abcdef12-3456-4789-abcd-0123456789ab"
	mockSessionPrefixLookup(t, server, canonicalID)
	server.SetRequestHandler("session.resume", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		return []byte(`{"sessionId":"abcdef12-3456-4789-abcd-0123456789ab","capabilities":{"ui":{"autoModeRecovery":true}}}`), nil
	})
	server.SetRequestHandler("session.options.update", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		_, err := server.Request(t.Context(), "sessionFs.writeFile", map[string]any{
			"sessionId": canonicalID, "path": "/state", "content": "{}",
		})
		if err != nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: err.Error()}
		}
		return []byte(`{"success":true}`), nil
	})
	nested := make(chan error, 1)
	session, err := resumeAutoModeRecoverySession(t.Context(), client, "abcdef12", &ResumeSessionConfig{
		SkipCustomInstructions: Bool(true),
		OnAutoModeSwitchRequest: func(AutoModeSwitchRequest, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
			return AutoModeSwitchResponseNo, nil
		},
		CreateSessionFSProvider: func(*Session) SessionFSProvider {
			return reentrySessionFSProvider{write: func() error {
				ctx, cancel := context.WithTimeout(t.Context(), time.Second)
				defer cancel()
				nested <- client.DeleteSession(ctx, "abcdef12-9999-4789-abcd-0123456789ab")
				return nil
			}}
		},
	})
	if err != nil {
		t.Fatal(err)
	}
	defer session.stopEventProcessing()
	if err := <-nested; err == nil || !strings.Contains(err.Error(), "active session callback") {
		t.Fatalf("canonical callback waited on its own prefix permit: %v", err)
	}
}

func TestSessionFSFactoryRejectsSameIDDeletion(t *testing.T) {
	for _, resume := range []bool{false, true} {
		for _, failSetup := range []bool{false, true} {
			name := map[bool]string{false: "create", true: "resume"}[resume]
			if failSetup {
				name += "-invalid-provider"
			}
			t.Run(name, func(t *testing.T) {
				client, server, _ := autoModeRecoveryClient(t, nil)
				client.options.SessionFS = &SessionFSConfig{
					InitialWorkingDirectory: "/workspace",
					SessionStatePath:        "/sessions",
					Conventions:             rpc.SessionFSSetProviderConventionsPosix,
				}
				if failSetup {
					client.options.SessionFS.Capabilities = &SessionFSCapabilities{Binary: true}
				}
				deletions := make(chan string, 3)
				server.SetRequestHandler("session.delete", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
					var input struct {
						SessionID string `json:"sessionId"`
					}
					if err := json.Unmarshal(params, &input); err != nil {
						return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
					}
					deletions <- input.SessionID
					return []byte(`{"success":true}`), nil
				})
				var nestedError error
				factory := func(session *Session) SessionFSProvider {
					ctx, cancel := context.WithTimeout(t.Context(), time.Second)
					defer cancel()
					nestedError = client.DeleteSession(ctx, session.SessionID)
					if err := client.DeleteSession(t.Context(), "unrelated"); err != nil {
						t.Errorf("unrelated deletion in factory: %v", err)
					}
					return reentrySessionFSProvider{}
				}
				var session *Session
				var err error
				if resume {
					session, err = client.ResumeSession(t.Context(), "factory", &ResumeSessionConfig{
						CreateSessionFSProvider: factory,
					})
				} else {
					session, err = client.CreateSession(t.Context(), &SessionConfig{
						SessionID: "factory", CreateSessionFSProvider: factory,
					})
				}
				if session != nil {
					defer session.stopEventProcessing()
				}
				if (err != nil) != failSetup {
					t.Fatalf("setup error = %v, want failure = %v", err, failSetup)
				}
				if nestedError == nil || !strings.Contains(nestedError.Error(), "active session callback") {
					t.Errorf("expected prompt factory deletion rejection, got %v", nestedError)
				}
				if got := <-deletions; got != "unrelated" {
					t.Fatalf("factory deletion reached runtime: %s", got)
				}
				if err := client.DeleteSession(t.Context(), "factory"); err != nil {
					t.Fatalf("factory callback scope survived setup completion: %v", err)
				}
				if got := <-deletions; got != "factory" {
					t.Fatalf("post-factory deletion = %s", got)
				}
				client.options.SessionFS.Capabilities = nil
				replacement, err := client.CreateSession(t.Context(), &SessionConfig{
					SessionID: "factory",
					CreateSessionFSProvider: func(*Session) SessionFSProvider {
						return reentrySessionFSProvider{}
					},
				})
				if err != nil {
					t.Fatalf("initialization after factory completion: %v", err)
				}
				replacement.stopEventProcessing()
			})
		}
	}
}

func TestSessionFSInitializationStillSerializesOutsideCallbacks(t *testing.T) {
	client, server, _ := autoModeRecoveryClient(t, nil)
	client.options.SessionFS = &SessionFSConfig{
		InitialWorkingDirectory: "/workspace",
		SessionStatePath:        "/sessions",
		Conventions:             rpc.SessionFSSetProviderConventionsPosix,
	}
	entered := make(chan struct{}, 2)
	release := make(chan struct{})
	defer close(release)
	server.SetRequestHandler("session.create", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var input struct {
			SessionID string `json:"sessionId"`
		}
		if err := json.Unmarshal(params, &input); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
		}
		if input.SessionID == "serialized" {
			entered <- struct{}{}
			<-release
		}
		result, _ := json.Marshal(map[string]string{"sessionId": input.SessionID})
		return result, nil
	})
	factory := func(*Session) SessionFSProvider { return reentrySessionFSProvider{} }
	results := make(chan error, 2)
	create := func() {
		session, err := client.CreateSession(t.Context(), &SessionConfig{
			SessionID: "serialized", CreateSessionFSProvider: factory,
		})
		if session != nil {
			session.stopEventProcessing()
		}
		results <- err
	}
	go create()
	<-entered
	go create()
	deadline := time.After(5 * time.Second)
	for {
		client.sessionOperationsMux.Lock()
		queued := client.sessionOperations["serialized"].users == 2
		client.sessionOperationsMux.Unlock()
		if queued {
			break
		}
		select {
		case <-deadline:
			t.Fatal("second same-ID initialization did not queue")
		default:
			runtime.Gosched()
		}
	}
	select {
	case <-entered:
		t.Fatal("second same-ID request bypassed serialization")
	case err := <-results:
		t.Fatalf("same-ID initialization rejected instead of waiting: %v", err)
	default:
	}
	other, err := client.CreateSession(t.Context(), &SessionConfig{
		SessionID: "other", CreateSessionFSProvider: factory,
	})
	if err != nil {
		t.Fatal(err)
	}
	other.stopEventProcessing()
	release <- struct{}{}
	<-entered
	release <- struct{}{}
	for range 2 {
		if err := <-results; err != nil {
			t.Fatal(err)
		}
	}
}
