package main

import (
	"context"
	"fmt"
	"log"
	"os"
	"sync"

	copilot "github.com/github/copilot-sdk/go"
)

const piratePrompt = `You are a pirate. Always say Arrr!`
const robotPrompt = `You are a robot. Always say BEEP BOOP!`

func main() {
	client := copilot.NewClient(&copilot.ClientOptions{
		GitHubToken: os.Getenv("GITHUB_TOKEN"),
	})

	ctx := context.Background()
	if err := client.Start(ctx); err != nil {
		log.Fatal(err)
	}
	defer client.Stop()

	session1, err := client.CreateSession(ctx, &copilot.SessionConfig{
		Model: "claude-haiku-4.5",
		SystemMessage: &copilot.SystemMessageConfig{
			Mode:    "replace",
			Content: piratePrompt,
		},
		AvailableTools: []string{},
	})
	if err != nil {
		log.Fatal(err)
	}
	defer session1.Destroy()

	session2, err := client.CreateSession(ctx, &copilot.SessionConfig{
		Model: "claude-haiku-4.5",
		SystemMessage: &copilot.SystemMessageConfig{
			Mode:    "replace",
			Content: robotPrompt,
		},
		AvailableTools: []string{},
	})
	if err != nil {
		log.Fatal(err)
	}
	defer session2.Destroy()

	type result struct {
		label   string
		content string
	}

	var wg sync.WaitGroup
	results := make([]result, 2)

	wg.Add(2)
	go func() {
		defer wg.Done()
		resp, err := session1.SendAndWait(ctx, copilot.MessageOptions{
			Prompt: "What is the capital of France?",
		})
		if err != nil {
			log.Fatal(err)
		}
		if resp != nil && resp.Data.Content != nil {
			results[0] = result{label: "Session 1 (pirate)", content: *resp.Data.Content}
		}
	}()
	go func() {
		defer wg.Done()
		resp, err := session2.SendAndWait(ctx, copilot.MessageOptions{
			Prompt: "What is the capital of France?",
		})
		if err != nil {
			log.Fatal(err)
		}
		if resp != nil && resp.Data.Content != nil {
			results[1] = result{label: "Session 2 (robot)", content: *resp.Data.Content}
		}
	}()
	wg.Wait()

	for _, r := range results {
		if r.label != "" {
			fmt.Printf("%s: %s\n", r.label, r.content)
		}
	}
}
