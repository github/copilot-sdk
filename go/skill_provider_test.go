package copilot

import (
	"context"
	"encoding/json"
	"errors"
	"log"
	"strings"
	"sync"
	"testing"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
)

type testSkillProvider struct {
	mu       sync.Mutex
	skills   []rpc.SkillProviderDescriptor
	markdown map[string]string
	listErr  error
	readErr  error
	calls    []string
}

func (p *testSkillProvider) ListSkills(context.Context) ([]rpc.SkillProviderDescriptor, error) {
	p.mu.Lock()
	defer p.mu.Unlock()
	p.calls = append(p.calls, "list")
	if p.listErr != nil {
		return nil, p.listErr
	}
	return p.skills, nil
}

func (p *testSkillProvider) ReadSkill(_ context.Context, name string) (string, error) {
	p.mu.Lock()
	defer p.mu.Unlock()
	p.calls = append(p.calls, "read:"+name)
	if p.readErr != nil {
		return "", p.readErr
	}
	markdown, ok := p.markdown[name]
	if !ok {
		return "", ErrSkillNotFound
	}
	return markdown, nil
}

func (p *testSkillProvider) snapshotCalls() []string {
	p.mu.Lock()
	defer p.mu.Unlock()
	return append([]string(nil), p.calls...)
}

var reviewSkillDescriptor = rpc.SkillProviderDescriptor{
	Name:        "review",
	Description: "Reviews code",
}

func newTestSkillProvider() *testSkillProvider {
	return &testSkillProvider{
		skills:   []rpc.SkillProviderDescriptor{reviewSkillDescriptor},
		markdown: map[string]string{"review": "Review carefully."},
	}
}

func TestSkillProviderSessionPayloads(t *testing.T) {
	t.Run("omits flag without provider and sends flag with provider", func(t *testing.T) {
		rpcClient, server, _ := newRuntimeShutdownRpcPair(t)
		t.Cleanup(server.Stop)
		client := &Client{
			client:   rpcClient,
			RPC:      rpc.NewServerRPC(rpcClient),
			sessions: make(map[string]*Session),
		}

		createParams := make(chan json.RawMessage, 2)
		resumeParams := make(chan json.RawMessage, 2)
		server.SetRequestHandler("session.create", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			createParams <- append(json.RawMessage(nil), params...)
			sessionID := sessionIDFromParams(t, params)
			return []byte(`{"sessionId":"` + sessionID + `","workspacePath":"/workspace"}`), nil
		})
		server.SetRequestHandler("session.resume", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			resumeParams <- append(json.RawMessage(nil), params...)
			sessionID := sessionIDFromParams(t, params)
			return []byte(`{"sessionId":"` + sessionID + `","workspacePath":"/workspace"}`), nil
		})

		if _, err := client.CreateSession(t.Context(), &SessionConfig{}); err != nil {
			t.Fatalf("CreateSession without provider failed: %v", err)
		}
		if _, err := client.ResumeSession(t.Context(), "resume-without-provider", &ResumeSessionConfig{}); err != nil {
			t.Fatalf("ResumeSession without provider failed: %v", err)
		}
		provider := newTestSkillProvider()
		if _, err := client.CreateSession(t.Context(), &SessionConfig{SkillProvider: provider}); err != nil {
			t.Fatalf("CreateSession with provider failed: %v", err)
		}
		if _, err := client.ResumeSession(t.Context(), "resume-with-provider", &ResumeSessionConfig{SkillProvider: provider}); err != nil {
			t.Fatalf("ResumeSession with provider failed: %v", err)
		}

		assertSkillProviderFlag(t, <-createParams, false)
		assertSkillProviderFlag(t, <-resumeParams, false)
		assertSkillProviderFlag(t, <-createParams, true)
		assertSkillProviderFlag(t, <-resumeParams, true)
		if got := provider.snapshotCalls(); len(got) != 0 {
			t.Fatalf("provider was called during open: %v", got)
		}
	})

	t.Run("keeps empty mode enableSkills default with provider", func(t *testing.T) {
		rpcClient, server, _ := newRuntimeShutdownRpcPair(t)
		t.Cleanup(server.Stop)
		client := &Client{
			client:   rpcClient,
			RPC:      rpc.NewServerRPC(rpcClient),
			sessions: make(map[string]*Session),
			options:  ClientOptions{Mode: ModeEmpty},
		}

		createParams := make(chan json.RawMessage, 1)
		server.SetRequestHandler("session.create", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			createParams <- append(json.RawMessage(nil), params...)
			sessionID := sessionIDFromParams(t, params)
			return []byte(`{"sessionId":"` + sessionID + `"}`), nil
		})
		server.SetRequestHandler("session.options.update", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			return []byte(`{}`), nil
		})

		if _, err := client.CreateSession(t.Context(), &SessionConfig{
			AvailableTools: []string{},
			SkillProvider:  newTestSkillProvider(),
		}); err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		var payload map[string]any
		if err := json.Unmarshal(<-createParams, &payload); err != nil {
			t.Fatal(err)
		}
		if payload["enableSkills"] != false {
			t.Fatalf("enableSkills = %v, want false", payload["enableSkills"])
		}
		if payload["hasSkillProvider"] != true {
			t.Fatalf("hasSkillProvider = %v, want true", payload["hasSkillProvider"])
		}
	})
}

