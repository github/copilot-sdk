package copilot

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"reflect"
	"sync"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
	"github.com/google/uuid"
)

// AhpHostExit reports termination of a connection-owned listener.
// Experimental: AHP hosting may change or be removed.
type AhpHostExit = rpc.HostExitedNotification

// AhpSessionCreateRequest contains the settings selected by the AHP host.
// Preserve Config when creating the session; add application prompts, tools and callbacks.
// Experimental: AHP hosting may change or be removed.
type AhpSessionCreateRequest struct {
	Config *SessionConfig
}

// AhpSessionResumeRequest identifies a durable application session to resume.
// Preserve Config, or return the exact retained original from the owning client.
// Experimental: AHP hosting may change or be removed.
type AhpSessionResumeRequest struct {
	SessionID string
	Config    *ResumeSessionConfig
}

// AhpHostOptions configures an experimental, in-process AHP listener.
// Factory contexts are cancelled when participation ends. A late returned session
// is still released exactly once; the SDK never disconnects or destroys it.
// Notification callbacks run asynchronously, with errors reported to the Go logger.
// At least one transport must be supplied. Both may be enabled.
// Experimental: AHP hosting may change or be removed.
type AhpHostOptions struct {
	// ComputeID selects a durable catalog identity; defaults are runtime-owned.
	// It must agree with GitHubEnvironment.ComputeID when both are supplied.
	ComputeID *string
	// LocalServer explicitly enables the local WebSocket listener.
	LocalServer *rpc.HostLocalServerOptions
	// GitHubEnvironment registers the host with Mission Control; Name and ComputeID are required.
	GitHubEnvironment *rpc.HostGitHubEnvironmentOptions
	CreateSession     func(context.Context, AhpSessionCreateRequest) (*Session, error)
	ResumeSession     func(context.Context, AhpSessionResumeRequest) (*Session, error)
	OnSessionReleased func(*Session) error
	OnExit            func(AhpHostExit) error
}

// AhpHost is an experimental listener owned by one SDK connection.
// Reconnecting the client does not transfer this handle to its new connection.
// Experimental: AHP hosting may change or be removed.
type AhpHost struct {
	HostID string
	// URL is absent when no local listener was requested.
	URL   *string
	Token *string
	// EnvironmentID identifies the optional Mission Control registration.
	EnvironmentID *string
	// PID is absent for in-process listeners, not the owning runtime's PID.
	PID *int64
	rpc *rpc.ServerHostAPI
}

// Dispose stops the listener and joins runtime cleanup without deleting application
// sessions. Every call reaches the runtime, including repeated or concurrent calls.
func (h *AhpHost) Dispose(ctx context.Context) error {
	_, err := h.rpc.Dispose(ctx, &rpc.HostDisposeRequest{HostID: h.HostID})
	return err
}

// PublishSession durably advertises the exact attached session in the compute-scoped catalog.
// It does not invoke factories or transfer ownership.
func (h *AhpHost) PublishSession(ctx context.Context, sessionID string) (*rpc.HostPublishSessionResult, error) {
	return h.rpc.PublishSession(ctx, &rpc.HostPublishSessionRequest{HostID: h.HostID, SessionID: sessionID})
}

// ListSessions returns all live and dormant catalog sessions advertised by this host.
func (h *AhpHost) ListSessions(ctx context.Context) (*rpc.HostListSessionsResult, error) {
	return h.rpc.ListSessions(ctx, &rpc.HostListSessionsRequest{HostID: &h.HostID})
}

type ahpHostState struct {
	mu       sync.Mutex
	hosts    map[string]AhpHostOptions
	handoffs map[string]*ahpHandoff
}

type ahpHandoff struct {
	hostID    string
	sessionID string
	options   AhpHostOptions
	ctx       context.Context
	cancel    context.CancelFunc
	session   *Session
	configs   map[*Session]map[string]any
	released  bool
}

var errAhpHandoffEnded = errors.New("AHP session handoff ended")

