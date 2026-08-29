package copilot

import (
	"encoding/json"
	"errors"
	"fmt"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
)

func TestGitHubTokenProviderConfigValidation(t *testing.T) {
	provider := func(GitHubTokenProviderArgs) (*GitHubTokenProviderResult, error) {
		return GitHubTokenCancelled(), nil
	}

	if _, err := NewClient(nil).CreateSession(t.Context(), &SessionConfig{
		GitHubToken:         "static",
		GitHubTokenProvider: provider,
	}); err == nil || !strings.Contains(err.Error(), "cannot be used together") {
		t.Fatalf("CreateSession error = %v", err)
	}
	if _, err := NewClient(nil).ResumeSession(t.Context(), "session", &ResumeSessionConfig{
		GitHubToken:         "static",
		GitHubTokenProvider: provider,
	}); err == nil || !strings.Contains(err.Error(), "cannot be used together") {
		t.Fatalf("ResumeSession error = %v", err)
	}
}

func TestGitHubTokenProviderCreateRequestAndCallback(t *testing.T) {
	rpcClient, server, _ := newRuntimeShutdownRpcPair(t)
	t.Cleanup(server.Stop)
	client := &Client{
		client:   rpcClient,
		RPC:      rpc.NewServerRPC(rpcClient),
		sessions: make(map[string]*Session),
	}
	client.setupNotificationHandler()

	var createParams json.RawMessage
	server.SetRequestHandler("session.create", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		createParams = append(json.RawMessage(nil), params...)
		sessionID := sessionIDFromParams(t, params)
		return []byte(`{"sessionId":"` + sessionID + `","workspacePath":"/workspace"}`), nil
	})
	server.SetRequestHandler("session.destroy", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		return []byte(`{}`), nil
	})

	var gotArgs GitHubTokenProviderArgs
	session, err := client.CreateSession(t.Context(), &SessionConfig{
		GitHubTokenProvider: func(args GitHubTokenProviderArgs) (*GitHubTokenProviderResult, error) {
			gotArgs = args
			return GitHubTokenResult(&GitHubToken{
				AccessToken: "secret-token",
				TokenType:   String("bearer"),
				ExpiresIn:   8 * 60 * 60,
			}), nil
		},
	})
	if err != nil {
		t.Fatalf("CreateSession failed: %v", err)
	}

	var wire struct {
		RegistrationID string `json:"gitHubTokenProviderRegistrationId"`
		GitHubToken    string `json:"gitHubToken"`
	}
	if err := json.Unmarshal(createParams, &wire); err != nil {
		t.Fatal(err)
	}
	if wire.RegistrationID == "" {
		t.Fatal("gitHubTokenProviderRegistrationId was not serialized")
	}
	if wire.GitHubToken != "" {
		t.Fatal("static gitHubToken should not be serialized")
	}

	sessionID := session.SessionID
	raw, rpcErr := server.Request(t.Context(), "gitHubToken.getToken", &rpc.GitHubTokenAcquireRequest{
		RegistrationID: wire.RegistrationID,
		Host:           "github.example.com",
		SessionID:      &sessionID,
		Reason:         rpc.GitHubTokenAcquireReasonRefresh,
	})
	if rpcErr != nil {
		t.Fatalf("getToken failed: %v", rpcErr)
	}
	var tokenResult struct {
		Kind        string `json:"kind"`
		AccessToken string `json:"accessToken"`
		ExpiresIn   int64  `json:"expiresIn"`
	}
	if err := json.Unmarshal(raw, &tokenResult); err != nil {
		t.Fatal(err)
	}
	if tokenResult.Kind != "token" || tokenResult.AccessToken != "secret-token" || tokenResult.ExpiresIn != 8*60*60 {
		t.Fatalf("unexpected token result: %+v", tokenResult)
	}
	if gotArgs.Host != "github.example.com" || gotArgs.SessionID == nil ||
		*gotArgs.SessionID != sessionID || gotArgs.Reason != GitHubTokenRequestReasonRefresh {
		t.Fatalf("unexpected callback args: %+v", gotArgs)
	}

	if err := session.Disconnect(); err != nil {
		t.Fatal(err)
	}
	if len(client.gitHubTokenProviders) != 0 {
		t.Fatal("provider registration was not removed on disconnect")
	}
	if _, rpcErr := server.Request(t.Context(), "gitHubToken.getToken", &rpc.GitHubTokenAcquireRequest{
		RegistrationID: wire.RegistrationID,
		Host:           "github.com",
		Reason:         rpc.GitHubTokenAcquireReasonInitial,
	}); rpcErr == nil {
		t.Fatal("unknown registration ID should return a handler error")
	}

	var resumeParams json.RawMessage
	server.SetRequestHandler("session.resume", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		resumeParams = append(json.RawMessage(nil), params...)
		return []byte(`{"sessionId":"resumed-session","workspacePath":"/workspace"}`), nil
	})
	resumed, err := client.ResumeSession(t.Context(), "resumed-session", &ResumeSessionConfig{
		GitHubTokenProvider: func(GitHubTokenProviderArgs) (*GitHubTokenProviderResult, error) {
			return GitHubTokenCancelled(), nil
		},
	})
	if err != nil {
		t.Fatal(err)
	}
	if err := json.Unmarshal(resumeParams, &wire); err != nil {
		t.Fatal(err)
	}
	if wire.RegistrationID == "" {
		t.Fatal("resume did not serialize gitHubTokenProviderRegistrationId")
	}
	if err := resumed.Disconnect(); err != nil {
		t.Fatal(err)
	}
}

