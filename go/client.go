// Package copilot provides a Go SDK for interacting with the GitHub Copilot CLI.
//
// The copilot package enables Go applications to communicate with the Copilot CLI
// server, create and manage conversation sessions, and integrate custom tools.
//
// Basic usage:
//
//	client := copilot.NewClient(nil)
//	if err := client.Start(); err != nil {
//	    log.Fatal(err)
//	}
//	defer client.Stop()
//
//	session, err := client.CreateSession(&copilot.SessionConfig{
//	    Model: "gpt-4",
//	})
//	if err != nil {
//	    log.Fatal(err)
//	}
//
//	session.On(func(event copilot.SessionEvent) {
//	    if event.Type == "assistant.message" {
//	        fmt.Println(event.Data.Content)
//	    }
//	})
//
//	session.Send(copilot.MessageOptions{Prompt: "Hello!"})
package copilot

import (
	"bufio"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net"
	"os"
	"os/exec"
	"regexp"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
)

// Client manages the connection to the Copilot CLI server and provides session management.
//
// The Client can either spawn a CLI server process or connect to an existing server.
// It handles JSON-RPC communication, session lifecycle, tool execution, and permission requests.
//
// Example:
//
//	// Create a client with default options (spawns CLI server using stdio)
//	client := copilot.NewClient(nil)
//
//	// Or connect to an existing server
//	client := copilot.NewClient(&copilot.ClientOptions{
//	    CLIUrl: "localhost:3000",
//	})
//
//	if err := client.Start(); err != nil {
//	    log.Fatal(err)
//	}
//	defer client.Stop()
type Client struct {
	options                ClientOptions
	process                *exec.Cmd
	client                 *jsonrpc2.Client
	actualPort             int
	actualHost             string
	state                  ConnectionState
	sessions               map[string]*Session
	sessionsMux            sync.Mutex
	isExternalServer       bool
	conn                   net.Conn // stores net.Conn for external TCP connections
	useStdio               bool     // resolved value from options
	autoStart              bool     // resolved value from options
	autoRestart            bool     // resolved value from options
	modelsCache            []ModelInfo
	modelsCacheMux         sync.Mutex
	lifecycleHandlers      []SessionLifecycleHandler
	typedLifecycleHandlers map[SessionLifecycleEventType][]SessionLifecycleHandler
	lifecycleHandlersMux   sync.Mutex
}

// NewClient creates a new Copilot CLI client with the given options.
//
// If options is nil, default options are used (spawns CLI server using stdio).
// The client is not connected after creation; call [Client.Start] to connect.
//
// Example:
//
//	// Default options
//	client := copilot.NewClient(nil)
//
//	// Custom options
//	client := copilot.NewClient(&copilot.ClientOptions{
//	    CLIPath:  "/usr/local/bin/copilot",
//	    LogLevel: "debug",
//	})
func NewClient(options *ClientOptions) *Client {
	opts := ClientOptions{
		CLIPath:  "copilot",
		Cwd:      "",
		Port:     0,
		LogLevel: "info",
	}

	client := &Client{
		options:          opts,
		state:            StateDisconnected,
		sessions:         make(map[string]*Session),
		actualHost:       "localhost",
		isExternalServer: false,
		useStdio:         true,
		autoStart:        true, // default
		autoRestart:      true, // default
	}

	if options != nil {
		// Validate mutually exclusive options
		if options.CLIUrl != "" && ((options.UseStdio != nil) || options.CLIPath != "") {
			panic("CLIUrl is mutually exclusive with UseStdio and CLIPath")
		}

		// Validate auth options with external server
		if options.CLIUrl != "" && (options.GithubToken != "" || options.UseLoggedInUser != nil) {
			panic("GithubToken and UseLoggedInUser cannot be used with CLIUrl (external server manages its own auth)")
		}

		// Parse CLIUrl if provided
		if options.CLIUrl != "" {
			host, port := parseCliUrl(options.CLIUrl)
			client.actualHost = host
			client.actualPort = port
			client.isExternalServer = true
			client.useStdio = false
			opts.CLIUrl = options.CLIUrl
		}

		if options.CLIPath != "" {
			opts.CLIPath = options.CLIPath
		}
		if options.Cwd != "" {
			opts.Cwd = options.Cwd
		}
		if options.Port > 0 {
			opts.Port = options.Port
			// If port is specified, switch to TCP mode
			client.useStdio = false
		}
		if options.LogLevel != "" {
			opts.LogLevel = options.LogLevel
		}
		if options.Env != nil {
			opts.Env = options.Env
		}
		if options.UseStdio != nil {
			client.useStdio = *options.UseStdio
		}
		if options.AutoStart != nil {
			client.autoStart = *options.AutoStart
		}
		if options.AutoRestart != nil {
			client.autoRestart = *options.AutoRestart
		}
		if options.GithubToken != "" {
			opts.GithubToken = options.GithubToken
		}
		if options.UseLoggedInUser != nil {
			opts.UseLoggedInUser = options.UseLoggedInUser
		}
	}

	// Default Env to current environment if not set
	if opts.Env == nil {
		opts.Env = os.Environ()
	}

	// Check environment variable for CLI path
	if cliPath := os.Getenv("COPILOT_CLI_PATH"); cliPath != "" {
		opts.CLIPath = cliPath
	}

	client.options = opts
	return client
}

// parseCliUrl parses a CLI URL into host and port components.
//
// Supports formats: "host:port", "http://host:port", "https://host:port", or just "port".
// Panics if the URL format is invalid or the port is out of range.
func parseCliUrl(url string) (string, int) {
	// Remove protocol if present
	cleanUrl, _ := strings.CutPrefix(url, "https://")
	cleanUrl, _ = strings.CutPrefix(cleanUrl, "http://")

	// Parse host:port or port format
	var host string
	var portStr string
	if before, after, found := strings.Cut(cleanUrl, ":"); found {
		host = before
		portStr = after
	} else {
		// Only port provided
		portStr = before
	}

	if host == "" {
		host = "localhost"
	}

	// Validate port
	port, err := strconv.Atoi(portStr)
	if err != nil || port <= 0 || port > 65535 {
		panic(fmt.Sprintf("Invalid port in CLIUrl: %s", url))
	}

	return host, port
}

