package jsonrpc2

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"sync"
	"testing"
	"testing/synctest"
	"time"
)

type writeCloser struct {
	io.Writer
}

func (w writeCloser) Close() error { return nil }

type writeFunc func([]byte) (int, error)

func (w writeFunc) Write(data []byte) (int, error) { return w(data) }

func TestUnreadableErrorFailsRequestAndKeepsReading(t *testing.T) {
	for _, errorJSON := range []string{
		`null`,
		`"not an object"`,
		`{"code":-32603,"message":42}`,
		`{"code":9223372036854775808,"message":"overflow"}`,
		`{"code":-32603,"message":"readable","data":{"detail":true}}`,
	} {
		t.Run(errorJSON, func(t *testing.T) {
			stdinR, stdinW := io.Pipe()
			stdoutR, stdoutW := io.Pipe()
			client := NewClient(stdinW, stdoutR)
			client.Start()
			defer client.Stop()
			defer stdinR.Close()
			defer stdoutW.Close()

			peerDone := make(chan error, 1)
			go func() {
				reader := newHeaderReader(stdinR)
				writer := newHeaderWriter(stdoutW)
				for i := range 2 {
					frame, err := reader.Read()
					if err != nil {
						peerDone <- err
						return
					}
					var request Request
					if err := json.Unmarshal(frame, &request); err != nil {
						peerDone <- err
						return
					}
					payload := `"result":{"recovered":true}`
					if i == 0 {
						payload = `"error":` + errorJSON
					}
					response := fmt.Sprintf(`{"jsonrpc":"2.0","id":%s,%s}`, request.ID, payload)
					if err := writer.Write([]byte(response)); err != nil {
						peerDone <- err
						return
					}
				}
				peerDone <- nil
			}()

			ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
			defer cancel()
			callbackCalled := false
			_, err := client.RequestWithInlineResponse(ctx, "bad", nil, func(json.RawMessage) error {
				callbackCalled = true
				return nil
			})
			var rpcError *Error
			if !errors.As(err, &rpcError) {
				t.Fatalf("first request error = %v, want JSON-RPC error", err)
			}
			if errorJSON == `{"code":-32603,"message":"readable","data":{"detail":true}}` {
				if rpcError.Message != "readable" || string(rpcError.Data) != `{"detail":true}` {
					t.Fatalf("readable error changed: %+v", rpcError)
				}
			} else if rpcError.Code != ErrInternal.Code || !bytes.Contains([]byte(rpcError.Message), []byte("invalid JSON-RPC error response")) {
				t.Fatalf("malformed error = %+v, want a decoding failure", rpcError)
			}
			if callbackCalled {
				t.Fatal("success callback ran for an error response")
			}
			result, err := client.Request(ctx, "next", nil)
			if err != nil || string(result) != `{"recovered":true}` {
				t.Fatalf("next request = %s, %v; want recovered result", result, err)
			}
			if err := <-peerDone; err != nil {
				t.Fatalf("peer failed: %v", err)
			}
		})
	}
}

func TestRequestPreparationPrecedesFollowingNotification(t *testing.T) {
	client := NewClient(writeCloser{io.Discard}, io.NopCloser(bytes.NewReader(nil)))
	release, finished := make(chan struct{}), make(chan struct{})
	prepared := false
	client.SetRequestHandlerWithPreparation("validate", func(json.RawMessage) (json.RawMessage, *Error) {
		defer close(finished)
		<-release
		return json.RawMessage(`null`), nil
	}, func(json.RawMessage) *Error {
		prepared = true
		return nil
	})
	client.SetRequestHandler("cancel", func(json.RawMessage) (json.RawMessage, *Error) {
		if !prepared {
			t.Error("cancellation overtook request registration")
		}
		return nil, nil
	})
	client.handleRequest(&Request{ID: json.RawMessage(`1`), Method: "validate"})
	client.handleRequest(&Request{Method: "cancel"})
	close(release)
	select {
	case <-finished:
	case <-time.After(time.Second):
		t.Fatal("request did not finish")
	}
}

