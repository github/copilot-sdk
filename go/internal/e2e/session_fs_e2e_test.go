package e2e

import (
	"fmt"
	"os"
	"path/filepath"
	"regexp"
	"runtime"
	"strings"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

func TestSessionFsE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	providerRoot := t.TempDir()
	sessionStatePath := createSessionStatePath(t)
	sessionFsConfig := &copilot.SessionFsConfig{
		InitialCwd:       "/",
		SessionStatePath: sessionStatePath,
		Conventions:      rpc.SessionFsSetProviderConventionsPosix,
	}
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

		events, err := os.ReadFile(p(session.SessionID, sessionStatePath+"/events.jsonl"))
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

		if _, err := os.Stat(p(sessionID, sessionStatePath+"/events.jsonl")); err != nil {
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
			t.Fatal("Expected Start to fail when SessionFs provider is set after sessions already exist")
		}
	})

	t.Run("should map large output handling into SessionFs", func(t *testing.T) {
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
		if !strings.Contains(toolResult, sessionStatePath+"/temp/") {
			t.Fatalf("Expected tool result to reference %s/temp/, got %q", sessionStatePath, toolResult)
		}
		match := regexp.MustCompile(`(` + regexp.QuoteMeta(sessionStatePath) + `/temp/[^\s]+)`).FindStringSubmatch(toolResult)
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

	t.Run("should succeed with compaction while using SessionFs", func(t *testing.T) {
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

		eventsPath := p(session.SessionID, sessionStatePath+"/events.jsonl")
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
	t.Run("should write workspace metadata via SessionFs", func(t *testing.T) {
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

		// WorkspaceManager should have created workspace.yaml via SessionFs
		workspaceYamlPath := p(session.SessionID, sessionStatePath+"/workspace.yaml")
		if err := waitForFileContent(workspaceYamlPath, "id:", 5*time.Second); err != nil {
			t.Fatalf("Timed out waiting for workspace.yaml content: %v", err)
		}

		// Checkpoint index should also exist
		indexPath := p(session.SessionID, sessionStatePath+"/checkpoints/index.md")
		if err := waitForFile(indexPath, 5*time.Second); err != nil {
			t.Fatalf("Timed out waiting for checkpoints/index.md: %v", err)
		}

		if err := session.Disconnect(); err != nil {
			t.Fatalf("Failed to disconnect session: %v", err)
		}
	})

	t.Run("should persist plan.md via SessionFs", func(t *testing.T) {
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

		planPath := p(session.SessionID, sessionStatePath+"/plan.md")
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

func createSessionStatePath(t *testing.T) string {
	t.Helper()
	if runtime.GOOS == "windows" {
		return "/session-state"
	}
	return filepath.ToSlash(filepath.Join(t.TempDir(), "session-state"))
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

func (h *testSessionFsHandler) ReaddirWithTypes(path string) ([]rpc.SessionFsReaddirWithTypesEntry, error) {
	entries, err := os.ReadDir(providerPath(h.root, h.sessionID, path))
	if err != nil {
		return nil, err
	}
	result := make([]rpc.SessionFsReaddirWithTypesEntry, 0, len(entries))
	for _, entry := range entries {
		entryType := rpc.SessionFsReaddirWithTypesEntryTypeFile
		if entry.IsDir() {
			entryType = rpc.SessionFsReaddirWithTypesEntryTypeDirectory
		}
		result = append(result, rpc.SessionFsReaddirWithTypesEntry{
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

// TestSessionFsHandlerOperations mirrors the C# Should_Map_All_SessionFs_Handler_Operations test.
// It exercises every operation on testSessionFsHandler directly to ensure the test helper
// implementation routes file operations correctly to the per-session provider root.
func TestSessionFsHandlerOperationsE2E(t *testing.T) {
	providerRoot := t.TempDir()
	sessionID := "handler-session"
	handler := &testSessionFsHandler{root: providerRoot, sessionID: sessionID}

	if err := handler.Mkdir("/workspace/nested", true, nil); err != nil {
		t.Fatalf("Mkdir failed: %v", err)
	}

	if err := handler.WriteFile("/workspace/nested/file.txt", "hello", nil); err != nil {
		t.Fatalf("WriteFile failed: %v", err)
	}

	if err := handler.AppendFile("/workspace/nested/file.txt", " world", nil); err != nil {
		t.Fatalf("AppendFile failed: %v", err)
	}

	exists, err := handler.Exists("/workspace/nested/file.txt")
	if err != nil {
		t.Fatalf("Exists failed: %v", err)
	}
	if !exists {
		t.Error("Expected file to exist after WriteFile+AppendFile")
	}

	stat, err := handler.Stat("/workspace/nested/file.txt")
	if err != nil {
		t.Fatalf("Stat failed: %v", err)
	}
	if !stat.IsFile {
		t.Error("Expected IsFile=true")
	}
	if stat.IsDirectory {
		t.Error("Expected IsDirectory=false")
	}
	if stat.Size != int64(len("hello world")) {
		t.Errorf("Expected Size=%d, got %d", len("hello world"), stat.Size)
	}

	content, err := handler.ReadFile("/workspace/nested/file.txt")
	if err != nil {
		t.Fatalf("ReadFile failed: %v", err)
	}
	if content != "hello world" {
		t.Errorf("Expected content 'hello world', got %q", content)
	}

	entries, err := handler.Readdir("/workspace/nested")
	if err != nil {
		t.Fatalf("Readdir failed: %v", err)
	}
	if !sliceContains(entries, "file.txt") {
		t.Errorf("Expected entries to contain 'file.txt', got %v", entries)
	}

	typedEntries, err := handler.ReaddirWithTypes("/workspace/nested")
	if err != nil {
		t.Fatalf("ReaddirWithTypes failed: %v", err)
	}
	var found bool
	for _, entry := range typedEntries {
		if entry.Name == "file.txt" && entry.Type == rpc.SessionFsReaddirWithTypesEntryTypeFile {
			found = true
			break
		}
	}
	if !found {
		t.Errorf("Expected typed entry {file.txt, file}, got %+v", typedEntries)
	}

	if err := handler.Rename("/workspace/nested/file.txt", "/workspace/nested/renamed.txt"); err != nil {
		t.Fatalf("Rename failed: %v", err)
	}
	oldExists, err := handler.Exists("/workspace/nested/file.txt")
	if err != nil {
		t.Fatalf("Exists (old path) failed: %v", err)
	}
	if oldExists {
		t.Error("Expected old path to no longer exist after Rename")
	}
	renamedContent, err := handler.ReadFile("/workspace/nested/renamed.txt")
	if err != nil {
		t.Fatalf("ReadFile (renamed) failed: %v", err)
	}
	if renamedContent != "hello world" {
		t.Errorf("Expected renamed content 'hello world', got %q", renamedContent)
	}

	if err := handler.Rm("/workspace/nested/renamed.txt", false, false); err != nil {
		t.Fatalf("Rm failed: %v", err)
	}
	removed, err := handler.Exists("/workspace/nested/renamed.txt")
	if err != nil {
		t.Fatalf("Exists (removed) failed: %v", err)
	}
	if removed {
		t.Error("Expected file to be gone after Rm")
	}

	// Force removing a missing path should succeed.
	if err := handler.Rm("/workspace/nested/missing.txt", false, true); err != nil {
		t.Errorf("Rm with force on missing path should not error, got %v", err)
	}

	// Stat on a missing file should return os.ErrNotExist.
	if _, err := handler.Stat("/workspace/nested/missing.txt"); err == nil || !os.IsNotExist(err) {
		t.Errorf("Expected os.ErrNotExist from Stat on missing file, got %v", err)
	}
}

func sliceContains(slice []string, value string) bool {
	for _, item := range slice {
		if item == value {
			return true
		}
	}
	return false
}
