package e2e

import (
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

const (
	rewindFileName            = "rewind-sdk.txt"
	rewindFileOriginalContent = "Original rewind content"
	rewindFilePreparedContent = "Prepared rewind content"
	rewindFileContent         = "SDK rewind content"
)

func TestRewindE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should restore tracked file and conversation", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		filePath := filepath.Join(ctx.WorkDir, rewindFileName)
		if err := os.WriteFile(filePath, []byte(rewindFileOriginalContent), 0o600); err != nil {
			t.Fatalf("Failed to create original file: %v", err)
		}
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			Model:                    "claude-sonnet-4.5",
			EnableFileChangeTracking: copilot.Bool(true),
			OnPermissionRequest:      copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		defer session.Disconnect()

		ready, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Use the edit tool to replace the exact contents of " + rewindFileName + " from " +
				rewindFileOriginalContent + " to " + rewindFilePreparedContent +
				". After the tool succeeds, reply with exactly SDK_REWIND_READY.",
		})
		if err != nil {
			t.Fatalf("SendAndWait readiness turn failed: %v", err)
		}
		readyData, ok := ready.Data.(*copilot.AssistantMessageData)
		if !ok || readyData.Content != "SDK_REWIND_READY" {
			t.Fatalf("Expected SDK_REWIND_READY response, got %+v", ready)
		}
		content, err := os.ReadFile(filePath)
		if err != nil {
			t.Fatalf("Failed to read prepared file: %v", err)
		}
		if string(content) != rewindFilePreparedContent {
			t.Fatalf("Expected file content %q, got %q", rewindFilePreparedContent, content)
		}

		response, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Use the edit tool to replace the exact contents of " + rewindFileName + " from " +
				rewindFilePreparedContent + " to " + rewindFileContent +
				". After the tool succeeds, reply with exactly SDK_REWIND_DONE.",
		})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}
		responseData, ok := response.Data.(*copilot.AssistantMessageData)
		if !ok || responseData.Content != "SDK_REWIND_DONE" {
			t.Fatalf("Expected SDK_REWIND_DONE response, got %+v", response)
		}
		content, err = os.ReadFile(filePath)
		if err != nil {
			t.Fatalf("Failed to read updated file: %v", err)
		}
		if string(content) != rewindFileContent {
			t.Fatalf("Expected file content %q, got %q", rewindFileContent, content)
		}

		rewindPoints := waitForRewindPoints(t, session)
		if !rewindPoints.FileChangeTrackingEnabled {
			t.Fatal("Expected file change tracking to be enabled")
		}
		if len(rewindPoints.Points) != 2 {
			t.Fatalf("Expected two rewind points, got %+v", rewindPoints.Points)
		}
		rewindPoint := rewindPoints.Points[1]
		if !rewindPoint.TurnChangedFiles {
			t.Fatalf("Expected the edit turn to report changed files, got %+v", rewindPoint)
		}
		if !rewindPoint.CanRestoreFiles || rewindPoint.FileCount != 1 {
			t.Fatalf("Expected one restorable file, got %+v", rewindPoint)
		}

		preview, err := session.RPC.History.PreviewRewind(t.Context(), &rpc.HistoryPreviewRewindRequest{
			EventID: rewindPoint.EventID,
		})
		if err != nil {
			t.Fatalf("PreviewRewind failed: %v", err)
		}
		if !preview.Available || len(preview.Files) != 1 {
			t.Fatalf("Expected one available preview file, got %+v", preview)
		}
		assertSameRewindPath(t, filePath, preview.Files[0].Path)

		rewind, err := session.RPC.History.Rewind(t.Context(), &rpc.HistoryRewindRequest{
			EventID: rewindPoint.EventID,
			Mode:    rpc.HistoryRewindModeConversationAndFiles,
		})
		if err != nil {
			t.Fatalf("Rewind failed: %v", err)
		}
		if rewind.Outcome != rpc.HistoryRewindOutcomeSuccess {
			t.Fatalf("Expected successful rewind, got %+v", rewind)
		}
		if rewind.EventsRemoved == nil || *rewind.EventsRemoved < 1 {
			t.Fatalf("Expected rewind to remove events, got %+v", rewind)
		}
		if len(rewind.RestoredFiles) != 1 {
			t.Fatalf("Expected one restored file, got %+v", rewind.RestoredFiles)
		}
		assertSameRewindPath(t, filePath, rewind.RestoredFiles[0])
		content, err = os.ReadFile(filePath)
		if err != nil {
			t.Fatalf("Failed to read restored file: %v", err)
		}
		if string(content) != rewindFilePreparedContent {
			t.Fatalf("Expected restored file content %q, got %q", rewindFilePreparedContent, content)
		}

		events, err := session.GetEvents(t.Context())
		if err != nil {
			t.Fatalf("GetEvents failed: %v", err)
		}
		for _, event := range events {
			if event.ID == rewindPoint.EventID {
				t.Fatalf("Expected rewound event %q to be removed", rewindPoint.EventID)
			}
		}
	})
}

func waitForRewindPoints(t *testing.T, session *copilot.Session) *rpc.HistoryListRewindPointsResult {
	t.Helper()
	deadline := time.Now().Add(30 * time.Second)
	for {
		result, err := session.RPC.History.ListRewindPoints(t.Context())
		if err != nil {
			t.Fatalf("ListRewindPoints failed: %v", err)
		}
		if result.UnavailableReason == nil &&
			len(result.Points) == 2 &&
			result.Points[1].TurnChangedFiles &&
			result.Points[1].CanRestoreFiles &&
			result.Points[1].FileCount == 1 {
			return result
		}
		if time.Now().After(deadline) {
			t.Fatalf("Timed out waiting for a restorable rewind point: %+v", result)
		}
		time.Sleep(100 * time.Millisecond)
	}
}

func assertSameRewindPath(t *testing.T, expected, actual string) {
	t.Helper()
	expectedPath, err := filepath.Abs(expected)
	if err != nil {
		t.Fatalf("Failed to resolve expected path: %v", err)
	}
	actualPath, err := filepath.Abs(actual)
	if err != nil {
		t.Fatalf("Failed to resolve actual path: %v", err)
	}

	expectedPath = filepath.Clean(expectedPath)
	actualPath = filepath.Clean(actualPath)
	if runtime.GOOS == "windows" {
		if !strings.EqualFold(expectedPath, actualPath) {
			t.Fatalf("Expected path %q, got %q", expectedPath, actualPath)
		}
	} else if expectedPath != actualPath {
		t.Fatalf("Expected path %q, got %q", expectedPath, actualPath)
	}
}
