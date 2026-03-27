package jsonrpc2

import (
	"bufio"
	"fmt"
	"io"
	"math"
	"strconv"
	"strings"
)

// headerReader reads Content-Length delimited JSON-RPC frames from a stream.
type headerReader struct {
	in *bufio.Reader
}

func newHeaderReader(r io.Reader) *headerReader {
	return &headerReader{in: bufio.NewReader(r)}
}

// Read reads the next complete frame from the stream. It returns io.EOF on a
// clean end-of-stream (no partial data) and io.ErrUnexpectedEOF if the stream
// was interrupted mid-header.
func (r *headerReader) Read() ([]byte, error) {
	firstRead := true
	var contentLength int64
	// Read headers, stop on the first blank line.
	for {
		line, err := r.in.ReadString('\n')
		if err != nil {
			if err == io.EOF {
				if firstRead && line == "" {
					return nil, io.EOF // clean EOF
				}
				err = io.ErrUnexpectedEOF
			}
			return nil, fmt.Errorf("failed reading header line: %w", err)
		}
		firstRead = false

		line = strings.TrimSpace(line)
		if line == "" {
			break
		}
		colon := strings.IndexRune(line, ':')
		if colon < 0 {
			return nil, fmt.Errorf("invalid header line %q", line)
		}
		name, value := line[:colon], strings.TrimSpace(line[colon+1:])
		switch name {
		case "Content-Length":
			contentLength, err = strconv.ParseInt(value, 10, 64)
			if err != nil {
				return nil, fmt.Errorf("failed parsing Content-Length: %v", value)
			}
			if contentLength <= 0 {
				return nil, fmt.Errorf("invalid Content-Length: %v", contentLength)
			}
		default:
			// ignoring unknown headers
		}
	}
	if contentLength == 0 {
		return nil, fmt.Errorf("missing Content-Length header")
	}
	if contentLength > math.MaxInt {
		return nil, fmt.Errorf("Content-Length too large: %d", contentLength)
	}
	data := make([]byte, contentLength)
	if _, err := io.ReadFull(r.in, data); err != nil {
		return nil, err
	}
	return data, nil
}

// headerWriter writes Content-Length delimited JSON-RPC frames to a stream.
type headerWriter struct {
	out io.Writer
}

func newHeaderWriter(w io.Writer) *headerWriter {
	return &headerWriter{out: w}
}

// Write sends a single frame with Content-Length header.
func (w *headerWriter) Write(data []byte) error {
	if _, err := fmt.Fprintf(w.out, "Content-Length: %d\r\n\r\n", len(data)); err != nil {
		return err
	}
	_, err := w.out.Write(data)
	return err
}
