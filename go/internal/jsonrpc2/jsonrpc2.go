package jsonrpc2

import (
	"crypto/rand"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"os"
	"reflect"
	"sync"
	"sync/atomic"
)

const version = "2.0"

// Standard JSON-RPC 2.0 error codes.
var (
	ErrParse          = &Error{Code: -32700, Message: "parse error"}
	ErrInvalidRequest = &Error{Code: -32600, Message: "invalid request"}
	ErrMethodNotFound = &Error{Code: -32601, Message: "method not found"}
	ErrInvalidParams  = &Error{Code: -32602, Message: "invalid params"}
	ErrInternal       = &Error{Code: -32603, Message: "internal error"}
)

// Error represents a JSON-RPC error response.
type Error struct {
	Code    int             `json:"code"`
	Message string          `json:"message"`
	Data    json.RawMessage `json:"data,omitempty"`
}

func (e *Error) Error() string {
	return fmt.Sprintf("JSON-RPC Error %d: %s", e.Code, e.Message)
}

// Request represents a JSON-RPC 2.0 request
type Request struct {
	JSONRPC string          `json:"jsonrpc"`
	ID      json.RawMessage `json:"id"` // nil for notifications
	Method  string          `json:"method"`
	Params  json.RawMessage `json:"params"`
}

func (r *Request) IsCall() bool {
	return len(r.ID) > 0
}

// Response represents a JSON-RPC 2.0 response
type Response struct {
	JSONRPC string          `json:"jsonrpc"`
	ID      json.RawMessage `json:"id,omitempty"`
	Result  json.RawMessage `json:"result,omitempty"`
	Error   *Error          `json:"error,omitempty"`
}

// NotificationHandler handles incoming notifications
type NotificationHandler func(method string, params json.RawMessage)

// RequestHandler handles incoming server requests and returns a result or error
type RequestHandler func(params json.RawMessage) (json.RawMessage, *Error)

// Client is a minimal JSON-RPC 2.0 client for stdio transport.
type Client struct {
	reader          *headerReader // reads frames from the remote side
	stdout          io.ReadCloser
	writer          chan *headerWriter // 1-buffered; holds the writer when not in use
	mu              sync.Mutex
	pendingRequests map[string]chan *Response
	requestHandlers map[string]RequestHandler
	running         atomic.Bool
	stopChan        chan struct{}
	wg              sync.WaitGroup
	processDone     chan struct{} // closed when the underlying process exits
	processError    error         // set before processDone is closed
	processErrorMu  sync.RWMutex  // protects processError
	onClose         func()        // called when the read loop exits unexpectedly
}

// NewClient creates a new JSON-RPC client.
func NewClient(stdin io.WriteCloser, stdout io.ReadCloser) *Client {
	c := &Client{
		reader:          newHeaderReader(stdout),
		stdout:          stdout,
		writer:          make(chan *headerWriter, 1),
		pendingRequests: make(map[string]chan *Response),
		requestHandlers: make(map[string]RequestHandler),
		stopChan:        make(chan struct{}),
	}
	c.writer <- newHeaderWriter(stdin)
	return c
}

// SetProcessDone sets a channel that will be closed when the process exits,
// and stores the error that should be returned to pending/future requests.
func (c *Client) SetProcessDone(done chan struct{}, errPtr *error) {
	c.processDone = done
	// Monitor the channel and copy the error when it closes
	go func() {
		<-done
		if errPtr != nil {
			c.processErrorMu.Lock()
			c.processError = *errPtr
			c.processErrorMu.Unlock()
		}
	}()
}

// getProcessError returns the process exit error if the process has exited
func (c *Client) getProcessError() error {
	c.processErrorMu.RLock()
	defer c.processErrorMu.RUnlock()
	return c.processError
}

// Start begins listening for messages in a background goroutine
func (c *Client) Start() {
	c.running.Store(true)
	c.wg.Add(1)
	go c.readLoop()
}

// Stop stops the client and cleans up
func (c *Client) Stop() {
	if !c.running.Load() {
		return
	}
	c.running.Store(false)
	close(c.stopChan)

	// Close stdout to unblock the readLoop
	if c.stdout != nil {
		c.stdout.Close()
	}

	c.wg.Wait()
}

