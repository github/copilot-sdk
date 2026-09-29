// Copyright (c) Microsoft Corporation. All rights reserved.

// Package diagnosticmetadata keeps optional diagnostics from rejecting product data.
package diagnosticmetadata

import (
	"encoding/json"
	"log"
	"regexp"
	"strconv"
	"unicode"
	"unicode/utf8"
)

var uuidPattern = regexp.MustCompile(`^[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$`)

func text(value any) bool {
	s, ok := value.(string)
	if !ok || s == "" || len(s) > 256 {
		return false
	}
	for _, character := range s {
		if unicode.IsControl(character) {
			return false
		}
	}
	return true
}

func uuid(value any) bool {
	s, ok := value.(string)
	return ok && uuidPattern.MatchString(s)
}

func hexQuad(value []byte) (uint16, bool) {
	if len(value) < 4 {
		return 0, false
	}
	var result uint16
	for _, character := range value[:4] {
		result <<= 4
		switch {
		case character >= '0' && character <= '9':
			result += uint16(character - '0')
		case character >= 'a' && character <= 'f':
			result += uint16(character-'a') + 10
		case character >= 'A' && character <= 'F':
			result += uint16(character-'A') + 10
		default:
			return 0, false
		}
	}
	return result, true
}

func validUnicodeScalarEscapes(raw []byte) bool {
	if !utf8.Valid(raw) {
		return false
	}
	inString := false
	for index := 0; index < len(raw); index++ {
		character := raw[index]
		if !inString {
			if character == '"' {
				inString = true
			}
			continue
		}
		if character == '"' {
			inString = false
			continue
		}
		if character != '\\' {
			continue
		}
		if index+1 >= len(raw) {
			return false
		}
		if raw[index+1] != 'u' {
			index++
			continue
		}
		codeUnit, ok := hexQuad(raw[index+2:])
		if !ok {
			return false
		}
		if codeUnit >= 0xd800 && codeUnit <= 0xdbff {
			if index+11 >= len(raw) || raw[index+6] != '\\' || raw[index+7] != 'u' {
				return false
			}
			low, ok := hexQuad(raw[index+8:])
			if !ok || low < 0xdc00 || low > 0xdfff {
				return false
			}
			index += 11
		} else {
			if codeUnit >= 0xdc00 && codeUnit <= 0xdfff {
				return false
			}
			index += 5
		}
	}
	return !inString
}

func fields(value map[string]any, allowed ...string) bool {
	if len(value) > len(allowed) {
		return false
	}
	for key := range value {
		found := false
		for _, candidate := range allowed {
			if key == candidate {
				found = true
				break
			}
		}
		if !found {
			return false
		}
	}
	return true
}

func reference(value any, eventType string) bool {
	ref, ok := value.(map[string]any)
	if !ok {
		return false
	}
	agent, hasAgent := ref["agentId"]
	return fields(ref, "sessionId", "eventId", "agentId", "eventType", "provenance") &&
		text(ref["sessionId"]) && uuid(ref["eventId"]) &&
		(!hasAgent || text(agent)) && ref["eventType"] == eventType &&
		(ref["provenance"] == "native" || ref["provenance"] == "ahp_coordinator")
}

