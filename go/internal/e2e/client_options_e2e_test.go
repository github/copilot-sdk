package e2e

import (
	"encoding/json"
	"net"
	"os"
	"path/filepath"
	"strings"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

// Mirrors the E2E portions of dotnet/test/ClientOptionsTests.cs (snapshot category "client_options").
// .NET-only tests that exercise validation on the options struct alone are skipped here because
// Go's ClientOptions is a plain struct with no setter validation; equivalent behavior is covered
// in package-level unit tests.
func TestClientOptionsE2E(t *testing.T) {
	t.Run("should listen on configured TCP port", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		port := getAvailableTCPPort(t)

		client := ctx.NewClient(func(opts *copilot.ClientOptions) {
			opts.Connection = copilot.TCPConnection{Path: ctx.CLIPath, Port: port}
		})
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Start failed: %v", err)
		}
		if got := client.RuntimePort(); got != port {
			t.Errorf("Expected RuntimePort=%d, got %d", port, got)
		}

		// Ping over the connection to confirm it is usable.
		pingResp, err := client.Ping(t.Context(), "fixed-port")
		if err != nil {
			t.Fatalf("Ping failed: %v", err)
		}
		if !strings.Contains(pingResp.Message, "fixed-port") {
			t.Errorf("Expected ping response to echo 'fixed-port', got %q", pingResp.Message)
		}
	})

	t.Run("should use client cwd for default workingdirectory", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		ctx.ConfigureForTest(t)

		clientCwd := filepath.Join(ctx.WorkDir, "client-cwd")
		if err := os.MkdirAll(clientCwd, 0755); err != nil {
			t.Fatalf("Failed to create clientCwd: %v", err)
		}
		if err := os.WriteFile(filepath.Join(clientCwd, "marker.txt"), []byte("I am in the client cwd"), 0644); err != nil {
			t.Fatalf("Failed to write marker file: %v", err)
		}

		client := ctx.NewClient(func(opts *copilot.ClientOptions) {
			opts.WorkingDirectory = clientCwd
		})
		t.Cleanup(func() { client.ForceStop() })

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		t.Cleanup(func() { session.Disconnect() })

		evt, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Read the file marker.txt and tell me what it says",
		})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}
		assistant, ok := evt.Data.(*copilot.AssistantMessageData)
		if !ok {
			t.Fatalf("Expected AssistantMessageData, got %T", evt.Data)
		}
		if !strings.Contains(assistant.Content, "client cwd") {
			t.Errorf("Expected assistant message to contain 'client cwd', got %q", assistant.Content)
		}
	})

	t.Run("should propagate process options to spawned cli", func(t *testing.T) {
		// Mirrors: Should_Propagate_Process_Options_To_Spawned_Cli
		// Spawns a fake stdio CLI (a Node.js script) so we can assert that the
		// SDK passes the right argv / env / cwd / RPC params through to the
		// subprocess.
		ctx := testharness.NewTestContext(t)

		cliPath := filepath.Join(ctx.WorkDir, "fake-cli-"+randomHex(t)+".js")
		capturePath := filepath.Join(ctx.WorkDir, "fake-cli-capture-"+randomHex(t)+".json")
		telemetryPath := filepath.Join(ctx.WorkDir, "telemetry.jsonl")
		if err := os.WriteFile(cliPath, []byte(fakeStdioCliScript), 0644); err != nil {
			t.Fatalf("Failed to write fake CLI script: %v", err)
		}

		client := ctx.NewClient(func(opts *copilot.ClientOptions) {
			opts.Connection = copilot.StdioConnection{
				Path: cliPath,
				Args: []string{"--capture-file", capturePath},
			}
			opts.BaseDirectory = filepath.Join(ctx.WorkDir, "copilot-home-from-option")
			opts.Env = append([]string{}, opts.Env...)
			opts.Env = append(opts.Env, "COPILOT_HOME="+filepath.Join(ctx.WorkDir, "copilot-home-from-env"))
			opts.GitHubToken = "process-option-token"
			opts.LogLevel = "debug"
			opts.SessionIdleTimeoutSeconds = 17
			opts.Telemetry = &copilot.TelemetryConfig{
				OTLPEndpoint:   "http://127.0.0.1:4318",
				OTLPProtocol:   "http/protobuf",
				FilePath:       telemetryPath,
				ExporterType:   "file",
				SourceName:     "go-sdk-e2e",
				CaptureContent: copilot.Bool(true),
			}
			opts.UseLoggedInUser = copilot.Bool(false)
		})
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Start failed: %v", err)
		}

		capture := readCapture(t, capturePath)
		args := capture.Args

		assertArgValue(t, args, "--log-level", "debug")
		if !containsStringE(args, "--stdio") {
			t.Errorf("Expected --stdio in args, got %v", args)
		}
		assertArgValue(t, args, "--auth-token-env", "COPILOT_SDK_AUTH_TOKEN")
		if !containsStringE(args, "--no-auto-login") {
			t.Errorf("Expected --no-auto-login in args, got %v", args)
		}
		assertArgValue(t, args, "--session-idle-timeout", "17")

		expectedCwd, _ := filepath.Abs(ctx.WorkDir)
		actualCwd, _ := filepath.Abs(capture.WorkingDirectory)
		if expectedCwd != actualCwd {
			t.Errorf("Expected cwd=%q, got %q", expectedCwd, actualCwd)
		}

		expectEnv := map[string]string{
			"COPILOT_HOME":                                       filepath.Join(ctx.WorkDir, "copilot-home-from-option"),
			"COPILOT_SDK_AUTH_TOKEN":                             "process-option-token",
			"COPILOT_OTEL_ENABLED":                               "true",
			"OTEL_EXPORTER_OTLP_ENDPOINT":                        "http://127.0.0.1:4318",
			"OTEL_EXPORTER_OTLP_PROTOCOL":                        "http/protobuf",
			"COPILOT_OTEL_FILE_EXPORTER_PATH":                    telemetryPath,
			"COPILOT_OTEL_EXPORTER_TYPE":                         "file",
			"COPILOT_OTEL_SOURCE_NAME":                           "go-sdk-e2e",
			"OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT": "true",
		}
		for k, v := range expectEnv {
			if got := capture.Env[k]; got != v {
				t.Errorf("Expected env[%s]=%q, got %q", k, v, got)
			}
		}

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			EnableConfigDiscovery:              copilot.Bool(true),
			EnableOnDemandInstructionDiscovery: copilot.Bool(true),
			IncludeSubAgentStreamingEvents:     copilot.Bool(false),
			OnPermissionRequest:                copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		t.Cleanup(func() { session.Disconnect() })

		updated := readCapture(t, capturePath)
		var createReq *capturedRequest
		for i := range updated.Requests {
			if updated.Requests[i].Method == "session.create" {
				createReq = &updated.Requests[i]
				break
			}
		}
		if createReq == nil {
			t.Fatalf("session.create request was not captured. Captured requests: %+v", updated.Requests)
			return
		}
		params, ok := createReq.Params.(map[string]any)
		if !ok {
			t.Fatalf("Expected session.create params to be an object, got %T", createReq.Params)
		}
		if v, ok := params["enableConfigDiscovery"].(bool); !ok || v != true {
			t.Errorf("Expected session.create.params.enableConfigDiscovery=true, got %v", params["enableConfigDiscovery"])
		}
		if v, ok := params["enableOnDemandInstructionDiscovery"].(bool); !ok || v != true {
			t.Errorf("Expected session.create.params.enableOnDemandInstructionDiscovery=true, got %v", params["enableOnDemandInstructionDiscovery"])
		}
		if v, ok := params["includeSubAgentStreamingEvents"].(bool); !ok || v != false {
			t.Errorf("Expected session.create.params.includeSubAgentStreamingEvents=false, got %v", params["includeSubAgentStreamingEvents"])
		}
	})

	t.Run("should forward advanced session creation options to the CLI", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		cliPath := filepath.Join(ctx.WorkDir, "fake-cli-"+randomHex(t)+".js")
		capturePath := filepath.Join(ctx.WorkDir, "fake-cli-capture-"+randomHex(t)+".json")
		if err := os.WriteFile(cliPath, []byte(fakeStdioCliScript), 0644); err != nil {
			t.Fatalf("Failed to write fake CLI script: %v", err)
		}

		client := ctx.NewClient(func(opts *copilot.ClientOptions) {
			opts.Connection = copilot.StdioConnection{Path: cliPath, Args: []string{"--capture-file", capturePath}}
			opts.GitHubToken = "advanced-create-client-token"
			opts.UseLoggedInUser = copilot.Bool(false)
		})
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Start failed: %v", err)
		}

		sessionID := "advanced-session-id"
		workingDirectory := t.TempDir()
		configDirectory := t.TempDir()
		embeddingCacheStorage := "in-memory"
		organizationCustomInstructions := "organization guidance"
		maxAiCredits := float64(42)
		extensionSDKPath := filepath.Join(ctx.WorkDir, "extension-sdk")
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			SessionID:                          sessionID,
			ClientName:                         "go-sdk-e2e-client",
			Model:                              "claude-sonnet-4.5",
			ReasoningEffort:                    "low",
			ReasoningSummary:                   copilot.ReasoningSummaryNone,
			ContextTier:                        copilot.ContextTierLongContext,
			ConfigDirectory:                    configDirectory,
			EnableConfigDiscovery:              copilot.Bool(true),
			SkipEmbeddingRetrieval:             copilot.Bool(true),
			EmbeddingCacheStorage:              &embeddingCacheStorage,
			OrganizationCustomInstructions:     &organizationCustomInstructions,
			EnableOnDemandInstructionDiscovery: copilot.Bool(true),
			EnableFileHooks:                    copilot.Bool(false),
			EnableHostGitOperations:            copilot.Bool(false),
			EnableSessionStore:                 copilot.Bool(false),
			EnableSkills:                       copilot.Bool(false),
			WorkingDirectory:                   workingDirectory,
			Streaming:                          copilot.Bool(true),
			IncludeSubAgentStreamingEvents:     copilot.Bool(false),
			AvailableTools:                     []string{"read_file"},
			ExcludedTools:                      []string{"bash"},
			ExcludedBuiltInAgents:              []string{"legacy-agent"},
			EnableSessionTelemetry:             copilot.Bool(false),
			EnableCitations:                    copilot.Bool(true),
			SessionLimits:                      &rpc.SessionLimitsConfig{MaxAiCredits: &maxAiCredits},
			SkipCustomInstructions:             copilot.Bool(true),
			CustomAgentsLocalOnly:              copilot.Bool(true),
			CoauthorEnabled:                    copilot.Bool(false),
			ManageScheduleEnabled:              copilot.Bool(false),
			GitHubToken:                        "advanced-create-session-token",
			RemoteSession:                      rpc.RemoteSessionModeExport,
			SkillDirectories:                   []string{"skills"},
			PluginDirectories:                  []string{"plugins"},
			InstructionDirectories:             []string{"instructions"},
			DisabledSkills:                     []string{"disabled-skill"},
			EnableMCPApps:                      true,
			Canvases: []copilot.CanvasDeclaration{{
				ID:          "canvas",
				DisplayName: "Canvas",
				Description: "Canvas description",
				InputSchema: map[string]any{"type": "object"},
			}},
			RequestCanvasRenderer: copilot.Bool(true),
			RequestExtensions:     copilot.Bool(true),
			ExtensionSDKPath:      &extensionSDKPath,
			ExtensionInfo:         &copilot.ExtensionInfo{Source: "github-app", Name: "go-e2e-extension"},
			ExpAssignments:        &copilot.CopilotExpAssignmentResponse{Flights: map[string]string{"feature": "enabled"}, AssignmentContext: "ctx"},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		session.Disconnect()

		createReq := getCapturedRequest(t, capturePath, "session.create")
		params, ok := createReq.Params.(map[string]any)
		if !ok {
			t.Fatalf("Expected session.create params object, got %T", createReq.Params)
		}
		expectedValues := map[string]any{
			"sessionId":                          sessionID,
			"clientName":                         "go-sdk-e2e-client",
			"model":                              "claude-sonnet-4.5",
			"reasoningEffort":                    "low",
			"reasoningSummary":                   "none",
			"contextTier":                        "long_context",
			"configDir":                          configDirectory,
			"enableConfigDiscovery":              true,
			"skipEmbeddingRetrieval":             true,
			"embeddingCacheStorage":              embeddingCacheStorage,
			"organizationCustomInstructions":     organizationCustomInstructions,
			"enableOnDemandInstructionDiscovery": true,
			"enableFileHooks":                    false,
			"enableHostGitOperations":            false,
			"enableSessionStore":                 false,
			"enableSkills":                       false,
			"workingDirectory":                   workingDirectory,
			"streaming":                          true,
			"includeSubAgentStreamingEvents":     false,
			"enableSessionTelemetry":             false,
			"enableCitations":                    true,
			"skipCustomInstructions":             true,
			"customAgentsLocalOnly":              true,
			"coauthorEnabled":                    false,
			"manageScheduleEnabled":              false,
			"gitHubToken":                        "advanced-create-session-token",
			"remoteSession":                      "export",
			"requestMcpApps":                     true,
			"requestCanvasRenderer":              true,
			"requestExtensions":                  true,
			"extensionSdkPath":                   extensionSDKPath,
			"envValueMode":                       "direct",
		}
		for key, expected := range expectedValues {
			if params[key] != expected {
				t.Fatalf("Expected %s=%#v, got %#v in %#v", key, expected, params[key], params)
			}
		}
		assertStringArray(t, params["availableTools"], []string{"read_file"})
		assertStringArray(t, params["excludedTools"], []string{"bash"})
		assertStringArray(t, params["excludedBuiltinAgents"], []string{"legacy-agent"})
		assertStringArray(t, params["skillDirectories"], []string{"skills"})
		assertStringArray(t, params["pluginDirectories"], []string{"plugins"})
		assertStringArray(t, params["instructionDirectories"], []string{"instructions"})
		assertStringArray(t, params["disabledSkills"], []string{"disabled-skill"})
		if params["sessionLimits"].(map[string]any)["maxAiCredits"] != maxAiCredits {
			t.Fatalf("Expected sessionLimits to be forwarded, got %#v", params["sessionLimits"])
		}
		extensionInfo := params["extensionInfo"].(map[string]any)
		if extensionInfo["source"] != "github-app" || extensionInfo["name"] != "go-e2e-extension" {
			t.Fatalf("Expected extensionInfo to be forwarded, got %#v", extensionInfo)
		}
		canvases := params["canvases"].([]any)
		canvas := canvases[0].(map[string]any)
		if canvas["id"] != "canvas" || canvas["displayName"] != "Canvas" || canvas["description"] != "Canvas description" {
			t.Fatalf("Expected canvas declaration to be forwarded, got %#v", canvas)
		}
		if params["expAssignments"].(map[string]any)["Flights"].(map[string]any)["feature"] != "enabled" {
			t.Fatalf("Expected expAssignments to be forwarded, got %#v", params["expAssignments"])
		}
	})

	t.Run("should forward singular provider configuration on session creation", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		cliPath := filepath.Join(ctx.WorkDir, "fake-cli-"+randomHex(t)+".js")
		capturePath := filepath.Join(ctx.WorkDir, "fake-cli-capture-"+randomHex(t)+".json")
		if err := os.WriteFile(cliPath, []byte(fakeStdioCliScript), 0644); err != nil {
			t.Fatalf("Failed to write fake CLI script: %v", err)
		}

		client := ctx.NewClient(func(opts *copilot.ClientOptions) {
			opts.Connection = copilot.StdioConnection{Path: cliPath, Args: []string{"--capture-file", capturePath}}
			opts.GitHubToken = "provider-client-token"
			opts.UseLoggedInUser = copilot.Bool(false)
		})
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Start failed: %v", err)
		}

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			Provider: &copilot.ProviderConfig{
				Type:            "openai",
				WireAPI:         "responses",
				Transport:       "websockets",
				BaseURL:         "https://models.example.test/v1",
				APIKey:          "provider-key",
				ModelID:         "base-model",
				WireModel:       "wire-model",
				MaxPromptTokens: 1000,
				MaxOutputTokens: 2000,
				Headers:         map[string]string{"x-provider": "go"},
			},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		session.Disconnect()

		createReq := getCapturedRequest(t, capturePath, "session.create")
		params := createReq.Params.(map[string]any)
		provider := params["provider"].(map[string]any)
		for key, expected := range map[string]any{
			"type":            "openai",
			"wireApi":         "responses",
			"transport":       "websockets",
			"baseUrl":         "https://models.example.test/v1",
			"apiKey":          "provider-key",
			"modelId":         "base-model",
			"wireModel":       "wire-model",
			"maxPromptTokens": float64(1000),
			"maxOutputTokens": float64(2000),
		} {
			if provider[key] != expected {
				t.Fatalf("Expected provider.%s=%#v, got %#v in %#v", key, expected, provider[key], provider)
			}
		}
		if provider["headers"].(map[string]any)["x-provider"] != "go" {
			t.Fatalf("Expected provider headers to be forwarded, got %#v", provider["headers"])
		}
	})

	t.Run("should forward advanced session resume options to the CLI", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		cliPath := filepath.Join(ctx.WorkDir, "fake-cli-"+randomHex(t)+".js")
		capturePath := filepath.Join(ctx.WorkDir, "fake-cli-capture-"+randomHex(t)+".json")
		if err := os.WriteFile(cliPath, []byte(fakeStdioCliScript), 0644); err != nil {
			t.Fatalf("Failed to write fake CLI script: %v", err)
		}

		client := ctx.NewClient(func(opts *copilot.ClientOptions) {
			opts.Connection = copilot.StdioConnection{Path: cliPath, Args: []string{"--capture-file", capturePath}}
			opts.GitHubToken = "advanced-resume-client-token"
			opts.UseLoggedInUser = copilot.Bool(false)
		})
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Start failed: %v", err)
		}

		workingDirectory := t.TempDir()
		configDirectory := t.TempDir()
		continuePendingWork := false
		extensionSDKPath := filepath.Join(ctx.WorkDir, "resume-extension-sdk")
		session, err := client.ResumeSession(t.Context(), "resume-session-id", &copilot.ResumeSessionConfig{
			Model:                          "gpt-5-mini",
			ReasoningEffort:                "low",
			ReasoningSummary:               copilot.ReasoningSummaryNone,
			ContextTier:                    copilot.ContextTierLongContext,
			WorkingDirectory:               workingDirectory,
			ConfigDirectory:                configDirectory,
			EnableConfigDiscovery:          copilot.Bool(false),
			SuppressResumeEvent:            true,
			ContinuePendingWork:            &continuePendingWork,
			Streaming:                      copilot.Bool(true),
			IncludeSubAgentStreamingEvents: copilot.Bool(false),
			GitHubToken:                    "advanced-resume-session-token",
			Canvases: []copilot.CanvasDeclaration{{
				ID:          "resume-canvas",
				DisplayName: "Resume Canvas",
				Description: "Resume canvas description",
				InputSchema: map[string]any{"type": "object"},
			}},
			OpenCanvases: []rpc.OpenCanvasInstance{{
				CanvasID:    "resume-canvas",
				ExtensionID: "github-app/go-e2e-extension",
				InstanceID:  "resume-instance",
				Input:       map[string]any{"value": "from-resume"},
			}},
			RequestCanvasRenderer: copilot.Bool(true),
			RequestExtensions:     copilot.Bool(true),
			ExtensionSDKPath:      &extensionSDKPath,
			ExtensionInfo:         &copilot.ExtensionInfo{Source: "github-app", Name: "go-e2e-extension"},
			ExpAssignments:        &copilot.CopilotExpAssignmentResponse{Flights: map[string]string{"resumeFeature": "enabled"}, AssignmentContext: "ctx"},
		})
		if err != nil {
			t.Fatalf("ResumeSession failed: %v", err)
		}
		session.Disconnect()

		resumeReq := getCapturedRequest(t, capturePath, "session.resume")
		params := resumeReq.Params.(map[string]any)
		expectedValues := map[string]any{
			"sessionId":                      "resume-session-id",
			"model":                          "gpt-5-mini",
			"reasoningEffort":                "low",
			"reasoningSummary":               "none",
			"contextTier":                    "long_context",
			"workingDirectory":               workingDirectory,
			"configDir":                      configDirectory,
			"enableConfigDiscovery":          false,
			"disableResume":                  true,
			"continuePendingWork":            false,
			"streaming":                      true,
			"includeSubAgentStreamingEvents": false,
			"gitHubToken":                    "advanced-resume-session-token",
			"requestCanvasRenderer":          true,
			"requestExtensions":              true,
			"extensionSdkPath":               extensionSDKPath,
			"envValueMode":                   "direct",
		}
		for key, expected := range expectedValues {
			if params[key] != expected {
				t.Fatalf("Expected resume %s=%#v, got %#v in %#v", key, expected, params[key], params)
			}
		}
		openCanvases := params["openCanvases"].([]any)
		openCanvas := openCanvases[0].(map[string]any)
		if openCanvas["canvasId"] != "resume-canvas" || openCanvas["extensionId"] != "github-app/go-e2e-extension" ||
			openCanvas["instanceId"] != "resume-instance" {
			t.Fatalf("Expected open canvas state to be forwarded, got %#v", openCanvas)
		}
		extensionInfo := params["extensionInfo"].(map[string]any)
		if extensionInfo["source"] != "github-app" || extensionInfo["name"] != "go-e2e-extension" {
			t.Fatalf("Expected extensionInfo on resume, got %#v", extensionInfo)
		}
		if params["expAssignments"].(map[string]any)["Flights"].(map[string]any)["resumeFeature"] != "enabled" {
			t.Fatalf("Expected resume expAssignments to be forwarded, got %#v", params["expAssignments"])
		}
	})

}

