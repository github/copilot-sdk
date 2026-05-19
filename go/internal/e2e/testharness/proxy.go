package testharness

import (
	"bufio"
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"os/exec"
	"regexp"
	"strings"
	"sync"
)

// CapiProxy manages a child process that acts as a replaying proxy to AI endpoints.
// It spawns the shared test harness server from test/harness/server.ts.
type CapiProxy struct {
	cmd             *exec.Cmd
	proxyURL        string
	connectProxyURL string
	caFilePath      string
	mu              sync.Mutex
}

// NewCapiProxy creates a new proxy instance.
func NewCapiProxy() *CapiProxy {
	return &CapiProxy{}
}

// Start launches the proxy server and returns its URL.
func (p *CapiProxy) Start() (string, error) {
	p.mu.Lock()
	defer p.mu.Unlock()

	if p.proxyURL != "" {
		return p.proxyURL, nil
	}

	// The harness server is in the shared test directory
	serverPath := "../../../test/harness/server.ts"

	p.cmd = exec.Command("npx", "tsx", serverPath)
	p.cmd.Dir = "." // Will be resolved relative to test execution

	stdout, err := p.cmd.StdoutPipe()
	if err != nil {
		return "", fmt.Errorf("failed to get stdout pipe: %w", err)
	}

	// Forward stderr to parent for debugging
	p.cmd.Stderr = os.Stderr

	if err := p.cmd.Start(); err != nil {
		return "", fmt.Errorf("failed to start proxy server: %w", err)
	}

	// Read until the server prints "Listening: http://..."; npm/npx may emit
	// wrapper output first on some platforms.
	reader := bufio.NewReader(stdout)
	re := regexp.MustCompile(`Listening: (http://[^\s]+)\s+(\{.*\})$`)
	var matches []string
	var line string
	for {
		nextLine, err := reader.ReadString('\n')
		if err != nil && err != io.EOF {
			p.cmd.Process.Kill()
			return "", fmt.Errorf("failed to read proxy URL: %w", err)
		}
		line = strings.TrimSpace(nextLine)
		matches = re.FindStringSubmatch(line)
		if len(matches) >= 3 {
			break
		}
		if strings.Contains(line, "Listening: ") {
			p.cmd.Process.Kill()
			return "", fmt.Errorf("proxy startup line missing CONNECT proxy metadata: %s", line)
		}
		if err == io.EOF {
			p.cmd.Process.Kill()
			return "", fmt.Errorf("proxy exited before startup; last output: %s", line)
		}
	}

	p.proxyURL = matches[1]
	var metadata struct {
		ConnectProxyURL string `json:"connectProxyUrl"`
		CAFilePath      string `json:"caFilePath"`
	}
	if err := json.Unmarshal([]byte(matches[2]), &metadata); err != nil {
		p.cmd.Process.Kill()
		return "", fmt.Errorf("failed to parse proxy startup metadata: %w", err)
	}
	p.connectProxyURL = metadata.ConnectProxyURL
	p.caFilePath = metadata.CAFilePath
	if p.connectProxyURL == "" || p.caFilePath == "" {
		p.cmd.Process.Kill()
		return "", fmt.Errorf("proxy startup metadata missing CONNECT proxy details: %s", line)
	}
	return p.proxyURL, nil
}

// Stop gracefully shuts down the proxy server.
func (p *CapiProxy) Stop() error {
	return p.StopWithOptions(false)
}

// StopWithOptions gracefully shuts down the proxy server.
// If skipWritingCache is true, the proxy won't write captured exchanges to disk.
func (p *CapiProxy) StopWithOptions(skipWritingCache bool) error {
	p.mu.Lock()
	defer p.mu.Unlock()

	if p.cmd == nil || p.cmd.Process == nil {
		return nil
	}

	// Send stop request to the server
	if p.proxyURL != "" {
		stopURL := p.proxyURL + "/stop"
		if skipWritingCache {
			stopURL += "?skipWritingCache=true"
		}
		// Best effort - ignore errors
		resp, err := http.Post(stopURL, "application/json", nil)
		if err == nil {
			resp.Body.Close()
		}
	}

	// Wait for process to exit
	p.cmd.Wait()
	p.cmd = nil
	p.proxyURL = ""

	return nil
}

