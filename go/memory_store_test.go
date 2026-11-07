package copilot

import (
	"context"
	"encoding/json"
	"errors"
	"strings"
	"testing"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
)

func TestMemoryStoreRegisteredBeforeOpen(t *testing.T) {
	for _, method := range []string{"session.create", "session.resume"} {
		t.Run(method, func(t *testing.T) {
			client, server := newSkillProviderClient(t)
			openParams := make(chan json.RawMessage, 1)
			server.SetRequestHandler(method, func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				openParams <- append(json.RawMessage(nil), params...)
				sessionID := sessionIDFromParams(t, params)
				raw, err := server.Request(t.Context(), "memoryStore.store", map[string]any{
					"sessionId": sessionID,
					"subject":   "package manager",
					"fact":      "Use pnpm.",
					"citations": "package.json",
					"reason":    "The workspace uses pnpm.",
				})
				if err != nil {
					return nil, &jsonrpc2.Error{Code: -32603, Message: "early memoryStore.store failed: " + err.Error()}
				}
				if string(raw) != "null" {
					return nil, &jsonrpc2.Error{Code: -32603, Message: "early memoryStore.store returned " + string(raw)}
				}
				return []byte(`{"sessionId":"` + sessionID + `"}`), nil
			})
			server.SetRequestHandler("session.memory.configureStore", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				t.Error("unexpected session.memory.configureStore request")
				return []byte(`null`), nil
			})

			var stored []StoredMemory
			memory := &MemoryConfiguration{Enabled: true}
			storeMemory := func(_ context.Context, memory StoredMemory) error {
				stored = append(stored, memory)
				return nil
			}
			var session *Session
			var err error
			if method == "session.create" {
				session, err = client.CreateSession(t.Context(), &SessionConfig{Memory: memory, StoreMemory: storeMemory})
			} else {
				session, err = client.ResumeSession(t.Context(), "memory-resume", &ResumeSessionConfig{Memory: memory, StoreMemory: storeMemory})
			}
			if err != nil {
				t.Fatalf("%s failed: %v", method, err)
			}

			var payload map[string]any
			if err := json.Unmarshal(<-openParams, &payload); err != nil {
				t.Fatal(err)
			}
			if payload["hasMemoryStore"] != true {
				t.Fatalf("hasMemoryStore = %v, want true", payload["hasMemoryStore"])
			}
			wire, _ := json.Marshal(payload["memory"])
			if string(wire) != `{"enabled":true}` {
				t.Fatalf("memory on the wire = %s, want only enabled", wire)
			}
			if len(stored) != 1 || stored[0].SessionID != session.SessionID || stored[0].Fact != "Use pnpm." {
				t.Fatalf("stored = %+v", stored)
			}
		})
	}
}

func TestMemoryStoreRequiresEnabledBeforeConnect(t *testing.T) {
	storeMemory := func(context.Context, StoredMemory) error { return nil }
	const want = "StoreMemory requires Memory.Enabled to be true."

	client := &Client{}
	if _, err := client.CreateSession(t.Context(), &SessionConfig{StoreMemory: storeMemory}); err == nil || err.Error() != want {
		t.Fatalf("CreateSession error = %v, want %q", err, want)
	}
	if _, err := client.ResumeSession(t.Context(), "memory-resume", &ResumeSessionConfig{Memory: &MemoryConfiguration{}, StoreMemory: storeMemory}); err == nil || err.Error() != want {
		t.Fatalf("ResumeSession error = %v, want %q", err, want)
	}
	if client.client != nil {
		t.Fatal("client connected before rejecting disabled memory store")
	}
}

func TestMemoryStoreRejectsCloudBeforeConnect(t *testing.T) {
	client := &Client{}
	_, err := client.CreateSession(t.Context(), &SessionConfig{
		Cloud:       &CloudSessionOptions{},
		Memory:      &MemoryConfiguration{Enabled: true},
		StoreMemory: func(context.Context, StoredMemory) error { return nil },
	})
	if err == nil || err.Error() != "Memory stores are not supported for cloud sessions." {
		t.Fatalf("CreateSession error = %v", err)
	}
	if client.client != nil {
		t.Fatal("client connected before rejecting cloud memory store")
	}
}

