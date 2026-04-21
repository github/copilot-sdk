package e2e

import (
	"fmt"
	"os"
	"path/filepath"
	"regexp"
	"strings"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

func TestSessionFs(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	providerRoot := t.TempDir()
	createSessionFsHandler := func(session *copilot.Session) copilot.SessionFsProvider {
		return &testSessionFsHandler{
			root:      providerRoot,
			sessionID: session.SessionID,
		}
	}
	p := func(sessionID string, path string) string {
		return providerPath(providerRoot, sessionID, path)
	}

	client := ctx.NewClient(func(opts *copilot.ClientOptions) {
		opts.SessionFs = sessionFsConfig
	})
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should route file operations through the session fs provider", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest:    copilot.PermissionHandler.ApproveAll,
			CreateSessionFsHandler: createSessionFsHandler,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		msg, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "What is 100 + 200?"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}
		content := ""
		if msg != nil {
			if d, ok := msg.Data.(*copilot.AssistantMessageData); ok {
				content = d.Content
			}
		}
		if !strings.Contains(content, "300") {
			t.Fatalf("Expected response to contain 300, got %q", content)
		}
		if err := session.Disconnect(); err != nil {
			t.Fatalf("Failed to disconnect session: %v", err)
		}

		events, err := os.ReadFile(p(session.SessionID, "/session-state/events.jsonl"))
		if err != nil {
			t.Fatalf("Failed to read events file: %v", err)
		}
		if !strings.Contains(string(events), "300") {
			t.Fatalf("Expected events file to contain 300")
		}
	})

	t.Run("should load session data from fs provider on resume", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session1, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest:    copilot.PermissionHandler.ApproveAll,
			CreateSessionFsHandler: createSessionFsHandler,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		sessionID := session1.SessionID

		msg, err := session1.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "What is 50 + 50?"})
		if err != nil {
			t.Fatalf("Failed to send first message: %v", err)
		}
		content := ""
		if msg != nil {
			if d, ok := msg.Data.(*copilot.AssistantMessageData); ok {
				content = d.Content
			}
		}
		if !strings.Contains(content, "100") {
			t.Fatalf("Expected response to contain 100, got %q", content)
		}
		if err := session1.Disconnect(); err != nil {
			t.Fatalf("Failed to disconnect first session: %v", err)
		}

		if _, err := os.Stat(p(sessionID, "/session-state/events.jsonl")); err != nil {
			t.Fatalf("Expected events file to exist before resume: %v", err)
		}

		session2, err := client.ResumeSession(t.Context(), sessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest:    copilot.PermissionHandler.ApproveAll,
			CreateSessionFsHandler: createSessionFsHandler,
		})
		if err != nil {
			t.Fatalf("Failed to resume session: %v", err)
		}

		msg2, err := session2.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "What is that times 3?"})
		if err != nil {
			t.Fatalf("Failed to send second message: %v", err)
		}
		content2 := ""
		if msg2 != nil {
			if d, ok := msg2.Data.(*copilot.AssistantMessageData); ok {
				content2 = d.Content
			}
		}
		if !strings.Contains(content2, "300") {
			t.Fatalf("Expected response to contain 300, got %q", content2)
		}
		if err := session2.Disconnect(); err != nil {
			t.Fatalf("Failed to disconnect resumed session: %v", err)
		}
	})

	t.Run("should reject setProvider when sessions already exist", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		client1 := ctx.NewClient(func(opts *copilot.ClientOptions) {
			opts.UseStdio = copilot.Bool(false)
		})
		t.Cleanup(func() { client1.ForceStop() })

		if _, err := client1.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		}); err != nil {
			t.Fatalf("Failed to create initial session: %v", err)
		}

		actualPort := client1.ActualPort()
		if actualPort == 0 {
			t.Fatalf("Expected non-zero port from TCP mode client")
		}

		client2 := copilot.NewClient(&copilot.ClientOptions{
			CLIUrl:    fmt.Sprintf("localhost:%d", actualPort),
			LogLevel:  "error",
			Env:       ctx.Env(),
			SessionFs: sessionFsConfig,
		})
		t.Cleanup(func() { client2.ForceStop() })

		if err := client2.Start(t.Context()); err == nil {
			t.Fatal("Expected Start to fail when sessionFs provider is set after sessions already exist")
		}
	})

	t.Run("should map large output handling into sessionFs", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		suppliedFileContent := strings.Repeat("x", 100_000)
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest:    copilot.PermissionHandler.ApproveAll,
			CreateSessionFsHandler: createSessionFsHandler,
			Tools: []copilot.Tool{
				copilot.DefineTool("get_big_string", "Returns a large string",
					func(_ struct{}, inv copilot.ToolInvocation) (string, error) {
						return suppliedFileContent, nil
					}),
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		if _, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Call the get_big_string tool and reply with the word DONE only.",
		}); err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		messages, err := session.GetMessages(t.Context())
		if err != nil {
			t.Fatalf("Failed to get messages: %v", err)
		}
		toolResult := findToolCallResult(messages, "get_big_string")
		if !strings.Contains(toolResult, "/session-state/temp/") {
			t.Fatalf("Expected tool result to reference /session-state/temp/, got %q", toolResult)
		}
		match := regexp.MustCompile(`(/session-state/temp/[^\s]+)`).FindStringSubmatch(toolResult)
		if len(match) < 2 {
			t.Fatalf("Expected temp file path in tool result, got %q", toolResult)
		}

		fileContent, err := os.ReadFile(p(session.SessionID, match[1]))
		if err != nil {
			t.Fatalf("Failed to read temp file: %v", err)
		}
		if string(fileContent) != suppliedFileContent {
			t.Fatalf("Expected temp file content to match supplied content")
		}
	})

	t.Run("should succeed with compaction while using sessionFs", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest:    copilot.PermissionHandler.ApproveAll,
			CreateSessionFsHandler: createSessionFsHandler,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		if _, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "What is 2+2?"}); err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		eventsPath := p(session.SessionID, "/session-state/events.jsonl")
		if err := waitForFile(eventsPath, 5*time.Second); err != nil {
			t.Fatalf("Timed out waiting for events file: %v", err)
		}
		contentBefore, err := os.ReadFile(eventsPath)
		if err != nil {
			t.Fatalf("Failed to read events file before compaction: %v", err)
		}
		if strings.Contains(string(contentBefore), "checkpointNumber") {
			t.Fatalf("Expected events file to not contain checkpointNumber before compaction")
		}

		compactionResult, err := session.RPC.History.Compact(t.Context())
		if err != nil {
			t.Fatalf("Failed to compact session: %v", err)
		}
		if compactionResult == nil || !compactionResult.Success {
			t.Fatalf("Expected compaction to succeed, got %+v", compactionResult)
		}

		if err := waitForFileContent(eventsPath, "checkpointNumber", 5*time.Second); err != nil {
			t.Fatalf("Timed out waiting for checkpoint rewrite: %v", err)
		}
	})
	t.Run("should write workspace metadata via sessionFs", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest:    copilot.PermissionHandler.ApproveAll,
			CreateSessionFsHandler: createSessionFsHandler,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		msg, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "What is 7 * 8?"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}
		content := ""
		if msg != nil {
			if d, ok := msg.Data.(*copilot.AssistantMessageData); ok {
				content = d.Content
			}
		}
		if !strings.Contains(content, "56") {
			t.Fatalf("Expected response to contain 56, got %q", content)
		}

		// WorkspaceManager should have created workspace.yaml via sessionFs
		workspaceYamlPath := p(session.SessionID, "/session-state/workspace.yaml")
		if err := waitForFile(workspaceYamlPath, 5*time.Second); err != nil {
			t.Fatalf("Timed out waiting for workspace.yaml: %v", err)
		}
		yaml, err := os.ReadFile(workspaceYamlPath)
		if err != nil {
			t.Fatalf("Failed to read workspace.yaml: %v", err)
		}
		if !strings.Contains(string(yaml), "id:") {
			t.Fatalf("Expected workspace.yaml to contain 'id:', got %q", string(yaml))
		}

		// Checkpoint index should also exist
		indexPath := p(session.SessionID, "/session-state/checkpoints/index.md")
		if err := waitForFile(indexPath, 5*time.Second); err != nil {
			t.Fatalf("Timed out waiting for checkpoints/index.md: %v", err)
		}

		if err := session.Disconnect(); err != nil {
			t.Fatalf("Failed to disconnect session: %v", err)
		}
	})

	t.Run("should persist plan.md via sessionFs", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest:    copilot.PermissionHandler.ApproveAll,
			CreateSessionFsHandler: createSessionFsHandler,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// Write a plan via the session RPC
		if _, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "What is 2 + 3?"}); err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}
		if _, err := session.RPC.Plan.Update(t.Context(), &rpc.PlanUpdateRequest{Content: "# Test Plan\n\nThis is a test."}); err != nil {
			t.Fatalf("Failed to update plan: %v", err)
		}

		planPath := p(session.SessionID, "/session-state/plan.md")
		if err := waitForFile(planPath, 5*time.Second); err != nil {
			t.Fatalf("Timed out waiting for plan.md: %v", err)
		}
		planContent, err := os.ReadFile(planPath)
		if err != nil {
			t.Fatalf("Failed to read plan.md: %v", err)
		}
		if !strings.Contains(string(planContent), "# Test Plan") {
			t.Fatalf("Expected plan.md to contain '# Test Plan', got %q", string(planContent))
		}

		if err := session.Disconnect(); err != nil {
			t.Fatalf("Failed to disconnect session: %v", err)
		}
	})
}

