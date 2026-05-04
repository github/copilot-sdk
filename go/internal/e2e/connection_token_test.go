package e2e

import (
	"fmt"
	"strings"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

func TestConnectionToken(t *testing.T) {
	t.Run("explicit token round-trips successfully", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		client := ctx.NewClient(func(opts *copilot.ClientOptions) {
			opts.UseStdio = copilot.Bool(false)
			opts.TCPConnectionToken = "right-token"
		})
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Start failed: %v", err)
		}

		resp, err := client.Ping(t.Context(), "hi")
		if err != nil {
			t.Fatalf("Ping failed: %v", err)
		}
		if resp.Message != "pong: hi" {
			t.Errorf("expected message 'pong: hi', got %q", resp.Message)
		}
	})

	t.Run("auto-generated token round-trips successfully", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		client := ctx.NewClient(func(opts *copilot.ClientOptions) {
			opts.UseStdio = copilot.Bool(false)
		})
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Start failed: %v", err)
		}

		resp, err := client.Ping(t.Context(), "hi")
		if err != nil {
			t.Fatalf("Ping failed: %v", err)
		}
		if resp.Message != "pong: hi" {
			t.Errorf("expected message 'pong: hi', got %q", resp.Message)
		}
	})

	t.Run("sibling client with wrong token is rejected", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		good := ctx.NewClient(func(opts *copilot.ClientOptions) {
			opts.UseStdio = copilot.Bool(false)
			opts.TCPConnectionToken = "right-token"
		})
		t.Cleanup(func() { good.ForceStop() })

		if err := good.Start(t.Context()); err != nil {
			t.Fatalf("good client Start failed: %v", err)
		}
		port := good.ActualPort()
		if port == 0 {
			t.Fatalf("expected non-zero port from TCP mode client")
		}

		bad := copilot.NewClient(&copilot.ClientOptions{
			CLIUrl:             fmt.Sprintf("localhost:%d", port),
			TCPConnectionToken: "wrong",
		})
		t.Cleanup(func() { bad.ForceStop() })

		err := bad.Start(t.Context())
		if err == nil {
			t.Fatalf("expected sibling client with wrong token to fail")
		}
		if !strings.Contains(err.Error(), "AUTHENTICATION_FAILED") {
			t.Errorf("expected AUTHENTICATION_FAILED error, got: %v", err)
		}
	})

	t.Run("sibling client with no token is rejected", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		good := ctx.NewClient(func(opts *copilot.ClientOptions) {
			opts.UseStdio = copilot.Bool(false)
			opts.TCPConnectionToken = "right-token"
		})
		t.Cleanup(func() { good.ForceStop() })

		if err := good.Start(t.Context()); err != nil {
			t.Fatalf("good client Start failed: %v", err)
		}
		port := good.ActualPort()
		if port == 0 {
			t.Fatalf("expected non-zero port from TCP mode client")
		}

		none := copilot.NewClient(&copilot.ClientOptions{
			CLIUrl: fmt.Sprintf("localhost:%d", port),
		})
		t.Cleanup(func() { none.ForceStop() })

		err := none.Start(t.Context())
		if err == nil {
			t.Fatalf("expected sibling client with no token to fail")
		}
		if !strings.Contains(err.Error(), "AUTHENTICATION_FAILED") {
			t.Errorf("expected AUTHENTICATION_FAILED error, got: %v", err)
		}
	})
}
