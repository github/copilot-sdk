package copilot

import (
	"bufio"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net"
	"net/http"
	"strconv"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
)

const responseProtocolTimeout = 5 * time.Second

type roundTripFunc func(*http.Request) (*http.Response, error)

func (f roundTripFunc) RoundTrip(req *http.Request) (*http.Response, error) {
	return f(req)
}

type trackedResponseBody struct {
	reader    *io.PipeReader
	closed    chan struct{}
	closeOnce sync.Once
}

type zeroProgressResponseBody struct {
	closed    chan struct{}
	closeOnce sync.Once
}

func (b *zeroProgressResponseBody) Read([]byte) (int, error) {
	return 0, nil
}

func (b *zeroProgressResponseBody) Close() error {
	b.closeOnce.Do(func() {
		close(b.closed)
	})
	return nil
}

func newTrackedResponseBody() (*trackedResponseBody, *io.PipeWriter) {
	reader, writer := io.Pipe()
	return &trackedResponseBody{reader: reader, closed: make(chan struct{})}, writer
}

func (b *trackedResponseBody) Read(p []byte) (int, error) {
	return b.reader.Read(p)
}

func (b *trackedResponseBody) Close() error {
	var err error
	b.closeOnce.Do(func() {
		close(b.closed)
		err = b.reader.Close()
	})
	return err
}

type responseProtocolMessage struct {
	JSONRPC string          `json:"jsonrpc"`
	ID      json.RawMessage `json:"id,omitempty"`
	Method  string          `json:"method,omitempty"`
	Params  json.RawMessage `json:"params,omitempty"`
}

type responseProtocolPeer struct {
	t       *testing.T
	conn    net.Conn
	reader  *bufio.Reader
	client  *jsonrpc2.Client
	adapter *copilotRequestAdapter
	stop    sync.Once
}

func newResponseProtocolPeer(t *testing.T, body io.ReadCloser) *responseProtocolPeer {
	return newResponseProtocolPeerWithTransport(t, roundTripFunc(func(*http.Request) (*http.Response, error) {
		return &http.Response{
			StatusCode: http.StatusOK,
			Status:     "200 OK",
			Header:     http.Header{"Content-Type": {"text/event-stream"}},
			Body:       body,
		}, nil
	}))
}

func newResponseProtocolPeerWithTransport(t *testing.T, transport http.RoundTripper) *responseProtocolPeer {
	t.Helper()
	sdkConn, runtimeConn := net.Pipe()
	client := jsonrpc2.NewClient(sdkConn, sdkConn)
	serverRPC := rpc.NewServerRPC(client)
	handler := &CopilotRequestHandler{
		Transport: transport,
	}
	adapter := newCopilotRequestAdapter(handler, func() *rpc.ServerLlmInferenceAPI {
		return serverRPC.LlmInference
	})
	rpc.RegisterClientGlobalAPIHandlers(client, &rpc.ClientGlobalAPIHandlers{LlmInference: adapter})
	client.SetOnClose(adapter.close)
	client.Start()

	peer := &responseProtocolPeer{
		t:       t,
		conn:    runtimeConn,
		reader:  bufio.NewReader(runtimeConn),
		client:  client,
		adapter: adapter,
	}
	t.Cleanup(peer.close)
	return peer
}

func (p *responseProtocolPeer) close() {
	p.stop.Do(func() {
		_ = p.conn.Close()
		p.adapter.close()
		p.client.Stop()
	})
}

func (p *responseProtocolPeer) beginRequest() {
	p.t.Helper()
	p.send(responseProtocolMessage{
		JSONRPC: "2.0",
		ID:      json.RawMessage("1"),
		Method:  "llmInference.httpRequestStart",
		Params: mustMarshal(p.t, map[string]any{
			"requestId": "test",
			"method":    "GET",
			"url":       "http://unused.test",
			"headers":   map[string][]string{},
		}),
	})
	p.send(responseProtocolMessage{
		JSONRPC: "2.0",
		ID:      json.RawMessage("2"),
		Method:  "llmInference.httpRequestChunk",
		Params: mustMarshal(p.t, map[string]any{
			"requestId": "test",
			"data":      "",
			"end":       true,
		}),
	})
}

