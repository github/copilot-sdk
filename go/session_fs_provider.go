/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package copilot

import (
	"errors"
	"os"
	"time"

	"github.com/github/copilot-sdk/go/rpc"
)

// SessionFsProvider is the interface that SDK users implement to provide
// a session filesystem. Methods use idiomatic Go error handling: return an
// error for failures (the adapter maps os.ErrNotExist → ENOENT automatically).
//
// To add SQLite support, also implement [SessionFsSqliteProvider] on the same type.
type SessionFsProvider interface {
	// ReadFile reads the full content of a file. Return os.ErrNotExist (or wrap it)
	// if the file does not exist.
	ReadFile(path string) (string, error)
	// WriteFile writes content to a file, creating it and parent directories if needed.
	// mode is an optional POSIX-style permission mode. Pass nil to use the OS default.
	WriteFile(path string, content string, mode *int) error
	// AppendFile appends content to a file, creating it and parent directories if needed.
	// mode is an optional POSIX-style permission mode. Pass nil to use the OS default.
	AppendFile(path string, content string, mode *int) error
	// Exists checks whether the given path exists.
	Exists(path string) (bool, error)
	// Stat returns metadata about a file or directory.
	// Return os.ErrNotExist if the path does not exist.
	Stat(path string) (*SessionFsFileInfo, error)
	// Mkdir creates a directory. If recursive is true, create parent directories as needed.
	// mode is an optional POSIX-style permission mode (e.g., 0o755). Pass nil to use the OS default.
	Mkdir(path string, recursive bool, mode *int) error
	// Readdir lists the names of entries in a directory.
	// Return os.ErrNotExist if the directory does not exist.
	Readdir(path string) ([]string, error)
	// ReaddirWithTypes lists entries with type information.
	// Return os.ErrNotExist if the directory does not exist.
	ReaddirWithTypes(path string) ([]rpc.SessionFsReaddirWithTypesEntry, error)
	// Rm removes a file or directory. If recursive is true, remove contents too.
	// If force is true, do not return an error when the path does not exist.
	Rm(path string, recursive bool, force bool) error
	// Rename moves/renames a file or directory.
	Rename(src string, dest string) error
}

// SessionFsSqliteProvider is an optional interface that a [SessionFsProvider]
// may also implement to support per-session SQLite databases. The adapter
// checks for this interface at runtime using a type assertion. If the
// provider does not implement it, SQLite requests return an "unsupported" error.
//
// Providers are already session-scoped (created per session by the factory),
// so these methods do not take a session ID parameter.
type SessionFsSqliteProvider interface {
	// SqliteQuery executes a SQLite query against the provider's per-session database.
	SqliteQuery(queryType rpc.SessionFsSqliteQueryType, query string, params map[string]any) (*SessionFsSqliteQueryResult, error)
	// SqliteExists checks whether the provider has a SQLite database for the session.
	SqliteExists() (bool, error)
}

// SessionFsSqliteQueryResult holds the result of a SQLite query execution.
// Same shape as the generated RPC type but without the Error field,
// since providers signal errors by returning a Go error.
type SessionFsSqliteQueryResult struct {
	Columns         []string         `json:"columns"`
	Rows            []map[string]any `json:"rows"`
	RowsAffected    int64            `json:"rowsAffected"`
	LastInsertRowid *int64           `json:"lastInsertRowid,omitempty"`
}

// SessionFsFileInfo holds file metadata returned by SessionFsProvider.Stat.
type SessionFsFileInfo struct {
	IsFile      bool
	IsDirectory bool
	Size        int64
	Mtime       time.Time
	Birthtime   time.Time
}

// sessionFsAdapter wraps a SessionFsProvider to implement rpc.SessionFsHandler,
// converting idiomatic Go errors into SessionFsError results.
type sessionFsAdapter struct {
	provider SessionFsProvider
}

func newSessionFsAdapter(provider SessionFsProvider) rpc.SessionFsHandler {
	return &sessionFsAdapter{provider: provider}
}

func (a *sessionFsAdapter) ReadFile(request *rpc.SessionFsReadFileRequest) (*rpc.SessionFsReadFileResult, error) {
	content, err := a.provider.ReadFile(request.Path)
	if err != nil {
		return &rpc.SessionFsReadFileResult{Error: toSessionFsError(err)}, nil
	}
	return &rpc.SessionFsReadFileResult{Content: content}, nil
}

func (a *sessionFsAdapter) WriteFile(request *rpc.SessionFsWriteFileRequest) (*rpc.SessionFsError, error) {
	var mode *int
	if request.Mode != nil {
		m := int(*request.Mode)
		mode = &m
	}
	if err := a.provider.WriteFile(request.Path, request.Content, mode); err != nil {
		return toSessionFsError(err), nil
	}
	return nil, nil
}

func (a *sessionFsAdapter) AppendFile(request *rpc.SessionFsAppendFileRequest) (*rpc.SessionFsError, error) {
	var mode *int
	if request.Mode != nil {
		m := int(*request.Mode)
		mode = &m
	}
	if err := a.provider.AppendFile(request.Path, request.Content, mode); err != nil {
		return toSessionFsError(err), nil
	}
	return nil, nil
}

