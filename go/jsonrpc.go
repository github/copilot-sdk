package copilot

import (
	"bufio"
	"crypto/rand"
	"encoding/json"
	"fmt"
	"io"
	"sync"
)

// JSONRPCError represents a JSON-RPC error response
type JSONRPCError struct {
	Code    int                    `json:"code"`
	Message string                 `json:"message"`
	Data    map[string]interface{} `json:"data,omitempty"`
}

func (e *JSONRPCError) Error() string {
	return fmt.Sprintf("JSON-RPC Error %d: %s", e.Code, e.Message)
}

// JSONRPCRequest represents a JSON-RPC 2.0 request
type JSONRPCRequest struct {
	JSONRPC string                 `json:"jsonrpc"`
	ID      json.RawMessage        `json:"id"`
	Method  string                 `json:"method"`
	Params  map[string]interface{} `json:"params"`
}

// JSONRPCResponse represents a JSON-RPC 2.0 response
type JSONRPCResponse struct {
	JSONRPC string                 `json:"jsonrpc"`
	ID      json.RawMessage        `json:"id,omitempty"`
	Result  map[string]interface{} `json:"result,omitempty"`
	Error   *JSONRPCError          `json:"error,omitempty"`
}

// JSONRPCNotification represents a JSON-RPC 2.0 notification
type JSONRPCNotification struct {
	JSONRPC string                 `json:"jsonrpc"`
	Method  string                 `json:"method"`
	Params  map[string]interface{} `json:"params"`
}

// NotificationHandler handles incoming notifications
type NotificationHandler func(method string, params map[string]interface{})

// RequestHandler handles incoming server requests and returns a result or error
type RequestHandler func(params map[string]interface{}) (map[string]interface{}, *JSONRPCError)

// JSONRPCClient is a minimal JSON-RPC 2.0 client for stdio transport
type JSONRPCClient struct {
	stdin               io.WriteCloser
	stdout              io.ReadCloser
	mu                  sync.Mutex
	pendingRequests     map[string]chan *JSONRPCResponse
	notificationHandler NotificationHandler
	requestHandlers     map[string]RequestHandler
	running             bool
	stopChan            chan struct{}
	wg                  sync.WaitGroup
}

// NewJSONRPCClient creates a new JSON-RPC client
func NewJSONRPCClient(stdin io.WriteCloser, stdout io.ReadCloser) *JSONRPCClient {
	return &JSONRPCClient{
		stdin:           stdin,
		stdout:          stdout,
		pendingRequests: make(map[string]chan *JSONRPCResponse),
		requestHandlers: make(map[string]RequestHandler),
		stopChan:        make(chan struct{}),
	}
}

// Start begins listening for messages in a background goroutine
func (c *JSONRPCClient) Start() {
	c.running = true
	c.wg.Add(1)
	go c.readLoop()
}

// Stop stops the client and cleans up
func (c *JSONRPCClient) Stop() {
	if !c.running {
		return
	}
	c.running = false
	close(c.stopChan)

	// Close stdout to unblock the readLoop
	if c.stdout != nil {
		c.stdout.Close()
	}

	c.wg.Wait()
}

// SetNotificationHandler sets the handler for incoming notifications
func (c *JSONRPCClient) SetNotificationHandler(handler NotificationHandler) {
	c.mu.Lock()
	defer c.mu.Unlock()
	c.notificationHandler = handler
}

// SetRequestHandler registers a handler for incoming requests from the server
func (c *JSONRPCClient) SetRequestHandler(method string, handler RequestHandler) {
	c.mu.Lock()
	defer c.mu.Unlock()
	if handler == nil {
		delete(c.requestHandlers, method)
		return
	}
	c.requestHandlers[method] = handler
}

// Request sends a JSON-RPC request and waits for the response
func (c *JSONRPCClient) Request(method string, params map[string]interface{}) (map[string]interface{}, error) {
	requestID := generateUUID()

	// Create response channel
	responseChan := make(chan *JSONRPCResponse, 1)
	c.mu.Lock()
	c.pendingRequests[requestID] = responseChan
	c.mu.Unlock()

	// Clean up on exit
	defer func() {
		c.mu.Lock()
		delete(c.pendingRequests, requestID)
		c.mu.Unlock()
	}()

	// Send request
	request := JSONRPCRequest{
		JSONRPC: "2.0",
		ID:      json.RawMessage(`"` + requestID + `"`),
		Method:  method,
		Params:  params,
	}

	if err := c.sendMessage(request); err != nil {
		return nil, fmt.Errorf("failed to send request: %w", err)
	}

	// Wait for response
	select {
	case response := <-responseChan:
		if response.Error != nil {
			return nil, response.Error
		}
		return response.Result, nil
	case <-c.stopChan:
		return nil, fmt.Errorf("client stopped")
	}
}

// Notify sends a JSON-RPC notification (no response expected)
func (c *JSONRPCClient) Notify(method string, params map[string]interface{}) error {
	notification := JSONRPCNotification{
		JSONRPC: "2.0",
		Method:  method,
		Params:  params,
	}
	return c.sendMessage(notification)
}