// StartAhpHost starts a full AHP listener in the connected runtime, without a
// companion process. The listener ends when this connection closes.
// Cancellation abandons the wait; a listener that subsequently starts is disposed.
// Experimental: AHP hosting may change or be removed.
func (c *Client) StartAhpHost(ctx context.Context, options *AhpHostOptions) (*AhpHost, error) {
	if options == nil || (options.LocalServer == nil && options.GitHubEnvironment == nil) {
		return nil, errors.New("AHP hosting requires localServer or githubEnvironment")
	}
	if err := c.ensureConnected(ctx); err != nil {
		return nil, err
	}
	if err := ctx.Err(); err != nil {
		return nil, err
	}
	opts := *options
	c.startStopMux.RLock()
	if c.RPC == nil {
		c.startStopMux.RUnlock()
		return nil, errors.New("client is not connected")
	}
	hostRPC := c.RPC.Host
	c.startStopMux.RUnlock()
	hostID := uuid.NewString()
	c.ahp.mu.Lock()
	if c.ahp.hosts == nil {
		c.ahp.hosts = make(map[string]AhpHostOptions)
		c.ahp.handoffs = make(map[string]*ahpHandoff)
	}
	c.ahp.hosts[hostID] = opts
	c.ahp.mu.Unlock()
	params := &rpc.HostStartRequest{
		HostID: hostID, ComputeID: opts.ComputeID, LocalServer: opts.LocalServer,
		GitHubEnvironment: opts.GitHubEnvironment,
	}
	if opts.CreateSession != nil {
		params.SessionFactory = Bool(true)
	}
	if opts.ResumeSession != nil {
		params.ResumeFactory = Bool(true)
	}
	type startResult struct {
		info *rpc.HostStartResult
		err  error
	}
	started := make(chan startResult, 1)
	go func() {
		info, err := hostRPC.Start(context.WithoutCancel(ctx), params)
		started <- startResult{info, err}
	}()
	select {
	case result := <-started:
		if result.err != nil {
			c.releaseAhpHost(hostID)
			return nil, result.err
		}
		info := result.info
		return &AhpHost{HostID: info.HostID, URL: info.URL, Token: info.Token, PID: info.Pid, EnvironmentID: info.EnvironmentID, rpc: hostRPC}, nil
	case <-ctx.Done():
		// Wait for registration before disposing: an early dispose can miss the listener.
		go func() {
			defer c.releaseAhpHost(hostID)
			if result := <-started; result.err == nil {
				if _, err := hostRPC.Dispose(context.Background(), &rpc.HostDisposeRequest{HostID: result.info.HostID}); err != nil {
					log.Printf("AHP cancelled startup cleanup failed: %v", err)
				}
			}
		}()
		return nil, ctx.Err()
	}
}

func (c *Client) releaseAhpHost(hostID string) (AhpHostOptions, bool) {
	c.ahp.mu.Lock()
	options, exists := c.ahp.hosts[hostID]
	delete(c.ahp.hosts, hostID)
	var handoffs []string
	for id, entry := range c.ahp.handoffs {
		if entry.hostID == hostID {
			handoffs = append(handoffs, id)
		}
	}
	c.ahp.mu.Unlock()
	for _, id := range handoffs {
		c.releaseAhpSession(&rpc.HostSessionReleasedNotification{HostID: hostID, HandoffID: id})
	}
	return options, exists
}

func (c *Client) handleAhpExit(event *rpc.HostExitedNotification) {
	options, exists := c.releaseAhpHost(event.HostID)
	if exists && options.OnExit != nil {
		go reportAhpCallback("exit", func() error { return options.OnExit(*event) })
	}
}

func (c *Client) disconnectAhpHosts() {
	c.ahp.mu.Lock()
	var ids []string
	for id := range c.ahp.hosts {
		ids = append(ids, id)
	}
	c.ahp.mu.Unlock()
	for _, id := range ids {
		c.handleAhpExit(&rpc.HostExitedNotification{
			HostID: id, Reason: "ownerDisconnected",
			Error: String("Owner connection closed; runtime cleanup cannot be acknowledged on this connection."),
		})
	}
}

