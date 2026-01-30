package e2e

import (
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/e2e/testharness"
)

func TestClient(t *testing.T) {
	cliPath := testharness.CLIPath()
	if cliPath == "" {
		t.Fatal("CLI not found. Run 'npm install' in the nodejs directory first.")
	}

	t.Run("should start and connect to server using stdio", func(t *testing.T) {
		client := copilot.NewClient(&copilot.ClientOptions{
			CLIPath:  cliPath,
			UseStdio: copilot.Bool(true),
		})
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(); err != nil {
			t.Fatalf("Failed to start client: %v", err)
		}

		if client.GetState() != copilot.StateConnected {
			t.Errorf("Expected state to be 'connected', got %q", client.GetState())
		}

		pong, err := client.Ping("test message")
		if err != nil {
			t.Fatalf("Failed to ping: %v", err)
		}

		if pong.Message != "pong: test message" {
			t.Errorf("Expected pong.message to be 'pong: test message', got %q", pong.Message)
		}

		if pong.Timestamp < 0 {
			t.Errorf("Expected pong.timestamp >= 0, got %d", pong.Timestamp)
		}

		if errs := client.Stop(); len(errs) != 0 {
			t.Errorf("Expected no errors on stop, got %v", errs)
		}

		if client.GetState() != copilot.StateDisconnected {
			t.Errorf("Expected state to be 'disconnected', got %q", client.GetState())
		}
	})

	t.Run("should start and connect to server using tcp", func(t *testing.T) {
		client := copilot.NewClient(&copilot.ClientOptions{
			CLIPath:  cliPath,
			UseStdio: copilot.Bool(false),
		})
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(); err != nil {
			t.Fatalf("Failed to start client: %v", err)
		}

		if client.GetState() != copilot.StateConnected {
			t.Errorf("Expected state to be 'connected', got %q", client.GetState())
		}

		pong, err := client.Ping("test message")
		if err != nil {
			t.Fatalf("Failed to ping: %v", err)
		}

		if pong.Message != "pong: test message" {
			t.Errorf("Expected pong.message to be 'pong: test message', got %q", pong.Message)
		}

		if pong.Timestamp < 0 {
			t.Errorf("Expected pong.timestamp >= 0, got %d", pong.Timestamp)
		}

		if errs := client.Stop(); len(errs) != 0 {
			t.Errorf("Expected no errors on stop, got %v", errs)
		}

		if client.GetState() != copilot.StateDisconnected {
			t.Errorf("Expected state to be 'disconnected', got %q", client.GetState())
		}
	})

	t.Run("should return errors on failed cleanup", func(t *testing.T) {
		client := copilot.NewClient(&copilot.ClientOptions{
			CLIPath: cliPath,
		})
		t.Cleanup(func() { client.ForceStop() })

		_, err := client.CreateSession(nil)
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// Kill the server process to force cleanup to fail
		client.ForceStop()
		time.Sleep(100 * time.Millisecond)

		errs := client.Stop()
		if len(errs) > 0 {
			t.Logf("Got expected errors: %v", errs)
		}

		if client.GetState() != copilot.StateDisconnected {
			t.Errorf("Expected state to be 'disconnected', got %q", client.GetState())
		}
	})

	t.Run("should forceStop without cleanup", func(t *testing.T) {
		client := copilot.NewClient(&copilot.ClientOptions{
			CLIPath: cliPath,
		})
		t.Cleanup(func() { client.ForceStop() })

		_, err := client.CreateSession(nil)
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		client.ForceStop()

		if client.GetState() != copilot.StateDisconnected {
			t.Errorf("Expected state to be 'disconnected', got %q", client.GetState())
		}
	})

	t.Run("should get status with version and protocol info", func(t *testing.T) {
		client := copilot.NewClient(&copilot.ClientOptions{
			CLIPath:  cliPath,
			UseStdio: copilot.Bool(true),
		})
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(); err != nil {
			t.Fatalf("Failed to start client: %v", err)
		}

		status, err := client.GetStatus()
		if err != nil {
			t.Fatalf("Failed to get status: %v", err)
		}

		if status.Version == "" {
			t.Error("Expected status.Version to be non-empty")
		}

		if status.ProtocolVersion < 1 {
			t.Errorf("Expected status.ProtocolVersion >= 1, got %d", status.ProtocolVersion)
		}

		client.Stop()
	})

	t.Run("should get auth status", func(t *testing.T) {
		client := copilot.NewClient(&copilot.ClientOptions{
			CLIPath:  cliPath,
			UseStdio: copilot.Bool(true),
		})
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(); err != nil {
			t.Fatalf("Failed to start client: %v", err)
		}

		authStatus, err := client.GetAuthStatus()
		if err != nil {
			t.Fatalf("Failed to get auth status: %v", err)
		}

		// isAuthenticated is a bool, just verify we got a response
		if authStatus.IsAuthenticated {
			if authStatus.AuthType == nil {
				t.Error("Expected authType to be set when authenticated")
			}
			if authStatus.StatusMessage == nil {
				t.Error("Expected statusMessage to be set when authenticated")
			}
		}

		client.Stop()
	})

	t.Run("should list models when authenticated", func(t *testing.T) {
		client := copilot.NewClient(&copilot.ClientOptions{
			CLIPath:  cliPath,
			UseStdio: copilot.Bool(true),
		})
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(); err != nil {
			t.Fatalf("Failed to start client: %v", err)
		}

		authStatus, err := client.GetAuthStatus()
		if err != nil {
			t.Fatalf("Failed to get auth status: %v", err)
		}

		if !authStatus.IsAuthenticated {
			// Skip if not authenticated - models.list requires auth
			client.Stop()
			return
		}

		models, err := client.ListModels()
		if err != nil {
			t.Fatalf("Failed to list models: %v", err)
		}

		if len(models) > 0 {
			model := models[0]
			if model.ID == "" {
				t.Error("Expected model.ID to be non-empty")
			}
			if model.Name == "" {
				t.Error("Expected model.Name to be non-empty")
			}
		}

		client.Stop()
	})
}
