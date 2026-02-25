package main

import (
	"context"
	"fmt"
	"log"
	"os"

	copilot "github.com/github/copilot-sdk/go"
)

type GrepParams struct {
	Query string `json:"query" jsonschema:"Search query"`
}

func main() {
	client := copilot.NewClient(&copilot.ClientOptions{
		GitHubToken: os.Getenv("GITHUB_TOKEN"),
	})

	ctx := context.Background()
	if err := client.Start(ctx); err != nil {
		log.Fatal(err)
	}
	defer client.Stop()

	session, err := client.CreateSession(ctx, &copilot.SessionConfig{
		Model:              "claude-haiku-4.5",
		OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		Tools: []copilot.Tool{
			copilot.DefineTool("grep", "A custom grep implementation that overrides the built-in",
				func(params GrepParams, inv copilot.ToolInvocation) (string, error) {
					return "CUSTOM_GREP_RESULT: " + params.Query, nil
				}),
		},
	})
	if err != nil {
		log.Fatal(err)
	}
	defer session.Destroy()

	response, err := session.SendAndWait(ctx, copilot.MessageOptions{
		Prompt: "Use grep to search for the word 'hello'",
	})
	if err != nil {
		log.Fatal(err)
	}

	if response != nil && response.Data.Content != nil {
		fmt.Println(*response.Data.Content)
	}
}
