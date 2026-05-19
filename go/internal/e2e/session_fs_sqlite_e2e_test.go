package e2e

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"sort"
	"strings"
	"sync"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"

	_ "modernc.org/sqlite"
)

type sqliteCall struct {
	SessionID string
	QueryType string
	Query     string
}

// inMemorySqliteProvider is a SessionFsProvider backed by in-memory maps and a real SQLite DB.
type inMemorySqliteProvider struct {
	mu          sync.Mutex
	sessionID   string
	files       map[string]string
	dirs        map[string]bool
	db          *sql.DB
	sqliteCalls *[]sqliteCall
}

func newInMemorySqliteProvider(sessionID string, calls *[]sqliteCall) *inMemorySqliteProvider {
	return &inMemorySqliteProvider{
		sessionID:   sessionID,
		files:       make(map[string]string),
		dirs:        map[string]bool{"/": true},
		sqliteCalls: calls,
	}
}

// getOrCreateDBLocked must be called while holding p.mu.
func (p *inMemorySqliteProvider) getOrCreateDBLocked() (*sql.DB, error) {
	if p.db == nil {
		db, err := sql.Open("sqlite", ":memory:")
		if err != nil {
			return nil, err
		}
		_, err = db.Exec("PRAGMA busy_timeout = 5000")
		if err != nil {
			db.Close()
			return nil, err
		}
		p.db = db
	}
	return p.db, nil
}

func (p *inMemorySqliteProvider) ensureParent(path string) {
	parts := strings.Split(strings.TrimRight(path, "/"), "/")
	for i := 1; i < len(parts); i++ {
		p.dirs[strings.Join(parts[:i], "/")] = true
	}
}

func (p *inMemorySqliteProvider) ReadFile(path string) (string, error) {
	p.mu.Lock()
	defer p.mu.Unlock()
	content, ok := p.files[path]
	if !ok {
		return "", fmt.Errorf("file not found: %s", path)
	}
	return content, nil
}

func (p *inMemorySqliteProvider) WriteFile(path string, content string, mode *int) error {
	p.mu.Lock()
	defer p.mu.Unlock()
	p.ensureParent(path)
	p.files[path] = content
	return nil
}

func (p *inMemorySqliteProvider) AppendFile(path string, content string, mode *int) error {
	p.mu.Lock()
	defer p.mu.Unlock()
	p.ensureParent(path)
	p.files[path] = p.files[path] + content
	return nil
}

func (p *inMemorySqliteProvider) Exists(path string) (bool, error) {
	p.mu.Lock()
	defer p.mu.Unlock()
	_, isFile := p.files[path]
	_, isDir := p.dirs[path]
	return isFile || isDir, nil
}

func (p *inMemorySqliteProvider) Stat(path string) (*copilot.SessionFsFileInfo, error) {
	p.mu.Lock()
	defer p.mu.Unlock()
	now := time.Now().UTC()
	if p.dirs[path] {
		return &copilot.SessionFsFileInfo{
			IsFile: false, IsDirectory: true, Size: 0, Mtime: now, Birthtime: now,
		}, nil
	}
	if content, ok := p.files[path]; ok {
		return &copilot.SessionFsFileInfo{
			IsFile: true, IsDirectory: false, Size: int64(len(content)), Mtime: now, Birthtime: now,
		}, nil
	}
	return nil, fmt.Errorf("not found: %s", path)
}

func (p *inMemorySqliteProvider) Mkdir(path string, recursive bool, mode *int) error {
	p.mu.Lock()
	defer p.mu.Unlock()
	if recursive {
		parts := strings.Split(strings.TrimRight(path, "/"), "/")
		for i := 1; i <= len(parts); i++ {
			p.dirs[strings.Join(parts[:i], "/")] = true
		}
	} else {
		p.dirs[path] = true
	}
	return nil
}

func (p *inMemorySqliteProvider) Readdir(path string) ([]string, error) {
	p.mu.Lock()
	defer p.mu.Unlock()
	prefix := strings.TrimRight(path, "/") + "/"
	names := map[string]bool{}
	for f := range p.files {
		if strings.HasPrefix(f, prefix) {
			rest := f[len(prefix):]
			if rest != "" {
				names[strings.SplitN(rest, "/", 2)[0]] = true
			}
		}
	}
	for d := range p.dirs {
		if strings.HasPrefix(d, prefix) {
			rest := d[len(prefix):]
			if rest != "" {
				names[strings.SplitN(rest, "/", 2)[0]] = true
			}
		}
	}
	result := make([]string, 0, len(names))
	for n := range names {
		result = append(result, n)
	}
	sort.Strings(result)
	return result, nil
}