// ---------------------------------------------------------------------------
// Unit-style tests mirroring the property-only tests in
// dotnet/test/ClientOptionsTests.cs.
// ---------------------------------------------------------------------------

func TestClientOptionsUnit(t *testing.T) {
	t.Run("should accept GitHubToken option", func(t *testing.T) {
		// Mirrors: Should_Accept_GitHubToken_Option
		opts := copilot.ClientOptions{GitHubToken: "gho_test_token"}
		if opts.GitHubToken != "gho_test_token" {
			t.Errorf("Expected GitHubToken=%q, got %q", "gho_test_token", opts.GitHubToken)
		}
	})

	t.Run("should default UseLoggedInUser to nil", func(t *testing.T) {
		// Mirrors: Should_Default_UseLoggedInUser_To_Null
		opts := copilot.ClientOptions{}
		if opts.UseLoggedInUser != nil {
			t.Errorf("Expected UseLoggedInUser to be nil by default, got %v", opts.UseLoggedInUser)
		}
	})

	t.Run("should allow explicit UseLoggedInUser false", func(t *testing.T) {
		// Mirrors: Should_Allow_Explicit_UseLoggedInUser_False
		opts := copilot.ClientOptions{UseLoggedInUser: copilot.Bool(false)}
		if opts.UseLoggedInUser == nil || *opts.UseLoggedInUser != false {
			t.Errorf("Expected UseLoggedInUser=false, got %v", opts.UseLoggedInUser)
		}
	})

	t.Run("should allow explicit UseLoggedInUser true with GitHubToken", func(t *testing.T) {
		// Mirrors: Should_Allow_Explicit_UseLoggedInUser_True_With_GitHubToken
		opts := copilot.ClientOptions{
			GitHubToken:     "gho_test_token",
			UseLoggedInUser: copilot.Bool(true),
		}
		if opts.UseLoggedInUser == nil || *opts.UseLoggedInUser != true {
			t.Errorf("Expected UseLoggedInUser=true, got %v", opts.UseLoggedInUser)
		}
		if opts.GitHubToken != "gho_test_token" {
			t.Errorf("Expected GitHubToken=%q, got %q", "gho_test_token", opts.GitHubToken)
		}
	})

	t.Run("should panic when GitHubToken used with URIConnection", func(t *testing.T) {
		assertPanics(t, func() {
			_ = copilot.NewClient(&copilot.ClientOptions{
				Connection:  copilot.URIConnection{URL: "localhost:8080"},
				GitHubToken: "gho_test_token",
			})
		})
	})

	t.Run("should panic when UseLoggedInUser used with URIConnection", func(t *testing.T) {
		assertPanics(t, func() {
			_ = copilot.NewClient(&copilot.ClientOptions{
				Connection:      copilot.URIConnection{URL: "localhost:8080"},
				UseLoggedInUser: copilot.Bool(false),
			})
		})
	})

	t.Run("should default SessionIdleTimeoutSeconds to zero", func(t *testing.T) {
		// Mirrors: Should_Default_SessionIdleTimeoutSeconds_To_Null
		// Go uses int (no nullable wrapper); the zero value is 0 and is
		// treated as "unset" by the SDK (no --session-idle-timeout flag).
		opts := copilot.ClientOptions{}
		if opts.SessionIdleTimeoutSeconds != 0 {
			t.Errorf("Expected SessionIdleTimeoutSeconds=0 by default, got %d", opts.SessionIdleTimeoutSeconds)
		}
	})

	t.Run("should accept SessionIdleTimeoutSeconds option", func(t *testing.T) {
		// Mirrors: Should_Accept_SessionIdleTimeoutSeconds_Option
		opts := copilot.ClientOptions{SessionIdleTimeoutSeconds: 600}
		if opts.SessionIdleTimeoutSeconds != 600 {
			t.Errorf("Expected SessionIdleTimeoutSeconds=600, got %d", opts.SessionIdleTimeoutSeconds)
		}
	})
}

