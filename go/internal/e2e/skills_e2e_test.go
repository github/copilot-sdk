package e2e

import (
	"os"
	"path/filepath"
	"strings"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
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

func TestSkillsE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should load and apply skill from skillDirectories", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		cleanSkillsDir(t, ctx.WorkDir)
		skillsDir := createTestSkillDir(t, ctx.WorkDir, skillMarker)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			SkillDirectories:    []string{skillsDir},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// The skill instructs the model to include a marker - verify it appears
		message, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Say hello briefly using the test skill.",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		if md, ok := message.Data.(*copilot.AssistantMessageData); !ok || !strings.Contains(md.Content, skillMarker) {
			t.Errorf("Expected message to contain skill marker '%s', got: %v", skillMarker, message.Data)
		}

		session.Disconnect()
	})

	t.Run("should not apply skill when disabled via disabledSkills", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		cleanSkillsDir(t, ctx.WorkDir)
		skillsDir := createTestSkillDir(t, ctx.WorkDir, skillMarker)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			SkillDirectories:    []string{skillsDir},
			DisabledSkills:      []string{"test-skill"},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// The skill is disabled, so the marker should NOT appear
		message, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Say hello briefly using the test skill.",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		if md, ok := message.Data.(*copilot.AssistantMessageData); ok && strings.Contains(md.Content, skillMarker) {
			t.Errorf("Expected message to NOT contain skill marker '%s' when disabled, got: %v", skillMarker, md.Content)
		}

		session.Disconnect()
	})

	t.Run("should allow agent with skills to invoke skill", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		cleanSkillsDir(t, ctx.WorkDir)
		skillsDir := createTestSkillDir(t, ctx.WorkDir, skillMarker)

		customAgents := []copilot.CustomAgentConfig{
			{
				Name:        "skill-agent",
				Description: "An agent with access to test-skill",
				Prompt:      "You are a helpful test agent.",
				Skills:      []string{"test-skill"},
			},
		}

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			SkillDirectories:    []string{skillsDir},
			CustomAgents:        customAgents,
			Agent:               "skill-agent",
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// The agent has Skills: ["test-skill"], so the skill content is preloaded into its context
		message, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Say hello briefly using the test skill.",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		if md, ok := message.Data.(*copilot.AssistantMessageData); !ok || !strings.Contains(md.Content, skillMarker) {
			t.Errorf("Expected message to contain skill marker '%s', got: %v", skillMarker, message.Data)
		}

		session.Disconnect()
	})

	t.Run("should not provide skills to agent without skills field", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		cleanSkillsDir(t, ctx.WorkDir)
		skillsDir := createTestSkillDir(t, ctx.WorkDir, skillMarker)

		customAgents := []copilot.CustomAgentConfig{
			{
				Name:        "no-skill-agent",
				Description: "An agent without skills access",
				Prompt:      "You are a helpful test agent.",
			},
		}

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			SkillDirectories:    []string{skillsDir},
			CustomAgents:        customAgents,
			Agent:               "no-skill-agent",
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// The agent has no Skills field, so no skill content is injected
		message, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Say hello briefly using the test skill.",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		if md, ok := message.Data.(*copilot.AssistantMessageData); ok && strings.Contains(md.Content, skillMarker) {
			t.Errorf("Expected message to NOT contain skill marker '%s' when agent has no skills, got: %v", skillMarker, md.Content)
		}

		session.Disconnect()
	})

	t.Run("should apply skill on session resume with skillDirectories", func(t *testing.T) {
		t.Skip("See the big comment around the equivalent test in the Node SDK. Skipped because the feature doesn't work correctly yet.")
		ctx.ConfigureForTest(t)
		cleanSkillsDir(t, ctx.WorkDir)
		skillsDir := createTestSkillDir(t, ctx.WorkDir, skillMarker)

		// Create a session without skills first
		session1, err := client.CreateSession(t.Context(), &copilot.SessionConfig{OnPermissionRequest: copilot.PermissionHandler.ApproveAll})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		sessionID := session1.SessionID

		// First message without skill - marker should not appear
		message1, err := session1.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Say hi."})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		if md, ok := message1.Data.(*copilot.AssistantMessageData); ok && strings.Contains(md.Content, skillMarker) {
			t.Errorf("Expected message to NOT contain skill marker before skill was added, got: %v", md.Content)
		}

		// Resume with skillDirectories - skill should now be active
		session2, err := client.ResumeSessionWithOptions(t.Context(), sessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			SkillDirectories:    []string{skillsDir},
		})
		if err != nil {
			t.Fatalf("Failed to resume session: %v", err)
		}

		if session2.SessionID != sessionID {
			t.Errorf("Expected session ID %s, got %s", sessionID, session2.SessionID)
		}

		// Now the skill should be applied
		message2, err := session2.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Say hello again using the test skill."})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		if md, ok := message2.Data.(*copilot.AssistantMessageData); !ok || !strings.Contains(md.Content, skillMarker) {
			t.Errorf("Expected message to contain skill marker '%s' after resume, got: %v", skillMarker, message2.Data)
		}

		session2.Disconnect()
	})

	t.Run("should control ambient project skills with enableConfigDiscovery", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		projectDir := filepath.Join(ctx.WorkDir, "config-discovery-"+randomHex(t))
		projectSkillsDir := filepath.Join(projectDir, ".github", "skills")
		if err := os.MkdirAll(projectSkillsDir, 0o755); err != nil {
			t.Fatalf("MkdirAll failed: %v", err)
		}
		skillName := "ambient-skill-" + randomHex(t)
		skillSubdir := filepath.Join(projectSkillsDir, skillName)
		if err := os.MkdirAll(skillSubdir, 0o755); err != nil {
			t.Fatalf("MkdirAll (skillSubdir) failed: %v", err)
		}
		skillContent := "---\nname: " + skillName + "\ndescription: A project skill discovered from .github/skills\n---\n\n" +
			"# " + skillName + "\n\nUse the exact phrase AMBIENT_DISCOVERY_SKILL when this skill is active.\n"
		if err := os.WriteFile(filepath.Join(skillSubdir, "SKILL.md"), []byte(skillContent), 0o644); err != nil {
			t.Fatalf("WriteFile (SKILL.md) failed: %v", err)
		}

		// Discovery disabled: ambient project skill should NOT appear in Skills.List.
		disabledSession, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest:   copilot.PermissionHandler.ApproveAll,
			WorkingDirectory:      projectDir,
			EnableConfigDiscovery: false,
		})
		if err != nil {
			t.Fatalf("CreateSession (disabled) failed: %v", err)
		}
		disabledList, err := disabledSession.RPC.Skills.List(t.Context())
		if err != nil {
			t.Fatalf("Skills.List (disabled) failed: %v", err)
		}
		for _, skill := range disabledList.Skills {
			if skill.Name == skillName {
				t.Errorf("Did not expect skill %q to be discovered when EnableConfigDiscovery=false", skillName)
			}
		}
		_ = disabledSession.Disconnect()

		// Discovery enabled: ambient project skill should appear with Source=project.
		enabledSession, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest:   copilot.PermissionHandler.ApproveAll,
			WorkingDirectory:      projectDir,
			EnableConfigDiscovery: true,
		})
		if err != nil {
			t.Fatalf("CreateSession (enabled) failed: %v", err)
		}
		t.Cleanup(func() { _ = enabledSession.Disconnect() })

		enabledList, err := enabledSession.RPC.Skills.List(t.Context())
		if err != nil {
			t.Fatalf("Skills.List (enabled) failed: %v", err)
		}
		var discovered *rpc.Skill
		for i, skill := range enabledList.Skills {
			if skill.Name == skillName {
				discovered = &enabledList.Skills[i]
				break
			}
		}
		if discovered == nil {
			t.Fatalf("Expected to discover skill %q via EnableConfigDiscovery", skillName)
		}
		if !discovered.Enabled {
			t.Error("Expected discovered skill to be Enabled=true")
		}
		if discovered.Source != "project" {
			t.Errorf("Expected Source='project', got %q", discovered.Source)
		}
		expectedSuffix := filepath.Join(skillName, "SKILL.md")
		if discovered.Path == nil || !strings.HasSuffix(filepath.ToSlash(*discovered.Path), filepath.ToSlash(expectedSuffix)) {
			t.Errorf("Expected Path to end with %q, got %v", expectedSuffix, discovered.Path)
		}
	})
}
