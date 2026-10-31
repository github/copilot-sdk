// Copyright (c) Microsoft Corporation. All rights reserved.

package copilot

import (
	"bufio"
	"context"
	"encoding/base64"
	"encoding/json"
	"errors"
	"net"
	"path/filepath"
	"reflect"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
)

type kerberosAuthServer struct {
	url              string
	connected        chan *jsonrpc2.Client
	sessionRequested chan struct{}
	failing          chan struct{}
	mux              sync.Mutex
	requests         []string
	failMethod       string
	holdFailure      bool
}

// newKerberosAuthServer starts a fake runtime that records Kerberos
// registrations, later startup registrations, and session creation. With
// holdConnect, the connect response waits until a session is requested or a
// timeout elapses.
func newKerberosAuthServer(t *testing.T, holdConnect bool) *kerberosAuthServer {
	t.Helper()
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatalf("listen: %v", err)
	}
	s := &kerberosAuthServer{
		url:              listener.Addr().String(),
		connected:        make(chan *jsonrpc2.Client, 1),
		sessionRequested: make(chan struct{}),
		failing:          make(chan struct{}),
	}
	accepted := make(chan *jsonrpc2.Client, 1)
	go func() {
		conn, acceptErr := listener.Accept()
		if acceptErr != nil {
			return
		}
		server := jsonrpc2.NewClient(conn, conn)
		server.SetRequestHandler("connect", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			s.connected <- server
			if holdConnect {
				select {
				case <-s.sessionRequested:
				case <-time.After(500 * time.Millisecond):
				}
			}
			return []byte(`{"ok":true,"protocolVersion":3,"version":"test"}`), nil
		})
		server.SetRequestHandler("kerberosAuth.setProvider", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			s.record("kerberosAuth.setProvider " + string(params))
			return []byte(`{}`), nil
		})
		var failingOnce sync.Once
		for _, method := range []string{"registerExtensionLaunchProvider", "plugins.builtin.set", "sessionFs.setProvider", "llmInference.setProvider"} {
			server.SetRequestHandler(method, func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				s.record(method)
				s.mux.Lock()
				fail, hold := method == s.failMethod, s.holdFailure
				s.mux.Unlock()
				if !fail {
					return []byte(`{}`), nil
				}
				failingOnce.Do(func() { close(s.failing) })
				if hold {
					<-server.ConnectionClosed()
				}
				return nil, &jsonrpc2.Error{Code: -32000, Message: method + " failed"}
			})
		}
		var sessionRequestedOnce sync.Once
		server.SetRequestHandler("session.create", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			s.record("session.create")
			sessionRequestedOnce.Do(func() { close(s.sessionRequested) })
			sessionID := sessionIDFromParams(t, params)
			return []byte(`{"sessionId":"` + sessionID + `","workspacePath":"/workspace"}`), nil
		})
		server.Start()
		accepted <- server
	}()
	t.Cleanup(func() {
		listener.Close()
		select {
		case server := <-accepted:
			server.Stop()
		default:
		}
	})
	return s
}

func (s *kerberosAuthServer) record(request string) {
	s.mux.Lock()
	defer s.mux.Unlock()
	s.requests = append(s.requests, request)
}

func (s *kerberosAuthServer) recorded() []string {
	s.mux.Lock()
	defer s.mux.Unlock()
	return append([]string(nil), s.requests...)
}

// failRequest makes the runtime fail requests for method, closing failing when
// the first arrives. With hold, it holds each until the connection closes.
func (s *kerberosAuthServer) failRequest(method string, hold bool) {
	s.mux.Lock()
	defer s.mux.Unlock()
	s.failMethod, s.holdFailure = method, hold
}

type unusedExtensionLaunchProvider struct{}

func (unusedExtensionLaunchProvider) Resolve(*rpc.ExtensionLaunchProviderResolveRequest) (*rpc.ExtensionLaunchProviderResolveResult, error) {
	return nil, errors.New("unused")
}

