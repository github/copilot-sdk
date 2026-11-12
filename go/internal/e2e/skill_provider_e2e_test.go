package e2e

import (
	"context"
	"encoding/json"
	"errors"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

type providedSkill struct {
	descriptor rpc.SkillProviderDescriptor
	read       func() (string, error)
}

type testSkillProvider struct {
	mu     sync.Mutex
	skills []providedSkill
	calls  []string
}

func newTestSkillProvider(skills []providedSkill) *testSkillProvider {
	return &testSkillProvider{skills: skills}
}

func providedSkillWithMarkdown(name, description, markdown string) providedSkill {
	return providedSkill{
		descriptor: rpc.SkillProviderDescriptor{Name: name, Description: description},
		read:       func() (string, error) { return markdown, nil },
	}
}

func (p *testSkillProvider) ListSkills(ctx context.Context) ([]rpc.SkillProviderDescriptor, error) {
	p.mu.Lock()
	defer p.mu.Unlock()
	p.calls = append(p.calls, "list")
	skills := make([]rpc.SkillProviderDescriptor, 0, len(p.skills))
	for _, skill := range p.skills {
		skills = append(skills, skill.descriptor)
	}
	return skills, nil
}

func (p *testSkillProvider) ReadSkill(ctx context.Context, name string) (string, error) {
	p.mu.Lock()
	defer p.mu.Unlock()
	p.calls = append(p.calls, "read:"+name)
	for _, skill := range p.skills {
		if skill.descriptor.Name == name {
			return skill.read()
		}
	}
	return "", copilot.ErrSkillNotFound
}

func (p *testSkillProvider) Calls() []string {
	p.mu.Lock()
	defer p.mu.Unlock()
	calls := make([]string, len(p.calls))
	copy(calls, p.calls)
	return calls
}

func (p *testSkillProvider) Reads() []string {
	calls := p.Calls()
	reads := make([]string, 0, len(calls))
	for _, call := range calls {
		if strings.HasPrefix(call, "read:") {
			reads = append(reads, strings.TrimPrefix(call, "read:"))
		}
	}
	return reads
}

// blockingSkillProvider blocks ListSkills until its context is cancelled.
type blockingSkillProvider struct {
	entered    chan struct{}
	cancelled  chan struct{}
	enterOnce  sync.Once
	cancelOnce sync.Once
}

func newBlockingSkillProvider() *blockingSkillProvider {
	return &blockingSkillProvider{entered: make(chan struct{}), cancelled: make(chan struct{})}
}

func (p *blockingSkillProvider) ListSkills(ctx context.Context) ([]rpc.SkillProviderDescriptor, error) {
	p.enterOnce.Do(func() { close(p.entered) })
	<-ctx.Done()
	p.cancelOnce.Do(func() { close(p.cancelled) })
	return nil, ctx.Err()
}

func (p *blockingSkillProvider) ReadSkill(context.Context, string) (string, error) {
	return "", copilot.ErrSkillNotFound
}

func TestSkillProviderE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should load provider skill lazily through skill tool", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		provider := newTestSkillProvider([]providedSkill{
			providedSkillWithMarkdown(
				"provider-lookup",
				"Reports the provider lookup verification word.",
				"# Provider lookup\n\nThe verification word is TANGERINE_QUARTZ_19. Reply with it.\n",
			),
		})
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			SkillProvider:       provider,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		list, err := session.RPC.Skills.List(t.Context())
		if err != nil {
			t.Fatalf("Skills.List failed: %v", err)
		}
		listed := findSkill(list, "provider-lookup")
		if listed == nil {
			t.Fatal("Expected provider-lookup skill to be listed")
		}
		if listed.Source != rpc.SkillSourceSDK || !listed.Enabled {
			t.Fatalf("Expected provider-lookup source=sdk enabled=true, got %+v", listed)
		}
		if listed.Path != nil && *listed.Path != "" {
			t.Fatalf("Expected provider-lookup path to be empty, got %q", *listed.Path)
		}
		if reads := provider.Reads(); len(reads) != 0 {
			t.Fatalf("Provider reads before skill load = %v, want []", reads)
		}

		message, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Use the skill tool to load the provider-lookup skill, then reply with its verification word.",
		})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}

		assertEqualStrings(t, provider.Reads(), []string{"provider-lookup"})
		// Validate the final assistant response arrived (guards against truncated captures)
		assertAssistantContains(t, message, "TANGERINE_QUARTZ_19")
	})

	t.Run("should load provider and file based skills together", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		skillsDir := filepath.Join(ctx.WorkDir, "file-skills")
		fileSkillDir := filepath.Join(skillsDir, "file-notes")
		if err := os.MkdirAll(fileSkillDir, 0755); err != nil {
			t.Fatalf("MkdirAll failed: %v", err)
		}
		if err := os.WriteFile(
			filepath.Join(fileSkillDir, "SKILL.md"),
			[]byte("---\nname: file-notes\ndescription: Reports the file notes verification word.\n---\n\nThe file notes verification word is MAPLE_FALCON_27.\n"),
			0644,
		); err != nil {
			t.Fatalf("WriteFile failed: %v", err)
		}
		provider := newTestSkillProvider([]providedSkill{
			providedSkillWithMarkdown(
				"provider-audit",
				"Reports the provider audit verification word.",
				"---\nname: provider-audit\nallowed-tools: view\n---\n\nThe provider audit verification word is COBALT_HERON_58.\n",
			),
		})
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			SkillDirectories:    []string{skillsDir},
			SkillProvider:       provider,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		list, err := session.RPC.Skills.List(t.Context())
		if err != nil {
			t.Fatalf("Skills.List failed: %v", err)
		}
		fileSkill := findSkill(list, "file-notes")
		providerSkill := findSkill(list, "provider-audit")
		if fileSkill == nil || fileSkill.Source == rpc.SkillSourceSDK || fileSkill.Path == nil || *fileSkill.Path == "" {
			t.Fatalf("Unexpected file-notes skill: %+v", fileSkill)
		}
		if providerSkill == nil || providerSkill.Source != rpc.SkillSourceSDK {
			t.Fatalf("Unexpected provider-audit skill: %+v", providerSkill)
		}

		message, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Use the skill tool to load the file-notes skill and the provider-audit skill, then reply with both verification words.",
		})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}

		assertEqualStrings(t, provider.Reads(), []string{"provider-audit"})
		assertAssistantContains(t, message, "MAPLE_FALCON_27")
		// Validate the final assistant response arrived (guards against truncated captures)
		assertAssistantContains(t, message, "COBALT_HERON_58")
	})

	t.Run("should rebind skill provider on resume", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		original := newTestSkillProvider([]providedSkill{
			providedSkillWithMarkdown(
				"rebind-check",
				"Reports the rebind verification word.",
				"The rebind verification word is AMBER_ALPHA_11.\n",
			),
		})
		replacement := newTestSkillProvider([]providedSkill{
			providedSkillWithMarkdown(
				"rebind-check",
				"Reports the rebind verification word.",
				"The rebind verification word is BRONZE_BETA_22.\n",
			),
		})
		first, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			SkillProvider:       original,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		sessionID := first.SessionID
		if _, err := first.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Without using any tools or skills, reply with exactly REBIND_READY.",
		}); err != nil {
			t.Fatalf("Initial SendAndWait failed: %v", err)
		}
		if err := first.Disconnect(); err != nil {
			t.Fatalf("Disconnect failed: %v", err)
		}
		if reads := original.Reads(); len(reads) != 0 {
			t.Fatalf("Original provider reads before resume = %v, want []", reads)
		}
		originalCallsBeforeResume := len(original.Calls())

		session, err := client.ResumeSession(t.Context(), sessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			SkillProvider:       replacement,
		})
		if err != nil {
			t.Fatalf("ResumeSession failed: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		message, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Use the skill tool to load the rebind-check skill, then reply with its verification word.",
		})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}

		assertEqualStrings(t, replacement.Reads(), []string{"rebind-check"})
		if got := len(original.Calls()); got != originalCallsBeforeResume {
			t.Fatalf("Original provider call count after resume = %d, want %d", got, originalCallsBeforeResume)
		}
		// Validate the final assistant response arrived (guards against truncated captures)
		assertAssistantContains(t, message, "BRONZE_BETA_22")
		assertAssistantNotContains(t, message, "AMBER_ALPHA_11")
	})

	t.Run("should report provider read failure without leaking details", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		secret := "PROVIDER_SECRET_7F3A9C"
		provider := newTestSkillProvider([]providedSkill{
			{
				descriptor: rpc.SkillProviderDescriptor{
					Name:        "broken-lookup",
					Description: "Reports the broken lookup verification word.",
				},
				read: func() (string, error) {
					return "", errors.New("database unavailable: " + secret)
				},
			},
		})
		events := newEventRecorder()
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			SkillProvider:       provider,
			OnEvent:             events.Record,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		message, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Use the skill tool to load the broken-lookup skill. If loading fails, reply with exactly LOAD_FAILED.",
		})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}

		assertContainsString(t, provider.Reads(), "broken-lookup")
		failures := failedToolCompletions(events.Snapshot())
		if len(failures) != 1 {
			t.Fatalf("Expected exactly 1 failed tool completion, got %d", len(failures))
		}
		rawEvents, err := json.Marshal(events.Snapshot())
		if err != nil {
			t.Fatalf("Marshal events failed: %v", err)
		}
		if strings.Contains(string(rawEvents), secret) {
			t.Fatalf("Provider error leaked secret in events: %s", rawEvents)
		}
		// Validate the final assistant response arrived (guards against truncated captures)
		assertAssistantContains(t, message, "LOAD_FAILED")
	})

	t.Run("should report missing provider skill as not found", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		provider := newTestSkillProvider([]providedSkill{
			{
				descriptor: rpc.SkillProviderDescriptor{
					Name:        "vanished-lookup",
					Description: "Reports the vanished lookup verification word.",
				},
				read: func() (string, error) {
					return "", copilot.ErrSkillNotFound
				},
			},
		})
		events := newEventRecorder()
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			SkillProvider:       provider,
			OnEvent:             events.Record,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		message, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Use the skill tool to load the vanished-lookup skill. If loading fails, reply with exactly LOAD_FAILED.",
		})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}

		assertContainsString(t, provider.Reads(), "vanished-lookup")
		failures := failedToolCompletions(events.Snapshot())
		if len(failures) != 1 {
			t.Fatalf("Expected exactly 1 failed tool completion, got %d", len(failures))
		}
		rawFailure, err := json.Marshal(failures[0])
		if err != nil {
			t.Fatalf("Marshal failure failed: %v", err)
		}
		if !strings.Contains(strings.ToLower(string(rawFailure)), "not found") {
			t.Fatalf("Expected failure to mention not found, got %s", rawFailure)
		}
		// Validate the final assistant response arrived (guards against truncated captures)
		assertAssistantContains(t, message, "LOAD_FAILED")
	})

	t.Run("should keep provider dormant when skills disabled", func(t *testing.T) {
		ctx.ConfigureWithoutSnapshot(t)
		provider := newTestSkillProvider([]providedSkill{
			providedSkillWithMarkdown("dormant-lookup", "Never listed.", "Never read.\n"),
		})
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			EnableSkills:        copilot.Bool(false),
			SkillProvider:       provider,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		if _, err := session.RPC.Skills.EnsureLoaded(t.Context()); err != nil {
			t.Fatalf("Skills.EnsureLoaded failed: %v", err)
		}
		list, err := session.RPC.Skills.List(t.Context())
		if err != nil {
			t.Fatalf("Skills.List failed: %v", err)
		}
		if sdkSkills := sdkSkills(list); len(sdkSkills) != 0 {
			t.Fatalf("Expected no sdk skills when disabled, got %+v", sdkSkills)
		}
		if calls := provider.Calls(); len(calls) != 0 {
			t.Fatalf("Provider calls when skills disabled = %v, want []", calls)
		}
	})

	t.Run("should unbind provider when resumed without one", func(t *testing.T) {
		ctx.ConfigureWithoutSnapshot(t)
		provider := newTestSkillProvider([]providedSkill{
			providedSkillWithMarkdown("unbound-lookup", "Reports the unbound lookup word.", "Unbound.\n"),
		})
		first, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			SkillProvider:       provider,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		before, err := first.RPC.Skills.List(t.Context())
		if err != nil {
			t.Fatalf("Skills.List before resume failed: %v", err)
		}
		if findSkill(before, "unbound-lookup") == nil {
			t.Fatal("Expected unbound-lookup before resume")
		}
		callsBeforeResume := len(provider.Calls())

		session, err := client.ResumeSession(t.Context(), first.SessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("ResumeSession failed: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		if _, err := session.RPC.Skills.Reload(t.Context()); err != nil {
			t.Fatalf("Skills.Reload failed: %v", err)
		}
		list, err := session.RPC.Skills.List(t.Context())
		if err != nil {
			t.Fatalf("Skills.List after resume failed: %v", err)
		}
		if sdkSkills := sdkSkills(list); len(sdkSkills) != 0 {
			t.Fatalf("Expected no sdk skills after unbound resume, got %+v", sdkSkills)
		}
		if got := len(provider.Calls()); got != callsBeforeResume {
			t.Fatalf("Provider call count after unbound resume = %d, want %d", got, callsBeforeResume)
		}
	})

	t.Run("should cancel a blocked provider call when the session disconnects", func(t *testing.T) {
		ctx.ConfigureWithoutSnapshot(t)
		provider := newBlockingSkillProvider()
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			SkillProvider:       provider,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		// The list RPC fails once the binding is removed; only the provider's
		// cancellation matters here.
		go func() { _, _ = session.RPC.Skills.List(context.Background()) }()
		select {
		case <-provider.entered:
		case <-time.After(30 * time.Second):
			t.Fatal("provider ListSkills was not called")
		}

		if err := session.Disconnect(); err != nil {
			t.Fatalf("Disconnect failed: %v", err)
		}
		select {
		case <-provider.cancelled:
		case <-time.After(10 * time.Second):
			t.Fatal("provider context was not cancelled after Disconnect")
		}
	})

	t.Run("should reject skill provider for cloud sessions", func(t *testing.T) {
		ctx.ConfigureWithoutSnapshot(t)
		provider := newTestSkillProvider([]providedSkill{
			providedSkillWithMarkdown("cloud-lookup", "Never listed.", "Never read.\n"),
		})

		_, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Cloud:               &copilot.CloudSessionOptions{},
			SkillProvider:       provider,
		})
		if err == nil || err.Error() != "Skill providers are not supported for cloud sessions." {
			t.Fatalf("CreateSession error = %v, want cloud skill provider rejection", err)
		}
		if calls := provider.Calls(); len(calls) != 0 {
			t.Fatalf("Provider calls after cloud rejection = %v, want []", calls)
		}
	})
}