func TestSkillProviderServesEarlyCallbacksDuringOpen(t *testing.T) {
	for _, method := range []string{"session.create", "session.resume"} {
		t.Run(method, func(t *testing.T) {
			rpcClient, server, _ := newRuntimeShutdownRpcPair(t)
			t.Cleanup(server.Stop)
			client := &Client{
				client:   rpcClient,
				RPC:      rpc.NewServerRPC(rpcClient),
				sessions: make(map[string]*Session),
			}
			client.setupNotificationHandler()

			earlyResult := make(chan rpc.SkillProviderListResult, 1)
			server.SetRequestHandler(method, func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				sessionID := sessionIDFromParams(t, params)
				raw, err := server.Request(t.Context(), "skillProvider.list", map[string]any{"sessionId": sessionID})
				if err != nil {
					if rpcErr, ok := err.(*jsonrpc2.Error); ok {
						return nil, rpcErr
					}
					return nil, &jsonrpc2.Error{Code: -32603, Message: err.Error()}
				}
				var result rpc.SkillProviderListResult
				if err := json.Unmarshal(raw, &result); err != nil {
					return nil, &jsonrpc2.Error{Code: -32603, Message: err.Error()}
				}
				earlyResult <- result
				return []byte(`{"sessionId":"` + sessionID + `"}`), nil
			})

			provider := newTestSkillProvider()
			if method == "session.create" {
				if _, err := client.CreateSession(t.Context(), &SessionConfig{SkillProvider: provider}); err != nil {
					t.Fatalf("CreateSession failed: %v", err)
				}
			} else if _, err := client.ResumeSession(t.Context(), "early-resume", &ResumeSessionConfig{SkillProvider: provider}); err != nil {
				t.Fatalf("ResumeSession failed: %v", err)
			}

			got := <-earlyResult
			if len(got.Skills) != 1 || got.Skills[0].Name != "review" {
				t.Fatalf("early list result = %+v", got)
			}
		})
	}
}

func TestSkillProviderRejectsCloudBeforeConnect(t *testing.T) {
	provider := newTestSkillProvider()
	client := &Client{}

	_, err := client.CreateSession(t.Context(), &SessionConfig{
		Cloud:         &CloudSessionOptions{},
		SkillProvider: provider,
	})

	if err == nil || err.Error() != "Skill providers are not supported for cloud sessions." {
		t.Fatalf("CreateSession error = %v", err)
	}
	if client.client != nil {
		t.Fatal("client connected before rejecting cloud skill provider")
	}
	if got := provider.snapshotCalls(); len(got) != 0 {
		t.Fatalf("provider was called: %v", got)
	}
}

