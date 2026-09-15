package testharness

import (
	"os"
	"os/exec"
	"reflect"
	"strings"
	"testing"
)

func TestIsolatedTestArgs(t *testing.T) {
	args := []string{"-test.run=original", "-test.count=3", "-test.gocoverdir=coverage"}
	got := isolatedTestArgs(args, "TestExample/case_[1]")
	want := []string{
		"-test.run=original", "-test.count=3", "-test.gocoverdir=coverage",
		`-test.run=^TestExample$/^case_\[1\]$`, "-test.count=1", "-test.v=true",
		"-test.coverprofile=", "-test.testlogfile=",
	}
	if !reflect.DeepEqual(got, want) {
		t.Fatalf("Unexpected isolated arguments: %v", got)
	}
	if len(args) != 3 || args[0] != "-test.run=original" {
		t.Fatal("Parent test arguments were modified")
	}
}

func TestRunWithInProcessGlobals(t *testing.T) {
	const scenarioEnv = "COPILOT_SDK_ISOLATION_HELPER_SCENARIO"
	if scenario := os.Getenv(scenarioEnv); scenario != "" {
		if RunWithInProcessGlobals(t) {
			return
		}
		if !IsInProcessTransport() || os.Getenv(isolatedInProcessTestEnv) != t.Name() {
			t.Fatal("Isolated child did not retain the in-process transport")
		}
		if scenario == "failure" {
			t.Fatal("intentional isolated failure")
		}
		t.Log("isolated assertions executed")
		return
	}

	executable, err := os.Executable()
	if err != nil {
		t.Fatal(err)
	}
	for _, scenario := range []string{"success", "failure"} {
		t.Run(scenario, func(t *testing.T) {
			command := exec.CommandContext(t.Context(), executable, isolatedTestArgs(os.Args[1:], "TestRunWithInProcessGlobals")...)
			command.Env = append(os.Environ(),
				"COPILOT_SDK_DEFAULT_CONNECTION=inprocess",
				"COPILOT_CLI_PATH="+executable,
				isolatedInProcessTestEnv+"=",
				scenarioEnv+"="+scenario,
			)
			output, err := command.CombinedOutput()
			if scenario == "failure" {
				if err == nil || !strings.Contains(string(output), "intentional isolated failure") {
					t.Fatalf("Isolated failure was not propagated: %v\n%s", err, output)
				}
			} else if err != nil || !strings.Contains(string(output), "isolated assertions executed") {
				t.Fatalf("Isolated assertions did not pass: %v\n%s", err, output)
			}
		})
	}
}

func TestInitializeInProcessGlobals(t *testing.T) {
	t.Setenv(isolatedInProcessTestEnv, t.Name())
	t.Setenv("COPILOT_ALLOW_GET_PROVIDER_ENDPOINT", "false")
	inProcessGlobals.Lock()
	inProcessGlobals.initialized = false
	inProcessGlobals.Unlock()
	t.Cleanup(func() {
		inProcessGlobals.Lock()
		inProcessGlobals.initialized = false
		inProcessGlobals.Unlock()
	})

	initializeInProcessGlobals([]string{"COPILOT_ALLOW_GET_PROVIDER_ENDPOINT=true"})
	if os.Getenv("COPILOT_ALLOW_GET_PROVIDER_ENDPOINT") != "true" {
		t.Fatal("Legacy option was not initialized before native startup")
	}
	initializeInProcessGlobals([]string{"COPILOT_ALLOW_GET_PROVIDER_ENDPOINT=true"})
	defer func() {
		if recover() == nil {
			t.Error("Changing a legacy option after initialization must fail")
		}
		if os.Getenv("COPILOT_ALLOW_GET_PROVIDER_ENDPOINT") != "true" {
			t.Error("Initialized process environment was changed")
		}
	}()
	initializeInProcessGlobals([]string{"COPILOT_ALLOW_GET_PROVIDER_ENDPOINT=false"})
}
