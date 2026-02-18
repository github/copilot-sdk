package main

import (
	"context"
	"fmt"
	"log"
	"os"
	"path/filepath"
	"runtime"

	copilot "github.com/github/copilot-sdk/go"
)

func main() {
	client := copilot.NewClient(&copilot.ClientOptions{
		GithubToken: os.Getenv("GITHUB_TOKEN"),
	})

	ctx := context.Background()
	if err := client.Start(ctx); err != nil {
		log.Fatal(err)
	}
	defer client.Stop()

	_, thisFile, _, _ := runtime.Caller(0)
	skillsDir := filepath.Join(filepath.Dir(thisFile), "..", "sample-skills")

	session, err := client.CreateSession(ctx, &copilot.SessionConfig{
		Model:            "gpt-4.1",
		SkillDirectories: []string{skillsDir},
	})
	if err != nil {
		log.Fatal(err)
	}
	defer session.Destroy()

	response, err := session.SendAndWait(ctx, copilot.MessageOptions{
		Prompt: "Use the greeting skill to greet someone named Alice.",
	})
	if err != nil {
		log.Fatal(err)
	}

	if response != nil && response.Data.Content != nil {
		fmt.Println(*response.Data.Content)
	}

	fmt.Println("\nSkill directories configured successfully")
}