func TestOnCloseCalledOnUnexpectedExit(t *testing.T) {
	stdinR, stdinW := io.Pipe()
	stdoutR, stdoutW := io.Pipe()
	defer stdinR.Close()

	client := NewClient(stdinW, stdoutR)

	called := make(chan struct{}, 1)
	client.SetOnClose(func() {
		called <- struct{}{}
	})

	client.Start()

	// Simulate unexpected process death by closing the stdout writer
	stdoutW.Close()

	select {
	case <-called:
	case <-time.After(200 * time.Millisecond):
		t.Error("expected onClose to be called when read loop exits unexpectedly")
	}
}

func TestConnectionCloseCancelsInFlightRequestHandlers(t *testing.T) {
	stdinR, stdinW := io.Pipe()
	stdoutR, stdoutW := io.Pipe()
	defer stdinR.Close()
	go io.Copy(io.Discard, stdinR)

	client := NewClient(stdinW, stdoutR)
	entered := make(chan struct{})
	cancelled := make(chan struct{})
	client.SetRequestContextHandler("memoryStore.store", func(ctx context.Context, _ json.RawMessage) (json.RawMessage, *Error) {
		close(entered)
		<-ctx.Done()
		close(cancelled)
		return nil, nil
	})
	client.Start()
	t.Cleanup(client.Stop)

	writer := newHeaderWriter(stdoutW)
	if err := writer.Write([]byte(`{"jsonrpc":"2.0","id":1,"method":"memoryStore.store","params":{}}`)); err != nil {
		t.Fatalf("Write request: %v", err)
	}
	select {
	case <-entered:
	case <-time.After(time.Second):
		t.Fatal("handler did not start")
	}
	stdoutW.Close()

	select {
	case <-cancelled:
	case <-time.After(time.Second):
		t.Fatal("connection close did not cancel the in-flight handler")
	}
}

func TestOnCloseNotCalledOnIntentionalStop(t *testing.T) {
	stdinR, stdinW := io.Pipe()
	stdoutR, stdoutW := io.Pipe()
	defer stdinR.Close()
	defer stdoutW.Close()

	client := NewClient(stdinW, stdoutR)

	var called bool
	var mu sync.Mutex
	client.SetOnClose(func() {
		mu.Lock()
		called = true
		mu.Unlock()
	})

	client.Start()

	// Intentional stop — should set running=false before closing stdout,
	// so the readLoop should NOT invoke onClose.
	client.Stop()

	time.Sleep(200 * time.Millisecond)

	mu.Lock()
	defer mu.Unlock()
	if called {
		t.Error("onClose should not be called on intentional Stop()")
	}
}

func TestRequestReturnsWhenConnectionCloses(t *testing.T) {
	stdinR, stdinW := io.Pipe()
	stdoutR, stdoutW := io.Pipe()
	defer stdinR.Close()
	defer stdoutR.Close()

	client := NewClient(stdinW, stdoutR)
	client.Start()
	defer client.Stop()

	result := make(chan error, 1)
	go func() {
		_, err := client.Request(context.Background(), "test.method", nil)
		result <- err
	}()

	if _, err := newHeaderReader(stdinR).Read(); err != nil {
		t.Fatalf("Read request failed: %v", err)
	}

	if err := stdoutW.Close(); err != nil {
		t.Fatalf("Close response stream failed: %v", err)
	}

	select {
	case err := <-result:
		if err == nil || err.Error() != "connection closed" {
			t.Fatalf("Request error = %v, want connection closed", err)
		}
	case <-time.After(time.Second):
		t.Fatal("Request did not return when the connection closed")
	}
}

// startWatchedClient starts a client whose CLI process is watched through
// SetProcessDone, as on the stdio transport. It returns the stream that closes
// the connection and a channel that receives each request the client writes.
func startWatchedClient(t *testing.T, done chan struct{}, processErr *error) (*Client, io.Closer, <-chan struct{}) {
	t.Helper()
	stdinR, stdinW := io.Pipe()
	stdoutR, stdoutW := io.Pipe()
	client := NewClient(stdinW, stdoutR)
	client.SetProcessDone(done, processErr)
	client.Start()
	t.Cleanup(func() {
		client.Stop()
		stdinR.Close()
		stdoutW.Close()
	})
	written := make(chan struct{}, 1)
	go func() {
		reader := newHeaderReader(stdinR)
		for {
			if _, err := reader.Read(); err != nil {
				return
			}
			written <- struct{}{}
		}
	}()
	return client, stdoutW, written
}

