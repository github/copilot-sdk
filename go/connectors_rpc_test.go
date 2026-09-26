package copilot

import (
	"encoding/json"
	"fmt"
	"net"
	"reflect"
	"testing"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
)

const (
	connectorAccountID    = "opaque-account-selection"
	connectorStatusResult = `{
	"accountId":"` + connectorAccountID + `",
	"apiVersion":1,
	"availability":"enabled",
	"catalog":{
		"connectors":[{
			"description":"Calendar and mail",
			"displayName":"Outlook",
			"name":"outlook",
			"runtimeServerIds":["connector-outlook"],
			"status":"connected"
		}],
		"refreshedAtMs":1700000000000,
		"revision":7
	},
	"pendingConnections":2,
	"runtimeServers":[{
		"connectorName":"outlook",
		"runtimeServerId":"connector-outlook",
		"status":"connected"
	}]
}`
)

func TestSessionRPCConnectors(t *testing.T) {
	clientConn, serverConn := net.Pipe()
	client := jsonrpc2.NewClient(clientConn, clientConn)
	server := jsonrpc2.NewClient(serverConn, serverConn)
	client.Start()
	server.Start()
	t.Cleanup(func() {
		client.Stop()
		server.Stop()
		_ = clientConn.Close()
		_ = serverConn.Close()
	})

	session := newSession("session-1", client, "", false)
	t.Cleanup(session.stopEventProcessing)
	if session.RPC == nil || session.RPC.Connectors == nil {
		t.Fatal("session.RPC.Connectors is not initialized")
	}

	t.Run("get capabilities", func(t *testing.T) {
		expectConnectorRPC(t, server, "session.connectors.getCapabilities", map[string]any{
			"sessionId": "session-1",
		}, `{
			"apiVersion":1,
			"availability":"enabled",
			"consentContinuation":true,
			"maxDeadlineMs":60000,
			"maxPollAttempts":10,
			"maxPollIntervalMs":5000,
			"opaqueAccountSelection":true
		}`)

		result, err := session.RPC.Connectors.GetCapabilities(t.Context())
		if err != nil {
			t.Fatalf("GetCapabilities: %v", err)
		}
		if result.APIVersion != 1 ||
			result.Availability != rpc.ConnectorAvailabilityEnabled ||
			!result.ConsentContinuation ||
			result.MaxDeadlineMs != 60000 ||
			result.MaxPollAttempts != 10 ||
			result.MaxPollIntervalMs != 5000 ||
			!result.OpaqueAccountSelection {
			t.Fatalf("GetCapabilities result = %#v", result)
		}
	})

	t.Run("get status", func(t *testing.T) {
		expectConnectorRPC(t, server, "session.connectors.getStatus", map[string]any{
			"sessionId": "session-1",
		}, connectorStatusResult)

		result, err := session.RPC.Connectors.GetStatus(t.Context())
		if err != nil {
			t.Fatalf("GetStatus: %v", err)
		}
		assertConnectorStatus(t, result)
	})

	t.Run("list", func(t *testing.T) {
		expectConnectorRPC(t, server, "session.connectors.list", map[string]any{
			"accountId": connectorAccountID,
			"sessionId": "session-1",
		}, `{
			"connectors":[{
				"description":"Calendar and mail",
				"displayName":"Outlook",
				"name":"outlook",
				"runtimeServerIds":["connector-outlook"],
				"status":"connected"
			}],
			"refreshedAtMs":1700000000000,
			"revision":7
		}`)

		result, err := session.RPC.Connectors.List(t.Context(), &rpc.ConnectorAccountRequest{
			AccountID: connectorAccountID,
		})
		if err != nil {
			t.Fatalf("List: %v", err)
		}
		assertConnectorCatalog(t, result, 7, rpc.ConnectorCatalogStatusConnected)
	})

	t.Run("refresh", func(t *testing.T) {
		expectConnectorRPC(t, server, "session.connectors.refresh", map[string]any{
			"accountId": connectorAccountID,
			"sessionId": "session-1",
		}, `{
			"connectors":[{
				"displayName":"Outlook",
				"name":"outlook",
				"runtimeServerIds":[],
				"status":"not_connected"
			}],
			"refreshedAtMs":1700000000100,
			"revision":8
		}`)

		result, err := session.RPC.Connectors.Refresh(t.Context(), &rpc.ConnectorAccountRequest{
			AccountID: connectorAccountID,
		})
		if err != nil {
			t.Fatalf("Refresh: %v", err)
		}
		assertConnectorCatalog(t, result, 8, rpc.ConnectorCatalogStatusNotConnected)
	})

	t.Run("connect", func(t *testing.T) {
		expectConnectorRPC(t, server, "session.connectors.connect", map[string]any{
			"accountId":     connectorAccountID,
			"connectorName": "outlook",
			"sessionId":     "session-1",
		}, `{
			"kind":"consent_required",
			"consentUrl":"https://example.com/consent",
			"continuationId":"continuation-1"
		}`)

		result, err := session.RPC.Connectors.Connect(t.Context(), &rpc.ConnectorConnectRequest{
			AccountID:     connectorAccountID,
			ConnectorName: "outlook",
		})
		if err != nil {
			t.Fatalf("Connect: %v", err)
		}
		consent, ok := result.(*rpc.ConnectorConnectResultConsentRequired)
		if !ok || consent.ConsentURL != "https://example.com/consent" || consent.ContinuationID != "continuation-1" {
			t.Fatalf("Connect result = %#v", result)
		}
	})

	t.Run("reconnect", func(t *testing.T) {
		expectConnectorRPC(t, server, "session.connectors.reconnect", map[string]any{
			"accountId":     connectorAccountID,
			"connectorName": "outlook",
			"sessionId":     "session-1",
		}, `{"kind":"pending","continuationId":"continuation-2"}`)

		result, err := session.RPC.Connectors.Reconnect(t.Context(), &rpc.ConnectorConnectRequest{
			AccountID:     connectorAccountID,
			ConnectorName: "outlook",
		})
		if err != nil {
			t.Fatalf("Reconnect: %v", err)
		}
		pending, ok := result.(*rpc.ConnectorConnectResultPending)
		if !ok || pending.ContinuationID != "continuation-2" {
			t.Fatalf("Reconnect result = %#v", result)
		}
	})

	t.Run("continue connection", func(t *testing.T) {
		expectConnectorRPC(t, server, "session.connectors.continueConnection", map[string]any{
			"continuationId": "continuation-2",
			"deadlineMs":     float64(30000),
			"maxAttempts":    float64(4),
			"pollIntervalMs": float64(500),
			"sessionId":      "session-1",
		}, `{"kind":"connected","status":`+connectorStatusResult+`}`)

		result, err := session.RPC.Connectors.ContinueConnection(t.Context(), &rpc.ConnectorContinueRequest{
			ContinuationID: "continuation-2",
			DeadlineMs:     30000,
			MaxAttempts:    4,
			PollIntervalMs: 500,
		})
		if err != nil {
			t.Fatalf("ContinueConnection: %v", err)
		}
		connected, ok := result.(*rpc.ConnectorConnectResultConnected)
		if !ok {
			t.Fatalf("ContinueConnection result = %T", result)
		}
		assertConnectorStatus(t, &connected.Status)
	})

	t.Run("disconnect", func(t *testing.T) {
		expectConnectorRPC(t, server, "session.connectors.disconnect", map[string]any{
			"accountId":     connectorAccountID,
			"connectorName": "outlook",
			"sessionId":     "session-1",
		}, `{"disconnected":true,"status":`+connectorStatusResult+`}`)

		result, err := session.RPC.Connectors.Disconnect(t.Context(), &rpc.ConnectorConnectRequest{
			AccountID:     connectorAccountID,
			ConnectorName: "outlook",
		})
		if err != nil {
			t.Fatalf("Disconnect: %v", err)
		}
		if !result.Disconnected {
			t.Fatalf("Disconnect result = %#v", result)
		}
		assertConnectorStatus(t, &result.Status)
	})

	t.Run("reconcile", func(t *testing.T) {
		expectConnectorRPC(t, server, "session.connectors.reconcile", map[string]any{
			"accountId":      connectorAccountID,
			"refreshCatalog": true,
			"sessionId":      "session-1",
		}, connectorStatusResult)

		result, err := session.RPC.Connectors.Reconcile(t.Context(), &rpc.ConnectorReconcileRequest{
			AccountID:      connectorAccountID,
			RefreshCatalog: Bool(true),
		})
		if err != nil {
			t.Fatalf("Reconcile: %v", err)
		}
		assertConnectorStatus(t, result)
	})
}

