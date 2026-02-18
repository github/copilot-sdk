package main

import (
	"context"
	"fmt"
	"log"
	"os"
	"os/exec"
	"strings"

	copilot "github.com/github/copilot-sdk/go"
)

func resolveToken() (string, string) {
	if t := os.Getenv("COPILOT_GITHUB_TOKEN"); t != "" {
		return t, "COPILOT_GITHUB_TOKEN"
	}
	if t := os.Getenv("GH_TOKEN"); t != "" {
		return t, "GH_TOKEN"
	}
	if t := os.Getenv("GITHUB_TOKEN"); t != "" {
		return t, "GITHUB_TOKEN"
	}
	out, err := exec.Command("gh", "auth", "token").Output()
	if err == nil {
		token := strings.TrimSpace(string(out))
		if token != "" {
			return token, "gh CLI"
		}
	}
	return "", ""
}

func main() {
	token, source := resolveToken()
	fmt.Printf("Token source resolved: %s\n", source)

	client := copilot.NewClient(&copilot.ClientOptions{
		GithubToken: token,
	})

	ctx := context.Background()
	if err := client.Start(ctx); err != nil {
		log.Fatal(err)
	}
	defer client.Stop()

	session, err := client.CreateSession(ctx, &copilot.SessionConfig{
		Model:          "gpt-4.1",
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

	fmt.Println("\nAuth test passed — token resolved successfully")
}
