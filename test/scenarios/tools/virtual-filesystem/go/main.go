package main

import (
	"context"
	"fmt"
	"log"
	"os"
	"strings"
	"sync"

	copilot "github.com/github/copilot-sdk/go"
)

// In-memory virtual filesystem
var (
	virtualFs   = make(map[string]string)
	virtualFsMu sync.Mutex
)

type CreateFileArgs struct {
	Path    string `json:"path" description:"File path"`
	Content string `json:"content" description:"File content"`
}

type ReadFileArgs struct {
	Path string `json:"path" description:"File path"`
}

func main() {
	createFile := copilot.DefineTool[CreateFileArgs, string](
		"create_file",
		"Create or overwrite a file at the given path with the provided content",
		func(args CreateFileArgs, inv copilot.ToolInvocation) (string, error) {
			virtualFsMu.Lock()
			virtualFs[args.Path] = args.Content
			virtualFsMu.Unlock()
			return fmt.Sprintf("Created %s (%d bytes)", args.Path, len(args.Content)), nil
		},
	)

	readFile := copilot.DefineTool[ReadFileArgs, string](
		"read_file",
		"Read the contents of a file at the given path",
		func(args ReadFileArgs, inv copilot.ToolInvocation) (string, error) {
			virtualFsMu.Lock()
			content, ok := virtualFs[args.Path]
			virtualFsMu.Unlock()
			if !ok {
				return fmt.Sprintf("Error: file not found: %s", args.Path), nil
			}
			return content, nil
		},
	)

	listFiles := copilot.Tool{
		Name:        "list_files",
		Description: "List all files in the virtual filesystem",
		Parameters: map[string]any{
			"type":       "object",
			"properties": map[string]any{},
		},
		Handler: func(inv copilot.ToolInvocation) (copilot.ToolResult, error) {
			virtualFsMu.Lock()
			defer virtualFsMu.Unlock()
			if len(virtualFs) == 0 {
				return copilot.ToolResult{TextResultForLLM: "No files"}, nil
			}
			paths := make([]string, 0, len(virtualFs))
			for p := range virtualFs {
				paths = append(paths, p)
			}
			return copilot.ToolResult{TextResultForLLM: strings.Join(paths, "\n")}, nil
		},
	}

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
		// Remove all built-in tools — only our custom virtual FS tools are available
		AvailableTools: []string{},
		Tools:          []copilot.Tool{createFile, readFile, listFiles},
		OnPermissionRequest: func(req copilot.PermissionRequest, inv copilot.PermissionInvocation) (copilot.PermissionRequestResult, error) {
			return copilot.PermissionRequestResult{Kind: "approved"}, nil
		},
		Hooks: &copilot.SessionHooks{
			OnPreToolUse: func(input copilot.PreToolUseHookInput, inv copilot.HookInvocation) (*copilot.PreToolUseHookOutput, error) {
				return &copilot.PreToolUseHookOutput{PermissionDecision: "allow"}, nil
			},
		},
	})
	if err != nil {
		log.Fatal(err)
	}
	defer session.Destroy()

	response, err := session.SendAndWait(ctx, copilot.MessageOptions{
		Prompt: "Create a file called plan.md with a brief 3-item project plan " +
			"for building a CLI tool. Then read it back and tell me what you wrote.",
	})
	if err != nil {
		log.Fatal(err)
	}

	if response != nil && response.Data.Content != nil {
		fmt.Println(*response.Data.Content)
	}

	// Dump the virtual filesystem to prove nothing touched disk
	fmt.Println("\n--- Virtual filesystem contents ---")
	for path, content := range virtualFs {
		fmt.Printf("\n[%s]\n", path)
		fmt.Println(content)
	}
}