var sessionFsConfig = &copilot.SessionFsConfig{
	InitialCwd:       "/",
	SessionStatePath: "/session-state",
	Conventions:      rpc.SessionFSSetProviderConventionsPosix,
}

type testSessionFsHandler struct {
	root      string
	sessionID string
}

func (h *testSessionFsHandler) ReadFile(path string) (string, error) {
	content, err := os.ReadFile(providerPath(h.root, h.sessionID, path))
	if err != nil {
		return "", err
	}
	return string(content), nil
}

func (h *testSessionFsHandler) WriteFile(path string, content string, mode *int) error {
	fullPath := providerPath(h.root, h.sessionID, path)
	if err := os.MkdirAll(filepath.Dir(fullPath), 0o755); err != nil {
		return err
	}
	perm := os.FileMode(0o666)
	if mode != nil {
		perm = os.FileMode(*mode)
	}
	return os.WriteFile(fullPath, []byte(content), perm)
}

func (h *testSessionFsHandler) AppendFile(path string, content string, mode *int) error {
	fullPath := providerPath(h.root, h.sessionID, path)
	if err := os.MkdirAll(filepath.Dir(fullPath), 0o755); err != nil {
		return err
	}
	perm := os.FileMode(0o666)
	if mode != nil {
		perm = os.FileMode(*mode)
	}
	f, err := os.OpenFile(fullPath, os.O_CREATE|os.O_WRONLY|os.O_APPEND, perm)
	if err != nil {
		return err
	}
	defer f.Close()
	_, err = f.WriteString(content)
	return err
}

