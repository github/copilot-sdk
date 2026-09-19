// Copyright (c) Microsoft Corporation. All rights reserved.

package rpc

import (
	"encoding/json"
	"strings"
	"testing"
)

func TestSessionsGetClientMetadataResultUnmarshalJSON(t *testing.T) {
	const input = `[
		{"status":"corrupt","sessionId":"corrupt-session"},
		{"status":"notFound","sessionId":"missing-session"},
		{"status":"ok","sessionId":"ok-session","metadata":{"branch":"main","owner":"github"}},
		{"status":"unavailable","sessionId":"locked-session","code":"EBUSY","message":"metadata is locked"},
		{"status":"unsupportedVersion","sessionId":"future-session"}
	]`

	var result SessionsGetClientMetadataResult
	if err := json.Unmarshal([]byte(input), &result); err != nil {
		t.Fatalf("Unmarshal failed: %v", err)
	}
	if len(result) != 5 {
		t.Fatalf("Expected five entries, got %d", len(result))
	}

	corrupt, ok := result[0].(*SessionsClientMetadataEntryCorrupt)
	if !ok || corrupt.SessionID != "corrupt-session" {
		t.Fatalf("Unexpected corrupt entry: %#v", result[0])
	}
	notFound, ok := result[1].(*SessionsClientMetadataEntryNotFound)
	if !ok || notFound.SessionID != "missing-session" {
		t.Fatalf("Unexpected not-found entry: %#v", result[1])
	}
	metadata, ok := result[2].(*SessionsClientMetadataEntryOk)
	if !ok || metadata.SessionID != "ok-session" || metadata.Metadata["branch"] != "main" || metadata.Metadata["owner"] != "github" {
		t.Fatalf("Unexpected ok entry: %#v", result[2])
	}
	unavailable, ok := result[3].(*SessionsClientMetadataEntryUnavailable)
	if !ok || unavailable.SessionID != "locked-session" || unavailable.Code != "EBUSY" || unavailable.Message != "metadata is locked" {
		t.Fatalf("Unexpected unavailable entry: %#v", result[3])
	}
	unsupported, ok := result[4].(*SessionsClientMetadataEntryUnsupportedVersion)
	if !ok || unsupported.SessionID != "future-session" {
		t.Fatalf("Unexpected unsupported-version entry: %#v", result[4])
	}
}

func TestSessionsGetClientMetadataResultUnmarshalJSONRejectsInvalidEntries(t *testing.T) {
	tests := []struct {
		name  string
		input string
		want  string
	}{
		{
			name:  "malformed entry",
			input: `[{"status":42,"sessionId":"session"}]`,
			want:  "cannot unmarshal number",
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			var result SessionsGetClientMetadataResult
			err := json.Unmarshal([]byte(test.input), &result)
			if err == nil || !strings.Contains(err.Error(), test.want) {
				t.Fatalf("Expected error containing %q, got %v", test.want, err)
			}
		})
	}
}

func TestSessionsGetClientMetadataResultUnmarshalJSONPreservesUnknownEntries(t *testing.T) {
	const input = `[{"status":"future-status","sessionId":"future-session","detail":{"attempts":2}}]`
	var result SessionsGetClientMetadataResult
	if err := json.Unmarshal([]byte(input), &result); err != nil {
		t.Fatalf("Unmarshal failed: %v", err)
	}
	if len(result) != 1 {
		t.Fatalf("Expected one entry, got %d", len(result))
	}
	raw, ok := result[0].(*RawSessionsClientMetadataEntryData)
	if !ok || raw.Discriminator != "future-status" || string(raw.Raw) != input[1:len(input)-1] {
		t.Fatalf("Unexpected raw entry: %#v", result[0])
	}
}

func TestSessionsGetClientMetadataResultUnmarshalJSONPreservesNull(t *testing.T) {
	result := SessionsGetClientMetadataResult{
		&SessionsClientMetadataEntryNotFound{SessionID: "existing"},
	}
	if err := json.Unmarshal([]byte("null"), &result); err != nil {
		t.Fatalf("Unmarshal failed: %v", err)
	}
	if result != nil {
		t.Fatalf("Expected nil result, got %#v", result)
	}
}
