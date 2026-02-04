// Source: getting-started.md:313
package main

import (
	"context"
	"fmt"
	"log"
	"os"

	copilot "github.com/github/copilot-sdk/go"
)

func main() {
	ctx := context.Background()
	client := copilot.NewClient(nil)
	if err := client.Start(ctx); err != nil {
		log.Fatal(err)
	}
	defer client.Stop()

	session, err := client.CreateSession(ctx, &copilot.SessionConfig{
		Model:     "gpt-4.1",
		Streaming: true,
	})
	if err != nil {
		log.Fatal(err)
	}

	// Listen for response chunks
	session.On(func(event copilot.SessionEvent) {
		if event.Type == "assistant.message_delta" {
			fmt.Print(*event.Data.DeltaContent)
		}
		if event.Type == "session.idle" {
			fmt.Println()
		}
	})

	_, err = session.SendAndWait(ctx, copilot.MessageOptions{Prompt: "Tell me a short joke"})
	if err != nil {
		log.Fatal(err)
	}
	os.Exit(0)
}