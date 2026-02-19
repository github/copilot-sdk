package main

import (
	"context"
	"fmt"
	"log"
	"os"

	copilot "github.com/github/copilot-sdk/go"
)

func main() {
	endpoint := os.Getenv("AZURE_OPENAI_ENDPOINT")
	apiKey := os.Getenv("AZURE_OPENAI_API_KEY")
	if endpoint == "" || apiKey == "" {
		log.Fatal("Required: AZURE_OPENAI_ENDPOINT and AZURE_OPENAI_API_KEY")
	}

	model := os.Getenv("AZURE_OPENAI_MODEL")
	if model == "" {
		model = "claude-haiku-4.5"
	}

	apiVersion := os.Getenv("AZURE_API_VERSION")
	if apiVersion == "" {
		apiVersion = "2024-10-21"
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
			Type:    "azure",
			BaseURL: endpoint,
			APIKey:  apiKey,
			Azure: &copilot.AzureProviderOptions{
				APIVersion: apiVersion,
			},
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