// Configure sends configuration to the proxy.
func (p *CapiProxy) Configure(filePath, workDir string) error {
	p.mu.Lock()
	url := p.proxyURL
	p.mu.Unlock()

	if url == "" {
		return fmt.Errorf("proxy not started")
	}

	config := fmt.Sprintf(`{"filePath":%q,"workDir":%q}`, filePath, workDir)
	resp, err := http.Post(url+"/config", "application/json", strings.NewReader(config))
	if err != nil {
		return fmt.Errorf("failed to configure proxy: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != 200 {
		return fmt.Errorf("proxy config failed with status %d", resp.StatusCode)
	}

	return nil
}

// GetExchanges retrieves the captured HTTP exchanges from the proxy.
func (p *CapiProxy) GetExchanges() ([]ParsedHttpExchange, error) {
	p.mu.Lock()
	url := p.proxyURL
	p.mu.Unlock()

	if url == "" {
		return nil, fmt.Errorf("proxy not started")
	}

	resp, err := http.Get(url + "/exchanges")
	if err != nil {
		return nil, fmt.Errorf("failed to get exchanges: %w", err)
	}
	defer resp.Body.Close()

	var exchanges []ParsedHttpExchange
	if err := json.NewDecoder(resp.Body).Decode(&exchanges); err != nil {
		return nil, fmt.Errorf("failed to decode exchanges: %w", err)
	}

	return exchanges, nil
}

// ParsedHttpExchange represents a captured HTTP exchange.
type ParsedHttpExchange struct {
	Request        ChatCompletionRequest      `json:"request"`
	Response       *ChatCompletionResponse    `json:"response,omitempty"`
	RequestHeaders map[string]json.RawMessage `json:"requestHeaders,omitempty"`
}

// ChatCompletionRequest represents an OpenAI chat completion request.
type ChatCompletionRequest struct {
	Model    string                  `json:"model"`
	Messages []ChatCompletionMessage `json:"messages"`
	Tools    []ChatCompletionTool    `json:"tools,omitempty"`
}

// ChatCompletionMessage represents a message in the chat completion request.
type ChatCompletionMessage struct {
	Role       string          `json:"role"`
	Content    string          `json:"content,omitempty"`
	RawContent json.RawMessage `json:"-"`
	ToolCallID string          `json:"tool_call_id,omitempty"`
	ToolCalls  []ToolCall      `json:"tool_calls,omitempty"`
}

// UnmarshalJSON handles Content being either a plain string or an array of
// content parts (e.g. multimodal messages with image_url entries).
func (m *ChatCompletionMessage) UnmarshalJSON(data []byte) error {
	type Alias ChatCompletionMessage
	aux := &struct {
		Content json.RawMessage `json:"content,omitempty"`
		*Alias
	}{
		Alias: (*Alias)(m),
	}
	if err := json.Unmarshal(data, aux); err != nil {
		return err
	}
	m.RawContent = aux.Content
	m.Content = ""
	if len(aux.Content) > 0 {
		var s string
		if json.Unmarshal(aux.Content, &s) == nil {
			m.Content = s
		}
	}
	return nil
}

// ToolCall represents a tool call in an assistant message.
type ToolCall struct {
	ID       string       `json:"id"`
	Type     string       `json:"type"`
	Function FunctionCall `json:"function"`
}

// FunctionCall represents the function details in a tool call.
type FunctionCall struct {
	Name      string `json:"name"`
	Arguments string `json:"arguments"`
}

// Message is an alias for ChatCompletionMessage for test convenience.
type Message = ChatCompletionMessage

// ChatCompletionTool represents a tool in the chat completion request.
type ChatCompletionTool struct {
	Type     string                     `json:"type"`
	Function ChatCompletionToolFunction `json:"function"`
}

// ChatCompletionToolFunction represents a function tool.
type ChatCompletionToolFunction struct {
	Name        string `json:"name"`
	Description string `json:"description,omitempty"`
}

// ChatCompletionResponse represents an OpenAI chat completion response.
type ChatCompletionResponse struct {
	ID      string                 `json:"id"`
	Model   string                 `json:"model"`
	Choices []ChatCompletionChoice `json:"choices"`
}

// ChatCompletionChoice represents a choice in the response.
type ChatCompletionChoice struct {
	Index        int                   `json:"index"`
	Message      ChatCompletionMessage `json:"message"`
	FinishReason string                `json:"finish_reason"`
}

// URL returns the proxy URL, or empty if not started.
func (p *CapiProxy) URL() string {
	p.mu.Lock()
	defer p.mu.Unlock()
	return p.proxyURL
}

// ProxyEnv returns environment variables that route HTTPS traffic through the CONNECT proxy.
func (p *CapiProxy) ProxyEnv() []string {
	p.mu.Lock()
	defer p.mu.Unlock()
	if p.connectProxyURL == "" || p.caFilePath == "" {
		return nil
	}

	noProxy := "127.0.0.1,localhost,::1"
	return []string{
		"HTTP_PROXY=" + p.connectProxyURL,
		"HTTPS_PROXY=" + p.connectProxyURL,
		"http_proxy=" + p.connectProxyURL,
		"https_proxy=" + p.connectProxyURL,
		"NO_PROXY=" + noProxy,
		"no_proxy=" + noProxy,
		"NODE_EXTRA_CA_CERTS=" + p.caFilePath,
		"SSL_CERT_FILE=" + p.caFilePath,
		"REQUESTS_CA_BUNDLE=" + p.caFilePath,
		"CURL_CA_BUNDLE=" + p.caFilePath,
		"GIT_SSL_CAINFO=" + p.caFilePath,
		"GH_TOKEN=",
		"GITHUB_TOKEN=",
		"GH_ENTERPRISE_TOKEN=",
		"GITHUB_ENTERPRISE_TOKEN=",
	}
}

// SetCopilotUserByToken registers a per-token user configuration on the proxy.
func (p *CapiProxy) SetCopilotUserByToken(token string, response map[string]interface{}) error {
	p.mu.Lock()
	url := p.proxyURL
	p.mu.Unlock()

	if url == "" {
		return fmt.Errorf("proxy not started")
	}

	body := map[string]interface{}{
		"token":    token,
		"response": response,
	}
	data, err := json.Marshal(body)
	if err != nil {
		return err
	}
	resp, err := http.Post(url+"/copilot-user-config", "application/json", bytes.NewReader(data))
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	if resp.StatusCode != 200 {
		return fmt.Errorf("setCopilotUserByToken: unexpected status %d", resp.StatusCode)
	}
	return nil
}
