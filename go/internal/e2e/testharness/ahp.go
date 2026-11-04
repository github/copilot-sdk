package testharness

import (
	"bufio"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"os"
	"os/exec"
	"sync"
	"testing"
	"time"
)

// AhpTestClient runs the standard TypeScript AHP client independently of CAPI replay.
type AhpTestClient struct {
	command *exec.Cmd
	input   io.WriteCloser
	output  *bufio.Reader
	mu      sync.Mutex
	done    chan struct{}
	waitErr error
}

func NewAhpTestClient(t *testing.T) *AhpTestClient {
	t.Helper()
	command := exec.Command("node", "--import", "tsx",
		RepoPath("nodejs", "test", "e2e", "harness", "ahpTestDriver.ts"))
	command.Dir = RepoPath("nodejs")
	command.Stderr = os.Stderr
	input, err := command.StdinPipe()
	if err != nil {
		t.Fatal(err)
	}
	output, err := command.StdoutPipe()
	if err != nil {
		t.Fatal(err)
	}
	if err := command.Start(); err != nil {
		t.Fatal(err)
	}
	client := &AhpTestClient{command: command, input: input, output: bufio.NewReader(output), done: make(chan struct{})}
	go func() { client.waitErr = command.Wait(); close(client.done) }()
	t.Cleanup(func() {
		if err := client.input.Close(); err != nil && !errors.Is(err, os.ErrClosed) {
			t.Errorf("close AHP test input: %v", err)
		}
		select {
		case <-client.done:
		case <-time.After(10 * time.Second):
			if err := command.Process.Kill(); err != nil {
				t.Errorf("kill stalled AHP test client: %v", err)
			}
			<-client.done
			t.Error("AHP test client did not exit after input closed")
		}
		if client.waitErr != nil {
			t.Errorf("AHP test client exited: %v", client.waitErr)
		}
	})
	ctx, cancel := context.WithTimeout(t.Context(), 10*time.Second)
	defer cancel()
	data, err := client.read(ctx)
	if err != nil {
		t.Fatal(err)
	}
	var ready struct{ Ready bool }
	if err := json.Unmarshal(data, &ready); err != nil || !ready.Ready {
		t.Fatalf("AHP test client did not become ready: %s (%v)", data, err)
	}
	return client
}

func (c *AhpTestClient) read(ctx context.Context) ([]byte, error) {
	type response struct {
		data []byte
		err  error
	}
	done := make(chan response, 1)
	go func() { data, err := c.output.ReadBytes('\n'); done <- response{data, err} }()
	select {
	case result := <-done:
		return result.data, result.err
	case <-ctx.Done():
		if err := c.command.Process.Kill(); err != nil && !errors.Is(err, os.ErrProcessDone) {
			return nil, fmt.Errorf("stop timed-out AHP test client: %w", err)
		}
		return nil, ctx.Err()
	}
}

func (c *AhpTestClient) Request(ctx context.Context, command map[string]any) (json.RawMessage, error) {
	c.mu.Lock()
	defer c.mu.Unlock()
	if err := json.NewEncoder(c.input).Encode(command); err != nil {
		return nil, err
	}
	data, err := c.read(ctx)
	if err != nil {
		return nil, err
	}
	var response struct {
		Result json.RawMessage `json:"result"`
		Error  string          `json:"error"`
	}
	if err := json.Unmarshal(data, &response); err != nil {
		return nil, err
	}
	if response.Error != "" {
		return nil, fmt.Errorf("AHP test client: %s", response.Error)
	}
	return response.Result, nil
}
