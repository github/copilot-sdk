// Copyright (c) Microsoft Corporation. All rights reserved.

package copilot_test

import (
	"bufio"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net"
	"runtime"
	"strconv"
	"strings"
	"sync"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
)

type responseRetirementPeer struct {
	listener net.Listener
	mu       sync.Mutex
	conn     net.Conn
	writeMu  sync.Mutex
	sent     chan struct{}
	done     chan error
}

func newResponseRetirementPeer(t *testing.T, expectedSends int) *responseRetirementPeer {
	t.Helper()
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	peer := &responseRetirementPeer{
		listener: listener,
		sent:     make(chan struct{}),
		done:     make(chan error, 1),
	}
	t.Cleanup(func() {
		if err := listener.Close(); err != nil && !errors.Is(err, net.ErrClosed) {
			t.Error("close peer listener:", err)
		}
		peer.mu.Lock()
		conn := peer.conn
		peer.mu.Unlock()
		if conn != nil {
			if err := conn.Close(); err != nil && !errors.Is(err, net.ErrClosed) {
				t.Error("close peer connection:", err)
			}
		}
		select {
		case err := <-peer.done:
			if err != nil {
				t.Error("TCP peer:", err)
			}
		case <-time.After(5 * time.Second):
			t.Error("TCP peer did not exit")
		}
	})
	go func() { peer.done <- peer.serve(expectedSends) }()
	return peer
}

func (p *responseRetirementPeer) serve(expectedSends int) error {
	conn, err := p.listener.Accept()
	if err != nil {
		if errors.Is(err, net.ErrClosed) {
			return nil
		}
		return err
	}
	p.mu.Lock()
	p.conn = conn
	p.mu.Unlock()
	defer conn.Close()
	reader := bufio.NewReader(conn)
	sends := 0
	for {
		frame, err := readRetirementFrame(reader)
		if err != nil {
			if errors.Is(err, io.EOF) || errors.Is(err, net.ErrClosed) {
				return nil
			}
			return err
		}
		var request struct {
			ID     json.RawMessage `json:"id"`
			Method string          `json:"method"`
		}
		if err := json.Unmarshal(frame, &request); err != nil {
			return err
		}
		var result any
		switch request.Method {
		case "connect":
			result = map[string]any{"ok": true, "protocolVersion": copilot.SDKProtocolVersion, "version": "force-stop-test"}
		case "session.create":
			result = map[string]any{"sessionId": "retirement-session"}
		case "session.send":
			sends++
			result = map[string]any{"messageId": fmt.Sprintf("message-%d", sends)}
		case "ping":
			result = map[string]any{"message": "pong", "timestamp": "2026-01-01T00:00:00Z", "protocolVersion": copilot.SDKProtocolVersion}
		default:
			return fmt.Errorf("unexpected RPC: %s", request.Method)
		}
		if err := p.write(map[string]any{"jsonrpc": "2.0", "id": request.ID, "result": result}); err != nil {
			return err
		}
		if request.Method == "session.send" && sends == expectedSends {
			close(p.sent)
		}
	}
}

func readRetirementFrame(reader *bufio.Reader) ([]byte, error) {
	length := -1
	for {
		line, err := reader.ReadString('\n')
		if err != nil {
			if errors.Is(err, io.EOF) && line == "" && length == -1 {
				return nil, io.EOF
			}
			return nil, fmt.Errorf("read frame header: %w", err)
		}
		if line == "\r\n" {
			break
		}
		name, value, ok := strings.Cut(line, ":")
		if !ok {
			return nil, fmt.Errorf("invalid frame header: %q", line)
		}
		if strings.EqualFold(name, "Content-Length") {
			if length != -1 {
				return nil, errors.New("duplicate Content-Length")
			}
			length, err = strconv.Atoi(strings.TrimSpace(value))
			if err != nil {
				return nil, err
			}
		}
	}
	if length <= 0 || length > 128*1024 {
		return nil, fmt.Errorf("invalid frame length: %d", length)
	}
	body := make([]byte, length)
	if _, err := io.ReadFull(reader, body); err != nil {
		return nil, err
	}
	return body, nil
}

