// Package testdiagnostics captures test-process state before the CI job deadline.
package testdiagnostics

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"runtime"
	"strconv"
	"strings"
	"time"
)

// Run preserves the test runner's result and optionally records pre-timeout
// goroutine stacks. The external CI watchdog owns termination and native samples.
func Run(run func() int) int {
	directory := os.Getenv("GO_TEST_DIAGNOSTIC_DIRECTORY")
	if directory == "" {
		return run()
	}
	captureAt, err := strconv.ParseInt(os.Getenv("GO_TEST_DIAGNOSTIC_CAPTURE_AT"), 10, 64)
	if err != nil || captureAt <= 0 {
		fmt.Fprintln(os.Stderr, "Go diagnostics require a positive GO_TEST_DIAGNOSTIC_CAPTURE_AT timestamp")
		return 1
	}
	directory, err = filepath.Abs(directory)
	if err != nil {
		fmt.Fprintln(os.Stderr, "Go diagnostic directory:", err)
		return 1
	}
	return runMonitored(run, directory, time.UnixMilli(captureAt), func() error {
		return captureStacks(directory)
	})
}

func runMonitored(run func() int, directory string, captureAt time.Time, capture func() error) int {
	if err := os.MkdirAll(directory, 0700); err != nil {
		fmt.Fprintln(os.Stderr, "Creating Go diagnostic directory:", err)
		return 1
	}
	record := func(phase string, code *int) error {
		data, err := json.Marshal(struct {
			Phase    string    `json:"phase"`
			At       time.Time `json:"at"`
			PID      int       `json:"pid"`
			Go       string    `json:"go"`
			ExitCode *int      `json:"exitCode,omitempty"`
		}{phase, time.Now().UTC(), os.Getpid(), runtime.Version(), code})
		if err != nil {
			return err
		}
		return os.WriteFile(filepath.Join(directory, "go-test-process.json"), data, 0600)
	}
	if err := record("tests-started", nil); err != nil {
		fmt.Fprintln(os.Stderr, "Recording Go test startup:", err)
		return 1
	}
	captured := make(chan error, 1)
	timer := time.AfterFunc(time.Until(captureAt), func() {
		err := capture()
		if err != nil {
			fmt.Fprintln(os.Stderr, "Capturing Go goroutines:", err)
		}
		captured <- err
	})
	code := run()
	if !timer.Stop() {
		if err := <-captured; err != nil && code == 0 {
			code = 1
		}
	}
	if err := record("tests-finished", &code); err != nil {
		fmt.Fprintln(os.Stderr, "Recording Go test completion:", err)
		if code == 0 {
			code = 1
		}
	}
	return code
}

func captureStacks(directory string) error {
	// runtime.Stack reports call frames and numeric arguments, not heap contents,
	// RPC payloads, environment variables, or arbitrary test output.
	for size := 64 * 1024; size <= 16*1024*1024; size *= 2 {
		buffer := make([]byte, size)
		n := runtime.Stack(buffer, true)
		if n == len(buffer) {
			continue
		}
		stacks := string(buffer[:n])
		for _, name := range []string{"COPILOT_HMAC_KEY", "GH_TOKEN", "GITHUB_TOKEN", "COPILOT_GITHUB_TOKEN"} {
			if value := os.Getenv(name); value != "" {
				stacks = strings.ReplaceAll(stacks, value, "[REDACTED]")
			}
		}
		return os.WriteFile(filepath.Join(directory, "go-goroutines.txt"), []byte(stacks), 0600)
	}
	return fmt.Errorf("goroutine stacks exceeded the 16 MiB diagnostic limit")
}
