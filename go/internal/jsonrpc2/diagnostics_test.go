package jsonrpc2

import (
	"context"
	"encoding/json"
	"errors"
	"io"
	"os"
	"os/exec"
	"strings"
	"testing"
	"time"
)

const protocolSensitiveMarker = "SYNTHETIC_SENSITIVE_PROTOCOL_TEXT"

func TestProtocolDiagnosticsDoNotWriteOutput(t *testing.T) {
	const helperEnv = "COPILOT_SDK_JSONRPC_DIAGNOSTICS_TEST"
	if os.Getenv(helperEnv) == "1" {
		testProtocolReadErrors(t)
		testProtocolDecodeErrors(t)
		testProtocolResponseSendErrors(t)
		testProtocolRPCError(t)
		if t.Failed() {
			os.Exit(1)
		}
		// Skip the test runner's PASS and coverage summaries in this output-only helper.
		os.Exit(0)
	}

	t.Parallel()
	executable, err := os.Executable()
	if err != nil {
		t.Fatal(err)
	}
	ctx, cancel := context.WithTimeout(t.Context(), 30*time.Second)
	defer cancel()

	// Capture a subprocess so concurrent tests keep their own stdout and stderr.
	command := exec.CommandContext(ctx, executable,
		"-test.run=^TestProtocolDiagnosticsDoNotWriteOutput$", "-test.timeout=20s")
	command.Env = append(os.Environ(), helperEnv+"=1")
	output, err := command.CombinedOutput()
	if err != nil {
		t.Fatalf("protocol diagnostics subprocess failed: %v\n%s", err, output)
	}
	if len(output) != 0 {
		t.Fatalf("unexpected protocol output on stdout or stderr: %q", output)
	}
}

func testProtocolReadErrors(t *testing.T) {
	for _, tc := range []struct {
		name  string
		input string
		err   error
	}{
		{name: "malformed_header", input: protocolSensitiveMarker + "\r\n\r\n"},
		{name: "malformed_content_length", input: "Content-Length: " + protocolSensitiveMarker + "\r\n\r\n"},
		{name: "read_error", err: errors.New(protocolSensitiveMarker)},
		{name: "truncated_header", input: "Content-Length: "},
		{name: "truncated_body", input: "Content-Length: 100\r\n\r\n" + protocolSensitiveMarker},
		{name: "eof"},
	} {
		t.Run(tc.name, func(t *testing.T) {
			client, peerReader, peerWriter := newProtocolOutputTestClient(t)
			closed := make(chan struct{})
			client.SetOnClose(func() { close(closed) })
			client.Start()
			_, result := startProtocolOutputTestRequest(t, client, peerReader)

			if _, err := io.WriteString(peerWriter, tc.input); err != nil {
				t.Fatal(err)
			}
			if err := peerWriter.CloseWithError(tc.err); err != nil {
				t.Fatal(err)
			}

			got := <-result
			if got.err == nil || got.err.Error() != "connection closed" {
				t.Fatalf("Request error = %v, want connection closed", got.err)
			}
			select {
			case <-client.ConnectionClosed():
			default:
				t.Fatal("read failure did not close the connection signal")
			}
			select {
			case <-closed:
			default:
				t.Fatal("read failure did not invoke onClose")
			}
			_, err := client.Request(t.Context(), "test.method", nil)
			if err == nil || err.Error() != "connection closed" {
				t.Fatalf("subsequent Request error = %v, want connection closed", err)
			}
			assertProtocolOutputTestRequestsCleanedUp(t, client)
			client.Stop()
		})
	}
}

func testProtocolDecodeErrors(t *testing.T) {
	for _, tc := range []struct {
		name string
		data string
	}{
		{name: "malformed_json", data: `{"jsonrpc":"2.0","method":` + protocolSensitiveMarker + `}`},
		{name: "unsupported_version", data: `{"jsonrpc":"` + protocolSensitiveMarker + `","method":"test.method"}`},
	} {
		t.Run(tc.name, func(t *testing.T) {
			client, peerReader, peerWriter := newProtocolOutputTestClient(t)
			client.Start()
			request, result := startProtocolOutputTestRequest(t, client, peerReader)
			writer := newHeaderWriter(peerWriter)
			if err := writer.Write([]byte(tc.data)); err != nil {
				t.Fatal(err)
			}
			response, err := json.Marshal(Response{
				JSONRPC: version,
				ID:      request.ID,
				Result:  json.RawMessage(`{"ok":true}`),
			})
			if err != nil {
				t.Fatal(err)
			}
			if err := writer.Write(response); err != nil {
				t.Fatal(err)
			}
			got := <-result
			if got.err != nil || string(got.data) != `{"ok":true}` {
				t.Fatalf("Request after malformed frame = (%s, %v), want successful response", got.data, got.err)
			}
			select {
			case <-client.ConnectionClosed():
				t.Fatal("decode failure closed the connection")
			default:
			}
			assertProtocolOutputTestRequestsCleanedUp(t, client)
		})
	}
}

