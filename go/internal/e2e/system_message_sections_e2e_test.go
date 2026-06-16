// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package e2e

import (
	"strings"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

func TestSystemMessageSectionsE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should_use_replaced_identity_section_in_response", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			SystemMessage: &copilot.SystemMessageConfig{
				Mode: "customize",
				Sections: map[string]copilot.SectionOverride{
					"identity": {
						Action:  copilot.SectionActionReplace,
						Content: "You are a helpful gardening assistant called Botanica. You only answer questions about plants and gardening.",
					},
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		response, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Who are you?",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}
		if response == nil {
			t.Fatal("Expected a response from the assistant")
		}

		ad, ok := response.Data.(*copilot.AssistantMessageData)
		if !ok {
			t.Fatalf("Expected AssistantMessageData, got %T", response.Data)
		}
		content := strings.ToLower(ad.Content)
		if !strings.Contains(content, "botanica") && !strings.Contains(content, "garden") && !strings.Contains(content, "plant") {
			t.Errorf("Expected response to reflect the replaced identity section, but got: %s", ad.Content)
		}
	})
}
