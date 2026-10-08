package copilot

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"strings"
	"sync"
	"sync/atomic"
	"testing"
	"time"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
)

type ahpTestLog struct {
	sync.Mutex
	bytes.Buffer
}

func (b *ahpTestLog) Write(data []byte) (int, error) {
	b.Lock()
	defer b.Unlock()
	return b.Buffer.Write(data)
}

func (b *ahpTestLog) text() string {
	b.Lock()
	defer b.Unlock()
	return b.String()
}

func TestAhpHostRequiresExplicitTransport(t *testing.T) {
	for _, options := range []*AhpHostOptions{nil, {}} {
		client := NewClient(nil)
		if _, err := client.StartAhpHost(t.Context(), options); err == nil || !strings.Contains(err.Error(), "requires localServer or githubEnvironment") {
			t.Fatalf("expected missing transport error, got %v", err)
		}
	}
}

func TestAhpHostTransportsAndOptionalResults(t *testing.T) {
	for _, transport := range []string{"local", "github", "both"} {
		t.Run(transport, func(t *testing.T) {
			client, server, _ := ahpFixture(t)
			options := &AhpHostOptions{ComputeID: String("compute")}
			want := map[string]any{"computeId": "compute"}
			if transport != "github" {
				port := int32(0)
				options.LocalServer = &rpc.HostLocalServerOptions{
					Hostname: String("127.0.0.1"), Port: &port,
					Token: String("secret"), RequireConnectionToken: Bool(true),
				}
				want["localServer"] = map[string]any{
					"hostname": "127.0.0.1", "port": float64(0),
					"token": "secret", "requireConnectionToken": true,
				}
			}
			if transport != "local" {
				options.GitHubEnvironment = &rpc.HostGitHubEnvironmentOptions{Name: "SDK host", ComputeID: "compute"}
				want["githubEnvironment"] = map[string]any{"name": "SDK host", "computeId": "compute"}
			}
			server.SetRequestHandler("host.start", func(data json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				var params map[string]any
				if err := json.Unmarshal(data, &params); err != nil {
					t.Error(err)
				}
				hostID := params["hostId"]
				delete(params, "hostId")
				got, _ := json.Marshal(params)
				expected, _ := json.Marshal(want)
				if !bytes.Equal(got, expected) {
					t.Errorf("transport request = %s, want %s", got, expected)
				}
				result := map[string]any{"hostId": hostID}
				if transport != "github" {
					result["url"] = "ws://127.0.0.1:12345"
				}
				if transport != "local" {
					result["environmentId"] = "environment"
				}
				response, _ := json.Marshal(result)
				return response, nil
			})
			host, err := client.StartAhpHost(t.Context(), options)
			if err != nil {
				t.Fatal(err)
			}
			if host.Token != nil || host.PID != nil {
				t.Fatal("absent result fields must remain nil")
			}
			if transport == "github" {
				if host.URL != nil {
					t.Fatal("Mission Control-only host has a local URL")
				}
			} else if host.URL == nil || *host.URL != "ws://127.0.0.1:12345" {
				t.Fatalf("incorrect local URL: %v", host.URL)
			}
			if transport == "local" {
				if host.EnvironmentID != nil {
					t.Fatal("local-only host has an environment ID")
				}
			} else if host.EnvironmentID == nil || *host.EnvironmentID != "environment" {
				t.Fatalf("incorrect environment ID: %v", host.EnvironmentID)
			}
		})
	}
}

func TestAhpHostListSessionsUsesOwningHost(t *testing.T) {
	client, server, _ := ahpFixture(t)
	host, err := client.StartAhpHost(t.Context(), &AhpHostOptions{LocalServer: &rpc.HostLocalServerOptions{}})
	if err != nil {
		t.Fatal(err)
	}
	server.SetRequestHandler("host.listSessions", jsonrpc2.RequestHandlerFor(func(params *rpc.HostListSessionsRequest) (*rpc.HostListSessionsResult, *jsonrpc2.Error) {
		if params.HostID == nil || *params.HostID != host.HostID {
			t.Errorf("hostId = %v, want %q", params.HostID, host.HostID)
		}
		return &rpc.HostListSessionsResult{Sessions: []rpc.HostSessionSummary{}}, nil
	}))
	result, err := host.ListSessions(t.Context())
	if err != nil {
		t.Fatal(err)
	}
	if result.Sessions == nil || len(result.Sessions) != 0 {
		t.Fatalf("sessions = %#v, want an empty catalog", result.Sessions)
	}
}

