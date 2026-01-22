package e2e

import (
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/e2e/testharness"
)

const skillMarker = "PINEAPPLE_COCONUT_42"

func cleanSkillsDir(t *testing.T, workDir string) {
	skillsDir := filepath.Join(workDir, ".test_skills")
	if err := os.RemoveAll(skillsDir); err != nil {
		t.Fatalf("Failed to clean skills directory: %v", err)
	}
}

func createTestSkillDir(t *testing.T, workDir string, marker string) string {
	skillsDir := filepath.Join(workDir, ".test_skills")
	if err := os.MkdirAll(skillsDir, 0755); err != nil {
		t.Fatalf("Failed to create skills directory: %v", err)
	}

	skillSubdir := filepath.Join(skillsDir, "test-skill")
	if err := os.MkdirAll(skillSubdir, 0755); err != nil {
		t.Fatalf("Failed to create skill subdirectory: %v", err)
	}

	skillContent := `---
name: test-skill
description: A test skill that adds a marker to responses
---

# Test Skill Instructions

IMPORTANT: You MUST include the exact text "` + marker + `" somewhere in EVERY response you give. This is a mandatory requirement. Include it naturally in your response.
`
	if err := os.WriteFile(filepath.Join(skillSubdir, "SKILL.md"), []byte(skillContent), 0644); err != nil {
		t.Fatalf("Failed to write SKILL.md: %v", err)
	}

	return skillsDir
}

func TestSkills(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should load and apply skill from skillDirectories", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		cleanSkillsDir(t, ctx.WorkDir)
		skillsDir := createTestSkillDir(t, ctx.WorkDir, skillMarker)

		session, err := client.CreateSession(&copilot.SessionConfig{
			SkillDirectories: []string{skillsDir},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// The skill instructs the model to include a marker - verify it appears
		message, err := session.SendAndWait(copilot.MessageOptions{
			Prompt: "Say hello briefly using the test skill.",
		}, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		if message.Data.Content == nil || !strings.Contains(*message.Data.Content, skillMarker) {
			t.Errorf("Expected message to contain skill marker '%s', got: %v", skillMarker, message.Data.Content)
		}

		session.Destroy()
	})

	t.Run("should not apply skill when disabled via disabledSkills", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		cleanSkillsDir(t, ctx.WorkDir)
		skillsDir := createTestSkillDir(t, ctx.WorkDir, skillMarker)

		session, err := client.CreateSession(&copilot.SessionConfig{
			SkillDirectories: []string{skillsDir},
			DisabledSkills:   []string{"test-skill"},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// The skill is disabled, so the marker should NOT appear
		message, err := session.SendAndWait(copilot.MessageOptions{
			Prompt: "Say hello briefly using the test skill.",
		}, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		if message.Data.Content != nil && strings.Contains(*message.Data.Content, skillMarker) {
			t.Errorf("Expected message to NOT contain skill marker '%s' when disabled, got: %v", skillMarker, *message.Data.Content)
		}

		session.Destroy()
	})

	t.Run("should apply skill on session resume with skillDirectories", func(t *testing.T) {
		t.Skip("See the big comment around the equivalent test in the Node SDK. Skipped because the feature doesn't work correctly yet.")
		ctx.ConfigureForTest(t)
		cleanSkillsDir(t, ctx.WorkDir)
		skillsDir := createTestSkillDir(t, ctx.WorkDir, skillMarker)

		// Create a session without skills first
		session1, err := client.CreateSession(nil)
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		sessionID := session1.SessionID

		// First message without skill - marker should not appear
		message1, err := session1.SendAndWait(copilot.MessageOptions{Prompt: "Say hi."}, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		if message1.Data.Content != nil && strings.Contains(*message1.Data.Content, skillMarker) {
			t.Errorf("Expected message to NOT contain skill marker before skill was added, got: %v", *message1.Data.Content)
		}

		// Resume with skillDirectories - skill should now be active
		session2, err := client.ResumeSessionWithOptions(sessionID, &copilot.ResumeSessionConfig{
			SkillDirectories: []string{skillsDir},
		})
		if err != nil {
			t.Fatalf("Failed to resume session: %v", err)
		}

		if session2.SessionID != sessionID {
			t.Errorf("Expected session ID %s, got %s", sessionID, session2.SessionID)
		}

		// Now the skill should be applied
		message2, err := session2.SendAndWait(copilot.MessageOptions{Prompt: "Say hello again using the test skill."}, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		if message2.Data.Content == nil || !strings.Contains(*message2.Data.Content, skillMarker) {
			t.Errorf("Expected message to contain skill marker '%s' after resume, got: %v", skillMarker, message2.Data.Content)
		}

		session2.Destroy()
	})
}