func NotificationHandlerFor[In any](handler func(params In)) RequestHandler {
	return func(params json.RawMessage) (json.RawMessage, *Error) {
		var in In
		// If In is a pointer type, allocate the underlying value and unmarshal into it directly
		var target any = &in
		if t := reflect.TypeFor[In](); t.Kind() == reflect.Pointer {
			in = reflect.New(t.Elem()).Interface().(In)
			target = in
		}
		if err := json.Unmarshal(params, target); err != nil {
			return nil, &Error{
				Code:    ErrInvalidParams.Code,
				Message: fmt.Sprintf("Invalid params: %v", err),
			}
		}
		handler(in)
		return nil, nil
	}
}

// RequestHandlerFor creates a RequestHandler from a typed function
func RequestHandlerFor[In, Out any](handler func(params In) (Out, *Error)) RequestHandler {
	return func(params json.RawMessage) (json.RawMessage, *Error) {
		var in In
		// If In is a pointer type, allocate the underlying value and unmarshal into it directly
		var target any = &in
		if t := reflect.TypeOf(in); t != nil && t.Kind() == reflect.Pointer {
			in = reflect.New(t.Elem()).Interface().(In)
			target = in
		}
		if err := json.Unmarshal(params, target); err != nil {
			return nil, &Error{
				Code:    ErrInvalidParams.Code,
				Message: fmt.Sprintf("Invalid params: %v", err),
			}
		}
		out, errj := handler(in)
		if errj != nil {
			return nil, errj
		}
		outData, err := json.Marshal(out)
		if err != nil {
			return nil, &Error{
				Code:    ErrInternal.Code,
				Message: fmt.Sprintf("Failed to marshal response: %v", err),
			}
		}
		return outData, nil
	}
}

// SetRequestHandler registers a handler for incoming requests from the server
func (c *Client) SetRequestHandler(method string, handler RequestHandler) {
	c.mu.Lock()
	defer c.mu.Unlock()
	if handler == nil {
		delete(c.requestHandlers, method)
		return
	}
	c.requestHandlers[method] = handler
}