func TestSkillProviderDispatch(t *testing.T) {
	rpcClient, server, _ := newRuntimeShutdownRpcPair(t)
	t.Cleanup(server.Stop)
	client := &Client{
		client:   rpcClient,
		RPC:      rpc.NewServerRPC(rpcClient),
		sessions: make(map[string]*Session),
	}
	client.setupNotificationHandler()

	provider := &testSkillProvider{
		skills: []rpc.SkillProviderDescriptor{
			reviewSkillDescriptor,
			{
				Name:                   "deploy",
				Description:            "Deploys the service",
				UserInvocable:          Bool(false),
				DisableModelInvocation: Bool(true),
				ArgumentHint:           String("[environment]"),
			},
		},
		markdown: map[string]string{"deploy": "# deploy"},
	}
	session := newSession("dispatch-session", rpcClient, "", false)
	session.registerSkillProvider(provider)
	client.sessions[session.SessionID] = session

	listRaw, rpcErr := server.Request(t.Context(), "skillProvider.list", map[string]any{"sessionId": session.SessionID})
	if rpcErr != nil {
		t.Fatalf("skillProvider.list failed: %v", rpcErr)
	}
	var listResult rpc.SkillProviderListResult
	if err := json.Unmarshal(listRaw, &listResult); err != nil {
		t.Fatal(err)
	}
	if len(listResult.Skills) != 2 || listResult.Skills[1].Name != "deploy" {
		t.Fatalf("list result = %+v", listResult)
	}

	readRaw, rpcErr := server.Request(t.Context(), "skillProvider.read", map[string]any{
		"sessionId": session.SessionID,
		"name":      "deploy",
	})
	if rpcErr != nil {
		t.Fatalf("skillProvider.read failed: %v", rpcErr)
	}
	var readResult rpc.SkillProviderReadResult
	if err := json.Unmarshal(readRaw, &readResult); err != nil {
		t.Fatal(err)
	}
	if readResult.Markdown == nil {
		t.Fatal("markdown = nil, want # deploy")
	}
	if *readResult.Markdown != "# deploy" {
		t.Fatalf("markdown = %q, want # deploy", *readResult.Markdown)
	}
	if got := provider.snapshotCalls(); strings.Join(got, ",") != "list,read:deploy" {
		t.Fatalf("provider calls = %v", got)
	}
}

func TestSkillProviderOmitsUnsetOptionalDescriptorFields(t *testing.T) {
	data, err := json.Marshal(rpc.SkillProviderListResult{
		Skills: []rpc.SkillProviderDescriptor{{Name: "review", Description: "Reviews code"}},
	})
	if err != nil {
		t.Fatal(err)
	}
	var decoded map[string][]map[string]any
	if err := json.Unmarshal(data, &decoded); err != nil {
		t.Fatal(err)
	}
	skill := decoded["skills"][0]
	for _, key := range []string{"argumentHint", "userInvocable", "disableModelInvocation"} {
		if _, ok := skill[key]; ok {
			t.Fatalf("%s should be omitted when unset: %s", key, data)
		}
	}
}

func TestSkillProviderEmptyListBecomesEmptyArray(t *testing.T) {
	rpcClient, server, _ := newRuntimeShutdownRpcPair(t)
	t.Cleanup(server.Stop)
	client := &Client{
		client:   rpcClient,
		RPC:      rpc.NewServerRPC(rpcClient),
		sessions: make(map[string]*Session),
	}
	client.setupNotificationHandler()
	session := newSession("empty-list-session", rpcClient, "", false)
	session.registerSkillProvider(&testSkillProvider{})
	client.sessions[session.SessionID] = session

	raw, rpcErr := server.Request(t.Context(), "skillProvider.list", map[string]any{"sessionId": session.SessionID})
	if rpcErr != nil {
		t.Fatalf("skillProvider.list failed: %v", rpcErr)
	}
	if string(raw) != `{"skills":[]}` {
		t.Fatalf("list response = %s, want empty skills array", raw)
	}
}

