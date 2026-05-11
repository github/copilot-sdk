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
	raw, err := json.Marshal(UIElicitationBooleanValue(true))
	if err != nil {
		t.Fatalf("marshal bool value: %v", err)
	}
	if string(raw) != `true` {
		t.Fatalf("marshal bool value = %s", raw)
	}

	var response UIElicitationResponse
	if err := json.Unmarshal([]byte(`{"action":"accept","content":{"choices":["a","b"]}}`), &response); err != nil {
		t.Fatalf("unmarshal response with string array value: %v", err)
	}
	decodedArray, ok := response.Content["choices"].(UIElicitationStringArrayValue)
	if !ok {
		t.Fatalf("unmarshal string array value = %T, want UIElicitationStringArrayValue", response.Content["choices"])
	}
	if len(decodedArray) != 2 || decodedArray[0] != "a" || decodedArray[1] != "b" {
		t.Fatalf("unmarshal string array value = %#v", decodedArray)
	}
}

func TestUIElicitationSchemaPropertyJSONUnion(t *testing.T) {
	var schema UIElicitationSchema
	if err := json.Unmarshal([]byte(`{
		"type":"object",
		"properties":{
			"confirmed":{"type":"boolean","default":true},
			"choice":{"type":"string","enum":["a","b"]},
			"freeform":{"type":"string","minLength":1},
			"count":{"type":"integer","minimum":0},
			"arrayChoice":{"type":"array","items":{"type":"string","enum":["a","b"]}},
			"arrayAnyOf":{"type":"array","items":{"anyOf":[{"const":"a","title":"A"}]}}
		},
		"required":["confirmed"]
	}`), &schema); err != nil {
		t.Fatalf("unmarshal elicitation schema: %v", err)
	}

	confirmed, ok := schema.Properties["confirmed"].(*UIElicitationSchemaPropertyBoolean)
	if !ok {
		t.Fatalf("confirmed property = %T, want *UIElicitationSchemaPropertyBoolean", schema.Properties["confirmed"])
	}
	if confirmed.Default == nil || !*confirmed.Default {
		t.Fatalf("confirmed default = %v, want true", confirmed.Default)
	}

	choice, ok := schema.Properties["choice"].(*UIElicitationStringEnumField)
	if !ok {
		t.Fatalf("choice property = %T, want *UIElicitationStringEnumField", schema.Properties["choice"])
	}
	if len(choice.Enum) != 2 || choice.Enum[0] != "a" || choice.Enum[1] != "b" {
		t.Fatalf("choice enum = %#v", choice.Enum)
	}

	freeform, ok := schema.Properties["freeform"].(*UIElicitationSchemaPropertyString)
	if !ok {
		t.Fatalf("freeform property = %T, want *UIElicitationSchemaPropertyString", schema.Properties["freeform"])
	}
	if freeform.MinLength == nil || *freeform.MinLength != 1 {
		t.Fatalf("freeform minLength = %v, want 1", freeform.MinLength)
	}

	count, ok := schema.Properties["count"].(*UIElicitationSchemaPropertyNumber)
	if !ok {
		t.Fatalf("count property = %T, want *UIElicitationSchemaPropertyNumber", schema.Properties["count"])
	}
	if count.Type() != UIElicitationSchemaPropertyTypeInteger {
		t.Fatalf("count type = %q, want %q", count.Type(), UIElicitationSchemaPropertyTypeInteger)
	}

	arrayChoice, ok := schema.Properties["arrayChoice"].(*UIElicitationArrayEnumField)
	if !ok {
		t.Fatalf("arrayChoice property = %T, want *UIElicitationArrayEnumField", schema.Properties["arrayChoice"])
	}
	if len(arrayChoice.Items.Enum) != 2 || arrayChoice.Items.Enum[0] != "a" || arrayChoice.Items.Enum[1] != "b" {
		t.Fatalf("arrayChoice items enum = %#v", arrayChoice.Items.Enum)
	}

	arrayAnyOf, ok := schema.Properties["arrayAnyOf"].(*UIElicitationArrayAnyOfField)
	if !ok {
		t.Fatalf("arrayAnyOf property = %T, want *UIElicitationArrayAnyOfField", schema.Properties["arrayAnyOf"])
	}
	if len(arrayAnyOf.Items.AnyOf) != 1 || arrayAnyOf.Items.AnyOf[0].Const != "a" || arrayAnyOf.Items.AnyOf[0].Title != "A" {
		t.Fatalf("arrayAnyOf items anyOf = %#v", arrayAnyOf.Items.AnyOf)
	}

	defaultValue := true
	encoded, err := json.Marshal(UIElicitationSchema{
		Type: UIElicitationSchemaTypeObject,
		Properties: map[string]UIElicitationSchemaProperty{
			"confirmed": &UIElicitationSchemaPropertyBoolean{Default: &defaultValue},
		},
	})
	if err != nil {
		t.Fatalf("marshal elicitation schema: %v", err)
	}
	var roundTrip UIElicitationSchema
	if err := json.Unmarshal(encoded, &roundTrip); err != nil {
		t.Fatalf("unmarshal marshaled elicitation schema: %v", err)
	}
	if _, ok := roundTrip.Properties["confirmed"].(*UIElicitationSchemaPropertyBoolean); !ok {
		t.Fatalf("round-trip confirmed property = %T, want *UIElicitationSchemaPropertyBoolean", roundTrip.Properties["confirmed"])
	}
}

func stringPtr(value string) *string {
	return &value
}