func expectConnectorRPC(t *testing.T, server *jsonrpc2.Client, method string, wantParams map[string]any, result string) {
	t.Helper()
	wantJSON, err := json.Marshal(wantParams)
	if err != nil {
		t.Fatalf("marshal expected %s params: %v", method, err)
	}
	var want any
	if err := json.Unmarshal(wantJSON, &want); err != nil {
		t.Fatalf("normalize expected %s params: %v", method, err)
	}

	server.SetRequestHandler(method, func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var got any
		if err := json.Unmarshal(params, &got); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
		}
		if !reflect.DeepEqual(got, want) {
			return nil, &jsonrpc2.Error{
				Code:    -32602,
				Message: fmt.Sprintf("%s params = %s, want %s", method, params, wantJSON),
			}
		}
		return json.RawMessage(result), nil
	})
}

func assertConnectorCatalog(t *testing.T, catalog *rpc.ConnectorCatalogResult, revision int64, status rpc.ConnectorCatalogStatus) {
	t.Helper()
	if catalog.Revision != revision || len(catalog.Connectors) != 1 {
		t.Fatalf("catalog = %#v", catalog)
	}
	connector := catalog.Connectors[0]
	if connector.Name != "outlook" || connector.DisplayName != "Outlook" || connector.Status != status {
		t.Fatalf("connector = %#v", connector)
	}
}

func assertConnectorStatus(t *testing.T, status *rpc.ConnectorStatus) {
	t.Helper()
	if status.AccountID == nil ||
		*status.AccountID != connectorAccountID ||
		status.APIVersion != 1 ||
		status.Availability != rpc.ConnectorAvailabilityEnabled ||
		status.PendingConnections != 2 ||
		len(status.RuntimeServers) != 1 {
		t.Fatalf("status = %#v", status)
	}
	assertConnectorCatalog(t, status.Catalog, 7, rpc.ConnectorCatalogStatusConnected)
	runtimeServer := status.RuntimeServers[0]
	if runtimeServer.ConnectorName != "outlook" ||
		runtimeServer.RuntimeServerID != "connector-outlook" ||
		runtimeServer.Status != rpc.ConnectorMCPStatusConnected {
		t.Fatalf("runtime server = %#v", runtimeServer)
	}
}