func (p *inMemorySqliteProvider) ReaddirWithTypes(path string) ([]rpc.SessionFsReaddirWithTypesEntry, error) {
	p.mu.Lock()
	defer p.mu.Unlock()
	prefix := strings.TrimRight(path, "/") + "/"
	entries := map[string]rpc.SessionFsReaddirWithTypesEntryType{}
	for d := range p.dirs {
		if strings.HasPrefix(d, prefix) {
			rest := d[len(prefix):]
			if rest != "" {
				name := strings.SplitN(rest, "/", 2)[0]
				entries[name] = rpc.SessionFsReaddirWithTypesEntryTypeDirectory
			}
		}
	}
	for f := range p.files {
		if strings.HasPrefix(f, prefix) {
			rest := f[len(prefix):]
			if rest != "" {
				name := strings.SplitN(rest, "/", 2)[0]
				if _, exists := entries[name]; !exists {
					entries[name] = rpc.SessionFsReaddirWithTypesEntryTypeFile
				}
			}
		}
	}
	result := make([]rpc.SessionFsReaddirWithTypesEntry, 0, len(entries))
	for name, typ := range entries {
		result = append(result, rpc.SessionFsReaddirWithTypesEntry{Name: name, Type: typ})
	}
	sort.Slice(result, func(i, j int) bool { return result[i].Name < result[j].Name })
	return result, nil
}

func (p *inMemorySqliteProvider) Rm(path string, recursive bool, force bool) error {
	p.mu.Lock()
	defer p.mu.Unlock()
	delete(p.files, path)
	delete(p.dirs, path)
	return nil
}

func (p *inMemorySqliteProvider) Rename(src string, dest string) error {
	p.mu.Lock()
	defer p.mu.Unlock()
	if content, ok := p.files[src]; ok {
		p.ensureParent(dest)
		p.files[dest] = content
		delete(p.files, src)
	}
	return nil
}

func (p *inMemorySqliteProvider) SqliteQuery(sessionID string, query string, queryType rpc.SessionFsSqliteQueryType, params map[string]any) (*rpc.SessionFsSqliteQueryResult, error) {
	p.mu.Lock()
	defer p.mu.Unlock()
	*p.sqliteCalls = append(*p.sqliteCalls, sqliteCall{
		SessionID: sessionID,
		QueryType: string(queryType),
		Query:     query,
	})
	db, err := p.getOrCreateDBLocked()
	if err != nil {
		return nil, err
	}

	trimmed := strings.TrimSpace(query)
	if trimmed == "" {
		return &rpc.SessionFsSqliteQueryResult{Columns: []string{}, Rows: []map[string]any{}}, nil
	}

	switch queryType {
	case rpc.SessionFsSqliteQueryTypeExec:
		_, err := db.Exec(trimmed)
		if err != nil {
			return nil, err
		}
		return &rpc.SessionFsSqliteQueryResult{Columns: []string{}, Rows: []map[string]any{}}, nil

	case rpc.SessionFsSqliteQueryTypeQuery:
		rows, err := db.Query(trimmed)
		if err != nil {
			return nil, err
		}
		defer rows.Close()
		columns, _ := rows.Columns()
		var result []map[string]any
		for rows.Next() {
			vals := make([]any, len(columns))
			ptrs := make([]any, len(columns))
			for i := range vals {
				ptrs[i] = &vals[i]
			}
			if err := rows.Scan(ptrs...); err != nil {
				return nil, err
			}
			row := make(map[string]any)
			for i, col := range columns {
				row[col] = vals[i]
			}
			result = append(result, row)
		}
		if result == nil {
			result = []map[string]any{}
		}
		return &rpc.SessionFsSqliteQueryResult{Columns: columns, Rows: result}, nil

	case rpc.SessionFsSqliteQueryTypeRun:
		res, err := db.Exec(trimmed)
		if err != nil {
			return nil, err
		}
		affected, _ := res.RowsAffected()
		lastID, _ := res.LastInsertId()
		lastIDFloat := float64(lastID)
		return &rpc.SessionFsSqliteQueryResult{
			Columns:         []string{},
			Rows:            []map[string]any{},
			RowsAffected:    affected,
			LastInsertRowid: &lastIDFloat,
		}, nil
	}

	return &rpc.SessionFsSqliteQueryResult{Columns: []string{}, Rows: []map[string]any{}}, nil
}

func (p *inMemorySqliteProvider) SqliteExists(sessionID string) (bool, error) {
	p.mu.Lock()
	defer p.mu.Unlock()
	return p.db != nil, nil
}

