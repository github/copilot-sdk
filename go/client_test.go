package copilot

import (
	"context"
	"encoding/json"
	"os"
	"path/filepath"
	"reflect"
	"regexp"
	"sync"
	"testing"
)

// This file is for unit tests. Where relevant, prefer to add e2e tests in e2e/*.test.go instead

func TestClient_URLParsing(t *testing.T) {
	t.Run("should parse port-only URL format", func(t *testing.T) {
		client := NewClient(&ClientOptions{
			CLIUrl: "8080",
		})

		if client.actualPort != 8080 {
			t.Errorf("Expected port 8080, got %d", client.actualPort)
		}
		if client.actualHost != "localhost" {
			t.Errorf("Expected host localhost, got %s", client.actualHost)
		}
		if !client.isExternalServer {
			t.Error("Expected isExternalServer to be true")
		}
	})

	t.Run("should parse host:port URL format", func(t *testing.T) {
		client := NewClient(&ClientOptions{
			CLIUrl: "127.0.0.1:9000",
		})

		if client.actualPort != 9000 {
			t.Errorf("Expected port 9000, got %d", client.actualPort)
		}
		if client.actualHost != "127.0.0.1" {
			t.Errorf("Expected host 127.0.0.1, got %s", client.actualHost)
		}
		if !client.isExternalServer {
			t.Error("Expected isExternalServer to be true")
		}
	})

	t.Run("should parse http://host:port URL format", func(t *testing.T) {
		client := NewClient(&ClientOptions{
			CLIUrl: "http://localhost:7000",
		})

		if client.actualPort != 7000 {
			t.Errorf("Expected port 7000, got %d", client.actualPort)
		}
		if client.actualHost != "localhost" {
			t.Errorf("Expected host localhost, got %s", client.actualHost)
		}
		if !client.isExternalServer {
			t.Error("Expected isExternalServer to be true")
		}
	})

	t.Run("should parse https://host:port URL format", func(t *testing.T) {
		client := NewClient(&ClientOptions{
			CLIUrl: "https://example.com:443",
		})

		if client.actualPort != 443 {
			t.Errorf("Expected port 443, got %d", client.actualPort)
		}
		if client.actualHost != "example.com" {
			t.Errorf("Expected host example.com, got %s", client.actualHost)
		}
		if !client.isExternalServer {
			t.Error("Expected isExternalServer to be true")
		}
	})

	t.Run("should throw error for invalid URL format", func(t *testing.T) {
		defer func() {
			if r := recover(); r == nil {
				t.Error("Expected panic for invalid URL format")
			} else {
				matched, _ := regexp.MatchString("Invalid port in CLIUrl", r.(string))
				if !matched {
					t.Errorf("Expected panic message to contain 'Invalid port in CLIUrl', got: %v", r)
				}
			}
		}()

		NewClient(&ClientOptions{
			CLIUrl: "invalid-url",
		})
	})

	t.Run("should throw error for invalid port - too high", func(t *testing.T) {
		defer func() {
			if r := recover(); r == nil {
				t.Error("Expected panic for invalid port")
			} else {
				matched, _ := regexp.MatchString("Invalid port in CLIUrl", r.(string))
				if !matched {
					t.Errorf("Expected panic message to contain 'Invalid port in CLIUrl', got: %v", r)
				}
			}
		}()

		NewClient(&ClientOptions{
			CLIUrl: "localhost:99999",
		})
	})

	t.Run("should throw error for invalid port - zero", func(t *testing.T) {
		defer func() {
			if r := recover(); r == nil {
				t.Error("Expected panic for invalid port")
			} else {
				matched, _ := regexp.MatchString("Invalid port in CLIUrl", r.(string))
				if !matched {
					t.Errorf("Expected panic message to contain 'Invalid port in CLIUrl', got: %v", r)
				}
			}
		}()

		NewClient(&ClientOptions{
			CLIUrl: "localhost:0",
		})
	})

	t.Run("should throw error for invalid port - negative", func(t *testing.T) {
		defer func() {
			if r := recover(); r == nil {
				t.Error("Expected panic for invalid port")
			} else {
				matched, _ := regexp.MatchString("Invalid port in CLIUrl", r.(string))
				if !matched {
					t.Errorf("Expected panic message to contain 'Invalid port in CLIUrl', got: %v", r)
				}
			}
		}()

		NewClient(&ClientOptions{
			CLIUrl: "localhost:-1",
		})
	})

	t.Run("should throw error when CLIUrl is used with UseStdio", func(t *testing.T) {
		defer func() {
			if r := recover(); r == nil {
				t.Error("Expected panic for mutually exclusive options")
			} else {
				matched, _ := regexp.MatchString("CLIUrl is mutually exclusive", r.(string))
				if !matched {
					t.Errorf("Expected panic message to contain 'CLIUrl is mutually exclusive', got: %v", r)
				}
			}
		}()

		NewClient(&ClientOptions{
			CLIUrl:   "localhost:8080",
			UseStdio: Bool(true),
		})
	})

	t.Run("should throw error when CLIUrl is used with CLIPath", func(t *testing.T) {
		defer func() {
			if r := recover(); r == nil {
				t.Error("Expected panic for mutually exclusive options")
			} else {
				matched, _ := regexp.MatchString("CLIUrl is mutually exclusive", r.(string))
				if !matched {
					t.Errorf("Expected panic message to contain 'CLIUrl is mutually exclusive', got: %v", r)
				}
			}
		}()

		NewClient(&ClientOptions{
			CLIUrl:  "localhost:8080",
			CLIPath: "/path/to/cli",
		})
	})

	t.Run("should set UseStdio to false when CLIUrl is provided", func(t *testing.T) {
		client := NewClient(&ClientOptions{
			CLIUrl: "8080",
		})

		if client.useStdio {
			t.Error("Expected UseStdio to be false when CLIUrl is provided")
		}
	})

	t.Run("should set UseStdio to true when UseStdio is set to true", func(t *testing.T) {
		client := NewClient(&ClientOptions{
			UseStdio: Bool(true),
		})

		if !client.useStdio {
			t.Error("Expected UseStdio to be true when UseStdio is set to true")
		}
	})

	t.Run("should set UseStdio to false when UseStdio is set to false", func(t *testing.T) {
		client := NewClient(&ClientOptions{
			UseStdio: Bool(false),
		})

		if client.useStdio {
			t.Error("Expected UseStdio to be false when UseStdio is set to false")
		}
	})

	t.Run("should mark client as using external server", func(t *testing.T) {
		client := NewClient(&ClientOptions{
			CLIUrl: "localhost:8080",
		})

		if !client.isExternalServer {
			t.Error("Expected isExternalServer to be true when CLIUrl is provided")
		}
	})
}

