package copilot

import (
	"fmt"

	"github.com/github/copilot-sdk/go/rpc"
)

// GitHubTokenRequestReason describes why the runtime needs a GitHub token.
//
// Experimental: GitHubTokenRequestReason may change or be removed.
type GitHubTokenRequestReason = rpc.GitHubTokenAcquireReason

const (
	// GitHubTokenRequestReasonInitial indicates the session needs its initial token.
	GitHubTokenRequestReasonInitial = rpc.GitHubTokenAcquireReasonInitial
	// GitHubTokenRequestReasonRefresh indicates the session needs a refreshed token.
	GitHubTokenRequestReasonRefresh = rpc.GitHubTokenAcquireReasonRefresh
)

// GitHubTokenProviderArgs contains the context for a GitHub token request.
//
// Experimental: GitHubTokenProviderArgs may change or be removed.
type GitHubTokenProviderArgs struct {
	// Host is the effective GitHub host for which a token is needed.
	Host string
	// SessionID identifies the session receiving the token. It is nil before a
	// cloud session has been assigned an ID.
	SessionID *string
	// Reason indicates whether this is the initial token or a refresh.
	Reason GitHubTokenRequestReason
}

// GitHubToken contains a GitHub access token returned by a provider.
//
// Experimental: GitHubToken may change or be removed.
type GitHubToken struct {
	// AccessToken is the GitHub access token.
	AccessToken string
	// TokenType is the OAuth token type. The runtime defaults it to "bearer".
	TokenType *string
	// ExpiresIn is the required positive number of seconds remaining when the
	// callback completes. Production GitHub tokens typically last eight hours.
	ExpiresIn int64
}

// String returns a redacted description that never includes the access token.
func (t GitHubToken) String() string {
	tokenType := ""
	if t.TokenType != nil {
		tokenType = *t.TokenType
	}
	return fmt.Sprintf("GitHubToken{TokenType:%q, ExpiresIn:%d, AccessToken:<redacted>}", tokenType, t.ExpiresIn)
}

// GoString returns a redacted Go-syntax description that never includes the access token.
func (t GitHubToken) GoString() string {
	return t.String()
}

// GitHubTokenProviderResult is the result of a GitHub token request.
//
// Experimental: GitHubTokenProviderResult may change or be removed.
type GitHubTokenProviderResult struct {
	Cancelled bool
	Token     *GitHubToken
}

// GitHubTokenResult returns a successful token-provider result.
func GitHubTokenResult(token *GitHubToken) *GitHubTokenProviderResult {
	return &GitHubTokenProviderResult{Token: token}
}

// GitHubTokenCancelled returns a result indicating that token acquisition was cancelled.
func GitHubTokenCancelled() *GitHubTokenProviderResult {
	return &GitHubTokenProviderResult{Cancelled: true}
}

// GitHubTokenProvider acquires session-scoped GitHub tokens on demand. Initial
// cancellation, errors, and invalid token responses reject session creation or
// resume instead of falling back to ambient authentication.
//
// Experimental: GitHubTokenProvider may change or be removed.
type GitHubTokenProvider func(args GitHubTokenProviderArgs) (*GitHubTokenProviderResult, error)