// Start starts the CLI server (if not using an external server) and establishes
// a connection.
//
// If connecting to an external server (via CLIUrl), only establishes the connection.
// Otherwise, spawns the CLI server process and then connects.
//
// This method is called automatically when creating a session if AutoStart is true (default).
//
// Returns an error if the server fails to start or the connection fails.
//
// Example:
//
//	client := copilot.NewClient(&copilot.ClientOptions{AutoStart: boolPtr(false)})
//	if err := client.Start(context.Background()); err != nil {
//	    log.Fatal("Failed to start:", err)
//	}
//	// Now ready to create sessions
func (c *Client) Start(ctx context.Context) error {
	if c.state == StateConnected {
		return nil
	}

	c.state = StateConnecting

	// Only start CLI server process if not connecting to external server
	if !c.isExternalServer {
		if err := c.startCLIServer(ctx); err != nil {
			c.state = StateError
			return err
		}
	}

	// Connect to the server
	if err := c.connectToServer(ctx); err != nil {
		c.state = StateError
		return err
	}

	// Verify protocol version compatibility
	if err := c.verifyProtocolVersion(ctx); err != nil {
		c.state = StateError
		return err
	}

	c.state = StateConnected
	return nil
}

// Stop stops the CLI server and closes all active sessions.
//
// This method performs graceful cleanup:
//  1. Destroys all active sessions
//  2. Closes the JSON-RPC connection
//  3. Terminates the CLI server process (if spawned by this client)
//
// Returns an error that aggregates all errors encountered during cleanup.
//
// Example:
//
//	if err := client.Stop(); err != nil {
//	    log.Printf("Cleanup error: %v", err)
//	}
func (c *Client) Stop() error {
	var errs []error

	// Destroy all active sessions
	c.sessionsMux.Lock()
	sessions := make([]*Session, 0, len(c.sessions))
	for _, session := range c.sessions {
		sessions = append(sessions, session)
	}
	c.sessionsMux.Unlock()

	for _, session := range sessions {
		if err := session.Destroy(); err != nil {
			errs = append(errs, fmt.Errorf("failed to destroy session %s: %w", session.SessionID, err))
		}
	}

	c.sessionsMux.Lock()
	c.sessions = make(map[string]*Session)
	c.sessionsMux.Unlock()

	// Kill CLI process FIRST (this closes stdout and unblocks readLoop) - only if we spawned it
	if c.process != nil && !c.isExternalServer {
		if err := c.process.Process.Kill(); err != nil {
			errs = append(errs, fmt.Errorf("failed to kill CLI process: %w", err))
		}
		c.process = nil
	}

	// Close external TCP connection if exists
	if c.isExternalServer && c.conn != nil {
		if err := c.conn.Close(); err != nil {
			errs = append(errs, fmt.Errorf("failed to close socket: %w", err))
		}
		c.conn = nil
	}

	// Then close JSON-RPC client (readLoop can now exit)
	if c.client != nil {
		c.client.Stop()
		c.client = nil
	}

	// Clear models cache
	c.modelsCacheMux.Lock()
	c.modelsCache = nil
	c.modelsCacheMux.Unlock()

	c.state = StateDisconnected
	if !c.isExternalServer {
		c.actualPort = 0
	}

	return errors.Join(errs...)
}

// ForceStop forcefully stops the CLI server without graceful cleanup.
//
// Use this when [Client.Stop] fails or takes too long. This method:
//   - Clears all sessions immediately without destroying them
//   - Force closes the connection
//   - Kills the CLI process (if spawned by this client)
//
// Example:
//
//	// If normal stop hangs, force stop
//	done := make(chan struct{})
//	go func() {
//	    client.Stop()
//	    close(done)
//	}()
//
//	select {
//	case <-done:
//	    // Stopped successfully
//	case <-time.After(5 * time.Second):
//	    client.ForceStop()
//	}
func (c *Client) ForceStop() {
	// Clear sessions immediately without trying to destroy them
	c.sessionsMux.Lock()
	c.sessions = make(map[string]*Session)
	c.sessionsMux.Unlock()

	// Kill CLI process (only if we spawned it)
	if c.process != nil && !c.isExternalServer {
		c.process.Process.Kill() // Ignore errors
		c.process = nil
	}

	// Close external TCP connection if exists
	if c.isExternalServer && c.conn != nil {
		_ = c.conn.Close() // Ignore errors
		c.conn = nil
	}

	// Close JSON-RPC client
	if c.client != nil {
		c.client.Stop()
		c.client = nil
	}

	// Clear models cache
	c.modelsCacheMux.Lock()
	c.modelsCache = nil
	c.modelsCacheMux.Unlock()

	c.state = StateDisconnected
	if !c.isExternalServer {
		c.actualPort = 0
	}
}

// buildProviderParams converts a ProviderConfig to a map for JSON-RPC params.
func buildProviderParams(p *ProviderConfig) map[string]any {
	params := make(map[string]any)
	if p.Type != "" {
		params["type"] = p.Type
	}
	if p.WireApi != "" {
		params["wireApi"] = p.WireApi
	}
	if p.BaseURL != "" {
		params["baseUrl"] = p.BaseURL
	}
	if p.APIKey != "" {
		params["apiKey"] = p.APIKey
	}
	if p.BearerToken != "" {
		params["bearerToken"] = p.BearerToken
	}
	if p.Azure != nil {
		azure := make(map[string]any)
		if p.Azure.APIVersion != "" {
			azure["apiVersion"] = p.Azure.APIVersion
		}
		if len(azure) > 0 {
			params["azure"] = azure
		}
	}
	return params
}

func (c *Client) ensureConnected() error {
	if c.client != nil {
		return nil
	}
	if c.autoStart {
		return c.Start(context.Background())
	}
	return fmt.Errorf("client not connected. Call Start() first")
}

