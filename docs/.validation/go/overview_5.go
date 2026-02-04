// Source: hooks/overview.md:87
package main

import (
    "context"
    "fmt"
    copilot "github.com/github/copilot-sdk/go"
)

func main() {
    client := copilot.NewClient(nil)

    session, _ := client.CreateSession(context.Background(), &copilot.SessionConfig{
        Hooks: &copilot.SessionHooks{
            OnPreToolUse: func(input copilot.PreToolUseHookInput, inv copilot.HookInvocation) (*copilot.PreToolUseHookOutput, error) {
                fmt.Printf("Tool called: %s\n", input.ToolName)
                return &copilot.PreToolUseHookOutput{
                    PermissionDecision: "allow",
                }, nil
            },
            OnPostToolUse: func(input copilot.PostToolUseHookInput, inv copilot.HookInvocation) (*copilot.PostToolUseHookOutput, error) {
                fmt.Printf("Tool result: %v\n", input.ToolResult)
                return nil, nil
            },
            OnSessionStart: func(input copilot.SessionStartHookInput, inv copilot.HookInvocation) (*copilot.SessionStartHookOutput, error) {
                return &copilot.SessionStartHookOutput{
                    AdditionalContext: "User prefers concise answers.",
                }, nil
            },
        },
    })
    _ = session
}