func TestSessionFsSqliteE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	sessionStatePath := createSessionStatePath(t)
	sessionFsConfig := &copilot.SessionFsConfig{
		InitialCwd:       "/",
		SessionStatePath: sessionStatePath,
		Conventions:      rpc.SessionFsSetProviderConventionsPosix,
		Capabilities:     &copilot.SessionFsCapabilities{Sqlite: true},
	}

	var sqliteCalls []sqliteCall
	var providers sync.Map

	createSessionFsHandler := func(session *copilot.Session) copilot.SessionFsProvider {
		p := newInMemorySqliteProvider(session.SessionID, &sqliteCalls)
		providers.Store(session.SessionID, p)
		return p
	}

	client := ctx.NewClient(func(opts *copilot.ClientOptions) {
		opts.SessionFs = sessionFsConfig
	})
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should route sql queries through the sessionfs sqlite handler", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		sqliteCalls = nil

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest:    copilot.PermissionHandler.ApproveAll,
			CreateSessionFsHandler: createSessionFsHandler,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		msg, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: `Use the sql tool to create a table called "items" with columns id (TEXT PRIMARY KEY) and name (TEXT). ` +
				`Then insert a row with id "a1" and name "Widget". ` +
				`Then select all rows from items and tell me what you find.`,
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		content := ""
		if msg != nil {
			if d, ok := msg.Data.(*copilot.AssistantMessageData); ok {
				content = d.Content
			}
		}
		if !strings.Contains(content, "Widget") {
			t.Errorf("Expected response to contain 'Widget', got: %s", content)
		}

		// Verify sqlite handler was called
		sessionCalls := filterCalls(sqliteCalls, session.SessionID)
		if len(sessionCalls) == 0 {
			t.Fatal("Expected sqlite handler to be called")
		}
		assertCallContains(t, sessionCalls, "CREATE TABLE")
		assertCallContains(t, sessionCalls, "INSERT")
		assertCallContains(t, sessionCalls, "SELECT")

		// Verify queryType is set correctly
		assertQueryType(t, sessionCalls, "exec")
		assertQueryType(t, sessionCalls, "query")
		assertQueryType(t, sessionCalls, "run")

		if err := session.Disconnect(); err != nil {
			t.Fatalf("Failed to disconnect: %v", err)
		}
	})

	t.Run("should allow subagents to use sql tool via inherited sessionfs", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		sqliteCalls = nil

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest:    copilot.PermissionHandler.ApproveAll,
			CreateSessionFsHandler: createSessionFsHandler,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		_, err = session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Use the task tool to ask a task agent to do the following: " +
				"Use the sql tool to run this query: INSERT INTO todos " +
				"(id, title, status) VALUES ('subagent-test', 'Created by subagent', 'done')",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		if err := session.Disconnect(); err != nil {
			t.Fatalf("Failed to disconnect: %v", err)
		}

		// Verify INSERT calls were routed
		sessionCalls := filterCalls(sqliteCalls, session.SessionID)
		insertCalls := filterByQuery(sessionCalls, "INSERT")
		if len(insertCalls) == 0 {
			t.Fatal("Expected INSERT calls from subagent")
		}

		// Read events.jsonl from in-memory FS
		val, ok := providers.Load(session.SessionID)
		if !ok {
			t.Fatal("Provider not found for session")
		}
		provider := val.(*inMemorySqliteProvider)
		eventsPath := sessionStatePath + "/events.jsonl"
		content, err := provider.ReadFile(eventsPath)
		if err != nil {
			t.Fatalf("Failed to read events.jsonl: %v", err)
		}
		lines := strings.Split(strings.TrimSpace(content), "\n")
		var sqlToolEvents []map[string]any
		for _, line := range lines {
			if line == "" {
				continue
			}
			var event map[string]any
			if err := json.Unmarshal([]byte(line), &event); err != nil {
				continue
			}
			if event["type"] == "tool.execution_start" {
				if data, ok := event["data"].(map[string]any); ok {
					if data["toolName"] == "sql" {
						sqlToolEvents = append(sqlToolEvents, event)
					}
				}
			}
		}
		if len(sqlToolEvents) == 0 {
			t.Fatal("Expected sql tool events in events.jsonl")
		}
		for _, e := range sqlToolEvents {
			if e["agentId"] == nil || e["agentId"] == "" {
				t.Error("Expected agentId on sql tool event")
			}
		}
	})
}

func filterCalls(calls []sqliteCall, sessionID string) []sqliteCall {
	var result []sqliteCall
	for _, c := range calls {
		if c.SessionID == sessionID {
			result = append(result, c)
		}
	}
	return result
}

func filterByQuery(calls []sqliteCall, keyword string) []sqliteCall {
	var result []sqliteCall
	for _, c := range calls {
		if strings.Contains(strings.ToUpper(c.Query), keyword) {
			result = append(result, c)
		}
	}
	return result
}

func assertCallContains(t *testing.T, calls []sqliteCall, keyword string) {
	t.Helper()
	for _, c := range calls {
		if strings.Contains(strings.ToUpper(c.Query), keyword) {
			return
		}
	}
	t.Errorf("Expected a call with query containing %q", keyword)
}

func assertQueryType(t *testing.T, calls []sqliteCall, queryType string) {
	t.Helper()
	for _, c := range calls {
		if c.QueryType == queryType {
			return
		}
	}
	t.Errorf("Expected a call with queryType %q", queryType)
}
