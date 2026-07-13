package testharness

import (
	"os"
	"path/filepath"
	"regexp"
	"runtime"
	"strings"
	"sync"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
)

const defaultGitHubToken = "fake-token-for-e2e-tests"

var (
	cliPath     string
	cliPathOnce sync.Once
)

// CLIPath returns the path to the Copilot CLI, discovering it once and caching.
func CLIPath() string {
	cliPathOnce.Do(func() {
		// Check environment variable first
		if path := os.Getenv("COPILOT_CLI_PATH"); path != "" {
			cliPath = path
			return
		}

		// Look for CLI in sibling nodejs directory's node_modules. As of CLI
		// 1.0.64-1 the @github/copilot package is a thin loader; the runnable
		// index.js ships in the installed platform package
		// (e.g. @github/copilot-linux-x64).
		base, err := filepath.Abs("../../../nodejs/node_modules/@github")
		if err == nil {
			matches, _ := filepath.Glob(filepath.Join(base, "copilot-*", "index.js"))
			if len(matches) > 0 {
				cliPath = matches[0]
				return
			}
		}
	})
	return cliPath
}

// TestContext holds shared resources for E2E tests.
type TestContext struct {
	CLIPath  string
	HomeDir  string
	WorkDir  string
	ProxyURL string

	proxy *CapiProxy

	// In-process transport state. When the inprocess CI matrix cell is active the
	// worker inherits this process's ambient env and cwd (per-client env/working
	// directory are rejected in-process), so the isolated test env/cwd are mirrored
	// onto the real process and restored on Close.
	inProcess  bool
	restoreEnv []envRestore
	restoreCwd string
}

// envRestore captures a single environment variable's prior value so the
// in-process ambient mirror can be undone during teardown.
type envRestore struct {
	key  string
	prev string
	had  bool
}

// isInProcessTransport reports whether the in-process (FFI) transport is selected
// for E2E tests via COPILOT_SDK_DEFAULT_CONNECTION=inprocess. Mirrors the
// Node/Python/.NET harnesses.
func isInProcessTransport() bool {
	return strings.EqualFold(os.Getenv("COPILOT_SDK_DEFAULT_CONNECTION"), "inprocess")
}

// IsInProcessTransport reports whether E2E tests run under the in-process (FFI)
// transport. Tests that configure options unsupported in-process (e.g. per-client
// telemetry) should skip when this returns true.
func IsInProcessTransport() bool {
	return isInProcessTransport()
}

// NewTestContext creates a new test context with isolated directories and a replaying proxy.
func NewTestContext(t *testing.T) *TestContext {
	t.Helper()

	cliPath := CLIPath()
	if cliPath == "" || !fileExists(cliPath) {
		t.Fatalf("CLI not found at %s. Run 'npm install' in the nodejs directory first.", cliPath)
	}

	homeDir, err := os.MkdirTemp("", "copilot-test-config-")
	if err != nil {
		t.Fatalf("Failed to create temp home dir: %v", err)
	}
	if resolved, err := filepath.EvalSymlinks(homeDir); err == nil {
		homeDir = resolved
	}

	workDir, err := os.MkdirTemp("", "copilot-test-work-")
	if err != nil {
		os.RemoveAll(homeDir)
		t.Fatalf("Failed to create temp work dir: %v", err)
	}
	// Resolve symlinks (e.g., macOS /var -> /private/var) so paths
	// match what spawned subprocesses see when they resolve their cwd.
	if resolved, err := filepath.EvalSymlinks(workDir); err == nil {
		workDir = resolved
	}

	proxy := NewCapiProxy()
	proxyURL, err := proxy.Start()
	if err != nil {
		os.RemoveAll(homeDir)
		os.RemoveAll(workDir)
		t.Fatalf("Failed to start proxy: %v", err)
	}
	if err := proxy.SetCopilotUserByToken(defaultGitHubToken, map[string]interface{}{
		"login":        "e2e-test-user",
		"copilot_plan": "individual_pro",
		"endpoints": map[string]interface{}{
			"api":       proxyURL,
			"telemetry": "https://localhost:1/telemetry",
		},
		"analytics_tracking_id": "e2e-test-tracking-id",
	}); err != nil {
		proxy.StopWithOptions(true)
		os.RemoveAll(homeDir)
		os.RemoveAll(workDir)
		t.Fatalf("Failed to configure default Copilot user: %v", err)
	}

	ctx := &TestContext{
		CLIPath:   cliPath,
		HomeDir:   homeDir,
		WorkDir:   workDir,
		ProxyURL:  proxyURL,
		proxy:     proxy,
		inProcess: isInProcessTransport(),
	}

	t.Cleanup(func() {
		ctx.Close(t.Failed())
	})

	return ctx
}