func TestSkillProviderErrorEnvelopes(t *testing.T) {
	t.Run("read not found returns null markdown", func(t *testing.T) {
		_, server, sessionID := skillProviderErrorHarness(t, newTestSkillProvider())
		raw, rpcErr := server.Request(t.Context(), "skillProvider.read", map[string]any{
			"sessionId": sessionID,
			"name":      "missing",
		})
		if rpcErr != nil {
			t.Fatalf("skillProvider.read failed: %v", rpcErr)
		}
		if string(raw) != `{"markdown":null}` {
			t.Fatalf("read response = %s, want null markdown", raw)
		}
		var result rpc.SkillProviderReadResult
		if err := json.Unmarshal(raw, &result); err != nil {
			t.Fatal(err)
		}
		if result.Markdown != nil {
			t.Fatalf("markdown = %q, want nil", *result.Markdown)
		}
	})

	for _, method := range []string{"skillProvider.list", "skillProvider.read"} {
		t.Run(method+" provider failure", func(t *testing.T) {
			output := captureSkillProviderLog(t)
			secret := errors.New("db-password-in-error")
			provider := newTestSkillProvider()
			operation := "listSkills"
			if method == "skillProvider.list" {
				provider.listErr = secret
			} else {
				provider.readErr = secret
				operation = "readSkill"
			}
			_, server, sessionID := skillProviderErrorHarness(t, provider)
			params := map[string]any{"sessionId": sessionID}
			wantMessage := "Skill provider " + operation + " failed"
			if method == "skillProvider.read" {
				params["name"] = "review"
			}

			rpcErr := requestSkillProviderError(t, server, method, params)
			if rpcErr.Code != -32603 || rpcErr.Message != wantMessage {
				t.Fatalf("error = %+v, want generic message %q", rpcErr, wantMessage)
			}
			if rpcErr.Data != nil {
				t.Fatalf("generic provider error data = %s, want omitted", rpcErr.Data)
			}
			if strings.Contains(rpcErr.Message, secret.Error()) {
				t.Fatalf("provider error leaked secret: %q", rpcErr.Message)
			}
			wantLog := "skill provider " + operation + " failed: session_id=" + sessionID + " error=" + secret.Error()
			if got := output.text(); !strings.Contains(got, wantLog) {
				t.Fatalf("log = %q, want it to contain %q", got, wantLog)
			}
		})
	}

	t.Run("not-found classification panic", func(t *testing.T) {
		captureSkillProviderLog(t)
		provider := newTestSkillProvider()
		provider.readErr = panickyIsError{}
		_, server, sessionID := skillProviderErrorHarness(t, provider)

		rpcErr := requestSkillProviderError(t, server, "skillProvider.read", map[string]any{
			"sessionId": sessionID,
			"name":      "review",
		})
		if rpcErr.Code != -32603 || rpcErr.Message != "Skill provider readSkill failed" || rpcErr.Data != nil {
			t.Fatalf("error = %+v, want generic readSkill failure", rpcErr)
		}
	})

	t.Run("unknown session", func(t *testing.T) {
		_, server, _ := skillProviderErrorHarness(t, newTestSkillProvider())
		rpcErr := requestSkillProviderError(t, server, "skillProvider.list", map[string]any{"sessionId": "missing"})
		assertSkillProviderError(t, rpcErr, "No skill provider for session: missing")
	})

	for _, method := range []string{"skillProvider.list", "skillProvider.read"} {
		t.Run(method+" provider panic", func(t *testing.T) {
			output := captureSkillProviderLog(t)
			_, server, sessionID := skillProviderErrorHarness(t, panickingSkillProvider{})
			params := map[string]any{"sessionId": sessionID}
			wantMessage := "Skill provider listSkills failed"
			if method == "skillProvider.read" {
				params["name"] = "review"
				wantMessage = "Skill provider readSkill failed"
			}

			rpcErr := requestSkillProviderError(t, server, method, params)
			if rpcErr.Code != -32603 || rpcErr.Message != wantMessage || rpcErr.Data != nil {
				t.Fatalf("error = %+v, want generic message %q without data", rpcErr, wantMessage)
			}
			if got := output.text(); !strings.Contains(got, "panic=db-password-in-panic") {
				t.Fatalf("log = %q, want the recovered panic value", got)
			}
		})
	}

	t.Run("no provider", func(t *testing.T) {
		rpcClient, server, _ := newRuntimeShutdownRpcPair(t)
		t.Cleanup(server.Stop)
		client := &Client{
			client:   rpcClient,
			RPC:      rpc.NewServerRPC(rpcClient),
			sessions: make(map[string]*Session),
		}
		client.setupNotificationHandler()
		session := newSession("no-provider-session", rpcClient, "", false)
		client.sessions[session.SessionID] = session

		rpcErr := requestSkillProviderError(t, server, "skillProvider.read", map[string]any{
			"sessionId": session.SessionID,
			"name":      "review",
		})
		assertSkillProviderError(t, rpcErr, "No skill provider for session: no-provider-session")
	})
}

