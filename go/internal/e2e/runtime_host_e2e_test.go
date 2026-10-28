package e2e

import (
	"context"
	"encoding/json"
	"fmt"
	"os"
	"strings"
	"sync"
	"sync/atomic"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

const ahpToolPrompt = "Use the magic_number tool with seed 'hello' and tell me the result"
const ahpComposedPrompt = "Call magic_number with seed 'hello' and client_echo with text 'ping', then report both results"
const ahpPromptMarker = "APPLICATION_OWNED_AHP_PROMPT"

type ahpTestResponse struct {
	ClientID        string `json:"clientId"`
	SessionID       string `json:"sessionId"`
	Text            string `json:"text"`
	ClientToolCalls int    `json:"clientToolCalls"`
	History         []struct {
		Message struct {
			Text string `json:"text"`
		} `json:"message"`
	} `json:"history"`
}

func driveAhp(t *testing.T, ahp *testharness.AhpTestClient, command map[string]any) ahpTestResponse {
	t.Helper()
	deadline, cancel := context.WithTimeout(context.Background(), time.Minute)
	defer cancel()
	data, err := ahp.Request(deadline, command)
	if err != nil {
		t.Fatal(err)
	}
	var response ahpTestResponse
	if err := json.Unmarshal(data, &response); err != nil {
		t.Fatal(err)
	}
	return response
}

func connectAhpTest(t *testing.T, ahp *testharness.AhpTestClient, host *copilot.AhpHost, clientID string) (string, func()) {
	t.Helper()
	if host.URL == nil {
		t.Fatal("local host did not return a URL")
	}
	command := map[string]any{"op": "connect", "url": *host.URL, "githubToken": "fake-token-for-e2e-tests"}
	if host.Token != nil {
		command["token"] = *host.Token
	}
	if clientID != "" {
		command["clientId"] = clientID
	}
	id := driveAhp(t, ahp, command).ClientID
	var once sync.Once
	close := func() { once.Do(func() { driveAhp(t, ahp, map[string]any{"op": "close", "clientId": id}) }) }
	t.Cleanup(close)
	return id, close
}

type ahpApplication struct {
	client   *copilot.Client
	workDir  string
	session  atomic.Pointer[copilot.Session]
	creates  atomic.Int32
	resumes  atomic.Int32
	createID atomic.Value
	resumeID atomic.Value
	tools    atomic.Int32
	hooks    atomic.Int32
	released chan *copilot.Session
	exited   chan copilot.AhpHostExit
}

func newAhpApplication(client *copilot.Client, workDir string) *ahpApplication {
	return &ahpApplication{
		client: client, workDir: workDir,
		released: make(chan *copilot.Session, 4), exited: make(chan copilot.AhpHostExit, 4),
	}
}

func (a *ahpApplication) callbacks() ([]copilot.Tool, *copilot.SessionHooks) {
	type seed struct {
		Seed string `json:"seed" jsonschema:"A seed value"`
	}
	tool := copilot.DefineTool("magic_number", "Returns a magic number", func(args seed, invocation copilot.ToolInvocation) (string, error) {
		if args.Seed != "hello" || a.session.Load() == nil || invocation.SessionID != a.session.Load().SessionID {
			return "", fmt.Errorf("wrong tool arguments or session identity")
		}
		a.tools.Add(1)
		return "MAGIC_hello_42", nil
	})
	hooks := &copilot.SessionHooks{
		OnPreToolUse: func(_ copilot.PreToolUseHookInput, invocation copilot.HookInvocation) (*copilot.PreToolUseHookOutput, error) {
			if a.session.Load() == nil || invocation.SessionID != a.session.Load().SessionID {
				return nil, fmt.Errorf("wrong hook session identity")
			}
			a.hooks.Add(1)
			return nil, nil
		},
	}
	return []copilot.Tool{tool}, hooks
}

func (a *ahpApplication) createConfig(config *copilot.SessionConfig) {
	config.OnPermissionRequest = copilot.PermissionHandler.ApproveAll
	config.SystemMessage = &copilot.SystemMessageConfig{Mode: "append", Content: ahpPromptMarker}
	config.Tools, config.Hooks = a.callbacks()
}

func (a *ahpApplication) options() *copilot.AhpHostOptions {
	return &copilot.AhpHostOptions{
		LocalServer: &rpc.HostLocalServerOptions{},
		CreateSession: func(ctx context.Context, request copilot.AhpSessionCreateRequest) (*copilot.Session, error) {
			a.creates.Add(1)
			if ctx.Err() != nil || request.Config.WorkingDirectory != a.workDir {
				return nil, fmt.Errorf("invalid create handoff")
			}
			if request.Config.SessionID == "" {
				return nil, fmt.Errorf("create handoff omitted runtime session identity")
			}
			a.createID.Store(request.Config.SessionID)
			a.createConfig(request.Config)
			session, err := a.client.CreateSession(ctx, request.Config)
			if err == nil && session.SessionID != request.Config.SessionID {
				return nil, fmt.Errorf("create returned %q, requested %q", session.SessionID, request.Config.SessionID)
			}
			a.session.Store(session)
			return session, err
		},
		ResumeSession: func(ctx context.Context, request copilot.AhpSessionResumeRequest) (*copilot.Session, error) {
			a.resumes.Add(1)
			a.resumeID.Store(request.SessionID)
			config := request.Config
			if ctx.Err() != nil || config.WorkingDirectory != a.workDir ||
				config.ContinuePendingWork == nil || *config.ContinuePendingWork {
				return nil, fmt.Errorf("invalid resume handoff")
			}
			config.OnPermissionRequest = copilot.PermissionHandler.ApproveAll
			config.SystemMessage = &copilot.SystemMessageConfig{Mode: "append", Content: ahpPromptMarker}
			config.Tools, config.Hooks = a.callbacks()
			session, err := a.client.ResumeSessionWithOptions(ctx, request.SessionID, config)
			if err == nil && session.SessionID != request.SessionID {
				return nil, fmt.Errorf("resume returned %q, requested %q", session.SessionID, request.SessionID)
			}
			a.session.Store(session)
			return session, err
		},
		OnSessionReleased: func(session *copilot.Session) error { a.released <- session; return nil },
		OnExit:            func(event copilot.AhpHostExit) error { a.exited <- event; return nil },
	}
}

func assertAhpApplication(t *testing.T, ctx *testharness.TestContext, app *ahpApplication) {
	t.Helper()
	if app.tools.Load() != 1 || app.hooks.Load() == 0 {
		t.Fatalf("application callbacks missing: tools=%d hooks=%d", app.tools.Load(), app.hooks.Load())
	}
	exchanges, err := ctx.GetExchanges()
	if err != nil {
		t.Fatal(err)
	}
	advertised, prompted := false, false
	for _, exchange := range exchanges {
		for _, message := range exchange.Request.Messages {
			prompted = prompted || strings.Contains(message.Content, ahpPromptMarker)
		}
		for _, tool := range exchange.Request.Tools {
			advertised = advertised || tool.Function.Name == "magic_number"
		}
	}
	if !advertised || !prompted {
		t.Fatal("application prompt/tool declarations were not preserved")
	}
}

func awaitAhpRelease(t *testing.T, app *ahpApplication) {
	t.Helper()
	select {
	case session := <-app.released:
		if session != app.session.Load() {
			t.Fatal("release did not receive the original session object")
		}
	case <-time.After(10 * time.Second):
		t.Fatal("session was not released")
	}
}

func TestRuntimeHostE2E(t *testing.T) {
	if os.Getenv("COPILOT_RUNTIME_HOST_E2E") != "1" {
		t.Skip("Requires an integrated runtime; set COPILOT_RUNTIME_HOST_E2E=1")
	}
	for _, publish := range []bool{false, true} {
		name := "creates application session"
		if publish {
			name = "publishes exact resident session"
		}
		t.Run(name, func(t *testing.T) {
			ctx := testharness.NewTestContext(t)
			ahp := testharness.NewAhpTestClient(t)
			ctx.ConfigureSnapshot(t, "multi_client/both_clients_see_tool_request_and_completion_events")
			client := ctx.NewClient()
			t.Cleanup(func() {
				if err := client.Stop(); err != nil {
					t.Error(err)
				}
			})
			app := newAhpApplication(client, ctx.WorkDir)
			if publish {
				config := &copilot.SessionConfig{WorkingDirectory: ctx.WorkDir}
				app.createConfig(config)
				session, err := client.CreateSession(t.Context(), config)
				if err != nil {
					t.Fatal(err)
				}
				app.session.Store(session)
			}
			host, err := client.StartAhpHost(t.Context(), app.options())
			if err != nil {
				t.Fatal(err)
			}
			if host.PID != nil {
				t.Fatal("in-process listener returned a companion PID")
			}
			clientID, _ := connectAhpTest(t, ahp, host, "")
			var sessionID string
			if publish {
				sessionID = app.session.Load().SessionID
				result, err := host.PublishSession(t.Context(), sessionID)
				if err != nil || result.SessionID != sessionID || result.SessionURI != "ahp-session:/"+sessionID {
					t.Fatalf("incorrect publication: %v %v", result, err)
				}
				driveAhp(t, ahp, map[string]any{"op": "attach", "clientId": clientID, "sessionId": sessionID})
			} else {
				sessionID = driveAhp(t, ahp, map[string]any{"op": "create", "clientId": clientID, "workDir": ctx.WorkDir}).SessionID
				if app.creates.Load() != 1 || app.session.Load().SessionID != app.createID.Load() ||
					app.session.Load().SessionID == sessionID {
					t.Fatal("create callback did not materialize the requested identity")
				}
			}
			response := driveAhp(t, ahp, map[string]any{"op": "turn", "clientId": clientID, "sessionId": sessionID, "prompt": ahpToolPrompt})
			if !strings.Contains(response.Text, "MAGIC_hello_42") {
				t.Fatal(response.Text)
			}
			assertAhpApplication(t, ctx, app)
			for range 2 {
				if err := host.Dispose(t.Context()); err != nil {
					t.Fatal(err)
				}
			}
			driveAhp(t, ahp, map[string]any{"op": "stopped", "clientId": clientID, "url": host.URL})
			if publish {
				if app.creates.Load() != 0 || app.resumes.Load() != 0 || len(app.released) != 0 {
					t.Fatal("publication invoked a factory or transferred ownership")
				}
			} else {
				awaitAhpRelease(t, app)
			}
			select {
			case <-app.exited:
			case <-time.After(10 * time.Second):
				t.Fatal("listener exit was not reported")
			}
			events, err := app.session.Load().GetEvents(t.Context())
			if err != nil || len(events) == 0 {
				t.Fatalf("original session did not survive: %v", err)
			}
			pong, err := client.Ping(t.Context(), "still alive")
			if err != nil || pong.Message != "pong: still alive" {
				t.Fatalf("runtime did not survive: %v", err)
			}
		})
	}

	t.Run("resumes after runtime restart and composes tools", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		ahp := testharness.NewAhpTestClient(t)
		ctx.ConfigureSnapshot(t, "runtime_host/app_resume_callback_composes_tools_after_history")
		firstOwner := ctx.NewClient()
		t.Cleanup(firstOwner.ForceStop)
		first := newAhpApplication(firstOwner, ctx.WorkDir)
		host, err := firstOwner.StartAhpHost(t.Context(), first.options())
		if err != nil {
			t.Fatal(err)
		}
		clientID, closeFirst := connectAhpTest(t, ahp, host, "")
		sessionID := driveAhp(t, ahp, map[string]any{"op": "create", "clientId": clientID, "workDir": ctx.WorkDir, "clientTools": true}).SessionID
		runtimeID := first.session.Load().SessionID
		if first.creates.Load() != 1 || first.createID.Load() != runtimeID || runtimeID == sessionID {
			t.Fatal("create callback did not preserve its distinct runtime session identity")
		}
		answer := driveAhp(t, ahp, map[string]any{"op": "turn", "clientId": clientID, "sessionId": sessionID, "prompt": "What is 2+2?"})
		if !strings.Contains(answer.Text, "4") {
			t.Fatal(answer.Text)
		}
		if err := host.Dispose(t.Context()); err != nil {
			t.Fatal(err)
		}
		driveAhp(t, ahp, map[string]any{"op": "stopped", "clientId": clientID, "url": host.URL})
		awaitAhpRelease(t, first)
		closeFirst()
		if err := firstOwner.Stop(); err != nil {
			t.Fatal(err)
		}

		resumedOwner := ctx.NewClient()
		t.Cleanup(func() {
			if err := resumedOwner.Stop(); err != nil {
				t.Error(err)
			}
		})
		resumed := newAhpApplication(resumedOwner, ctx.WorkDir)
		replacement, err := resumedOwner.StartAhpHost(t.Context(), resumed.options())
		if err != nil {
			t.Fatal(err)
		}
		reconnected, _ := connectAhpTest(t, ahp, replacement, clientID)
		history := driveAhp(t, ahp, map[string]any{"op": "attach", "clientId": reconnected, "sessionId": sessionID, "clientTools": true}).History
		if len(history) != 1 || history[0].Message.Text != "What is 2+2?" {
			t.Fatalf("lost durable history: %+v", history)
		}
		if resumed.creates.Load() != 0 || resumed.resumes.Load() != 1 ||
			resumed.resumeID.Load() != runtimeID ||
			resumed.session.Load() == first.session.Load() || resumed.session.Load().SessionID != runtimeID {
			t.Fatal("incorrect application resume callback or identity")
		}
		response := driveAhp(t, ahp, map[string]any{"op": "turn", "clientId": reconnected, "sessionId": sessionID, "prompt": ahpComposedPrompt, "clientTools": true})
		if response.ClientToolCalls != 1 || !strings.Contains(response.Text, "CLIENT_ECHO_ping") || !strings.Contains(response.Text, "MAGIC_hello_42") {
			t.Fatalf("tools did not compose: %+v", response)
		}
		assertAhpApplication(t, ctx, resumed)
		if err := replacement.Dispose(t.Context()); err != nil {
			t.Fatal(err)
		}
		driveAhp(t, ahp, map[string]any{"op": "stopped", "clientId": reconnected, "url": replacement.URL})
		awaitAhpRelease(t, resumed)
		if _, err := resumed.session.Load().GetEvents(t.Context()); err != nil {
			t.Fatal(err)
		}
	})
}