// startKerberosAuthClient starts a client against a fake runtime and returns
// the server side of the connection.
func startKerberosAuthClient(t *testing.T, config *KerberosAuthConfig) (*kerberosAuthServer, *jsonrpc2.Client) {
	t.Helper()
	server := newKerberosAuthServer(t, false)
	client := NewClient(&ClientOptions{
		Connection:   URIConnection{URL: server.url},
		KerberosAuth: config,
	})
	t.Cleanup(client.ForceStop)
	if err := client.Start(t.Context()); err != nil {
		t.Fatalf("Start failed: %v", err)
	}
	return server, <-server.connected
}

func acquireKerberosTokens(t *testing.T, peer *jsonrpc2.Client, spns ...string) []string {
	t.Helper()
	responses := make([]string, 0, len(spns))
	for _, spn := range spns {
		result, err := peer.Request(t.Context(), "kerberosAuth.acquire", map[string]string{"spn": spn})
		if err != nil {
			responses = append(responses, err.Error())
		} else {
			responses = append(responses, string(result))
		}
	}
	return responses
}

// newKerberosAuthPeer connects a client to a raw JSON-RPC peer, which can send
// the numeric request IDs that $/cancelRequest requires.
func newKerberosAuthPeer(t *testing.T, callback func(context.Context, KerberosAuthAcquireRequest) (string, error)) (*Client, *installationConfirmationPeer) {
	t.Helper()
	clientConn, serverConn := net.Pipe()
	rpcClient := jsonrpc2.NewClient(clientConn, clientConn)
	client := NewClient(&ClientOptions{KerberosAuth: &KerberosAuthConfig{Mode: KerberosAuthModeAlways, Callback: callback}})
	client.client, client.RPC, client.state = rpcClient, rpc.NewServerRPC(rpcClient), stateConnected
	client.setupNotificationHandler()
	rpcClient.Start()
	t.Cleanup(func() {
		client.ForceStop()
		_ = serverConn.Close()
		_ = clientConn.Close()
	})
	return client, &installationConfirmationPeer{t: t, conn: serverConn, reader: bufio.NewReader(serverConn)}
}

// blockingKerberosAuthCallback blocks until its context is cancelled, closing
// entered when it starts and returned when it returns.
func blockingKerberosAuthCallback(entered, returned chan struct{}) func(context.Context, KerberosAuthAcquireRequest) (string, error) {
	return func(ctx context.Context, _ KerberosAuthAcquireRequest) (string, error) {
		defer close(returned)
		close(entered)
		<-ctx.Done()
		return "", ctx.Err()
	}
}

