// Copyright (c) Microsoft Corporation. All rights reserved.

package copilot

import (
	"errors"
	"fmt"
	"os"
	"testing"

	"github.com/github/copilot-sdk/go/rpc"
)

type failingWriteProvider struct {
	SessionFSProvider
	err error
}

func (p failingWriteProvider) WriteFile(string, string, *int) error  { return p.err }
func (p failingWriteProvider) AppendFile(string, string, *int) error { return p.err }

func TestSessionFSWriteFailure(t *testing.T) {
	for _, tc := range []struct {
		name        string
		err         error
		wantChanged bool
		wantCode    rpc.SessionFSErrorCode
		wantMessage string
	}{
		{"rejected", errors.New("rejected"), false, rpc.SessionFSErrorCodeUNKNOWN, "rejected"},
		{"partial", &SessionFSWriteFailure{Err: errors.New("disk full")}, true, rpc.SessionFSErrorCodeUNKNOWN, "disk full"},
		{"wrapped", fmt.Errorf("write failed: %w", &SessionFSWriteFailure{Err: os.ErrNotExist}), true, rpc.SessionFSErrorCodeENOENT, "write failed: file does not exist"},
		{"nil cause", &SessionFSWriteFailure{}, true, rpc.SessionFSErrorCodeUNKNOWN, "session filesystem write failed after changing the target"},
		{"nil marker", (*SessionFSWriteFailure)(nil), false, rpc.SessionFSErrorCodeUNKNOWN, "session filesystem write failed after changing the target"},
	} {
		t.Run(tc.name, func(t *testing.T) {
			adapter := newSessionFSAdapter(failingWriteProvider{err: tc.err})
			wire, err := adapter.WriteFile(&rpc.SessionFSWriteFileRequest{Path: "/file", Content: "data"})
			if err != nil || wire == nil {
				t.Fatalf("WriteFile = %v, %v; want wire error", wire, err)
			}
			if wire.Code != tc.wantCode {
				t.Errorf("code = %v; want %v", wire.Code, tc.wantCode)
			}
			if wire.Message == nil || *wire.Message != tc.wantMessage {
				t.Errorf("message = %v; want %q", wire.Message, tc.wantMessage)
			}
			gotChanged := wire.WriteChanged != nil && *wire.WriteChanged
			if gotChanged != tc.wantChanged || !tc.wantChanged && wire.WriteChanged != nil {
				t.Errorf("writeChanged = %v; want %v", wire.WriteChanged, tc.wantChanged)
			}
		})
	}
}

func TestSessionFSWriteFailureDoesNotMarkAppend(t *testing.T) {
	adapter := newSessionFSAdapter(failingWriteProvider{
		err: &SessionFSWriteFailure{Err: errors.New("append failed")},
	})
	wire, err := adapter.AppendFile(&rpc.SessionFSAppendFileRequest{Path: "/file", Content: "data"})
	if err != nil || wire == nil || wire.WriteChanged != nil {
		t.Fatalf("AppendFile = %v, %v; want unmarked wire error", wire, err)
	}
}