// A watched CLI process can outlive its connection: its stdout can close, or a
// malformed frame can end the read loop, while the process keeps running.
func TestRequestReturnsWhenConnectionClosesWhileProcessRuns(t *testing.T) {
	for _, sent := range []string{"before close", "after close"} {
		t.Run(sent, func(t *testing.T) {
			t.Parallel()
			var processErr error
			client, stdout, written := startWatchedClient(t, make(chan struct{}), &processErr)

			result := make(chan error, 1)
			request := func() {
				go func() {
					_, err := client.Request(context.Background(), "test.method", nil)
					result <- err
				}()
			}
			if sent == "before close" {
				request()
				<-written
			}
			if err := stdout.Close(); err != nil {
				t.Fatalf("Close response stream failed: %v", err)
			}
			<-client.ConnectionClosed()
			if sent == "after close" {
				request()
			}

			select {
			case err := <-result:
				if err == nil || err.Error() != "connection closed" {
					t.Fatalf("Request error = %v, want connection closed", err)
				}
			case <-time.After(5 * time.Second):
				t.Fatal("Request did not return when the connection closed while the process kept running")
			}
			if sent == "after close" && len(written) != 0 {
				t.Fatal("Request was written after the connection closed")
			}
		})
	}
}

// When the CLI process exits, its stdout closes before the process is reaped
// and its stderr collected, so the connection closes first. The request must
// still report the process error, which carries the CLI's own diagnostic.
func TestRequestReportsProcessErrorWhenConnectionClosesBeforeExit(t *testing.T) {
	for _, sent := range []string{"before close", "after close"} {
		t.Run(sent, func(t *testing.T) {
			t.Parallel()
			synctest.Test(t, func(t *testing.T) {
				done := make(chan struct{})
				var processErr error
				client, stdout, written := startWatchedClient(t, done, &processErr)

				result := make(chan error, 1)
				request := func() {
					go func() {
						_, err := client.Request(context.Background(), "test.method", nil)
						result <- err
					}()
				}
				if sent == "before close" {
					request()
					<-written
				}
				if err := stdout.Close(); err != nil {
					t.Fatalf("Close response stream failed: %v", err)
				}
				<-client.ConnectionClosed()
				if sent == "after close" {
					request()
				}
				// Admit the request's grace-period wait without consuming its deadline.
				synctest.Wait()
				processErr = errors.New("CLI process exited: exit status 3\nstderr: fatal error")
				close(done)

				select {
				case err := <-result:
					if err == nil || err.Error() != processErr.Error() {
						t.Fatalf("Request error = %v, want the process error %q", err, processErr)
					}
				case <-time.After(5 * time.Second):
					t.Fatal("Request did not return after the process exited")
				}
			})
		})
	}
}

func TestCompletionCancellationDoesNotWaitForPeerToRead(t *testing.T) {
	stdinR, stdinW := io.Pipe()
	stdoutR, stdoutW := io.Pipe()
	t.Cleanup(func() {
		stdinR.Close()
		stdoutW.Close()
	})
	client := NewClient(stdinW, stdoutR)
	client.Start()
	t.Cleanup(client.Stop)
	ctx, cancel := context.WithCancel(t.Context())
	defer cancel()
	result := make(chan error, 1)
	go func() {
		_, err := client.Request(ctx, "session.sendAndWait", map[string]string{"prompt": "question"})
		result <- err
	}()
	if _, err := newHeaderReader(stdinR).Read(); err != nil {
		t.Fatalf("Read request: %v", err)
	}
	cancel()
	select {
	case err := <-result:
		if !errors.Is(err, context.Canceled) {
			t.Fatalf("Request error = %v, want context.Canceled", err)
		}
	case <-time.After(5 * time.Second):
		t.Fatal("Completion cancellation waited for the peer to read the cancellation frame")
	}
}

