package testdiagnostics

import (
	"encoding/json"
	"errors"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"
)

func TestDisabledDiagnosticsPreserveResult(t *testing.T) {
	t.Setenv("GO_TEST_DIAGNOSTIC_DIRECTORY", "")
	if code := Run(func() int { return 37 }); code != 37 {
		t.Fatalf("Run returned %d, want 37", code)
	}
}

func TestInvalidConfigurationFailsBeforeTests(t *testing.T) {
	for _, timestamp := range []string{"", "invalid", "0", "-1"} {
		t.Run(timestamp, func(t *testing.T) {
			t.Setenv("GO_TEST_DIAGNOSTIC_DIRECTORY", t.TempDir())
			t.Setenv("GO_TEST_DIAGNOSTIC_CAPTURE_AT", timestamp)
			called := false
			code := Run(func() int { called = true; return 0 })
			if code != 1 || called {
				t.Fatalf("code=%d, called=%v; invalid configuration must fail", code, called)
			}
		})
	}
}

func TestConfiguredDiagnosticsPreserveResult(t *testing.T) {
	directory := t.TempDir()
	t.Setenv("GO_TEST_DIAGNOSTIC_DIRECTORY", directory)
	t.Setenv("GO_TEST_DIAGNOSTIC_CAPTURE_AT", "4102444800000")
	if code := Run(func() int { return 37 }); code != 37 {
		t.Fatalf("configured Run returned %d, want 37", code)
	}
	if _, err := os.Stat(filepath.Join(directory, "go-test-process.json")); err != nil {
		t.Fatalf("configured Run did not write process state: %v", err)
	}
}

func TestCompletionPreservesExitAndCancelsCapture(t *testing.T) {
	for _, expected := range []int{0, 37} {
		directory := t.TempDir()
		code := runMonitored(func() int { return expected }, directory, time.Now().Add(time.Hour), func() error {
			t.Error("capture ran after normal completion")
			return nil
		})
		data, err := os.ReadFile(filepath.Join(directory, "go-test-process.json"))
		if err != nil {
			t.Fatal(err)
		}
		var state struct {
			Phase    string `json:"phase"`
			ExitCode int    `json:"exitCode"`
		}
		if err := json.Unmarshal(data, &state); err != nil {
			t.Fatal(err)
		}
		if code != expected || state.ExitCode != expected || state.Phase != "tests-finished" {
			t.Fatalf("code=%d, state=%+v, want exit %d", code, state, expected)
		}
		if _, err := os.Stat(filepath.Join(directory, "go-goroutines.txt")); !errors.Is(err, os.ErrNotExist) {
			t.Fatalf("unexpected stack artifact: %v", err)
		}
	}
}

func TestCaptureRunsBeforeTestCleanup(t *testing.T) {
	directory := t.TempDir()
	captured := make(chan struct{})
	code := runMonitored(func() int {
		<-captured
		return 37
	}, directory, time.Now(), func() error {
		defer close(captured)
		if err := captureStacks(directory); err != nil {
			return err
		}
		data, err := os.ReadFile(filepath.Join(directory, "go-test-process.json"))
		if err == nil && !strings.Contains(string(data), `"phase":"tests-started"`) {
			t.Errorf("capture did not precede cleanup: %s", data)
		}
		return err
	})
	if code != 37 {
		t.Fatalf("capture changed first failure to %d", code)
	}
	data, err := os.ReadFile(filepath.Join(directory, "go-goroutines.txt"))
	if err != nil {
		t.Fatal(err)
	}
	if !strings.Contains(string(data), ".TestCaptureRunsBeforeTestCleanup.func1(") {
		t.Fatalf("timed capture did not include the blocked test runner: %s", data)
	}
}

func TestCaptureFailureIsNotSuccess(t *testing.T) {
	for _, expected := range []int{0, 37} {
		captured := make(chan struct{})
		code := runMonitored(func() int { <-captured; return expected }, t.TempDir(), time.Now(), func() error {
			defer close(captured)
			return errors.New("controlled capture failure")
		})
		want := expected
		if want == 0 {
			want = 1
		}
		if code != want {
			t.Fatalf("code=%d, want %d", code, want)
		}
	}
}

func diagnosticBlockedRoutine(ready chan<- struct{}, release <-chan struct{}, done chan<- struct{}) {
	close(ready)
	<-release
	close(done)
}

func TestCaptureIncludesBlockedGoroutine(t *testing.T) {
	directory := t.TempDir()
	ready, release, done := make(chan struct{}), make(chan struct{}), make(chan struct{})
	go diagnosticBlockedRoutine(ready, release, done)
	<-ready
	defer func() { close(release); <-done }()
	if err := captureStacks(directory); err != nil {
		t.Fatal(err)
	}
	data, err := os.ReadFile(filepath.Join(directory, "go-goroutines.txt"))
	if err != nil {
		t.Fatal(err)
	}
	if !strings.Contains(string(data), ".diagnosticBlockedRoutine(") || !strings.Contains(string(data), "[chan receive]") {
		t.Fatalf("missing blocked goroutine in stack capture: %s", data)
	}
}