func TestSkillProviderTeardown(t *testing.T) {
	t.Run("disconnect clears provider", func(t *testing.T) {
		client, server, provider, session := newSkillProviderOpenSession(t, "disconnect-session")

		if err := session.Disconnect(); err != nil {
			t.Fatalf("Disconnect failed: %v", err)
		}
		rpcErr := requestSkillProviderError(t, server, "skillProvider.list", map[string]any{"sessionId": session.SessionID})
		assertSkillProviderError(t, rpcErr, "No skill provider for session: disconnect-session")
		if got := provider.snapshotCalls(); len(got) != 0 {
			t.Fatalf("provider was called after disconnect: %v", got)
		}
		_ = client
	})

	t.Run("delete clears provider", func(t *testing.T) {
		client, server, provider, session := newSkillProviderOpenSession(t, "delete-session")
		server.SetRequestHandler("session.delete", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			return []byte(`{"success":true}`), nil
		})

		if err := client.DeleteSession(t.Context(), session.SessionID); err != nil {
			t.Fatalf("DeleteSession failed: %v", err)
		}
		rpcErr := requestSkillProviderError(t, server, "skillProvider.list", map[string]any{"sessionId": session.SessionID})
		assertSkillProviderError(t, rpcErr, "No skill provider for session: delete-session")
		if got := provider.snapshotCalls(); len(got) != 0 {
			t.Fatalf("provider was called after delete: %v", got)
		}
	})

	t.Run("connection loss clears provider", func(t *testing.T) {
		client, server, provider, session := newSkillProviderOpenSession(t, "closed-session")

		client.handleConnectionClose()
		rpcErr := requestSkillProviderError(t, server, "skillProvider.list", map[string]any{"sessionId": session.SessionID})
		assertSkillProviderError(t, rpcErr, "No skill provider for session: closed-session")
		if got := provider.snapshotCalls(); len(got) != 0 {
			t.Fatalf("provider was called after connection loss: %v", got)
		}
	})

	t.Run("failed create cleanup", func(t *testing.T) {
		provider := newTestSkillProvider()
		var sessionID string
		client, server := newSkillProviderClient(t)
		server.SetRequestHandler("session.create", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			sessionID = sessionIDFromParams(t, params)
			return nil, &jsonrpc2.Error{Code: -32000, Message: "create failed"}
		})

		if _, err := client.CreateSession(t.Context(), &SessionConfig{SkillProvider: provider}); err == nil {
			t.Fatal("CreateSession succeeded unexpectedly")
		}
		rpcErr := requestSkillProviderError(t, server, "skillProvider.list", map[string]any{"sessionId": sessionID})
		assertSkillProviderError(t, rpcErr, "No skill provider for session: "+sessionID)
		if got := provider.snapshotCalls(); len(got) != 0 {
			t.Fatalf("provider was called after failed create: %v", got)
		}
	})

	t.Run("failed resume cleanup", func(t *testing.T) {
		provider := newTestSkillProvider()
		client, server := newSkillProviderClient(t)
		server.SetRequestHandler("session.resume", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			return nil, &jsonrpc2.Error{Code: -32000, Message: "resume failed"}
		})

		if _, err := client.ResumeSession(t.Context(), "failed-resume", &ResumeSessionConfig{SkillProvider: provider}); err == nil {
			t.Fatal("ResumeSession succeeded unexpectedly")
		}
		rpcErr := requestSkillProviderError(t, server, "skillProvider.list", map[string]any{"sessionId": "failed-resume"})
		assertSkillProviderError(t, rpcErr, "No skill provider for session: failed-resume")
		if got := provider.snapshotCalls(); len(got) != 0 {
			t.Fatalf("provider was called after failed resume: %v", got)
		}
	})
}

