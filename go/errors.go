package copilot

import "github.com/github/copilot-sdk/go/internal/jsonrpc2"

// RPCError is an error response from the runtime's JSON-RPC API.
// Use errors.As to retrieve it from errors wrapped by SDK operations.
//
// Code and Message contain the JSON-RPC error code and message. Data contains
// the optional JSON value, which can be an object, array, or scalar. Omitted
// data is nil; explicit JSON null is the JSON text "null". Error() does not
// include Data.
//
// RPCError is an alias of the transport error, preserving its identity and
// existing wrapping behavior.
type RPCError = jsonrpc2.Error
