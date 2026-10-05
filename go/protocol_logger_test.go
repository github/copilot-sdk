// Copyright (c) Microsoft Corporation. All rights reserved.

package copilot_test

import (
	"bufio"
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log/slog"
	"net"
	"os"
	"strings"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
)

const protocolLoggerPeerEnv = "COPILOT_SDK_PROTOCOL_LOGGER_PEER"
const protocolLoggerMarker = "SYNTHETIC_SENSITIVE_PROTOCOL_VERSION"

func TestProtocolLogger(t *testing.T) {
	for _, transport := range []string{"stdio", "tcp"} {
		t.Run(transport, func(t *testing.T) {
			t.Parallel()
			var output bytes.Buffer
			options := &copilot.ClientOptions{
				LogLevel:       "none",
				ProtocolLogger: slog.New(slog.NewJSONHandler(&output, nil)),
			}
			if transport == "stdio" {
				executable, err := os.Executable()
				if err != nil {
					t.Fatal(err)
				}
				options.Connection = copilot.StdioConnection{
					Path: executable,
					Args: []string{"-test.run=^TestProtocolLoggerPeer$", "--"},
					Env:  append(os.Environ(), protocolLoggerPeerEnv+"=1"),
				}
				options.UseLoggedInUser = copilot.Bool(false)
			} else {
				listener, err := net.Listen("tcp", "127.0.0.1:0")
				if err != nil {
					t.Fatal(err)
				}
				done := make(chan error, 1)
				go func() {
					conn, err := listener.Accept()
					if err != nil {
						done <- err
						return
					}
					defer conn.Close()
					if err := conn.SetDeadline(time.Now().Add(10 * time.Second)); err != nil {
						done <- err
						return
					}
					done <- serveProtocolLoggerPeer(conn, conn)
				}()
				t.Cleanup(func() {
					listener.Close()
					select {
					case err := <-done:
						if err != nil {
							t.Error(err)
						}
					case <-time.After(10 * time.Second):
						t.Error("protocol logger peer did not stop")
					}
				})
				options.Connection = copilot.URIConnection{URL: listener.Addr().String()}
			}

			client := copilot.NewClient(options)
			t.Cleanup(client.ForceStop)
			ctx, cancel := context.WithTimeout(t.Context(), 10*time.Second)
			defer cancel()
			if err := client.Start(ctx); err != nil {
				t.Fatal(err)
			}
			if err := client.Stop(); err != nil {
				t.Fatal(err)
			}

			var record struct {
				Level   string `json:"level"`
				Message string `json:"msg"`
				Error   string `json:"error"`
			}
			if err := json.NewDecoder(&output).Decode(&record); err != nil {
				t.Fatalf("expected a protocol diagnostic: %v", err)
			}
			if record.Level != "ERROR" || record.Message != "Error decoding message" || !strings.Contains(record.Error, protocolLoggerMarker) {
				t.Fatalf("protocol logger did not receive the decode error: %+v", record)
			}
		})
	}
}

func TestProtocolLoggerPeer(t *testing.T) {
	if os.Getenv(protocolLoggerPeerEnv) != "1" {
		return
	}
	if err := serveProtocolLoggerPeer(os.Stdin, os.Stdout); err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
	os.Exit(0)
}

func serveProtocolLoggerPeer(input io.Reader, output io.Writer) error {
	reader := bufio.NewReader(input)
	writeFrame := func(body string) error {
		_, err := fmt.Fprintf(output, "Content-Length: %d\r\n\r\n%s", len(body), body)
		return err
	}
	for {
		body, err := readRPCErrorFrame(reader)
		if errors.Is(err, io.EOF) {
			return nil
		}
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
		var result string
		switch request.Method {
		case "connect":
			if err := writeFrame(`{"jsonrpc":"` + protocolLoggerMarker + `","method":"test.notification"}`); err != nil {
				return err
			}
			result = fmt.Sprintf(`{"ok":true,"protocolVersion":%d,"version":"test"}`, copilot.GetSDKProtocolVersion())
		case "runtime.shutdown":
			result = `{}`
		default:
			return fmt.Errorf("unexpected method %q", request.Method)
		}
		if err := writeFrame(fmt.Sprintf(`{"jsonrpc":"2.0","id":%s,"result":%s}`, request.ID, result)); err != nil {
			return err
		}
	}
}