func (c *Client) releaseAhpSession(notification *rpc.HostSessionReleasedNotification) {
	c.ahp.mu.Lock()
	entry := c.ahp.handoffs[notification.HandoffID]
	if entry == nil || entry.hostID != notification.HostID {
		c.ahp.mu.Unlock()
		return
	}
	delete(c.ahp.handoffs, notification.HandoffID)
	entry.released = true
	entry.cancel()
	session := entry.session
	entry.session = nil
	entry.configs = nil
	c.ahp.mu.Unlock()
	notifyAhpReleased(entry.options, session)
}

func notifyAhpReleased(options AhpHostOptions, session *Session) {
	if session != nil && options.OnSessionReleased != nil {
		go reportAhpCallback("session release", func() error { return options.OnSessionReleased(session) })
	}
}

func reportAhpCallback(label string, callback func() error) {
	defer func() {
		if failure := recover(); failure != nil {
			log.Printf("AHP %s callback panicked: %v", label, failure)
		}
	}()
	if err := callback(); err != nil {
		log.Printf("AHP %s callback failed: %v", label, err)
	}
}

func (c *Client) captureAhpSession(session *Session, request any) error {
	c.ahp.mu.Lock()
	var entries []*ahpHandoff
	for _, entry := range c.ahp.handoffs {
		if entry.sessionID == session.SessionID && entry.configs != nil {
			entries = append(entries, entry)
		}
	}
	c.ahp.mu.Unlock()
	if len(entries) == 0 {
		return nil
	}
	data, err := json.Marshal(request)
	if err != nil {
		return fmt.Errorf("capture AHP session settings: %w", err)
	}
	var snapshot map[string]any
	if err := json.Unmarshal(data, &snapshot); err != nil {
		return fmt.Errorf("capture AHP session settings: %w", err)
	}
	c.ahp.mu.Lock()
	defer c.ahp.mu.Unlock()
	for _, entry := range entries {
		if !entry.released && entry.configs != nil {
			entry.configs[session] = snapshot
		}
	}
	return nil
}

func decodeAhpConfig(config map[string]any, target any) error {
	copy := make(map[string]any, len(config))
	var token string
	for key, value := range config {
		if key == "gitHubToken" {
			var ok bool
			token, ok = value.(string)
			if !ok {
				return errors.New("AHP gitHubToken must be a string")
			}
			continue
		}
		if key == "configDir" {
			key = "configDirectory"
		}
		copy[key] = value
	}
	if _, resume := target.(*ResumeSessionConfig); resume {
		delete(copy, "sessionId")
	}
	data, err := json.Marshal(copy)
	if err != nil {
		return err
	}
	decoder := json.NewDecoder(bytes.NewReader(data))
	decoder.DisallowUnknownFields()
	if err := decoder.Decode(target); err != nil {
		return err
	}
	// Per-session credentials are intentionally excluded from ordinary config JSON.
	switch options := target.(type) {
	case *SessionConfig:
		options.GitHubToken = token
	case *ResumeSessionConfig:
		options.GitHubToken = token
	}
	return nil
}

func containsAhpConfig(actual, expected any, root bool) bool {
	if required, ok := expected.(map[string]any); ok {
		value, ok := actual.(map[string]any)
		if !ok {
			return false
		}
		for key, expectedValue := range required {
			actualValue, exists := value[key]
			// These default-false flags are omitted by the session request builder.
			if !exists && root && expectedValue == false && (key == "requestMcpApps" || key == "disableResume") {
				continue
			}
			if !exists || !containsAhpConfig(actualValue, expectedValue, false) {
				return false
			}
		}
		return true
	}
	return reflect.DeepEqual(actual, expected)
}