func TestClient_AuthOptions(t *testing.T) {
	t.Run("should accept GitHubToken option", func(t *testing.T) {
		client := NewClient(&ClientOptions{
			GitHubToken: "gho_test_token",
		})

		if client.options.GitHubToken != "gho_test_token" {
			t.Errorf("Expected GitHubToken to be 'gho_test_token', got %q", client.options.GitHubToken)
		}
	})

	t.Run("should default UseLoggedInUser to nil when no GitHubToken", func(t *testing.T) {
		client := NewClient(&ClientOptions{})

		if client.options.UseLoggedInUser != nil {
			t.Errorf("Expected UseLoggedInUser to be nil, got %v", client.options.UseLoggedInUser)
		}
	})

	t.Run("should allow explicit UseLoggedInUser false", func(t *testing.T) {
		client := NewClient(&ClientOptions{
			UseLoggedInUser: Bool(false),
		})

		if client.options.UseLoggedInUser == nil || *client.options.UseLoggedInUser != false {
			t.Error("Expected UseLoggedInUser to be false")
		}
	})

	t.Run("should allow explicit UseLoggedInUser true with GitHubToken", func(t *testing.T) {
		client := NewClient(&ClientOptions{
			GitHubToken:     "gho_test_token",
			UseLoggedInUser: Bool(true),
		})

		if client.options.UseLoggedInUser == nil || *client.options.UseLoggedInUser != true {
			t.Error("Expected UseLoggedInUser to be true")
		}
	})

	t.Run("should throw error when GitHubToken is used with CLIUrl", func(t *testing.T) {
		defer func() {
			if r := recover(); r == nil {
				t.Error("Expected panic for auth options with CLIUrl")
			} else {
				matched, _ := regexp.MatchString("GitHubToken and UseLoggedInUser cannot be used with CLIUrl", r.(string))
				if !matched {
					t.Errorf("Expected panic message about auth options, got: %v", r)
				}
			}
		}()

		NewClient(&ClientOptions{
			CLIUrl:      "localhost:8080",
			GitHubToken: "gho_test_token",
		})
	})

	t.Run("should throw error when UseLoggedInUser is used with CLIUrl", func(t *testing.T) {
		defer func() {
			if r := recover(); r == nil {
				t.Error("Expected panic for auth options with CLIUrl")
			} else {
				matched, _ := regexp.MatchString("GitHubToken and UseLoggedInUser cannot be used with CLIUrl", r.(string))
				if !matched {
					t.Errorf("Expected panic message about auth options, got: %v", r)
				}
			}
		}()

		NewClient(&ClientOptions{
			CLIUrl:          "localhost:8080",
			UseLoggedInUser: Bool(false),
		})
	})
}