func (p *responseProtocolPeer) cancelRequest() {
	p.t.Helper()
	p.send(responseProtocolMessage{
		JSONRPC: "2.0",
		ID:      json.RawMessage("3"),
		Method:  "llmInference.httpRequestChunk",
		Params: mustMarshal(p.t, map[string]any{
			"requestId": "test",
			"data":      "",
			"cancel":    true,
		}),
	})
}

func (p *responseProtocolPeer) send(message any) {
	p.t.Helper()
	data, err := json.Marshal(message)
	if err != nil {
		p.t.Fatal(err)
	}
	if err := p.conn.SetWriteDeadline(time.Now().Add(responseProtocolTimeout)); err != nil {
		p.t.Fatal(err)
	}
	if _, err := fmt.Fprintf(p.conn, "Content-Length: %d\r\n\r\n", len(data)); err != nil {
		p.t.Fatal(err)
	}
	if _, err := p.conn.Write(data); err != nil {
		p.t.Fatal(err)
	}
}

func (p *responseProtocolPeer) nextRequest() responseProtocolMessage {
	p.t.Helper()
	message, ok := p.nextRequestWithin(responseProtocolTimeout)
	if !ok {
		p.t.Fatal("timed out waiting for runtime request")
	}
	return message
}

func (p *responseProtocolPeer) nextRequestWithin(timeout time.Duration) (responseProtocolMessage, bool) {
	p.t.Helper()
	deadline := time.Now().Add(timeout)
	for {
		if err := p.conn.SetReadDeadline(deadline); err != nil {
			p.t.Fatal(err)
		}
		message, err := readResponseProtocolMessage(p.reader)
		if err != nil {
			if netErr, ok := err.(net.Error); ok && netErr.Timeout() {
				return responseProtocolMessage{}, false
			}
			p.t.Fatal(err)
		}
		if message.Method != "" {
			return message, true
		}
	}
}

func (p *responseProtocolPeer) ack(message responseProtocolMessage) {
	p.respondAccepted(message, true)
}

func (p *responseProtocolPeer) respondAccepted(message responseProtocolMessage, accepted bool) {
	p.t.Helper()
	p.send(struct {
		JSONRPC string          `json:"jsonrpc"`
		ID      json.RawMessage `json:"id"`
		Result  map[string]bool `json:"result"`
	}{
		JSONRPC: "2.0",
		ID:      message.ID,
		Result:  map[string]bool{"accepted": accepted},
	})
}

func (p *responseProtocolPeer) reject(message responseProtocolMessage, text string) {
	p.t.Helper()
	p.send(struct {
		JSONRPC string          `json:"jsonrpc"`
		ID      json.RawMessage `json:"id"`
		Error   map[string]any  `json:"error"`
	}{
		JSONRPC: "2.0",
		ID:      message.ID,
		Error:   map[string]any{"code": -32603, "message": text},
	})
}

func (p *responseProtocolPeer) startAndAck() {
	p.t.Helper()
	p.beginRequest()
	head := p.nextRequest()
	if head.Method != "llmInference.httpResponseStart" {
		p.t.Fatalf("first request method = %q, want httpResponseStart", head.Method)
	}
	p.ack(head)
}

func readResponseProtocolMessage(reader *bufio.Reader) (responseProtocolMessage, error) {
	contentLength := 0
	for {
		line, err := reader.ReadString('\n')
		if err != nil {
			return responseProtocolMessage{}, err
		}
		if line == "\r\n" {
			break
		}
		if value, ok := strings.CutPrefix(line, "Content-Length:"); ok {
			contentLength, err = strconv.Atoi(strings.TrimSpace(value))
			if err != nil {
				return responseProtocolMessage{}, err
			}
		}
	}
	if contentLength == 0 {
		return responseProtocolMessage{}, errors.New("missing Content-Length")
	}
	data := make([]byte, contentLength)
	if _, err := io.ReadFull(reader, data); err != nil {
		return responseProtocolMessage{}, err
	}
	var message responseProtocolMessage
	if err := json.Unmarshal(data, &message); err != nil {
		return responseProtocolMessage{}, err
	}
	return message, nil
}