func getAvailableTCPPort(t *testing.T) int {
	t.Helper()
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatalf("Failed to listen on a free TCP port: %v", err)
	}
	defer listener.Close()
	return listener.Addr().(*net.TCPAddr).Port
}

func assertPanics(t *testing.T, fn func()) {
	t.Helper()
	defer func() {
		if r := recover(); r == nil {
			t.Error("Expected the function to panic, but it did not")
		}
	}()
	fn()
}

func containsStringE(slice []string, s string) bool {
	for _, v := range slice {
		if v == s {
			return true
		}
	}
	return false
}

func assertArgValue(t *testing.T, args []string, name, expected string) {
	t.Helper()
	for i, v := range args {
		if v == name {
			if i+1 >= len(args) {
				t.Errorf("Argument %q is missing a value. Args: %v", name, args)
				return
			}
			if args[i+1] != expected {
				t.Errorf("Expected argument %q to have value %q, got %q. Args: %v", name, expected, args[i+1], args)
			}
			return
		}
	}
	t.Errorf("Argument %q was not present. Args: %v", name, args)
}

// capturedCli mirrors the JSON file written by the fake stdio CLI script.
type capturedCli struct {
	Args             []string          `json:"args"`
	WorkingDirectory string            `json:"cwd"`
	Requests         []capturedRequest `json:"requests"`
	Env              map[string]string `json:"env"`
}

