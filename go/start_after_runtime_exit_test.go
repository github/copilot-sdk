// Copyright (c) Microsoft Corporation. All rights reserved.

package copilot

import (
	"bufio"
	"encoding/json"
	"fmt"
	"io"
	"net/textproto"
	"os"
	"os/exec"
	"path/filepath"
	"strconv"
	"strings"
	"testing"
	"time"
)

const (
	startAfterExitRoleEnv  = "GO_START_AFTER_EXIT_ROLE"
	startAfterExitDirEnv   = "GO_START_AFTER_EXIT_DIR"
	startAfterExitStateEnv = "GO_START_AFTER_EXIT_STATE"
)

// TestStartAfterRuntimeExitHelper is the stand-in runtime and the process it
// leaves behind; it does nothing when the test binary runs normally.
func TestStartAfterRuntimeExitHelper(t *testing.T) {
	dir := os.Getenv(startAfterExitDirEnv)
	switch os.Getenv(startAfterExitRoleEnv) {
	case "runtime":
		os.Exit(runStartAfterExitRuntime(dir, os.Getenv(startAfterExitStateEnv)))
	case "descendant":
		// Hold the inherited output until the test releases it, then report
		// that this process is about to exit.
		waitForStartAfterExitFile(dir, "release")
		_ = os.WriteFile(filepath.Join(dir, "descendant-done"), nil, 0o600)
		os.Exit(0)
	}
}

// waitForStartAfterExitFile reports whether the test created name in dir
// within 30 seconds.
func waitForStartAfterExitFile(dir, name string) bool {
	deadline := time.Now().Add(30 * time.Second)
	for time.Now().Before(deadline) {
		if _, err := os.Stat(filepath.Join(dir, name)); err == nil {
			return true
		}
		time.Sleep(20 * time.Millisecond)
	}
	return false
}

// runStartAfterExitRuntime answers JSON-RPC requests on stdio. The first
// instance answers its first ping, waits until the test has received that
// reply, then does what state asks of it; later instances keep serving.
func runStartAfterExitRuntime(dir, state string) int {
	instance := 1
	for ; ; instance++ {
		file, err := os.OpenFile(filepath.Join(dir, "instance-"+strconv.Itoa(instance)), os.O_CREATE|os.O_EXCL, 0o600)
		if err == nil {
			file.Close()
			break
		}
	}
	reader := textproto.NewReader(bufio.NewReader(os.Stdin))
	for {
		header, err := reader.ReadMIMEHeader()
		if err != nil {
			return 0
		}
		length, err := strconv.Atoi(header.Get("Content-Length"))
		if err != nil {
			return 2
		}
		body := make([]byte, length)
		if _, err := io.ReadFull(reader.R, body); err != nil {
			return 0
		}
		var request struct {
			ID     json.RawMessage `json:"id"`
			Method string          `json:"method"`
		}
		if err := json.Unmarshal(body, &request); err != nil {
			return 2
		}
		if request.ID == nil {
			continue
		}
		var result any = map[string]any{}
		switch request.Method {
		case "connect":
			result = map[string]any{"ok": true, "protocolVersion": SDKProtocolVersion, "version": "test"}
		case "ping":
			result = map[string]any{"message": fmt.Sprintf("pong %d", instance), "timestamp": time.Now()}
		}
		response, _ := json.Marshal(map[string]any{"jsonrpc": "2.0", "id": request.ID, "result": result})
		fmt.Fprintf(os.Stdout, "Content-Length: %d\r\n\r\n%s", len(response), response)
		if request.Method != "ping" || instance != 1 || state == "running" {
			continue
		}
		if !waitForStartAfterExitFile(dir, "exit") {
			return 2
		}
		if state != "exited" {
			descendant := exec.Command(os.Args[0], "-test.run=^TestStartAfterRuntimeExitHelper$")
			descendant.Env = append(os.Environ(), startAfterExitRoleEnv+"=descendant")
			descendant.Stdout = os.Stdout
			if state == "exited, a process it started holds its stdout and stderr" {
				descendant.Stderr = os.Stderr
			}
			if err := descendant.Start(); err != nil {
				return 2
			}
		}
		return 3
	}
}

// Start must start a new stdio runtime once the one it started has exited, also
// when a process that runtime started still holds its output, and keep a
// runtime that is still running.
func TestStartAfterRuntimeExit(t *testing.T) {
	for _, tc := range []struct {
		state string
		want  string
	}{
		{state: "running", want: "pong 1"},
		{state: "exited", want: "pong 2"},
		{state: "exited, a process it started holds its stdout", want: "pong 2"},
		{state: "exited, a process it started holds its stdout and stderr", want: "pong 2"},
	} {
		t.Run(tc.state, func(t *testing.T) {
			dir := t.TempDir()
			// Runs before the directory is removed. Wait for the process the
			// stand-in runtime started to exit: it runs the test binary, which
			// Windows cannot delete while it is running.
			t.Cleanup(func() {
				if err := os.WriteFile(filepath.Join(dir, "release"), nil, 0o600); err != nil {
					t.Error(err)
					return
				}
				if strings.Contains(tc.state, "holds") && !waitForStartAfterExitFile(dir, "descendant-done") {
					t.Error("the process the stand-in runtime started did not exit within 30s")
				}
			})
			client := NewClient(&ClientOptions{
				Connection: StdioConnection{
					Path: os.Args[0],
					Args: []string{"-test.run=^TestStartAfterRuntimeExitHelper$", "--"},
					Env: append(os.Environ(),
						startAfterExitRoleEnv+"=runtime",
						startAfterExitDirEnv+"="+dir,
						startAfterExitStateEnv+"="+tc.state,
					),
				},
			})
			t.Cleanup(client.ForceStop)
			ctx := t.Context()

			if err := client.Start(ctx); err != nil {
				t.Fatal("first Start:", err)
			}
			first := client.processDone
			if response, err := client.Ping(ctx, "first"); err != nil || response.Message != "pong 1" {
				t.Fatalf("first ping = %v, %v; want pong 1", response, err)
			}
			// Exit only once the reply has arrived, so the first ping cannot
			// race the exit.
			if err := os.WriteFile(filepath.Join(dir, "exit"), nil, 0o600); err != nil {
				t.Fatal(err)
			}
			if tc.state != "running" {
				select {
				case <-first:
				case <-time.After(5 * time.Second):
					t.Fatal("the client did not observe the runtime exit within 5s")
				}
			}

			if err := client.Start(ctx); err != nil {
				t.Fatal("Start after the first runtime:", err)
			}
			response, err := client.Ping(ctx, "second")
			if err != nil || response.Message != tc.want {
				t.Fatalf("ping after Start = %v, %v; want %s", response, err, tc.want)
			}
		})
	}
}