func source(value any) bool {
	s, ok := value.(map[string]any)
	if !ok {
		return false
	}
	input, inputOK := s["input"].(map[string]any)
	admissions, admissionsOK := s["admissions"].([]any)
	_, complete := s["captureComplete"].(bool)
	if !fields(s, "input", "admissions", "captureComplete", "completion", "notification", "admittedDuring") ||
		!inputOK || !fields(input, "queueItemId", "agentId", "sender", "senderBridges") ||
		!admissionsOK || len(admissions) > 32 || !complete ||
		!uuid(input["queueItemId"]) || !text(input["agentId"]) {
		return false
	}
	sender, hasSender := input["sender"]
	if hasSender && !reference(sender, "tool.execution_start") {
		return false
	}
	if raw, exists := input["senderBridges"]; exists {
		edges, ok := raw.([]any)
		if !ok || !hasSender || len(edges) > 32 {
			return false
		}
		for _, raw := range edges {
			edge, ok := raw.(map[string]any)
			if !ok || !fields(edge, "source", "reported") ||
				!reference(edge["source"], "tool.execution_start") || !reference(edge["reported"], "tool.execution_start") {
				return false
			}
		}
	}
	for _, raw := range admissions {
		admission, ok := raw.(map[string]any)
		if !ok || !fields(admission, "kind", "messageId", "event", "ahpTurnId") ||
			!text(admission["messageId"]) ||
			(admission["kind"] != "queued_input" && admission["kind"] != "system_continuation") {
			return false
		}
		if turn, exists := admission["ahpTurnId"]; exists && !uuid(turn) {
			return false
		}
		if event, exists := admission["event"]; exists {
			if !reference(event, "user.message") || event.(map[string]any)["agentId"] != input["agentId"] {
				return false
			}
		}
	}
	for field, eventType := range map[string]string{"completion": "subagent.completed", "admittedDuring": "assistant.turn_start"} {
		if event, exists := s[field]; exists && !reference(event, eventType) {
			return false
		}
	}
	if raw, exists := s["notification"]; exists {
		n, ok := raw.(map[string]any)
		if !ok || !fields(n, "deliveryId", "event", "mode") ||
			!uuid(n["deliveryId"]) || (n["mode"] != "queued" && n["mode"] != "immediate") {
			return false
		}
		if event, exists := n["event"]; exists && !reference(event, "system.notification") {
			return false
		}
	}
	return true
}

func compactJSONStringSize(value string) int {
	size := 2
	for _, character := range value {
		switch character {
		case '"', '\\', '\b', '\f', '\n', '\r', '\t':
			size += 2
		default:
			if character < 0x20 {
				size += 6
			} else {
				size += utf8.RuneLen(character)
			}
		}
	}
	return size
}

func compactJSONSize(value any, depth int) (int, bool) {
	if depth > 32 {
		return 0, false
	}
	switch value := value.(type) {
	case nil:
		return 4, true
	case bool:
		if value {
			return 4, true
		}
		return 5, true
	case float64:
		return len(strconv.AppendFloat(nil, value, 'g', -1, 64)), true
	case string:
		return compactJSONStringSize(value), true
	case []any:
		size := 2
		for index, item := range value {
			itemSize, ok := compactJSONSize(item, depth+1)
			if !ok {
				return 0, false
			}
			if index > 0 {
				size++
			}
			size += itemSize
		}
		return size, true
	case map[string]any:
		size := 2
		index := 0
		for key, item := range value {
			itemSize, ok := compactJSONSize(item, depth+1)
			if !ok {
				return 0, false
			}
			if index > 0 {
				size++
			}
			size += compactJSONStringSize(key) + 1 + itemSize
			index++
		}
		return size, true
	default:
		return 0, false
	}
}

func small(value any, remaining *int, depth int) bool {
	*remaining--
	if *remaining < 0 || depth > 32 {
		return false
	}
	switch value := value.(type) {
	case string:
		*remaining -= len(value)
	case []any:
		for _, item := range value {
			if !small(item, remaining, depth+1) {
				return false
			}
		}
	case map[string]any:
		for key, item := range value {
			if !small(key, remaining, depth+1) || !small(item, remaining, depth+1) {
				return false
			}
		}
	}
	return *remaining >= 0
}

func supported(value map[string]any) bool {
	sources, ok := value["sources"].([]any)
	_, complete := value["captureComplete"].(bool)
	if !fields(value, "version", "observationProvenance", "sources", "captureComplete") ||
		!ok || len(sources) > 32 || !complete || value["version"] != float64(1) ||
		(value["observationProvenance"] != "native" && value["observationProvenance"] != "ahp_coordinator") {
		return false
	}
	for _, entry := range sources {
		if !source(entry) || (value["captureComplete"] == true && entry.(map[string]any)["captureComplete"] != true) {
			return false
		}
	}
	return true
}

// ReadWorkerCausality decodes availability only, not enclosing self identity or association.
func ReadWorkerCausality(raw json.RawMessage, target any) bool {
	if len(raw) == 0 || !validUnicodeScalarEscapes(raw) {
		return false
	}
	var value map[string]any
	remaining := 4096
	if json.Unmarshal(raw, &value) == nil && small(value, &remaining, 0) && supported(value) {
		size, ok := compactJSONSize(map[string]any{"workerCausality": value}, 0)
		if ok && size <= 4096 && json.Unmarshal(raw, target) == nil {
			return true
		}
	}
	log.Print("Ignoring invalid, unsupported or oversized workerCausality metadata")
	return false
}