func TestGitHubTokenProviderResultsErrorsAndRollback(t *testing.T) {
	client := &Client{}
	adapter := &gitHubTokenAdapter{client: client}

	cancelID := client.registerGitHubTokenProvider(func(GitHubTokenProviderArgs) (*GitHubTokenProviderResult, error) {
		return GitHubTokenCancelled(), nil
	})
	result, err := adapter.GetToken(&rpc.GitHubTokenAcquireRequest{RegistrationID: cancelID})
	if err != nil {
		t.Fatal(err)
	}
	if _, ok := result.(*rpc.GitHubTokenAcquireResultCancelled); !ok {
		t.Fatalf("result type = %T", result)
	}

	sentinel := errors.New("provider failed")
	errorID := client.registerGitHubTokenProvider(func(GitHubTokenProviderArgs) (*GitHubTokenProviderResult, error) {
		return nil, sentinel
	})
	if _, err := adapter.GetToken(&rpc.GitHubTokenAcquireRequest{RegistrationID: errorID}); !errors.Is(err, sentinel) {
		t.Fatalf("provider error = %v", err)
	}

	rpcClient, server, _ := newRuntimeShutdownRpcPair(t)
	t.Cleanup(server.Stop)
	rollbackClient := &Client{
		client:   rpcClient,
		RPC:      rpc.NewServerRPC(rpcClient),
		sessions: make(map[string]*Session),
	}
	server.SetRequestHandler("session.create", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		return nil, &jsonrpc2.Error{Code: -32000, Message: "create failed"}
	})
	if _, err := rollbackClient.CreateSession(t.Context(), &SessionConfig{
		GitHubTokenProvider: func(GitHubTokenProviderArgs) (*GitHubTokenProviderResult, error) {
			return GitHubTokenCancelled(), nil
		},
	}); err == nil {
		t.Fatal("expected create failure")
	}
	if len(rollbackClient.gitHubTokenProviders) != 0 {
		t.Fatal("provider registration was not rolled back")
	}
}

func TestGitHubTokenStringRedactsAccessToken(t *testing.T) {
	token := GitHubToken{AccessToken: "secret-token", ExpiresIn: 28_800}

	if got := fmt.Sprintf("%v %#v", token, token); strings.Contains(got, token.AccessToken) {
		t.Fatalf("GitHubToken formatting exposed the access token: %s", got)
	}
}

func TestGitHubTokenProviderCleanupOnDisconnectError(t *testing.T) {
	rpcClient, server, _ := newRuntimeShutdownRpcPair(t)
	t.Cleanup(server.Stop)
	server.SetRequestHandler("session.destroy", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		return nil, &jsonrpc2.Error{Code: -32000, Message: "destroy failed"}
	})
	client := &Client{}
	registrationID := client.registerGitHubTokenProvider(func(GitHubTokenProviderArgs) (*GitHubTokenProviderResult, error) {
		return GitHubTokenCancelled(), nil
	})
	session := newSession("cleanup-session", rpcClient, "", false)
	session.setGitHubTokenProviderRegistrationRelease(func() {
		client.unregisterGitHubTokenProvider(registrationID)
	})

	if err := session.Disconnect(); err == nil || !strings.Contains(err.Error(), "destroy failed") {
		t.Fatalf("Disconnect error = %v", err)
	}
	if len(client.gitHubTokenProviders) != 0 {
		t.Fatal("provider registration was not removed after disconnect failed")
	}
}

