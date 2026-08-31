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
	rewindFileName    = "rewind-sdk.txt"
	rewindFileContent = "SDK rewind content"
)

func TestRewindE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should restore tracked file and conversation", func(t *testing.T) {
		if runtime.GOOS == "windows" {
			t.Skip("blocked on CLI 1.0.81 file-change tracking regression on Windows")
		}

		ctx.ConfigureForTest(t)
		filePath := filepath.Join(ctx.WorkDir, rewindFileName)
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			Model:                    "claude-sonnet-4.5",
			EnableFileChangeTracking: copilot.Bool(true),
			OnPermissionRequest:      copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		defer session.Disconnect()

		response, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Use the create tool to create " + rewindFileName + " containing exactly " +
				rewindFileContent + ". After the tool succeeds, reply with exactly SDK_REWIND_DONE.",
		})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}
		responseData, ok := response.Data.(*copilot.AssistantMessageData)
		if !ok || responseData.Content != "SDK_REWIND_DONE" {
			t.Fatalf("Expected SDK_REWIND_DONE response, got %+v", response)
		}
		content, err := os.ReadFile(filePath)
		if err != nil {
			t.Fatalf("Failed to read created file: %v", err)
		}
		if string(content) != rewindFileContent {
			t.Fatalf("Expected file content %q, got %q", rewindFileContent, content)
		}

		rewindPoints := waitForRewindPoints(t, session)
		if !rewindPoints.FileChangeTrackingEnabled {
			t.Fatal("Expected file change tracking to be enabled")
		}
		if len(rewindPoints.Points) != 1 {
			t.Fatalf("Expected one rewind point, got %+v", rewindPoints.Points)
		}
		rewindPoint := rewindPoints.Points[0]
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
		if _, err := os.Stat(filePath); !os.IsNotExist(err) {
			t.Fatalf("Expected rewound file to be removed, stat error: %v", err)
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
			len(result.Points) == 1 &&
			result.Points[0].CanRestoreFiles &&
			result.Points[0].FileCount == 1 {
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