type eventRecorder struct {
	mu     sync.Mutex
	events []copilot.SessionEvent
}

func newEventRecorder() *eventRecorder {
	return &eventRecorder{}
}

func (r *eventRecorder) Record(event copilot.SessionEvent) {
	r.mu.Lock()
	defer r.mu.Unlock()
	r.events = append(r.events, event)
}

func (r *eventRecorder) Snapshot() []copilot.SessionEvent {
	r.mu.Lock()
	defer r.mu.Unlock()
	events := make([]copilot.SessionEvent, len(r.events))
	copy(events, r.events)
	return events
}

func findSkill(list *rpc.SkillList, name string) *rpc.Skill {
	if list == nil {
		return nil
	}
	for i := range list.Skills {
		if list.Skills[i].Name == name {
			return &list.Skills[i]
		}
	}
	return nil
}

func sdkSkills(list *rpc.SkillList) []rpc.Skill {
	if list == nil {
		return nil
	}
	var matches []rpc.Skill
	for _, skill := range list.Skills {
		if skill.Source == rpc.SkillSourceSDK {
			matches = append(matches, skill)
		}
	}
	return matches
}

func failedToolCompletions(events []copilot.SessionEvent) []*copilot.ToolExecutionCompleteData {
	var failures []*copilot.ToolExecutionCompleteData
	for _, event := range events {
		if data, ok := event.Data.(*copilot.ToolExecutionCompleteData); ok && !data.Success {
			failures = append(failures, data)
		}
	}
	return failures
}

func assertAssistantNotContains(t *testing.T, event *copilot.SessionEvent, text string) {
	t.Helper()
	data, ok := event.Data.(*copilot.AssistantMessageData)
	if !ok {
		t.Fatalf("Expected AssistantMessageData, got %T", event.Data)
	}
	if strings.Contains(data.Content, text) {
		t.Fatalf("Expected assistant response not to contain %q, got %q", text, data.Content)
	}
}

func assertEqualStrings(t *testing.T, got, want []string) {
	t.Helper()
	if len(got) != len(want) {
		t.Fatalf("strings = %v, want %v", got, want)
	}
	for i := range got {
		if got[i] != want[i] {
			t.Fatalf("strings = %v, want %v", got, want)
		}
	}
}

func assertContainsString(t *testing.T, got []string, want string) {
	t.Helper()
	for _, value := range got {
		if value == want {
			return
		}
	}
	t.Fatalf("strings = %v, want to contain %q", got, want)
}