// CreateSession creates a new conversation session with the Copilot CLI.
//
// Sessions maintain conversation state, handle events, and manage tool execution.
// If the client is not connected and AutoStart is enabled, this will automatically
// start the connection.
//
// The config parameter is optional; pass nil for default settings.
//
// Returns the created session or an error if session creation fails.
//
// Example:
//
//	// Basic session
//	session, err := client.CreateSession(context.Background(), nil)
//
//	// Session with model and tools
//	session, err := client.CreateSession(context.Background(), &copilot.SessionConfig{
//	    Model: "gpt-4",
//	    Tools: []copilot.Tool{
//	        {
//	            Name:        "get_weather",
//	            Description: "Get weather for a location",
//	            Handler:     weatherHandler,
//	        },
//	    },
//	})
func (c *Client) CreateSession(ctx context.Context, config *SessionConfig) (*Session, error) {
	if err := c.ensureConnected(); err != nil {
		return nil, err
	}

	params := make(map[string]any)
	if config != nil {
		if config.Model != "" {
			params["model"] = config.Model
		}
		if config.SessionID != "" {
			params["sessionId"] = config.SessionID
		}
		if config.ReasoningEffort != "" {
			params["reasoningEffort"] = config.ReasoningEffort
		}
		if len(config.Tools) > 0 {
			toolDefs := make([]map[string]any, 0, len(config.Tools))
			for _, tool := range config.Tools {
				if tool.Name == "" {
					continue
				}
				definition := map[string]any{
					"name":        tool.Name,
					"description": tool.Description,
				}
				if tool.Parameters != nil {
					definition["parameters"] = tool.Parameters
				}
				toolDefs = append(toolDefs, definition)
			}
			if len(toolDefs) > 0 {
				params["tools"] = toolDefs
			}
		}
		// Add system message configuration if provided
		if config.SystemMessage != nil {
			systemMessage := make(map[string]any)

			if config.SystemMessage.Mode != "" {
				systemMessage["mode"] = config.SystemMessage.Mode
			}

			if config.SystemMessage.Mode == "replace" {
				if config.SystemMessage.Content != "" {
					systemMessage["content"] = config.SystemMessage.Content
				}
			} else {
				if config.SystemMessage.Content != "" {
					systemMessage["content"] = config.SystemMessage.Content
				}
			}

			if len(systemMessage) > 0 {
				params["systemMessage"] = systemMessage
			}
		}
		// Add tool filtering options
		if len(config.AvailableTools) > 0 {
			params["availableTools"] = config.AvailableTools
		}
		if len(config.ExcludedTools) > 0 {
			params["excludedTools"] = config.ExcludedTools
		}
		// Add streaming option
		if config.Streaming {
			params["streaming"] = config.Streaming
		}
		// Add provider configuration
		if config.Provider != nil {
			params["provider"] = buildProviderParams(config.Provider)
		}
		// Add permission request flag
		if config.OnPermissionRequest != nil {
			params["requestPermission"] = true
		}
		// Add user input request flag
		if config.OnUserInputRequest != nil {
			params["requestUserInput"] = true
		}
		// Add hooks flag
		if config.Hooks != nil && (config.Hooks.OnPreToolUse != nil ||
			config.Hooks.OnPostToolUse != nil ||
			config.Hooks.OnUserPromptSubmitted != nil ||
			config.Hooks.OnSessionStart != nil ||
			config.Hooks.OnSessionEnd != nil ||
			config.Hooks.OnErrorOccurred != nil) {
			params["hooks"] = true
		}
		// Add working directory
		if config.WorkingDirectory != "" {
			params["workingDirectory"] = config.WorkingDirectory
		}
		// Add MCP servers configuration
		if len(config.MCPServers) > 0 {
			params["mcpServers"] = config.MCPServers
		}
		// Add custom agents configuration
		if len(config.CustomAgents) > 0 {
			customAgents := make([]map[string]any, 0, len(config.CustomAgents))
			for _, agent := range config.CustomAgents {
				agentMap := map[string]any{
					"name":   agent.Name,
					"prompt": agent.Prompt,
				}
				if agent.DisplayName != "" {
					agentMap["displayName"] = agent.DisplayName
				}
				if agent.Description != "" {
					agentMap["description"] = agent.Description
				}
				if len(agent.Tools) > 0 {
					agentMap["tools"] = agent.Tools
				}
				if len(agent.MCPServers) > 0 {
					agentMap["mcpServers"] = agent.MCPServers
				}
				if agent.Infer != nil {
					agentMap["infer"] = *agent.Infer
				}
				customAgents = append(customAgents, agentMap)
			}
			params["customAgents"] = customAgents
		}
		// Add config directory override
		if config.ConfigDir != "" {
			params["configDir"] = config.ConfigDir
		}
		// Add skill directories configuration
		if len(config.SkillDirectories) > 0 {
			params["skillDirectories"] = config.SkillDirectories
		}
		// Add disabled skills configuration
		if len(config.DisabledSkills) > 0 {
			params["disabledSkills"] = config.DisabledSkills
		}
		// Add infinite sessions configuration
		if config.InfiniteSessions != nil {
			infiniteSessions := make(map[string]any)
			if config.InfiniteSessions.Enabled != nil {
				infiniteSessions["enabled"] = *config.InfiniteSessions.Enabled
			}
			if config.InfiniteSessions.BackgroundCompactionThreshold != nil {
				infiniteSessions["backgroundCompactionThreshold"] = *config.InfiniteSessions.BackgroundCompactionThreshold
			}
			if config.InfiniteSessions.BufferExhaustionThreshold != nil {
				infiniteSessions["bufferExhaustionThreshold"] = *config.InfiniteSessions.BufferExhaustionThreshold
			}
			params["infiniteSessions"] = infiniteSessions
		}
	}

	result, err := c.client.Request("session.create", params)
	if err != nil {
		return nil, fmt.Errorf("failed to create session: %w", err)
	}

	sessionID, ok := result["sessionId"].(string)
	if !ok {
		return nil, fmt.Errorf("invalid response: missing sessionId")
	}

	workspacePath, _ := result["workspacePath"].(string)

	session := newSession(sessionID, c.client, workspacePath)

	if config != nil {
		session.registerTools(config.Tools)
		if config.OnPermissionRequest != nil {
			session.registerPermissionHandler(config.OnPermissionRequest)
		}
		if config.OnUserInputRequest != nil {
			session.registerUserInputHandler(config.OnUserInputRequest)
		}
		if config.Hooks != nil {
			session.registerHooks(config.Hooks)
		}
	} else {
		session.registerTools(nil)
	}

	c.sessionsMux.Lock()
	c.sessions[sessionID] = session
	c.sessionsMux.Unlock()

	return session, nil
}

// ResumeSession resumes an existing conversation session by its ID using default options.
//
// This is a convenience method that calls [Client.ResumeSessionWithOptions] with nil config.
//
// Example:
//
//	session, err := client.ResumeSession(context.Background(), "session-123")
func (c *Client) ResumeSession(ctx context.Context, sessionID string) (*Session, error) {
	return c.ResumeSessionWithOptions(ctx, sessionID, nil)
}

