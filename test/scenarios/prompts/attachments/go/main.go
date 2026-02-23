package main

import (
	"context"
	"fmt"
	"log"
	"os"
	"path/filepath"

	copilot "github.com/github/copilot-sdk/go"
)

const systemPrompt = `You are a helpful assistant. Answer questions about attached files concisely.`

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
		Model: "claude-haiku-4.5",
		SystemMessage: &copilot.SystemMessageConfig{
			Mode:    "replace",
			Content: systemPrompt,
		},
		AvailableTools: []string{},
	})
	if err != nil {
		log.Fatal(err)
	}
	defer session.Destroy()

	exe, err := os.Executable()
	if err != nil {
		log.Fatal(err)
	}
	sampleFile := filepath.Join(filepath.Dir(exe), "..", "sample-data.txt")
	sampleFile, err = filepath.Abs(sampleFile)
	if err != nil {
		log.Fatal(err)
	}

	response, err := session.SendAndWait(ctx, copilot.MessageOptions{
		Prompt: "What languages are listed in the attached file?",
		Attachments: []copilot.Attachment{
			{Type: "file", Path: &sampleFile},
		},
	})
	if err != nil {
		log.Fatal(err)
	}

	if response != nil && response.Data.Content != nil {
		fmt.Println(*response.Data.Content)
	}
}