func (a *sessionFsAdapter) Exists(request *rpc.SessionFsExistsRequest) (*rpc.SessionFsExistsResult, error) {
	exists, err := a.provider.Exists(request.Path)
	if err != nil {
		return &rpc.SessionFsExistsResult{Exists: false}, nil
	}
	return &rpc.SessionFsExistsResult{Exists: exists}, nil
}

func (a *sessionFsAdapter) Stat(request *rpc.SessionFsStatRequest) (*rpc.SessionFsStatResult, error) {
	info, err := a.provider.Stat(request.Path)
	if err != nil {
		return &rpc.SessionFsStatResult{Error: toSessionFsError(err)}, nil
	}
	return &rpc.SessionFsStatResult{
		IsFile:      info.IsFile,
		IsDirectory: info.IsDirectory,
		Size:        info.Size,
		Mtime:       info.Mtime,
		Birthtime:   info.Birthtime,
	}, nil
}

func (a *sessionFsAdapter) Mkdir(request *rpc.SessionFsMkdirRequest) (*rpc.SessionFsError, error) {
	recursive := request.Recursive != nil && *request.Recursive
	var mode *int
	if request.Mode != nil {
		m := int(*request.Mode)
		mode = &m
	}
	if err := a.provider.Mkdir(request.Path, recursive, mode); err != nil {
		return toSessionFsError(err), nil
	}
	return nil, nil
}

func (a *sessionFsAdapter) Readdir(request *rpc.SessionFsReaddirRequest) (*rpc.SessionFsReaddirResult, error) {
	entries, err := a.provider.Readdir(request.Path)
	if err != nil {
		return &rpc.SessionFsReaddirResult{Error: toSessionFsError(err)}, nil
	}
	return &rpc.SessionFsReaddirResult{Entries: entries}, nil
}

func (a *sessionFsAdapter) ReaddirWithTypes(request *rpc.SessionFsReaddirWithTypesRequest) (*rpc.SessionFsReaddirWithTypesResult, error) {
	entries, err := a.provider.ReaddirWithTypes(request.Path)
	if err != nil {
		return &rpc.SessionFsReaddirWithTypesResult{Error: toSessionFsError(err)}, nil
	}
	return &rpc.SessionFsReaddirWithTypesResult{Entries: entries}, nil
}

func (a *sessionFsAdapter) Rm(request *rpc.SessionFsRmRequest) (*rpc.SessionFsError, error) {
	recursive := request.Recursive != nil && *request.Recursive
	force := request.Force != nil && *request.Force
	if err := a.provider.Rm(request.Path, recursive, force); err != nil {
		return toSessionFsError(err), nil
	}
	return nil, nil
}

func (a *sessionFsAdapter) Rename(request *rpc.SessionFsRenameRequest) (*rpc.SessionFsError, error) {
	if err := a.provider.Rename(request.Src, request.Dest); err != nil {
		return toSessionFsError(err), nil
	}
	return nil, nil
}

func (a *sessionFsAdapter) SqliteQuery(request *rpc.SessionFsSqliteQueryRequest) (*rpc.SessionFsSqliteQueryResult, error) {
	sp, ok := a.provider.(SessionFsSqliteProvider)
	if !ok {
		msg := "SQLite is not supported by this session filesystem provider"
		return &rpc.SessionFsSqliteQueryResult{
			Columns:      []string{},
			Rows:         []map[string]any{},
			RowsAffected: 0,
			Error:        &rpc.SessionFsError{Code: rpc.SessionFsErrorCodeUNKNOWN, Message: &msg},
		}, nil
	}
	result, err := sp.SqliteQuery(request.QueryType, request.Query, request.Params)
	if err != nil {
		return &rpc.SessionFsSqliteQueryResult{
			Columns:      []string{},
			Rows:         []map[string]any{},
			RowsAffected: 0,
			Error:        toSessionFsError(err),
		}, nil
	}
	if result == nil {
		return &rpc.SessionFsSqliteQueryResult{
			Columns:      []string{},
			Rows:         []map[string]any{},
			RowsAffected: 0,
		}, nil
	}
	var wireRowid *float64
	if result.LastInsertRowid != nil {
		f := float64(*result.LastInsertRowid)
		wireRowid = &f
	}
	return &rpc.SessionFsSqliteQueryResult{
		Columns:         result.Columns,
		Rows:            result.Rows,
		RowsAffected:    result.RowsAffected,
		LastInsertRowid: wireRowid,
	}, nil
}

func (a *sessionFsAdapter) SqliteExists(request *rpc.SessionFsSqliteExistsRequest) (*rpc.SessionFsSqliteExistsResult, error) {
	sp, ok := a.provider.(SessionFsSqliteProvider)
	if !ok {
		return &rpc.SessionFsSqliteExistsResult{Exists: false}, nil
	}
	exists, err := sp.SqliteExists()
	if err != nil {
		return &rpc.SessionFsSqliteExistsResult{Exists: false}, nil
	}
	return &rpc.SessionFsSqliteExistsResult{Exists: exists}, nil
}

func toSessionFsError(err error) *rpc.SessionFsError {
	code := rpc.SessionFsErrorCodeUNKNOWN
	if errors.Is(err, os.ErrNotExist) {
		code = rpc.SessionFsErrorCodeENOENT
	}
	msg := err.Error()
	return &rpc.SessionFsError{Code: code, Message: &msg}
}
