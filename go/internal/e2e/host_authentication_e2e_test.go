package e2e

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"sync/atomic"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

type hostAuthBoundary struct {
	home, work string
	ahp        *testharness.AhpTestClient
}

func newHostAuthBoundary(t *testing.T) *hostAuthBoundary {
	t.Helper()
	if os.Getenv("COPILOT_RUNTIME_HOST_E2E") != "1" {
		t.Skip("Requires the built host; set COPILOT_RUNTIME_HOST_E2E=1")
	}
	// Keep owned files in the checkout, including when the OS temp root is shared.
	root, err := os.MkdirTemp(".", ".host-authentication-")
	if err != nil {
		t.Fatal(err)
	}
	root, err = filepath.Abs(root)
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() {
		if err := os.RemoveAll(root); err != nil {
			t.Error(err)
		}
	})
	boundary := &hostAuthBoundary{home: filepath.Join(root, "home"), work: filepath.Join(root, "work")}
	for _, path := range []string{boundary.home, boundary.work} {
		if err := os.Mkdir(path, 0700); err != nil {
			t.Fatal(err)
		}
	}
	boundary.ahp = testharness.NewAhpTestClientWithDriver(t, testharness.RepoPath("test", "harness", "hostAuthenticationDriver.mjs"))
	return boundary
}

func (b *hostAuthBoundary) client(t *testing.T) *copilot.Client {
	t.Helper()
	env := make(map[string]string)
	for _, item := range os.Environ() {
		key, value, _ := strings.Cut(item, "=")
		env[key] = value
	}
	for _, key := range []string{"GH_TOKEN", "GITHUB_TOKEN", "COPILOT_GITHUB_TOKEN", "COPILOT_HMAC_KEY", "CAPI_HMAC_KEY"} {
		env[key] = ""
	}
	env["COPILOT_HOME"], env["HOME"], env["USERPROFILE"] = b.home, b.home, b.home
	var environment []string
	for key, value := range env {
		environment = append(environment, key+"="+value)
	}
	client := copilot.NewClient(&copilot.ClientOptions{
		Connection: copilot.StdioConnection{Path: testharness.CLIPath()},
		Env:        environment, WorkingDirectory: b.work, UseLoggedInUser: copilot.Bool(false),
	})
	t.Cleanup(func() {
		if err := client.Stop(); err != nil {
			t.Error(err)
		}
	})
	return client
}

func (b *hostAuthBoundary) request(t *testing.T, command map[string]any) json.RawMessage {
	t.Helper()
	ctx, cancel := context.WithTimeout(t.Context(), time.Minute)
	defer cancel()
	result, err := b.ahp.Request(ctx, command)
	if err != nil {
		t.Fatal(err)
	}
	return result
}

func (b *hostAuthBoundary) reject(t *testing.T, command map[string]any) {
	t.Helper()
	ctx, cancel := context.WithTimeout(t.Context(), time.Minute)
	defer cancel()
	if result, err := b.ahp.Request(ctx, command); err == nil {
		t.Fatalf("request unexpectedly succeeded: %s", result)
	}
}

func hostAuthPolicy(authenticate func(context.Context, string) (*copilot.AhpAuthenticationResult, error)) copilot.AhpHostAuthentication {
	return copilot.AhpHostAuthentication{Mode: "custom", RequireIdentity: "owner", Authenticate: authenticate}
}

func acceptedHostAuth() *copilot.AhpAuthenticationResult {
	return &copilot.AhpAuthenticationResult{Identity: "owner", ExpiresAt: time.Now().Add(time.Minute).Format(time.RFC3339Nano)}
}