func mustMarshal(t *testing.T, value any) json.RawMessage {
	t.Helper()
	data, err := json.Marshal(value)
	if err != nil {
		t.Fatal(err)
	}
	return data
}

func responseChunkParams(t *testing.T, message responseProtocolMessage) map[string]any {
	t.Helper()
	if message.Method != "llmInference.httpResponseChunk" {
		t.Fatalf("request method = %q, want httpResponseChunk", message.Method)
	}
	var params map[string]any
	if err := json.Unmarshal(message.Params, &params); err != nil {
		t.Fatal(err)
	}
	return params
}

func responseChunkData(t *testing.T, message responseProtocolMessage) []byte {
	t.Helper()
	params := responseChunkParams(t, message)
	if params["end"] != false {
		t.Fatalf("chunk end = %v, want false", params["end"])
	}
	if params["binary"] != true {
		t.Fatalf("chunk binary = %v, want true", params["binary"])
	}
	data, err := base64.StdEncoding.DecodeString(params["data"].(string))
	if err != nil {
		t.Fatal(err)
	}
	if len(data) == 0 || len(data) > httpResponseReadAheadSize {
		t.Fatalf("chunk size = %d, want 1..%d", len(data), httpResponseReadAheadSize)
	}
	return data
}

func waitForBodyClose(t *testing.T, body *trackedResponseBody) {
	t.Helper()
	select {
	case <-body.closed:
	case <-time.After(responseProtocolTimeout):
		t.Fatal("response body was not closed")
	}
}

func TestHTTPResponseReaderCloseStopsZeroProgressSource(t *testing.T) {
	body := &zeroProgressResponseBody{closed: make(chan struct{})}
	reader := newHTTPResponseReader(body)
	closed := make(chan struct{})

	go func() {
		reader.Close()
		close(closed)
	}()

	select {
	case <-closed:
	case <-time.After(responseProtocolTimeout):
		t.Fatal("reader close blocked on a zero-progress source")
	}

	select {
	case <-body.closed:
	default:
		t.Fatal("response body was not closed")
	}
}

func TestHTTPResponseReadsAheadAndCoalescesUnderOneOutstandingRPC(t *testing.T) {
	body, writer := newTrackedResponseBody()
	peer := newResponseProtocolPeer(t, body)
	peer.startAndAck()

	if _, err := writer.Write([]byte("first")); err != nil {
		t.Fatal(err)
	}
	first := peer.nextRequest()
	if got := string(responseChunkData(t, first)); got != "first" {
		t.Fatalf("first chunk = %q", got)
	}

	writeDone := make(chan error, 1)
	go func() {
		for range 32 {
			if _, err := writer.Write(make([]byte, 1024)); err != nil {
				writeDone <- err
				return
			}
		}
		_, err := writer.Write([]byte("last"))
		writeDone <- err
	}()

	select {
	case err := <-writeDone:
		t.Fatalf("writer completed before read-ahead space was released: %v", err)
	case <-time.After(50 * time.Millisecond):
	}
	if _, ok := peer.nextRequestWithin(50 * time.Millisecond); ok {
		t.Fatal("sent another data RPC while the first acknowledgement was withheld")
	}

	peer.ack(first)
	combined := peer.nextRequest()
	if got := len(responseChunkData(t, combined)); got != httpResponseReadAheadSize {
		t.Fatalf("coalesced chunk size = %d, want %d", got, httpResponseReadAheadSize)
	}
	select {
	case err := <-writeDone:
		if err != nil {
			t.Fatal(err)
		}
	case <-time.After(responseProtocolTimeout):
		t.Fatal("source did not resume after acknowledgement")
	}
	if _, ok := peer.nextRequestWithin(50 * time.Millisecond); ok {
		t.Fatal("sent another data RPC while the coalesced chunk acknowledgement was withheld")
	}

	peer.ack(combined)
	last := peer.nextRequest()
	if got := string(responseChunkData(t, last)); got != "last" {
		t.Fatalf("last chunk = %q", got)
	}
	if err := writer.Close(); err != nil {
		t.Fatal(err)
	}
	peer.ack(last)
	end := peer.nextRequest()
	if params := responseChunkParams(t, end); params["end"] != true || params["error"] != nil {
		t.Fatalf("unexpected terminal chunk: %s", end.Params)
	}
	peer.ack(end)
}

