package copilot

import (
	"bufio"
	"context"
	"encoding/json"
	"fmt"
	"net"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"testing"
	"time"
)

// serveRuntimeStandIn answers connections on listener the way a failing
// runtime would. In drop mode it closes every connection it accepts, so the
// handshake fails. In serve mode it accepts one connection, stops listening,
// answers the connect handshake, reads one more request without answering it
// and returns, closing the connection.
func serveRuntimeStandIn(listener net.Listener, mode string) {
	switch mode {
	case "drop":
		for {
			conn, err := listener.Accept()
			if err != nil {
				return
			}
			conn.Close()
		}
	case "serve":
		conn, err := listener.Accept()
		if err != nil {
			return
		}
		// Stop listening before the connection closes, so a reconnect is refused.
		listener.Close()
		defer conn.Close()
		reader := bufio.NewReader(conn)
		frame, err := readTestJSONRPCFrame(reader)
		if err != nil {
			return
		}
		var request struct {
			ID json.RawMessage `json:"id"`
		}
		if err := json.Unmarshal(frame, &request); err != nil {
			return
		}
		response, _ := json.Marshal(map[string]any{
			"jsonrpc": "2.0",
			"id":      request.ID,
			"result":  map[string]any{"protocolVersion": GetSDKProtocolVersion()},
		})
		fmt.Fprintf(conn, "Content-Length: %d\r\n\r\n%s", len(response), response)
		_, _ = readTestJSONRPCFrame(reader)
	}
}

// TestRuntimePortStandInProcess stands in for a runtime the client starts over
// TCP. It announces a port the way the runtime does, then, depending on
// GO_WANT_RUNTIME_PORT_STAND_IN, refuses connections (refuse), runs
// serveRuntimeStandIn (drop), or runs it and exits with status 3 (serve).
// When the file named by RUNTIME_PORT_STAND_IN_ONCE exists, it exits before
// announcing a port; otherwise it creates that file, so only its first launch
// starts.
func TestRuntimePortStandInProcess(t *testing.T) {
	mode := os.Getenv("GO_WANT_RUNTIME_PORT_STAND_IN")
	if mode == "" {
		return
	}
	if once := os.Getenv("RUNTIME_PORT_STAND_IN_ONCE"); once != "" {
		if _, err := os.Stat(once); err == nil {
			os.Exit(4)
		}
		if err := os.WriteFile(once, nil, 0o600); err != nil {
			os.Exit(5)
		}
	}
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		os.Exit(5)
	}
	// Never outlive the test that started it.
	time.AfterFunc(time.Minute, func() { os.Exit(6) })
	port := listener.Addr().(*net.TCPAddr).Port
	if mode == "refuse" {
		listener.Close()
	}
	fmt.Printf("CLI server listening on port %d\n", port)
	switch mode {
	case "refuse":
		time.Sleep(time.Minute)
	case "drop":
		serveRuntimeStandIn(listener, mode)
	case "serve":
		serveRuntimeStandIn(listener, mode)
		os.Exit(3)
	}
	os.Exit(0)
}

func runtimePortStandIn(mode, once string) TCPConnection {
	env := append(os.Environ(), "GO_WANT_RUNTIME_PORT_STAND_IN="+mode)
	if once != "" {
		env = append(env, "RUNTIME_PORT_STAND_IN_ONCE="+once)
	}
	return TCPConnection{
		Path: os.Args[0],
		Args: []string{"-test.run=^TestRuntimePortStandInProcess$", "--"},
		Env:  env,
	}
}

