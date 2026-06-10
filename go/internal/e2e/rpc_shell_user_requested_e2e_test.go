package e2e

import (
	"fmt"
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"testing"
	"time"

	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

func TestRpcShellUserRequested(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should_execute_user_requested_shell_command", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		session := createPortedSession(t, client, nil)
		defer session.Disconnect()
		marker := "copilotusershell" + randomHex(t)
		requestID := "req-" + randomHex(t)

		result, err := session.RPC.Shell.ExecuteUserRequested(t.Context(), &rpc.ShellExecuteUserRequestedRequest{
			RequestID: requestID,
			Command:   "echo " + marker,
		})
		if err != nil {
			t.Fatalf("Shell.ExecuteUserRequested failed: %v", err)
		}
		if !result.Success {
			t.Fatalf("Expected shell command to succeed, got error %v", result.Error)
		}
		if result.ExitCode == nil || *result.ExitCode != 0 {
			t.Fatalf("Expected exit code 0, got %v", result.ExitCode)
		}
		if !strings.Contains(result.Output, marker) {
			t.Fatalf("Expected output to contain %q, got %q", marker, result.Output)
		}
		if strings.TrimSpace(result.ToolCallID) == "" {
			t.Fatal("Expected non-empty tool call ID")
		}
	})

	t.Run("should_cancel_user_requested_shell_command", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		session := createPortedSession(t, client, nil)
		defer session.Disconnect()

		missing, err := session.RPC.Shell.CancelUserRequested(t.Context(), &rpc.ShellCancelUserRequestedRequest{RequestID: "missing-" + randomHex(t)})
		if err != nil {
			t.Fatalf("Shell.CancelUserRequested(missing) failed: %v", err)
		}
		if missing.Cancelled {
			t.Fatal("Expected cancelling an unknown request to return Cancelled=false")
		}

		requestID := "req-" + randomHex(t)
		markerPath := filepath.Join(os.TempDir(), "shell-cancel-"+randomHex(t)+".txt")
		defer tryRemovePortedFile(markerPath)

		type executeResult struct {
			result *rpc.UserRequestedShellCommandResult
			err    error
		}
		executeCh := make(chan executeResult, 1)
		execDone := false
		go func() {
			result, err := session.RPC.Shell.ExecuteUserRequested(t.Context(), &rpc.ShellExecuteUserRequestedRequest{
				RequestID: requestID,
				Command:   createPortedMarkerThenSleepCommand(markerPath, 60),
			})
			executeCh <- executeResult{result: result, err: err}
		}()
		defer func() {
			if execDone {
				return
			}
			_, _ = session.RPC.Shell.CancelUserRequested(t.Context(), &rpc.ShellCancelUserRequestedRequest{RequestID: requestID})
			select {
			case <-executeCh:
			case <-time.After(30 * time.Second):
			}
		}()

		waitForRPCCondition(t, 30*time.Second, "user-requested shell marker file", func() (bool, error) {
			_, err := os.Stat(markerPath)
			if err == nil {
				return true, nil
			}
			if os.IsNotExist(err) {
				return false, nil
			}
			return false, err
		})

		waitForRPCCondition(t, 15*time.Second, "user-requested shell command to become cancellable", func() (bool, error) {
			cancel, err := session.RPC.Shell.CancelUserRequested(t.Context(), &rpc.ShellCancelUserRequestedRequest{RequestID: requestID})
			if err != nil {
				return false, err
			}
			return cancel.Cancelled, nil
		})

		select {
		case execution := <-executeCh:
			execDone = true
			if execution.err != nil {
				t.Fatalf("ExecuteUserRequested returned error after cancellation: %v", execution.err)
			}
			if execution.result == nil {
				t.Fatal("Expected execution result after cancellation")
			}
			if execution.result.Success {
				t.Fatalf("Expected cancelled execution to be unsuccessful, got %+v", execution.result)
			}
		case <-time.After(30 * time.Second):
			t.Fatal("Timed out waiting for cancelled user-requested shell command to finish")
		}
	})
}

func createPortedMarkerThenSleepCommand(markerPath string, seconds int) string {
	if runtime.GOOS == "windows" {
		escaped := strings.ReplaceAll(markerPath, "'", "''")
		return fmt.Sprintf("Set-Content -LiteralPath '%s' -Value 'running'; Start-Sleep -Seconds %d", escaped, seconds)
	}
	escaped := strings.ReplaceAll(markerPath, "'", "'\\''")
	return fmt.Sprintf("echo running > '%s'; sleep %d", escaped, seconds)
}

func tryRemovePortedFile(path string) {
	if err := os.Remove(path); err != nil && !os.IsNotExist(err) {
		_ = err
	}
}
