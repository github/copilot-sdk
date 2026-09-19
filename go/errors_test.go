package copilot_test

import (
	"bufio"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net"
	"strconv"
	"strings"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
)

func TestRPCErrorData(t *testing.T) {
	payloads := []struct {
		name string
		data string
	}{
		{"object", `{"reason":"private detail","nested":{"values":[1,null,false]}}`},
		{"array", `[1,"private detail",{"nested":true}]`},
		{"string", `"private detail"`},
		{"integer", `9007199254740993`},
		{"fraction", `1.25`},
		{"zero", `0`},
		{"true", `true`},
		{"false", `false`},
		{"empty object", `{}`},
		{"empty array", `[]`},
		{"empty string", `""`},
		{"omitted", ``},
		{"null", `null`},
	}

	for _, payload := range payloads {
		t.Run(payload.name, func(t *testing.T) {
			client := newRPCErrorClient(t, payload.data)
			ctx, cancel := context.WithTimeout(t.Context(), 10*time.Second)
			defer cancel()

			for _, method := range []string{"status.get", "session.create", "models.list"} {
				var err error
				switch method {
				case "session.create":
					_, err = client.CreateSession(ctx, &copilot.SessionConfig{})
				case "status.get":
					_, err = client.GetStatus(ctx)
				case "models.list":
					_, err = client.RPC.Models.List(ctx, nil)
				}

				var rpcErr *copilot.RPCError
				if !errors.As(err, &rpcErr) {
					t.Fatalf("%s: expected RPCError, got %T: %v", method, err, err)
				}
				if rpcErr.Code != -32000 || rpcErr.Message != "request rejected" {
					t.Fatalf("unexpected RPC error: %+v", rpcErr)
				}
				if string(rpcErr.Data) != payload.data {
					t.Fatalf("data = %s, want %s", rpcErr.Data, payload.data)
				}
				if (rpcErr.Data == nil) != (payload.data == "") {
					t.Fatalf("data presence = %v, want %v", rpcErr.Data != nil, payload.data != "")
				}
				want := "JSON-RPC Error -32000: request rejected"
				if rpcErr.Error() != want {
					t.Fatalf("RPC error string = %q, want %q", rpcErr.Error(), want)
				}
				if method == "session.create" {
					want = "failed to create session: " + want
					if errors.Unwrap(err) != rpcErr {
						t.Fatal("SDK wrapper did not preserve the RPC error identity")
					}
				} else if err != rpcErr {
					t.Fatal("direct call changed the RPC error identity")
				}
				if err.Error() != want {
					t.Fatalf("error string = %q, want %q", err.Error(), want)
				}
				outer := fmt.Errorf("application context: %w", err)
				var recovered *copilot.RPCError
				if !errors.As(outer, &recovered) || recovered != rpcErr {
					t.Fatal("application wrapper did not preserve errors.As access")
				}
			}

			response, err := client.Ping(ctx, "still connected")
			if err != nil || response.Message != "still connected" {
				t.Fatalf("successful response after RPC errors: response=%+v, err=%v", response, err)
			}
		})
	}
}

func TestRPCErrorNonRPCFailure(t *testing.T) {
	client := copilot.NewClient(&copilot.ClientOptions{})
	_, err := client.GetStatus(t.Context())
	var rpcErr *copilot.RPCError
	if err == nil || errors.As(err, &rpcErr) {
		t.Fatalf("expected a non-RPC error, got %T: %v", err, err)
	}
}

// Use a raw framed peer so these tests exercise the public API without importing
// the SDK's internal transport or re-encoding the error through RPCError.
func newRPCErrorClient(t *testing.T, data string) *copilot.Client {
	t.Helper()
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	done := make(chan error, 1)
	go func() {
		done <- serveRPCErrorPeer(listener, data)
	}()
	t.Cleanup(func() {
		listener.Close()
		select {
		case err := <-done:
			if err != nil && !errors.Is(err, io.EOF) && !errors.Is(err, net.ErrClosed) {
				t.Errorf("RPC peer: %v", err)
			}
		case <-time.After(10 * time.Second):
			t.Error("RPC peer did not stop")
		}
	})

	client := copilot.NewClient(&copilot.ClientOptions{
		Connection: copilot.URIConnection{URL: listener.Addr().String()},
	})
	t.Cleanup(client.ForceStop)
	ctx, cancel := context.WithTimeout(t.Context(), 10*time.Second)
	defer cancel()
	if err := client.Start(ctx); err != nil {
		t.Fatal(err)
	}
	return client
}

func serveRPCErrorPeer(listener net.Listener, data string) error {
	conn, err := listener.Accept()
	if err != nil {
		return err
	}
	defer conn.Close()
	if err := conn.SetDeadline(time.Now().Add(10 * time.Second)); err != nil {
		return err
	}
	reader := bufio.NewReader(conn)
	for {
		body, err := readRPCErrorFrame(reader)
		if err != nil {
			return err
		}
		var request struct {
			ID     json.RawMessage `json:"id"`
			Method string          `json:"method"`
		}
		if err := json.Unmarshal(body, &request); err != nil {
			return err
		}
		if len(request.ID) == 0 {
			continue
		}
		var response string
		switch request.Method {
		case "connect":
			response = `"result":{"ok":true,"protocolVersion":3,"version":"test"}`
		case "status.get", "session.create", "models.list":
			response = `"error":{"code":-32000,"message":"request rejected"`
			if data != "" {
				response += `,"data":` + data
			}
			response += `}`
		case "ping":
			response = `"result":{"message":"still connected","timestamp":"2026-01-01T00:00:00Z"}`
		default:
			return fmt.Errorf("unexpected method %q", request.Method)
		}
		frame := fmt.Sprintf(`{"jsonrpc":"2.0","id":%s,%s}`, request.ID, response)
		if _, err := fmt.Fprintf(conn, "Content-Length: %d\r\n\r\n%s", len(frame), frame); err != nil {
			return err
		}
	}
}

func readRPCErrorFrame(reader *bufio.Reader) ([]byte, error) {
	length := 0
	for {
		line, err := reader.ReadString('\n')
		if err != nil {
			return nil, err
		}
		line = strings.TrimSpace(line)
		if line == "" {
			break
		}
		name, value, ok := strings.Cut(line, ":")
		if ok && name == "Content-Length" {
			length, err = strconv.Atoi(strings.TrimSpace(value))
			if err != nil {
				return nil, err
			}
		}
	}
	if length <= 0 || length > 1024*1024 {
		return nil, fmt.Errorf("invalid Content-Length %d", length)
	}
	body := make([]byte, length)
	_, err := io.ReadFull(reader, body)
	return body, err
}
