// Copyright (c) Microsoft Corporation. All rights reserved.

package copilot_test

import (
	"context"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
)

func TestOwnedStdioShutdown(t *testing.T) {
	node, err := exec.LookPath("node")
	if err != nil {
		t.Fatal("shutdown fixture requires Node.js:", err)
	}
	fixture, err := filepath.Abs("../test/harness/stdio-shutdown-runtime.cjs")
	if err != nil {
		t.Fatal(err)
	}

	for _, mode := range []string{"stop", "force", "fallback", "start-failure"} {
		t.Run(mode, func(t *testing.T) {
			directory := t.TempDir()
			marker := filepath.Join(directory, "telemetry.jsonl")
			pidFile := filepath.Join(directory, "runtime.pid")
			client := copilot.NewClient(&copilot.ClientOptions{
				Connection: copilot.StdioConnection{
					Path: node,
					Args: []string{fixture, marker, mode, pidFile},
				},
				UseLoggedInUser: copilot.Bool(false),
			})
			t.Cleanup(client.ForceStop)

			ctx, cancel := context.WithTimeout(t.Context(), 10*time.Second)
			defer cancel()
			err := client.Start(ctx)
			if mode == "start-failure" {
				if err == nil || !strings.Contains(err.Error(), "protocol version") {
					t.Fatalf("expected protocol version failure, got %v", err)
				}
			} else {
				if err != nil {
					t.Fatal("Start failed:", err)
				}
				started := time.Now()
				if mode == "force" {
					runShutdownWithWatchdog(t, func() error {
						client.ForceStop()
						return nil
					})
					if elapsed := time.Since(started); elapsed >= 10*time.Second {
						t.Fatalf("ForceStop waited for graceful timeout: %s", elapsed)
					}
				} else {
					runShutdownWithWatchdog(t, client.Stop)
				}
				if mode == "fallback" && time.Since(started) < 10*time.Second {
					t.Fatal("Stop did not allow the full graceful exit timeout")
				}
			}

			// Force-stop and failed startup kill without waiting for the child to be reaped.
			exitWait := "0"
			if mode == "force" || mode == "start-failure" {
				exitWait = "5000"
			}
			assertShutdownChildExited(t, node, pidFile, exitWait)
			contents, err := os.ReadFile(marker)
			if mode == "force" || mode == "start-failure" {
				if !os.IsNotExist(err) {
					t.Fatalf("forced termination unexpectedly finalized telemetry: %q (error: %v)", contents, err)
				}
			} else if err != nil || string(contents) != "{\"type\":\"span\"}\n" {
				t.Fatalf("Stop returned without EOF cleanup: %q (error: %v)", contents, err)
			}
			runShutdownWithWatchdog(t, client.Stop)
		})
	}
}

func runShutdownWithWatchdog(t *testing.T, stop func() error) {
	t.Helper()
	done := make(chan error, 1)
	go func() { done <- stop() }()
	select {
	case err := <-done:
		if err != nil {
			t.Fatal("shutdown failed:", err)
		}
	case <-time.After(40 * time.Second):
		// Cover shutdown RPC, graceful exit, and forced reap budgets, plus scheduling slack.
		t.Fatal("shutdown exceeded all cleanup budgets")
	}
}

func assertShutdownChildExited(t *testing.T, node, pidFile, waitMillis string) {
	t.Helper()
	ctx, cancel := context.WithTimeout(t.Context(), 10*time.Second)
	defer cancel()
	// Node's process probe is portable, unlike os.Process.Signal(0) on Windows.
	cmd := exec.CommandContext(ctx, node, "-e", `
const fs = require("node:fs");
const pid = Number(fs.readFileSync(process.argv[1], "utf8"));
const deadline = Date.now() + Number(process.argv[2]);
function check() {
    try {
        process.kill(pid, 0);
    } catch (error) {
        if (error.code === "ESRCH") return;
        throw error;
    }
    if (Date.now() >= deadline) throw new Error("Child still running");
    setTimeout(check, 25);
}
check();
`, pidFile, waitMillis)
	if output, err := cmd.CombinedOutput(); err != nil {
		t.Fatalf("child process did not exit: %v\n%s", err, output)
	}
}
