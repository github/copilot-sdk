/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use super::TraceContext;

#[test]
fn new_yields_empty_context() {
    let ctx = TraceContext::new();
    assert!(ctx.is_empty());
    assert!(ctx.traceparent.is_none());
    assert!(ctx.tracestate.is_none());
}

#[test]
fn builder_composes_traceparent_and_tracestate() {
    let ctx = TraceContext::new()
        .with_traceparent("00-trace-span-01")
        .with_tracestate("vendor=key");
    assert_eq!(ctx.traceparent.as_deref(), Some("00-trace-span-01"));
    assert_eq!(ctx.tracestate.as_deref(), Some("vendor=key"));
    assert!(!ctx.is_empty());
}

#[test]
fn from_traceparent_matches_builder() {
    let direct = TraceContext::from_traceparent("00-trace-span-01");
    let chained = TraceContext::new().with_traceparent("00-trace-span-01");
    assert_eq!(direct, chained);
}