func (h *testSessionFsHandler) Exists(path string) (bool, error) {
	_, err := os.Stat(providerPath(h.root, h.sessionID, path))
	if err == nil {
		return true, nil
	}
	if os.IsNotExist(err) {
		return false, nil
	}
	return false, err
}

func (h *testSessionFsHandler) Stat(path string) (*copilot.SessionFsFileInfo, error) {
	info, err := os.Stat(providerPath(h.root, h.sessionID, path))
	if err != nil {
		return nil, err
	}
	ts := info.ModTime().UTC()
	return &copilot.SessionFsFileInfo{
		IsFile:      !info.IsDir(),
		IsDirectory: info.IsDir(),
		Size:        info.Size(),
		Mtime:       ts,
		Birthtime:   ts,
	}, nil
}

func (h *testSessionFsHandler) Mkdir(path string, recursive bool, mode *int) error {
	fullPath := providerPath(h.root, h.sessionID, path)
	perm := os.FileMode(0o777)
	if mode != nil {
		perm = os.FileMode(*mode)
	}
	if recursive {
		return os.MkdirAll(fullPath, perm)
	}
	return os.Mkdir(fullPath, perm)
}

func (h *testSessionFsHandler) Readdir(path string) ([]string, error) {
	entries, err := os.ReadDir(providerPath(h.root, h.sessionID, path))
	if err != nil {
		return nil, err
	}
	names := make([]string, 0, len(entries))
	for _, entry := range entries {
		names = append(names, entry.Name())
	}
	return names, nil
}

