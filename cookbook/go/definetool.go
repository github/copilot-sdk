/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package copilot

import (
	"encoding/json"
	"fmt"
	"reflect"

	"github.com/google/jsonschema-go/jsonschema"
)

// DefineTool creates a Tool with automatic JSON schema generation from a typed handler function.
// The handler receives typed arguments (automatically unmarshaled from JSON) and the raw ToolInvocation.
// The handler can return any value - strings pass through directly, other types are JSON-serialized.
//
// Example:
//
//	type GetWeatherParams struct {
//	    City string `json:"city" jsonschema:"city name"`
//	    Unit string `json:"unit" jsonschema:"temperature unit (celsius or fahrenheit)"`
//	}
//
//	tool := copilot.DefineTool("get_weather", "Get weather for a city",
//	    func(params GetWeatherParams, inv copilot.ToolInvocation) (any, error) {
//	        return fmt.Sprintf("Weather in %s: 22°%s", params.City, params.Unit), nil
//	    })
func DefineTool[T any, U any](name, description string, handler func(T, ToolInvocation) (U, error)) Tool {
	var zero T
	schema := generateSchemaForType(reflect.TypeOf(zero))

	return Tool{
		Name:        name,
		Description: description,
		Parameters:  schema,
		Handler:     createTypedHandler(handler),
	}
}

// createTypedHandler wraps a typed handler function into the standard ToolHandler signature.
func createTypedHandler[T any, U any](handler func(T, ToolInvocation) (U, error)) ToolHandler {
	return func(inv ToolInvocation) (ToolResult, error) {
		var params T

		// Convert arguments to typed struct via JSON round-trip
		// Arguments is already map[string]interface{} from JSON-RPC parsing
		jsonBytes, err := json.Marshal(inv.Arguments)
		if err != nil {
			return ToolResult{}, fmt.Errorf("failed to marshal arguments: %w", err)
		}

		if err := json.Unmarshal(jsonBytes, &params); err != nil {
			return ToolResult{}, fmt.Errorf("failed to unmarshal arguments into %T: %w", params, err)
		}

		result, err := handler(params, inv)
		if err != nil {
			return ToolResult{}, err
		}

		return normalizeResult(result)
	}
}

// normalizeResult converts any value to a ToolResult.
// Strings pass through directly, ToolResult passes through, other types are JSON-serialized.
func normalizeResult(result any) (ToolResult, error) {
	if result == nil {
		return ToolResult{
			TextResultForLLM: "",
			ResultType:       "success",
		}, nil
	}

	// ToolResult passes through directly
	if tr, ok := result.(ToolResult); ok {
		return tr, nil
	}

	// Strings pass through directly
	if str, ok := result.(string); ok {
		return ToolResult{
			TextResultForLLM: str,
			ResultType:       "success",
		}, nil
	}

	// Everything else gets JSON-serialized
	jsonBytes, err := json.Marshal(result)
	if err != nil {
		return ToolResult{}, fmt.Errorf("failed to serialize result: %w", err)
	}

	return ToolResult{
		TextResultForLLM: string(jsonBytes),
		ResultType:       "success",
	}, nil
}

// generateSchemaForType generates a JSON schema map from a Go type using reflection.
// Panics if schema generation fails, as this indicates a programming error.
func generateSchemaForType(t reflect.Type) map[string]interface{} {
	if t == nil {
		return nil
	}

	// Handle pointer types
	if t.Kind() == reflect.Ptr {
		t = t.Elem()
	}

	// Use google/jsonschema-go to generate the schema
	schema, err := jsonschema.ForType(t, nil)
	if err != nil {
		panic(fmt.Sprintf("failed to generate schema for type %v: %v", t, err))
	}

	// Convert schema to map[string]interface{}
	schemaBytes, err := json.Marshal(schema)
	if err != nil {
		panic(fmt.Sprintf("failed to marshal schema for type %v: %v", t, err))
	}

	var schemaMap map[string]interface{}
	if err := json.Unmarshal(schemaBytes, &schemaMap); err != nil {
		panic(fmt.Sprintf("failed to unmarshal schema for type %v: %v", t, err))
	}

	return schemaMap
}