func testProtocolResponseSendErrors(t *testing.T) {
	for _, tc := range []struct {
		name string
		send func(context.Context, *Client)
	}{
		{
			name: "response_send_error",
			send: func(ctx context.Context, client *Client) {
				client.sendResponse(ctx, json.RawMessage("1"), json.RawMessage("{}"))
			},
		},
		{
			name: "error_response_send_error",
			send: func(ctx context.Context, client *Client) {
				client.sendErrorResponse(ctx, json.RawMessage("1"), &Error{
					Code: ErrInternal.Code, Message: protocolSensitiveMarker,
				})
			},
		},
	} {
		t.Run(tc.name, func(t *testing.T) {
			writeErr := errors.New(protocolSensitiveMarker)
			writer := &protocolOutputErrorWriter{err: writeErr}
			client := NewClient(writeCloser{Writer: writer}, io.NopCloser(strings.NewReader("")))

			tc.send(t.Context(), client)
			if writer.calls != 1 {
				t.Fatalf("response write attempts = %d, want 1", writer.calls)
			}

			_, err := client.Request(t.Context(), "test.method", nil)
			if !errors.Is(err, writeErr) {
				t.Fatalf("Request error = %v, want wrapped write error", err)
			}
			assertProtocolOutputTestRequestsCleanedUp(t, client)
		})
	}
}

func testProtocolRPCError(t *testing.T) {
	t.Run("returned_rpc_error", func(t *testing.T) {
		client, peerReader, peerWriter := newProtocolOutputTestClient(t)
		client.Start()
		request, result := startProtocolOutputTestRequest(t, client, peerReader)
		want := &Error{
			Code:    ErrInvalidParams.Code,
			Message: protocolSensitiveMarker,
			Data:    json.RawMessage(`{"detail":"` + protocolSensitiveMarker + `"}`),
		}
		response, err := json.Marshal(Response{JSONRPC: version, ID: request.ID, Error: want})
		if err != nil {
			t.Fatal(err)
		}
		if err := newHeaderWriter(peerWriter).Write(response); err != nil {
			t.Fatal(err)
		}
		got := <-result
		var rpcErr *Error
		if !errors.As(got.err, &rpcErr) {
			t.Fatalf("Request error = %v, want *Error", got.err)
		}
		if rpcErr.Code != want.Code || rpcErr.Message != want.Message || string(rpcErr.Data) != string(want.Data) {
			t.Fatalf("RPC error = %#v, want %#v", rpcErr, want)
		}
		assertProtocolOutputTestRequestsCleanedUp(t, client)
	})
}

type protocolOutputErrorWriter struct {
	err   error
	calls int
}

func (w *protocolOutputErrorWriter) Write([]byte) (int, error) {
	w.calls++
	return 0, w.err
}

func newProtocolOutputTestClient(t *testing.T) (*Client, *io.PipeReader, *io.PipeWriter) {
	t.Helper()
	stdinReader, stdinWriter := io.Pipe()
	stdoutReader, stdoutWriter := io.Pipe()
	client := NewClient(stdinWriter, stdoutReader)
	t.Cleanup(func() {
		stdinReader.Close()
		stdinWriter.Close()
		client.Stop()
		stdoutWriter.Close()
		stdoutReader.Close()
	})
	return client, stdinReader, stdoutWriter
}

type protocolOutputTestResult struct {
	data json.RawMessage
	err  error
}

func startProtocolOutputTestRequest(t *testing.T, client *Client, reader io.Reader) (*Request, <-chan protocolOutputTestResult) {
	t.Helper()
	ctx, cancel := context.WithTimeout(t.Context(), 5*time.Second)
	t.Cleanup(cancel)
	result := make(chan protocolOutputTestResult, 1)
	go func() {
		data, err := client.RequestWithInlineResponse(ctx, "test.method", nil, func(json.RawMessage) error { return nil })
		result <- protocolOutputTestResult{data: data, err: err}
	}()
	data, err := newHeaderReader(reader).Read()
	if err != nil {
		t.Fatal(err)
	}
	var request Request
	if err := json.Unmarshal(data, &request); err != nil {
		t.Fatal(err)
	}
	return &request, result
}

func assertProtocolOutputTestRequestsCleanedUp(t *testing.T, client *Client) {
	t.Helper()
	client.mu.Lock()
	defer client.mu.Unlock()
	if len(client.pendingRequests) != 0 || len(client.pendingInlineCallbacks) != 0 {
		t.Fatalf("pending requests = %d, inline callbacks = %d, want both empty",
			len(client.pendingRequests), len(client.pendingInlineCallbacks))
	}
}
