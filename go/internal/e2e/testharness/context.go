package testharness

import (
	"os"
	"path/filepath"
	"regexp"
	"runtime"
	"strings"
	"sync"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
)

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

		// Look for CLI in sibling nodejs directory's node_modules
		abs, err := filepath.Abs("../../../nodejs/node_modules/@github/copilot/index.js")
		if err == nil && fileExists(abs) {
			cliPath = abs
			return
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

	workDir, err := os.MkdirTemp("", "copilot-test-work-")
	if err != nil {
		os.RemoveAll(homeDir)
		t.Fatalf("Failed to create temp work dir: %v", err)
	}

	proxy := NewCapiProxy()
	proxyURL, err := proxy.Start()
	if err != nil {
		os.RemoveAll(homeDir)
		os.RemoveAll(workDir)
		t.Fatalf("Failed to start proxy: %v", err)
	}

	ctx := &TestContext{
		CLIPath:  cliPath,
		HomeDir:  homeDir,
		WorkDir:  workDir,
		ProxyURL: proxyURL,
		proxy:    proxy,
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

	// Extract test file name: ask_user_test.go -> ask_user
	testFile := strings.TrimSuffix(filepath.Base(callerFile), "_test.go")

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

// Close cleans up the test context resources.
func (c *TestContext) Close(testFailed bool) {
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

// GetExchanges retrieves the captured HTTP exchanges from the proxy.
func (c *TestContext) GetExchanges() ([]ParsedHttpExchange, error) {
	return c.proxy.GetExchanges()
}

// Env returns environment variables configured for isolated testing.
func (c *TestContext) Env() []string {
	env := os.Environ()

	// Add overrides (later values take precedence in most systems)
	env = append(env,
		"COPILOT_API_URL="+c.ProxyURL,
		"XDG_CONFIG_HOME="+c.HomeDir,
		"XDG_STATE_HOME="+c.HomeDir,
	)
	return env
}

// NewClient creates a CopilotClient configured for this test context.
func (c *TestContext) NewClient() *copilot.Client {
	options := &copilot.ClientOptions{
		CLIPath: c.CLIPath,
		Cwd:     c.WorkDir,
		Env:     c.Env(),
	}

	// Use fake token in CI to allow cached responses without real auth
	if os.Getenv("CI") == "true" {
		options.GithubToken = "fake-token-for-e2e-tests"
	}

	return copilot.NewClient(options)
}

func fileExists(path string) bool {
	_, err := os.Stat(path)
	return err == nil
}