func TestClient_EnvOptions(t *testing.T) {
	t.Run("should store custom environment variables", func(t *testing.T) {
		client := NewClient(&ClientOptions{
			Env: []string{"FOO=bar", "BAZ=qux"},
		})

		if len(client.options.Env) != 2 {
			t.Errorf("Expected 2 environment variables, got %d", len(client.options.Env))
		}
		if client.options.Env[0] != "FOO=bar" {
			t.Errorf("Expected first env var to be 'FOO=bar', got %q", client.options.Env[0])
		}
		if client.options.Env[1] != "BAZ=qux" {
			t.Errorf("Expected second env var to be 'BAZ=qux', got %q", client.options.Env[1])
		}
	})

	t.Run("should default to inherit from current process", func(t *testing.T) {
		client := NewClient(&ClientOptions{})

		if want := os.Environ(); !reflect.DeepEqual(client.options.Env, want) {
			t.Errorf("Expected Env to be %v, got %v", want, client.options.Env)
		}
	})

	t.Run("should default to inherit from current process with nil options", func(t *testing.T) {
		client := NewClient(nil)

		if want := os.Environ(); !reflect.DeepEqual(client.options.Env, want) {
			t.Errorf("Expected Env to be %v, got %v", want, client.options.Env)
		}
	})

	t.Run("should allow empty environment", func(t *testing.T) {
		client := NewClient(&ClientOptions{
			Env: []string{},
		})

		if client.options.Env == nil {
			t.Error("Expected Env to be non-nil empty slice")
		}
		if len(client.options.Env) != 0 {
			t.Errorf("Expected 0 environment variables, got %d", len(client.options.Env))
		}
	})
}

func findCLIPathForTest() string {
	abs, _ := filepath.Abs("../nodejs/node_modules/@github/copilot/index.js")
	if fileExistsForTest(abs) {
		return abs
	}
	return ""
}

func fileExistsForTest(path string) bool {
	_, err := os.Stat(path)
	return err == nil
}

func TestCreateSessionRequest_ClientName(t *testing.T) {
	t.Run("includes clientName in JSON when set", func(t *testing.T) {
		req := createSessionRequest{ClientName: "my-app"}
		data, err := json.Marshal(req)
		if err != nil {
			t.Fatalf("Failed to marshal: %v", err)
		}
		var m map[string]any
		if err := json.Unmarshal(data, &m); err != nil {
			t.Fatalf("Failed to unmarshal: %v", err)
		}
		if m["clientName"] != "my-app" {
			t.Errorf("Expected clientName to be 'my-app', got %v", m["clientName"])
		}
	})

	t.Run("omits clientName from JSON when empty", func(t *testing.T) {
		req := createSessionRequest{}
		data, _ := json.Marshal(req)
		var m map[string]any
		json.Unmarshal(data, &m)
		if _, ok := m["clientName"]; ok {
			t.Error("Expected clientName to be omitted when empty")
		}
	})
}

