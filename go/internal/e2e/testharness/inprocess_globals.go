package testharness

import (
	"fmt"
	"os"
	"os/exec"
	"regexp"
	"runtime"
	"strings"
	"sync"
	"testing"
	"time"
)

const isolatedInProcessTestEnv = "COPILOT_SDK_ISOLATED_INPROCESS_TEST"

// RunWithInProcessGlobals re-executes this test in a fresh process when FFI is
// selected. Call it before creating any clients and return when it returns true.
// The child runs the same test over FFI, with the same assertions and deadline.
// This is required for legacy runtime options that still read process globals.
func RunWithInProcessGlobals(t *testing.T) bool {
	t.Helper()
	if !IsInProcessTransport() {
		return false
	}
	if isolatedTest := os.Getenv(isolatedInProcessTestEnv); isolatedTest != "" {
		if isolatedTest != t.Name() {
			t.Fatalf("Unexpected isolated in-process test: want %s, got %s", isolatedTest, t.Name())
		}
		return false
	}

	executable, err := os.Executable()
	if err != nil {
		t.Fatalf("Locate test executable: %v", err)
	}
	args := isolatedTestArgs(os.Args[1:], t.Name())
	if deadline, ok := t.Deadline(); ok {
		remaining := time.Until(deadline)
		if remaining <= 0 {
			t.Fatal("Test deadline expired before isolated FFI execution")
		}
		args = append(args, "-test.timeout="+remaining.String())
	}
	command := exec.CommandContext(t.Context(), executable, args...)
	command.Env = append(os.Environ(), isolatedInProcessTestEnv+"="+t.Name())
	command.WaitDelay = 5 * time.Second
	output, err := command.CombinedOutput()
	t.Logf("Isolated FFI test output:\n%s", output)
	if err != nil {
		t.Fatalf("Isolated FFI test failed: %v", err)
	}
	if !strings.Contains(string(output), "--- PASS: "+t.Name()+" (") {
		t.Fatal("Isolated FFI process did not report passing the requested test")
	}
	return true
}

func isolatedTestArgs(args []string, name string) []string {
	parts := strings.Split(name, "/")
	for i, part := range parts {
		parts[i] = "^" + regexp.QuoteMeta(part) + "$"
	}
	result := append([]string(nil), args...)
	// Keep the shared gocoverdir so the parent includes child counters, but do
	// not overwrite the parent's final coverage profile or test-cache log.
	return append(result, "-test.run="+strings.Join(parts, "/"), "-test.count=1", "-test.v=true",
		"-test.coverprofile=", "-test.testlogfile=")
}

var inProcessGlobals struct {
	sync.Mutex
	initialized bool
}

// These legacy consumers bypass the native host's environment snapshot.
// Only isolated tests may configure them, once, before their first host starts.
func initializeInProcessGlobals(env []string) {
	if os.Getenv(isolatedInProcessTestEnv) == "" {
		return
	}
	inProcessGlobals.Lock()
	defer inProcessGlobals.Unlock()
	for _, key := range []string{
		"COPILOT_DEBUG_GITHUB_API_URL",
		"COPILOT_ALLOW_GET_PROVIDER_ENDPOINT",
		"COPILOT_ENABLE_SECRET_FILTERING",
	} {
		value, present := os.LookupEnv(key)
		for _, entry := range env {
			entryKey, entryValue, ok := strings.Cut(entry, "=")
			if ok && (entryKey == key || runtime.GOOS == "windows" && strings.EqualFold(entryKey, key)) {
				value, present = entryValue, true
			}
		}
		if !present {
			continue
		}
		current, exists := os.LookupEnv(key)
		if current == value && exists {
			continue
		}
		if inProcessGlobals.initialized {
			panic(fmt.Sprintf("process-global runtime option %s changed after native startup; isolate the test separately", key))
		}
		if err := os.Setenv(key, value); err != nil {
			panic(fmt.Errorf("initialize process-global runtime option %s: %w", key, err))
		}
	}
	inProcessGlobals.initialized = true
}