func TestClient_RuntimePortAfterFailedStart(t *testing.T) {
	for _, tc := range []struct {
		name    string
		mode    string
		wantErr string
	}{
		{name: "connect refused after the port is announced", mode: "refuse", wantErr: "failed to connect to CLI server"},
		{name: "handshake dropped after the port is announced", mode: "drop"},
	} {
		t.Run(tc.name, func(t *testing.T) {
			client := NewClient(&ClientOptions{Connection: runtimePortStandIn(tc.mode, "")})
			t.Cleanup(client.ForceStop)
			if err := client.Start(t.Context()); err == nil || !strings.Contains(err.Error(), tc.wantErr) {
				t.Fatalf("Start error = %v, want a failure containing %q", err, tc.wantErr)
			}
			if port := client.RuntimePort(); port != 0 {
				t.Errorf("RuntimePort after a failed Start = %d, want 0", port)
			}
		})
	}

	for _, tc := range []struct {
		name              string
		replacementStarts bool
	}{
		{name: "restart after the runtime exited reports the new runtime's port", replacementStarts: true},
		{name: "failed restart after the runtime exited", replacementStarts: false},
	} {
		t.Run(tc.name, func(t *testing.T) {
			once := ""
			if !tc.replacementStarts {
				once = filepath.Join(t.TempDir(), "started")
			}
			client := NewClient(&ClientOptions{Connection: runtimePortStandIn("serve", once)})
			t.Cleanup(client.ForceStop)
			ctx, cancel := context.WithTimeout(t.Context(), 30*time.Second)
			defer cancel()
			if err := client.Start(ctx); err != nil {
				t.Fatalf("Start failed: %v", err)
			}
			if port := client.RuntimePort(); port == 0 {
				t.Fatal("RuntimePort after a successful Start = 0, want the announced port")
			}
			// The stand-in exits on this request.
			if _, err := client.Ping(ctx, ""); err == nil || !strings.Contains(err.Error(), "exit status 3") {
				t.Fatalf("Ping error = %v, want the runtime's exit status", err)
			}
			err := client.Start(ctx)
			if tc.replacementStarts {
				if err != nil {
					t.Fatalf("Start after the runtime exited failed: %v", err)
				}
				if port := client.RuntimePort(); port == 0 {
					t.Error("RuntimePort after a successful restart = 0, want the new runtime's port")
				}
				return
			}
			// The replacement runtime exits before it announces a port.
			if err == nil || !strings.Contains(err.Error(), "exited before reporting port") {
				t.Fatalf("Start error = %v, want the replacement runtime's start failure", err)
			}
			if port := client.RuntimePort(); port != 0 {
				t.Errorf("RuntimePort after a failed restart = %d, want 0", port)
			}
		})
	}
}

// A client of an external runtime reports the configured port whether or not
// Start succeeds, because the client does not own that runtime.
func TestClient_RuntimePortOfExternalServerAfterFailedStart(t *testing.T) {
	listen := func(t *testing.T) (net.Listener, int) {
		t.Helper()
		listener, err := net.Listen("tcp", "127.0.0.1:0")
		if err != nil {
			t.Fatal(err)
		}
		t.Cleanup(func() { listener.Close() })
		return listener, listener.Addr().(*net.TCPAddr).Port
	}
	external := func(t *testing.T, port int) *Client {
		t.Helper()
		client := NewClient(&ClientOptions{Connection: URIConnection{URL: "127.0.0.1:" + strconv.Itoa(port)}})
		t.Cleanup(client.ForceStop)
		return client
	}

	t.Run("connect refused", func(t *testing.T) {
		listener, port := listen(t)
		listener.Close()
		client := external(t, port)
		if err := client.Start(t.Context()); err == nil || !strings.Contains(err.Error(), "failed to connect to CLI server") {
			t.Fatalf("Start error = %v, want a connect failure", err)
		}
		if got := client.RuntimePort(); got != port {
			t.Errorf("RuntimePort = %d, want the configured port %d", got, port)
		}
	})

	t.Run("handshake dropped", func(t *testing.T) {
		listener, port := listen(t)
		go serveRuntimeStandIn(listener, "drop")
		client := external(t, port)
		if err := client.Start(t.Context()); err == nil {
			t.Fatal("Start succeeded against a server that drops every connection")
		}
		if got := client.RuntimePort(); got != port {
			t.Errorf("RuntimePort = %d, want the configured port %d", got, port)
		}
	})

	t.Run("restart after the connection was lost", func(t *testing.T) {
		listener, port := listen(t)
		go serveRuntimeStandIn(listener, "serve")
		client := external(t, port)
		ctx, cancel := context.WithTimeout(t.Context(), 30*time.Second)
		defer cancel()
		if err := client.Start(ctx); err != nil {
			t.Fatalf("Start failed: %v", err)
		}
		// The server closes the connection and stops listening on this request.
		if _, err := client.Ping(ctx, ""); err == nil {
			t.Fatal("Ping succeeded after the server closed the connection")
		}
		if err := client.Start(ctx); err == nil || !strings.Contains(err.Error(), "failed to connect to CLI server") {
			t.Fatalf("Start error = %v, want a connect failure", err)
		}
		if got := client.RuntimePort(); got != port {
			t.Errorf("RuntimePort = %d, want the configured port %d", got, port)
		}
	})
}
