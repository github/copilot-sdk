package rpc

import (
	"encoding/json"
	"testing"
)

func TestExternalToolResultJSONUnion(t *testing.T) {
	stringResult := ExternalToolResult{String: stringPtr("tool result")}
	raw, err := json.Marshal(stringResult)
	if err != nil {
		t.Fatalf("marshal string result: %v", err)
	}
	if string(raw) != `"tool result"` {
		t.Fatalf("marshal string result = %s", raw)
	}

	var decodedString ExternalToolResult
	if err := json.Unmarshal([]byte(`"tool result"`), &decodedString); err != nil {
		t.Fatalf("unmarshal string result: %v", err)
	}
	if decodedString.String == nil || *decodedString.String != "tool result" {
		t.Fatalf("unmarshal string result = %#v", decodedString)
	}

	objectResult := ExternalToolResult{ExternalToolTextResultForLlm: &ExternalToolTextResultForLlm{TextResultForLlm: "expanded"}}
	raw, err = json.Marshal(objectResult)
	if err != nil {
		t.Fatalf("marshal object result: %v", err)
	}
	if string(raw) != `{"textResultForLlm":"expanded"}` {
		t.Fatalf("marshal object result = %s", raw)
	}

	var decodedObject ExternalToolResult
	if err := json.Unmarshal([]byte(`{"textResultForLlm":"expanded"}`), &decodedObject); err != nil {
		t.Fatalf("unmarshal object result: %v", err)
	}
	if decodedObject.ExternalToolTextResultForLlm == nil || decodedObject.ExternalToolTextResultForLlm.TextResultForLlm != "expanded" {
		t.Fatalf("unmarshal object result = %#v", decodedObject)
	}
}

func TestFilterMappingJSONUnion(t *testing.T) {
	mapping := FilterMapping{EnumMap: map[string]FilterMappingValue{"secret": FilterMappingValueHiddenCharacters}}
	raw, err := json.Marshal(mapping)
	if err != nil {
		t.Fatalf("marshal filter mapping map: %v", err)
	}
	if string(raw) != `{"secret":"hidden_characters"}` {
		t.Fatalf("marshal filter mapping map = %s", raw)
	}

	var decodedMap FilterMapping
	if err := json.Unmarshal([]byte(`{"secret":"hidden_characters"}`), &decodedMap); err != nil {
		t.Fatalf("unmarshal filter mapping map: %v", err)
	}
	if decodedMap.EnumMap["secret"] != FilterMappingValueHiddenCharacters {
		t.Fatalf("unmarshal filter mapping map = %#v", decodedMap)
	}

	enumValue := FilterMappingStringMarkdown
	raw, err = json.Marshal(FilterMapping{Enum: &enumValue})
	if err != nil {
		t.Fatalf("marshal filter mapping enum: %v", err)
	}
	if string(raw) != `"markdown"` {
		t.Fatalf("marshal filter mapping enum = %s", raw)
	}

	var decodedEnum FilterMapping
	if err := json.Unmarshal([]byte(`"markdown"`), &decodedEnum); err != nil {
		t.Fatalf("unmarshal filter mapping enum: %v", err)
	}
	if decodedEnum.Enum == nil || *decodedEnum.Enum != FilterMappingStringMarkdown {
		t.Fatalf("unmarshal filter mapping enum = %#v", decodedEnum)
	}
}

func TestUIElicitationFieldValueJSONUnion(t *testing.T) {
	boolValue := true
	raw, err := json.Marshal(UIElicitationFieldValue{Bool: &boolValue})
	if err != nil {
		t.Fatalf("marshal bool value: %v", err)
	}
	if string(raw) != `true` {
		t.Fatalf("marshal bool value = %s", raw)
	}

	var decodedArray UIElicitationFieldValue
	if err := json.Unmarshal([]byte(`["a","b"]`), &decodedArray); err != nil {
		t.Fatalf("unmarshal string array value: %v", err)
	}
	if len(decodedArray.StringArray) != 2 || decodedArray.StringArray[0] != "a" || decodedArray.StringArray[1] != "b" {
		t.Fatalf("unmarshal string array value = %#v", decodedArray)
	}
}

func stringPtr(value string) *string {
	return &value
}
