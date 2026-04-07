package e2e

import (
	"fmt"
	"sort"
	"strings"
	"sync"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

// inMemoryFS is a simple in memory filesystem for testing.
type inMemoryFS struct {
	mu    sync.Mutex
	files map[string]string
	dirs  map[string]bool
	mtime map[string]time.Time
}

func newInMemoryFS() *inMemoryFS {
	return &inMemoryFS{
		files: make(map[string]string),
		dirs:  map[string]bool{"/": true},
		mtime: make(map[string]time.Time),
	}
}

func (fs *inMemoryFS) ensureParents(p string) {
	parts := strings.Split(p, "/")
	for i := 1; i < len(parts)-1; i++ {
		dir := strings.Join(parts[:i+1], "/")
		fs.dirs[dir] = true
	}
}

// sessionFsHandler adapts the in memory FS for a specific session.
type sessionFsHandler struct {
	sessionID string
	fs        *inMemoryFS
}

func (h *sessionFsHandler) sp(p string) string {
	if strings.HasPrefix(p, "/") {
		return "/" + h.sessionID + p
	}
	return "/" + h.sessionID + "/" + p
}

func (h *sessionFsHandler) ReadFile(params copilot.SessionFsReadFileParams) (*copilot.SessionFsReadFileResult, error) {
	h.fs.mu.Lock()
	defer h.fs.mu.Unlock()
	content, ok := h.fs.files[h.sp(params.Path)]
	if !ok {
		return nil, fmt.Errorf("file not found: %s", params.Path)
	}
	return &copilot.SessionFsReadFileResult{Content: content}, nil
}

func (h *sessionFsHandler) WriteFile(params copilot.SessionFsWriteFileParams) error {
	h.fs.mu.Lock()
	defer h.fs.mu.Unlock()
	p := h.sp(params.Path)
	h.fs.ensureParents(p)
	h.fs.files[p] = params.Content
	h.fs.mtime[p] = time.Now()
	return nil
}

func (h *sessionFsHandler) AppendFile(params copilot.SessionFsAppendFileParams) error {
	h.fs.mu.Lock()
	defer h.fs.mu.Unlock()
	p := h.sp(params.Path)
	h.fs.ensureParents(p)
	h.fs.files[p] += params.Content
	h.fs.mtime[p] = time.Now()
	return nil
}

func (h *sessionFsHandler) Exists(params copilot.SessionFsExistsParams) (*copilot.SessionFsExistsResult, error) {
	h.fs.mu.Lock()
	defer h.fs.mu.Unlock()
	p := h.sp(params.Path)
	_, fileOk := h.fs.files[p]
	_, dirOk := h.fs.dirs[p]
	return &copilot.SessionFsExistsResult{Exists: fileOk || dirOk}, nil
}

func (h *sessionFsHandler) Stat(params copilot.SessionFsStatParams) (*copilot.SessionFsStatResult, error) {
	h.fs.mu.Lock()
	defer h.fs.mu.Unlock()
	p := h.sp(params.Path)
	if content, ok := h.fs.files[p]; ok {
		mt := h.fs.mtime[p]
		return &copilot.SessionFsStatResult{
			IsFile:      true,
			IsDirectory: false,
			Size:        int64(len(content)),
			Mtime:       mt.Format(time.RFC3339Nano),
			Birthtime:   mt.Format(time.RFC3339Nano),
		}, nil
	}
	if h.fs.dirs[p] {
		return &copilot.SessionFsStatResult{
			IsFile:      false,
			IsDirectory: true,
			Mtime:       time.Now().Format(time.RFC3339Nano),
			Birthtime:   time.Now().Format(time.RFC3339Nano),
		}, nil
	}
	return nil, fmt.Errorf("path not found: %s", params.Path)
}

func (h *sessionFsHandler) Mkdir(params copilot.SessionFsMkdirParams) error {
	h.fs.mu.Lock()
	defer h.fs.mu.Unlock()
	p := h.sp(params.Path)
	if params.Recursive != nil && *params.Recursive {
		h.fs.ensureParents(p + "/x")
	}
	h.fs.dirs[p] = true
	return nil
}

func (h *sessionFsHandler) Readdir(params copilot.SessionFsReaddirParams) (*copilot.SessionFsReaddirResult, error) {
	h.fs.mu.Lock()
	defer h.fs.mu.Unlock()
	p := h.sp(params.Path)
	if !strings.HasSuffix(p, "/") {
		p += "/"
	}
	entries := map[string]bool{}
	for k := range h.fs.files {
		if strings.HasPrefix(k, p) && len(k) > len(p) {
			rest := k[len(p):]
			if idx := strings.Index(rest, "/"); idx >= 0 {
				entries[rest[:idx]] = true
			} else {
				entries[rest] = true
			}
		}
	}
	for k := range h.fs.dirs {
		if strings.HasPrefix(k, p) && len(k) > len(p) {
			rest := k[len(p):]
			if idx := strings.Index(rest, "/"); idx >= 0 {
				entries[rest[:idx]] = true
			} else {
				entries[rest] = true
			}
		}
	}
	result := make([]string, 0, len(entries))
	for e := range entries {
		result = append(result, e)
	}
	sort.Strings(result)
	return &copilot.SessionFsReaddirResult{Entries: result}, nil
}

func (h *sessionFsHandler) ReaddirWithTypes(params copilot.SessionFsReaddirWithTypesParams) (*copilot.SessionFsReaddirWithTypesResult, error) {
	dirResult, err := h.Readdir(copilot.SessionFsReaddirParams{SessionID: params.SessionID, Path: params.Path})
	if err != nil {
		return nil, err
	}
	p := h.sp(params.Path)
	if !strings.HasSuffix(p, "/") {
		p += "/"
	}
	h.fs.mu.Lock()
	defer h.fs.mu.Unlock()
	var entries []copilot.SessionFsDirEntry
	for _, name := range dirResult.Entries {
		full := p + name
		entryType := "file"
		if h.fs.dirs[full] {
			entryType = "directory"
		} else {
			// Check if any file has this as prefix (implicit directory)
			for k := range h.fs.files {
				if strings.HasPrefix(k, full+"/") {
					entryType = "directory"
					break
				}
			}
		}
		entries = append(entries, copilot.SessionFsDirEntry{Name: name, Type: entryType})
	}
	return &copilot.SessionFsReaddirWithTypesResult{Entries: entries}, nil
}

func (h *sessionFsHandler) Rm(params copilot.SessionFsRmParams) error {
	h.fs.mu.Lock()
	defer h.fs.mu.Unlock()
	p := h.sp(params.Path)
	delete(h.fs.files, p)
	delete(h.fs.dirs, p)
	delete(h.fs.mtime, p)
	return nil
}

func (h *sessionFsHandler) Rename(params copilot.SessionFsRenameParams) error {
	h.fs.mu.Lock()
	defer h.fs.mu.Unlock()
	src := h.sp(params.Src)
	dest := h.sp(params.Dest)
	if content, ok := h.fs.files[src]; ok {
		h.fs.ensureParents(dest)
		h.fs.files[dest] = content
		h.fs.mtime[dest] = h.fs.mtime[src]
		delete(h.fs.files, src)
		delete(h.fs.mtime, src)
	}
	return nil
}

func TestSessionFs(t *testing.T) {
	ctx := testharness.NewTestContext(t)

	// Shared in memory filesystem across tests
	memFS := newInMemoryFS()

	client := ctx.NewClient(func(opts *copilot.ClientOptions) {
		opts.SessionFs = &copilot.SessionFsConfig{
			InitialCwd:       "/",
			SessionStatePath: "/session-state",
			Conventions:      "posix",
		}
	})
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should route file operations through the session fs provider", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			CreateSessionFsHandler: func(s *copilot.Session) copilot.SessionFsHandler {
				return &sessionFsHandler{sessionID: s.SessionID, fs: memFS}
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		msg, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "What is 100 + 200?"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}
		if msg == nil || msg.Data.Content == nil || !strings.Contains(*msg.Data.Content, "300") {
			t.Fatalf("Expected response containing '300', got: %v", msg)
		}
		session.Disconnect()

		// Verify the events file was written through our provider
		eventsPath := "/" + session.SessionID + "/session-state/events.jsonl"
		memFS.mu.Lock()
		content, ok := memFS.files[eventsPath]
		memFS.mu.Unlock()
		if !ok {
			t.Fatal("Expected events.jsonl to exist in in memory filesystem")
		}
		if !strings.Contains(content, "300") {
			t.Errorf("Expected events.jsonl to contain '300', got: %s", content[:min(200, len(content))])
		}
	})

	t.Run("should load session data from fs provider on resume", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session1, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			CreateSessionFsHandler: func(s *copilot.Session) copilot.SessionFsHandler {
				return &sessionFsHandler{sessionID: s.SessionID, fs: memFS}
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		sessionID := session1.SessionID

		msg, err := session1.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "What is 50 + 50?"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}
		if msg == nil || msg.Data.Content == nil || !strings.Contains(*msg.Data.Content, "100") {
			t.Fatalf("Expected response containing '100', got: %v", msg)
		}
		session1.Disconnect()

		// Verify events file exists
		eventsPath := "/" + sessionID + "/session-state/events.jsonl"
		memFS.mu.Lock()
		_, exists := memFS.files[eventsPath]
		memFS.mu.Unlock()
		if !exists {
			t.Fatal("Expected events.jsonl to exist before resume")
		}

		// Resume the session
		session2, err := client.ResumeSession(t.Context(), sessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			CreateSessionFsHandler: func(s *copilot.Session) copilot.SessionFsHandler {
				return &sessionFsHandler{sessionID: s.SessionID, fs: memFS}
			},
		})
		if err != nil {
			t.Fatalf("Failed to resume session: %v", err)
		}

		msg2, err := session2.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "What is that times 3?"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}
		session2.Disconnect()
		if msg2 == nil || msg2.Data.Content == nil || !strings.Contains(*msg2.Data.Content, "300") {
			t.Fatalf("Expected response containing '300', got: %v", msg2)
		}
	})
}
