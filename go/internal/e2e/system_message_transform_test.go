// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package e2e

import (
	"os"
	"path/filepath"
	"strings"
	"sync"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

func TestSystemMessageTransform(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should_invoke_transform_callbacks_with_section_content", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		var identityContent string
		var toneContent string
		var mu sync.Mutex
		identityCalled := false
		toneCalled := false

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			SystemMessage: &copilot.SystemMessageConfig{
				Mode: "customize",
				Sections: map[string]copilot.SectionOverride{
					"identity": {
						Transform: func(currentContent string) (string, error) {
							mu.Lock()
							identityCalled = true
							identityContent = currentContent
							mu.Unlock()
							return currentContent, nil
						},
					},
					"tone": {
						Transform: func(currentContent string) (string, error) {
							mu.Lock()
							toneCalled = true
							toneContent = currentContent
							mu.Unlock()
							return currentContent, nil
						},
					},
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		testFile := filepath.Join(ctx.WorkDir, "test.txt")
		err = os.WriteFile(testFile, []byte("Hello transform!"), 0644)
		if err != nil {
			t.Fatalf("Failed to write test file: %v", err)
		}

		_, err = session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Read the contents of test.txt and tell me what it says",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		mu.Lock()
		defer mu.Unlock()

		if !identityCalled {
			t.Error("Expected identity transform callback to be invoked")
		}
		if !toneCalled {
			t.Error("Expected tone transform callback to be invoked")
		}
		if identityContent == "" {
			t.Error("Expected identity transform to receive non-empty content")
		}
		if toneContent == "" {
			t.Error("Expected tone transform to receive non-empty content")
		}
	})

	t.Run("should_apply_transform_modifications_to_section_content", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			SystemMessage: &copilot.SystemMessageConfig{
				Mode: "customize",
				Sections: map[string]copilot.SectionOverride{
					"identity": {
						Transform: func(currentContent string) (string, error) {
							return currentContent + "\nAlways end your reply with TRANSFORM_MARKER", nil
						},
					},
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		testFile := filepath.Join(ctx.WorkDir, "hello.txt")
		err = os.WriteFile(testFile, []byte("Hello!"), 0644)
		if err != nil {
			t.Fatalf("Failed to write test file: %v", err)
		}

		assistantMessage, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Read the contents of hello.txt",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		// Verify the transform result was actually applied to the system message
		traffic, err := ctx.GetExchanges()
		if err != nil {
			t.Fatalf("Failed to get exchanges: %v", err)
		}
		if len(traffic) == 0 {
			t.Fatal("Expected at least one exchange")
		}
		systemMessage := getSystemMessage(traffic[0])
		if !strings.Contains(systemMessage, "TRANSFORM_MARKER") {
			t.Errorf("Expected system message to contain TRANSFORM_MARKER, got %q", systemMessage)
		}

		_ = assistantMessage
	})

	t.Run("should_work_with_static_overrides_and_transforms_together", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		var mu sync.Mutex
		transformCalled := false

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			SystemMessage: &copilot.SystemMessageConfig{
				Mode: "customize",
				Sections: map[string]copilot.SectionOverride{
					"safety": {
						Action: copilot.SectionActionRemove,
					},
					"identity": {
						Transform: func(currentContent string) (string, error) {
							mu.Lock()
							transformCalled = true
							mu.Unlock()
							return currentContent, nil
						},
					},
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		testFile := filepath.Join(ctx.WorkDir, "combo.txt")
		err = os.WriteFile(testFile, []byte("Combo test!"), 0644)
		if err != nil {
			t.Fatalf("Failed to write test file: %v", err)
		}

		_, err = session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Read the contents of combo.txt and tell me what it says",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		mu.Lock()
		defer mu.Unlock()

		if !transformCalled {
			t.Error("Expected identity transform callback to be invoked")
		}
	})
}