// ConfigureForTest configures the proxy for a specific subtest.
// Call this at the start of each t.Run subtest.
func (c *TestContext) ConfigureForTest(t *testing.T) {
	t.Helper()

	// Format: test/snapshots/<testFile>/<testName>.yaml
	// e.g., test/snapshots/session/should_have_stateful_conversation.yaml

	// Get the test file name from the caller's file path
	_, callerFile, _, ok := runtime.Caller(1)
	if !ok {
		t.Fatal("Failed to get caller information")
	}

	// Extract test file name: ask_user_test.go -> ask_user, ask_user_e2e_test.go -> ask_user
	testFile := strings.TrimSuffix(filepath.Base(callerFile), "_test.go")
	testFile = strings.TrimSuffix(testFile, "_e2e")

	// Extract and sanitize the subtest name from t.Name()
	// t.Name() returns "TestAskUser/should_handle_freeform_user_input_response"
	testName := t.Name()
	parts := strings.SplitN(testName, "/", 2)
	if len(parts) < 2 {
		t.Fatalf("Expected test name with subtest, got: %s", testName)
	}
	sanitizedName := strings.ToLower(regexp.MustCompile(`[^a-zA-Z0-9]`).ReplaceAllString(parts[1], "_"))
	snapshotPath := filepath.Join("..", "..", "..", "test", "snapshots", testFile, sanitizedName+".yaml")

	absSnapshotPath, err := filepath.Abs(snapshotPath)
	if err != nil {
		t.Fatalf("Failed to get absolute path: %v", err)
	}

	if err := c.proxy.Configure(absSnapshotPath, c.WorkDir); err != nil {
		t.Fatalf("Failed to configure proxy: %v", err)
	}
}

// ConfigureWithoutSnapshot initializes the replay proxy without loading a recorded CAPI
// exchange file. Use this for tests that serve all model-layer behavior locally but
// still need proxy-backed auth and GitHub API endpoints.
func (c *TestContext) ConfigureWithoutSnapshot(t *testing.T) {
	t.Helper()

	dummySnapshotPath := filepath.Join(c.WorkDir, "__no_snapshot__.yaml")
	if err := c.proxy.Configure(dummySnapshotPath, c.WorkDir); err != nil {
		t.Fatalf("Failed to configure proxy without snapshot: %v", err)
	}
}

// Close cleans up the test context resources.
func (c *TestContext) Close(testFailed bool) {
	c.restoreInProcessEnvironment()
	if c.proxy != nil {
		c.proxy.StopWithOptions(testFailed)
	}
	if c.HomeDir != "" {
		os.RemoveAll(c.HomeDir)
	}
	if c.WorkDir != "" {
		os.RemoveAll(c.WorkDir)
	}
}

// applyInProcessEnvironment mirrors the isolated test environment onto the real
// process for in-process hosting: the worker inherits this process's env and cwd
// at spawn, so per-test redirects must live on os.Environ and the process cwd.
// Auth flows via GH_TOKEN/GITHUB_TOKEN (the FFI argv omits the stdio auth-token
// wiring) and HMAC is disabled so host-side auth matches the replay snapshots.
// mergedEnv is the effective per-client env (harness defaults plus any per-test
// additions); workDir is the effective working directory. Values are restored in
// Close. Safe to call more than once (restores unwind in reverse).
func (c *TestContext) applyInProcessEnvironment(mergedEnv []string, workDir string) {
	inprocessEnv := map[string]string{}
	for _, kv := range mergedEnv {
		if key, value, ok := strings.Cut(kv, "="); ok {
			inprocessEnv[key] = value
		}
	}
	// Auth flows via GH_TOKEN/GITHUB_TOKEN and HMAC is disabled for the in-process
	// host, overriding any inherited values.
	inprocessEnv["GH_TOKEN"] = defaultGitHubToken
	inprocessEnv["GITHUB_TOKEN"] = defaultGitHubToken
	inprocessEnv["COPILOT_HMAC_KEY"] = ""
	inprocessEnv["CAPI_HMAC_KEY"] = ""

	for key, value := range inprocessEnv {
		prev, had := os.LookupEnv(key)
		c.restoreEnv = append(c.restoreEnv, envRestore{key: key, prev: prev, had: had})
		os.Setenv(key, value)
	}
	if workDir != "" {
		if c.restoreCwd == "" {
			if cwd, err := os.Getwd(); err == nil {
				c.restoreCwd = cwd
			}
		}
		os.Chdir(workDir)
	}
}

// restoreInProcessEnvironment undoes applyInProcessEnvironment during teardown.
func (c *TestContext) restoreInProcessEnvironment() {
	for i := len(c.restoreEnv) - 1; i >= 0; i-- {
		r := c.restoreEnv[i]
		if r.had {
			os.Setenv(r.key, r.prev)
		} else {
			os.Unsetenv(r.key)
		}
	}
	c.restoreEnv = nil
	if c.restoreCwd != "" {
		os.Chdir(c.restoreCwd)
		c.restoreCwd = ""
	}
}

