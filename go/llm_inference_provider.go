/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package copilot

import (
	"context"
	"encoding/base64"
	"fmt"
	"net/http"
	"sync"

	"github.com/github/copilot-sdk/go/rpc"
)

// LlmInferenceRequest is an outbound model-layer request the runtime is asking
// the SDK consumer to service on its behalf.
//
// It is a low-level shape: URL / method / headers verbatim, the request body
// delivered as a stream of frames, and the response written through
// ResponseBody. The runtime does not classify the request (no provider type,
// endpoint kind, or wire API); consumers that need that information derive it
// from the URL and headers. For the idiomatic [net/http] view, use
// [LlmRequestHandler] instead of implementing [LlmInferenceProvider] directly.
type LlmInferenceRequest struct {
	// RequestID is an opaque runtime-minted id, stable across the request lifecycle.
	RequestID string
	// SessionID is the id of the runtime session that triggered this request, or
	// empty when the request was issued outside any session (for example the
	// startup model catalog).
	SessionID string
	// Method is the HTTP method (GET, POST, ...).
	Method string
	// URL is the absolute request URL.
	URL string
	// Headers are the request headers, multi-valued.
	Headers http.Header
	// Transport is the transport the runtime would otherwise use: "http" (the
	// default, covering plain HTTP and SSE) or "websocket" (a full-duplex
	// message channel where each RequestBody frame is one inbound message and
	// each ResponseBody write is one outbound message).
	Transport string
	// RequestBody yields request body frames as they arrive from the runtime.
	// The channel is closed when the body ends or the request is cancelled;
	// check Context.Err() to distinguish a clean end from a cancellation.
	RequestBody <-chan []byte
	// Context is cancelled when the runtime cancels this in-flight request (for
	// example because the agent turn was aborted upstream). Pass it to the
	// outbound call so the upstream is torn down too.
	Context context.Context
	// ResponseBody is the sink the consumer writes the upstream response into.
	// Call Start exactly once before writing body frames, then zero or more
	// Write/WriteBinary calls, and finish with End or Error.
	ResponseBody LlmInferenceResponseSink
}

// LlmInferenceResponseInit is the response head passed to
// [LlmInferenceResponseSink.Start].
type LlmInferenceResponseInit struct {
	Status     int
	StatusText string
	Headers    http.Header
}

// LlmInferenceResponseSink is the sink a consumer writes an upstream response
// into. The state machine is strict: Start once, then zero or more
// Write/WriteBinary, then exactly one of End or Error. Calling out of order
// returns an error.
type LlmInferenceResponseSink interface {
	// Start sends the response head (status + headers) back to the runtime.
	Start(init LlmInferenceResponseInit) error
	// Write sends a body frame as UTF-8 text (the common case for JSON / SSE).
	Write(data []byte) error
	// WriteBinary sends a body frame as binary (base64 on the wire).
	WriteBinary(data []byte) error
	// End marks end-of-stream cleanly.
	End() error
	// Error marks end-of-stream with a transport-level failure. code is optional.
	Error(message string, code string) error
}

// LlmInferenceProvider is the low-level registration seam. The SDK consumer
// implements OnLlmRequest; the same callback handles both buffered and
// streaming responses by calling ResponseBody.Write zero or more times before
// End. Most consumers should embed or use [LlmRequestHandler] instead, which
// exposes idiomatic [net/http] request/response seams.
type LlmInferenceProvider interface {
	// OnLlmRequest is called once per outbound model-layer request the consumer
	// has opted to handle. The consumer must eventually call ResponseBody.End or
	// ResponseBody.Error; returning a non-nil error surfaces a transport-level
	// failure to the runtime (equivalent to ResponseBody.Error when Start has
	// not yet been called).
	OnLlmRequest(req *LlmInferenceRequest) error
}

// LlmInferenceConfig configures a connection-level LLM inference callback. When
// set on [ClientOptions], the client registers as the inference provider on
// connect, and the runtime routes its model-layer HTTP and WebSocket traffic
// through Handler instead of issuing the calls itself.
type LlmInferenceConfig struct {
	// Handler services intercepted requests. Use a [*LlmRequestHandler] for the
	// idiomatic net/http view, or any type implementing [LlmInferenceProvider]
	// for full low-level control.
	Handler LlmInferenceProvider
}