func TestGitHubTokenProviderReleaseBeforeOwnershipTransfer(t *testing.T) {
	client := &Client{}
	registrationID := client.registerGitHubTokenProvider(func(GitHubTokenProviderArgs) (*GitHubTokenProviderResult, error) {
		return GitHubTokenCancelled(), nil
	})
	session := &Session{}

	session.releaseGitHubTokenProviderRegistration()
	session.setGitHubTokenProviderRegistrationRelease(func() {
		client.unregisterGitHubTokenProvider(registrationID)
	})

	if len(client.gitHubTokenProviders) != 0 {
		t.Fatal("provider registration was not removed after a pending session had already been retired")
	}
}

func TestGitHubTokenProviderCleanupOnDelete(t *testing.T) {
	rpcClient, server, _ := newRuntimeShutdownRpcPair(t)
	t.Cleanup(server.Stop)
	server.SetRequestHandler("session.delete", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		return []byte(`{"success":true}`), nil
	})
	client := &Client{
		client:   rpcClient,
		sessions: make(map[string]*Session),
		state:    stateConnected,
	}
	registrationID := client.registerGitHubTokenProvider(func(GitHubTokenProviderArgs) (*GitHubTokenProviderResult, error) {
		return GitHubTokenCancelled(), nil
	})
	session := newSession("delete-session", rpcClient, "", false)
	session.setGitHubTokenProviderRegistrationRelease(func() {
		client.unregisterGitHubTokenProvider(registrationID)
	})
	client.sessions[session.SessionID] = session

	if err := client.DeleteSession(t.Context(), session.SessionID); err != nil {
		t.Fatal(err)
	}
	if len(client.gitHubTokenProviders) != 0 {
		t.Fatal("provider registration was not removed after session deletion")
	}
}

func TestSessionOperationsSerializeBySessionID(t *testing.T) {
	client := &Client{}
	unlockFirst := client.lockSessionOperation("same-session")
	sameSessionAcquired := make(chan struct{})
	go func() {
		unlock := client.lockSessionOperation("same-session")
		close(sameSessionAcquired)
		unlock()
	}()

	select {
	case <-sameSessionAcquired:
		t.Fatal("same-session operation was not serialized")
	case <-time.After(25 * time.Millisecond):
	}

	otherSessionAcquired := make(chan struct{})
	go func() {
		unlock := client.lockSessionOperation("other-session")
		close(otherSessionAcquired)
		unlock()
	}()
	select {
	case <-otherSessionAcquired:
	case <-time.After(time.Second):
		t.Fatal("different-session operation was unnecessarily blocked")
	}

	unlockFirst()
	select {
	case <-sameSessionAcquired:
	case <-time.After(time.Second):
		t.Fatal("same-session operation did not proceed after release")
	}
}

func TestGitHubTokenProvidersClearedOnConnectionClose(t *testing.T) {
	client := &Client{state: stateConnected}
	client.registerGitHubTokenProvider(func(GitHubTokenProviderArgs) (*GitHubTokenProviderResult, error) {
		return GitHubTokenCancelled(), nil
	})

	client.handleConnectionClose()

	if len(client.gitHubTokenProviders) != 0 {
		t.Fatal("provider registrations were not cleared after connection closure")
	}
}

func TestGitHubTokenProviderConcurrentRegistrationsAreIsolated(t *testing.T) {
	client := &Client{}
	adapter := &gitHubTokenAdapter{client: client}
	idA := client.registerGitHubTokenProvider(func(GitHubTokenProviderArgs) (*GitHubTokenProviderResult, error) {
		return GitHubTokenResult(&GitHubToken{AccessToken: "a", ExpiresIn: 1}), nil
	})
	idB := client.registerGitHubTokenProvider(func(GitHubTokenProviderArgs) (*GitHubTokenProviderResult, error) {
		return GitHubTokenResult(&GitHubToken{AccessToken: "b", ExpiresIn: 1}), nil
	})

	var wg sync.WaitGroup
	for id, want := range map[string]string{idA: "a", idB: "b"} {
		wg.Add(1)
		go func() {
			defer wg.Done()
			result, err := adapter.GetToken(&rpc.GitHubTokenAcquireRequest{RegistrationID: id})
			if err != nil {
				t.Error(err)
				return
			}
			if got := result.(*rpc.GitHubTokenAcquireResultToken).AccessToken; got != want {
				t.Errorf("token = %q, want %q", got, want)
			}
		}()
	}
	wg.Wait()
}