func TestHostAuthenticationBYOKCreateAndColdResumeE2E(t *testing.T) {
	for _, mode := range []string{"none", "custom"} {
		t.Run(mode, func(t *testing.T) {
			b := newHostAuthBoundary(t)
			var mu sync.Mutex
			var authorizations, bodies []string
			endpoint := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				body, err := io.ReadAll(r.Body)
				if err != nil {
					t.Error(err)
				}
				mu.Lock()
				authorizations = append(authorizations, r.Header.Get("Authorization"))
				bodies = append(bodies, string(body))
				mu.Unlock()
				w.Header().Set("Content-Type", "text/event-stream")
				for _, choice := range []string{
					`{"delta":{"role":"assistant","content":"GO_HOST_BYOK_OK"},"finish_reason":null}`,
					`{"delta":{},"finish_reason":"stop"}`,
				} {
					fmt.Fprintf(w, "data: {\"id\":\"local\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"local-model\",\"choices\":[{\"index\":0,%s}]}\n\n", strings.Trim(choice, "{}"))
				}
				fmt.Fprint(w, "data: [DONE]\n\n")
			}))
			defer endpoint.Close()
			provider := &copilot.ProviderConfig{Type: "openai", WireAPI: "completions", BaseURL: endpoint.URL + "/v1", APIKey: "inference-only-key"}
			policy := copilot.AhpHostAuthentication{Mode: copilot.AhpHostAuthenticationMode(mode)}
			var validations atomic.Int32
			if mode == "custom" {
				policy = hostAuthPolicy(func(_ context.Context, token string) (*copilot.AhpAuthenticationResult, error) {
					validations.Add(1)
					if token != "admission-only-key" {
						return nil, nil
					}
					return acceptedHostAuth(), nil
				})
			}
			var uri string
			var durableID atomic.Value
			for _, resume := range []bool{false, true} {
				owner := b.client(t)
				var original atomic.Pointer[copilot.Session]
				var called atomic.Int32
				check := func(authentication copilot.AhpSessionAuthentication, token string) error {
					called.Add(1)
					if token != "" || authentication.Mode() != rpc.HostSessionAuthenticationMode(mode) {
						return fmt.Errorf("incorrect authentication context: %#v, githubToken=%q", authentication, token)
					}
					if mode == "custom" {
						verified, ok := authentication.(*copilot.AhpSessionAuthenticationCustom)
						if !ok || verified.Identity != "owner" {
							return fmt.Errorf("incorrect custom identity: %#v", authentication)
						}
					}
					return nil
				}
				host, err := owner.StartAhpHost(t.Context(), &copilot.AhpHostOptions{
					Authentication: policy, LocalServer: &rpc.HostLocalServerOptions{Token: copilot.String("independent-websocket-secret")},
					AllowCreatingNewSessions: copilot.Bool(!resume),
					CreateSession: func(ctx context.Context, request copilot.AhpSessionCreateRequest) (*copilot.Session, error) {
						if err := check(request.Authentication, request.Config.GitHubToken); err != nil {
							return nil, err
						}
						request.Config.Provider, request.Config.Model = provider, "local-model"
						request.Config.ExtensionSDKPath = copilot.String(testharness.RepoPath("nodejs", "dist"))
						request.Config.OnPermissionRequest = copilot.PermissionHandler.ApproveAll
						session, err := owner.CreateSession(ctx, request.Config)
						if err == nil {
							durableID.Store(session.SessionID)
							original.Store(session)
						}
						return session, err
					},
					ResumeSession: func(ctx context.Context, request copilot.AhpSessionResumeRequest) (*copilot.Session, error) {
						if err := check(request.Authentication, request.Config.GitHubToken); err != nil {
							return nil, err
						}
						if request.SessionID != durableID.Load() {
							return nil, fmt.Errorf("resumed %q, expected %q", request.SessionID, durableID.Load())
						}
						request.Config.Provider, request.Config.Model = provider, "local-model"
						request.Config.ExtensionSDKPath = copilot.String(testharness.RepoPath("nodejs", "dist"))
						request.Config.OnPermissionRequest = copilot.PermissionHandler.ApproveAll
						session, err := owner.ResumeSessionWithOptions(ctx, request.SessionID, request.Config)
						if err == nil {
							original.Store(session)
						}
						return session, err
					},
				})
				if err != nil {
					t.Fatal(err)
				}
				if mode == "none" {
					b.reject(t, map[string]any{"op": "connect", "url": *host.URL, "token": "wrong-websocket-secret"})
				}
				b.request(t, map[string]any{"op": "connect", "url": *host.URL, "token": *host.Token})
				if mode == "custom" {
					b.reject(t, map[string]any{"op": "create", "workDir": b.work})
				}
				resources := b.request(t, map[string]any{"op": "authenticate", "token": "admission-only-key"})
				if mode == "none" && string(resources) != `{"resources":[]}` {
					t.Fatalf("anonymous host advertises protected resources: %s", resources)
				}
				if resume {
					b.request(t, map[string]any{"op": "attach", "uri": uri})
				} else {
					var created struct {
						URI string `json:"uri"`
					}
					if err := json.Unmarshal(b.request(t, map[string]any{"op": "create", "workDir": b.work}), &created); err != nil {
						t.Fatal(err)
					}
					uri = created.URI
				}
				var response struct {
					Text string `json:"text"`
				}
				if err := json.Unmarshal(b.request(t, map[string]any{"op": "turn"}), &response); err != nil || response.Text != "GO_HOST_BYOK_OK" {
					t.Fatalf("BYOK turn = %#v (%v)", response, err)
				}
				if called.Load() != 1 {
					t.Fatalf("factory calls = %d, want 1", called.Load())
				}
				if err := host.Dispose(t.Context()); err != nil {
					t.Fatal(err)
				}
				b.request(t, map[string]any{"op": "close"})
				if session := original.Load(); session != nil {
					if err := session.Disconnect(); err != nil {
						t.Fatal(err)
					}
				}
				if err := owner.Stop(); err != nil {
					t.Fatal(err)
				}
			}
			mu.Lock()
			defer mu.Unlock()
			if len(authorizations) != 2 {
				t.Fatalf("inference requests = %d, want 2", len(authorizations))
			}
			for i, authorization := range authorizations {
				if authorization != "Bearer inference-only-key" || strings.Contains(bodies[i], "admission-only-key") || strings.Contains(bodies[i], "independent-websocket-secret") {
					t.Fatalf("admission credential leaked into inference request %d: %q %s", i, authorization, bodies[i])
				}
			}
			if mode == "custom" && validations.Load() != 2 {
				t.Fatalf("validator calls = %d, want 2", validations.Load())
			}
		})
	}
}

