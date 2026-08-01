package rpc

import (
	"bytes"
	"encoding/json"
	"fmt"
	"strconv"
	"time"
)

// UnmarshalJSON accepts both current ISO-8601 ping timestamps and older
// epoch-millisecond timestamps emitted by some CLI builds.
func (p *PingResult) UnmarshalJSON(data []byte) error {
	var wire struct {
		Message         string          `json:"message"`
		ProtocolVersion int64           `json:"protocolVersion"`
		Timestamp       json.RawMessage `json:"timestamp"`
	}
	if err := json.Unmarshal(data, &wire); err != nil {
		return err
	}

	timestamp, err := parsePingTimestamp(wire.Timestamp)
	if err != nil {
		return err
	}

	p.Message = wire.Message
	p.ProtocolVersion = wire.ProtocolVersion
	p.Timestamp = timestamp
	return nil
}

func parsePingTimestamp(raw json.RawMessage) (time.Time, error) {
	raw = bytes.TrimSpace(raw)
	if len(raw) == 0 || bytes.Equal(raw, []byte("null")) {
		return time.Time{}, nil
	}

	if raw[0] == '"' {
		var value string
		if err := json.Unmarshal(raw, &value); err != nil {
			return time.Time{}, fmt.Errorf("ping timestamp: %w", err)
		}
		return parsePingTimestampString(value)
	}

	var number json.Number
	decoder := json.NewDecoder(bytes.NewReader(raw))
	decoder.UseNumber()
	if err := decoder.Decode(&number); err != nil {
		return time.Time{}, fmt.Errorf("ping timestamp: %w", err)
	}
	return parsePingTimestampNumber(number)
}

func parsePingTimestampString(value string) (time.Time, error) {
	if milliseconds, err := strconv.ParseInt(value, 10, 64); err == nil {
		return time.UnixMilli(milliseconds), nil
	}
	if timestamp, err := time.Parse(time.RFC3339Nano, value); err == nil {
		return timestamp, nil
	}
	return time.Time{}, fmt.Errorf("ping timestamp: unsupported string value %q", value)
}

func parsePingTimestampNumber(number json.Number) (time.Time, error) {
	if milliseconds, err := number.Int64(); err == nil {
		return time.UnixMilli(milliseconds), nil
	}
	value, err := strconv.ParseFloat(number.String(), 64)
	if err != nil {
		return time.Time{}, fmt.Errorf("ping timestamp: %w", err)
	}
	return time.UnixMilli(int64(value)), nil
}
