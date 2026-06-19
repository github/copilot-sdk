/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package copilot

import (
	"bytes"
	"context"
	"io"
	"net/http"
	"strconv"
	"strings"
	"sync"

	"github.com/coder/websocket"
)

// Hop-by-hop and length headers the transport recomputes; forwarding them
// verbatim corrupts the request.
var forbiddenRequestHeaders = map[string]struct{}{
	"host":              {},
	"connection":        {},
	"content-length":    {},
	"transfer-encoding": {},
	"keep-alive":        {},
	"upgrade":           {},
	"proxy-connection":  {},
	"te":                {},
	"trailer":           {},
}

func isForbiddenRequestHeader(name string) bool {
	lower := strings.ToLower(name)
	if _, ok := forbiddenRequestHeaders[lower]; ok {
		return true
	}
	return strings.HasPrefix(lower, "sec-websocket-")
}

var sharedHTTPTransport = func() http.RoundTripper {
	t := http.DefaultTransport.(*http.Transport).Clone()
	t.DisableCompression = true
	return t
}()

// LlmRequestContext is the per-request context handed to every
// [LlmRequestHandler] seam.
type LlmRequestContext struct {
	RequestID string
	SessionID string
	Transport string
	URL       string
	Headers   http.Header
	// Context is cancelled when the runtime cancels this in-flight request.
	Context context.Context
}

// LlmWebSocketCloseStatus is the terminal status for a callback-owned WebSocket
// connection.
type LlmWebSocketCloseStatus struct {
	Description string
	Code        string
	Err         error
}

// LlmRequestHandler is the idiomatic base for consumers that observe or replace
// LLM inference requests. It implements [LlmInferenceProvider] by translating
// each request into Go's canonical net/http types.
//
// HTTP requests are forwarded through Transport (an [http.RoundTripper]); supply
// a custom RoundTripper to mutate the request, post-process the response, or
// replace the call entirely. WebSocket requests are serviced by OpenWebSocket;
// supply one to mutate the handshake or return a fully custom handler.
type LlmRequestHandler struct {
	// Transport forwards HTTP requests. When nil a shared default transport is
	// used. RoundTrip is called directly, so redirects are not followed.
	Transport http.RoundTripper
	// OpenWebSocket returns a per-connection WebSocket handler. When nil a
	// transparent forwarding connection to the request URL is opened.
	OpenWebSocket func(ctx *LlmRequestContext) (CopilotWebSocketHandler, error)
}

// OnLlmRequest implements [LlmInferenceProvider].
func (h *LlmRequestHandler) OnLlmRequest(req *LlmInferenceRequest) error {
	rctx := &LlmRequestContext{
		RequestID: req.RequestID,
		SessionID: req.SessionID,
		Transport: req.Transport,
		URL:       req.URL,
		Headers:   req.Headers,
		Context:   req.Context,
	}
	if req.Transport == "websocket" {
		return h.handleWebSocket(req, rctx)
	}
	return h.handleHTTP(req, rctx)
}

func (h *LlmRequestHandler) roundTripper() http.RoundTripper {
	if h.Transport != nil {
		return h.Transport
	}
	return sharedHTTPTransport
}

func (h *LlmRequestHandler) handleHTTP(req *LlmInferenceRequest, _ *LlmRequestContext) error {
	httpReq, err := buildHTTPRequest(req)
	if err != nil {
		return err
	}
	resp, err := h.roundTripper().RoundTrip(httpReq)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	return streamResponseToSink(resp, req)
}

func buildHTTPRequest(req *LlmInferenceRequest) (*http.Request, error) {
	body := drainBody(req.RequestBody)
	method := strings.ToUpper(req.Method)
	var bodyReader io.Reader
	if len(body) > 0 && method != http.MethodGet && method != http.MethodHead {
		bodyReader = bytes.NewReader(body)
	}
	httpReq, err := http.NewRequestWithContext(req.Context, method, req.URL, bodyReader)
	if err != nil {
		return nil, err
	}
	for name, values := range req.Headers {
		if isForbiddenRequestHeader(name) {
			continue
		}
		for _, v := range values {
			httpReq.Header.Add(name, v)
		}
	}
	return httpReq, nil
}

func drainBody(ch <-chan []byte) []byte {
	var buf bytes.Buffer
	for frame := range ch {
		buf.Write(frame)
	}
	return buf.Bytes()
}