// sendMessage writes a message to stdin
func (c *JSONRPCClient) sendMessage(message interface{}) error {
	data, err := json.Marshal(message)
	if err != nil {
		return fmt.Errorf("failed to marshal message: %w", err)
	}

	c.mu.Lock()
	defer c.mu.Unlock()

	// Write Content-Length header + message
	header := fmt.Sprintf("Content-Length: %d\r\n\r\n", len(data))
	if _, err := c.stdin.Write([]byte(header)); err != nil {
		return fmt.Errorf("failed to write header: %w", err)
	}
	if _, err := c.stdin.Write(data); err != nil {
		return fmt.Errorf("failed to write message: %w", err)
	}

	return nil
}

// readLoop reads messages from stdout in a background goroutine
func (c *JSONRPCClient) readLoop() {
	defer c.wg.Done()

	reader := bufio.NewReader(c.stdout)

	for c.running {
		// Read Content-Length header
		var contentLength int
		for {
			line, err := reader.ReadString('\n')
			if err != nil {
				// Only log unexpected errors (not EOF or closed pipe during shutdown)
				if err != io.EOF && c.running {
					fmt.Printf("Error reading header: %v\n", err)
				}
				return
			}

			// Check for blank line (end of headers)
			if line == "\r\n" || line == "\n" {
				break
			}

			// Parse Content-Length
			var length int
			if _, err := fmt.Sscanf(line, "Content-Length: %d", &length); err == nil {
				contentLength = length
			}
		}

		if contentLength == 0 {
			continue
		}

		// Read message body
		body := make([]byte, contentLength)
		if _, err := io.ReadFull(reader, body); err != nil {
			fmt.Printf("Error reading body: %v\n", err)
			return
		}

		// Try to parse as request first (has both ID and Method)
		var request JSONRPCRequest
		if err := json.Unmarshal(body, &request); err == nil && request.Method != "" && len(request.ID) > 0 {
			c.handleRequest(&request)
			continue
		}

		// Try to parse as response (has ID but no Method)
		var response JSONRPCResponse
		if err := json.Unmarshal(body, &response); err == nil && len(response.ID) > 0 {
			c.handleResponse(&response)
			continue
		}

		// Try to parse as notification (has Method but no ID)
		var notification JSONRPCNotification
		if err := json.Unmarshal(body, &notification); err == nil && notification.Method != "" {
			c.handleNotification(&notification)
			continue
		}
	}
}

// handleResponse dispatches a response to the waiting request
func (c *JSONRPCClient) handleResponse(response *JSONRPCResponse) {
	var id string
	if err := json.Unmarshal(response.ID, &id); err != nil {
		return // ignore responses with non-string IDs
	}
	c.mu.Lock()
	responseChan, ok := c.pendingRequests[id]
	c.mu.Unlock()

	if ok {
		select {
		case responseChan <- response:
		default:
		}
	}
}

// handleNotification dispatches a notification to the handler
func (c *JSONRPCClient) handleNotification(notification *JSONRPCNotification) {
	c.mu.Lock()
	handler := c.notificationHandler
	c.mu.Unlock()

	if handler != nil {
		handler(notification.Method, notification.Params)
	}
}

func (c *JSONRPCClient) handleRequest(request *JSONRPCRequest) {
	c.mu.Lock()
	handler := c.requestHandlers[request.Method]
	c.mu.Unlock()

	if handler == nil {
		c.sendErrorResponse(request.ID, -32601, fmt.Sprintf("Method not found: %s", request.Method), nil)
		return
	}

	go func() {
		defer func() {
			if r := recover(); r != nil {
				c.sendErrorResponse(request.ID, -32603, fmt.Sprintf("request handler panic: %v", r), nil)
			}
		}()

		result, err := handler(request.Params)
		if err != nil {
			c.sendErrorResponse(request.ID, err.Code, err.Message, err.Data)
			return
		}
		if result == nil {
			result = make(map[string]interface{})
		}
		c.sendResponse(request.ID, result)
	}()
}

func (c *JSONRPCClient) sendResponse(id json.RawMessage, result map[string]interface{}) {
	response := JSONRPCResponse{
		JSONRPC: "2.0",
		ID:      id,
		Result:  result,
	}
	if err := c.sendMessage(response); err != nil {
		fmt.Printf("Failed to send JSON-RPC response: %v\n", err)
	}
}

func (c *JSONRPCClient) sendErrorResponse(id json.RawMessage, code int, message string, data map[string]interface{}) {
	response := JSONRPCResponse{
		JSONRPC: "2.0",
		ID:      id,
		Error: &JSONRPCError{
			Code:    code,
			Message: message,
			Data:    data,
		},
	}
	if err := c.sendMessage(response); err != nil {
		fmt.Printf("Failed to send JSON-RPC error response: %v\n", err)
	}
}

// generateUUID generates a simple UUID v4 without external dependencies
func generateUUID() string {
	b := make([]byte, 16)
	rand.Read(b)
	b[6] = (b[6] & 0x0f) | 0x40 // Version 4
	b[8] = (b[8] & 0x3f) | 0x80 // Variant is 10
	return fmt.Sprintf("%x-%x-%x-%x-%x", b[0:4], b[4:6], b[6:8], b[8:10], b[10:])
}

func init() {

}
