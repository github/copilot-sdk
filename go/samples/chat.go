package main

import (
	"bufio"
	"context"
	"encoding/json"
	"flag"
	"fmt"
	"net"
	"os"
	"strings"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
	"github.com/github/copilot-sdk/go/samples/internal/sampleutil"
)

const blue = "\033[34m"
const reset = "\033[0m"

func main() {
	envelopeExample := flag.Bool("permission-envelope", false, "Print a lossless permission request without starting the CLI")
	eventExample := flag.Bool("permission-envelope-events", false, "Demonstrate lossless permission events with an isolated JSON-RPC peer")
	reorderExample := flag.Bool("permission-envelope-reorder", false, "Reorder original command JSON without losing unknown fields")
	copyEditExample := flag.Bool("permission-envelope-copy-edit", false, "Copy and edit original command JSON without losing unknown fields")
	combinedExample := flag.Bool("permission-envelope-mcp-refresh", false, "Demonstrate both callbacks with an isolated JSON-RPC peer")
	recoveryExample := flag.Bool("permission-envelope-recovery", false, "Demonstrate lossless permissions, Auto recovery, and MCP auth with an isolated JSON-RPC peer")
	flag.Parse()
	if *eventExample {
		showCombinedPermissionHandlers(false, true)
		return
	}
	if *reorderExample || *copyEditExample {
		showPermissionEnvelopeArrayEdit(*copyEditExample)
		return
	}
	if *recoveryExample {
		showCombinedPermissionHandlers(true, false)
		return
	}
	if *combinedExample {
		showCombinedPermissionHandlers(false, false)
		return
	}
	if *envelopeExample {
		showPermissionEnvelope()
		return
	}
	ctx := context.Background()
	cliPath, err := sampleutil.CLIPath()
	if err != nil {
		panic(err)
	}

	client := copilot.NewClient(&copilot.ClientOptions{Connection: copilot.StdioConnection{Path: cliPath}})
	if err := client.Start(ctx); err != nil {
		panic(err)
	}
	defer client.Stop()

	session, err := client.CreateSession(ctx, &copilot.SessionConfig{
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

	fmt.Println("Chat with Copilot (Ctrl+C to exit)")
	fmt.Println()
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

func showPermissionEnvelope() {
	var envelope copilot.PermissionRequestEnvelope
	input := `{"kind":"read","intention":"Read a file","path":"/workspace/original","sandboxPathGrant":{"access":"read","path":"/workspace/original","deniedPath":null,"futureGrant":9007199254740993},"futureDetails":true}`
	if err := json.Unmarshal([]byte(input), &envelope); err != nil {
		panic(err)
	}
	request := envelope.Request.(*copilot.PermissionRequestRead)
	request.Path = "/workspace/changed"
	request.SandboxPathGrant.Path = "/workspace/changed"
	data, err := json.Marshal(envelope)
	if err != nil {
		panic(err)
	}
	fmt.Println(string(data))
}

func showPermissionEnvelopeArrayEdit(copyAndEdit bool) {
	input := `{"kind":"shell","commands":[{"identifier":"first","readOnly":true,"futurePolicy":"first-policy"},{"identifier":"second","readOnly":true,"futurePolicy":{"id":9007199254740993,"nullable":null}}]}`
	var source copilot.PermissionRequestEnvelope
	if err := json.Unmarshal([]byte(input), &source); err != nil {
		panic(err)
	}
	original, err := json.Marshal(source)
	if err != nil {
		panic(err)
	}
	var fields map[string]json.RawMessage
	if err := json.Unmarshal(original, &fields); err != nil {
		panic(err)
	}
	var commands []json.RawMessage
	if err := json.Unmarshal(fields["commands"], &commands); err != nil {
		panic(err)
	}
	if copyAndEdit {
		var command map[string]json.RawMessage
		if err := json.Unmarshal(commands[1], &command); err != nil {
			panic(err)
		}
		command["readOnly"] = json.RawMessage(`false`)
		commands[0], err = json.Marshal(command)
		if err != nil {
			panic(err)
		}
	} else {
		commands[0], commands[1] = commands[1], commands[0]
	}
	fields["commands"], err = json.Marshal(commands)
	if err != nil {
		panic(err)
	}
	reordered, err := json.Marshal(fields)
	if err != nil {
		panic(err)
	}
	var envelope copilot.PermissionRequestEnvelope
	if err := json.Unmarshal(reordered, &envelope); err != nil {
		panic(err)
	}
	data, err := json.Marshal(envelope)
	if err != nil {
		panic(err)
	}
	fmt.Println(string(data))
}

func showCombinedPermissionHandlers(recovery, events bool) {
	ctx := context.Background()
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		panic(err)
	}
	defer listener.Close()
	type peer struct {
		server *jsonrpc2.Client
		conn   net.Conn
	}
	ready := make(chan peer, 1)
	authResult := make(chan json.RawMessage, 1)
	go func() {
		conn, err := listener.Accept()
		if err != nil {
			panic(err)
		}
		server := jsonrpc2.NewClient(conn, conn)
		for method, result := range map[string]string{
			"connect":                           `{"ok":true,"protocolVersion":3,"version":"example"}`,
			"plugins.builtin.set":               `{}`,
			"session.create":                    `{"sessionId":"combined-example","capabilities":{"ui":{"autoModeRecovery":true}}}`,
			"session.eventLog.registerInterest": `{}`,
			"session.options.update":            `{"success":true}`,
		} {
			server.SetRequestHandler(method, func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				return json.RawMessage(result), nil
			})
		}
		server.SetRequestHandler("session.mcp.oauth.handlePendingRequest", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			authResult <- params
			return json.RawMessage(`{}`), nil
		})
		server.Start()
		ready <- peer{server, conn}
	}()
	client := copilot.NewClient(&copilot.ClientOptions{Connection: copilot.URIConnection{URL: listener.Addr().String()}})
	if err := client.Start(ctx); err != nil {
		panic(err)
	}
	transport := <-ready
	defer func() {
		client.ForceStop()
		transport.server.Stop()
	}()
	permissionResult := make(chan json.RawMessage, 1)
	permissionHandler := func(request *copilot.PermissionRequestEnvelope, _ copilot.PermissionHandlerInvocation) (rpc.PermissionDecision, error) {
		data, err := json.Marshal(request)
		permissionResult <- data
		return &rpc.PermissionDecisionNoResult{}, err
	}
	if events {
		permissionHandler = nil
	}
	refreshHandler := func(request copilot.MCPAuthRequest, _ copilot.MCPAuthInvocation, supported bool) (*copilot.MCPAuthResult, error) {
		if supported && request.Reason == copilot.MCPOauthRequestReasonRefresh {
			return copilot.MCPAuthResultRuntimeRefresh(), nil
		}
		return copilot.MCPAuthResultCancelled(), nil
	}
	config := &copilot.SessionConfig{SessionID: "combined-example"}
	var session *copilot.Session
	if recovery {
		session, err = client.CreateSessionWithPermissionEnvelopeAndAutoModeRecovery(ctx, config, permissionHandler, &copilot.AutoModeRecoveryOptions{
			OnAutoModeSwitchRequest: func(copilot.AutoModeSwitchRequestWithAutoModeRecovery, copilot.AutoModeSwitchInvocation) (copilot.AutoModeSwitchResponse, error) {
				return copilot.AutoModeSwitchResponseNo, nil
			},
			MCPAuthRuntimeRefreshHandler: refreshHandler,
		})
	} else {
		session, err = client.CreateSessionWithPermissionEnvelopeAndMCPAuthRuntimeRefresh(ctx, config, permissionHandler, refreshHandler)
	}
	if err != nil {
		panic(err)
	}
	if events {
		unsubscribe := session.OnWithPermissionEnvelope(func(event copilot.SessionEvent) {
			if data, ok := event.Data.(*copilot.PermissionRequestedDataWithEnvelope); ok {
				payload, err := json.Marshal(data.Envelope)
				if err != nil {
					panic(err)
				}
				permissionResult <- payload
			}
		})
		defer unsubscribe()
	}
	for _, event := range []map[string]any{
		{"type": "permission.requested", "data": map[string]any{
			"requestId": "permission-1", "permissionRequest": json.RawMessage(`{"kind":"read","intention":"Read a file","path":"/workspace/file.txt","futureDetails":9007199254740993}`),
		}},
		{"type": "mcp.oauth_required", "data": map[string]any{
			"requestId": "auth-1", "serverName": "oauth-server", "serverUrl": "https://example.com/mcp",
			"reason": "refresh", "runtimeRefreshSupported": true,
		}},
	} {
		body, err := json.Marshal(map[string]any{
			"jsonrpc": "2.0", "method": "session.event",
			"params": map[string]any{"sessionId": session.SessionID, "event": event},
		})
		if err != nil {
			panic(err)
		}
		if _, err := fmt.Fprintf(transport.conn, "Content-Length: %d\r\n\r\n%s", len(body), body); err != nil {
			panic(err)
		}
	}
	fmt.Println("Permission envelope:", string(<-permissionResult))
	fmt.Println("MCP response:", string(<-authResult))
	if recovery {
		response, err := transport.server.Request(ctx, "autoModeSwitch.request", map[string]any{
			"sessionId": session.SessionID,
			"autoModeRecovery": map[string]any{
				"requestId": "recovery-1", "originalModel": "example-model", "allowAlways": false,
			},
		})
		if err != nil {
			panic(err)
		}
		fmt.Println("Recovery response:", string(response))
	}
}