func (p *responseRetirementPeer) write(message any) error {
	body, err := json.Marshal(message)
	if err != nil {
		return err
	}
	p.writeMu.Lock()
	defer p.writeMu.Unlock()
	p.mu.Lock()
	conn := p.conn
	p.mu.Unlock()
	_, err = fmt.Fprintf(conn, "Content-Length: %d\r\n\r\n%s", len(body), body)
	return err
}

type responseRetirementCapture struct {
	data [64]byte
}

func capturedRetirementHandler() (copilot.SessionEventHandler, <-chan struct{}) {
	capture := &responseRetirementCapture{data: [64]byte{1}}
	collected := make(chan struct{})
	runtime.AddCleanup(capture, func(done chan struct{}) { close(done) }, collected)
	return func(copilot.SessionEvent) { runtime.KeepAlive(capture) }, collected
}

func awaitRetirementCapture(t *testing.T, collected <-chan struct{}) {
	t.Helper()
	deadline := time.Now().Add(5 * time.Second)
	for {
		runtime.GC()
		select {
		case <-collected:
			return
		default:
		}
		if time.Now().After(deadline) {
			t.Fatal("force-stopped retained session still roots an unadmitted callback capture")
		}
		runtime.Gosched()
	}
}

func TestForceStopReleasesRetainedEventCallbacks(t *testing.T) {
	for _, blockedHandler := range []bool{false, true} {
		t.Run(fmt.Sprintf("blocked handler=%t", blockedHandler), func(t *testing.T) {
			t.Parallel()
			peer := newResponseRetirementPeer(t, 0)
			client := copilot.NewClient(&copilot.ClientOptions{
				Connection: copilot.URIConnection{URL: peer.listener.Addr().String()},
			})
			t.Cleanup(client.ForceStop)
			ctx, cancel := context.WithTimeout(t.Context(), 10*time.Second)
			defer cancel()
			if err := client.Start(ctx); err != nil {
				t.Fatal("start:", err)
			}
			session, err := client.CreateSession(ctx, &copilot.SessionConfig{
				SessionID: "retirement-session", OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			})
			if err != nil {
				t.Fatal("create:", err)
			}
			entered := make(chan struct{})
			release := make(chan struct{})
			exited := make(chan struct{})
			admitted := false
			releaseHandler := sync.OnceFunc(func() { close(release) })
			t.Cleanup(func() {
				client.ForceStop()
				releaseHandler()
				if admitted {
					select {
					case <-exited:
					case <-time.After(5 * time.Second):
						t.Error("admitted event handler did not finish")
					}
				}
			})
			if blockedHandler {
				session.On(func(copilot.SessionEvent) {
					close(entered)
					<-release
					close(exited)
				})
			}
			handler, collected := capturedRetirementHandler()
			unsubscribe := session.On(handler)
			if blockedHandler {
				if err := peer.write(map[string]any{
					"jsonrpc": "2.0", "method": "session.event",
					"params": map[string]any{
						"sessionId": session.SessionID,
						"event": map[string]any{
							"id": "00000000-0000-4000-8000-000000000002", "parentId": nil,
							"timestamp": "2026-01-01T00:00:00Z", "type": "session.idle",
							"ephemeral": true, "data": map[string]any{},
						},
					},
				}); err != nil {
					t.Fatal("notify:", err)
				}
				select {
				case <-entered:
					admitted = true
				case <-ctx.Done():
					t.Fatal("event handler did not enter:", ctx.Err())
				}
			}
			client.ForceStop()
			awaitRetirementCapture(t, collected)
			runtime.KeepAlive(session)
			runtime.KeepAlive(unsubscribe)
		})
	}
}

func createRetirementHookSession(t *testing.T, ctx context.Context, client *copilot.Client) (*copilot.Session, <-chan struct{}) {
	t.Helper()
	capture := &responseRetirementCapture{data: [64]byte{2}}
	collected := make(chan struct{})
	runtime.AddCleanup(capture, func(done chan struct{}) { close(done) }, collected)
	session, err := client.CreateSession(ctx, &copilot.SessionConfig{
		SessionID: "retirement-session", OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		Hooks: &copilot.SessionHooks{
			OnSessionEnd: func(copilot.SessionEndHookInput, copilot.HookInvocation) (*copilot.SessionEndHookOutput, error) {
				runtime.KeepAlive(capture)
				return nil, nil
			},
		},
	})
	if err != nil {
		t.Fatal("create:", err)
	}
	return session, collected
}

