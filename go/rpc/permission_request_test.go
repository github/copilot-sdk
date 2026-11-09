package rpc_test

import (
	"bytes"
	"encoding/json"
	"reflect"
	"strings"
	"testing"

	"github.com/github/copilot-sdk/go/rpc"
)

func TestPermissionRequestEnvelopeRejectsAmbiguousArrayEdits(t *testing.T) {
	for _, mode := range []string{"swap", "swap-and-edit", "multiple-edits", "copy", "copy-and-edit"} {
		t.Run(mode, func(t *testing.T) {
			t.Parallel()
			var envelope rpc.PermissionRequestEnvelope
			input := `{"kind":"shell","commands":[{"identifier":"first","readOnly":true,"futurePolicy":"first-policy"},{"identifier":"second","readOnly":true,"futurePolicy":"second-policy"}]}`
			if err := json.Unmarshal([]byte(input), &envelope); err != nil {
				t.Fatal(err)
			}
			commands := envelope.Request.(*rpc.PermissionRequestShell).Commands
			if mode == "copy" || mode == "copy-and-edit" {
				commands[0] = commands[1]
			} else if mode != "multiple-edits" {
				commands[0], commands[1] = commands[1], commands[0]
			}
			if mode == "swap-and-edit" || mode == "multiple-edits" {
				commands[0].ReadOnly = false
				commands[1].ReadOnly = false
			}
			if mode == "copy-and-edit" {
				commands[0].ReadOnly = false
			}
			if output, err := json.Marshal(envelope); err == nil || !strings.Contains(err.Error(), "ambiguous array edits") {
				t.Fatalf("ambiguous edits must fail instead of mismatching policies: output=%s error=%v", output, err)
			}
		})
	}
}

func TestPermissionRequestEnvelopeLosslessArrayReorder(t *testing.T) {
	t.Parallel()
	input := `{"kind":"shell","commands":[{"identifier":"first","readOnly":true,"futurePolicy":{"id":9007199254740993,"nullable":null}},{"identifier":"second","readOnly":false,"futurePolicy":"second-policy"}],"futureDetails":true}`
	var fields map[string]json.RawMessage
	if err := json.Unmarshal([]byte(input), &fields); err != nil {
		t.Fatal(err)
	}
	var commands []json.RawMessage
	if err := json.Unmarshal(fields["commands"], &commands); err != nil {
		t.Fatal(err)
	}
	commands[0], commands[1] = commands[1], commands[0]
	var err error
	fields["commands"], err = json.Marshal(commands)
	if err != nil {
		t.Fatal(err)
	}
	reordered, err := json.Marshal(fields)
	if err != nil {
		t.Fatal(err)
	}
	var envelope rpc.PermissionRequestEnvelope
	if err := json.Unmarshal(reordered, &envelope); err != nil {
		t.Fatal(err)
	}
	output, err := json.Marshal(envelope)
	if err != nil {
		t.Fatal(err)
	}
	var got map[string]json.RawMessage
	if err := json.Unmarshal(output, &got); err != nil {
		t.Fatal(err)
	}
	if !reflect.DeepEqual(got, fields) || envelope.Request.(*rpc.PermissionRequestShell).Commands[0].Identifier != "second" {
		t.Fatalf("reorder lost entry identity or original fields: got=%s want=%s", output, reordered)
	}
}

func TestPermissionRequestEnvelopeArrayFieldEdits(t *testing.T) {
	for _, input := range []string{
		`{"kind":"shell","commands":[{"identifier":"first","readOnly":true,"futurePolicy":{"id":9007199254740993,"nullable":null}}]}`,
		`{"kind":"shell","commands":[{"identifier":"first","readOnly":true},{"identifier":"second","readOnly":true}]}`,
	} {
		t.Run(input, func(t *testing.T) {
			t.Parallel()
			var envelope rpc.PermissionRequestEnvelope
			if err := json.Unmarshal([]byte(input), &envelope); err != nil {
				t.Fatal(err)
			}
			envelope.Request.(*rpc.PermissionRequestShell).Commands[0].ReadOnly = false
			output, err := json.Marshal(envelope)
			if err != nil {
				t.Fatal(err)
			}
			expected := strings.Replace(input, `"readOnly":true`, `"readOnly":false`, 1)
			var got, want any
			for _, item := range []struct {
				data  []byte
				value *any
			}{{output, &got}, {[]byte(expected), &want}} {
				decoder := json.NewDecoder(bytes.NewReader(item.data))
				decoder.UseNumber()
				if err := decoder.Decode(item.value); err != nil {
					t.Fatal(err)
				}
			}
			if !reflect.DeepEqual(got, want) {
				t.Fatalf("field edit changed unrelated fields: got=%s want=%s", output, expected)
			}
		})
	}
}