func TestAhpFactoryCancellationLogging(t *testing.T) {
	output := new(ahpTestLog)
	previous := log.Writer()
	log.SetOutput(output)
	defer log.SetOutput(previous)
	for _, released := range []bool{true, false} {
		client, server, _ := ahpFixture(t)
		entered := make(chan struct{}, 1)
		finish := make(chan struct{})
		host, err := client.StartAhpHost(t.Context(), &AhpHostOptions{
			LocalServer: &rpc.HostLocalServerOptions{},
			CreateSession: func(ctx context.Context, _ AhpSessionCreateRequest) (*Session, error) {
				entered <- struct{}{}
				<-finish
				err := ctx.Err()
				if err == nil {
					err = context.Canceled
				}
				return nil, fmt.Errorf("factory cancelled: %w", err)
			},
		})
		if err != nil {
			t.Fatal(err)
		}
		done := make(chan error, 1)
		go func() {
			_, err := server.Request(t.Context(), "host.materializeSession", &rpc.HostSessionCreateCallback{
				HostID: host.HostID, HandoffID: "cancelled", Config: map[string]any{"sessionId": "requested"},
			})
			done <- err
		}()
		awaitAhpTest(t, entered)
		if released {
			client.releaseAhpSession(&rpc.HostSessionReleasedNotification{HostID: host.HostID, HandoffID: "cancelled"})
		}
		close(finish)
		if err := awaitAhpTest(t, done); err == nil {
			t.Fatal("expected failed handoff")
		}
		if released {
			time.Sleep(200 * time.Millisecond)
			if strings.Contains(output.text(), "AHP session factory failed") {
				t.Fatal("expected cooperative cancellation was logged")
			}
		} else if !strings.Contains(output.text(), "AHP session factory failed") {
			t.Fatal("unexpected factory cancellation was not logged")
		}
	}
}

func TestAhpCancelledStartupDisposesAfterStartCompletes(t *testing.T) {
	client, server, _ := ahpFixture(t)
	entered := make(chan string, 1)
	finish := make(chan struct{})
	disposed := make(chan string, 1)
	server.SetRequestHandler("host.start", jsonrpc2.RequestHandlerFor(func(params *rpc.HostStartRequest) (*rpc.HostStartResult, *jsonrpc2.Error) {
		entered <- params.HostID
		<-finish
		return &rpc.HostStartResult{HostID: params.HostID, URL: String("ws://127.0.0.1:12345")}, nil
	}))
	server.SetRequestHandler("host.dispose", jsonrpc2.RequestHandlerFor(func(params *rpc.HostDisposeRequest) (map[string]any, *jsonrpc2.Error) {
		disposed <- params.HostID
		return map[string]any{}, nil
	}))
	ctx, cancel := context.WithCancel(t.Context())
	done := make(chan error, 1)
	go func() {
		_, err := client.StartAhpHost(ctx, &AhpHostOptions{LocalServer: &rpc.HostLocalServerOptions{}})
		done <- err
	}()
	hostID := awaitAhpTest(t, entered)
	cancel()
	if err := awaitAhpTest(t, done); !errors.Is(err, context.Canceled) {
		t.Fatalf("expected cancellation, got %v", err)
	}
	if len(disposed) != 0 {
		t.Fatal("disposed before startup settled")
	}
	client.startStopMux.Lock()
	client.RPC = nil
	client.startStopMux.Unlock()
	close(finish)
	if got := awaitAhpTest(t, disposed); got != hostID {
		t.Fatalf("disposed %s instead of abandoned %s", got, hostID)
	}
}

func ahpFixture(t *testing.T) (*Client, *jsonrpc2.Client, *atomic.Int32) {
	t.Helper()
	transport, server, _ := newRuntimeShutdownRpcPair(t)
	client := NewClient(nil)
	client.client, client.RPC, client.state = transport, rpc.NewServerRPC(transport), stateConnected
	client.setupNotificationHandler()
	t.Cleanup(func() { client.ForceStop(); server.Stop() })
	disposals := new(atomic.Int32)
	server.SetRequestHandler("host.start", jsonrpc2.RequestHandlerFor(func(params *rpc.HostStartRequest) (*rpc.HostStartResult, *jsonrpc2.Error) {
		return &rpc.HostStartResult{HostID: params.HostID, URL: String("ws://127.0.0.1:12345"), Token: String("secret")}, nil
	}))
	server.SetRequestHandler("host.dispose", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		disposals.Add(1)
		return []byte(`{}`), nil
	})
	for _, method := range []string{"session.create", "session.resume"} {
		server.SetRequestHandler(method, func(data json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			var request struct {
				SessionID string `json:"sessionId"`
			}
			if err := json.Unmarshal(data, &request); err != nil {
				t.Error(err)
			}
			response, err := json.Marshal(map[string]any{"sessionId": request.SessionID})
			if err != nil {
				t.Error(err)
			}
			return response, nil
		})
	}
	for _, method := range []string{"session.options.update", "session.detach"} {
		server.SetRequestHandler(method, func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			return []byte(`{}`), nil
		})
	}
	return client, server, disposals
}

