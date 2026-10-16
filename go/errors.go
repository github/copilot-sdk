// Copyright (c) Microsoft Corporation. All rights reserved.

package copilot

import "github.com/github/copilot-sdk/go/internal/jsonrpc2"

// RPCError is the SDK's JSON-RPC transport error.
// Use errors.As to retrieve it from errors wrapped by SDK operations.
// It represents remote error responses and locally synthesized errors when an
// inline response callback fails; its type alone does not establish provenance.
//
// Code and Message contain the JSON-RPC error code and message. Data contains
// the optional JSON value, which can be an object, array, or scalar. Omitted
// data is nil; explicit JSON null is the JSON text "null". Error() does not
// include Data.
//
// RPCError is an alias of the transport error, preserving its identity and
// existing wrapping behavior. Its exported fields are part of the public API.
// The fields and Data bytes are shared with the error chain; copy before mutation.
type RPCError = jsonrpc2.Error
