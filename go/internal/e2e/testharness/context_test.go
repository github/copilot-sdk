package testharness

import (
	"encoding/json"
	"io"
	"net/http"
	"net/http/httptest"
	"os"
	"os/exec"
	"reflect"
	"strings"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
)

func TestScopedClientsPreserveProcessState(t *testing.T) {
	t.Parallel()
	beforeEnv := os.Environ()
	beforeCwd, err := os.Getwd()
	if err != nil {
		t.Fatal(err)
	}
	for _, inProcess := range []bool{false, true} {
		for _, dir := range []string{"first", "second"} {
			fixture := &TestContext{
				CLIPath:   "copilot-runtime",
				HomeDir:   dir + "-home",
				WorkDir:   dir + "-work",
				proxy:     NewCapiProxy(),
				inProcess: inProcess,
			}
			client := fixture.NewClient()
			config := &copilot.SessionConfig{OnPermissionRequest: copilot.PermissionHandler.ApproveAll}
			got := client.sessionConfig(config)
			if got.WorkingDirectory != fixture.WorkDir || config.WorkingDirectory != "" {
				t.Fatalf("Scoped config = %+v; original = %+v", got, config)
			}
			explicit := &copilot.SessionConfig{WorkingDirectory: "explicit"}
			if client.sessionConfig(explicit) != explicit || client.sessionConfig(nil) != nil {
				t.Fatal("Explicit directory or invalid nil config was changed")
			}
			cloud := &copilot.SessionConfig{Cloud: &copilot.CloudSessionOptions{}}
			if client.sessionConfig(cloud) != cloud {
				t.Fatal("Cloud session was assigned a local directory")
			}
			client.ForceStop()
			virtualClient := fixture.NewClient(func(options *copilot.ClientOptions) {
				options.SessionFS = &copilot.SessionFSConfig{
					InitialWorkingDirectory: "/",
					SessionStatePath:        "/state",
					Conventions:             "posix",
				}
			})
			if virtualClient.sessionConfig(config) != config {
				t.Fatal("Virtual filesystem session was assigned a local directory")
			}
			virtualClient.ForceStop()
			observer := fixture.NewClient(func(options *copilot.ClientOptions) {
				options.Connection = copilot.URIConnection{URL: "localhost:12345"}
			})
			if observer.sessionConfig(config) != config {
				t.Fatal("Existing runtime was assigned the observer's local directory")
			}
			observer.ForceStop()
		}
	}
	afterCwd, err := os.Getwd()
	if err != nil || beforeCwd != afterCwd || !reflect.DeepEqual(beforeEnv, os.Environ()) {
		t.Fatalf("Fixture changed process cwd/environment: cwd %q -> %q, err=%v", beforeCwd, afterCwd, err)
	}
}

func TestInProcessClientRejectsExplicitFixtureEnvironment(t *testing.T) {
	t.Parallel()
	fixture := &TestContext{
		CLIPath:   "copilot-runtime",
		HomeDir:   t.TempDir(),
		WorkDir:   t.TempDir(),
		proxy:     NewCapiProxy(),
		inProcess: true,
	}
	defer func() {
		message, ok := recover().(string)
		if !ok || !strings.Contains(message, "environment overrides") {
			t.Errorf("Expected explicit environment overrides to be rejected, got %q", message)
		}
	}()
	fixture.NewClient(func(options *copilot.ClientOptions) {
		options.Env = append(options.Env, "EXPLICIT_FIXTURE_ENV=value")
	}).ForceStop()
}

func TestWithoutSigningKeys(t *testing.T) {
	t.Parallel()
	env := []string{"COPILOT_HMAC_KEY=secret", "CAPI_HMAC_KEY=secret", "GH_TOKEN=token", "EMPTY="}
	got := withoutSigningKeys(env)
	want := []string{"GH_TOKEN=token", "EMPTY="}
	if !reflect.DeepEqual(got, want) {
		t.Fatalf("Scoped env = %v, want %v", got, want)
	}
	if len(env) != 4 || env[0] != "COPILOT_HMAC_KEY=secret" {
		t.Fatal("Source environment was mutated")
	}
}

func TestSharedSnapshotIsReadOnly(t *testing.T) {
	t.Parallel()
	cmd := exec.Command(os.Args[0], "-test.run=^TestProxyShutdownProcess$")
	cmd.Env = append(os.Environ(), "COPILOT_PROXY_SHUTDOWN_PROCESS=1")
	stdin, err := cmd.StdinPipe()
	if err != nil {
		t.Fatal(err)
	}
	if err := cmd.Start(); err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() {
		stdin.Close()
		if cmd.ProcessState == nil {
			cmd.Process.Kill()
			cmd.Wait()
		}
	})
	workDir := t.TempDir()
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		switch r.URL.Path {
		case "/config":
			var config struct{ FilePath, WorkDir string }
			if err := json.NewDecoder(r.Body).Decode(&config); err != nil {
				t.Error(err)
			}
			if config.FilePath != RepoPath("test", "snapshots", "session/example.yaml") || config.WorkDir != workDir {
				t.Errorf("Unexpected shared snapshot configuration: %+v", config)
			}
		case "/stop":
			if r.URL.Query().Get("skipWritingCache") != "true" {
				t.Error("Shared replay fixture would be overwritten")
			}
			stdin.Close()
		default:
			t.Errorf("Unexpected proxy request: %s", r.URL)
		}
		io.WriteString(w, "{}")
	}))
	t.Cleanup(server.Close)
	fixture := &TestContext{WorkDir: workDir, proxy: &CapiProxy{cmd: cmd, proxyURL: server.URL}}
	fixture.ConfigureSnapshot(t, "session/example")
	if err := fixture.Close(false); err != nil {
		t.Fatalf("Close successful shared-snapshot test: %v", err)
	}
}
