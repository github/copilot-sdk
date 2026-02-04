// Source: mcp/overview.md:103
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

    // MCPServerConfig is map[string]any for flexibility
    session, err := client.CreateSession(ctx, &copilot.SessionConfig{
        Model: "gpt-5",
        MCPServers: map[string]copilot.MCPServerConfig{
            "my-local-server": {
                "type":    "local",
                "command": "node",
                "args":    []string{"./mcp-server.js"},
                "tools":   []string{"*"},
            },
        },
    })
    if err != nil {
        log.Fatal(err)
    }
    defer session.Destroy()

    // Use the session...
}