// frameQueue is an unbounded FIFO of body frames, decoupling the RPC dispatch
// goroutine (which only pushes) from the consumer goroutine (which pops).
type frameQueue struct {
	mu    sync.Mutex
	cond  *sync.Cond
	items [][]byte
	done  bool
}

func newFrameQueue() *frameQueue {
	q := &frameQueue{}
	q.cond = sync.NewCond(&q.mu)
	return q
}

func (q *frameQueue) push(b []byte) {
	q.mu.Lock()
	if !q.done {
		q.items = append(q.items, b)
	}
	q.cond.Signal()
	q.mu.Unlock()
}

func (q *frameQueue) close() {
	q.mu.Lock()
	q.done = true
	q.cond.Broadcast()
	q.mu.Unlock()
}

func (q *frameQueue) pop() ([]byte, bool) {
	q.mu.Lock()
	defer q.mu.Unlock()
	for len(q.items) == 0 && !q.done {
		q.cond.Wait()
	}
	if len(q.items) > 0 {
		b := q.items[0]
		q.items = q.items[1:]
		return b, true
	}
	return nil, false
}

type llmPendingState struct {
	mu        sync.Mutex
	queue     *frameQueue
	ctx       context.Context
	cancel    context.CancelFunc
	started   bool
	finished  bool
	cancelled bool
}

type llmInferenceAdapter struct {
	handler LlmInferenceProvider
	getRPC  func() *rpc.ServerLlmInferenceAPI

	mu      sync.Mutex
	pending map[string]*llmPendingState
	// staged buffers chunks that arrive before their start frame — a reordering
	// the runtime's ordered dispatch should make impossible, drained the moment
	// the matching start frame registers so a body byte is never dropped.
	staged map[string][]*rpc.LlmInferenceHTTPRequestChunkRequest
}

// newLlmInferenceAdapter adapts an [LlmInferenceProvider] into the generated
// rpc.LlmInferenceHandler consumed by the SDK's RPC dispatcher.
func newLlmInferenceAdapter(handler LlmInferenceProvider, getRPC func() *rpc.ServerLlmInferenceAPI) rpc.LlmInferenceHandler {
	return &llmInferenceAdapter{
		handler: handler,
		getRPC:  getRPC,
		pending: make(map[string]*llmPendingState),
		staged:  make(map[string][]*rpc.LlmInferenceHTTPRequestChunkRequest),
	}
}

func (a *llmInferenceAdapter) HttpRequestStart(params *rpc.LlmInferenceHTTPRequestStartRequest) (*rpc.LlmInferenceHTTPRequestStartResult, error) {
	ctx, cancel := context.WithCancel(context.Background())
	queue := newFrameQueue()
	bodyCh := make(chan []byte)
	state := &llmPendingState{queue: queue, ctx: ctx, cancel: cancel}

	go func() {
		defer close(bodyCh)
		for {
			b, ok := queue.pop()
			if !ok {
				return
			}
			select {
			case bodyCh <- b:
			case <-ctx.Done():
				return
			}
		}
	}()

	a.mu.Lock()
	a.pending[params.RequestID] = state
	staged := a.staged[params.RequestID]
	delete(a.staged, params.RequestID)
	a.mu.Unlock()

	for _, chunk := range staged {
		a.routeChunk(state, chunk)
	}

	transport := "http"
	if params.Transport != nil {
		transport = string(*params.Transport)
	}
	sessionID := ""
	if params.SessionID != nil {
		sessionID = *params.SessionID
	}
	headers := http.Header{}
	for k, v := range params.Headers {
		headers[k] = append([]string(nil), v...)
	}
	sink := &llmResponseSink{requestID: params.RequestID, adapter: a, state: state}
	req := &LlmInferenceRequest{
		RequestID:    params.RequestID,
		SessionID:    sessionID,
		Method:       params.Method,
		URL:          params.URL,
		Headers:      headers,
		Transport:    transport,
		RequestBody:  bodyCh,
		Context:      ctx,
		ResponseBody: sink,
	}
	go a.runHandler(req, sink, state)
	return &rpc.LlmInferenceHTTPRequestStartResult{}, nil
}

