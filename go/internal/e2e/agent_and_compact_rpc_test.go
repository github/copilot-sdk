package e2e

import (
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

func TestAgentSelectionRpc(t *testing.T) {
	cliPath := testharness.CLIPath()
	if cliPath == "" {
		t.Fatal("CLI not found. Run 'npm install' in the nodejs directory first.")
	}

	t.Run("should list available custom agents", func(t *testing.T) {
		client := copilot.NewClient(&copilot.ClientOptions{
			CLIPath:  cliPath,
			UseStdio: copilot.Bool(true),
		})
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Failed to start client: %v", err)
		}

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			CustomAgents: []copilot.CustomAgentConfig{
				{
					Name:        "test-agent",
					DisplayName: "Test Agent",
					Description: "A test agent",
					Prompt:      "You are a test agent.",
				},
				{
					Name:        "another-agent",
					DisplayName: "Another Agent",
					Description: "Another test agent",
					Prompt:      "You are another agent.",
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		result, err := session.RPC.Agent.List(t.Context())
		if err != nil {
			t.Fatalf("Failed to list agents: %v", err)
		}

		if len(result.Agents) != 2 {
			t.Fatalf("Expected 2 agents, got %d", len(result.Agents))
		}
		if result.Agents[0].Name != "test-agent" {
			t.Errorf("Expected first agent name 'test-agent', got %q", result.Agents[0].Name)
		}
		if result.Agents[0].DisplayName != "Test Agent" {
			t.Errorf("Expected first agent displayName 'Test Agent', got %q", result.Agents[0].DisplayName)
		}
		if result.Agents[1].Name != "another-agent" {
			t.Errorf("Expected second agent name 'another-agent', got %q", result.Agents[1].Name)
		}

		if err := client.Stop(); err != nil {
			t.Errorf("Expected no errors on stop, got %v", err)
		}
	})

	t.Run("should return null when no agent is selected", func(t *testing.T) {
		client := copilot.NewClient(&copilot.ClientOptions{
			CLIPath:  cliPath,
			UseStdio: copilot.Bool(true),
		})
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Failed to start client: %v", err)
		}

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			CustomAgents: []copilot.CustomAgentConfig{
				{
					Name:        "test-agent",
					DisplayName: "Test Agent",
					Description: "A test agent",
					Prompt:      "You are a test agent.",
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		result, err := session.RPC.Agent.GetCurrent(t.Context())
		if err != nil {
			t.Fatalf("Failed to get current agent: %v", err)
		}

		if result.Agent != nil {
			t.Errorf("Expected no agent selected, got %v", result.Agent)
		}

		if err := client.Stop(); err != nil {
			t.Errorf("Expected no errors on stop, got %v", err)
		}
	})

	t.Run("should select and get current agent", func(t *testing.T) {
		client := copilot.NewClient(&copilot.ClientOptions{
			CLIPath:  cliPath,
			UseStdio: copilot.Bool(true),
		})
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Failed to start client: %v", err)
		}

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			CustomAgents: []copilot.CustomAgentConfig{
				{
					Name:        "test-agent",
					DisplayName: "Test Agent",
					Description: "A test agent",
					Prompt:      "You are a test agent.",
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// Select the agent
		selectResult, err := session.RPC.Agent.Select(t.Context(), &rpc.SessionAgentSelectParams{Name: "test-agent"})
		if err != nil {
			t.Fatalf("Failed to select agent: %v", err)
		}
		if selectResult.Agent.Name != "test-agent" {
			t.Errorf("Expected selected agent 'test-agent', got %q", selectResult.Agent.Name)
		}
		if selectResult.Agent.DisplayName != "Test Agent" {
			t.Errorf("Expected displayName 'Test Agent', got %q", selectResult.Agent.DisplayName)
		}

		// Verify getCurrent returns the selected agent
		currentResult, err := session.RPC.Agent.GetCurrent(t.Context())
		if err != nil {
			t.Fatalf("Failed to get current agent: %v", err)
		}
		if currentResult.Agent == nil {
			t.Fatal("Expected an agent to be selected")
		}
		if currentResult.Agent.Name != "test-agent" {
			t.Errorf("Expected current agent 'test-agent', got %q", currentResult.Agent.Name)
		}

		if err := client.Stop(); err != nil {
			t.Errorf("Expected no errors on stop, got %v", err)
		}
	})

	t.Run("should deselect current agent", func(t *testing.T) {
		client := copilot.NewClient(&copilot.ClientOptions{
			CLIPath:  cliPath,
			UseStdio: copilot.Bool(true),
		})
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Failed to start client: %v", err)
		}

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			CustomAgents: []copilot.CustomAgentConfig{
				{
					Name:        "test-agent",
					DisplayName: "Test Agent",
					Description: "A test agent",
					Prompt:      "You are a test agent.",
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// Select then deselect
		_, err = session.RPC.Agent.Select(t.Context(), &rpc.SessionAgentSelectParams{Name: "test-agent"})
		if err != nil {
			t.Fatalf("Failed to select agent: %v", err)
		}

		_, err = session.RPC.Agent.Deselect(t.Context())
		if err != nil {
			t.Fatalf("Failed to deselect agent: %v", err)
		}

		// Verify no agent is selected
		currentResult, err := session.RPC.Agent.GetCurrent(t.Context())
		if err != nil {
			t.Fatalf("Failed to get current agent: %v", err)
		}
		if currentResult.Agent != nil {
			t.Errorf("Expected no agent selected after deselect, got %v", currentResult.Agent)
		}

		if err := client.Stop(); err != nil {
			t.Errorf("Expected no errors on stop, got %v", err)
		}
	})

	t.Run("should return empty list when no custom agents configured", func(t *testing.T) {
		client := copilot.NewClient(&copilot.ClientOptions{
			CLIPath:  cliPath,
			UseStdio: copilot.Bool(true),
		})
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Failed to start client: %v", err)
		}

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		result, err := session.RPC.Agent.List(t.Context())
		if err != nil {
			t.Fatalf("Failed to list agents: %v", err)
		}

		if len(result.Agents) != 0 {
			t.Errorf("Expected empty agent list, got %d agents", len(result.Agents))
		}

		if err := client.Stop(); err != nil {
			t.Errorf("Expected no errors on stop, got %v", err)
		}
	})
}

func TestSessionCompactionRpc(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	if err := client.Start(t.Context()); err != nil {
		t.Fatalf("Failed to start client: %v", err)
	}

	t.Run("should compact session history after messages", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// Send a message to create some history
		_, err = session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "What is 2+2?",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		// Compact the session
		result, err := session.RPC.Compaction.Compact(t.Context())
		if err != nil {
			t.Fatalf("Failed to compact session: %v", err)
		}

		// Verify result has expected fields (just check it returned valid data)
		if result == nil {
			t.Fatal("Expected non-nil compact result")
		}
	})
}