// Request sends a JSON-RPC request and waits for the response
func (c *Client) Request(method string, params any) (json.RawMessage, error) {
	requestID := generateUUID()

	// Create response channel
	responseChan := make(chan *Response, 1)
	c.mu.Lock()
	c.pendingRequests[requestID] = responseChan
	c.mu.Unlock()

	// Clean up on exit
	defer func() {
		c.mu.Lock()
		delete(c.pendingRequests, requestID)
		c.mu.Unlock()
	}()

	// Check if process already exited before sending
	if c.processDone != nil {
		select {
		case <-c.processDone:
			if err := c.getProcessError(); err != nil {
				return nil, err
			}
			return nil, fmt.Errorf("process exited unexpectedly")
		default:
			// Process still running, continue
		}
	}

	var paramsData json.RawMessage
	if params == nil {
		paramsData = json.RawMessage("{}")
	} else {
		var err error
		paramsData, err = json.Marshal(params)
		if err != nil {
			return nil, fmt.Errorf("failed to marshal params: %w", err)
		}
	}

	// Send request
	request := Request{
		JSONRPC: version,
		ID:      json.RawMessage(`"` + requestID + `"`),
		Method:  method,
		Params:  paramsData,
	}

	if err := c.sendMessage(request); err != nil {
		return nil, fmt.Errorf("failed to send request: %w", err)
	}

	// Wait for response, also checking for process exit
	if c.processDone != nil {
		select {
		case response := <-responseChan:
			if response.Error != nil {
				return nil, response.Error
			}
			return response.Result, nil
		case <-c.processDone:
			if err := c.getProcessError(); err != nil {
				return nil, err
			}
			return nil, fmt.Errorf("process exited unexpectedly")
		case <-c.stopChan:
			return nil, fmt.Errorf("client stopped")
		}
	}
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

// sendMessage writes a message to the stream.
// Write serialization is achieved via a 1-buffered channel that holds the
// writer when not in use, avoiding the need for a mutex on the write path.
func (c *Client) sendMessage(message any) error {
	data, err := json.Marshal(message)
	if err != nil {
		return fmt.Errorf("failed to marshal message: %w", err)
	}

	w := <-c.writer
	defer func() { c.writer <- w }()
	return w.Write(data)
}

// SetOnClose sets a callback invoked when the read loop exits unexpectedly
// (e.g. the underlying connection or process was lost).
func (c *Client) SetOnClose(fn func()) {
	c.onClose = fn
}

// readLoop reads messages from the stream in a background goroutine.
func (c *Client) readLoop() {
	defer c.wg.Done()
	defer func() {
		// If still running, the read loop exited unexpectedly (process died or
		// connection dropped). Notify the caller so it can update its state.
		if c.onClose != nil && c.running.Load() {
			c.onClose()
		}
	}()

	for c.running.Load() {
		// Read the next frame.
		data, err := c.reader.Read()
		if err != nil {
			if !(errors.Is(err, io.EOF) || errors.Is(err, io.ErrClosedPipe) || errors.Is(err, os.ErrClosed)) && c.running.Load() {
				fmt.Printf("Error reading message: %v\n", err)
			}
			return
		}

		// Decode using a single unmarshal into the combined wire format.
		msg, err := decodeMessage(data)
		if err != nil {
			if c.running.Load() {
				fmt.Printf("Error decoding message: %v\n", err)
			}
			continue
		}

		switch msg := msg.(type) {
		case *Request:
			c.handleRequest(msg)
		case *Response:
			c.handleResponse(msg)
		}
	}
}

// handleResponse dispatches a response to the waiting request
func (c *Client) handleResponse(response *Response) {
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

func (c *Client) handleRequest(request *Request) {
	c.mu.Lock()
	handler := c.requestHandlers[request.Method]
	c.mu.Unlock()

	if handler == nil {
		if request.IsCall() {
			c.sendErrorResponse(request.ID, &Error{
				Code:    ErrMethodNotFound.Code,
				Message: fmt.Sprintf("Method not found: %s", request.Method),
			})
		}
		return
	}

	// Notifications run synchronously, calls run in a goroutine to avoid blocking
	if !request.IsCall() {
		handler(request.Params)
		return
	}

	go func() {
		defer func() {
			if r := recover(); r != nil {
				c.sendErrorResponse(request.ID, &Error{
					Code:    ErrInternal.Code,
					Message: fmt.Sprintf("request handler panic: %v", r),
				})
			}
		}()

		result, err := handler(request.Params)
		if err != nil {
			c.sendErrorResponse(request.ID, err)
			return
		}
		c.sendResponse(request.ID, result)
	}()
}

func (c *Client) sendResponse(id json.RawMessage, result json.RawMessage) {
	response := Response{
		JSONRPC: version,
		ID:      id,
		Result:  result,
	}
	if err := c.sendMessage(response); err != nil {
		fmt.Printf("Failed to send JSON-RPC response: %v\n", err)
	}
}

func (c *Client) sendErrorResponse(id json.RawMessage, rpcErr *Error) {
	response := Response{
		JSONRPC: version,
		ID:      id,
		Error:   rpcErr,
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

// decodeMessage decodes a JSON-RPC message from raw bytes, returning either
// a *Request or a *Response.
func decodeMessage(data []byte) (any, error) {
	// msg contains all fields of both Request and Response.
	var msg struct {
		JSONRPC string          `json:"jsonrpc"`
		ID      json.RawMessage `json:"id,omitempty"`
		Method  string          `json:"method,omitempty"`
		Params  json.RawMessage `json:"params,omitempty"`
		Result  json.RawMessage `json:"result,omitempty"`
		Error   *Error          `json:"error,omitempty"`
	}
	if err := json.Unmarshal(data, &msg); err != nil {
		return nil, fmt.Errorf("unmarshaling jsonrpc message: %w", err)
	}
	if msg.JSONRPC != version {
		return nil, fmt.Errorf("unsupported JSON-RPC version %q; expected %q", msg.JSONRPC, version)
	}
	if msg.Method != "" {
		return &Request{
			JSONRPC: msg.JSONRPC,
			ID:      msg.ID,
			Method:  msg.Method,
			Params:  msg.Params,
		}, nil
	}
	if len(msg.ID) > 0 {
		return &Response{
			JSONRPC: msg.JSONRPC,
			ID:      msg.ID,
			Result:  msg.Result,
			Error:   msg.Error,
		}, nil
	}
	return nil, fmt.Errorf("message is neither a request nor a response: %w", ErrInvalidRequest)
}