func (a *llmInferenceAdapter) HttpRequestChunk(params *rpc.LlmInferenceHTTPRequestChunkRequest) (*rpc.LlmInferenceHTTPRequestChunkResult, error) {
	a.mu.Lock()
	state := a.pending[params.RequestID]
	if state == nil {
		a.staged[params.RequestID] = append(a.staged[params.RequestID], params)
		a.mu.Unlock()
		return &rpc.LlmInferenceHTTPRequestChunkResult{}, nil
	}
	a.mu.Unlock()
	a.routeChunk(state, params)
	return &rpc.LlmInferenceHTTPRequestChunkResult{}, nil
}

func (a *llmInferenceAdapter) routeChunk(state *llmPendingState, params *rpc.LlmInferenceHTTPRequestChunkRequest) {
	if params.Cancel != nil && *params.Cancel {
		state.mu.Lock()
		state.cancelled = true
		state.mu.Unlock()
		state.cancel()
		state.queue.close()
		return
	}
	if params.Data != "" {
		binary := params.Binary != nil && *params.Binary
		if data, err := decodeChunkData(params.Data, binary); err == nil {
			state.queue.push(data)
		}
	}
	if params.End != nil && *params.End {
		state.queue.close()
	}
}

func (a *llmInferenceAdapter) runHandler(req *LlmInferenceRequest, sink *llmResponseSink, state *llmPendingState) {
	err := a.handler.OnLlmRequest(req)
	state.mu.Lock()
	finished := state.finished
	cancelled := state.cancelled
	state.mu.Unlock()
	if err != nil {
		if cancelled || state.ctx.Err() != nil {
			a.finishCancelled(sink, state)
			return
		}
		a.failViaSink(sink, state, err.Error())
		return
	}
	if !finished {
		a.failViaSink(sink, state, "LLM inference provider returned without finalising the response (call ResponseBody.End() or .Error())")
	}
}

func (a *llmInferenceAdapter) failViaSink(sink *llmResponseSink, state *llmPendingState, message string) {
	state.mu.Lock()
	finished := state.finished
	started := state.started
	state.mu.Unlock()
	if finished {
		return
	}
	if !started {
		_ = sink.Start(LlmInferenceResponseInit{Status: 502, Headers: http.Header{}})
	}
	_ = sink.Error(message, "")
}

func (a *llmInferenceAdapter) finishCancelled(sink *llmResponseSink, state *llmPendingState) {
	state.mu.Lock()
	finished := state.finished
	started := state.started
	state.mu.Unlock()
	if finished {
		return
	}
	if !started {
		_ = sink.Start(LlmInferenceResponseInit{Status: 499, Headers: http.Header{}})
	}
	_ = sink.Error("Request cancelled by runtime", "cancelled")
}

func (a *llmInferenceAdapter) removePending(requestID string) {
	a.mu.Lock()
	delete(a.pending, requestID)
	a.mu.Unlock()
}

func decodeChunkData(data string, binary bool) ([]byte, error) {
	if binary {
		return base64.StdEncoding.DecodeString(data)
	}
	return []byte(data), nil
}

type llmResponseSink struct {
	requestID string
	adapter   *llmInferenceAdapter
	state     *llmPendingState
}

func (s *llmResponseSink) rpcAPI() (*rpc.ServerLlmInferenceAPI, error) {
	r := s.adapter.getRPC()
	if r == nil {
		return nil, fmt.Errorf("LLM inference response sink used after RPC connection closed")
	}
	return r, nil
}

