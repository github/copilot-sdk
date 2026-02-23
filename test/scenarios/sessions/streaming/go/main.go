package main

import (
	"context"
	"fmt"
	"log"
	"os"

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

	session, err := client.CreateSession(ctx, &copilot.SessionConfig{
		Model:     "claude-haiku-4.5",
		Streaming: true,
	})
	if err != nil {
		log.Fatal(err)
	}
	defer session.Destroy()

	chunkCount := 0
	session.On(func(event copilot.SessionEvent) {
		if event.Type == "assistant.message_delta" {
			chunkCount++
		}
	})

	response, err := session.SendAndWait(ctx, copilot.MessageOptions{
		Prompt: "What is the capital of France?",
	})
	if err != nil {
		log.Fatal(err)
	}

	if response != nil && response.Data.Content != nil {
		fmt.Println(*response.Data.Content)
	}
	fmt.Printf("\nStreaming chunks received: %d\n", chunkCount)
}