func TestHTTPResponseFlushesPartialBytesImmediately(t *testing.T) {
	body, writer := newTrackedResponseBody()
	peer := newResponseProtocolPeer(t, body)
	peer.startAndAck()

	if _, err := writer.Write([]byte{0xf0}); err != nil {
		t.Fatal(err)
	}
	chunk := peer.nextRequest()
	if got := responseChunkData(t, chunk); len(got) != 1 || got[0] != 0xf0 {
		t.Fatalf("chunk = %v, want the single source byte", got)
	}
	peer.ack(chunk)
	if err := writer.Close(); err != nil {
		t.Fatal(err)
	}
	end := peer.nextRequest()
	if params := responseChunkParams(t, end); params["end"] != true {
		t.Fatalf("terminal end = %v, want true", params["end"])
	}
	peer.ack(end)
}

func TestHTTPResponseCompletionReleasesExchangeContext(t *testing.T) {
	body, writer := newTrackedResponseBody()
	peer := newResponseProtocolPeer(t, body)
	peer.startAndAck()

	peer.adapter.mu.Lock()
	exchange := peer.adapter.pending["test"]
	peer.adapter.mu.Unlock()
	if exchange == nil {
		t.Fatal("pending exchange was not registered")
	}

	if err := writer.Close(); err != nil {
		t.Fatal(err)
	}
	end := peer.nextRequest()
	if params := responseChunkParams(t, end); params["end"] != true || params["error"] != nil {
		t.Fatalf("unexpected terminal chunk: %s", end.Params)
	}
	peer.ack(end)

	select {
	case <-exchange.ctx.Done():
	case <-time.After(responseProtocolTimeout):
		t.Fatal("completed exchange context was not released")
	}
}

func TestHTTPResponseUpstreamErrorFollowsBufferedBytesAndAcknowledgement(t *testing.T) {
	body, writer := newTrackedResponseBody()
	peer := newResponseProtocolPeer(t, body)
	peer.startAndAck()

	if _, err := writer.Write([]byte("first")); err != nil {
		t.Fatal(err)
	}
	first := peer.nextRequest()
	if got := string(responseChunkData(t, first)); got != "first" {
		t.Fatalf("first chunk = %q", got)
	}

	sourceDone := make(chan struct{})
	go func() {
		_, _ = writer.Write([]byte("partial"))
		_ = writer.CloseWithError(errors.New("upstream failed"))
		close(sourceDone)
	}()
	select {
	case <-sourceDone:
	case <-time.After(responseProtocolTimeout):
		t.Fatal("source error was not read ahead")
	}
	if _, ok := peer.nextRequestWithin(50 * time.Millisecond); ok {
		t.Fatal("upstream error overtook the outstanding data RPC")
	}

	peer.ack(first)
	partial := peer.nextRequest()
	if got := string(responseChunkData(t, partial)); got != "partial" {
		t.Fatalf("partial chunk = %q", got)
	}
	if _, ok := peer.nextRequestWithin(50 * time.Millisecond); ok {
		t.Fatal("upstream error overtook buffered response bytes")
	}

	peer.ack(partial)
	terminal := peer.nextRequest()
	params := responseChunkParams(t, terminal)
	errorValue, ok := params["error"].(map[string]any)
	if params["end"] != true || !ok || errorValue["message"] != "upstream failed" {
		t.Fatalf("unexpected terminal error: %s", terminal.Params)
	}
	peer.ack(terminal)
}