// GetExchanges retrieves the captured HTTP exchanges from the proxy.
func (c *TestContext) GetExchanges() ([]ParsedHttpExchange, error) {
	return c.proxy.GetExchanges()
}

// WaitForExchanges waits until the proxy has captured at least the requested exchanges.
func (c *TestContext) WaitForExchanges(t *testing.T, minimumCount int) []ParsedHttpExchange {
	t.Helper()

	deadline := time.Now().Add(120 * time.Second)
	var lastErr error
	var exchanges []ParsedHttpExchange
	for time.Now().Before(deadline) {
		var err error
		exchanges, err = c.GetExchanges()
		if err == nil && len(exchanges) >= minimumCount {
			return exchanges
		}
		lastErr = err
		time.Sleep(100 * time.Millisecond)
	}

	if lastErr != nil {
		t.Fatalf("Timed out waiting for %d chat completion request(s): %v", minimumCount, lastErr)
	}
	t.Fatalf("Timed out waiting for %d chat completion request(s); captured %d", minimumCount, len(exchanges))
	return nil
}

// SetCopilotUserByToken registers a per-token user configuration on the proxy.
func (c *TestContext) SetCopilotUserByToken(token string, response map[string]interface{}) error {
	return c.proxy.SetCopilotUserByToken(token, response)
}

// Env returns environment variables configured for isolated testing.
func (c *TestContext) Env() []string {
	env := os.Environ()

	// Add overrides (later values take precedence in most systems)
	env = append(env, c.proxy.ProxyEnv()...)
	env = append(env,
		"COPILOT_API_URL="+c.ProxyURL,
		// Route GitHub API calls (e.g. the MCP registry policy check) to the
		// replay proxy so MCP enablement stays hermetic. Without this the CLI
		// reaches the real api.github.com, which is slow/unreachable on macOS
		// CI runners and makes MCP servers time out before reaching connected.
		"COPILOT_DEBUG_GITHUB_API_URL="+c.ProxyURL,
		"COPILOT_HOME="+c.HomeDir,
		"COPILOT_SDK_AUTH_TOKEN="+defaultGitHubToken,
		"GH_CONFIG_DIR="+c.HomeDir,
		"GH_TOKEN="+defaultGitHubToken,
		"GITHUB_TOKEN="+defaultGitHubToken,
		"COPILOT_MCP_APPS=true",
		"MCP_APPS=true",
		"XDG_CONFIG_HOME="+c.HomeDir,
		"XDG_STATE_HOME="+c.HomeDir,
	)
	return env
}

// NewClient creates a CopilotClient configured for this test context.
// Optional overrides can be applied to the default ClientOptions via the opts function.
func (c *TestContext) NewClient(opts ...func(*copilot.ClientOptions)) *copilot.Client {
	options := &copilot.ClientOptions{
		Connection:       copilot.StdioConnection{Path: c.CLIPath},
		WorkingDirectory: c.WorkDir,
		Env:              c.Env(),
	}

	for _, opt := range opts {
		opt(options)
	}

	_, externalRuntime := options.Connection.(copilot.URIConnection)
	if options.GitHubToken == "" && !externalRuntime {
		options.GitHubToken = defaultGitHubToken
	}

	// Under the inprocess matrix cell, host the default stdio connection in-process.
	// The worker inherits this process's ambient env/cwd (per-client env and working
	// directory are rejected in-process), so mirror the effective (merged) env and
	// cwd onto the real process and drop those options. Tests that pin a specific
	// transport (TCP/URI/custom stdio) or configure per-client telemetry are left on
	// their transport, mirroring the Node/.NET harnesses.
	if c.inProcess && c.shouldUseInProcess(options) {
		c.applyInProcessEnvironment(options.Env, options.WorkingDirectory)
		options.Connection = copilot.InProcessConnection{Path: c.CLIPath}
		options.Env = nil
		options.WorkingDirectory = ""
	}

	return copilot.NewClient(options)
}

// shouldUseInProcess reports whether a client built from options should be hosted
// in-process for the inprocess matrix cell. Only the harness default stdio
// connection is swapped; a test that pins a custom stdio path/args/env, a TCP/URI
// connection, or configures per-client telemetry (which cannot be carried
// in-process) is exercising behavior that must stay on its own transport.
func (c *TestContext) shouldUseInProcess(options *copilot.ClientOptions) bool {
	if options.Telemetry != nil {
		return false
	}
	s, ok := options.Connection.(copilot.StdioConnection)
	if !ok {
		return false
	}
	return s.Path == c.CLIPath && len(s.Args) == 0 && s.Env == nil
}

func fileExists(path string) bool {
	_, err := os.Stat(path)
	return err == nil
}
