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

	// 1. Create a session
	session, err := client.CreateSession(ctx, &copilot.SessionConfig{
		Model:          "claude-haiku-4.5",
		AvailableTools: []string{},
	})
	if err != nil {
		log.Fatal(err)
	}

	// 2. Send the secret word
	_, err = session.SendAndWait(ctx, copilot.MessageOptions{
		Prompt: "Remember this: the secret word is PINEAPPLE.",
	})
	if err != nil {
		log.Fatal(err)
	}

	// 3. Get the session ID (don't destroy — resume needs the session to persist)
	sessionID := session.SessionID

	// 4. Resume the session with the same ID
	resumed, err := client.ResumeSession(ctx, sessionID)
	if err != nil {
		log.Fatal(err)
	}
	fmt.Println("Session resumed")
	defer resumed.Destroy()

	// 5. Ask for the secret word
	response, err := resumed.SendAndWait(ctx, copilot.MessageOptions{
		Prompt: "What was the secret word I told you?",
	})
	if err != nil {
		log.Fatal(err)
	}

	if response != nil && response.Data.Content != nil {
		fmt.Println(*response.Data.Content)
	}
}