func TestMemoryStoreDispatch(t *testing.T) {
	stored := StoredMemory{
		Subject:   "package manager",
		Fact:      "Use pnpm.",
		Citations: "package.json",
		Reason:    "The workspace uses pnpm.",
	}
	params := func(sessionID string) map[string]any {
		return map[string]any{
			"sessionId": sessionID,
			"subject":   stored.Subject,
			"fact":      stored.Fact,
			"citations": stored.Citations,
			"reason":    stored.Reason,
		}
	}

	t.Run("invokes callback with payload", func(t *testing.T) {
		client, server := newSkillProviderClient(t)
		session := newSession("memory-session", client.client, "", false)
		var got []StoredMemory
		session.memoryStore = func(_ context.Context, memory StoredMemory) error {
			got = append(got, memory)
			return nil
		}
		client.sessions[session.SessionID] = session

		raw, err := server.Request(t.Context(), "memoryStore.store", params(session.SessionID))
		if err != nil {
			t.Fatalf("memoryStore.store failed: %v", err)
		}
		if string(raw) != "null" {
			t.Fatalf("response = %s, want null", raw)
		}
		want := stored
		want.SessionID = session.SessionID
		if len(got) != 1 || got[0] != want {
			t.Fatalf("stored = %+v, want %+v", got, want)
		}
	})

	t.Run("callback error is generic", func(t *testing.T) {
		captureSkillProviderLog(t)
		client, server := newSkillProviderClient(t)
		session := newSession("failing-memory-session", client.client, "", false)
		session.memoryStore = func(context.Context, StoredMemory) error {
			return errors.New("secret backend detail")
		}
		client.sessions[session.SessionID] = session

		rpcErr := requestSkillProviderError(t, server, "memoryStore.store", params(session.SessionID))
		assertSkillProviderError(t, rpcErr, "Memory store callback failed")
		if strings.Contains(rpcErr.Message, "secret backend detail") {
			t.Fatalf("callback error leaked: %q", rpcErr.Message)
		}
	})

	t.Run("no handler", func(t *testing.T) {
		client, server := newSkillProviderClient(t)
		session := newSession("no-memory-session", client.client, "", false)
		client.sessions[session.SessionID] = session

		rpcErr := requestSkillProviderError(t, server, "memoryStore.store", params(session.SessionID))
		assertSkillProviderError(t, rpcErr, "No memory store is registered")
	})

	t.Run("skips callback cancelled before dispatch", func(t *testing.T) {
		client, _ := newSkillProviderClient(t)
		session := newSession("cancelled-memory-session", client.client, "", false)
		called := false
		session.memoryStore = func(context.Context, StoredMemory) error {
			called = true
			return nil
		}
		client.sessions[session.SessionID] = session

		raw, err := json.Marshal(params(session.SessionID))
		if err != nil {
			t.Fatal(err)
		}
		ctx, cancel := context.WithCancel(t.Context())
		cancel()
		_, rpcErr := client.handleMemoryStore(ctx, raw)
		if rpcErr == nil || rpcErr.Code != -32800 {
			t.Fatalf("error = %v, want -32800", rpcErr)
		}
		if called {
			t.Fatal("callback ran after cancellation")
		}
	})

	t.Run("unknown session", func(t *testing.T) {
		_, server := newSkillProviderClient(t)
		rpcErr := requestSkillProviderError(t, server, "memoryStore.store", params("missing"))
		assertSkillProviderError(t, rpcErr, "Memory store owner is unavailable")
	})
}

func TestMemoryStoreServedUntilDetachCompletes(t *testing.T) {
	client, server := newSkillProviderClient(t)
	session := newSession("memory-detach", client.client, "", false)
	var stored []StoredMemory
	session.memoryStore = func(_ context.Context, memory StoredMemory) error {
		stored = append(stored, memory)
		return nil
	}
	client.sessions[session.SessionID] = session
	server.SetRequestHandler("session.detach", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		if _, err := server.Request(t.Context(), "memoryStore.store", map[string]any{
			"sessionId": session.SessionID,
			"subject":   "package manager",
			"fact":      "Use pnpm.",
			"citations": "package.json",
			"reason":    "The workspace uses pnpm.",
		}); err != nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: "memoryStore.store during detach failed: " + err.Error()}
		}
		return []byte(`{"success":true}`), nil
	})

	if err := session.Disconnect(); err != nil {
		t.Fatalf("Disconnect failed: %v", err)
	}
	if len(stored) != 1 {
		t.Fatalf("stored = %+v, want one memory stored during detach", stored)
	}
}
