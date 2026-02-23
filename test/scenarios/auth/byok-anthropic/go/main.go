package main

import (
	"context"
	"fmt"
	"log"
	"os"

	copilot "github.com/github/copilot-sdk/go"
)

func main() {
	apiKey := os.Getenv("ANTHROPIC_API_KEY")
	if apiKey == "" {
		log.Fatal("Missing ANTHROPIC_API_KEY.")
	}

	baseUrl := os.Getenv("ANTHROPIC_BASE_URL")
	if baseUrl == "" {
		baseUrl = "https://api.anthropic.com"
	}

	model := os.Getenv("ANTHROPIC_MODEL")
	if model == "" {
		model = "claude-sonnet-4-20250514"
	}

	client := copilot.NewClient(&copilot.ClientOptions{})

	ctx := context.Background()
	if err := client.Start(ctx); err != nil {
		log.Fatal(err)
	}
	defer client.Stop()

	session, err := client.CreateSession(ctx, &copilot.SessionConfig{
		Model: model,
		Provider: &copilot.ProviderConfig{
			Type:    "anthropic",
			BaseURL: baseUrl,
			APIKey:  apiKey,
		},
		AvailableTools: []string{},
		SystemMessage: &copilot.SystemMessageConfig{
			Mode:    "replace",
			Content: "You are a helpful assistant. Answer concisely.",
		},
	})
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
}