type capturedRequest struct {
	Method string `json:"method"`
	Params any    `json:"params"`
}

func readCapture(t *testing.T, path string) capturedCli {
	t.Helper()
	data, err := os.ReadFile(path)
	if err != nil {
		t.Fatalf("Failed to read capture file %q: %v", path, err)
	}
	var c capturedCli
	if err := json.Unmarshal(data, &c); err != nil {
		t.Fatalf("Failed to parse capture file %q: %v\nContent: %s", path, err, string(data))
	}
	return c
}

func getCapturedRequest(t *testing.T, path, method string) capturedRequest {
	t.Helper()
	capture := readCapture(t, path)
	for _, request := range capture.Requests {
		if request.Method == method {
			return request
		}
	}
	t.Fatalf("Expected %s request in capture, got %+v", method, capture.Requests)
	return capturedRequest{}
}

func assertStringArray(t *testing.T, value any, expected []string) {
	t.Helper()
	items, ok := value.([]any)
	if !ok {
		t.Fatalf("Expected string array %v, got %#v", expected, value)
	}
	if len(items) != len(expected) {
		t.Fatalf("Expected string array %v, got %#v", expected, items)
	}
	for i, expectedValue := range expected {
		if items[i] != expectedValue {
			t.Fatalf("Expected string array %v, got %#v", expected, items)
		}
	}
}