func TestSkillProviderResumeRebinding(t *testing.T) {
	t.Run("serves replacement provider", func(t *testing.T) {
		client, server := newSkillProviderClient(t)
		server.SetRequestHandler("session.create", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			sessionID := sessionIDFromParams(t, params)
			return []byte(`{"sessionId":"` + sessionID + `"}`), nil
		})
		server.SetRequestHandler("session.resume", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			sessionID := sessionIDFromParams(t, params)
			return []byte(`{"sessionId":"` + sessionID + `"}`), nil
		})
		original := newTestSkillProvider()
		replacement := newTestSkillProvider()
		replacement.markdown = map[string]string{"review": "replacement"}

		session, err := client.CreateSession(t.Context(), &SessionConfig{
			SessionID:     "rebind-session",
			SkillProvider: original,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		if _, err := client.ResumeSession(t.Context(), session.SessionID, &ResumeSessionConfig{SkillProvider: replacement}); err != nil {
			t.Fatalf("ResumeSession failed: %v", err)
		}
		raw, rpcErr := server.Request(t.Context(), "skillProvider.read", map[string]any{
			"sessionId": session.SessionID,
			"name":      "review",
		})
		if rpcErr != nil {
			t.Fatalf("skillProvider.read failed: %v", rpcErr)
		}
		var result rpc.SkillProviderReadResult
		if err := json.Unmarshal(raw, &result); err != nil {
			t.Fatal(err)
		}
		if result.Markdown == nil {
			t.Fatal("markdown = nil, want replacement")
		}
		if *result.Markdown != "replacement" {
			t.Fatalf("markdown = %q, want replacement", *result.Markdown)
		}
		if got := original.snapshotCalls(); len(got) != 0 {
			t.Fatalf("original provider was called after resume: %v", got)
		}
		if got := replacement.snapshotCalls(); strings.Join(got, ",") != "read:review" {
			t.Fatalf("replacement provider calls = %v", got)
		}
	})

	t.Run("resume without provider unbinds previous provider", func(t *testing.T) {
		client, server, original, session := newSkillProviderOpenSession(t, "unbind-session")
		server.SetRequestHandler("session.resume", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			sessionID := sessionIDFromParams(t, params)
			return []byte(`{"sessionId":"` + sessionID + `"}`), nil
		})

		if _, err := client.ResumeSession(t.Context(), session.SessionID, &ResumeSessionConfig{}); err != nil {
			t.Fatalf("ResumeSession failed: %v", err)
		}
		rpcErr := requestSkillProviderError(t, server, "skillProvider.list", map[string]any{"sessionId": session.SessionID})
		assertSkillProviderError(t, rpcErr, "No skill provider for session: unbind-session")
		if got := original.snapshotCalls(); len(got) != 0 {
			t.Fatalf("original provider was called after unbound resume: %v", got)
		}
	})
}

type panickingSkillProvider struct{}