func TestHTTPResponseCancellationClosesSourceWithoutDataAcknowledgement(t *testing.T) {
	body, writer := newTrackedResponseBody()
	peer := newResponseProtocolPeer(t, body)
	peer.startAndAck()

	if _, err := writer.Write([]byte("first")); err != nil {
		t.Fatal(err)
	}
	first := peer.nextRequest()
	if got := string(responseChunkData(t, first)); got != "first" {
		t.Fatalf("first chunk = %q", got)
	}

	peer.cancelRequest()
	terminal := peer.nextRequest()
	params := responseChunkParams(t, terminal)
	errorValue, ok := params["error"].(map[string]any)
	if params["end"] != true || !ok || errorValue["code"] != "cancelled" {
		t.Fatalf("unexpected cancellation chunk: %s", terminal.Params)
	}
	waitForBodyClose(t, body)
	peer.ack(terminal)
}

func TestHTTPResponseCancellationSendsHeadBeforeTerminalError(t *testing.T) {
	requestCancelled := make(chan struct{})
	peer := newResponseProtocolPeerWithTransport(t, roundTripFunc(func(request *http.Request) (*http.Response, error) {
		<-request.Context().Done()
		close(requestCancelled)
		return nil, request.Context().Err()
	}))

	peer.beginRequest()
	peer.cancelRequest()
	select {
	case <-requestCancelled:
	case <-time.After(responseProtocolTimeout):
		t.Fatal("upstream request was not cancelled")
	}

	head := peer.nextRequest()
	if head.Method != "llmInference.httpResponseStart" {
		t.Fatalf("first response method = %q, want httpResponseStart", head.Method)
	}
	var headParams map[string]any
	if err := json.Unmarshal(head.Params, &headParams); err != nil {
		t.Fatal(err)
	}
	if headParams["status"] != float64(499) {
		t.Fatalf("response status = %v, want 499", headParams["status"])
	}
	peer.ack(head)

	terminal := peer.nextRequest()
	params := responseChunkParams(t, terminal)
	errorValue, ok := params["error"].(map[string]any)
	if params["end"] != true || !ok || errorValue["code"] != "cancelled" {
		t.Fatalf("unexpected cancellation chunk: %s", terminal.Params)
	}
	peer.ack(terminal)
}

func TestHTTPResponseRPCRejectionAndConnectionLossCloseSource(t *testing.T) {
	t.Run("JSON-RPC rejection", func(t *testing.T) {
		body, writer := newTrackedResponseBody()
		peer := newResponseProtocolPeer(t, body)
		peer.startAndAck()

		if _, err := writer.Write([]byte("first")); err != nil {
			t.Fatal(err)
		}
		first := peer.nextRequest()
		peer.reject(first, "write rejected")
		waitForBodyClose(t, body)

		terminal := peer.nextRequest()
		params := responseChunkParams(t, terminal)
		errorValue, ok := params["error"].(map[string]any)
		if params["end"] != true || !ok || !strings.Contains(errorValue["message"].(string), "write rejected") {
			t.Fatalf("unexpected rejection terminal: %s", terminal.Params)
		}
		peer.ack(terminal)
	})

	t.Run("accepted false", func(t *testing.T) {
		body, writer := newTrackedResponseBody()
		peer := newResponseProtocolPeer(t, body)
		peer.startAndAck()

		if _, err := writer.Write([]byte("first")); err != nil {
			t.Fatal(err)
		}
		first := peer.nextRequest()
		peer.respondAccepted(first, false)
		waitForBodyClose(t, body)

		terminal := peer.nextRequest()
		params := responseChunkParams(t, terminal)
		errorValue, ok := params["error"].(map[string]any)
		if params["end"] != true || !ok || !strings.Contains(errorValue["message"].(string), "was rejected") {
			t.Fatalf("unexpected rejection terminal: %s", terminal.Params)
		}
		peer.ack(terminal)
	})

	t.Run("connection loss", func(t *testing.T) {
		body, writer := newTrackedResponseBody()
		peer := newResponseProtocolPeer(t, body)
		peer.startAndAck()

		if _, err := writer.Write([]byte("first")); err != nil {
			t.Fatal(err)
		}
		_ = peer.nextRequest()
		if err := peer.conn.Close(); err != nil {
			t.Fatal(err)
		}
		waitForBodyClose(t, body)
	})
}
