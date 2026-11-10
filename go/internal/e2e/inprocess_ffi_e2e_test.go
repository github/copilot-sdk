package e2e

import (
	"context"
	"fmt"
	"strings"
	"sync"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

// TestInProcessFfiE2E exercises native-host startup and overlapping callbacks
// through Go's FFI adapter, including disposal while another host is active.
func TestInProcessFfiE2E(t *testing.T) {
	t.Parallel()
	// Keep the dedicated native-host coverage in the existing in-process selection.
	if !testharness.IsInProcessTransport() {
		t.Skip("in-process FFI smoke test runs only under the inprocess transport cell")
	}

	cliPath := testharness.CLIPath()
	if cliPath == "" {
		t.Fatal("CLI not found. Run 'npm install' in the nodejs directory first.")
	}
	t.Run("should start and connect over in-process FFI", func(t *testing.T) {
		fixture := testharness.NewTestContext(t)
		client := copilot.NewClient(&copilot.ClientOptions{
			Connection:    copilot.InProcessConnection{},
			BaseDirectory: fixture.HomeDir,
		})
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Failed to start client over in-process FFI: %v", err)
		}

		pong, err := client.Ping(t.Context(), "ffi message")
		if err != nil {
			t.Fatalf("Failed to ping: %v", err)
		}

		if pong.Message != "pong: ffi message" {
			t.Errorf("Expected pong.message to be 'pong: ffi message', got %q", pong.Message)
		}

		if pong.Timestamp.IsZero() {
			t.Errorf("Expected non-zero pong.timestamp, got %s", pong.Timestamp)
		}

		if err := client.Stop(); err != nil {
			t.Errorf("Expected no errors on stop, got %v", err)
		}
	})

	t.Run("should overlap independent hosts and preserve the survivor", func(t *testing.T) {
		ctx, cancel := context.WithTimeout(t.Context(), 30*time.Second)
		defer cancel()

		type result struct {
			session *copilot.Session
			message *copilot.SessionEvent
			err     error
		}
		clients := make([]*testharness.Client, 2)
		releases := make([]func(), 2)
		results := make([]chan result, 2)
		entered := make(chan int, 2)
		for i := range clients {
			fixture := testharness.NewTestContext(t)
			fixture.ConfigureSnapshot(t, "session/should_have_stateful_conversation")
			token := fmt.Sprintf("ffi-host-%d-token", i)
			if err := fixture.SetDefaultCopilotUserByToken(token); err != nil {
				t.Fatal(err)
			}
			client := fixture.NewClient(func(options *copilot.ClientOptions) {
				options.GitHubToken = token
				options.BaseDirectory = fixture.HomeDir
			})
			clients[i] = client
			t.Cleanup(func() { client.ForceStop() })
			release := make(chan struct{})
			releases[i] = sync.OnceFunc(func() { close(release) })
			t.Cleanup(releases[i])
			results[i] = make(chan result, 1)
			go func() {
				session, err := client.CreateSession(ctx, &copilot.SessionConfig{
					OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
					Hooks: &copilot.SessionHooks{
						OnSessionStart: func(input copilot.SessionStartHookInput, _ copilot.HookInvocation) (*copilot.SessionStartHookOutput, error) {
							if input.WorkingDirectory != fixture.WorkDir {
								return nil, fmt.Errorf("host %d cwd = %q, want %q", i, input.WorkingDirectory, fixture.WorkDir)
							}
							entered <- i
							select {
							case <-release:
								return nil, nil
							case <-ctx.Done():
								return nil, ctx.Err()
							}
						},
					},
				})
				if err != nil {
					results[i] <- result{err: err}
					return
				}
				message, err := session.SendAndWait(ctx, copilot.MessageOptions{Prompt: "What is 1+1?"})
				results[i] <- result{session: session, message: message, err: err}
			}()
		}

		seen := map[int]bool{}
		for len(seen) < len(clients) {
			select {
			case i := <-entered:
				seen[i] = true
			case result := <-results[0]:
				t.Fatalf("First host finished before both callbacks overlapped: %v", result.err)
			case result := <-results[1]:
				t.Fatalf("Second host finished before both callbacks overlapped: %v", result.err)
			case <-ctx.Done():
				t.Fatalf("Native callbacks did not overlap; entered=%v: %v", seen, ctx.Err())
			}
		}
		awaitSession := func(i int) *copilot.Session {
			t.Helper()
			select {
			case result := <-results[i]:
				if result.err != nil {
					t.Fatalf("Host %d turn failed: %v", i, result.err)
				}
				if got := assistantContent(t, result.message); !strings.Contains(got, "2") {
					t.Fatalf("Host %d response = %q, want 2", i, got)
				}
				return result.session
			case <-ctx.Done():
				t.Fatalf("Host %d turn did not finish: %v", i, ctx.Err())
				return nil
			}
		}
		releases[0]()
		awaitSession(0)
		if err := clients[0].Stop(); err != nil {
			t.Fatalf("Stop first host while second callback is pending: %v", err)
		}
		releases[1]()
		survivor := awaitSession(1)
		if _, err := survivor.GetEvents(ctx); err != nil {
			t.Fatalf("Surviving session is unusable after peer disposal: %v", err)
		}
		pong, err := clients[1].Ping(ctx, "survivor")
		if err != nil || pong.Message != "pong: survivor" {
			t.Fatalf("Surviving host Ping = %v, %v", pong, err)
		}
		if err := clients[1].Stop(); err != nil {
			t.Fatalf("Stop surviving host: %v", err)
		}
	})
}
