package main

import (
	"context"
	"fmt"
	"log"
	"os"
	"strings"

	copilot "github.com/github/copilot-sdk/go"
)

func main() {
	client := copilot.NewClient(&copilot.ClientOptions{
		GitHubToken: os.Getenv("GITHUB_TOKEN"),
	})

	ctx := context.Background()
	if err := client.Start(ctx); err != nil {
		log.Fatal(err)
	}
	defer client.Stop()

	// MCP server config — demonstrates the configuration pattern.
	// When MCP_SERVER_CMD is set, connects to a real MCP server.
	// Otherwise, runs without MCP tools as a build/integration test.
	mcpServers := map[string]copilot.MCPServerConfig{}
	if cmd := os.Getenv("MCP_SERVER_CMD"); cmd != "" {
		var args []string
		if argsStr := os.Getenv("MCP_SERVER_ARGS"); argsStr != "" {
			args = strings.Split(argsStr, " ")
		}
		mcpServers["example"] = copilot.MCPServerConfig{
			"type":    "stdio",
			"command": cmd,
			"args":    args,
		}
	}

	sessionConfig := &copilot.SessionConfig{
		Model: "claude-haiku-4.5",
		SystemMessage: &copilot.SystemMessageConfig{
			Mode:    "replace",
			Content: "You are a helpful assistant. Answer questions concisely.",
		},
		AvailableTools: []string{},
	}
	if len(mcpServers) > 0 {
		sessionConfig.MCPServers = mcpServers
	}

	session, err := client.CreateSession(ctx, sessionConfig)
	if err != nil {
		log.Fatal(err)
	}
	defer session.Destroy()

	response, err := session.SendAndWait(ctx, copilot.MessageOptions{
		Prompt: "What is the capital of France?",
	})
	if err != nil {
		log.Fatal(err)
	}

	if response != nil && response.Data.Content != nil {
		fmt.Println(*response.Data.Content)
	}

	if len(mcpServers) > 0 {
		keys := make([]string, 0, len(mcpServers))
		for k := range mcpServers {
			keys = append(keys, k)
		}
		fmt.Printf("\nMCP servers configured: %s\n", strings.Join(keys, ", "))
	} else {
		fmt.Println("\nNo MCP servers configured (set MCP_SERVER_CMD to test with a real server)")
	}
}