func TestClient_KerberosAuth(t *testing.T) {
	type snapshot struct {
		Requests  []string
		Calls     []KerberosAuthAcquireRequest
		Responses []string
	}

	for _, mode := range []KerberosAuthMode{KerberosAuthModeFallback, KerberosAuthModeAlways} {
		t.Run("registers "+string(mode)+" and routes lookups", func(t *testing.T) {
			var mux sync.Mutex
			var calls []KerberosAuthAcquireRequest
			server, peer := startKerberosAuthClient(t, &KerberosAuthConfig{
				Mode: mode,
				Callback: func(_ context.Context, request KerberosAuthAcquireRequest) (string, error) {
					mux.Lock()
					calls = append(calls, request)
					mux.Unlock()
					return base64.StdEncoding.EncodeToString([]byte(request.Spn)), nil
				},
			})

			responses := acquireKerberosTokens(t, peer, "HTTP/proxy.example", "ldap/directory.example@EXAMPLE.COM")
			mux.Lock()
			got := snapshot{Requests: server.recorded(), Calls: calls, Responses: responses}
			mux.Unlock()
			want := snapshot{
				Requests: []string{`kerberosAuth.setProvider {"mode":"` + string(mode) + `"}`},
				Calls:    []KerberosAuthAcquireRequest{{Spn: "HTTP/proxy.example"}, {Spn: "ldap/directory.example@EXAMPLE.COM"}},
				Responses: []string{
					`{"token":"` + base64.StdEncoding.EncodeToString([]byte("HTTP/proxy.example")) + `"}`,
					`{"token":"` + base64.StdEncoding.EncodeToString([]byte("ldap/directory.example@EXAMPLE.COM")) + `"}`,
				},
			}
			if !reflect.DeepEqual(got, want) {
				t.Fatalf("got %+v, want %+v", got, want)
			}
		})
	}

	t.Run("registers the configured scope", func(t *testing.T) {
		for _, tc := range []struct {
			name  string
			scope KerberosAuthScope
			want  string
		}{
			{"unset", "", `kerberosAuth.setProvider {"mode":"fallback"}`},
			{"runtime", KerberosAuthScopeRuntime, `kerberosAuth.setProvider {"mode":"fallback","scope":"runtime"}`},
		} {
			t.Run(tc.name, func(t *testing.T) {
				server, _ := startKerberosAuthClient(t, &KerberosAuthConfig{
					Mode:     KerberosAuthModeFallback,
					Scope:    tc.scope,
					Callback: func(context.Context, KerberosAuthAcquireRequest) (string, error) { return "", nil },
				})
				if got, want := server.recorded(), []string{tc.want}; !reflect.DeepEqual(got, want) {
					t.Fatalf("requests = %v, want %v", got, want)
				}
			})
		}
	})

	t.Run("does not register without a callback", func(t *testing.T) {
		server, _ := startKerberosAuthClient(t, nil)
		if got := server.recorded(); len(got) != 0 {
			t.Fatalf("requests = %v, want none", got)
		}
	})

	t.Run("returns no token when unavailable and rejects callback failures", func(t *testing.T) {
		failures := []error{nil, errors.New("lookup failed")}
		var mux sync.Mutex
		_, peer := startKerberosAuthClient(t, &KerberosAuthConfig{
			Mode: KerberosAuthModeAlways,
			Callback: func(context.Context, KerberosAuthAcquireRequest) (string, error) {
				mux.Lock()
				defer mux.Unlock()
				err := failures[0]
				failures = failures[1:]
				return "", err
			},
		})

		got := acquireKerberosTokens(t, peer, "HTTP/proxy", "HTTP/proxy")
		want := []string{`{}`, "JSON-RPC Error -32603: lookup failed"}
		if !reflect.DeepEqual(got, want) {
			t.Fatalf("responses = %q, want %q", got, want)
		}
	})

	t.Run("cancels the callback when the runtime cancels the request", func(t *testing.T) {
		entered, returned := make(chan struct{}), make(chan struct{})
		_, peer := newKerberosAuthPeer(t, blockingKerberosAuthCallback(entered, returned))

		peer.request(1, "kerberosAuth.acquire", map[string]string{"spn": "HTTP/proxy"})
		waitForClosed(t, entered, "callback start")
		peer.notify("$/cancelRequest", map[string]any{"id": 1})
		response := peer.readResponse(1)
		waitForClosed(t, returned, "callback return after $/cancelRequest")
		want := &jsonrpc2.Error{Code: kerberosAuthRequestCancelledCode, Message: "Kerberos auth request cancelled"}
		if !reflect.DeepEqual(response.Error, want) {
			t.Fatalf("response error = %+v, want %+v", response.Error, want)
		}
	})

	t.Run("cancels the callback when the client stops", func(t *testing.T) {
		entered, returned := make(chan struct{}), make(chan struct{})
		client, peer := newKerberosAuthPeer(t, blockingKerberosAuthCallback(entered, returned))

		peer.request(1, "kerberosAuth.acquire", map[string]string{"spn": "HTTP/proxy"})
		waitForClosed(t, entered, "callback start")
		if err := client.Stop(); err != nil {
			t.Fatalf("Stop failed: %v", err)
		}
		waitForClosed(t, returned, "callback return after Stop")
	})

	t.Run("registers before sessions requested during startup", func(t *testing.T) {
		server := newKerberosAuthServer(t, true)
		client := NewClient(&ClientOptions{
			Connection: URIConnection{URL: server.url},
			KerberosAuth: &KerberosAuthConfig{
				Mode:     KerberosAuthModeAlways,
				Callback: func(context.Context, KerberosAuthAcquireRequest) (string, error) { return "", nil },
			},
		})
		t.Cleanup(client.ForceStop)

		started := make(chan error, 1)
		go func() { started <- client.Start(t.Context()) }()
		// The transport exists once connect arrives; startup is held until a
		// session is requested or the server's timeout elapses.
		<-server.connected
		if _, err := client.CreateSession(t.Context(), nil); err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		if err := <-started; err != nil {
			t.Fatalf("Start failed: %v", err)
		}

		want := []string{`kerberosAuth.setProvider {"mode":"always"}`, "session.create"}
		if got := server.recorded(); !reflect.DeepEqual(got, want) {
			t.Fatalf("requests = %v, want %v", got, want)
		}
	})

	t.Run("closes the registered connection when later startup fails", func(t *testing.T) {
		pluginDirectory, err := filepath.Abs("plugins")
		if err != nil {
			t.Fatalf("Abs failed: %v", err)
		}
		steps := []struct {
			method  string
			options ClientOptions
		}{
			{"registerExtensionLaunchProvider", ClientOptions{ExtensionLaunchProvider: unusedExtensionLaunchProvider{}}},
			{"plugins.builtin.set", ClientOptions{BuiltinPluginDirectories: []string{pluginDirectory}}},
			{"sessionFs.setProvider", ClientOptions{SessionFS: &SessionFSConfig{
				InitialWorkingDirectory: "/",
				SessionStatePath:        "/state",
				Conventions:             rpc.SessionFSSetProviderConventionsPosix,
			}}},
			{"llmInference.setProvider", ClientOptions{RequestHandler: &CopilotRequestHandler{}}},
		}
		for _, step := range steps {
			for _, cancelled := range []bool{false, true} {
				name := step.method + " failed"
				if cancelled {
					name = step.method + " cancelled"
				}
				t.Run(name, func(t *testing.T) {
					// A runtime-scoped registration is exclusive, so a failed start must not keep holding it.
					server := newKerberosAuthServer(t, false)
					server.failRequest(step.method, cancelled)
					options := step.options
					options.Connection = URIConnection{URL: server.url}
					options.KerberosAuth = &KerberosAuthConfig{
						Mode:     KerberosAuthModeAlways,
						Scope:    KerberosAuthScopeRuntime,
						Callback: func(context.Context, KerberosAuthAcquireRequest) (string, error) { return "", nil },
					}
					client := NewClient(&options)
					t.Cleanup(client.ForceStop)

					ctx, cancel := context.WithCancel(t.Context())
					defer cancel()
					started := make(chan error, 1)
					go func() { started <- client.Start(ctx) }()
					peer := <-server.connected
					if cancelled {
						waitForClosed(t, server.failing, step.method)
						cancel()
					}
					err := <-started
					if cancelled && !errors.Is(err, context.Canceled) || !cancelled && (err == nil || !strings.Contains(err.Error(), step.method+" failed")) {
						t.Fatalf("Start returned %v", err)
					}
					waitForClosed(t, peer.ConnectionClosed(), "the registered connection to close")

					type snapshot struct {
						Requests  []string
						State     connectionState
						Transport bool
					}
					got := snapshot{server.recorded(), client.state, client.client != nil || client.conn != nil || client.RPC != nil || client.internalRPC != nil}
					want := snapshot{[]string{`kerberosAuth.setProvider {"mode":"always","scope":"runtime"}`, step.method}, stateError, false}
					if !reflect.DeepEqual(got, want) {
						t.Fatalf("got %+v, want %+v", got, want)
					}
				})
			}
		}
	})

	t.Run("rejects invalid configuration", func(t *testing.T) {
		for name, config := range map[string]*KerberosAuthConfig{
			"missing mode":     {Callback: func(context.Context, KerberosAuthAcquireRequest) (string, error) { return "", nil }},
			"missing callback": {Mode: KerberosAuthModeFallback},
		} {
			t.Run(name, func(t *testing.T) {
				defer func() {
					if recover() == nil {
						t.Fatal("expected NewClient to panic")
					}
				}()
				NewClient(&ClientOptions{KerberosAuth: config})
			})
		}
	})
}