func streamResponseToSink(resp *http.Response, req *LlmInferenceRequest) error {
	init := LlmInferenceResponseInit{
		Status:     resp.StatusCode,
		StatusText: statusText(resp),
		Headers:    cloneHeader(resp.Header),
	}
	if err := req.ResponseBody.Start(init); err != nil {
		return err
	}
	buf := make([]byte, 32*1024)
	for {
		n, readErr := resp.Body.Read(buf)
		if n > 0 {
			frame := make([]byte, n)
			copy(frame, buf[:n])
			if err := req.ResponseBody.WriteBinary(frame); err != nil {
				return err
			}
		}
		if readErr == io.EOF {
			break
		}
		if readErr != nil {
			return req.ResponseBody.Error(readErr.Error(), "")
		}
	}
	return req.ResponseBody.End()
}

func statusText(resp *http.Response) string {
	text := strings.TrimSpace(strings.TrimPrefix(resp.Status, strconv.Itoa(resp.StatusCode)))
	return text
}

func cloneHeader(h http.Header) http.Header {
	out := http.Header{}
	for k, vs := range h {
		out[k] = append([]string(nil), vs...)
	}
	return out
}

// WebSocketResponseWriter forwards upstream→runtime WebSocket messages back into
// the runtime response. A [CopilotWebSocketHandler] receives one in [CopilotWebSocketHandler.Open].
type WebSocketResponseWriter interface {
	// SendText forwards an upstream text message to the runtime.
	SendText(data []byte) error
	// SendBinary forwards an upstream binary message to the runtime.
	SendBinary(data []byte) error
}

// CopilotWebSocketHandler is a per-connection WebSocket handler returned by
// [LlmRequestHandler.OpenWebSocket]. The default implementation is
// [ForwardingWebSocketHandler]; a full transport replacement implements this
// interface directly.
type CopilotWebSocketHandler interface {
	// Open establishes the connection and starts forwarding upstream→runtime
	// messages into resp. It must not block. ctx is cancelled on teardown.
	Open(ctx context.Context, resp WebSocketResponseWriter) error
	// SendRequestMessage forwards one runtime→upstream message.
	SendRequestMessage(ctx context.Context, data []byte) error
	// Done is closed when the upstream connection completes (closed or errored).
	Done() <-chan struct{}
	// Err returns the terminal error after Done is closed, or nil on clean close.
	Err() error
	// Close tears down the connection.
	Close() error
}

func (h *LlmRequestHandler) handleWebSocket(req *LlmInferenceRequest, rctx *LlmRequestContext) error {
	var handler CopilotWebSocketHandler
	var err error
	if h.OpenWebSocket != nil {
		handler, err = h.OpenWebSocket(rctx)
	} else {
		handler = NewForwardingWebSocketHandler(rctx.URL, rctx.Headers)
	}
	if err != nil {
		return err
	}

	writer := &wsResponseWriter{sink: req.ResponseBody}
	if err := writer.start(); err != nil {
		return err
	}
	if err := handler.Open(req.Context, writer); err != nil {
		return writer.fail(err.Error(), "")
	}
	defer func() { _ = handler.Close() }()

	clientDone := make(chan struct{})
	go func() {
		defer close(clientDone)
		for {
			select {
			case frame, ok := <-req.RequestBody:
				if !ok {
					return
				}
				if err := handler.SendRequestMessage(req.Context, frame); err != nil {
					return
				}
			case <-req.Context.Done():
				return
			}
		}
	}()

	select {
	case <-handler.Done():
		if e := handler.Err(); e != nil {
			return writer.fail(e.Error(), "")
		}
		return writer.end()
	case <-clientDone:
		_ = handler.Close()
		<-handler.Done()
		if e := handler.Err(); e != nil {
			return writer.fail(e.Error(), "")
		}
		return writer.end()
	case <-req.Context.Done():
		return writer.fail("Request cancelled by runtime", "cancelled")
	}
}

// wsResponseWriter serialises WebSocket response writes into the sink.
type wsResponseWriter struct {
	mu        sync.Mutex
	sink      LlmInferenceResponseSink
	started   bool
	completed bool
}

func (w *wsResponseWriter) start() error {
	w.mu.Lock()
	defer w.mu.Unlock()
	if w.started {
		return nil
	}
	w.started = true
	return w.sink.Start(LlmInferenceResponseInit{Status: 101, Headers: http.Header{}})
}

