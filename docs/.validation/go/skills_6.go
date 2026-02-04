// Source: guides/skills.md:74
package main

import (
    "context"
    "log"
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
        Model: "gpt-4.1",
        SkillDirectories: []string{
            "./skills/code-review",
            "./skills/documentation",
            "~/.copilot/skills",  // User-level skills
        },
    })
    if err != nil {
        log.Fatal(err)
    }

    // Copilot now has access to skills in those directories
    _, err = session.SendAndWait(ctx, copilot.MessageOptions{
        Prompt: "Review this code for security issues",
    })
    if err != nil {
        log.Fatal(err)
    }
}