package testharness

import (
	"context"
	"flag"
	"fmt"
	"os"
	"os/exec"
	"regexp"
	"strings"
	"testing"
	"time"
)

const isolatedInProcessTestEnv = "COPILOT_SDK_ISOLATED_INPROCESS_TEST"

// RunWithInProcessIsolation runs each selected top-level test in a fresh
// process when the FFI transport is selected.
func RunWithInProcessIsolation(m *testing.M) int {
	if !flag.Parsed() {
		flag.Parse()
	}
	if !IsInProcessTransport() || os.Getenv(isolatedInProcessTestEnv) != "" {
		return m.Run()
	}

	tests, err := listTopLevelTests()
	if err != nil {
		fmt.Fprintf(os.Stderr, "list isolated in-process tests: %v\n", err)
		return 1
	}

	runPattern, subtestPattern := selectedTestPatterns()
	runRegexp, err := regexp.Compile(runPattern)
	if err != nil {
		fmt.Fprintf(os.Stderr, "compile -test.run pattern %q: %v\n", runPattern, err)
		return 1
	}

	ctx := context.Background()
	cancel := func() {}
	start := time.Now()
	if timeout := testTimeout(); timeout > 0 {
		ctx, cancel = context.WithTimeout(ctx, timeout)
	}
	defer cancel()

	failed := false
	for _, name := range tests {
		if !runRegexp.MatchString(name) {
			continue
		}

		selector := "^" + regexp.QuoteMeta(name) + "$"
		if subtestPattern != "" {
			selector += "/" + subtestPattern
		}
		if err := runIsolatedProcess(ctx, name, selector, remainingTimeout(start)); err != nil {
			fmt.Fprintln(os.Stderr, err)
			failed = true
		}
	}
	if failed {
		return 1
	}
	fmt.Println("PASS")
	return 0
}

// RunInIsolatedProcess re-executes a subtest in a fresh process when its
// top-level worker already owns a different in-process test environment.
// Call it before creating a TestContext and return when it returns true.
func RunInIsolatedProcess(t *testing.T) bool {
	t.Helper()
	if !IsInProcessTransport() {
		return false
	}
	if isolatedTest := os.Getenv(isolatedInProcessTestEnv); isolatedTest == t.Name() {
		return false
	}

	timeout := time.Duration(0)
	if deadline, ok := t.Deadline(); ok {
		timeout = time.Until(deadline)
		if timeout <= 0 {
			t.Fatal("Test deadline expired before isolated FFI execution")
		}
	}
	selector := exactTestSelector(t.Name())
	if err := runIsolatedProcess(t.Context(), t.Name(), selector, timeout); err != nil {
		t.Fatal(err)
	}
	return true
}

func listTopLevelTests() ([]string, error) {
	executable, err := os.Executable()
	if err != nil {
		return nil, err
	}
	command := exec.Command(executable, isolatedTestArgs(os.Args[1:], "^Test", true, 0)...)
	command.Env = setEnvironmentValue(os.Environ(), isolatedInProcessTestEnv, "list")
	output, err := command.CombinedOutput()
	if err != nil {
		return nil, fmt.Errorf("%w\n%s", err, output)
	}

	var tests []string
	for _, line := range strings.Split(string(output), "\n") {
		line = strings.TrimSpace(line)
		if strings.HasPrefix(line, "Test") {
			tests = append(tests, line)
		}
	}
	return tests, nil
}

func runIsolatedProcess(ctx context.Context, name, selector string, timeout time.Duration) error {
	executable, err := os.Executable()
	if err != nil {
		return fmt.Errorf("locate test executable: %w", err)
	}
	command := exec.CommandContext(ctx, executable, isolatedTestArgs(os.Args[1:], selector, false, timeout)...)
	command.Env = setEnvironmentValue(os.Environ(), isolatedInProcessTestEnv, name)
	command.WaitDelay = 5 * time.Second
	output, err := command.CombinedOutput()
	fmt.Print(string(output))
	if err != nil {
		return fmt.Errorf("isolated FFI test %s failed: %w", name, err)
	}
	if !strings.Contains(string(output), "--- PASS: "+name+" (") &&
		!strings.Contains(string(output), "--- SKIP: "+name+" (") {
		return fmt.Errorf("isolated FFI process did not report completing %s", name)
	}
	return nil
}

func isolatedTestArgs(args []string, selector string, list bool, timeout time.Duration) []string {
	result := make([]string, 0, len(args)+5)
	for i := 0; i < len(args); i++ {
		arg := args[i]
		key := arg
		if before, _, ok := strings.Cut(arg, "="); ok {
			key = before
		}
		switch key {
		case "-test.run", "-test.list", "-test.timeout", "-test.count", "-test.v",
			"-test.coverprofile", "-test.testlogfile":
			if arg == key && i+1 < len(args) {
				i++
			}
			continue
		}
		result = append(result, arg)
	}
	if list {
		return append(result, "-test.list="+selector)
	}
	result = append(result, "-test.run="+selector, "-test.count=1", "-test.v=true")
	if timeout > 0 {
		result = append(result, "-test.timeout="+timeout.String())
	}
	return result
}

func selectedTestPatterns() (string, string) {
	run := ""
	if runFlag := flag.Lookup("test.run"); runFlag != nil {
		run = runFlag.Value.String()
	}
	if run == "" {
		return ".", ""
	}
	topLevel, subtest, _ := strings.Cut(run, "/")
	return topLevel, subtest
}

func testTimeout() time.Duration {
	timeoutFlag := flag.Lookup("test.timeout")
	if timeoutFlag == nil {
		return 0
	}
	timeout, _ := time.ParseDuration(timeoutFlag.Value.String())
	return timeout
}

func remainingTimeout(start time.Time) time.Duration {
	timeout := testTimeout()
	if timeout == 0 {
		return 0
	}
	remaining := timeout - time.Since(start)
	if remaining < 0 {
		return time.Nanosecond
	}
	return remaining
}

func exactTestSelector(name string) string {
	parts := strings.Split(name, "/")
	for i, part := range parts {
		parts[i] = "^" + regexp.QuoteMeta(part) + "$"
	}
	return strings.Join(parts, "/")
}

func setEnvironmentValue(env []string, key, value string) []string {
	prefix := key + "="
	result := make([]string, 0, len(env)+1)
	for _, entry := range env {
		if !strings.HasPrefix(entry, prefix) {
			result = append(result, entry)
		}
	}
	return append(result, prefix+value)
}
