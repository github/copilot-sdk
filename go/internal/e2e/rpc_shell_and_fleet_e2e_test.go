package e2e

import (
	"crypto/rand"
	"encoding/hex"
	"fmt"
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

// Mirrors dotnet/test/RpcShellAndFleetTests.cs (snapshot category "rpc_shell_and_fleet").
func TestRpcShellAndFleetE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should execute shell command", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		markerPath := filepath.Join(ctx.WorkDir, "shell-rpc-"+randomHex(t)+".txt")
		const marker = "copilot-sdk-shell-rpc"

		cwd := ctx.WorkDir
		result, err := session.RPC.Shell.Exec(t.Context(), &rpc.ShellExecRequest{
			Command: writeFileCommand(markerPath, marker),
			Cwd:     &cwd,
		})
		if err != nil {
			t.Fatalf("Failed to call session.shell.exec: %v", err)
		}
		if strings.TrimSpace(result.ProcessID) == "" {
			t.Fatal("Expected non-empty processId from shell.exec")
		}

		waitForFileText(t, markerPath, marker)
	})

	t.Run("should kill shell process", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		var command string
		if runtime.GOOS == "windows" {
			command = "powershell -NoLogo -NoProfile -Command \"Start-Sleep -Seconds 30\""
		} else {
			command = "sleep 30"
		}

		exec, err := session.RPC.Shell.Exec(t.Context(), &rpc.ShellExecRequest{Command: command})
		if err != nil {
			t.Fatalf("Failed to call session.shell.exec: %v", err)
		}
		if strings.TrimSpace(exec.ProcessID) == "" {
			t.Fatal("Expected non-empty processId from shell.exec")
		}

		kill, err := session.RPC.Shell.Kill(t.Context(), &rpc.ShellKillRequest{ProcessID: exec.ProcessID})
		if err != nil {
			t.Fatalf("Failed to call session.shell.kill: %v", err)
		}
		if !kill.Killed {
			t.Errorf("Expected shell.kill to report Killed=true, got %+v", kill)
		}
	})

	t.Run("should start fleet and complete custom tool task", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		markerPath := filepath.Join(ctx.WorkDir, "fleet-rpc-"+randomHex(t)+".txt")
		const marker = "copilot-sdk-fleet-rpc"
		const toolName = "record_fleet_completion"

		type RecordParams struct {
			Content string `json:"content" jsonschema:"Content to record"`
		}
		recordTool := copilot.DefineTool(toolName, "Records completion of the fleet validation task.",
			func(params RecordParams, inv copilot.ToolInvocation) (string, error) {
				if err := os.WriteFile(markerPath, []byte(params.Content), 0644); err != nil {
					return "", err
				}
				return params.Content, nil
			})

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Tools:               []copilot.Tool{recordTool},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		prompt := fmt.Sprintf("Use the %s tool with content '%s', then report that the fleet task is complete.", toolName, marker)
		promptCopy := prompt

		fleet, err := session.RPC.Fleet.Start(t.Context(), &rpc.FleetStartRequest{Prompt: &promptCopy})
		if err != nil {
			t.Fatalf("Failed to call session.fleet.start: %v", err)
		}
		if !fleet.Started {
			t.Fatal("Expected fleet.start to report Started=true")
		}

		waitForFileText(t, markerPath, marker)

		// Fleet-mode tasks do not emit SessionIdleEvent; poll session messages until the
		// assistant reply contains the expected text.
		messages := waitForFleetCompletion(t, session, "fleet task")

		var sawUser, sawAssistant bool
		var sawToolStart, sawToolComplete bool
		for _, evt := range messages {
			switch d := evt.Data.(type) {
			case *copilot.UserMessageData:
				if strings.Contains(d.Content, prompt) {
					sawUser = true
				}
			case *copilot.AssistantMessageData:
				if strings.Contains(strings.ToLower(d.Content), "fleet task") {
					sawAssistant = true
				}
			case *copilot.ToolExecutionStartData:
				if d.ToolName == toolName {
					sawToolStart = true
				}
			case *copilot.ToolExecutionCompleteData:
				if d.Success && d.Result != nil && strings.Contains(d.Result.Content, marker) {
					sawToolComplete = true
				}
			}
		}

		if !sawUser {
			t.Errorf("Expected user message containing original prompt; messages: %d", len(messages))
		}
		if !sawAssistant {
			t.Errorf("Expected assistant message containing 'fleet task'")
		}
		if !sawToolStart {
			t.Errorf("Expected ToolExecutionStart for %q", toolName)
		}
		if !sawToolComplete {
			t.Errorf("Expected successful ToolExecutionComplete with content containing %q", marker)
		}
	})
}

func randomHex(t *testing.T) string {
	t.Helper()
	var buf [8]byte
	if _, err := rand.Read(buf[:]); err != nil {
		t.Fatalf("Failed to generate random bytes: %v", err)
	}
	return hex.EncodeToString(buf[:])
}

func writeFileCommand(markerPath, marker string) string {
	if runtime.GOOS == "windows" {
		return fmt.Sprintf("powershell -NoLogo -NoProfile -Command \"Set-Content -LiteralPath '%s' -Value '%s'\"", markerPath, marker)
	}
	return fmt.Sprintf("sh -c \"printf '%%s' '%s' > '%s'\"", marker, markerPath)
}

func waitForFileText(t *testing.T, path, expected string) {
	t.Helper()
	deadline := time.Now().Add(30 * time.Second)
	for time.Now().Before(deadline) {
		if data, err := os.ReadFile(path); err == nil && strings.Contains(string(data), expected) {
			return
		}
		time.Sleep(100 * time.Millisecond)
	}
	t.Fatalf("Timed out waiting for shell command to write %q to %q", expected, path)
}

func waitForFleetCompletion(t *testing.T, session *copilot.Session, contentNeedle string) []copilot.SessionEvent {
	t.Helper()
	deadline := time.Now().Add(120 * time.Second)
	for time.Now().Before(deadline) {
		messages, err := session.GetMessages(t.Context())
		if err == nil {
			for _, evt := range messages {
				if d, ok := evt.Data.(*copilot.AssistantMessageData); ok && strings.Contains(strings.ToLower(d.Content), contentNeedle) {
					return messages
				}
			}
		}
		time.Sleep(250 * time.Millisecond)
	}
	t.Fatal("Timed out waiting for fleet-mode assistant reply")
	return nil
}