func TestHostAuthenticationRejectsInvalidCredentialsE2E(t *testing.T) {
	for _, failure := range []string{"reject", "error", "identity", "missing-expiry", "invalid-expiry", "past-expiry"} {
		t.Run(failure, func(t *testing.T) {
			b := newHostAuthBoundary(t)
			client := b.client(t)
			var factories atomic.Int32
			host, err := client.StartAhpHost(t.Context(), &copilot.AhpHostOptions{
				LocalServer: &rpc.HostLocalServerOptions{},
				Authentication: hostAuthPolicy(func(_ context.Context, token string) (*copilot.AhpAuthenticationResult, error) {
					if token != "admission-only-key" {
						return nil, errors.New("incorrect decrypted token")
					}
					result := acceptedHostAuth()
					switch failure {
					case "reject":
						return nil, nil
					case "error":
						return nil, errors.New("validator unavailable")
					case "identity":
						result.Identity = "another-owner"
					case "missing-expiry":
						result.ExpiresAt = ""
					case "invalid-expiry":
						result.ExpiresAt = "not-a-date"
					case "past-expiry":
						result.ExpiresAt = "2000-01-01T00:00:00Z"
					}
					return result, nil
				}),
				CreateSession: func(context.Context, copilot.AhpSessionCreateRequest) (*copilot.Session, error) {
					factories.Add(1)
					return nil, errors.New("must not materialize rejected session")
				},
			})
			if err != nil {
				t.Fatal(err)
			}
			b.request(t, map[string]any{"op": "connect", "url": *host.URL, "token": host.Token})
			b.reject(t, map[string]any{"op": "authenticate", "token": "admission-only-key"})
			b.reject(t, map[string]any{"op": "create", "workDir": b.work})
			if factories.Load() != 0 {
				t.Fatal("rejected credentials invoked factory")
			}
			if err := host.Dispose(t.Context()); err != nil {
				t.Fatal(err)
			}
			b.request(t, map[string]any{"op": "close"})
		})
	}
}

func TestHostAuthenticationCancellationE2E(t *testing.T) {
	b := newHostAuthBoundary(t)
	client := b.client(t)
	entered := make(chan context.Context, 1)
	finish := make(chan struct{})
	exited := make(chan struct{})
	var once sync.Once
	release := func() { once.Do(func() { close(finish) }) }
	t.Cleanup(release)
	host, err := client.StartAhpHost(t.Context(), &copilot.AhpHostOptions{
		LocalServer: &rpc.HostLocalServerOptions{},
		Authentication: hostAuthPolicy(func(ctx context.Context, _ string) (*copilot.AhpAuthenticationResult, error) {
			defer close(exited)
			entered <- ctx
			<-finish
			return acceptedHostAuth(), nil
		}),
	})
	if err != nil {
		t.Fatal(err)
	}
	b.request(t, map[string]any{"op": "connect", "url": *host.URL, "token": host.Token})
	done := make(chan error, 1)
	go func() {
		ctx, cancel := context.WithTimeout(t.Context(), time.Minute)
		defer cancel()
		_, err := b.ahp.Request(ctx, map[string]any{"op": "authenticate", "token": "pending"})
		done <- err
	}()
	var validation context.Context
	select {
	case validation = <-entered:
	case <-time.After(10 * time.Second):
		t.Fatal("validator did not start")
	}
	if err := host.Dispose(t.Context()); err != nil {
		t.Fatal(err)
	}
	select {
	case <-validation.Done():
	case <-time.After(10 * time.Second):
		t.Fatal("host disposal did not cancel validator")
	}
	select {
	case err := <-done:
		if err == nil {
			t.Fatal("cancelled authentication accepted")
		}
	case <-time.After(10 * time.Second):
		t.Fatal("authentication did not end after disposal")
	}
	b.request(t, map[string]any{"op": "close"})
	release()
	select {
	case <-exited:
	case <-time.After(10 * time.Second):
		t.Fatal("late validator did not exit")
	}
}

