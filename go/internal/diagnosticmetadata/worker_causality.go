// Copyright (c) Microsoft Corporation. All rights reserved.

// Package diagnosticmetadata keeps optional diagnostics from rejecting product data.
package diagnosticmetadata

import (
	"encoding/json"
	"errors"
	"log"
	"regexp"
)

var uuidPattern = regexp.MustCompile(`^[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$`)
var sessionPattern = regexp.MustCompile(`^[A-Za-z0-9_-]+$`)

func text(value any) bool {
	s, ok := value.(string)
	return ok && s != "" && len(s) <= 256
}

func uuid(value any) bool {
	s, ok := value.(string)
	return ok && uuidPattern.MatchString(s)
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
	session, ok := ref["sessionId"].(string)
	agent, hasAgent := ref["agentId"]
	return fields(ref, "sessionId", "eventId", "agentId", "eventType", "provenance") &&
		ok && len(session) <= 256 && sessionPattern.MatchString(session) && uuid(ref["eventId"]) &&
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

type budget struct{ count int }

func (b *budget) Write(p []byte) (int, error) {
	b.count += len(p)
	// Encoder adds one newline; the compact single-property field is limited to 4096.
	if b.count > 4097 {
		b.count = 4098
		return 0, errors.New("worker metadata budget")
	}
	return len(p), nil
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
	if len(raw) == 0 {
		return false
	}
	var value map[string]any
	remaining := 4096
	if json.Unmarshal(raw, &value) == nil && small(value, &remaining, 0) && supported(value) {
		writer := &budget{}
		encoder := json.NewEncoder(writer)
		encoder.SetEscapeHTML(false)
		if encoder.Encode(map[string]any{"workerCausality": value}) == nil && json.Unmarshal(raw, target) == nil {
			return true
		}
	}
	log.Print("Ignoring invalid, unsupported or oversized workerCausality metadata")
	return false
}