// ResumeSessionWithOptions resumes an existing conversation session with additional configuration.
//
// This allows you to continue a previous conversation, maintaining all conversation history.
// The session must have been previously created and not deleted.
//
// Example:
//
//	session, err := client.ResumeSessionWithOptions(context.Background(), "session-123", &copilot.ResumeSessionConfig{
//	    Tools: []copilot.Tool{myNewTool},
//	})
func (c *Client) ResumeSessionWithOptions(ctx context.Context, sessionID string, config *ResumeSessionConfig) (*Session, error) {
	if err := c.ensureConnected(); err != nil {
		return nil, err
	}

	params := map[string]any{
		"sessionId": sessionID,
	}

	if config != nil {
		if config.Model != "" {
			params["model"] = config.Model
		}
		if config.ReasoningEffort != "" {
			params["reasoningEffort"] = config.ReasoningEffort
		}
		if config.SystemMessage != nil {
			systemMessage := make(map[string]any)

			if config.SystemMessage.Mode != "" {
				systemMessage["mode"] = config.SystemMessage.Mode
			}

			if config.SystemMessage.Mode == "replace" {
				if config.SystemMessage.Content != "" {
					systemMessage["content"] = config.SystemMessage.Content
				}
			} else {
				if config.SystemMessage.Content != "" {
					systemMessage["content"] = config.SystemMessage.Content
				}
			}

			if len(systemMessage) > 0 {
				params["systemMessage"] = systemMessage
			}
		}
		if len(config.AvailableTools) > 0 {
			params["availableTools"] = config.AvailableTools
		}
		if len(config.ExcludedTools) > 0 {
			params["excludedTools"] = config.ExcludedTools
		}
		if len(config.Tools) > 0 {
			toolDefs := make([]map[string]any, 0, len(config.Tools))
			for _, tool := range config.Tools {
				if tool.Name == "" {
					continue
				}
				definition := map[string]any{
					"name":        tool.Name,
					"description": tool.Description,
				}
				if tool.Parameters != nil {
					definition["parameters"] = tool.Parameters
				}
				toolDefs = append(toolDefs, definition)
			}
			if len(toolDefs) > 0 {
				params["tools"] = toolDefs
			}
		}
		if config.Provider != nil {
			params["provider"] = buildProviderParams(config.Provider)
		}
		// Add streaming option
		if config.Streaming {
			params["streaming"] = config.Streaming
		}
		// Add permission request flag
		if config.OnPermissionRequest != nil {
			params["requestPermission"] = true
		}
		// Add user input request flag
		if config.OnUserInputRequest != nil {
			params["requestUserInput"] = true
		}
		// Add hooks flag
		if config.Hooks != nil && (config.Hooks.OnPreToolUse != nil ||
			config.Hooks.OnPostToolUse != nil ||
			config.Hooks.OnUserPromptSubmitted != nil ||
			config.Hooks.OnSessionStart != nil ||
			config.Hooks.OnSessionEnd != nil ||
			config.Hooks.OnErrorOccurred != nil) {
			params["hooks"] = true
		}
		// Add working directory
		if config.WorkingDirectory != "" {
			params["workingDirectory"] = config.WorkingDirectory
		}
		// Add config directory
		if config.ConfigDir != "" {
			params["configDir"] = config.ConfigDir
		}
		// Add disable resume flag
		if config.DisableResume {
			params["disableResume"] = true
		}
		// Add MCP servers configuration
		if len(config.MCPServers) > 0 {
			params["mcpServers"] = config.MCPServers
		}
		// Add custom agents configuration
		if len(config.CustomAgents) > 0 {
			customAgents := make([]map[string]any, 0, len(config.CustomAgents))
			for _, agent := range config.CustomAgents {
				agentMap := map[string]any{
					"name":   agent.Name,
					"prompt": agent.Prompt,
				}
				if agent.DisplayName != "" {
					agentMap["displayName"] = agent.DisplayName
				}
				if agent.Description != "" {
					agentMap["description"] = agent.Description
				}
				if len(agent.Tools) > 0 {
					agentMap["tools"] = agent.Tools
				}
				if len(agent.MCPServers) > 0 {
					agentMap["mcpServers"] = agent.MCPServers
				}
				if agent.Infer != nil {
					agentMap["infer"] = *agent.Infer
				}
				customAgents = append(customAgents, agentMap)
			}
			params["customAgents"] = customAgents
		}
		// Add skill directories configuration
		if len(config.SkillDirectories) > 0 {
			params["skillDirectories"] = config.SkillDirectories
		}
		// Add disabled skills configuration
		if len(config.DisabledSkills) > 0 {
			params["disabledSkills"] = config.DisabledSkills
		}
		// Add infinite sessions configuration
		if config.InfiniteSessions != nil {
			infiniteSessions := map[string]any{}
			if config.InfiniteSessions.Enabled != nil {
				infiniteSessions["enabled"] = *config.InfiniteSessions.Enabled
			}
			if config.InfiniteSessions.BackgroundCompactionThreshold != nil {
				infiniteSessions["backgroundCompactionThreshold"] = *config.InfiniteSessions.BackgroundCompactionThreshold
			}
			if config.InfiniteSessions.BufferExhaustionThreshold != nil {
				infiniteSessions["bufferExhaustionThreshold"] = *config.InfiniteSessions.BufferExhaustionThreshold
			}
			params["infiniteSessions"] = infiniteSessions
		}
	}

	result, err := c.client.Request("session.resume", params)
	if err != nil {
		return nil, fmt.Errorf("failed to resume session: %w", err)
	}

	resumedSessionID, ok := result["sessionId"].(string)
	if !ok {
		return nil, fmt.Errorf("invalid response: missing sessionId")
	}

	workspacePath, _ := result["workspacePath"].(string)

	session := newSession(resumedSessionID, c.client, workspacePath)
	if config != nil {
		session.registerTools(config.Tools)
		if config.OnPermissionRequest != nil {
			session.registerPermissionHandler(config.OnPermissionRequest)
		}
		if config.OnUserInputRequest != nil {
			session.registerUserInputHandler(config.OnUserInputRequest)
		}
		if config.Hooks != nil {
			session.registerHooks(config.Hooks)
		}
	} else {
		session.registerTools(nil)
	}

	c.sessionsMux.Lock()
	c.sessions[resumedSessionID] = session
	c.sessionsMux.Unlock()

	return session, nil
}

