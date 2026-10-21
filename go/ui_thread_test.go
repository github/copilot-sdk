package copilot

import (
	"context"
	"sync"
	"testing"
	"time"

	"github.com/github/copilot-sdk/go/rpc"
)

type uiThreadTestHost struct {
	inProcessHost
	actions chan uint32
}

func (h *uiThreadTestHost) RunUiAction(actionID uint32) error {
	h.actions <- actionID
	return nil
}

func TestUIThreadRPCRejectsOutOfRangeActionIDs(t *testing.T) {
	for _, tc := range []struct {
		name     string
		actionID int64
	}{
		{"negative", -1},
		{"overflow", 1 << 32},
	} {
		t.Run(tc.name, func(t *testing.T) {
			rpcClient, server, _ := newRuntimeShutdownRpcPair(t)
			t.Cleanup(rpcClient.Stop)
			t.Cleanup(server.Stop)
			ctx, cancel := context.WithTimeout(t.Context(), 5*time.Second)
			defer cancel()
			actions := make(chan uint32, 1)
			client := &Client{
				client:   rpcClient,
				RPC:      rpc.NewServerRPC(rpcClient),
				sessions: make(map[string]*Session),
				ffiHost:  &uiThreadTestHost{actions: actions},
				runOnUiThread: func(action func() error) error {
					return action()
				},
			}
			client.setupNotificationHandler()

			_, err := server.Request(ctx, "uiThread.run", map[string]int64{"actionId": tc.actionID})
			if err == nil || ctx.Err() != nil {
				t.Fatalf("want an RPC rejection before the deadline, got %v (context: %v)", err, ctx.Err())
			}
			select {
			case id := <-actions:
				t.Fatalf("native action %d executed for invalid wire ID %d", id, tc.actionID)
			default:
			}
		})
	}
}

func TestUIThreadRPCRemainsBoundToItsHostAndWaitsForScheduler(t *testing.T) {
	rpcClient, server, _ := newRuntimeShutdownRpcPair(t)
	t.Cleanup(rpcClient.Stop)
	t.Cleanup(server.Stop)
	ctx, cancel := context.WithTimeout(t.Context(), 5*time.Second)
	defer cancel()
	actions := make(chan uint32, 1)
	release := make(chan struct{})
	unblock := sync.OnceFunc(func() { close(release) })
	t.Cleanup(unblock)
	client := &Client{
		client:   rpcClient,
		RPC:      rpc.NewServerRPC(rpcClient),
		sessions: make(map[string]*Session),
		ffiHost:  &uiThreadTestHost{actions: actions},
		runOnUiThread: func(action func() error) error {
			err := action()
			<-release
			return err
		},
	}
	client.setupNotificationHandler()
	client.ffiHost = nil

	completed := make(chan error, 1)
	go func() {
		_, err := server.Request(ctx, "uiThread.run", &rpc.UIThreadRunRequest{ActionID: 42})
		completed <- err
	}()
	select {
	case id := <-actions:
		if id != 42 {
			t.Fatalf("native action ID = %d, want 42", id)
		}
	case err := <-completed:
		t.Fatalf("UI request completed without executing its connection's action: %v", err)
	case <-ctx.Done():
		t.Fatal(ctx.Err())
	}
	select {
	case err := <-completed:
		t.Fatalf("UI request completed before the scheduler: %v", err)
	default:
	}
	unblock()
	select {
	case err := <-completed:
		if err != nil {
			t.Fatalf("UI request failed: %v", err)
		}
	case <-ctx.Done():
		t.Fatal(ctx.Err())
	}
}