func awaitAhpTest[T any](t *testing.T, values <-chan T) T {
	t.Helper()
	select {
	case value := <-values:
		return value
	case <-time.After(5 * time.Second):
		t.Fatal("timed out waiting for AHP callback")
		var zero T
		return zero
	}
}

func TestAhpHostOriginalTransportAndExitOnce(t *testing.T) {
	client, _, disposals := ahpFixture(t)
	exits := make(chan AhpHostExit, 4)
	host, err := client.StartAhpHost(t.Context(), &AhpHostOptions{LocalServer: &rpc.HostLocalServerOptions{}, OnExit: func(event AhpHostExit) error {
		exits <- event
		return nil
	}})
	if err != nil {
		t.Fatal(err)
	}
	client.RPC = nil
	done := make(chan error, 2)
	for range 2 {
		go func() { done <- host.Dispose(t.Context()) }()
	}
	for range 2 {
		if err := awaitAhpTest(t, done); err != nil {
			t.Fatal(err)
		}
	}
	if disposals.Load() != 2 || host.PID != nil {
		t.Fatalf("incorrect disposal/PID: %d %v", disposals.Load(), host.PID)
	}
	for range 2 {
		client.handleAhpExit(&rpc.HostExitedNotification{HostID: host.HostID, Reason: "disposed"})
	}
	if awaitAhpTest(t, exits).Reason != "disposed" {
		t.Fatal("wrong exit")
	}
	client.disconnectAhpHosts()
	if len(exits) != 0 {
		t.Fatal("duplicate exit callback")
	}
}

func TestAhpFactoriesPreserveSettingsAndReleaseExactOriginal(t *testing.T) {
	for _, flags := range []struct{ resume, enabled bool }{{false, false}, {false, true}, {true, false}, {true, true}} {
		resume, enabled := flags.resume, flags.enabled
		t.Run(fmt.Sprintf("resume=%t/enabled=%t", resume, enabled), func(t *testing.T) {
			client, server, _ := ahpFixture(t)
			released := make(chan *Session, 4)
			original := make(chan *Session, 1)
			factoryCtx := make(chan context.Context, 1)
			host, err := client.StartAhpHost(t.Context(), &AhpHostOptions{
				LocalServer: &rpc.HostLocalServerOptions{},
				CreateSession: func(ctx context.Context, request AhpSessionCreateRequest) (*Session, error) {
					factoryCtx <- ctx
					session, err := client.CreateSession(ctx, request.Config)
					original <- session
					return session, err
				},
				ResumeSession: func(ctx context.Context, request AhpSessionResumeRequest) (*Session, error) {
					factoryCtx <- ctx
					session, err := client.ResumeSessionWithOptions(ctx, request.SessionID, request.Config)
					original <- session
					return session, err
				},
				OnSessionReleased: func(session *Session) error { released <- session; return nil },
			})
			if err != nil {
				t.Fatal(err)
			}
			config := map[string]any{
				"sessionId": "original", "workingDirectory": "/workspace",
				"additionalDirectories": []string{}, "gitHubToken": "test-auth",
				"mcpOAuthTokenStorage": "in-memory", "enableMcpApps": enabled, "streaming": enabled,
				"enableExperimentalMode": false,
				"infiniteSessions":       map[string]any{"enabled": false, "backgroundCompactionThreshold": 0.7},
				"featureFlags":           map[string]any{"Arbitrary.MixedCase": true},
			}
			if resume {
				config["continuePendingWork"] = false
				config["suppressResumeEvent"] = enabled
			}
			if _, err := server.Request(t.Context(), "host.materializeSession", &rpc.HostSessionCreateCallback{
				HostID: host.HostID, HandoffID: "handoff", Resume: Bool(resume), Config: config,
			}); err != nil {
				t.Fatal(err)
			}
			session := awaitAhpTest(t, original)
			context := awaitAhpTest(t, factoryCtx)
			client.releaseAhpSession(&rpc.HostSessionReleasedNotification{
				HostID: "wrong-host", HandoffID: "handoff",
			})
			if context.Err() != nil {
				t.Fatal("wrong host cancelled participation")
			}
			for range 2 {
				client.releaseAhpSession(&rpc.HostSessionReleasedNotification{
					HostID: host.HostID, HandoffID: "handoff",
				})
			}
			if awaitAhpTest(t, released) != session || context.Err() == nil || len(released) != 0 {
				t.Fatal("release lost identity, cancellation, or exactly-once semantics")
			}
			if client.sessions["original"] != session {
				t.Fatal("release destroyed the application session")
			}
		})
	}
}