func TestPermissionRequestEnvelopeOpaqueArgumentPrecision(t *testing.T) {
	for _, kind := range []string{"custom-tool", "mcp", "hook"} {
		t.Run(kind, func(t *testing.T) {
			t.Parallel()
			argumentField := "args"
			if kind == "hook" {
				argumentField = "toolArgs"
			}
			input := `{"kind":"` + kind + `","toolName":"original","toolDescription":"A tool","serverName":"server","toolTitle":"A tool","readOnly":false,"toolCallId":null,"` + argumentField + `":{"id":9007199254740993,"nested":[{"id":9007199254740993,"label":"original","nullable":null},{"id":9007199254740995,"label":"unchanged","nullable":null}]},"futureDetails":{"id":9007199254740993,"nullable":null}}`
			arguments := func(request rpc.PermissionRequest) map[string]any {
				t.Helper()
				switch request := request.(type) {
				case *rpc.PermissionRequestCustomTool:
					return request.Args.(map[string]any)
				case *rpc.PermissionRequestMCP:
					return request.Args.(map[string]any)
				case *rpc.PermissionRequestHook:
					return request.ToolArgs.(map[string]any)
				default:
					t.Fatalf("unexpected request type: %T", request)
					return nil
				}
			}

			t.Run("handler-visible", func(t *testing.T) {
				var envelope rpc.PermissionRequestEnvelope
				if err := json.Unmarshal([]byte(input), &envelope); err != nil {
					t.Fatal(err)
				}
				args := arguments(envelope.Request)
				for name, value := range map[string]any{
					"id":        args["id"],
					"nested.id": args["nested"].([]any)[0].(map[string]any)["id"],
				} {
					if value != json.Number("9007199254740993") {
						t.Errorf("%s = %v (%T), want json.Number(9007199254740993)", name, value, value)
					}
				}
			})

			t.Run("edited-nested-array", func(t *testing.T) {
				var envelope rpc.PermissionRequestEnvelope
				if err := json.Unmarshal([]byte(input), &envelope); err != nil {
					t.Fatal(err)
				}
				arguments(envelope.Request)["nested"].([]any)[0].(map[string]any)["label"] = "edited"
				switch request := envelope.Request.(type) {
				case *rpc.PermissionRequestCustomTool:
					request.ToolName = "edited"
				case *rpc.PermissionRequestMCP:
					request.ToolName = "edited"
				case *rpc.PermissionRequestHook:
					request.ToolName = "edited"
				}
				output, err := json.Marshal(envelope)
				if err != nil {
					t.Fatal(err)
				}
				expected := bytes.ReplaceAll([]byte(input), []byte(`"original"`), []byte(`"edited"`))
				var actualValue, expectedValue any
				for _, item := range []struct {
					data  []byte
					value *any
				}{{output, &actualValue}, {expected, &expectedValue}} {
					decoder := json.NewDecoder(bytes.NewReader(item.data))
					decoder.UseNumber()
					if err := decoder.Decode(item.value); err != nil {
						t.Fatal(err)
					}
				}
				if !reflect.DeepEqual(actualValue, expectedValue) {
					t.Fatalf("edited request = %s, want %s", output, expected)
				}
			})

			t.Run("legacy-float64", func(t *testing.T) {
				var event rpc.PermissionRequestedData
				if err := json.Unmarshal([]byte(`{"permissionRequest":`+input+`}`), &event); err != nil {
					t.Fatal(err)
				}
				args := arguments(event.PermissionRequest)
				for name, value := range map[string]any{
					"id":        args["id"],
					"nested.id": args["nested"].([]any)[0].(map[string]any)["id"],
				} {
					if value != float64(9007199254740992) {
						t.Errorf("%s = %v (%T), want legacy float64(9007199254740992)", name, value, value)
					}
				}
			})
		})
	}
}

func TestPermissionRequestEnvelopeOwnsOriginalJSON(t *testing.T) {
	for _, kind := range []string{"read", "future-kind"} {
		t.Run(kind, func(t *testing.T) {
			t.Parallel()
			original := []byte(`{"kind":"` + kind + `","intention":"Read a file","path":"/workspace/file.txt","futureDetails":9007199254740993}`)
			input := bytes.Clone(original)
			var envelope rpc.PermissionRequestEnvelope
			if err := json.Unmarshal(input, &envelope); err != nil {
				t.Fatal(err)
			}
			clear(input)
			data, err := json.Marshal(envelope)
			if err != nil {
				t.Fatal(err)
			}
			if !bytes.Equal(data, original) {
				t.Fatalf("envelope retained caller-owned input: got %s want %s", data, original)
			}
		})
	}
}

func TestPermissionRequestEnvelopeExplicitReplacement(t *testing.T) {
	var envelope rpc.PermissionRequestEnvelope
	if err := json.Unmarshal([]byte(`{"kind":"read","intention":"Read a file","path":"/original","futureDetails":true}`), &envelope); err != nil {
		t.Fatal(err)
	}
	retained := envelope
	replacement := &rpc.PermissionRequestRead{Intention: "Read a replacement", Path: "/replacement"}
	envelope.Replace(replacement)
	got, err := json.Marshal(envelope)
	if err != nil {
		t.Fatal(err)
	}
	want := `{"kind":"read","intention":"Read a replacement","path":"/replacement"}`
	if string(got) != want || envelope.Request != replacement {
		t.Fatalf("replacement merged original fields: got %s want %s", got, want)
	}
	data, err := json.Marshal(retained)
	if err != nil {
		t.Fatal(err)
	}
	if !bytes.Contains(data, []byte(`"futureDetails":true`)) {
		t.Fatalf("replacement changed a retained envelope copy: %s", data)
	}
}
