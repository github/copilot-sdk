//! W3C Trace Context propagation for distributed tracing.
//!
//! The GitHub Copilot CLI propagates [W3C Trace Context] headers (`traceparent`
//! and `tracestate`) so SDK consumers can correlate spans created by the
//! CLI with their own observability pipelines.
//!
//! Two injection paths are supported:
//!
//! - **Per-turn override** via [`MessageOptions::traceparent`] /
//!   [`MessageOptions::tracestate`](crate::types::MessageOptions::tracestate),
//!   which take precedence when set.
//! - **Ambient callback** via
//!   [`ClientOptions::on_get_trace_context`](crate::ClientOptions::on_get_trace_context),
//!   which the SDK invokes before `session.create`, `session.resume`, and
//!   `session.send` whenever the per-turn override is absent.
//!
//! [W3C Trace Context]: https://www.w3.org/TR/trace-context/
//! [`MessageOptions::traceparent`]: crate::types::MessageOptions::traceparent

use async_trait::async_trait;

/// W3C Trace Context headers propagated to and from the GitHub Copilot CLI.
///
/// `traceparent` carries the trace and parent-span identifiers; `tracestate`
/// carries vendor-specific extensions. Either field may be `None` when the
/// caller has nothing to propagate; in that case the corresponding wire
/// field is omitted.
#[derive(Debug, Clone, Default, PartialEq, Eq)]
#[non_exhaustive]
pub struct TraceContext {
    /// `traceparent` HTTP header value.
    pub traceparent: Option<String>,
    /// `tracestate` HTTP header value.
    pub tracestate: Option<String>,
}

impl TraceContext {
    /// Construct a [`TraceContext`] from a `traceparent` header value, with
    /// no `tracestate`.
    pub fn from_traceparent(traceparent: impl Into<String>) -> Self {
        Self {
            traceparent: Some(traceparent.into()),
            tracestate: None,
        }
    }

    /// Set or replace the `tracestate` header value, returning `self` for
    /// chaining.
    pub fn with_tracestate(mut self, tracestate: impl Into<String>) -> Self {
        self.tracestate = Some(tracestate.into());
        self
    }

    /// Returns `true` when neither `traceparent` nor `tracestate` is set.
    pub fn is_empty(&self) -> bool {
        self.traceparent.is_none() && self.tracestate.is_none()
    }
}

/// Async provider that returns the current [`TraceContext`] for outbound
/// session RPCs.
///
/// Set via
/// [`ClientOptions::on_get_trace_context`](crate::ClientOptions::on_get_trace_context).
/// The SDK invokes [`get_trace_context`](Self::get_trace_context) before
/// each `session.create`, `session.resume`, and `session.send` whenever
/// the call site does not carry a per-turn override.
///
/// Implementations should handle errors internally and return
/// [`TraceContext::default()`] to skip injection — no `Result` return type
/// is exposed because trace propagation is a best-effort observability
/// feature, not a correctness-critical RPC parameter.
#[async_trait]
pub trait TraceContextProvider: Send + Sync + 'static {
    /// Return the current trace context, or [`TraceContext::default()`] to
    /// skip injection.
    async fn get_trace_context(&self) -> TraceContext;
}

/// Inject `traceparent` / `tracestate` from `ctx` into the JSON `params`
/// object if either field is set. No-op when both are `None`.
pub(crate) fn inject_trace_context(params: &mut serde_json::Value, ctx: &TraceContext) {
    if let Some(tp) = &ctx.traceparent {
        params["traceparent"] = serde_json::Value::String(tp.clone());
    }
    if let Some(ts) = &ctx.tracestate {
        params["tracestate"] = serde_json::Value::String(ts.clone());
    }
}