// ListSessions returns metadata about all sessions known to the server.
//
// Returns a list of SessionMetadata for all available sessions, including their IDs,
// timestamps, and optional summaries.
//
// Example:
//
//	sessions, err := client.ListSessions(context.Background())
//	if err != nil {
//	    log.Fatal(err)
//	}
//	for _, session := range sessions {
//	    fmt.Printf("Session: %s\n", session.SessionID)
//	}
func (c *Client) ListSessions(ctx context.Context) ([]SessionMetadata, error) {
	if err := c.ensureConnected(); err != nil {
		return nil, err
	}

	result, err := c.client.Request("session.list", map[string]any{})
	if err != nil {
		return nil, err
	}

	// Marshal and unmarshal to convert map to struct
	jsonBytes, err := json.Marshal(result)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal sessions response: %w", err)
	}

	var response ListSessionsResponse
	if err := json.Unmarshal(jsonBytes, &response); err != nil {
		return nil, fmt.Errorf("failed to unmarshal sessions response: %w", err)
	}

	return response.Sessions, nil
}

// DeleteSession permanently deletes a session and all its conversation history.
//
// The session cannot be resumed after deletion. If the session is in the local
// sessions map, it will be removed.
//
// Example:
//
//	if err := client.DeleteSession(context.Background(), "session-123"); err != nil {
//	    log.Fatal(err)
//	}
func (c *Client) DeleteSession(ctx context.Context, sessionID string) error {
	if err := c.ensureConnected(); err != nil {
		return err
	}

	params := map[string]any{
		"sessionId": sessionID,
	}

	result, err := c.client.Request("session.delete", params)
	if err != nil {
		return err
	}

	// Marshal and unmarshal to convert map to struct
	jsonBytes, err := json.Marshal(result)
	if err != nil {
		return fmt.Errorf("failed to marshal delete response: %w", err)
	}

	var response DeleteSessionResponse
	if err := json.Unmarshal(jsonBytes, &response); err != nil {
		return fmt.Errorf("failed to unmarshal delete response: %w", err)
	}

	if !response.Success {
		errorMsg := "unknown error"
		if response.Error != nil {
			errorMsg = *response.Error
		}
		return fmt.Errorf("failed to delete session %s: %s", sessionID, errorMsg)
	}

	// Remove from local sessions map if present
	c.sessionsMux.Lock()
	delete(c.sessions, sessionID)
	c.sessionsMux.Unlock()

	return nil
}

// GetForegroundSessionID returns the ID of the session currently displayed in the TUI.
//
// This is only available when connecting to a server running in TUI+server mode
// (--ui-server). Returns nil if no foreground session is set.
//
// Example:
//
//	sessionID, err := client.GetForegroundSessionID()
//	if err != nil {
//	    log.Fatal(err)
//	}
//	if sessionID != nil {
//	    fmt.Printf("TUI is displaying session: %s\n", *sessionID)
//	}
func (c *Client) GetForegroundSessionID(ctx context.Context) (*string, error) {
	if c.client == nil {
		if c.autoStart {
			if err := c.Start(ctx); err != nil {
				return nil, err
			}
		} else {
			return nil, fmt.Errorf("client not connected. Call Start() first")
		}
	}

	result, err := c.client.Request("session.getForeground", map[string]any{})
	if err != nil {
		return nil, err
	}

	jsonBytes, err := json.Marshal(result)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal getForeground response: %w", err)
	}

	var response GetForegroundSessionResponse
	if err := json.Unmarshal(jsonBytes, &response); err != nil {
		return nil, fmt.Errorf("failed to unmarshal getForeground response: %w", err)
	}

	return response.SessionID, nil
}

// SetForegroundSessionID requests the TUI to switch to displaying the specified session.
//
// This is only available when connecting to a server running in TUI+server mode
// (--ui-server).
//
// Example:
//
//	if err := client.SetForegroundSessionID("session-123"); err != nil {
//	    log.Fatal(err)
//	}
func (c *Client) SetForegroundSessionID(ctx context.Context, sessionID string) error {
	if c.client == nil {
		if c.autoStart {
			if err := c.Start(ctx); err != nil {
				return err
			}
		} else {
			return fmt.Errorf("client not connected. Call Start() first")
		}
	}

	params := map[string]any{
		"sessionId": sessionID,
	}

	result, err := c.client.Request("session.setForeground", params)
	if err != nil {
		return err
	}

	jsonBytes, err := json.Marshal(result)
	if err != nil {
		return fmt.Errorf("failed to marshal setForeground response: %w", err)
	}

	var response SetForegroundSessionResponse
	if err := json.Unmarshal(jsonBytes, &response); err != nil {
		return fmt.Errorf("failed to unmarshal setForeground response: %w", err)
	}

	if !response.Success {
		errorMsg := "unknown error"
		if response.Error != nil {
			errorMsg = *response.Error
		}
		return fmt.Errorf("failed to set foreground session: %s", errorMsg)
	}

	return nil
}

// On subscribes to all session lifecycle events.
//
// Lifecycle events are emitted when sessions are created, deleted, updated,
// or change foreground/background state (in TUI+server mode).
//
// Returns a function that, when called, unsubscribes the handler.
//
// Example:
//
//	unsubscribe := client.On(func(event copilot.SessionLifecycleEvent) {
//	    fmt.Printf("Session %s: %s\n", event.SessionID, event.Type)
//	})
//	defer unsubscribe()
func (c *Client) On(handler SessionLifecycleHandler) func() {
	c.lifecycleHandlersMux.Lock()
	c.lifecycleHandlers = append(c.lifecycleHandlers, handler)
	c.lifecycleHandlersMux.Unlock()

	return func() {
		c.lifecycleHandlersMux.Lock()
		defer c.lifecycleHandlersMux.Unlock()
		for i, h := range c.lifecycleHandlers {
			// Compare function pointers
			if &h == &handler {
				c.lifecycleHandlers = append(c.lifecycleHandlers[:i], c.lifecycleHandlers[i+1:]...)
				break
			}
		}
	}
}