// fakeStdioCliScript is intentionally kept close to the fake CLIs used by the
// other SDK client-options E2E tests, while still matching Go's request capture shape.
const fakeStdioCliScript = `
const fs = require("fs");

const captureIndex = process.argv.indexOf("--capture-file");
const captureFile = captureIndex >= 0 ? process.argv[captureIndex + 1] : undefined;
const requests = [];

function saveCapture() {
  if (!captureFile) {
    return;
  }
  fs.writeFileSync(captureFile, JSON.stringify({
    args: process.argv.slice(2),
    cwd: process.cwd(),
    requests,
    env: {
      COPILOT_HOME: process.env.COPILOT_HOME,
      COPILOT_SDK_AUTH_TOKEN: process.env.COPILOT_SDK_AUTH_TOKEN,
      COPILOT_OTEL_ENABLED: process.env.COPILOT_OTEL_ENABLED,
      OTEL_EXPORTER_OTLP_ENDPOINT: process.env.OTEL_EXPORTER_OTLP_ENDPOINT,
      OTEL_EXPORTER_OTLP_PROTOCOL: process.env.OTEL_EXPORTER_OTLP_PROTOCOL,
      COPILOT_OTEL_FILE_EXPORTER_PATH: process.env.COPILOT_OTEL_FILE_EXPORTER_PATH,
      COPILOT_OTEL_EXPORTER_TYPE: process.env.COPILOT_OTEL_EXPORTER_TYPE,
      COPILOT_OTEL_SOURCE_NAME: process.env.COPILOT_OTEL_SOURCE_NAME,
      OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT:
        process.env.OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT,
    },
  }));
}

saveCapture();

let buffer = Buffer.alloc(0);
process.stdin.on("data", chunk => {
  buffer = Buffer.concat([buffer, chunk]);
  processBuffer();
});
process.stdin.resume();

function processBuffer() {
  while (true) {
    const headerEnd = buffer.indexOf("\r\n\r\n");
    if (headerEnd < 0) return;
    const header = buffer.subarray(0, headerEnd).toString("utf8");
    const match = /Content-Length:\s*(\d+)/i.exec(header);
    if (!match) throw new Error("Missing Content-Length header");
    const length = Number(match[1]);
    const bodyStart = headerEnd + 4;
    const bodyEnd = bodyStart + length;
    if (buffer.length < bodyEnd) return;
    const body = buffer.subarray(bodyStart, bodyEnd).toString("utf8");
    buffer = buffer.subarray(bodyEnd);
    handleMessage(JSON.parse(body));
  }
}

function handleMessage(message) {
  if (!Object.prototype.hasOwnProperty.call(message, "id")) {
    return;
  }
  requests.push({ method: message.method, params: message.params });
  saveCapture();
  if (message.method === "connect") {
    writeResponse(message.id, { ok: true, protocolVersion: 3, version: "fake" });
    return;
  }
  if (message.method === "ping") {
    writeResponse(message.id, { message: "pong", protocolVersion: 3, timestamp: Date.now() });
    return;
  }
  if (message.method === "session.create") {
    const sessionId = (message.params && message.params.sessionId) || "fake-session";
    writeResponse(message.id, { sessionId, workspacePath: null, capabilities: null });
    return;
  }
  if (message.method === "session.resume") {
    const sessionId = (message.params && message.params.sessionId) || "fake-session";
    writeResponse(message.id, { sessionId, workspacePath: null, capabilities: null });
    return;
  }
  writeResponse(message.id, {});
}

function writeResponse(id, result) {
  const body = JSON.stringify({ jsonrpc: "2.0", id, result });
  process.stdout.write("Content-Length: " + Buffer.byteLength(body, "utf8") + "\r\n\r\n" + body);
}
`