func TestCompletionCancellationUsesTheWrittenRequestID(t *testing.T) {
	testCancellationUsesTheWrittenRequestID(t, "session.sendAndWait", nil)
}

func TestLocalModelWatchCancellationUsesTheWrittenRequestID(t *testing.T) {
	testCancellationUsesTheWrittenRequestID(t, "localModels.watch", map[string]int{"afterRevision": 1})
}

func testCancellationUsesTheWrittenRequestID(t *testing.T, method string, params any) {
	t.Helper()
	stdinR, stdinW := io.Pipe()
	stdoutR, stdoutW := io.Pipe()
	t.Cleanup(func() {
		stdinR.Close()
		stdoutW.Close()
	})
	client := NewClient(stdinW, stdoutR)
	client.Start()
	t.Cleanup(client.Stop)
	ctx, cancel := context.WithCancel(t.Context())
	defer cancel()
	result := make(chan error, 1)
	go func() {
		_, err := client.Request(ctx, method, params)
		result <- err
	}()
	reader := newHeaderReader(stdinR)
	frame, err := reader.Read()
	if err != nil {
		t.Fatalf("Read request: %v", err)
	}
	var request Request
	if err := json.Unmarshal(frame, &request); err != nil {
		t.Fatalf("Decode request: %v", err)
	}
	cancel()
	type readResult struct {
		frame []byte
		err   error
	}
	cancellation := make(chan readResult, 1)
	go func() {
		frame, err := reader.Read()
		cancellation <- readResult{frame, err}
	}()
	select {
	case read := <-cancellation:
		frame, err = read.frame, read.err
	case <-time.After(5 * time.Second):
		t.Fatal("The canceled wait did not send its cancellation frame")
	}
	if err != nil {
		t.Fatalf("Read cancellation: %v", err)
	}
	var notification struct {
		Method string `json:"method"`
		Params struct {
			ID json.RawMessage `json:"id"`
		} `json:"params"`
	}
	if err := json.Unmarshal(frame, &notification); err != nil {
		t.Fatalf("Decode cancellation: %v", err)
	}
	if notification.Method != "$/cancelRequest" || !bytes.Equal(notification.Params.ID, request.ID) {
		t.Fatalf("Cancellation = %s, want $/cancelRequest for ID %s", frame, request.ID)
	}
	if err := <-result; !errors.Is(err, context.Canceled) {
		t.Fatalf("Request error = %v, want context.Canceled", err)
	}
}