func (w *wsResponseWriter) SendText(data []byte) error {
	w.mu.Lock()
	defer w.mu.Unlock()
	if w.completed {
		return nil
	}
	return w.sink.Write(data)
}

func (w *wsResponseWriter) SendBinary(data []byte) error {
	w.mu.Lock()
	defer w.mu.Unlock()
	if w.completed {
		return nil
	}
	return w.sink.WriteBinary(data)
}

func (w *wsResponseWriter) end() error {
	w.mu.Lock()
	defer w.mu.Unlock()
	if w.completed {
		return nil
	}
	w.completed = true
	return w.sink.End()
}

func (w *wsResponseWriter) fail(message string, code string) error {
	w.mu.Lock()
	defer w.mu.Unlock()
	if w.completed {
		return nil
	}
	w.completed = true
	return w.sink.Error(message, code)
}

// ForwardingWebSocketHandler is the default [CopilotWebSocketHandler]: it dials
// the real upstream and runs a receive loop forwarding upstream→runtime
// messages. Set OnSendRequestMessage / OnSendResponseMessage to observe,
// transform, or drop messages in either direction.
type ForwardingWebSocketHandler struct {
	URL     string
	Headers http.Header
	// OnSendRequestMessage observes or transforms each runtime→upstream frame.
	// Return nil to drop the frame.
	OnSendRequestMessage func(data []byte) []byte
	// OnSendResponseMessage observes or transforms each upstream→runtime frame.
	// Return nil to drop the frame.
	OnSendResponseMessage func(data []byte) []byte

	conn      *websocket.Conn
	resp      WebSocketResponseWriter
	done      chan struct{}
	err       error
	closeOnce sync.Once
}

// NewForwardingWebSocketHandler creates a forwarding handler targeting url with
// the given handshake headers.
func NewForwardingWebSocketHandler(url string, headers http.Header) *ForwardingWebSocketHandler {
	return &ForwardingWebSocketHandler{URL: url, Headers: headers, done: make(chan struct{})}
}

func (f *ForwardingWebSocketHandler) Open(ctx context.Context, resp WebSocketResponseWriter) error {
	f.resp = resp
	if f.done == nil {
		f.done = make(chan struct{})
	}
	opts := &websocket.DialOptions{HTTPHeader: f.dialHeaders()}
	conn, _, err := websocket.Dial(ctx, f.URL, opts)
	if err != nil {
		return err
	}
	conn.SetReadLimit(-1)
	f.conn = conn
	go f.receiveLoop(ctx)
	return nil
}

func (f *ForwardingWebSocketHandler) dialHeaders() http.Header {
	out := http.Header{}
	for name, values := range f.Headers {
		if isForbiddenRequestHeader(name) {
			continue
		}
		for _, v := range values {
			out.Add(name, v)
		}
	}
	return out
}

func (f *ForwardingWebSocketHandler) receiveLoop(ctx context.Context) {
	defer close(f.done)
	for {
		typ, data, err := f.conn.Read(ctx)
		if err != nil {
			if websocket.CloseStatus(err) == websocket.StatusNormalClosure || websocket.CloseStatus(err) == websocket.StatusGoingAway {
				f.err = nil
			} else if ctx.Err() != nil {
				f.err = nil
			} else {
				f.err = err
			}
			return
		}
		out := data
		if f.OnSendResponseMessage != nil {
			out = f.OnSendResponseMessage(data)
			if out == nil {
				continue
			}
		}
		if typ == websocket.MessageBinary {
			_ = f.resp.SendBinary(out)
		} else {
			_ = f.resp.SendText(out)
		}
	}
}

func (f *ForwardingWebSocketHandler) SendRequestMessage(ctx context.Context, data []byte) error {
	out := data
	if f.OnSendRequestMessage != nil {
		out = f.OnSendRequestMessage(data)
		if out == nil {
			return nil
		}
	}
	if f.conn == nil {
		return nil
	}
	return f.conn.Write(ctx, websocket.MessageText, out)
}

func (f *ForwardingWebSocketHandler) Done() <-chan struct{} { return f.done }

func (f *ForwardingWebSocketHandler) Err() error { return f.err }

func (f *ForwardingWebSocketHandler) Close() error {
	f.closeOnce.Do(func() {
		if f.conn != nil {
			_ = f.conn.Close(websocket.StatusNormalClosure, "")
		}
	})
	return nil
}