// OnEventType subscribes to a specific session lifecycle event type.
//
// Returns a function that, when called, unsubscribes the handler.
//
// Example:
//
//	unsubscribe := client.OnEventType(copilot.SessionLifecycleForeground, func(event copilot.SessionLifecycleEvent) {
//	    fmt.Printf("Session %s is now in foreground\n", event.SessionID)
//	})
//	defer unsubscribe()
func (c *Client) OnEventType(eventType SessionLifecycleEventType, handler SessionLifecycleHandler) func() {
	c.lifecycleHandlersMux.Lock()
	if c.typedLifecycleHandlers == nil {
		c.typedLifecycleHandlers = make(map[SessionLifecycleEventType][]SessionLifecycleHandler)
	}
	c.typedLifecycleHandlers[eventType] = append(c.typedLifecycleHandlers[eventType], handler)
	c.lifecycleHandlersMux.Unlock()

	return func() {
		c.lifecycleHandlersMux.Lock()
		defer c.lifecycleHandlersMux.Unlock()
		handlers := c.typedLifecycleHandlers[eventType]
		for i, h := range handlers {
			if &h == &handler {
				c.typedLifecycleHandlers[eventType] = append(handlers[:i], handlers[i+1:]...)
				break
			}
		}
	}
}

// dispatchLifecycleEvent dispatches a lifecycle event to all registered handlers
func (c *Client) dispatchLifecycleEvent(event SessionLifecycleEvent) {
	c.lifecycleHandlersMux.Lock()
	// Copy handlers to avoid holding lock during callbacks
	typedHandlers := make([]SessionLifecycleHandler, 0)
	if handlers, ok := c.typedLifecycleHandlers[event.Type]; ok {
		typedHandlers = append(typedHandlers, handlers...)
	}
	wildcardHandlers := make([]SessionLifecycleHandler, len(c.lifecycleHandlers))
	copy(wildcardHandlers, c.lifecycleHandlers)
	c.lifecycleHandlersMux.Unlock()

	// Dispatch to typed handlers
	for _, handler := range typedHandlers {
		func() {
			defer func() { recover() }() // Ignore handler panics
			handler(event)
		}()
	}

	// Dispatch to wildcard handlers
	for _, handler := range wildcardHandlers {
		func() {
			defer func() { recover() }() // Ignore handler panics
			handler(event)
		}()
	}
}

// State returns the current connection state of the client.
//
// Possible states: StateDisconnected, StateConnecting, StateConnected, StateError.
//
// Example:
//
//	if client.State() == copilot.StateConnected {
//	    session, err := client.CreateSession(context.Background(), nil)
//	}
func (c *Client) State() ConnectionState {
	return c.state
}

// Ping sends a ping request to the server to verify connectivity.
//
// The message parameter is optional and will be echoed back in the response.
// Returns a PingResponse containing the message and server timestamp, or an error.
//
// Example:
//
//	resp, err := client.Ping(context.Background(), "health check")
//	if err != nil {
//	    log.Printf("Server unreachable: %v", err)
//	} else {
//	    log.Printf("Server responded at %d", resp.Timestamp)
//	}
func (c *Client) Ping(ctx context.Context, message string) (*PingResponse, error) {
	if c.client == nil {
		return nil, fmt.Errorf("client not connected")
	}

	params := map[string]any{}
	if message != "" {
		params["message"] = message
	}

	result, err := c.client.Request("ping", params)
	if err != nil {
		return nil, err
	}

	response := &PingResponse{}
	if msg, ok := result["message"].(string); ok {
		response.Message = msg
	}
	if ts, ok := result["timestamp"].(float64); ok {
		response.Timestamp = int64(ts)
	}
	if pv, ok := result["protocolVersion"].(float64); ok {
		v := int(pv)
		response.ProtocolVersion = &v
	}

	return response, nil
}

// GetStatus returns CLI status including version and protocol information
func (c *Client) GetStatus(ctx context.Context) (*GetStatusResponse, error) {
	if c.client == nil {
		return nil, fmt.Errorf("client not connected")
	}

	result, err := c.client.Request("status.get", map[string]any{})
	if err != nil {
		return nil, err
	}

	response := &GetStatusResponse{}
	if v, ok := result["version"].(string); ok {
		response.Version = v
	}
	if pv, ok := result["protocolVersion"].(float64); ok {
		response.ProtocolVersion = int(pv)
	}

	return response, nil
}

// GetAuthStatus returns current authentication status
func (c *Client) GetAuthStatus(ctx context.Context) (*GetAuthStatusResponse, error) {
	if c.client == nil {
		return nil, fmt.Errorf("client not connected")
	}

	result, err := c.client.Request("auth.getStatus", map[string]any{})
	if err != nil {
		return nil, err
	}

	response := &GetAuthStatusResponse{}
	if v, ok := result["isAuthenticated"].(bool); ok {
		response.IsAuthenticated = v
	}
	if v, ok := result["authType"].(string); ok {
		response.AuthType = &v
	}
	if v, ok := result["host"].(string); ok {
		response.Host = &v
	}
	if v, ok := result["login"].(string); ok {
		response.Login = &v
	}
	if v, ok := result["statusMessage"].(string); ok {
		response.StatusMessage = &v
	}

	return response, nil
}

// ListModels returns available models with their metadata.
//
// Results are cached after the first successful call to avoid rate limiting.
// The cache is cleared when the client disconnects.
func (c *Client) ListModels(ctx context.Context) ([]ModelInfo, error) {
	if c.client == nil {
		return nil, fmt.Errorf("client not connected")
	}

	// Use mutex for locking to prevent race condition with concurrent calls
	c.modelsCacheMux.Lock()
	defer c.modelsCacheMux.Unlock()

	// Check cache (already inside lock)
	if c.modelsCache != nil {
		// Return a copy to prevent cache mutation
		result := make([]ModelInfo, len(c.modelsCache))
		copy(result, c.modelsCache)
		return result, nil
	}

	// Cache miss - fetch from backend while holding lock
	result, err := c.client.Request("models.list", map[string]any{})
	if err != nil {
		return nil, err
	}

	// Marshal and unmarshal to convert map to struct
	jsonBytes, err := json.Marshal(result)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal models response: %w", err)
	}

	var response GetModelsResponse
	if err := json.Unmarshal(jsonBytes, &response); err != nil {
		return nil, fmt.Errorf("failed to unmarshal models response: %w", err)
	}

	// Update cache before releasing lock
	c.modelsCache = response.Models

	// Return a copy to prevent cache mutation
	models := make([]ModelInfo, len(response.Models))
	copy(models, response.Models)
	return models, nil
}