func TestAbandonedRevisionReadCancelsItsTransportRequest(t *testing.T) {
	for _, abandon := range []string{"cancel", "deadline"} {
		t.Run(abandon, func(t *testing.T) {
			synctest.Test(t, func(t *testing.T) {
				stdinR, stdinW := io.Pipe()
				stdoutR, stdoutW := io.Pipe()
				t.Cleanup(func() {
					stdinR.Close()
					stdoutW.Close()
				})
				client := NewClient(stdinW, stdoutR)
				client.Start()
				t.Cleanup(client.Stop)
				var ctx context.Context
				var cancel context.CancelFunc
				want := context.Canceled
				if abandon == "deadline" {
					ctx, cancel = context.WithTimeout(t.Context(), 250*time.Millisecond)
					want = context.DeadlineExceeded
				} else {
					ctx, cancel = context.WithCancel(t.Context())
				}
				defer cancel()
				result := make(chan error, 1)
				go func() {
					_, err := client.Request(ctx, "session.featureFlags.read", map[string]any{"sessionId": "session", "afterRevision": 7})
					result <- err
				}()
				reader := newHeaderReader(stdinR)
				frame, err := reader.Read()
				if err != nil {
					t.Fatalf("Read request: %v", err)
				}
				var request Request
				if err := json.Unmarshal(frame, &request); err != nil {
					t.Fatalf("Decode request: %v", err)
				}
				// Admit the response wait before advancing the deadline or cancelling.
				synctest.Wait()
				if abandon == "cancel" {
					cancel()
				} else {
					time.Sleep(250 * time.Millisecond)
				}
				frames := make(chan []byte, 1)
				go func() {
					frame, err := reader.Read()
					if err != nil {
						frame = nil
					}
					frames <- frame
				}()
				select {
				case frame = <-frames:
				case <-time.After(5 * time.Second):
					t.Fatal("Abandoned revision read sent no $/cancelRequest")
				}
				var notification struct {
					Method string `json:"method"`
					Params struct {
						ID json.RawMessage `json:"id"`
					} `json:"params"`
				}
				if err := json.Unmarshal(frame, &notification); err != nil {
					t.Fatalf("Decode cancellation %q: %v", frame, err)
				}
				if notification.Method != "$/cancelRequest" || !bytes.Equal(notification.Params.ID, request.ID) {
					t.Fatalf("Cancellation = %s, want $/cancelRequest for ID %s", frame, request.ID)
				}
				if err := <-result; !errors.Is(err, want) {
					t.Fatalf("Request error = %v, want %v", err, want)
				}
			})
		})
	}
}
func TestRequestUnacknowledgedShutdown(t *testing.T) {
	for _, shutdown := range []string{"stop", "connection closed", "process exited", "process error"} {
		t.Run(shutdown, func(t *testing.T) {
			stdinR, stdinW := io.Pipe()
			defer stdinR.Close()
			defer stdinW.Close()
			client := NewClient(stdinW, io.NopCloser(bytes.NewReader(nil)))
			done := make(chan struct{})
			var processErr error
			if shutdown == "process error" {
				processErr = errors.New("CLI process exited: exit status 1")
			}
			if shutdown == "process exited" || shutdown == "process error" {
				client.SetProcessDone(done, &processErr)
			}
			ctx, cancel := context.WithCancel(t.Context())
			finished := make(chan struct{})
			t.Cleanup(func() {
				cancel()
				stdinR.Close()
				select {
				case <-finished:
				case <-time.After(time.Second):
					t.Error("request did not finish during cleanup")
				}
			})
			result := make(chan error, 1)
			go func() {
				defer close(finished)
				_, err := client.Request(ctx, "test.method", nil)
				result <- err
			}()
			if _, err := newHeaderReader(stdinR).Read(); err != nil {
				t.Fatalf("Read request failed: %v", err)
			}
			var expected string
			switch shutdown {
			case "stop":
				close(client.stopChan)
				expected = "client stopped"
			case "connection closed":
				close(client.connectionClosed)
				expected = "connection closed"
			case "process exited":
				close(done)
				expected = "process exited unexpectedly"
			case "process error":
				close(done)
				expected = processErr.Error()
			}
			select {
			case err := <-result:
				if err == nil || err.Error() != expected {
					t.Fatalf("unacknowledged request error = %v, want %s", err, expected)
				}
			case <-time.After(time.Second):
				t.Fatal("unacknowledged request did not stop")
			}
			client.mu.Lock()
			pending, callbacks := len(client.pendingRequests), len(client.pendingInlineCallbacks)
			client.mu.Unlock()
			if pending != 0 || callbacks != 0 {
				t.Fatalf("request cleanup left %d pending requests and %d callbacks", pending, callbacks)
			}
		})
	}
}

