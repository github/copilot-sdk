package testharness

import (
	"bytes"
	"encoding/json"
	"os"
	"os/exec"
	"reflect"
	"strings"
	"testing"
)

const isolationHelperScenarioEnv = "COPILOT_SDK_ISOLATION_HELPER_SCENARIO"

func TestMain(m *testing.M) {
	if os.Getenv(isolationHelperScenarioEnv) != "" {
		os.Exit(RunWithInProcessIsolation(m))
	}
	os.Exit(m.Run())
}

func TestIsolatedTestArgs(t *testing.T) {
	args := []string{
		"-test.run=original",
		"-test.count", "3",
		"-test.gocoverdir=coverage",
		"-test.coverprofile=parent.out",
	}
	got := isolatedTestArgs(args, `^TestExample$/^case_\[1\]$`, false, 0)
	want := []string{
		"-test.gocoverdir=coverage",
		`-test.run=^TestExample$/^case_\[1\]$`,
		"-test.count=1",
		"-test.v=true",
	}
	if !reflect.DeepEqual(got, want) {
		t.Fatalf("Unexpected isolated arguments: %v", got)
	}
	if len(args) != 5 || args[0] != "-test.run=original" {
		t.Fatal("Parent test arguments were modified")
	}
}

func TestExactTestSelector(t *testing.T) {
	got := exactTestSelector("TestExample/case_[1]")
	want := `^TestExample$/^case_\[1\]$`
	if got != want {
		t.Fatalf("Unexpected selector: got %q, want %q", got, want)
	}
}

func TestRunInIsolatedProcess(t *testing.T) {
	const isolatedTest = "TestRunInIsolatedProcess/isolated"
	if scenario := os.Getenv(isolationHelperScenarioEnv); scenario != "" {
		t.Run("isolated", func(t *testing.T) {
			if RunInIsolatedProcess(t) {
				return
			}
			if !IsInProcessTransport() || os.Getenv(isolatedInProcessTestEnv) != t.Name() {
				t.Fatal("Isolated child did not retain the in-process transport")
			}
			if scenario == "failure" {
				t.Fatal("intentional isolated failure")
			}
			if scenario == "skip" {
				t.Skip("intentional isolated skip")
			}
			t.Log("isolated assertions executed")
		})
		return
	}

	executable, err := os.Executable()
	if err != nil {
		t.Fatal(err)
	}
	for _, scenario := range []string{"success", "failure", "skip"} {
		t.Run(scenario, func(t *testing.T) {
			command := exec.CommandContext(t.Context(), executable, isolatedTestArgs(os.Args[1:], "^TestRunInIsolatedProcess$", false, 0)...)
			command.Env = append(os.Environ(),
				"COPILOT_SDK_DEFAULT_CONNECTION=inprocess",
				isolatedInProcessTestEnv+"=",
				isolationHelperScenarioEnv+"="+scenario,
			)
			output, err := command.CombinedOutput()
			if scenario == "failure" {
				if err == nil || !strings.Contains(string(output), "intentional isolated failure") {
					t.Fatalf("Isolated failure was not propagated: %v\n%s", err, output)
				}
			} else if scenario == "skip" {
				if err != nil || !strings.Contains(string(output), "--- SKIP: "+isolatedTest) {
					t.Fatalf("Isolated skip was not preserved: %v\n%s", err, output)
				}
			} else if err != nil || !strings.Contains(string(output), "isolated assertions executed") {
				t.Fatalf("Isolated assertions did not pass: %v\n%s", err, output)
			}

			converter := exec.CommandContext(t.Context(), "go", "tool", "test2json")
			converter.Stdin = bytes.NewReader(output)
			events, err := converter.CombinedOutput()
			if err != nil {
				t.Fatalf("Convert isolated test output to JSON: %v\n%s", err, events)
			}
			var outcomes []string
			for _, line := range bytes.Split(events, []byte("\n")) {
				if len(line) == 0 {
					continue
				}
				var event struct {
					Action string
					Test   string
				}
				if err := json.Unmarshal(line, &event); err != nil {
					t.Fatalf("Decode test event: %v\n%s", err, line)
				}
				if event.Test == isolatedTest &&
					(event.Action == "pass" || event.Action == "fail" || event.Action == "skip") {
					outcomes = append(outcomes, event.Action)
				}
			}
			wantOutcome := map[string]string{"success": "pass", "failure": "fail", "skip": "skip"}[scenario]
			if !reflect.DeepEqual(outcomes, []string{wantOutcome}) {
				t.Fatalf("Expected one %s outcome, got %v\n%s", wantOutcome, outcomes, events)
			}
		})
	}
}

func TestInitializeInProcessEnvironmentRejectsChanges(t *testing.T) {
	originalCwd, err := os.Getwd()
	if err != nil {
		t.Fatal(err)
	}
	workDir := t.TempDir()
	t.Cleanup(func() {
		if err := os.Chdir(originalCwd); err != nil {
			t.Errorf("Restore working directory: %v", err)
		}
		inProcessEnvironment.Lock()
		inProcessEnvironment.initialized = false
		inProcessEnvironment.values = nil
		inProcessEnvironment.workDir = ""
		inProcessEnvironment.Unlock()
	})

	context := &TestContext{CLIPath: os.Args[0]}
	env := append(os.Environ(), "COPILOT_SDK_ISOLATION_TEST_VALUE=first")
	context.initializeInProcessEnvironment(env, workDir)
	context.initializeInProcessEnvironment(env, workDir)

	defer func() {
		if recover() == nil {
			t.Error("Changing the environment after initialization must fail")
		}
		if os.Getenv("COPILOT_SDK_ISOLATION_TEST_VALUE") != "first" {
			t.Error("Initialized process environment was changed")
		}
	}()
	context.initializeInProcessEnvironment(
		append(os.Environ(), "COPILOT_SDK_ISOLATION_TEST_VALUE=second"),
		workDir,
	)
}