func TestForceStopReleasesRetainedHookCallbacks(t *testing.T) {
	peer := newResponseRetirementPeer(t, 0)
	client := copilot.NewClient(&copilot.ClientOptions{
		Connection: copilot.URIConnection{URL: peer.listener.Addr().String()},
	})
	t.Cleanup(client.ForceStop)
	ctx, cancel := context.WithTimeout(t.Context(), 10*time.Second)
	defer cancel()
	if err := client.Start(ctx); err != nil {
		t.Fatal("start:", err)
	}
	session, collected := createRetirementHookSession(t, ctx, client)
	client.ForceStop()
	awaitRetirementCapture(t, collected)
	runtime.KeepAlive(session)
}

func TestForceStopRejectsAcknowledgedPlainResponseWaits(t *testing.T) {
	for _, count := range []int{1, 2} {
		for _, blockedHandler := range []bool{false, true} {
			t.Run(fmt.Sprintf("%d waits/blocked handler=%t", count, blockedHandler), func(t *testing.T) {
				t.Parallel()
				peer := newResponseRetirementPeer(t, count)
				client := copilot.NewClient(&copilot.ClientOptions{
					Connection: copilot.URIConnection{URL: peer.listener.Addr().String()},
				})
				t.Cleanup(client.ForceStop)
				operationCtx, cancelOperation := context.WithTimeout(t.Context(), 10*time.Second)
				defer cancelOperation()
				if err := client.Start(operationCtx); err != nil {
					t.Fatal("start:", err)
				}

				session, err := client.CreateSession(operationCtx, &copilot.SessionConfig{
					SessionID: "retirement-session", OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
				})
				if err != nil {
					t.Fatal("create session:", err)
				}

				entered := make(chan struct{})
				release := make(chan struct{})
				releaseHandler := sync.OnceFunc(func() { close(release) })
				t.Cleanup(releaseHandler)
				if blockedHandler {
					session.On(func(copilot.SessionEvent) {
						close(entered)
						<-release
					})
				}
				waitCtx, cancelWait := context.WithCancel(t.Context())
				results := make(chan error, count)
				var waits sync.WaitGroup
				t.Cleanup(func() {
					cancelWait()
					releaseHandler()
					waits.Wait()
				})
				for range count {
					waits.Add(1)
					go func() {
						defer waits.Done()
						_, err := session.SendAndWait(waitCtx, copilot.MessageOptions{Prompt: "hold the response"})
						results <- err
					}()
				}
				select {
				case <-peer.sent:
				case <-operationCtx.Done():
					t.Fatal("send acknowledgments:", operationCtx.Err())
				}
				// This later reply proves every preceding send acknowledgment reached the transport.
				if _, err := client.Ping(operationCtx, "barrier"); err != nil {
					t.Fatal("ping:", err)
				}
				if blockedHandler {
					err := peer.write(map[string]any{
						"jsonrpc": "2.0", "method": "session.event",
						"params": map[string]any{
							"sessionId": session.SessionID,
							"event": map[string]any{
								"id": "00000000-0000-4000-8000-000000000001", "parentId": nil,
								"timestamp": "2026-01-01T00:00:00Z", "type": "session.idle",
								"ephemeral": true, "data": map[string]any{},
							},
						},
					})
					if err != nil {
						t.Fatal("notify:", err)
					}
					select {
					case <-entered:
					case <-operationCtx.Done():
						t.Fatal("event handler did not enter:", operationCtx.Err())
					}
				}
				stopped := make(chan struct{})
				go func() {
					client.ForceStop()
					close(stopped)
				}()
				select {
				case <-stopped:
				case <-operationCtx.Done():
					t.Fatal("force stop joined the admitted handler:", operationCtx.Err())
				}
				for range count {
					select {
					case err := <-results:
						if err == nil || !strings.Contains(err.Error(), "session closed before response completed") {
							t.Fatalf("expected response retirement error, got %v", err)
						}
					case <-time.After(5 * time.Second):
						t.Fatal("acknowledged response wait remained blocked after force stop")
					}
				}
			})
		}
	}
}