func TestResumeSessionRequest_ClientName(t *testing.T) {
	t.Run("includes clientName in JSON when set", func(t *testing.T) {
		req := resumeSessionRequest{SessionID: "s1", ClientName: "my-app"}
		data, err := json.Marshal(req)
		if err != nil {
			t.Fatalf("Failed to marshal: %v", err)
		}
		var m map[string]any
		if err := json.Unmarshal(data, &m); err != nil {
			t.Fatalf("Failed to unmarshal: %v", err)
		}
		if m["clientName"] != "my-app" {
			t.Errorf("Expected clientName to be 'my-app', got %v", m["clientName"])
		}
	})

	t.Run("omits clientName from JSON when empty", func(t *testing.T) {
		req := resumeSessionRequest{SessionID: "s1"}
		data, _ := json.Marshal(req)
		var m map[string]any
		json.Unmarshal(data, &m)
		if _, ok := m["clientName"]; ok {
			t.Error("Expected clientName to be omitted when empty")
		}
	})
}

func TestCreateSessionRequest_Agent(t *testing.T) {
	t.Run("includes agent in JSON when set", func(t *testing.T) {
		req := createSessionRequest{Agent: "test-agent"}
		data, err := json.Marshal(req)
		if err != nil {
			t.Fatalf("Failed to marshal: %v", err)
		}
		var m map[string]any
		if err := json.Unmarshal(data, &m); err != nil {
			t.Fatalf("Failed to unmarshal: %v", err)
		}
		if m["agent"] != "test-agent" {
			t.Errorf("Expected agent to be 'test-agent', got %v", m["agent"])
		}
	})

	t.Run("omits agent from JSON when empty", func(t *testing.T) {
		req := createSessionRequest{}
		data, _ := json.Marshal(req)
		var m map[string]any
		json.Unmarshal(data, &m)
		if _, ok := m["agent"]; ok {
			t.Error("Expected agent to be omitted when empty")
		}
	})
}

func TestResumeSessionRequest_Agent(t *testing.T) {
	t.Run("includes agent in JSON when set", func(t *testing.T) {
		req := resumeSessionRequest{SessionID: "s1", Agent: "test-agent"}
		data, err := json.Marshal(req)
		if err != nil {
			t.Fatalf("Failed to marshal: %v", err)
		}
		var m map[string]any
		if err := json.Unmarshal(data, &m); err != nil {
			t.Fatalf("Failed to unmarshal: %v", err)
		}
		if m["agent"] != "test-agent" {
			t.Errorf("Expected agent to be 'test-agent', got %v", m["agent"])
		}
	})

	t.Run("omits agent from JSON when empty", func(t *testing.T) {
		req := resumeSessionRequest{SessionID: "s1"}
		data, _ := json.Marshal(req)
		var m map[string]any
		json.Unmarshal(data, &m)
		if _, ok := m["agent"]; ok {
			t.Error("Expected agent to be omitted when empty")
		}
	})
}

func TestOverridesBuiltInTool(t *testing.T) {
	t.Run("OverridesBuiltInTool is serialized in tool definition", func(t *testing.T) {
		tool := Tool{
			Name:                 "grep",
			Description:          "Custom grep",
			OverridesBuiltInTool: true,
			Handler:              func(_ ToolInvocation) (ToolResult, error) { return ToolResult{}, nil },
		}
		data, err := json.Marshal(tool)
		if err != nil {
			t.Fatalf("failed to marshal: %v", err)
		}
		var m map[string]any
		if err := json.Unmarshal(data, &m); err != nil {
			t.Fatalf("failed to unmarshal: %v", err)
		}
		if v, ok := m["overridesBuiltInTool"]; !ok || v != true {
			t.Errorf("expected overridesBuiltInTool=true, got %v", m)
		}
	})

	t.Run("OverridesBuiltInTool omitted when false", func(t *testing.T) {
		tool := Tool{
			Name:        "custom_tool",
			Description: "A custom tool",
			Handler:     func(_ ToolInvocation) (ToolResult, error) { return ToolResult{}, nil },
		}
		data, err := json.Marshal(tool)
		if err != nil {
			t.Fatalf("failed to marshal: %v", err)
		}
		var m map[string]any
		if err := json.Unmarshal(data, &m); err != nil {
			t.Fatalf("failed to unmarshal: %v", err)
		}
		if _, ok := m["overridesBuiltInTool"]; ok {
			t.Errorf("expected overridesBuiltInTool to be omitted, got %v", m)
		}
	})
}

