package e2e

import (
	"strings"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/e2e/testharness"
)

func TestMCPServers(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("accept MCP server config on create", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		mcpServers := map[string]copilot.MCPServerConfig{
			"test-server": {
				"type":    "local",
				"command": "echo",
				"args":    []string{"hello"},
				"tools":   []string{"*"},
			},
		}

		session, err := client.CreateSession(&copilot.SessionConfig{
			MCPServers: mcpServers,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		if session.SessionID == "" {
			t.Error("Expected non-empty session ID")
		}

		// Simple interaction to verify session works
		_, err = session.Send(copilot.MessageOptions{
			Prompt: "What is 2+2?",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		message, err := testharness.GetFinalAssistantMessage(session, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to get final message: %v", err)
		}

		if message.Data.Content == nil || !strings.Contains(*message.Data.Content, "4") {
			t.Errorf("Expected message to contain '4', got: %v", message.Data.Content)
		}

		session.Destroy()
	})

	t.Run("accept MCP server config on resume", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		// Create a session first
		session1, err := client.CreateSession(nil)
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		sessionID := session1.SessionID

		_, err = session1.SendAndWait(copilot.MessageOptions{Prompt: "What is 1+1?"}, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		// Resume with MCP servers
		mcpServers := map[string]copilot.MCPServerConfig{
			"test-server": {
				"type":    "local",
				"command": "echo",
				"args":    []string{"hello"},
				"tools":   []string{"*"},
			},
		}

		session2, err := client.ResumeSessionWithOptions(sessionID, &copilot.ResumeSessionConfig{
			MCPServers: mcpServers,
		})
		if err != nil {
			t.Fatalf("Failed to resume session: %v", err)
		}

		if session2.SessionID != sessionID {
			t.Errorf("Expected session ID %s, got %s", sessionID, session2.SessionID)
		}

		message, err := session2.SendAndWait(copilot.MessageOptions{Prompt: "What is 3+3?"}, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		if message.Data.Content == nil || !strings.Contains(*message.Data.Content, "6") {
			t.Errorf("Expected message to contain '6', got: %v", message.Data.Content)
		}

		session2.Destroy()
	})

	t.Run("handle multiple MCP servers", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		mcpServers := map[string]copilot.MCPServerConfig{
			"server1": {
				"type":    "local",
				"command": "echo",
				"args":    []string{"server1"},
				"tools":   []string{"*"},
			},
			"server2": {
				"type":    "local",
				"command": "echo",
				"args":    []string{"server2"},
				"tools":   []string{"*"},
			},
		}

		session, err := client.CreateSession(&copilot.SessionConfig{
			MCPServers: mcpServers,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		if session.SessionID == "" {
			t.Error("Expected non-empty session ID")
		}

		session.Destroy()
	})
}

func TestCustomAgents(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("accept custom agent config on create", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		infer := true
		customAgents := []copilot.CustomAgentConfig{
			{
				Name:        "test-agent",
				DisplayName: "Test Agent",
				Description: "A test agent for SDK testing",
				Prompt:      "You are a helpful test agent.",
				Infer:       &infer,
			},
		}

		session, err := client.CreateSession(&copilot.SessionConfig{
			CustomAgents: customAgents,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		if session.SessionID == "" {
			t.Error("Expected non-empty session ID")
		}

		// Simple interaction to verify session works
		_, err = session.Send(copilot.MessageOptions{
			Prompt: "What is 5+5?",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		message, err := testharness.GetFinalAssistantMessage(session, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to get final message: %v", err)
		}

		if message.Data.Content == nil || !strings.Contains(*message.Data.Content, "10") {
			t.Errorf("Expected message to contain '10', got: %v", message.Data.Content)
		}

		session.Destroy()
	})

	t.Run("accept custom agent config on resume", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		// Create a session first
		session1, err := client.CreateSession(nil)
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		sessionID := session1.SessionID

		_, err = session1.SendAndWait(copilot.MessageOptions{Prompt: "What is 1+1?"}, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		// Resume with custom agents
		customAgents := []copilot.CustomAgentConfig{
			{
				Name:        "resume-agent",
				DisplayName: "Resume Agent",
				Description: "An agent added on resume",
				Prompt:      "You are a resume test agent.",
			},
		}

		session2, err := client.ResumeSessionWithOptions(sessionID, &copilot.ResumeSessionConfig{
			CustomAgents: customAgents,
		})
		if err != nil {
			t.Fatalf("Failed to resume session: %v", err)
		}

		if session2.SessionID != sessionID {
			t.Errorf("Expected session ID %s, got %s", sessionID, session2.SessionID)
		}

		message, err := session2.SendAndWait(copilot.MessageOptions{Prompt: "What is 6+6?"}, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		if message.Data.Content == nil || !strings.Contains(*message.Data.Content, "12") {
			t.Errorf("Expected message to contain '12', got: %v", message.Data.Content)
		}

		session2.Destroy()
	})

	t.Run("handle custom agent with tools", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		infer := true
		customAgents := []copilot.CustomAgentConfig{
			{
				Name:        "tool-agent",
				DisplayName: "Tool Agent",
				Description: "An agent with specific tools",
				Prompt:      "You are an agent with specific tools.",
				Tools:       []string{"bash", "edit"},
				Infer:       &infer,
			},
		}

		session, err := client.CreateSession(&copilot.SessionConfig{
			CustomAgents: customAgents,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		if session.SessionID == "" {
			t.Error("Expected non-empty session ID")
		}

		session.Destroy()
	})

	t.Run("handle custom agent with MCP servers", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		customAgents := []copilot.CustomAgentConfig{
			{
				Name:        "mcp-agent",
				DisplayName: "MCP Agent",
				Description: "An agent with its own MCP servers",
				Prompt:      "You are an agent with MCP servers.",
				MCPServers: map[string]copilot.MCPServerConfig{
					"agent-server": {
						"type":    "local",
						"command": "echo",
						"args":    []string{"agent-mcp"},
						"tools":   []string{"*"},
					},
				},
			},
		}

		session, err := client.CreateSession(&copilot.SessionConfig{
			CustomAgents: customAgents,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		if session.SessionID == "" {
			t.Error("Expected non-empty session ID")
		}

		session.Destroy()
	})

	t.Run("handle multiple custom agents", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		inferTrue := true
		inferFalse := false
		customAgents := []copilot.CustomAgentConfig{
			{
				Name:        "agent1",
				DisplayName: "Agent One",
				Description: "First agent",
				Prompt:      "You are agent one.",
				Infer:       &inferTrue,
			},
			{
				Name:        "agent2",
				DisplayName: "Agent Two",
				Description: "Second agent",
				Prompt:      "You are agent two.",
				Infer:       &inferFalse,
			},
		}

		session, err := client.CreateSession(&copilot.SessionConfig{
			CustomAgents: customAgents,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		if session.SessionID == "" {
			t.Error("Expected non-empty session ID")
		}

		session.Destroy()
	})
}

func TestCombinedConfiguration(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("accept MCP servers and custom agents", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		mcpServers := map[string]copilot.MCPServerConfig{
			"shared-server": {
				"type":    "local",
				"command": "echo",
				"args":    []string{"shared"},
				"tools":   []string{"*"},
			},
		}

		customAgents := []copilot.CustomAgentConfig{
			{
				Name:        "combined-agent",
				DisplayName: "Combined Agent",
				Description: "An agent using shared MCP servers",
				Prompt:      "You are a combined test agent.",
			},
		}

		session, err := client.CreateSession(&copilot.SessionConfig{
			MCPServers:   mcpServers,
			CustomAgents: customAgents,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		if session.SessionID == "" {
			t.Error("Expected non-empty session ID")
		}

		_, err = session.Send(copilot.MessageOptions{
			Prompt: "What is 7+7?",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		message, err := testharness.GetFinalAssistantMessage(session, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to get final message: %v", err)
		}

		if message.Data.Content == nil || !strings.Contains(*message.Data.Content, "14") {
			t.Errorf("Expected message to contain '14', got: %v", message.Data.Content)
		}

		session.Destroy()
	})
}
