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
	ReaddirWithTypes(path string) ([]rpc.SessionFSReaddirWithTypesEntry, error)
	// Rm removes a file or directory. If recursive is true, remove contents too.
	// If force is true, do not return an error when the path does not exist.
	Rm(path string, recursive bool, force bool) error
	// Rename moves/renames a file or directory.
	Rename(src string, dest string) error
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
// converting idiomatic Go errors into SessionFSError results.
type sessionFsAdapter struct {
	provider SessionFsProvider
}

func newSessionFsAdapter(provider SessionFsProvider) rpc.SessionFsHandler {
	return &sessionFsAdapter{provider: provider}
}

func (a *sessionFsAdapter) ReadFile(request *rpc.SessionFSReadFileRequest) (*rpc.SessionFSReadFileResult, error) {
	content, err := a.provider.ReadFile(request.Path)
	if err != nil {
		return &rpc.SessionFSReadFileResult{Error: toSessionFsError(err)}, nil
	}
	return &rpc.SessionFSReadFileResult{Content: content}, nil
}

func (a *sessionFsAdapter) WriteFile(request *rpc.SessionFSWriteFileRequest) (*rpc.SessionFSError, error) {
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

func (a *sessionFsAdapter) AppendFile(request *rpc.SessionFSAppendFileRequest) (*rpc.SessionFSError, error) {
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

func (a *sessionFsAdapter) Exists(request *rpc.SessionFSExistsRequest) (*rpc.SessionFSExistsResult, error) {
	exists, err := a.provider.Exists(request.Path)
	if err != nil {
		return &rpc.SessionFSExistsResult{Exists: false}, nil
	}
	return &rpc.SessionFSExistsResult{Exists: exists}, nil
}

func (a *sessionFsAdapter) Stat(request *rpc.SessionFSStatRequest) (*rpc.SessionFSStatResult, error) {
	info, err := a.provider.Stat(request.Path)
	if err != nil {
		return &rpc.SessionFSStatResult{Error: toSessionFsError(err)}, nil
	}
	return &rpc.SessionFSStatResult{
		IsFile:      info.IsFile,
		IsDirectory: info.IsDirectory,
		Size:        info.Size,
		Mtime:       info.Mtime,
		Birthtime:   info.Birthtime,
	}, nil
}

func (a *sessionFsAdapter) Mkdir(request *rpc.SessionFSMkdirRequest) (*rpc.SessionFSError, error) {
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

func (a *sessionFsAdapter) Readdir(request *rpc.SessionFSReaddirRequest) (*rpc.SessionFSReaddirResult, error) {
	entries, err := a.provider.Readdir(request.Path)
	if err != nil {
		return &rpc.SessionFSReaddirResult{Error: toSessionFsError(err)}, nil
	}
	return &rpc.SessionFSReaddirResult{Entries: entries}, nil
}

func (a *sessionFsAdapter) ReaddirWithTypes(request *rpc.SessionFSReaddirWithTypesRequest) (*rpc.SessionFSReaddirWithTypesResult, error) {
	entries, err := a.provider.ReaddirWithTypes(request.Path)
	if err != nil {
		return &rpc.SessionFSReaddirWithTypesResult{Error: toSessionFsError(err)}, nil
	}
	return &rpc.SessionFSReaddirWithTypesResult{Entries: entries}, nil
}

func (a *sessionFsAdapter) Rm(request *rpc.SessionFSRmRequest) (*rpc.SessionFSError, error) {
	recursive := request.Recursive != nil && *request.Recursive
	force := request.Force != nil && *request.Force
	if err := a.provider.Rm(request.Path, recursive, force); err != nil {
		return toSessionFsError(err), nil
	}
	return nil, nil
}

func (a *sessionFsAdapter) Rename(request *rpc.SessionFSRenameRequest) (*rpc.SessionFSError, error) {
	if err := a.provider.Rename(request.Src, request.Dest); err != nil {
		return toSessionFsError(err), nil
	}
	return nil, nil
}

func toSessionFsError(err error) *rpc.SessionFSError {
	code := rpc.SessionFSErrorCodeUNKNOWN
	if errors.Is(err, os.ErrNotExist) {
		code = rpc.SessionFSErrorCodeENOENT
	}
	msg := err.Error()
	return &rpc.SessionFSError{Code: code, Message: &msg}
}