func TestRequestAdmittedResponseSurvivesShutdown(t *testing.T) {
	for _, shutdown := range []string{"stop", "connection closed", "process exited", "process error"} {
		t.Run(shutdown, func(t *testing.T) {
			for _, rpcError := range []bool{false, true} {
				for iteration := range 32 {
					t.Run(fmt.Sprintf("RPC_error=%t/%d", rpcError, iteration), func(t *testing.T) {
						expected := json.RawMessage(`{"messageId":"acknowledged-message"}`)
						var expectedErr *Error
						if rpcError {
							expectedErr = &Error{Code: -32001, Message: "acknowledged failure", Data: json.RawMessage(`{"detail":"original"}`)}
						}
						done := make(chan struct{})
						var client *Client
						writer := writeFunc(func(data []byte) (int, error) {
							if bytes.HasPrefix(data, []byte("Content-Length:")) {
								return len(data), nil
							}
							var request Request
							if err := json.Unmarshal(data, &request); err != nil {
								return 0, err
							}
							// Both outcomes are ready before Request reaches its response select.
							client.handleResponse(&Response{ID: request.ID, Result: expected, Error: expectedErr})
							switch shutdown {
							case "stop":
								close(client.stopChan)
							case "connection closed":
								close(client.connectionClosed)
							default:
								close(done)
							}
							return len(data), nil
						})
						client = NewClient(writeCloser{Writer: writer}, io.NopCloser(bytes.NewReader(nil)))
						if shutdown == "process exited" || shutdown == "process error" {
							var processErr error
							if shutdown == "process error" {
								processErr = errors.New("CLI process exited: exit status 1")
							}
							client.SetProcessDone(done, &processErr)
						}

						result, err := client.Request(t.Context(), "test.method", nil)

						if expectedErr != nil {
							if result != nil || err != expectedErr {
								t.Fatalf("admitted error = (%s, %v), want original RPC error %v", result, err, expectedErr)
							}
						} else if err != nil || !bytes.Equal(result, expected) {
							t.Fatalf("admitted response = (%s, %v), want (%s, nil)", result, err, expected)
						}
						if len(client.pendingRequests) != 0 || len(client.pendingInlineCallbacks) != 0 {
							t.Fatal("completed request retained its pending response or callback")
						}
					})
				}
			}
		})
	}
}

// TestSetProcessDone_ErrorAvailableImmediately validates that getProcessError()
// returns the correct error immediately after processDone is closed.
// The current implementation stores a pointer to the process error
// synchronously when the processDone channel is closed, so callers should
// never observe a nil error after the channel has been closed.
func TestSetProcessDone_ErrorAvailableImmediately(t *testing.T) {
	misses := 0
	const iterations = 1000

	for i := 0; i < iterations; i++ {
		stdinR, stdinW := io.Pipe()
		stdoutR, stdoutW := io.Pipe()

		client := NewClient(stdinW, stdoutR)

		done := make(chan struct{})
		processErr := errors.New("CLI process exited: exit status 1")

		client.SetProcessDone(done, &processErr)

		// Simulate process exit: error is already set, close the channel.
		close(done)

		// Do NOT yield to the scheduler — check immediately.
		// In the current code the goroutine inside SetProcessDone may not
		// have copied the error to client.processError yet.
		if err := client.getProcessError(); err == nil {
			misses++
		}

		stdinR.Close()
		stdinW.Close()
		stdoutR.Close()
		stdoutW.Close()
	}

	if misses > 0 {
		t.Errorf("SetProcessDone regression: getProcessError() returned nil %d/%d times "+
			"immediately after processDone was closed, even though the error pointer "+
			"should be stored synchronously.", misses, iterations)
	}
}

// TestSetProcessDone_RequestMissesProcessError validates that the Request()
// method returns the specific process error instead of the generic
// "process exited unexpectedly" message once processDone has been closed.
func TestSetProcessDone_RequestMissesProcessError(t *testing.T) {
	misses := 0
	const iterations = 100

	for i := 0; i < iterations; i++ {
		stdinR, stdinW := io.Pipe()
		stdoutR, stdoutW := io.Pipe()

		client := NewClient(stdinW, stdoutR)
		client.Start()

		done := make(chan struct{})
		processErr := errors.New("CLI process exited: authentication failed")

		client.SetProcessDone(done, &processErr)

		// Simulate process exit.
		close(done)
		// Close the writer so the readLoop can exit.
		stdoutW.Close()

		// Make a request — should get the specific process error.
		_, err := client.Request(context.Background(), "test.method", nil)
		if err != nil && err.Error() == "process exited unexpectedly" {
			misses++
		}

		client.Stop()
		stdinR.Close()
		stdinW.Close()
		stdoutR.Close()
	}

	if misses > 0 {
		t.Errorf("Request() bug: returned generic 'process exited unexpectedly' %d/%d times "+
			"instead of the actual process error after process exit; the process "+
			"error was not correctly propagated from SetProcessDone.", misses, iterations)
	}
}