// verifyProtocolVersion verifies that the server's protocol version matches the SDK's expected version
func (c *Client) verifyProtocolVersion(ctx context.Context) error {
	expectedVersion := GetSdkProtocolVersion()
	pingResult, err := c.Ping(ctx, "")
	if err != nil {
		return err
	}

	if pingResult.ProtocolVersion == nil {
		return fmt.Errorf("SDK protocol version mismatch: SDK expects version %d, but server does not report a protocol version. Please update your server to ensure compatibility", expectedVersion)
	}

	if *pingResult.ProtocolVersion != expectedVersion {
		return fmt.Errorf("SDK protocol version mismatch: SDK expects version %d, but server reports version %d. Please update your SDK or server to ensure compatibility", expectedVersion, *pingResult.ProtocolVersion)
	}

	return nil
}

// startCLIServer starts the CLI server process.
//
// This spawns the CLI server as a subprocess using the configured transport
// mode (stdio or TCP).
func (c *Client) startCLIServer(ctx context.Context) error {
	args := []string{"--headless", "--log-level", c.options.LogLevel}

	// Choose transport mode
	if c.useStdio {
		args = append(args, "--stdio")
	} else if c.options.Port > 0 {
		args = append(args, "--port", strconv.Itoa(c.options.Port))
	}

	// Add auth-related flags
	if c.options.GithubToken != "" {
		args = append(args, "--auth-token-env", "COPILOT_SDK_AUTH_TOKEN")
	}
	// Default useLoggedInUser to false when GithubToken is provided
	useLoggedInUser := true
	if c.options.UseLoggedInUser != nil {
		useLoggedInUser = *c.options.UseLoggedInUser
	} else if c.options.GithubToken != "" {
		useLoggedInUser = false
	}
	if !useLoggedInUser {
		args = append(args, "--no-auto-login")
	}

	// If CLIPath is a .js file, run it with node
	// Note we can't rely on the shebang as Windows doesn't support it
	command := c.options.CLIPath
	if strings.HasSuffix(c.options.CLIPath, ".js") {
		command = "node"
		args = append([]string{c.options.CLIPath}, args...)
	}

	c.process = exec.CommandContext(ctx, command, args...)

	// Set working directory if specified
	if c.options.Cwd != "" {
		c.process.Dir = c.options.Cwd
	}

	// Add auth token if needed.
	c.process.Env = c.options.Env
	if c.options.GithubToken != "" {
		c.process.Env = append(c.process.Env, "COPILOT_SDK_AUTH_TOKEN="+c.options.GithubToken)
	}

	if c.useStdio {
		// For stdio mode, we need stdin/stdout pipes
		stdin, err := c.process.StdinPipe()
		if err != nil {
			return fmt.Errorf("failed to create stdin pipe: %w", err)
		}

		stdout, err := c.process.StdoutPipe()
		if err != nil {
			return fmt.Errorf("failed to create stdout pipe: %w", err)
		}

		stderr, err := c.process.StderrPipe()
		if err != nil {
			return fmt.Errorf("failed to create stderr pipe: %w", err)
		}

		// Read stderr in background
		go func() {
			scanner := bufio.NewScanner(stderr)
			for scanner.Scan() {
				// Optionally log stderr
				// fmt.Fprintf(os.Stderr, "CLI stderr: %s\n", scanner.Text())
			}
		}()

		if err := c.process.Start(); err != nil {
			return fmt.Errorf("failed to start CLI server: %w", err)
		}

		// Create JSON-RPC client immediately
		c.client = jsonrpc2.NewClient(stdin, stdout)
		c.setupNotificationHandler()
		c.client.Start()

		return nil
	} else {
		// For TCP mode, capture stdout to get port number
		stdout, err := c.process.StdoutPipe()
		if err != nil {
			return fmt.Errorf("failed to create stdout pipe: %w", err)
		}

		if err := c.process.Start(); err != nil {
			return fmt.Errorf("failed to start CLI server: %w", err)
		}

		// Wait for port announcement
		scanner := bufio.NewScanner(stdout)
		timeout := time.After(10 * time.Second)
		portRegex := regexp.MustCompile(`listening on port (\d+)`)

		for {
			select {
			case <-timeout:
				return fmt.Errorf("timeout waiting for CLI server to start")
			default:
				if scanner.Scan() {
					line := scanner.Text()
					if matches := portRegex.FindStringSubmatch(line); len(matches) > 1 {
						port, err := strconv.Atoi(matches[1])
						if err != nil {
							return fmt.Errorf("failed to parse port: %w", err)
						}
						c.actualPort = port
						return nil
					}
				}
			}
		}
	}
}

// connectToServer establishes a connection to the server.
func (c *Client) connectToServer(ctx context.Context) error {
	if c.useStdio {
		// Already connected via stdio in startCLIServer
		return nil
	}

	// Connect via TCP
	return c.connectViaTcp(ctx)
}

// connectViaTcp connects to the CLI server via TCP socket.
func (c *Client) connectViaTcp(ctx context.Context) error {
	if c.actualPort == 0 {
		return fmt.Errorf("server port not available")
	}

	// Create TCP connection that cancels on context done or after 10 seconds
	address := net.JoinHostPort(c.actualHost, fmt.Sprintf("%d", c.actualPort))
	dialer := net.Dialer{
		Timeout: 10 * time.Second,
	}
	conn, err := dialer.DialContext(ctx, "tcp", address)
	if err != nil {
		return fmt.Errorf("failed to connect to CLI server at %s: %w", address, err)
	}

	c.conn = conn

	// Create JSON-RPC client with the connection
	c.client = jsonrpc2.NewClient(conn, conn)
	c.setupNotificationHandler()
	c.client.Start()

	return nil
}

// setupNotificationHandler configures handlers for session events, tool calls, and permission requests.
func (c *Client) setupNotificationHandler() {
	c.client.SetNotificationHandler(func(method string, params map[string]any) {
		switch method {
		case "session.event":
			// Extract sessionId and event
			sessionID, ok := params["sessionId"].(string)
			if !ok {
				return
			}

			// Marshal the event back to JSON and unmarshal into typed struct
			eventJSON, err := json.Marshal(params["event"])
			if err != nil {
				return
			}

			event, err := UnmarshalSessionEvent(eventJSON)
			if err != nil {
				return
			}

			// Dispatch to session
			c.sessionsMux.Lock()
			session, ok := c.sessions[sessionID]
			c.sessionsMux.Unlock()

			if ok {
				session.dispatchEvent(event)
			}
		case "session.lifecycle":
			// Handle session lifecycle events
			eventJSON, err := json.Marshal(params)
			if err != nil {
				return
			}

			var event SessionLifecycleEvent
			if err := json.Unmarshal(eventJSON, &event); err != nil {
				return
			}

			c.dispatchLifecycleEvent(event)
		}
	})

	c.client.SetRequestHandler("tool.call", c.handleToolCallRequest)
	c.client.SetRequestHandler("permission.request", c.handlePermissionRequest)
	c.client.SetRequestHandler("userInput.request", c.handleUserInputRequest)
	c.client.SetRequestHandler("hooks.invoke", c.handleHooksInvoke)
}