func (c *Client) materializeAhpSession(params *rpc.HostSessionCreateCallback) (*rpc.HostSessionCreateResult, *jsonrpc2.Error) {
	fail := func(err error) (*rpc.HostSessionCreateResult, *jsonrpc2.Error) {
		return nil, &jsonrpc2.Error{Code: jsonrpc2.ErrInternal.Code, Message: err.Error()}
	}
	resume := params.Resume != nil && *params.Resume
	sessionID, _ := params.Config["sessionId"].(string)
	c.ahp.mu.Lock()
	options, exists := c.ahp.hosts[params.HostID]
	if !exists || c.ahp.handoffs[params.HandoffID] != nil ||
		(resume && options.ResumeSession == nil) || (!resume && options.CreateSession == nil) || sessionID == "" {
		c.ahp.mu.Unlock()
		return fail(errors.New("AHP session factory is unavailable or handoff already exists"))
	}
	ctx, cancel := context.WithCancel(context.Background())
	entry := &ahpHandoff{
		hostID: params.HostID, sessionID: sessionID, options: options,
		ctx: ctx, cancel: cancel, configs: make(map[*Session]map[string]any),
	}
	c.ahp.handoffs[params.HandoffID] = entry
	c.ahp.mu.Unlock()

	done := make(chan error, 1)
	go func() {
		session, err := invokeAhpFactory(entry, params.Config, resume)
		if err == nil {
			c.sessionsMux.Lock()
			owned := session != nil && session.SessionID == sessionID && c.sessions[sessionID] == session
			c.sessionsMux.Unlock()
			if !owned {
				err = errors.New("AHP callback must return the requested session from this client")
			}
		}
		expected := make(map[string]any, len(params.Config))
		for key, value := range params.Config {
			switch key {
			case "suppressResumeEvent":
				key = "disableResume"
			case "enableExperimentalMode":
				key = "isExperimentalMode"
			case "enableMcpApps":
				key = "requestMcpApps"
			}
			expected[key] = value
		}
		c.ahp.mu.Lock()
		entry.session = session
		released := entry.released
		if released {
			entry.session = nil
			if err == nil {
				err = errAhpHandoffEnded
			}
		} else if err == nil {
			actual, captured := entry.configs[session]
			if (!resume || captured) && !containsAhpConfig(actual, expected, true) {
				err = errors.New("AHP callback must preserve the supplied session configuration")
			}
		}
		entry.configs = nil
		c.ahp.mu.Unlock()
		if released {
			notifyAhpReleased(options, session)
		}
		if err != nil {
			if !errors.Is(err, errAhpHandoffEnded) && (!released || !errors.Is(err, context.Canceled)) {
				log.Printf("AHP session factory failed: %v", err)
			}
		}
		done <- err
		if err != nil {
			c.releaseAhpSession(&rpc.HostSessionReleasedNotification{HostID: params.HostID, HandoffID: params.HandoffID})
		}
	}()
	complete := func(err error) (*rpc.HostSessionCreateResult, *jsonrpc2.Error) {
		if err != nil {
			return fail(err)
		}
		return &rpc.HostSessionCreateResult{SessionID: sessionID}, nil
	}
	select {
	case err := <-done:
		return complete(err)
	case <-ctx.Done():
		select {
		case err := <-done:
			return complete(err)
		default:
			return fail(errAhpHandoffEnded)
		}
	}
}

func invokeAhpFactory(entry *ahpHandoff, config map[string]any, resume bool) (session *Session, err error) {
	defer func() {
		if failure := recover(); failure != nil {
			err = fmt.Errorf("AHP session factory panicked: %v", failure)
		}
	}()
	if resume {
		var options ResumeSessionConfig
		if err := decodeAhpConfig(config, &options); err != nil {
			return nil, err
		}
		return entry.options.ResumeSession(entry.ctx, AhpSessionResumeRequest{SessionID: entry.sessionID, Config: &options})
	}
	var options SessionConfig
	if err := decodeAhpConfig(config, &options); err != nil {
		return nil, err
	}
	return entry.options.CreateSession(entry.ctx, AhpSessionCreateRequest{Config: &options})
}