// TestSetProcessDone_ErrorAvailableImmediately verifies that the process error
// is available as soon as the done channel is closed, matching the
// pointer-based implementation where no asynchronous copy is required.
func TestSetProcessDone_ErrorCopiedEventually(t *testing.T) {
	stdinR, stdinW := io.Pipe()
	stdoutR, stdoutW := io.Pipe()
	defer stdinR.Close()
	defer stdinW.Close()
	defer stdoutR.Close()
	defer stdoutW.Close()

	client := NewClient(stdinW, stdoutR)

	done := make(chan struct{})
	processErr := errors.New("CLI process exited: version mismatch")

	client.SetProcessDone(done, &processErr)

	// Close the channel: the process error should now be observable immediately,
	// without needing to yield to another goroutine.
	close(done)

	err := client.getProcessError()
	if err == nil {
		t.Fatal("expected process error to be available immediately after done is closed, got nil")
	}
	if err.Error() != processErr.Error() {
		t.Errorf("expected %q, got %q", processErr.Error(), err.Error())
	}
}

func TestRequestReturnsContextErrorIfCanceledBeforeSend(t *testing.T) {
	var stdin bytes.Buffer
	client := NewClient(writeCloser{Writer: &stdin}, io.NopCloser(bytes.NewReader(nil)))

	ctx, cancel := context.WithCancel(context.Background())
	cancel()

	_, err := client.Request(ctx, "test.method", nil)
	if !errors.Is(err, context.Canceled) {
		t.Fatalf("expected context.Canceled, got %v", err)
	}
	if stdin.Len() != 0 {
		t.Fatalf("expected no request to be written after cancellation, got %d bytes", stdin.Len())
	}
	client.mu.Lock()
	pending := len(client.pendingRequests)
	client.mu.Unlock()
	if pending != 0 {
		t.Fatalf("expected no pending requests after cancellation, got %d", pending)
	}
}

func TestRequestReturnsContextErrorWhileAwaitingResponse(t *testing.T) {
	written := make(chan struct{}, 1)
	client := NewClient(writeCloser{Writer: writeFunc(func(data []byte) (int, error) {
		select {
		case written <- struct{}{}:
		default:
		}
		return len(data), nil
	})}, io.NopCloser(bytes.NewReader(nil)))
	ctx, cancel := context.WithCancel(context.Background())

	errCh := make(chan error, 1)
	go func() {
		_, err := client.Request(ctx, "test.method", map[string]string{"hello": "world"})
		errCh <- err
	}()

	select {
	case <-written:
	case <-time.After(time.Second):
		t.Fatal("timed out waiting for request write")
	}
	cancel()

	select {
	case err := <-errCh:
		if !errors.Is(err, context.Canceled) {
			t.Fatalf("expected context.Canceled, got %v", err)
		}
	case <-time.After(time.Second):
		t.Fatal("request did not return after context cancellation")
	}

	client.mu.Lock()
	pending := len(client.pendingRequests)
	client.mu.Unlock()
	if pending != 0 {
		t.Fatalf("expected pending request cleanup after cancellation, got %d", pending)
	}
}

func TestSendMessageReturnsContextErrorWhileWaitingForWriter(t *testing.T) {
	var stdin bytes.Buffer
	client := NewClient(writeCloser{Writer: &stdin}, io.NopCloser(bytes.NewReader(nil)))
	w := <-client.writer
	defer func() { client.writer <- w }()

	ctx, cancel := context.WithCancel(context.Background())
	errCh := make(chan error, 1)
	go func() {
		errCh <- client.sendMessage(ctx, Request{JSONRPC: version, Method: "test.method"})
	}()

	cancel()
	select {
	case err := <-errCh:
		if !errors.Is(err, context.Canceled) {
			t.Fatalf("expected context.Canceled, got %v", err)
		}
	case <-time.After(time.Second):
		t.Fatal("sendMessage did not return after context cancellation")
	}
}