func TestAhpRejectsChangedBooleanSettings(t *testing.T) {
	for _, setting := range []string{"streaming", "enableMcpApps", "suppressResumeEvent"} {
		for _, expected := range []bool{false, true} {
			t.Run(fmt.Sprintf("%s=%t", setting, expected), func(t *testing.T) {
				client, server, _ := ahpFixture(t)
				host, err := client.StartAhpHost(t.Context(), &AhpHostOptions{
					LocalServer: &rpc.HostLocalServerOptions{},
					ResumeSession: func(ctx context.Context, request AhpSessionResumeRequest) (*Session, error) {
						switch setting {
						case "streaming":
							request.Config.Streaming = Bool(!expected)
						case "enableMcpApps":
							request.Config.EnableMCPApps = !expected
						case "suppressResumeEvent":
							request.Config.SuppressResumeEvent = !expected
						}
						return client.ResumeSessionWithOptions(ctx, request.SessionID, request.Config)
					},
				})
				if err != nil {
					t.Fatal(err)
				}
				_, err = server.Request(t.Context(), "host.materializeSession", &rpc.HostSessionCreateCallback{
					HostID: host.HostID, HandoffID: "changed", Resume: Bool(true),
					Config: map[string]any{"sessionId": "changed", setting: expected},
				})
				if err == nil || !strings.Contains(err.Error(), "preserve") {
					t.Fatalf("expected configuration preservation error, got %v", err)
				}
			})
		}
	}
}

func TestAhpCancellationReleasesLateResultAndRejectsDuplicate(t *testing.T) {
	client, server, _ := ahpFixture(t)
	entered := make(chan context.Context, 1)
	unblock := make(chan struct{})
	released := make(chan *Session, 2)
	late := &Session{SessionID: "late"}
	host, err := client.StartAhpHost(t.Context(), &AhpHostOptions{
		LocalServer: &rpc.HostLocalServerOptions{},
		CreateSession: func(ctx context.Context, _ AhpSessionCreateRequest) (*Session, error) {
			entered <- ctx
			<-unblock
			return late, nil
		},
		OnSessionReleased: func(session *Session) error { released <- session; return nil },
	})
	if err != nil {
		t.Fatal(err)
	}
	params := &rpc.HostSessionCreateCallback{
		HostID: host.HostID, HandoffID: "pending", Config: map[string]any{"sessionId": "late"},
	}
	done := make(chan error, 1)
	go func() { _, err := server.Request(t.Context(), "host.materializeSession", params); done <- err }()
	ctx := awaitAhpTest(t, entered)
	if _, err := server.Request(t.Context(), "host.materializeSession", params); err == nil {
		t.Fatal("duplicate handoff accepted")
	}
	client.releaseAhpSession(&rpc.HostSessionReleasedNotification{
		HostID: host.HostID, HandoffID: "pending",
	})
	if err := awaitAhpTest(t, done); err == nil || !strings.Contains(err.Error(), "handoff ended") || ctx.Err() == nil {
		t.Fatalf("pending handoff did not cancel: %v", err)
	}
	close(unblock)
	if awaitAhpTest(t, released) != late {
		t.Fatal("late result was not released")
	}
}

func TestAhpRejectsModifiedSettingsAndAllowsRetainedResume(t *testing.T) {
	client, server, _ := ahpFixture(t)
	released := make(chan *Session, 1)
	var original *Session
	host, err := client.StartAhpHost(t.Context(), &AhpHostOptions{
		LocalServer: &rpc.HostLocalServerOptions{},
		CreateSession: func(ctx context.Context, request AhpSessionCreateRequest) (*Session, error) {
			request.Config.WorkingDirectory = "/wrong"
			session, err := client.CreateSession(ctx, request.Config)
			original = session
			return session, err
		},
		ResumeSession:     func(context.Context, AhpSessionResumeRequest) (*Session, error) { return original, nil },
		OnSessionReleased: func(session *Session) error { released <- session; return nil },
	})
	if err != nil {
		t.Fatal(err)
	}
	params := &rpc.HostSessionCreateCallback{
		HostID: host.HostID, HandoffID: "create", Config: map[string]any{"sessionId": "retained", "workingDirectory": "/selected"},
	}
	if _, err := server.Request(t.Context(), "host.materializeSession", params); err == nil || !strings.Contains(err.Error(), "configuration") {
		t.Fatalf("modified settings accepted: %v", err)
	}
	if awaitAhpTest(t, released) != original {
		t.Fatal("rejected session was not released")
	}
	params.HandoffID, params.Resume = "resume", Bool(true)
	if _, err := server.Request(t.Context(), "host.materializeSession", params); err != nil {
		t.Fatal(err)
	}
}
