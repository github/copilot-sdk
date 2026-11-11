// Copyright (c) Microsoft Corporation. All rights reserved.

package copilot

import (
	"bufio"
	"context"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"net"
	"path/filepath"
	"reflect"
	"slices"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
)

type networkServer struct {
	url              string
	connected        chan *jsonrpc2.Client
	sessionRequested chan struct{}
	failing          chan struct{}
	mux              sync.Mutex
	requests         []string
	failMethod       string
	holdFailure      bool
}

// newNetworkServer starts a fake runtime that records network callback
// registrations, later startup registrations, and session creation. With
// holdConnect, the connect response waits until a session is requested or a
// timeout elapses.
func newNetworkServer(t *testing.T, holdConnect bool) *networkServer {
	t.Helper()
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatalf("listen: %v", err)
	}
	s := &networkServer{
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
		var failingOnce sync.Once
		respond := func(method string) (json.RawMessage, *jsonrpc2.Error) {
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
		}
		server.SetRequestHandler("network.setProvider", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			s.record("network.setProvider " + string(params))
			return respond("network.setProvider")
		})
		for _, method := range []string{"registerExtensionLaunchProvider", "plugins.builtin.set", "sessionFs.setProvider", "llmInference.setProvider"} {
			server.SetRequestHandler(method, func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				s.record(method)
				return respond(method)
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

func (s *networkServer) record(request string) {
	s.mux.Lock()
	defer s.mux.Unlock()
	s.requests = append(s.requests, request)
}

func (s *networkServer) recorded() []string {
	s.mux.Lock()
	defer s.mux.Unlock()
	return append([]string(nil), s.requests...)
}

// failRequest makes the runtime fail requests for method, closing failing when
// the first arrives. With hold, it holds each until the connection closes.
func (s *networkServer) failRequest(method string, hold bool) {
	s.mux.Lock()
	defer s.mux.Unlock()
	s.failMethod, s.holdFailure = method, hold
}

type unusedExtensionLaunchProvider struct{}

func (unusedExtensionLaunchProvider) Resolve(*rpc.ExtensionLaunchProviderResolveRequest) (*rpc.ExtensionLaunchProviderResolveResult, error) {
	return nil, errors.New("unused")
}

// startNetworkClient starts a client with network against a fake runtime and
// returns the server side of the connection.
func startNetworkClient(t *testing.T, network *NetworkConfig) (*networkServer, *jsonrpc2.Client) {
	t.Helper()
	server := newNetworkServer(t, false)
	client := NewClient(&ClientOptions{Connection: URIConnection{URL: server.url}, Network: network})
	t.Cleanup(client.ForceStop)
	if err := client.Start(t.Context()); err != nil {
		t.Fatalf("Start failed: %v", err)
	}
	return server, <-server.connected
}

// newNetworkPeer connects a client with network to a raw JSON-RPC peer, which
// can send the numeric request IDs that $/cancelRequest requires.
func newNetworkPeer(t *testing.T, network *NetworkConfig) (*Client, *installationConfirmationPeer) {
	t.Helper()
	clientConn, serverConn := net.Pipe()
	rpcClient := jsonrpc2.NewClient(clientConn, clientConn)
	client := NewClient(&ClientOptions{Network: network})
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

// networkCallbacks configures all callbacks to answer with lookup, which
// receives the requested URL, SPN or proxy. A Basic answer is the user name,
// and "" means no credentials.
func networkCallbacks(lookup func(ctx context.Context, target string) (string, error)) *NetworkConfig {
	return &NetworkConfig{
		ResolveProxy: func(ctx context.Context, request NetworkResolveProxyRequest) (string, error) {
			return lookup(ctx, request.URL)
		},
		AuthenticateNegotiate: func(ctx context.Context, request NetworkAuthenticateNegotiateRequest) (string, error) {
			return lookup(ctx, request.Spn)
		},
		AuthenticateBasic: func(ctx context.Context, request NetworkAuthenticateBasicRequest) (*NetworkAuthenticateBasicResult, error) {
			username, err := lookup(ctx, request.Proxy)
			if username == "" {
				return nil, err
			}
			return &NetworkAuthenticateBasicResult{Username: &username}, err
		},
	}
}

// blockingLookup blocks until its context is cancelled, closing entered when
// it starts and returned when it returns.
func blockingLookup(entered, returned chan struct{}) func(context.Context, string) (string, error) {
	return func(ctx context.Context, _ string) (string, error) {
		defer close(returned)
		close(entered)
		<-ctx.Done()
		return "", ctx.Err()
	}
}

type networkRequest struct {
	method string
	params any
}

// sendNetworkRequests sends requests concurrently and returns the responses in
// the order of requests.
func sendNetworkRequests(t *testing.T, peer *jsonrpc2.Client, requests ...networkRequest) []string {
	t.Helper()
	responses := make([]string, len(requests))
	var wg sync.WaitGroup
	for i, request := range requests {
		wg.Go(func() {
			result, err := peer.Request(t.Context(), request.method, request.params)
			if err != nil {
				responses[i] = err.Error()
			} else {
				responses[i] = string(result)
			}
		})
	}
	wg.Wait()
	return responses
}

func TestClient_Network(t *testing.T) {
	methods := []struct {
		method string
		params any
		label  string
		handle func(*networkAdapter, context.Context, json.RawMessage) (json.RawMessage, *jsonrpc2.Error)
	}{
		{"network.resolveProxy", map[string]string{"url": "https://api.example.com/"}, "Proxy resolution", (*networkAdapter).resolveProxy},
		{"network.authenticateNegotiate", map[string]string{"spn": "HTTP/proxy"}, "Negotiate authentication", (*networkAdapter).authenticateNegotiate},
		{"network.authenticateBasic", map[string]any{"proxy": "http://proxy:8080", "previousRejected": false}, "Basic authentication", (*networkAdapter).authenticateBasic},
	}
	resolveProxy := func(context.Context, NetworkResolveProxyRequest) (string, error) { return "", nil }
	authenticateNegotiate := func(context.Context, NetworkAuthenticateNegotiateRequest) (string, error) { return "", nil }
	authenticateBasic := func(context.Context, NetworkAuthenticateBasicRequest) (*NetworkAuthenticateBasicResult, error) {
		return nil, nil
	}

	t.Run("registers all callbacks and answers concurrent lookups", func(t *testing.T) {
		var mux sync.Mutex
		var calls []string
		server, peer := startNetworkClient(t, &NetworkConfig{
			ResolveProxy: func(_ context.Context, request NetworkResolveProxyRequest) (string, error) {
				mux.Lock()
				calls = append(calls, "resolveProxy "+request.URL)
				mux.Unlock()
				if strings.HasPrefix(request.URL, "https:") {
					return "PROXY proxy.example:8080; DIRECT", nil
				}
				return "", nil
			},
			AuthenticateNegotiate: func(_ context.Context, request NetworkAuthenticateNegotiateRequest) (string, error) {
				mux.Lock()
				calls = append(calls, "authenticateNegotiate "+request.Spn)
				mux.Unlock()
				return base64.StdEncoding.EncodeToString([]byte(request.Spn)), nil
			},
			AuthenticateBasic: func(_ context.Context, request NetworkAuthenticateBasicRequest) (*NetworkAuthenticateBasicResult, error) {
				mux.Lock()
				calls = append(calls, fmt.Sprintf("authenticateBasic %s realm %q previousRejected %t", request.Proxy, *request.Realm, request.PreviousRejected))
				mux.Unlock()
				username, password := "alice", "secret"
				return &NetworkAuthenticateBasicResult{Username: &username, Password: &password}, nil
			},
		})

		responses := sendNetworkRequests(t, peer,
			networkRequest{"network.resolveProxy", map[string]string{"url": "https://api.example.com/"}},
			networkRequest{"network.resolveProxy", map[string]string{"url": "http://example.com/path"}},
			networkRequest{"network.authenticateNegotiate", map[string]string{"spn": "HTTP/proxy.example"}},
			networkRequest{"network.authenticateBasic", map[string]any{"proxy": "https://proxy.example:443", "realm": "corp", "previousRejected": true}},
		)
		type snapshot struct {
			Requests  []string
			Calls     []string
			Responses []string
		}
		mux.Lock()
		slices.Sort(calls)
		got := snapshot{server.recorded(), calls, responses}
		mux.Unlock()
		want := snapshot{
			Requests: []string{`network.setProvider {"authenticateBasic":{},"authenticateNegotiate":{},"resolveProxy":{}}`},
			Calls: []string{
				`authenticateBasic https://proxy.example:443 realm "corp" previousRejected true`,
				"authenticateNegotiate HTTP/proxy.example",
				"resolveProxy http://example.com/path",
				"resolveProxy https://api.example.com/",
			},
			Responses: []string{
				`{"proxy":"PROXY proxy.example:8080; DIRECT"}`,
				`{}`,
				`{"token":"` + base64.StdEncoding.EncodeToString([]byte("HTTP/proxy.example")) + `"}`,
				`{"password":"secret","username":"alice"}`,
			},
		}
		if !reflect.DeepEqual(got, want) {
			t.Fatalf("got %+v, want %+v", got, want)
		}
	})

	t.Run("registers only configured callbacks and scope", func(t *testing.T) {
		for _, tc := range []struct {
			name    string
			network *NetworkConfig
			want    []string
		}{
			{"unset", nil, nil},
			{"empty", &NetworkConfig{}, nil},
			{"scope without callbacks", &NetworkConfig{Scope: NetworkScopeRuntime}, nil},
			{"proxy resolution", &NetworkConfig{ResolveProxy: resolveProxy}, []string{`network.setProvider {"resolveProxy":{}}`}},
			{
				"runtime Negotiate authentication",
				&NetworkConfig{Scope: NetworkScopeRuntime, AuthenticateNegotiate: authenticateNegotiate},
				[]string{`network.setProvider {"authenticateNegotiate":{},"scope":"runtime"}`},
			},
			{"Basic authentication", &NetworkConfig{AuthenticateBasic: authenticateBasic}, []string{`network.setProvider {"authenticateBasic":{}}`}},
			{
				"connection proxy resolution",
				&NetworkConfig{Scope: NetworkScopeConnection, ResolveProxy: resolveProxy},
				[]string{`network.setProvider {"resolveProxy":{},"scope":"connection"}`},
			},
		} {
			t.Run(tc.name, func(t *testing.T) {
				server, _ := startNetworkClient(t, tc.network)
				if got := server.recorded(); !reflect.DeepEqual(got, tc.want) {
					t.Fatalf("requests = %v, want %v", got, tc.want)
				}
			})
		}
	})

	t.Run("answers only configured callbacks", func(t *testing.T) {
		_, peer := startNetworkClient(t, &NetworkConfig{
			ResolveProxy: func(context.Context, NetworkResolveProxyRequest) (string, error) { return "DIRECT", nil },
		})

		got := sendNetworkRequests(t, peer,
			networkRequest{"network.resolveProxy", map[string]string{"url": "https://api.example.com/"}},
			networkRequest{"network.authenticateNegotiate", map[string]string{"spn": "HTTP/proxy"}},
			networkRequest{"network.authenticateBasic", map[string]any{"proxy": "http://proxy:8080", "previousRejected": false}},
		)
		want := []string{
			`{"proxy":"DIRECT"}`,
			"JSON-RPC Error -32603: No network client-global handler registered",
			"JSON-RPC Error -32603: No network client-global handler registered",
		}
		if !reflect.DeepEqual(got, want) {
			t.Fatalf("responses = %q, want %q", got, want)
		}
	})

	t.Run("registers the configuration NewClient received", func(t *testing.T) {
		server := newNetworkServer(t, false)
		network := &NetworkConfig{ResolveProxy: resolveProxy}
		client := NewClient(&ClientOptions{Connection: URIConnection{URL: server.url}, Network: network})
		t.Cleanup(client.ForceStop)
		*network = NetworkConfig{Scope: NetworkScopeRuntime, AuthenticateNegotiate: authenticateNegotiate}

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Start failed: %v", err)
		}
		want := []string{`network.setProvider {"resolveProxy":{}}`}
		if got := server.recorded(); !reflect.DeepEqual(got, want) {
			t.Fatalf("requests = %v, want %v", got, want)
		}
	})

	t.Run("registers before sessions requested during startup", func(t *testing.T) {
		server := newNetworkServer(t, true)
		client := NewClient(&ClientOptions{
			Connection: URIConnection{URL: server.url},
			Network:    &NetworkConfig{AuthenticateNegotiate: authenticateNegotiate},
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

		want := []string{`network.setProvider {"authenticateNegotiate":{}}`, "session.create"}
		if got := server.recorded(); !reflect.DeepEqual(got, want) {
			t.Fatalf("requests = %v, want %v", got, want)
		}
	})

	t.Run("answers missing values and rejects callback failures", func(t *testing.T) {
		_, peer := startNetworkClient(t, networkCallbacks(func(_ context.Context, target string) (string, error) {
			switch {
			case strings.Contains(target, "missing"):
				return "", nil
			case strings.Contains(target, "panic"):
				panic("boom")
			default:
				// The value is ignored with the error.
				return "PROXY ignored.example:8080", errors.New("lookup failed")
			}
		}))

		got := sendNetworkRequests(t, peer,
			networkRequest{"network.resolveProxy", map[string]string{"url": "https://missing.example/"}},
			networkRequest{"network.authenticateNegotiate", map[string]string{"spn": "HTTP/missing.example"}},
			networkRequest{"network.authenticateBasic", map[string]any{"proxy": "http://missing.example:8080", "previousRejected": false}},
			networkRequest{"network.resolveProxy", map[string]string{"url": "https://fail.example/"}},
			networkRequest{"network.authenticateNegotiate", map[string]string{"spn": "HTTP/fail.example"}},
			networkRequest{"network.authenticateBasic", map[string]any{"proxy": "http://fail.example:8080", "previousRejected": false}},
			networkRequest{"network.resolveProxy", map[string]string{"url": "https://panic.example/"}},
			networkRequest{"network.authenticateNegotiate", map[string]string{"spn": "HTTP/panic.example"}},
			networkRequest{"network.authenticateBasic", map[string]any{"proxy": "http://panic.example:8080", "previousRejected": false}},
		)
		want := []string{
			`{}`,
			`{}`,
			`{}`,
			"JSON-RPC Error -32603: lookup failed",
			"JSON-RPC Error -32603: lookup failed",
			"JSON-RPC Error -32603: lookup failed",
			"JSON-RPC Error -32603: proxy resolution callback panicked: boom",
			"JSON-RPC Error -32603: negotiate authentication callback panicked: boom",
			"JSON-RPC Error -32603: basic authentication callback panicked: boom",
		}
		if !reflect.DeepEqual(got, want) {
			t.Fatalf("responses = %q, want %q", got, want)
		}
	})

	for _, m := range methods {
		t.Run("cancels the "+m.method+" callback and answers cancelled when the runtime cancels the request", func(t *testing.T) {
			started, returned := make(chan context.Context, 1), make(chan struct{})
			// Answers only after cancellation: the SDK must ignore the late result.
			_, peer := newNetworkPeer(t, networkCallbacks(func(ctx context.Context, _ string) (string, error) {
				defer close(returned)
				started <- ctx
				<-ctx.Done()
				return "late", nil
			}))

			peer.request(1, m.method, m.params)
			var callbackCtx context.Context
			select {
			case callbackCtx = <-started:
			case <-time.After(10 * time.Second):
				t.Fatal("timed out waiting for callback start")
			}
			errBeforeCancel := callbackCtx.Err()
			peer.notify("$/cancelRequest", map[string]any{"id": 1})
			response := peer.readResponse(1)
			waitForClosed(t, returned, "callback return after $/cancelRequest")

			type cancellation struct {
				ErrBeforeCancel error
				Err             error
				Result          string
				Error           *jsonrpc2.Error
			}
			got := cancellation{errBeforeCancel, callbackCtx.Err(), string(response.Result), response.Error}
			want := cancellation{nil, context.Canceled, "", &jsonrpc2.Error{Code: requestCancelledCode, Message: m.label + " request cancelled"}}
			if !reflect.DeepEqual(got, want) {
				t.Fatalf("got %+v, want %+v", got, want)
			}
		})

		t.Run("does not start a "+m.method+" callback cancelled while it was queued", func(t *testing.T) {
			invoked, skipped := make(chan struct{}), make(chan struct{})
			ctx, cancel := context.WithCancel(context.Background())
			defer cancel()
			adapter := &networkAdapter{
				config: *networkCallbacks(func(context.Context, string) (string, error) {
					close(invoked)
					return "answer", nil
				}),
				connectionClosed: make(chan struct{}),
				testHookCallbackGoroutine: func(stage string) {
					switch stage {
					case "starting":
						// The runtime cancels after the handler's own check, before the callback runs.
						cancel()
					case "skipped":
						close(skipped)
					}
				},
			}
			params, err := json.Marshal(m.params)
			if err != nil {
				t.Fatal(err)
			}

			_, rpcErr := m.handle(adapter, ctx, params)

			select {
			case <-skipped:
			case <-invoked:
				t.Fatal("the cancelled callback was started")
			case <-time.After(5 * time.Second):
				t.Fatal("timed out waiting for the callback goroutine")
			}
			want := &jsonrpc2.Error{Code: requestCancelledCode, Message: m.label + " request cancelled"}
			if !reflect.DeepEqual(rpcErr, want) {
				t.Fatalf("response error = %+v, want %+v", rpcErr, want)
			}
		})

		t.Run("cancels the "+m.method+" callback when the connection closes", func(t *testing.T) {
			entered, returned := make(chan struct{}), make(chan struct{})
			connectionClosed := make(chan struct{})
			adapter := &networkAdapter{
				config:           *networkCallbacks(blockingLookup(entered, returned)),
				connectionClosed: connectionClosed,
			}
			params, err := json.Marshal(m.params)
			if err != nil {
				t.Fatal(err)
			}

			answered := make(chan *jsonrpc2.Error, 1)
			go func() {
				_, rpcErr := m.handle(adapter, t.Context(), params)
				answered <- rpcErr
			}()
			waitForClosed(t, entered, "callback start")
			close(connectionClosed)
			waitForClosed(t, returned, "callback return after the connection closed")

			select {
			case rpcErr := <-answered:
				want := &jsonrpc2.Error{Code: jsonrpc2.ErrInternal.Code, Message: m.label + " connection closed"}
				if !reflect.DeepEqual(rpcErr, want) {
					t.Fatalf("response error = %+v, want %+v", rpcErr, want)
				}
			case <-time.After(10 * time.Second):
				t.Fatal("timed out waiting for the handler to answer")
			}
		})

		t.Run("fails a "+m.method+" callback that answers as the connection closes", func(t *testing.T) {
			params, err := json.Marshal(m.params)
			if err != nil {
				t.Fatal(err)
			}
			// The answer races the watcher that cancels the callback's context, so repeat it.
			var answered []string
			for range 100 {
				connectionClosed := make(chan struct{})
				adapter := &networkAdapter{
					config: *networkCallbacks(func(context.Context, string) (string, error) {
						close(connectionClosed)
						return "late", nil
					}),
					connectionClosed: connectionClosed,
				}
				result, rpcErr := m.handle(adapter, t.Context(), params)
				want := &jsonrpc2.Error{Code: jsonrpc2.ErrInternal.Code, Message: m.label + " connection closed"}
				if result != nil || !reflect.DeepEqual(rpcErr, want) {
					answered = append(answered, fmt.Sprintf("result %s, error %+v", result, rpcErr))
				}
			}
			if len(answered) > 0 {
				t.Fatalf("%d of 100 answers were not discarded, first: %s", len(answered), answered[0])
			}
		})

		t.Run("cancels the "+m.method+" callback when the client stops", func(t *testing.T) {
			entered, returned := make(chan struct{}), make(chan struct{})
			client, peer := newNetworkPeer(t, networkCallbacks(blockingLookup(entered, returned)))

			peer.request(1, m.method, m.params)
			waitForClosed(t, entered, "callback start")
			if err := client.Stop(); err != nil {
				t.Fatalf("Stop failed: %v", err)
			}
			waitForClosed(t, returned, "callback return after Stop")
		})
	}

	t.Run("closes the connection when registration fails", func(t *testing.T) {
		// For example, another connection already holds the runtime scope.
		server := newNetworkServer(t, false)
		server.failRequest("network.setProvider", false)
		client := NewClient(&ClientOptions{
			Connection: URIConnection{URL: server.url},
			Network:    &NetworkConfig{Scope: NetworkScopeRuntime, ResolveProxy: resolveProxy},
		})
		t.Cleanup(client.ForceStop)

		err := client.Start(t.Context())
		if err == nil || !strings.Contains(err.Error(), "network.setProvider failed") {
			t.Fatalf("Start returned %v", err)
		}
		peer := <-server.connected
		waitForClosed(t, peer.ConnectionClosed(), "the connection to close")

		type snapshot struct {
			Requests  []string
			State     connectionState
			Transport bool
		}
		got := snapshot{server.recorded(), client.state, client.client != nil || client.conn != nil || client.RPC != nil || client.internalRPC != nil}
		want := snapshot{[]string{`network.setProvider {"resolveProxy":{},"scope":"runtime"}`}, stateError, false}
		if !reflect.DeepEqual(got, want) {
			t.Fatalf("got %+v, want %+v", got, want)
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
					server := newNetworkServer(t, false)
					server.failRequest(step.method, cancelled)
					options := step.options
					options.Connection = URIConnection{URL: server.url}
					options.Network = &NetworkConfig{Scope: NetworkScopeRuntime, AuthenticateNegotiate: authenticateNegotiate}
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
					want := snapshot{[]string{`network.setProvider {"authenticateNegotiate":{},"scope":"runtime"}`, step.method}, stateError, false}
					if !reflect.DeepEqual(got, want) {
						t.Fatalf("got %+v, want %+v", got, want)
					}
				})
			}
		}
	})
}
