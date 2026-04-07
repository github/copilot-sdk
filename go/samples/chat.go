package main

import (
	"bufio"
	"context"
	"fmt"
	"os"
	"path/filepath"
	"strings"

	"github.com/github/copilot-sdk/go"
)

const blue = "\033[34m"
const reset = "\033[0m"

func main() {
	ctx := context.Background()
	cliPath := filepath.Join("..", "..", "nodejs", "node_modules", "@github", "copilot", "index.js")
	client := copilot.NewClient(&copilot.ClientOptions{CLIPath: cliPath})
	if err := client.Start(ctx); err != nil {
		panic(err)
	}
	defer client.Stop()

	session, err := client.CreateSession(ctx, &copilot.SessionConfig{
		CLIPath:             cliPath,
		OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
	})
	if err != nil {
		panic(err)
	}
	defer session.Disconnect()

	session.On(func(event copilot.SessionEvent) {
		var output string
		switch d := event.Data.(type) {
		case *copilot.AssistantReasoningData:
			output = fmt.Sprintf("[reasoning: %s]", d.Content)
		case *copilot.ToolExecutionStartData:
			output = fmt.Sprintf("[tool: %s]", d.ToolName)
		}
		if output != "" {
			fmt.Printf("%s%s%s\n", blue, output, reset)
		}
	})

	fmt.Println("Chat with Copilot (Ctrl+C to exit)\n")
	scanner := bufio.NewScanner(os.Stdin)

	for {
		fmt.Print("You: ")
		if !scanner.Scan() {
			break
		}
		input := strings.TrimSpace(scanner.Text())
		if input == "" {
			continue
		}
		fmt.Println()

		reply, _ := session.SendAndWait(ctx, copilot.MessageOptions{Prompt: input})
		content := ""
		if reply != nil {
			if d, ok := reply.Data.(*copilot.AssistantMessageData); ok {
				content = d.Content
			}
		}
		fmt.Printf("\nAssistant: %s\n\n", content)
	}
}
