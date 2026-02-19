package main

import (
	"context"
	"fmt"
	"log"
	"os"

	copilot "github.com/github/copilot-sdk/go"
)

func main() {
	cliUrl := os.Getenv("COPILOT_CLI_URL")
	if cliUrl == "" {
		cliUrl = "localhost:3000"
	}

	client := copilot.NewClient(&copilot.ClientOptions{
		CLIUrl: cliUrl,
	})

	ctx := context.Background()

	// Session 1
	fmt.Println("--- Session 1 ---")
	session1, err := client.CreateSession(ctx, &copilot.SessionConfig{
		Model: "claude-haiku-4.5",
	})
	if err != nil {
		log.Fatal(err)
	}

	response1, err := session1.SendAndWait(ctx, copilot.MessageOptions{
		Prompt: "What is the capital of France?",
	})
	if err != nil {
		log.Fatal(err)
	}

	if response1 != nil && response1.Data.Content != nil {
		fmt.Println(*response1.Data.Content)
	} else {
		log.Fatal("No response content received for session 1")
	}

	session1.Destroy()
	fmt.Println("Session 1 destroyed")
	fmt.Println()

	// Session 2 — tests that the server accepts new sessions
	fmt.Println("--- Session 2 ---")
	session2, err := client.CreateSession(ctx, &copilot.SessionConfig{
		Model: "claude-haiku-4.5",
	})
	if err != nil {
		log.Fatal(err)
	}

	response2, err := session2.SendAndWait(ctx, copilot.MessageOptions{
		Prompt: "What is the capital of France?",
	})
	if err != nil {
		log.Fatal(err)
	}

	if response2 != nil && response2.Data.Content != nil {
		fmt.Println(*response2.Data.Content)
	} else {
		log.Fatal("No response content received for session 2")
	}

	session2.Destroy()
	fmt.Println("Session 2 destroyed")

	fmt.Println("\nReconnect test passed — both sessions completed successfully")
}
