package copilot

import (
	"os"
	"path/filepath"
	"reflect"
	"regexp"
	"testing"
)

// This file is for unit tests. Where relevant, prefer to add e2e tests in e2e/*.test.go instead

func TestClient_HandleToolCallRequest(t *testing.T) {
	t.Run("returns a standardized failure result when a tool is not registered", func(t *testing.T) {
		cliPath := findCLIPathForTest()
		if cliPath == "" {
			t.Skip("CLI not found")
		}

		client := NewClient(&ClientOptions{CLIPath: cliPath})
		t.Cleanup(func() { client.ForceStop() })

		session, err := client.CreateSession(nil)
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		params := map[string]interface{}{
			"sessionId":  session.SessionID,
			"toolCallId": "123",
			"toolName":   "missing_tool",
			"arguments":  map[string]interface{}{},
		}
		response, _ := client.handleToolCallRequest(params)

		result, ok := response["result"].(ToolResult)
		if !ok {
			t.Fatalf("Expected result to be ToolResult, got %T", response["result"])
		}

		if result.ResultType != "failure" {
			t.Errorf("Expected resultType to be 'failure', got %q", result.ResultType)
		}

		if result.Error != "tool 'missing_tool' not supported" {
			t.Errorf("Expected error to be \"tool 'missing_tool' not supported\", got %q", result.Error)
		}
	})
}

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
				matched, _ := regexp.MatchString("Invalid CLIUrl format", r.(string))
				if !matched {
					t.Errorf("Expected panic message to contain 'Invalid CLIUrl format', got: %v", r)
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
	t.Run("should accept GithubToken option", func(t *testing.T) {
		client := NewClient(&ClientOptions{
			GithubToken: "gho_test_token",
		})

		if client.options.GithubToken != "gho_test_token" {
			t.Errorf("Expected GithubToken to be 'gho_test_token', got %q", client.options.GithubToken)
		}
	})

	t.Run("should default UseLoggedInUser to nil when no GithubToken", func(t *testing.T) {
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

	t.Run("should allow explicit UseLoggedInUser true with GithubToken", func(t *testing.T) {
		client := NewClient(&ClientOptions{
			GithubToken:     "gho_test_token",
			UseLoggedInUser: Bool(true),
		})

		if client.options.UseLoggedInUser == nil || *client.options.UseLoggedInUser != true {
			t.Error("Expected UseLoggedInUser to be true")
		}
	})

	t.Run("should throw error when GithubToken is used with CLIUrl", func(t *testing.T) {
		defer func() {
			if r := recover(); r == nil {
				t.Error("Expected panic for auth options with CLIUrl")
			} else {
				matched, _ := regexp.MatchString("GithubToken and UseLoggedInUser cannot be used with CLIUrl", r.(string))
				if !matched {
					t.Errorf("Expected panic message about auth options, got: %v", r)
				}
			}
		}()

		NewClient(&ClientOptions{
			CLIUrl:      "localhost:8080",
			GithubToken: "gho_test_token",
		})
	})

	t.Run("should throw error when UseLoggedInUser is used with CLIUrl", func(t *testing.T) {
		defer func() {
			if r := recover(); r == nil {
				t.Error("Expected panic for auth options with CLIUrl")
			} else {
				matched, _ := regexp.MatchString("GithubToken and UseLoggedInUser cannot be used with CLIUrl", r.(string))
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