func (panickingSkillProvider) ListSkills(context.Context) ([]rpc.SkillProviderDescriptor, error) {
	panic("db-password-in-panic")
}

func (panickingSkillProvider) ReadSkill(context.Context, string) (string, error) {
	panic("db-password-in-panic")
}

// panickyIsError panics when errors.Is asks whether it matches a target.
type panickyIsError struct{}

func (panickyIsError) Error() string { return "panicky" }

func (panickyIsError) Is(error) bool { panic("classification panicked") }

func captureSkillProviderLog(t *testing.T) *ahpTestLog {
	t.Helper()
	output := new(ahpTestLog)
	previous := log.Writer()
	log.SetOutput(output)
	t.Cleanup(func() { log.SetOutput(previous) })
	return output
}

func newSkillProviderClient(t *testing.T) (*Client, *jsonrpc2.Client) {
	t.Helper()
	rpcClient, server, _ := newRuntimeShutdownRpcPair(t)
	t.Cleanup(server.Stop)
	client := &Client{
		client:   rpcClient,
		RPC:      rpc.NewServerRPC(rpcClient),
		sessions: make(map[string]*Session),
	}
	client.setupNotificationHandler()
	return client, server
}

func newSkillProviderOpenSession(t *testing.T, sessionID string) (*Client, *jsonrpc2.Client, *testSkillProvider, *Session) {
	t.Helper()
	client, server := newSkillProviderClient(t)
	provider := newTestSkillProvider()
	server.SetRequestHandler("session.create", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		gotSessionID := sessionIDFromParams(t, params)
		return []byte(`{"sessionId":"` + gotSessionID + `"}`), nil
	})
	server.SetRequestHandler("session.detach", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		return []byte(`{"success":true}`), nil
	})

	session, err := client.CreateSession(t.Context(), &SessionConfig{
		SessionID:     sessionID,
		SkillProvider: provider,
	})
	if err != nil {
		t.Fatalf("CreateSession failed: %v", err)
	}
	return client, server, provider, session
}

func skillProviderErrorHarness(t *testing.T, provider SkillProvider) (*Client, *jsonrpc2.Client, string) {
	t.Helper()
	client, server := newSkillProviderClient(t)
	session := newSession("error-session", client.client, "", false)
	session.registerSkillProvider(provider)
	client.sessions[session.SessionID] = session
	return client, server, session.SessionID
}

func requestSkillProviderError(t *testing.T, server *jsonrpc2.Client, method string, params map[string]any) *jsonrpc2.Error {
	t.Helper()
	_, err := server.Request(t.Context(), method, params)
	if err == nil {
		t.Fatalf("%s succeeded unexpectedly", method)
	}
	rpcErr, ok := err.(*jsonrpc2.Error)
	if !ok {
		t.Fatalf("%s error = %T %v, want *jsonrpc2.Error", method, err, err)
	}
	return rpcErr
}

func assertSkillProviderError(t *testing.T, rpcErr *jsonrpc2.Error, wantMessage string) {
	t.Helper()
	if rpcErr.Code != -32603 || rpcErr.Message != wantMessage {
		t.Fatalf("error = %+v, want code -32603 message %q", rpcErr, wantMessage)
	}
	if rpcErr.Data != nil {
		t.Fatalf("error data = %s, want omitted", rpcErr.Data)
	}
}

func assertSkillProviderFlag(t *testing.T, params json.RawMessage, wantPresent bool) {
	t.Helper()
	var payload map[string]any
	if err := json.Unmarshal(params, &payload); err != nil {
		t.Fatalf("failed to decode request params: %v", err)
	}
	got, present := payload["hasSkillProvider"]
	if !wantPresent {
		if present {
			t.Fatalf("hasSkillProvider = %v, want omitted", got)
		}
		return
	}
	if got != true {
		t.Fatalf("hasSkillProvider = %v, want true", got)
	}
	if _, present := payload["skillProvider"]; present {
		t.Fatalf("skillProvider callback was serialized: %v", payload["skillProvider"])
	}
}