// handleToolCallRequest handles a tool call request from the CLI server.
func (c *Client) handleToolCallRequest(params map[string]any) (map[string]any, *jsonrpc2.Error) {
	sessionID, _ := params["sessionId"].(string)
	toolCallID, _ := params["toolCallId"].(string)
	toolName, _ := params["toolName"].(string)

	if sessionID == "" || toolCallID == "" || toolName == "" {
		return nil, &jsonrpc2.Error{Code: -32602, Message: "invalid tool call payload"}
	}

	c.sessionsMux.Lock()
	session, ok := c.sessions[sessionID]
	c.sessionsMux.Unlock()
	if !ok {
		return nil, &jsonrpc2.Error{Code: -32602, Message: fmt.Sprintf("unknown session %s", sessionID)}
	}

	handler, ok := session.getToolHandler(toolName)
	if !ok {
		return map[string]any{"result": buildUnsupportedToolResult(toolName)}, nil
	}

	arguments := params["arguments"]
	result := c.executeToolCall(sessionID, toolCallID, toolName, arguments, handler)

	return map[string]any{"result": result}, nil
}

// executeToolCall executes a tool handler and returns the result.
func (c *Client) executeToolCall(
	sessionID, toolCallID, toolName string,
	arguments any,
	handler ToolHandler,
) (result ToolResult) {
	invocation := ToolInvocation{
		SessionID:  sessionID,
		ToolCallID: toolCallID,
		ToolName:   toolName,
		Arguments:  arguments,
	}

	defer func() {
		if r := recover(); r != nil {
			result = buildFailedToolResult(fmt.Sprintf("tool panic: %v", r))
		}
	}()

	if handler != nil {
		var err error
		result, err = handler(invocation)
		if err != nil {
			result = buildFailedToolResult(err.Error())
		}
	}

	return result
}

// handlePermissionRequest handles a permission request from the CLI server.
func (c *Client) handlePermissionRequest(params map[string]any) (map[string]any, *jsonrpc2.Error) {
	sessionID, _ := params["sessionId"].(string)
	permissionRequest, _ := params["permissionRequest"].(map[string]any)

	if sessionID == "" {
		return nil, &jsonrpc2.Error{Code: -32602, Message: "invalid permission request payload"}
	}

	c.sessionsMux.Lock()
	session, ok := c.sessions[sessionID]
	c.sessionsMux.Unlock()
	if !ok {
		return nil, &jsonrpc2.Error{Code: -32602, Message: fmt.Sprintf("unknown session %s", sessionID)}
	}

	result, err := session.handlePermissionRequest(permissionRequest)
	if err != nil {
		// Return denial on error
		return map[string]any{
			"result": map[string]any{
				"kind": "denied-no-approval-rule-and-could-not-request-from-user",
			},
		}, nil
	}

	return map[string]any{"result": result}, nil
}

// handleUserInputRequest handles a user input request from the CLI server.
func (c *Client) handleUserInputRequest(params map[string]any) (map[string]any, *jsonrpc2.Error) {
	sessionID, _ := params["sessionId"].(string)
	question, _ := params["question"].(string)

	if sessionID == "" || question == "" {
		return nil, &jsonrpc2.Error{Code: -32602, Message: "invalid user input request payload"}
	}

	c.sessionsMux.Lock()
	session, ok := c.sessions[sessionID]
	c.sessionsMux.Unlock()
	if !ok {
		return nil, &jsonrpc2.Error{Code: -32602, Message: fmt.Sprintf("unknown session %s", sessionID)}
	}

	// Parse choices
	var choices []string
	if choicesRaw, ok := params["choices"].([]any); ok {
		for _, choice := range choicesRaw {
			if s, ok := choice.(string); ok {
				choices = append(choices, s)
			}
		}
	}

	var allowFreeform *bool
	if af, ok := params["allowFreeform"].(bool); ok {
		allowFreeform = &af
	}

	request := UserInputRequest{
		Question:      question,
		Choices:       choices,
		AllowFreeform: allowFreeform,
	}

	response, err := session.handleUserInputRequest(request)
	if err != nil {
		return nil, &jsonrpc2.Error{Code: -32603, Message: err.Error()}
	}

	return map[string]any{
		"answer":      response.Answer,
		"wasFreeform": response.WasFreeform,
	}, nil
}

// handleHooksInvoke handles a hooks invocation from the CLI server.
func (c *Client) handleHooksInvoke(params map[string]any) (map[string]any, *jsonrpc2.Error) {
	sessionID, _ := params["sessionId"].(string)
	hookType, _ := params["hookType"].(string)
	input, _ := params["input"].(map[string]any)

	if sessionID == "" || hookType == "" {
		return nil, &jsonrpc2.Error{Code: -32602, Message: "invalid hooks invoke payload"}
	}

	c.sessionsMux.Lock()
	session, ok := c.sessions[sessionID]
	c.sessionsMux.Unlock()
	if !ok {
		return nil, &jsonrpc2.Error{Code: -32602, Message: fmt.Sprintf("unknown session %s", sessionID)}
	}

	output, err := session.handleHooksInvoke(hookType, input)
	if err != nil {
		return nil, &jsonrpc2.Error{Code: -32603, Message: err.Error()}
	}

	result := make(map[string]any)
	if output != nil {
		result["output"] = output
	}
	return result, nil
}

// The detailed error is stored in the Error field but not exposed to the LLM for security.
func buildFailedToolResult(internalError string) ToolResult {
	return ToolResult{
		TextResultForLLM: "Invoking this tool produced an error. Detailed information is not available.",
		ResultType:       "failure",
		Error:            internalError,
		ToolTelemetry:    map[string]any{},
	}
}

// buildUnsupportedToolResult creates a failure ToolResult for an unsupported tool.
func buildUnsupportedToolResult(toolName string) ToolResult {
	return ToolResult{
		TextResultForLLM: fmt.Sprintf("Tool '%s' is not supported by this client instance.", toolName),
		ResultType:       "failure",
		Error:            fmt.Sprintf("tool '%s' not supported", toolName),
		ToolTelemetry:    map[string]any{},
	}
}
