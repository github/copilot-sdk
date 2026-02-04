// Source: auth/byok.md:96
package main

import (
    "context"
    "fmt"
    "os"
    copilot "github.com/github/copilot-sdk/go"
)

func main() {
    ctx := context.Background()
    client := copilot.NewClient(nil)
    if err := client.Start(ctx); err != nil {
        panic(err)
    }
    defer client.Stop()

    session, err := client.CreateSession(ctx, &copilot.SessionConfig{
        Model: "gpt-5.2-codex",  // Your deployment name
        Provider: &copilot.ProviderConfig{
            Type:    "openai",
            BaseURL: "https://your-resource.openai.azure.com/openai/v1/",
            WireApi: "responses",  // Use "completions" for older models
            APIKey:  os.Getenv("FOUNDRY_API_KEY"),
        },
    })
    if err != nil {
        panic(err)
    }

    response, err := session.SendAndWait(ctx, copilot.MessageOptions{
        Prompt: "What is 2+2?",
    })
    if err != nil {
        panic(err)
    }

    fmt.Println(*response.Data.Content)
}