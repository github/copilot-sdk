package main

import (
	"context"
	"fmt"
	"log"
	"os"

	copilot "github.com/github/copilot-sdk/go"
)

func boolPtr(b bool) *bool       { return &b }
func float64Ptr(f float64) *float64 { return &f }

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
		Model:          "claude-haiku-4.5",
		AvailableTools: []string{},
		SystemMessage: &copilot.SystemMessageConfig{
			Mode:    "replace",
			Content: "You are a helpful assistant. Answer concisely in one sentence.",
		},
		InfiniteSessions: &copilot.InfiniteSessionConfig{
			Enabled:                       boolPtr(true),
			BackgroundCompactionThreshold: float64Ptr(0.80),
			BufferExhaustionThreshold:     float64Ptr(0.95),
		},
	})
	if err != nil {
		log.Fatal(err)
	}
	defer session.Destroy()

	prompts := []string{
		"What is the capital of France?",
		"What is the capital of Japan?",
		"What is the capital of Brazil?",
	}

	for _, prompt := range prompts {
		response, err := session.SendAndWait(ctx, copilot.MessageOptions{
			Prompt: prompt,
		})
		if err != nil {
			log.Fatal(err)
		}
		if response != nil && response.Data.Content != nil {
			fmt.Printf("Q: %s\n", prompt)
			fmt.Printf("A: %s\n\n", *response.Data.Content)
		}
	}

	fmt.Println("Infinite sessions test complete — all messages processed successfully")
}