func TestHostAuthenticationExpiryAndReauthenticationE2E(t *testing.T) {
	b := newHostAuthBoundary(t)
	client := b.client(t)
	type credential struct {
		token  string
		expiry time.Time
		result *copilot.AhpAuthenticationResult
	}
	var validations atomic.Int32
	var issued atomic.Pointer[credential]
	host, err := client.StartAhpHost(t.Context(), &copilot.AhpHostOptions{
		LocalServer: &rpc.HostLocalServerOptions{},
		Authentication: hostAuthPolicy(func(_ context.Context, token string) (*copilot.AhpAuthenticationResult, error) {
			if token != "short-lived" && token != "renewed" {
				return nil, fmt.Errorf("unexpected fixture token: %q", token)
			}
			lifetime := 2 * time.Second
			if token == "renewed" {
				lifetime = time.Minute
			}
			// Credential expiration uses wall time, not the local monotonic reading.
			expiry := time.Now().Round(0).Add(lifetime)
			result := &copilot.AhpAuthenticationResult{Identity: "owner", ExpiresAt: expiry.Format(time.RFC3339Nano)}
			issued.Store(&credential{token: token, expiry: expiry, result: result})
			validations.Add(1)
			return result, nil
		}),
	})
	if err != nil {
		t.Fatal(err)
	}
	b.request(t, map[string]any{"op": "connect", "url": *host.URL, "token": host.Token})
	ctx, cancel := context.WithTimeout(t.Context(), time.Minute)
	defer cancel()
	var attempts int32
	for {
		issued.Store(nil)
		before := validations.Load()
		attempts++
		if err := ctx.Err(); err != nil {
			t.Fatal(err)
		}
		_, err := b.ahp.Request(ctx, map[string]any{"op": "authenticate", "token": "short-lived"})
		if err != nil && ctx.Err() != nil {
			t.Fatalf("authentication failed after the original owner ended: %v (context: %v)", err, ctx.Err())
		}
		if err != nil &&
			err.Error() != "AHP test client: RPC error -32603: internal error: application authentication failed" &&
			err.Error() != "AHP test client: RPC error -32007: authentication required for https://copilot.github.com/hosting/authentication" {
			t.Fatal(err)
		}
		current := issued.Load()
		if validations.Load() != before+1 || current == nil {
			t.Fatalf("authentication must return exactly one new fixture credential: calls=%d, before=%d, credential=%v, error=%v", validations.Load(), before, current, err)
		}
		if current.token != "short-lived" || current.result.Identity != "owner" || current.result.ExpiresAt != current.expiry.Format(time.RFC3339Nano) {
			t.Fatalf("unexpected fixture credential: %+v", current)
		}
		if err == nil {
			break
		}
		if current.expiry.After(time.Now()) {
			t.Fatal(err)
		}
		t.Logf("Retrying initial authentication with an already-expired fixture credential: %v", err)
	}
	b.request(t, map[string]any{"op": "expired"})
	b.reject(t, map[string]any{"op": "create", "workDir": b.work})
	issued.Store(nil)
	beforeRenewal := validations.Load()
	b.request(t, map[string]any{"op": "authenticate", "token": "renewed"})
	if validations.Load() != beforeRenewal+1 || issued.Load() == nil || issued.Load().token != "renewed" {
		t.Fatalf("renewal must invoke its validator once: calls=%d, before=%d, credential=%v", validations.Load(), beforeRenewal, issued.Load())
	}
	if validations.Load() != attempts+1 {
		t.Fatalf("validator calls = %d, want %d", validations.Load(), attempts+1)
	}
	if err := host.Dispose(t.Context()); err != nil {
		t.Fatal(err)
	}
	b.request(t, map[string]any{"op": "close"})
}
