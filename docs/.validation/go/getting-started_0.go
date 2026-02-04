// Source: getting-started.md:161
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

	session, err := client.CreateSession(ctx, &copilot.SessionConfig{Model: "gpt-4.1"})
	if err != nil {
		log.Fatal(err)
	}

	response, err := session.SendAndWait(ctx, copilot.MessageOptions{Prompt: "What is 2 + 2?"})
	if err != nil {
		log.Fatal(err)
	}

	fmt.Println(*response.Data.Content)
	os.Exit(0)
}