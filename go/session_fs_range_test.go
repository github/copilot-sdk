// Copyright (c) Microsoft Corporation. All rights reserved.

package copilot

import (
	"encoding/base64"
	"os"
	"testing"

	"github.com/github/copilot-sdk/go/rpc"
)

type rangeOnlyProvider struct {
	SessionFSProvider
	read func(string, int64, *int64) (*SessionFSByteRange, error)
}

func (p rangeOnlyProvider) ReadFileRange(path string, offset int64, length *int64) (*SessionFSByteRange, error) {
	return p.read(path, offset, length)
}

func TestSessionFSByteRanges(t *testing.T) {
	data := []byte("a☃🚀z")
	two, zero, twenty := int64(2), int64(0), int64(20)
	for _, tc := range []struct {
		offset int64
		length *int64
	}{
		{2, &two}, {4, nil}, {3, &zero}, {7, &twenty}, {9, nil}, {100, &two},
	} {
		start := min(tc.offset, int64(len(data)))
		end := int64(len(data))
		if tc.length != nil {
			end = min(end, start+*tc.length)
		}
		want := data[start:end]
		called := false
		adapter := newSessionFSAdapter(rangeOnlyProvider{read: func(path string, offset int64, length *int64) (*SessionFSByteRange, error) {
			called = true
			if path != "/file" || offset != tc.offset || length != tc.length {
				t.Fatal("range request was altered")
			}
			return &SessionFSByteRange{Content: want, ByteLength: int64(len(data))}, nil
		}})
		result, err := adapter.ReadFileRange(&rpc.SessionFSReadFileRangeRequest{Path: "/file", Offset: tc.offset, Length: tc.length})
		if err != nil || result.Error != nil || !called {
			t.Fatalf("ReadFileRange = %+v, %v", result, err)
		}
		if result.Content != base64.StdEncoding.EncodeToString(want) || result.ByteLength != int64(len(data)) {
			t.Fatalf("unexpected range: %+v", result)
		}
	}
}

func TestSessionFSByteRangeValidationAndErrors(t *testing.T) {
	called := false
	adapter := newSessionFSAdapter(rangeOnlyProvider{read: func(string, int64, *int64) (*SessionFSByteRange, error) {
		called = true
		return nil, os.ErrNotExist
	}})
	for _, position := range []int64{-1, 9_007_199_254_740_992} {
		for _, request := range []*rpc.SessionFSReadFileRangeRequest{
			{Offset: position}, {Length: &position},
		} {
			result, err := adapter.ReadFileRange(request)
			if err != nil || result.Error == nil || called {
				t.Fatalf("invalid range reached provider: %+v, %v", result, err)
			}
		}
	}
	result, err := adapter.ReadFileRange(&rpc.SessionFSReadFileRangeRequest{})
	if err != nil || result.Error == nil || result.Error.Code != rpc.SessionFSErrorCodeENOENT {
		t.Fatalf("lost filesystem error: %+v, %v", result, err)
	}
	legacy := newSessionFSAdapter(failingWriteProvider{})
	result, err = legacy.ReadFileRange(&rpc.SessionFSReadFileRangeRequest{})
	if err != nil || result.Error == nil {
		t.Fatalf("legacy range did not fail: %+v, %v", result, err)
	}
	for _, invalid := range []*SessionFSByteRange{nil, {Content: []byte{1}, ByteLength: 5}, {Content: []byte{1, 2, 3}, ByteLength: 5}, {ByteLength: -1}, {ByteLength: 9_007_199_254_740_992}} {
		adapter := newSessionFSAdapter(rangeOnlyProvider{read: func(string, int64, *int64) (*SessionFSByteRange, error) { return invalid, nil }})
		length := int64(2)
		result, err := adapter.ReadFileRange(&rpc.SessionFSReadFileRangeRequest{Offset: 1, Length: &length})
		if err != nil || result.Error == nil {
			t.Fatalf("accepted invalid provider range: %+v, %v", result, err)
		}
	}
}