func TestClient_CreateSession_RequiresPermissionHandler(t *testing.T) {
	t.Run("returns error when config is nil", func(t *testing.T) {
		client := NewClient(nil)
		_, err := client.CreateSession(t.Context(), nil)
		if err == nil {
			t.Fatal("Expected error when OnPermissionRequest is nil")
		}
		matched, _ := regexp.MatchString("OnPermissionRequest.*is required", err.Error())
		if !matched {
			t.Errorf("Expected error about OnPermissionRequest being required, got: %v", err)
		}
	})

	t.Run("returns error when OnPermissionRequest is not set", func(t *testing.T) {
		client := NewClient(nil)
		_, err := client.CreateSession(t.Context(), &SessionConfig{})
		if err == nil {
			t.Fatal("Expected error when OnPermissionRequest is nil")
		}
		matched, _ := regexp.MatchString("OnPermissionRequest.*is required", err.Error())
		if !matched {
			t.Errorf("Expected error about OnPermissionRequest being required, got: %v", err)
		}
	})
}

func TestClient_ResumeSession_RequiresPermissionHandler(t *testing.T) {
	t.Run("returns error when config is nil", func(t *testing.T) {
		client := NewClient(nil)
		_, err := client.ResumeSessionWithOptions(t.Context(), "some-id", nil)
		if err == nil {
			t.Fatal("Expected error when OnPermissionRequest is nil")
		}
		matched, _ := regexp.MatchString("OnPermissionRequest.*is required", err.Error())
		if !matched {
			t.Errorf("Expected error about OnPermissionRequest being required, got: %v", err)
		}
	})
}

func TestListModelsWithCustomHandler(t *testing.T) {
	customModels := []ModelInfo{
		{
			ID:   "my-custom-model",
			Name: "My Custom Model",
			Capabilities: ModelCapabilities{
				Supports: ModelSupports{Vision: false, ReasoningEffort: false},
				Limits:   ModelLimits{MaxContextWindowTokens: 128000},
			},
		},
	}

	callCount := 0
	handler := func(ctx context.Context) ([]ModelInfo, error) {
		callCount++
		return customModels, nil
	}

	client := NewClient(&ClientOptions{OnListModels: handler})

	models, err := client.ListModels(t.Context())
	if err != nil {
		t.Fatalf("ListModels failed: %v", err)
	}
	if callCount != 1 {
		t.Errorf("expected handler called once, got %d", callCount)
	}
	if len(models) != 1 || models[0].ID != "my-custom-model" {
		t.Errorf("unexpected models: %+v", models)
	}
}

func TestListModelsHandlerCachesResults(t *testing.T) {
	customModels := []ModelInfo{
		{
			ID:   "cached-model",
			Name: "Cached Model",
			Capabilities: ModelCapabilities{
				Supports: ModelSupports{Vision: false, ReasoningEffort: false},
				Limits:   ModelLimits{MaxContextWindowTokens: 128000},
			},
		},
	}

	callCount := 0
	handler := func(ctx context.Context) ([]ModelInfo, error) {
		callCount++
		return customModels, nil
	}

	client := NewClient(&ClientOptions{OnListModels: handler})

	_, _ = client.ListModels(t.Context())
	_, _ = client.ListModels(t.Context())
	if callCount != 1 {
		t.Errorf("expected handler called once due to caching, got %d", callCount)
	}
}

func TestClient_StartStopRace(t *testing.T) {
	cliPath := findCLIPathForTest()
	if cliPath == "" {
		t.Skip("CLI not found")
	}
	client := NewClient(&ClientOptions{CLIPath: cliPath})
	defer client.ForceStop()
	errChan := make(chan error)
	wg := sync.WaitGroup{}
	for range 10 {
		wg.Add(3)
		go func() {
			defer wg.Done()
			if err := client.Start(t.Context()); err != nil {
				select {
				case errChan <- err:
				default:
				}
			}
		}()
		go func() {
			defer wg.Done()
			if err := client.Stop(); err != nil {
				select {
				case errChan <- err:
				default:
				}
			}
		}()
		go func() {
			defer wg.Done()
			client.ForceStop()
		}()
	}
	wg.Wait()
	close(errChan)
	if err := <-errChan; err != nil {
		t.Fatal(err)
	}
}