func (h *testSessionFsHandler) ReaddirWithTypes(path string) ([]rpc.SessionFSReaddirWithTypesEntry, error) {
	entries, err := os.ReadDir(providerPath(h.root, h.sessionID, path))
	if err != nil {
		return nil, err
	}
	result := make([]rpc.SessionFSReaddirWithTypesEntry, 0, len(entries))
	for _, entry := range entries {
		entryType := rpc.SessionFSReaddirWithTypesEntryTypeFile
		if entry.IsDir() {
			entryType = rpc.SessionFSReaddirWithTypesEntryTypeDirectory
		}
		result = append(result, rpc.SessionFSReaddirWithTypesEntry{
			Name: entry.Name(),
			Type: entryType,
		})
	}
	return result, nil
}

func (h *testSessionFsHandler) Rm(path string, recursive bool, force bool) error {
	fullPath := providerPath(h.root, h.sessionID, path)
	var err error
	if recursive {
		err = os.RemoveAll(fullPath)
	} else {
		err = os.Remove(fullPath)
	}
	if err != nil && force && os.IsNotExist(err) {
		return nil
	}
	return err
}

func (h *testSessionFsHandler) Rename(src string, dest string) error {
	destPath := providerPath(h.root, h.sessionID, dest)
	if err := os.MkdirAll(filepath.Dir(destPath), 0o755); err != nil {
		return err
	}
	return os.Rename(providerPath(h.root, h.sessionID, src), destPath)
}

func providerPath(root string, sessionID string, path string) string {
	trimmed := strings.TrimPrefix(path, "/")
	if trimmed == "" {
		return filepath.Join(root, sessionID)
	}
	return filepath.Join(root, sessionID, filepath.FromSlash(trimmed))
}

func findToolCallResult(messages []copilot.SessionEvent, toolName string) string {
	for _, message := range messages {
		if d, ok := message.Data.(*copilot.ToolExecutionCompleteData); ok &&
			d.Result != nil &&
			findToolName(messages, d.ToolCallID) == toolName {
			return d.Result.Content
		}
	}
	return ""
}

func findToolName(messages []copilot.SessionEvent, toolCallID string) string {
	for _, message := range messages {
		if d, ok := message.Data.(*copilot.ToolExecutionStartData); ok &&
			d.ToolCallID == toolCallID {
			return d.ToolName
		}
	}
	return ""
}

func waitForFile(path string, timeout time.Duration) error {
	deadline := time.Now().Add(timeout)
	for time.Now().Before(deadline) {
		if _, err := os.Stat(path); err == nil {
			return nil
		}
		time.Sleep(50 * time.Millisecond)
	}
	return fmt.Errorf("file did not appear: %s", path)
}

func waitForFileContent(path string, needle string, timeout time.Duration) error {
	deadline := time.Now().Add(timeout)
	for time.Now().Before(deadline) {
		content, err := os.ReadFile(path)
		if err == nil && strings.Contains(string(content), needle) {
			return nil
		}
		time.Sleep(50 * time.Millisecond)
	}
	return fmt.Errorf("file %s did not contain %q", path, needle)
}
