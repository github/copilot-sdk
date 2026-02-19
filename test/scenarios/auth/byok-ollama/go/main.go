package main

import (
	"context"
	"fmt"
	"log"
	"os"

	copilot "github.com/github/copilot-sdk/go"
)

const compactSystemPrompt = "You are a compact local assistant. Keep answers short, concrete, and under 80 words."

func main() {
	baseUrl := os.Getenv("OLLAMA_BASE_URL")
	if baseUrl == "" {
		baseUrl = "http://localhost:11434/v1"
	}

	model := os.Getenv("OLLAMA_MODEL")
	if model == "" {
		model = "llama3.2:3b"
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
			Type:    "openai",
			BaseURL: baseUrl,
		},
		AvailableTools: []string{},
		SystemMessage: &copilot.SystemMessageConfig{
			Mode:    "replace",
			Content: compactSystemPrompt,
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