// rejectedByRuntime is invoked when the runtime acknowledges a response frame
// with accepted=false, meaning it has dropped the request (for example because
// it cancelled). It aborts the consumer's upstream work and stops emitting.
func (s *llmResponseSink) rejectedByRuntime() error {
	s.state.mu.Lock()
	if !s.state.cancelled {
		s.state.cancelled = true
		s.state.cancel()
	}
	s.state.finished = true
	s.state.mu.Unlock()
	s.adapter.removePending(s.requestID)
	return fmt.Errorf("LLM inference response was rejected by the runtime (request no longer active)")
}

func (s *llmResponseSink) Start(init LlmInferenceResponseInit) error {
	s.state.mu.Lock()
	if s.state.started {
		s.state.mu.Unlock()
		return fmt.Errorf("LLM inference response sink Start() called twice")
	}
	if s.state.finished {
		s.state.mu.Unlock()
		return fmt.Errorf("LLM inference response sink already finished")
	}
	s.state.started = true
	s.state.mu.Unlock()

	api, err := s.rpcAPI()
	if err != nil {
		return err
	}
	var statusText *string
	if init.StatusText != "" {
		st := init.StatusText
		statusText = &st
	}
	headers := map[string][]string(init.Headers)
	if headers == nil {
		headers = map[string][]string{}
	}
	result, err := api.HttpResponseStart(context.Background(), &rpc.LlmInferenceHTTPResponseStartRequest{
		RequestID:  s.requestID,
		Status:     int64(init.Status),
		StatusText: statusText,
		Headers:    headers,
	})
	if err != nil {
		return err
	}
	if !result.Accepted {
		return s.rejectedByRuntime()
	}
	return nil
}

func (s *llmResponseSink) Write(data []byte) error {
	return s.write(string(data), false)
}

func (s *llmResponseSink) WriteBinary(data []byte) error {
	return s.write(base64.StdEncoding.EncodeToString(data), true)
}

func (s *llmResponseSink) write(data string, binary bool) error {
	s.state.mu.Lock()
	cancelled := s.state.cancelled
	started := s.state.started
	finished := s.state.finished
	s.state.mu.Unlock()
	if cancelled {
		return fmt.Errorf("LLM inference request was cancelled by the runtime")
	}
	if !started {
		return fmt.Errorf("LLM inference response sink Write() called before Start()")
	}
	if finished {
		return fmt.Errorf("LLM inference response sink Write() called after End()/Error()")
	}
	api, err := s.rpcAPI()
	if err != nil {
		return err
	}
	end := false
	chunk := &rpc.LlmInferenceHTTPResponseChunkRequest{
		RequestID: s.requestID,
		Data:      data,
		End:       &end,
	}
	if binary {
		b := true
		chunk.Binary = &b
	}
	result, err := api.HttpResponseChunk(context.Background(), chunk)
	if err != nil {
		return err
	}
	if !result.Accepted {
		return s.rejectedByRuntime()
	}
	return nil
}

func (s *llmResponseSink) End() error {
	s.state.mu.Lock()
	if s.state.finished {
		s.state.mu.Unlock()
		return nil
	}
	s.state.finished = true
	s.state.mu.Unlock()
	s.adapter.removePending(s.requestID)
	api, err := s.rpcAPI()
	if err != nil {
		return err
	}
	end := true
	_, err = api.HttpResponseChunk(context.Background(), &rpc.LlmInferenceHTTPResponseChunkRequest{
		RequestID: s.requestID,
		Data:      "",
		End:       &end,
	})
	return err
}

func (s *llmResponseSink) Error(message string, code string) error {
	s.state.mu.Lock()
	if s.state.finished {
		s.state.mu.Unlock()
		return nil
	}
	s.state.finished = true
	s.state.mu.Unlock()
	s.adapter.removePending(s.requestID)
	api, err := s.rpcAPI()
	if err != nil {
		return err
	}
	end := true
	chunkErr := &rpc.LlmInferenceHTTPResponseChunkError{Message: message}
	if code != "" {
		c := code
		chunkErr.Code = &c
	}
	_, err = api.HttpResponseChunk(context.Background(), &rpc.LlmInferenceHTTPResponseChunkRequest{
		RequestID: s.requestID,
		Data:      "",
		End:       &end,
		Error:     chunkErr,
	})
	return err
}
